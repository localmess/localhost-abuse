import json
import glob
import re
import csv
from tld import get_fld
from urllib.parse import urlparse
from collections import defaultdict

input_folder = "new_york_data/data\\"
# input_folder = "frankfurt_data/data\\"
results_folder = "results/"
version = "_new_york_100k"
# version = "_frankfurt_100k"

SCRIPT_URL_CUTOFF = 512
INCLUDE_SDP_LOCAL = False

files = glob.glob(input_folder + '*.json', recursive = False)

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

def analyze_request(request : dict):
    """
    Analyze a request to check if it is a request to localhost and return its details if so.
    Returns request_result = {
                            "protocol": if the request is a WebSocket or HTTP request,
                            "port_num": port number found,
                            "script_domain": domain of script initiating request,
                            }
    """
    url = request.get("url", "")
    initiators = request.get("initiators", [])
    initial_initiator = initiators[0] if len(initiators) else "Unknown"

    is_localhost, _search_type, port = is_localhost_request(url)

    if not is_localhost:
        return None

    initiator_domain = get_fld(initial_initiator, fail_silently=True)
    protocol = request.get("type")

    request_result = {
                    "protocol": "WebSocket" if protocol == "WebSocket" else "HTTP",
                    "port_num": port,
                    "script_domain": initiator_domain,
                    }
    return request_result

def analyze_request_data(request_data : list):
    """
    Analyze the request data for any localhost requests.
    Returns a dict for WebSockets and HTTP requests with ports and their script domains.
    """
    HTTP_results = defaultdict(set)
    websocket_results = defaultdict(set)

    for request in request_data:
        request_result = analyze_request(request)
        if not request_result:
            continue

        port = request_result["port"]
        script_domain = request_result["script_domain"]
        if request_result["protocol"] == "HTTP":
            HTTP_results[port].add(script_domain)
        else:
            websocket_results[port].add(script_domain)

    return HTTP_results, websocket_results

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

def analyze_webRTC_call(webRTC_call : dict):
    """
    Analyze a request call for setting up a localhost connection.
    Returns a list of ports and the initiator domain.
    """
    call_type = webRTC_call.get("type", "")
    if  call_type == "RTCPeerConnection" or call_type == "":
        return [], ""
    if not INCLUDE_SDP_LOCAL and call_type == "SDP-Local":
        return [], ""

    search_type = webRTC_call.get("localhost", "None")
    if search_type == "None":
        return [], ""

    candidate = webRTC_call.get("candidate", "")
    all_ports = set(extract_ports(candidate))

    initiator = webRTC_call.get("source", "Unknown")
    initiator_domain = get_fld(initiator, fail_silently=True)

    return all_ports, initiator_domain

def analyze_webRTC_data(webRTC_data : list):
    """
    Analyze the WebRTC data for any localhost calls.
    Returns a list of call results.
    """
    webRTC_results = defaultdict(set)

    for call in webRTC_data:
        all_ports, initiator_domain = analyze_webRTC_call(call)

        for port in all_ports:
            webRTC_results[port].add(initiator_domain)

    return webRTC_results

def analyze_file(file_name : str):
    """
    Analyze the file for any localhost requests or WebRTC calls.
    Returns two lists for both request results and WebRTC results.
    """
    # Load the data from the .json file
    f = open(file_name, encoding="utf-8") 
    file_data = json.load(f)
    f.close()

    data = file_data.get("data", None)
    if not data:
        print(f"Error! No Data for {file_name}")
        return None, None

    request_data = data.get("requests", [])
    HTTP_results, websocket_results = analyze_request_data(request_data)

    webRTC_data = data.get("webRTC", [])
    webRTC_results = analyze_webRTC_data(webRTC_data)

    return HTTP_results, websocket_results, webRTC_results

def main():
    crawl_count = 0
    HTTP_port_domain_results = defaultdict(set)
    websocket_port_domain_results = defaultdict(set)
    webRTC_port_domain_results = defaultdict(set)

    for file in files:
        if file == input_folder + "metadata.json":
            continue

        crawl_count += 1
        if not crawl_count % 100:
            print(crawl_count)

        HTTP_results, websocket_results, webRTC_results = analyze_file(file)

        for port, domains in HTTP_results.items():
            HTTP_port_domain_results[port].update(domains)
        for port, domains in websocket_results.items():
            websocket_port_domain_results[port].update(domains)
        for port, domains in webRTC_results.items():
            webRTC_port_domain_results[port].update(domains)

    results = {
        "HTTP_port_domain_results": {port: list(domains) for port, domains in HTTP_port_domain_results.items()},
        "websocket_port_domain_results": {port: list(domains) for port, domains in websocket_port_domain_results.items()},
        "webRTC_port_domain_results": {port: list(domains) for port, domains in webRTC_port_domain_results.items()}
    }

    with open(results_folder + "port_results" + version + ".json", 'w', encoding='utf-8') as fp:
        json.dump(results, fp, default=tuple, sort_keys=False, indent=4)
        fp.close()

if __name__ == "__main__":
    main()