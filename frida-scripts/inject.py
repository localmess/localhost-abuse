
import frida
import sys
import subprocess
import argparse
import time
import socket
import threading
from pathlib import Path
import os

import shutil

import subprocess
import shutil
import os
from pathlib import Path
import signal

class TeeLogger:
    def __init__(self, log_path):
        self.terminal = sys.stdout
        self.log = open(log_path, "w", buffering=1)

    def write(self, message):
        self.terminal.write(message)
        self.log.write(message)

    def flush(self):
        self.terminal.flush()
        self.log.flush()

mitm_proc = None
proxy_set = False

def check_adb_device_available():
    result = subprocess.run(["adb", "devices"], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    lines = result.stdout.strip().splitlines()

    connected_devices = [line for line in lines[1:] if "device" in line and not line.startswith("*")]
    if not connected_devices:
        print("[-] No ADB device connected. Please connect a device and try again.")
        sys.exit(1)
    print(f"[+] Found {len(connected_devices)} connected ADB device(s).")


def get_app_version(package_name):
    try:
        result = subprocess.run(
            ["adb", "shell", f"dumpsys package {package_name} | grep versionName"],
            stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True
        )
        for line in result.stdout.splitlines():
            if "versionName=" in line:
                return line.strip().split("=")[-1]
    except Exception:
        return "unknown"
    return "unknown"



def signal_handler(sig, frame):
    print(f"\n[!] Caught signal {sig}.")
    cleanup()
    sys.exit(0)

def check_cert_in_system_store(cert_file):
    """Check if the cert file already exists in the system cert store."""
    check_cmd = ["adb", "shell", f"ls /system/etc/security/cacerts/{cert_file}"]
    result = subprocess.run(check_cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    return result.returncode == 0

def generate_android_system_cert(cert_input_path="~/.mitmproxy/mitmproxy-ca-cert.pem"):
    """Generate the system CA certificate file in <subject_hash>.0 format."""
    cert_input = Path(cert_input_path).expanduser()
    if not cert_input.exists():
        print(f"[-] mitmproxy CA cert not found at: {cert_input}")
        return None

    try:
        result = subprocess.run(
            ["openssl", "x509", "-inform", "PEM", "-subject_hash_old", "-in", str(cert_input)],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=True
        )
        subject_hash = result.stdout.decode().splitlines()[0].strip()
        output_filename = f"{subject_hash}.0"

        shutil.copy(cert_input, output_filename)
        print(f"[+] Created system CA cert file: {output_filename}")
        return output_filename
    except subprocess.CalledProcessError as e:
        print("[-] Failed to compute subject hash or copy file.")
        print(e.stderr.decode())
        return None

def push_mitmproxy_cert(cert_file, use_su=False):
    shell_prefix = ["adb", "shell"]
    if use_su:
        shell_prefix += ["su", "-c"]

    def run_adb_root():
        return subprocess.run(["adb", "root"], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    res = run_adb_root()
    if res.returncode != 0:
        print("[-] Failed to run adb root.")
        return False
    print("[+] adb root successful.")


    def run_shell(cmd):
        full_cmd = shell_prefix + [cmd]
        return subprocess.run(full_cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

    try:
        if check_cert_in_system_store(cert_file):
            print("[*] mitmproxy cert already present in system store.")
            return True

        print("[*] mitmproxy cert not found. Pushing to system store...")

        cmds = [
            'rm -rf /data/local/tmp/htk-ca-copy',
            'mkdir -m 700 /data/local/tmp/htk-ca-copy',
            'cp /system/etc/security/cacerts/* /data/local/tmp/htk-ca-copy/',
            'mount -t tmpfs tmpfs /system/etc/security/cacerts',
            'mv /data/local/tmp/htk-ca-copy/* /system/etc/security/cacerts/',
        ]

        for cmd in cmds:
            res = run_shell(cmd)
            if res.returncode != 0:
                print(f"[-] Failed: {cmd}\n{res.stderr.decode().strip()}")
                return False

        subprocess.run(["adb", "push", cert_file, f"/storage/emulated/0/Download/{cert_file}"], check=True)

        post_cmds = [
            f'cp /storage/emulated/0/Download/{cert_file} /system/etc/security/cacerts/',
            'chown root:root /system/etc/security/cacerts/*',
            'chmod 644 /system/etc/security/cacerts/*',
            'chcon u:object_r:system_file:s0 /system/etc/security/cacerts/*',
            'rm -r /data/local/tmp/htk-ca-copy',
        ]

        for cmd in post_cmds:
            res = run_shell(cmd)
            if res.returncode != 0:
                print(f"[-] Failed: {cmd}\n{res.stderr.decode().strip()}")
                return False

        print("[+] mitmproxy cert successfully installed into system store.")
        return True

    except Exception as e:
        print(f"[-] Exception during cert push: {e}")
        return False

def get_local_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("8.8.8.8", 80))
        return s.getsockname()[0]
    finally:
        s.close()

def set_wifi_proxy(host, port):
    try:
        print(f"[*] Setting Wi-Fi proxy to {host}:{port}...")
        subprocess.run(["adb", "shell", "settings", "put", "global", "http_proxy", ":0"], check=False)

        result = subprocess.run(["adb", "shell", "cmd", "wifi", "list-networks"], capture_output=True, text=True)
        lines = result.stdout.strip().splitlines()
        active_net_id = None
        for line in lines:
            if "*" in line:
                active_net_id = line.split()[0]
                break

        if active_net_id:
            subprocess.run([
                "adb", "shell", "cmd", "wifi", "set-ipproxy", active_net_id, f"http://{host}:{port}"
            ], check=True)
            print("[+] Proxy set on Wi-Fi network.")
        else:
            print("[-] No active Wi-Fi network found. Falling back to global proxy.")
            subprocess.run([
                "adb", "shell", "settings", "put", "global", "http_proxy", f"{host}:{port}"
            ], check=True)
    except subprocess.CalledProcessError as e:
        print("[-] Failed to set proxy:", e)

def clear_wifi_proxy():
    try:
        print("[*] Clearing proxy...")
        subprocess.run(["adb", "shell", "settings", "put", "global", "http_proxy", ":0"], check=True)
        print("[+] Proxy cleared.")
    except subprocess.CalledProcessError as e:
        print("[-] Failed to clear proxy:", e)

def stream_subprocess_output(pipe, label):
    for line in iter(pipe.readline, b""):
        print(f"[{label}] {line.decode().strip()}")

def start_mitm(dump_file_path, port, use_cli=False, upstream=None):
    try:
        tool = "mitmdump" if use_cli else "mitmweb"
        print(f"[*] Starting {tool} on port {port}, saving flows to {dump_file_path}...")

        cmd = [tool, "-p", str(port), "-w", str(dump_file_path)]
        if upstream:
            cmd += ["--mode", f"upstream:{upstream}"]
            print(f"[*] Using upstream proxy: {upstream}")

        proc = subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE
        )
        threading.Thread(target=stream_subprocess_output, args=(proc.stdout, tool), daemon=True).start()
        threading.Thread(target=stream_subprocess_output, args=(proc.stderr, tool), daemon=True).start()
        time.sleep(5)
        if proc.poll() is None:
            print(f"[+] {tool} started.")
            return proc
        else:
            print(f"[-] {tool} terminated early.")
            return None
    except Exception as e:
        print(f"[-] Failed to start {tool}: {e}")
        return None


def cleanup():
    global mitm_proc, proxy_set
    print("[*] Cleaning up...")
    if proxy_set:
        clear_wifi_proxy()
    if mitm_proc and mitm_proc.poll() is None:
        print("[*] Terminating mitmweb...")
        mitm_proc.terminate()
        mitm_proc.wait()
    print("[+] Cleanup complete. Exiting.")


def signal_handler(sig, frame):
    print(f"\n[!] Caught signal {sig}.")
    cleanup()
    sys.exit(0)


def on_message(message, data):
    if message["type"] == "send":
        print(f"[Frida] {message['payload']}")
    elif message["type"] == "error":
        print(f"[Frida][Error] {message['stack']}")
    else:
        print(f"[Frida][Other] {message}")

def load_frida_script(session, path, label):
    if not path.exists():
        print(f"[-] Missing script: {path}")
        sys.exit(1)
    with open(path, "r") as f:
        script = session.create_script(f.read())
        script.on("message", on_message)
        script.load()
        print(f"[+] Loaded: {label}")
    return script

    
signal.signal(signal.SIGINT, signal_handler)
signal.signal(signal.SIGTERM, signal_handler)

def hook_ssl_unpinning(package_name, proxy_port=8080, use_su=False, use_cli=False, upstream=None):
    """Hook SSL unpinning for a given package name using Frida and mitmproxy."""
    global mitm_proc, proxy_set

    cert_file = generate_android_system_cert()
    if not cert_file or not Path(cert_file).exists():
        print("[-] Failed to generate Android system cert.")
        sys.exit(1)

    if not check_cert_in_system_store(cert_file):
        if not push_mitmproxy_cert(cert_file, use_su):  
            print("[-] Could not push mitmproxy cert.")
            sys.exit(1)

    local_ip = get_local_ip()
    version = get_app_version(package_name)

    mitm_dump_dir = Path("mitmdumps")
    if not mitm_dump_dir.exists():
        print("[*] Creating mitmdumps directory...")
        mitm_dump_dir.mkdir(parents=True, exist_ok=True)
    mitm_dump_path = mitm_dump_dir / f"{package_name.replace('.', '_')}_v{version}.dump"


    set_wifi_proxy(local_ip, proxy_port)
    proxy_set = True

    mitm_proc = start_mitm(mitm_dump_path, proxy_port, use_cli, upstream)
    if not mitm_proc:
        cleanup()
        return
    
    try:
        device = frida.get_usb_device(timeout=5)
        print(f"[*] Spawning {package_name}...")
        pid = device.spawn([package_name])

        print("[*] Attaching to process...")
        session = device.attach(pid)

        universal_script_path = "ssl-unpinning.js"
        specific_script_path = None
        
        if "instagram" in package_name.lower():
            specific_script_path = "instagram_ssl_bypass.js"
        elif "facebook" in package_name.lower():
            specific_script_path = "facebook_ssl_bypass.js"
        else:
            print("[!] No specific SSL bypass needed for this package.")

        if not os.path.exists(universal_script_path):
            print(f"[-] Missing: {universal_script_path}")
            sys.exit(1)

        combined_script_content = Path(universal_script_path).read_text() + "\n"

        if specific_script_path and os.path.exists(specific_script_path):
            combined_script_content += Path(specific_script_path).read_text() + "\n"

        script = session.create_script(combined_script_content)
        script.on("message", on_message)
        script.load()
        print("[+] Loaded combined Frida script.")

        print("[*] Resuming app...")
        device.resume(pid)
        print("[*] All scripts loaded. Press Ctrl+C to detach.")

        sys.stdin.read() 

    except KeyboardInterrupt:
        print("\n[*] Interrupted by user.")
    except Exception as e:
        print(f"[-] Error: {e}")
    finally:
        cleanup()


if __name__ == "__main__":
    sys.stdout = TeeLogger("logs.txt")
    sys.stderr = sys.stdout

    parser = argparse.ArgumentParser(description="Frida injector with SSL unpinning and mitmweb")
    parser.add_argument("package", help="Package name (e.g., com.instagram.android)")
    parser.add_argument("--port", type=int, default=8080, help="Port for mitmweb (default: 8080)")
    parser.add_argument("--su", action="store_true", help="Use su for pushing cert (specifically for Magisk users)")
    parser.add_argument("--mitm_cli", action="store_true", help="Use mitmdump (headless) instead of mitmweb")
    parser.add_argument("--upstream", help="Upstream proxy (e.g., http://proxy:port)")
    args = parser.parse_args()

    check_adb_device_available()

    hook_ssl_unpinning(args.package, args.port, args.su, args.mitm_cli, args.upstream)
