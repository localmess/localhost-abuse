import socket
import struct
import signal
import sys

TURN_PORT = 10001
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.bind(("0.0.0.0", TURN_PORT))
print(f"Listening on UDP {TURN_PORT}...")

def is_stun(data):
    return len(data) >= 20 and data[4:8] == b'\x21\x12\xa4\x42'

def parse_username(data):
    i = 20
    while i + 4 <= len(data):
        attr_type, attr_len = struct.unpack("!HH", data[i:i+4])
        val = data[i+4:i+4+attr_len]
        if attr_type == 0x0006:  # USERNAME
            return val.decode(errors='ignore')
        i += 4 + ((attr_len + 3) // 4) * 4
    return None

def send_binding_success(sock, addr, transaction_id):
    # Minimal Binding Success Response
    header = struct.pack("!HHI", 0x0101, 0, 0x2112A442) + transaction_id
    sock.sendto(header, addr)

def send_allocate_unauthorized(sock, addr, transaction_id):
    # Send a proper 401 Unauthorized for Allocate Requests
    error_code = 401
    reason = b"Unauthorized"
    realm = b"webrtc.local"
    nonce = b"test_nonce"

    error_class = error_code // 100
    error_number = error_code % 100
    error_value = struct.pack("!HBB", 0, error_class, error_number) + reason
    error_value = error_value.ljust((len(error_value) + 3) // 4 * 4, b'\x00')
    attr_error = struct.pack("!HH", 0x0009, len(error_value)) + error_value

    attr_realm = struct.pack("!HH", 0x0014, len(realm)) + realm
    if len(realm) % 4 != 0:
        attr_realm += b'\x00' * (4 - len(realm) % 4)

    attr_nonce = struct.pack("!HH", 0x0015, len(nonce)) + nonce
    if len(nonce) % 4 != 0:
        attr_nonce += b'\x00' * (4 - len(nonce) % 4)

    attributes = attr_error + attr_realm + attr_nonce
    msg_len = len(attributes)
    header = struct.pack("!HHI", 0x0113, msg_len, 0x2112A442) + transaction_id
    response = header + attributes

    sock.sendto(response, addr)

def send_allocate_forbidden(sock, addr, transaction_id):
    """
    Send a 403 Forbidden error response to a TURN Allocate request.
    """
    error_code = 403
    reason = b"Forbidden"

    # Error-Code attribute (type 0x0009)
    error_class = error_code // 100
    error_number = error_code % 100
    error_value = struct.pack("!HBB", 0, error_class, error_number) + reason
    error_value_padded = error_value.ljust((len(error_value) + 3) // 4 * 4, b'\x00')
    attr_error_code = struct.pack("!HH", 0x0009, len(error_value)) + error_value_padded

    attributes = attr_error_code
    msg_length = len(attributes)

    # STUN header: Message Type = 0x0113 (Allocate Error Response)
    msg_type = 0x0113
    magic_cookie = 0x2112A442
    header = struct.pack("!HHI", msg_type, msg_length, magic_cookie) + transaction_id

    stun_message = header + attributes
    sock.sendto(stun_message, addr)

    print(f"→ Sent 403 Forbidden Allocate Error Response to {addr}")



def handle_exit(sig, frame):
    print("\nExiting gracefully.")
    sys.exit(0)

signal.signal(signal.SIGINT, handle_exit)

while True:
    data, addr = sock.recvfrom(4096)
    if is_stun(data):
        msg_type = struct.unpack("!H", data[0:2])[0]
        transaction_id = data[8:20]
        username = parse_username(data)
        
        if username:
            print(f"From {addr}: USERNAME = {username}")
            send_allocate_forbidden(sock, addr, transaction_id)
            sys.exit(0)

        if msg_type == 0x0001:
            print("Received Binding Request")
            send_binding_success(sock, addr, transaction_id)

        elif msg_type == 0x0003:
            print("Received Allocate Request")
            send_allocate_unauthorized(sock, addr, transaction_id)

