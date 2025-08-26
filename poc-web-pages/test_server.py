from flask import Flask, request, jsonify, make_response
from flask_cors import CORS
from flask_sock import Sock

import asyncio
import json
import time
from aiortc import RTCPeerConnection, RTCSessionDescription

import threading

app1 = Flask('http_app')
app2 = Flask('https_app')
cors1 = CORS(app1, resources={
    r"/send": {
        "origins": "*",
        "supports_credentials": True
    }
})
sock = Sock(app1)
cors2 = CORS(app2, resources={
    r"/send": {
        "origins": "*",
        "supports_credentials": True
    }
})

# --- HTTP Routes ---
@app1.after_request
def add_pna_headers1(response):
    origin = request.headers.get("Origin")
    print("in apply cors")
    if origin:
        response.headers["Access-Control-Allow-Origin"] = origin
        response.headers["Access-Control-Allow-Headers"] = "Content-Type"
        response.headers["Access-Control-Allow-Methods"] = "GET, POST, OPTIONS"
        response.headers["Access-Control-Allow-Credentials"] = "true"
        # This is crucial for Chrome PNA
        response.headers["Access-Control-Allow-Private-Network"] = "true"
    return response

@app1.route('/send', methods=['POST', 'OPTIONS'])
def receive_http():
    """Handles HTTP requests."""
    if request.method == 'OPTIONS':
        print("received options")
        return make_response('', 204)  # Respond to CORS preflight request
    data = request.json
    print(f"Received HTTP request: {data}")
    return jsonify({"status": "success", "received": data}), 200

@app1.route('/send', methods=['GET'])
def receive_http_get():
    """Handles HTTP GET requests."""
    number = request.args.get('number')
    print(number)
    return jsonify({"status": "success", "received": number}), 200

@app1.route('/', methods=['GET'])
def index():
    return jsonify({"message": "Hello from HTTP!"})

@app1.route('/echo', methods=['POST'])
def echo():
    data = request.get_json()
    return jsonify({"you_sent": data})

# --- HTTPS Routes ---
@app2.after_request
def add_pna_headers2(response):
    origin = request.headers.get("Origin")
    print("in apply cors")
    if origin:
        response.headers["Access-Control-Allow-Origin"] = origin
        response.headers["Access-Control-Allow-Headers"] = "Content-Type"
        response.headers["Access-Control-Allow-Methods"] = "GET, POST, OPTIONS"
        response.headers["Access-Control-Allow-Credentials"] = "true"
        # This is crucial for Chrome PNA
        response.headers["Access-Control-Allow-Private-Network"] = "true"
    return response

@app2.route('/send', methods=['POST', 'OPTIONS'])
def receive_https():
    """Handles HTTP requests."""
    if request.method == 'OPTIONS':
        print("received options")
        return make_response('', 204)  # Respond to CORS preflight request
    data = request.json
    print(f"Received HTTP request: {data}")
    return jsonify({"status": "success", "received": data}), 200

@app2.route('/send', methods=['GET'])
def receive_https_get():
    """Handles HTTPS GET requests."""
    number = request.args.get('number')
    print(number)
    return jsonify({"status": "success", "received": number}), 200

@app2.route("/", methods=["GET"])
def home():
    return "✅ Server is running and reachable at https://xxxyyy.com:5000"

# --- WebSocket Route using flask_sock ---
@sock.route('/ws')
def websocket(ws):
    while True:
        try:
            data = ws.receive()
            if data is None:
                break  # client disconnected
            print(data)
            ws.send(f"Echo from server: {data}")
        except Exception as e:
            print(f"WebSocket error: {e}")
            break

def run_http():
    app1.run(host='0.0.0.0', port=5000)

def run_https():
    app2.run(ssl_context=('cert.pem', 'key.pem'), host='0.0.0.0', port=5001)


if __name__ == '__main__':
    try:
        t1 = threading.Thread(target=run_http, daemon=True)
        t2 = threading.Thread(target=run_https, daemon=True)
        # t3 = threading.Thread(target=run_webRTC, daemon=True)
        
        print("HTTP/HTTPS servers starting...")
        t1.start()
        time.sleep(3)
        t2.start()
        # time.sleep(3)
        # print("webRTC starting...")
        # t3.start()
        while True:
            pass  # Keep main thread alive

    except KeyboardInterrupt:
        print("\nShutting down.")
