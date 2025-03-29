import hashlib
import base64
import re
import sys

def generate_hash(pk_file):
    with open(pk_file, 'r') as f:
        public_key_data = f.read()
    key_data_clean = re.sub(r"-----.*?-----", "", public_key_data).strip()
    key_bytes = base64.b64decode(key_data_clean)
    # truncates to 20 bytes
    hash_object = hashlib.sha256(key_bytes).digest()[:20]
    hash_hex = "0x" + hash_object.hex()
    #hash_base64 = base64.b64encode(hash_object).decode()
    return hash_hex

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python3 generate-pk-hash.py <filename>")
        sys.exit(1)
    file_path = sys.argv[1]
    hash_result = generate_hash(file_path)
    print(f"SHA-256 Hash of Public Key: {hash_result}")


