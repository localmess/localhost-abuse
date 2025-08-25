import json
import glob
import re
import csv
import socket
import subprocess
from tld import get_fld
from urllib.parse import urlparse
from functools import lru_cache

from concurrent.futures import ProcessPoolExecutor, as_completed, TimeoutError as concurrentTimeoutError
from tqdm import tqdm

# location = "frankfurt"
location = "new_york"

# version = "_100k"
# version = "_desktop"
# version = "_recrawl"
# version = "_ios"
version = "_post"

# input_folder = location + "_data/data\\"
# input_folder = "desktop_data/" + location + "/data\\"
# input_folder = location + "_recrawl" + "/data\\"
# input_folder = "iOS_crawls/" + location + "_ios_data\\"
input_folder = "../Meta_Pixel/post_release_crawls/" + location + "_post_data\\"

results_folder = "_new_results/"

rank_file_path = "202502.csv"
outputSize = 100000



LOCALHOST_IPS = {"127.0.0.1", "::1", "0.0.0.0"}
SCRIPT_URL_CUTOFF = 512
INCLUDE_SDP_LOCAL = False
MAX_WORKERS = 5
TIMEOUT_SECONDS = 60

ranking = {}



@lru_cache(maxsize=100_000)
def safe_get_fld(url):
    """
    Get domain with cache.
    """
    try:
        return get_fld(url, fail_silently=True)
    except Exception:
        return None

def resolves_to_localhost_with_timeout(hostname, timeout=2):
    """
    Resolve hostname and check if it points to localhost.
    """
    try:
        # Run nslookup with timeout
        result = subprocess.run(
            ["nslookup", hostname],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=timeout,
            text=True
        )
        output = result.stdout
        return any(ip in LOCALHOST_IPS for ip in re.findall(r"\b(?:\d{1,3}\.){3}\d{1,3}\b", output))
    except subprocess.TimeoutExpired:
        print(f"⏱️ DNS timeout: {hostname}")
        return False
    except Exception as e:
        return False

@lru_cache(maxsize=100_000)
def resolves_to_localhost(hostname):
    """
    Resolve hostname and check if it points to localhost.
    """
    return resolves_to_localhost_with_timeout(hostname)
    # try:
    #     ips = socket.gethostbyname_ex(hostname)[2]
    #     return any(ip in LOCALHOST_IPS for ip in ips)
    # except Exception:
    #     return False
    
def extract_port_parsed(parsed_url):
    """
    Extract port, returns defaults if not found.
    """
    if parsed_url.port:
        return parsed_url.port
    return 443 if parsed_url.scheme == "https" else 80

def is_localhost_request(url : str):
    """
    Check if a url is for a request to a localhost port by using urlparse().
    Returns if it is a localhost request, the search type and the found port.
    """
    parsed_url = urlparse(url)
    localhost_variants = {"127.0.0.1", "::1", "localhost", "0.0.0.0"}

    # Check if the hostname is a localhost variant
    if parsed_url.hostname in localhost_variants:
        # Get the port (default to 80 for HTTP and 443 for HTTPS if not explicitly set)
        port = parsed_url.port if parsed_url.port else (80 if parsed_url.scheme == "http" else 443)
        return True, parsed_url.hostname, port
    return False, None, None

def analyze_request(file_url : str, final_url : str, rank : str, request : dict):
    """
    Analyze a request to check if it is a request to localhost and return its details if so.
    Returns request_result = {
                            "rank": rank of the tested url,
                            "tested_url": initial url of the visited page,
                            "final_url": final url of the visited page after loading,
                            "final_domain": final domain of the visited page after loading,
                            "protocol": if the request is a WebSocket or HTTP request,
                            "search_type": which localhost variant is found in the request url,
                            "port_num": port number found,
                            "request_url": url of the request,
                            "script_domain": domain of script initiating request,
                            "script_url": script url of initiating script, cutoff to SCRIPT_URL_CUTOFF characters,
                            }
    """
    url = request.get("url", "")

    is_localhost, search_type, port = is_localhost_request(url)

    if not is_localhost:
        parsed = urlparse(url)
        hostname = parsed.hostname
        if hostname is None or not parsed.port:
            return None

        if not resolves_to_localhost(hostname):
            return None

        search_type = "resolves"
        port = extract_port_parsed(parsed)

    initiators = request.get("initiators", [])
    initial_initiator = initiators[0] if len(initiators) else "Unknown"

    parsed_final_url = urlparse(final_url)
    final_domain = safe_get_fld(parsed_final_url.scheme + "://" + parsed_final_url.hostname)
    parsed_initial_initiator = urlparse(initial_initiator)
    if parsed_initial_initiator.hostname:
        initiator_domain = safe_get_fld(parsed_initial_initiator.scheme + "://" + parsed_initial_initiator.hostname)
    else:
        initiator_domain = ""
    protocol = request.get("type")


    request_result = {
                    "rank": rank,
                    "tested_url": file_url,
                    "final_url": final_url,
                    "final_domain": final_domain,
                    "protocol": "WebSocket" if protocol == "WebSocket" else "HTTP",
                    "search_type": search_type,
                    "port_num": port,
                    "request_url": url,
                    "script_domain": initiator_domain,
                    "script_url": initial_initiator[:SCRIPT_URL_CUTOFF],
                    }
    return request_result

def analyze_request_data(file_url : str, final_url : str, rank : str, request_data : list):
    """
    Analyze the request data for any localhost requests.
    Returns a list of request results.
    """
    requests_results = []

    for request in request_data:
        request_result = analyze_request(file_url, final_url, rank, request)
        if not request_result:
            continue

        requests_results.append(request_result)

    return requests_results

def extract_ports(candidate_string):
    """
    Extracts port numbers from a WebRTC candidate.
    Returns a list of port numbers or ["N/A"] if none are found.
    """
    # Define regex patterns
    ip_port_pattern = re.findall(r'(\d+\.\d+\.\d+\.\d+)\s+(\d+)', candidate_string)
    sctp_port_pattern = re.findall(r'a=sctp-port:(\d+)', candidate_string)

    # Extract ports from matches
    ports = [port for _, port in ip_port_pattern] + sctp_port_pattern
    return ports if ports else ["N/A"]

def analyze_webRTC_call(file_url : str, final_url : str, rank : str, webRTC_call : dict):
    """
    Analyze a request call for setting up a localhost connection.
    Returns a list of call_results, one for each port found.
    call_result = {
                    "rank": rank of the tested url,
                    "tested_url": initial url of the visited page,
                    "final_url": final url of the visited page after loading,
                    "final_domain": final domain of the visited page after loading,
                    "protocol": the type of WebRTC function call,
                    "search_type": which localhost variant is found in the candidate,
                    "port_num": port number found,
                    "request_url": replaced with full candidate,
                    "script_domain": domain of script initiating call,
                    "script_url": script url of initiating script, cutoff to SCRIPT_URL_CUTOFF characters,
                    }
    """
    call_type = webRTC_call.get("type", "")
    if  call_type == "RTCPeerConnection" or call_type == "":
        return []
    if not INCLUDE_SDP_LOCAL and call_type == "SDP-Local":
        return []

    search_type = webRTC_call.get("localhost", "None")
    if search_type == "None":
        return []

    candidate = webRTC_call.get("candidate", "")
    all_ports = set(extract_ports(candidate))

    parsed_final_url = urlparse(final_url)
    final_domain = safe_get_fld(parsed_final_url.scheme + "://" + parsed_final_url.hostname)
    initiator = webRTC_call.get("source", "Unknown")
    parsed_initiator = urlparse(initiator)
    if parsed_initiator.hostname:
        initiator_domain = safe_get_fld(parsed_initiator.scheme + "://" + parsed_initiator.hostname)
    else:
        initiator_domain = ""

    all_call_results = []
    for port in all_ports:
        call_result = {
                    "rank": rank,
                    "tested_url": file_url,
                    "final_url": final_url,
                    "final_domain": final_domain,
                    "protocol": call_type,
                    "search_type": search_type,
                    "port_num": port,
                    "request_url": candidate,
                    "script_domain": initiator_domain,
                    "script_url": initiator[:SCRIPT_URL_CUTOFF],
                    }
        all_call_results.append(call_result)

    return all_call_results

def analyze_webRTC_data(file_url : str, final_url : str, rank : str, webRTC_data : list):
    """
    Analyze the WebRTC data for any localhost calls.
    Returns a list of unique call results.
    """
    webRTC_results = []

    for call in webRTC_data:
        call_results = analyze_webRTC_call(file_url, final_url, rank, call)

        if len(call_results) == 0:
            continue

        webRTC_results = webRTC_results + call_results
    return list(webRTC_results)

@lru_cache(maxsize=100_000)
def get_rank(tested_url):
    """
    Get the rank of the given url.
    Returns "?" if not found.
    """
    if tested_url.endswith("/"):
        tested_url = tested_url[:-1]
    return ranking.get(tested_url, "?")

def analyze_file(file_name : str):
    """
    Analyze the file for any localhost requests or WebRTC calls.
    Returns two lists for both request results and WebRTC results.
    """
    # print(f"🔍 Processing: {file_name}")
    if file_name == input_folder + "metadata.json":
        return None, None
    # Load the data from the .json file
    f = open(file_name, encoding="utf-8")
    file_data = json.load(f)
    f.close()

    initial_url = file_data.get("initialUrl", None)
    final_url = file_data.get("finalUrl", None)
    if not initial_url or not final_url:
        print(f"Warning! No initial_url or final_url for {file_name}")

    data = file_data.get("data", None)
    if not data:
        print(f"Error! No Data for {file_name}")
        return None, None
    
    rank = get_rank(initial_url)

    request_data = data.get("requests", [])
    request_results = analyze_request_data(initial_url, final_url, rank, request_data)

    webRTC_data = data.get("webRTC", [])
    webRTC_results = analyze_webRTC_data(initial_url, final_url, rank, webRTC_data)

    return request_results, webRTC_results

def main():
    files = glob.glob(input_folder + '*.json', recursive = False)
    crawl_count = 0

    print("Retrieving Ranking...")
    with open(rank_file_path) as file:
        reader = csv.reader(file)
        count = 0

        for row in reader:
            ranking[row[0]] = row[1]
            count += 1
            if count > outputSize:
                break

    with open(results_folder + "requests_output_" + location + version + ".csv", "w", newline="", encoding="utf-8") as f1, \
        open(results_folder + "webRTC_output_" + location + version + ".csv", "w", newline="", encoding="utf-8") as f2:
        WRITER_REQS = None
        WRITER_RTC = None
        print("Starting processing...")
        with ProcessPoolExecutor(max_workers=MAX_WORKERS) as executor:
            future_to_file = {executor.submit(analyze_file, file): file for file in files}

            for future in tqdm(as_completed(future_to_file), total=len(future_to_file), desc="Processing files"):
                file = future_to_file[future]
                try:
                    request_results, webRTC_results = future.result(timeout=TIMEOUT_SECONDS)

                    crawl_count += 1
                    # if not crawl_count % 100:
                    #     print(crawl_count)

                    if request_results and len(request_results):
                        if WRITER_REQS is None:
                            WRITER_REQS = csv.DictWriter(f1, fieldnames=request_results[0].keys())
                            WRITER_REQS.writeheader()
                        for res in request_results:
                            WRITER_REQS.writerow(res)
                    if webRTC_results and len(webRTC_results):
                        if WRITER_RTC is None:
                            WRITER_RTC = csv.DictWriter(f2, fieldnames=webRTC_results[0].keys())
                            WRITER_RTC.writeheader()
                        for res in webRTC_results:
                            WRITER_RTC.writerow(res)

                except concurrentTimeoutError:
                    print(f"⏱️ Timeout on file: {file}")
                    with open("timedout_" + location + version + ".txt", 'a') as f:
                        f.write(file + "\n")
                except Exception as e:
                    print(f"❌ Error processing {file}: {e}")
                    with open("failed_" + location + version + ".txt", 'a') as f:
                        f.write(file + "\n")

if __name__ == "__main__":
    main()
