import socket
import struct
import signal
import sys
import threading
import time

UDP_IP4 = "0.0.0.0"
UDP_PORT4 = 10000
# UDP_PORT4 = 12580

UDP_IP6 = "::"
UDP_PORT6 = 10000

def extract_ufrag(stun_data):
    if len(stun_data) < 20:
        return None
    # Confirm STUN Binding Request (0x0001) and Magic Cookie (fixed value)
    msg_type = struct.unpack("!H", stun_data[0:2])[0]
    magic_cookie = stun_data[4:8]

    if msg_type != 0x0001 or magic_cookie != b'\x21\x12\xa4\x42':
        return None

    # Parse attributes starting after the 20-byte STUN header
    i = 20
    while i + 4 <= len(stun_data):
        attr_type = struct.unpack("!H", stun_data[i:i+2])[0]
        attr_len = struct.unpack("!H", stun_data[i+2:i+4])[0]
        attr_val = stun_data[i+4:i+4+attr_len]

        if attr_type == 0x0006:  # USERNAME attribute
            try:
                full_username = attr_val.decode(errors='ignore')
                ufrag = full_username.split(":")[0]  # Extract local ufrag
                return ufrag
            except Exception:
                return None

        # Move to next attribute (aligned to 4-byte boundary)
        i += 4 + attr_len
        if attr_len % 4 != 0:
            i += 4 - (attr_len % 4)

    return None

def run_ipv4():
    sock4 = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock4.bind((UDP_IP4, UDP_PORT4))

    print(f"Listening for STUN Binding Requests on {UDP_IP4}:{UDP_PORT4}...")

    while True:
        data4, addr4 = sock4.recvfrom(4096)
        ufrag4 = extract_ufrag(data4)
        if ufrag4:
            print(f"Received ufrag from IPv4 {addr4[0]}:{addr4[1]} → {ufrag4}")

def run_ipv6():
    sock6 = socket.socket(socket.AF_INET6, socket.SOCK_DGRAM)
    sock6.bind((UDP_IP6, UDP_PORT6))

    print(f"Listening for STUN Binding Requests on {UDP_IP6}:{UDP_PORT6}...")

    while True:
        data6, addr6 = sock6.recvfrom(4096)
        ufrag6 = extract_ufrag(data6)
        if ufrag6:
            print(f"Received ufrag from IPv6 {addr6[0]}:{addr6[1]} → {ufrag6}")

if __name__ == '__main__':
    try:
        t1 = threading.Thread(target=run_ipv4, daemon=True)
        t2 = threading.Thread(target=run_ipv6, daemon=True)
        
        print("Listeners starting...")
        t1.start()
        time.sleep(3)
        t2.start()
        while True:
            pass

    except KeyboardInterrupt:
        print("\nShutting down.")
