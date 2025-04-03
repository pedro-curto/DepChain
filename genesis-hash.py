import json
import hashlib
import base64
import sys

def generate_hash(genesis_file):
    with open(genesis_file, 'r') as f:
        genesis = json.load(f)

    to_hash = (
            str(genesis.get('previous_hash', '')) +
            json.dumps(genesis.get('transactions', ''), sort_keys=True) +
            json.dumps(genesis.get('state', ''), sort_keys=True)
    )

    hash_bytes = hashlib.sha256(to_hash.encode()).digest()
    hash_base64 = base64.b64encode(hash_bytes).decode()
    print("Genesis Block Hash (Base64):", hash_base64)

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python3 generate-hash.py <filename>")
        sys.exit(1)
    file_path = sys.argv[1]
    generate_hash(file_path)
