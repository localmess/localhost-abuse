import socket
import struct

def decode_name(data, offset):
    labels = []
    while True:
        length = data[offset]
        if length == 0:
            offset += 1
            break
        # Handle name compression
        elif length & 0xC0 == 0xC0:
            pointer = struct.unpack("!H", data[offset:offset+2])[0] & 0x3FFF
            label, _ = decode_name(data, pointer)
            labels.append(label)
            offset += 2
            break
        else:
            offset += 1
            labels.append(data[offset:offset+length].decode())
            offset += length
    return ".".join(labels), offset

def parse_dns(data):
    transaction_id = data[0:2]
    flags = struct.unpack("!H", data[2:4])[0]
    qdcount = struct.unpack("!H", data[4:6])[0]
    ancount = struct.unpack("!H", data[6:8])[0]
    nscount = struct.unpack("!H", data[8:10])[0]
    arcount = struct.unpack("!H", data[10:12])[0]

    offset = 12
    # Questions
    for _ in range(qdcount):
        qname, offset = decode_name(data, offset)
        qtype, qclass = struct.unpack("!HH", data[offset:offset+4])
        offset += 4
        print(f"🔍 Query: {qname} (Type: {qtype}, Class: {qclass})")

    # Answers
    for _ in range(ancount):
        name, offset = decode_name(data, offset)
        atype, aclass, ttl, rdlength = struct.unpack("!HHIH", data[offset:offset+10])
        offset += 10
        rdata = data[offset:offset+rdlength]
        offset += rdlength

        if atype == 1:  # A record
            ip = ".".join(map(str, rdata))
            print(f"📦 Response: {name} has A record {ip}")
        elif atype == 28:  # AAAA
            ip6 = ":".join([rdata[i:i+2].hex() for i in range(0, 16, 2)])
            print(f"📦 Response: {name} has AAAA record {ip6}")
        elif atype == 12:  # PTR
            ptr_name, _ = decode_name(rdata, 0)
            print(f"📦 Response: {name} is PTR to {ptr_name}")
        else:
            print(f"📦 Response: {name} Type {atype} (not decoded)")

def listen_mdns():
    MCAST_GRP = '224.0.0.251'
    MCAST_PORT = 5353

    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.bind(('', MCAST_PORT))
    mreq = struct.pack("=4sl", socket.inet_aton(MCAST_GRP), socket.INADDR_ANY)
    sock.setsockopt(socket.IPPROTO_IP, socket.IP_ADD_MEMBERSHIP, mreq)

    print("🔎 Listening for mDNS queries and responses...")

    while True:
        data, addr = sock.recvfrom(2048)
        print(f"\n📡 Packet from {addr[0]}")
        try:
            parse_dns(data)
        except Exception as e:
            print(f"❌ Failed to parse packet: {e}")

if __name__ == "__main__":
    listen_mdns()
