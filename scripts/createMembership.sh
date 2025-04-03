#!/bin/bash
# blockchain members
users=("pedroribeiro" "pedrocurto" "rodrigogreedy" "dybizantino")
client=("paulo" "joao" "pedro")
MEMBERS_DIR="../member/membership"
CLIENTS_DIR="../client/membership"
MEMBERSHIP_FILE="$MEMBERS_DIR/membership.txt"
MEMBERS_LEADER_FILE="$MEMBERS_DIR/leader.txt"
MEMBERSHIP_CLIENT_FILE="$CLIENTS_DIR/membership.txt"
CLIENT_LEADER_FILE="$CLIENTS_DIR/leader.txt"
CLIENT_FILE="$MEMBERS_DIR/client.txt"
BASE_CLIENT_PORT=2000
BASE_PORT=5001
ADDRESS="localhost"

# creates dirs
mkdir -p "$MEMBERS_DIR"
mkdir -p "$CLIENTS_DIR"
# creates files if they don't exist and clears them
touch "$MEMBERSHIP_FILE"
touch "$MEMBERSHIP_CLIENT_FILE"
touch "$CLIENT_LEADER_FILE"
touch "$MEMBERS_LEADER_FILE"
touch "$CLIENT_FILE"
> "$MEMBERSHIP_FILE"
> "$MEMBERSHIP_CLIENT_FILE"
> "$CLIENT_LEADER_FILE"
> "$MEMBERS_LEADER_FILE"
> "$CLIENT_FILE"

# --- MEMBERS AND LEADER KEYS ---
for i in "${!users[@]}"; do
    user=${users[$i]}
    port=$((BASE_PORT + i))
    echo "Generating keys for user: ${user}"
    mkdir -p "$MEMBERS_DIR/$user"

    # generate privkey
    openssl genpkey -algorithm RSA -out "$MEMBERS_DIR/$user/${user}.privkey" -pkeyopt rsa_keygen_bits:2048
    if [ $? -ne 0 ]; then
        echo "Error: Failed to generate private key for ${user}"
        exit 1
    fi
    echo "Generated private key for ${user}"

    # generate pubkey from privkey
    openssl rsa -pubout -in "$MEMBERS_DIR/$user/${user}.privkey" -out "$MEMBERS_DIR/$user/${user}.pubkey"
    if [ $? -ne 0 ]; then
        echo "Error: Failed to generate public key for ${user}"
        exit 1
    fi
    echo "Generated public key for ${user}"

    echo "Keys for ${user} are stored in $MEMBERS_DIR/${user}/"
    echo "  - ${user}.privkey"
    echo "  - ${user}.pubkey"

    # append membership entry: memberName,address,port,publicKeyPath
    echo "${user},${ADDRESS},${port},$MEMBERS_DIR/${user}/${user}.pubkey" >> "$MEMBERSHIP_FILE"
    echo "${user},${ADDRESS},${port},$MEMBERS_DIR/${user}/${user}.pubkey" >> "$MEMBERSHIP_CLIENT_FILE"

    # create client leader file and places leader's public key for client to access
    # (only the first member, if i == 0)
    if [ $i -eq 0 ]; then
      echo "${user},${ADDRESS},${port}" >> "$CLIENT_LEADER_FILE"
      echo "${user},${ADDRESS},${port}" >> "$MEMBERS_LEADER_FILE"
      mkdir -p "$CLIENTS_DIR/$user"
      cp "$MEMBERS_DIR/$user/${user}.pubkey" "$CLIENTS_DIR/$user/${user}.pubkey"
    fi
done

# --- GENESIS FILE GENERATION (ALONG WITH CLIENT KEYS) ---

GENESIS_FILE="../genesis-file.json"

cat > "$GENESIS_FILE" << EOF
{
  "hash": "",
  "previous_hash": "",
  "transactions": [],
  "state": {
    "accounts": [],
    "contract": {
      "address": "0x1234567891234567891234567891234567891234",
      "deployment_bytecode": "",
      "runtime_bytecode": "",
      "owner": ""
    }
  }
}
EOF

# generate client keys and add account entries to genesis file accordingly
accounts_json=""
for i in "${!client[@]}"; do
    user=${client[$i]}
    echo "Generating keys for user: ${user}"
    mkdir -p "$CLIENTS_DIR/$user"
    mkdir -p "$MEMBERS_DIR/$user"

    # privkey
    openssl genpkey -algorithm RSA -out "$CLIENTS_DIR/$user/${user}.privkey" -pkeyopt rsa_keygen_bits:2048
    if [ $? -ne 0 ]; then
        echo "Error: Failed to generate private key for ${user}"
        exit 1
    fi
    echo "Generated private key for ${user}"

    # pubkey from privkey
    openssl rsa -pubout -in "$CLIENTS_DIR/$user/${user}.privkey" -out "$CLIENTS_DIR/$user/${user}.pubkey"
    if [ $? -ne 0 ]; then
        echo "Error: Failed to generate public key for ${user}"
        exit 1
    fi
    echo "Generated public key for ${user}"

    # copy client keys to members dir
    cp "$CLIENTS_DIR/$user/${user}.pubkey" "$MEMBERS_DIR/$user/${user}.pubkey"
    cp "$CLIENTS_DIR/$user/${user}.privkey" "$MEMBERS_DIR/$user/${user}.privkey"

    # create a client.txt at the membership directory similar to the membership
    echo "${user},${ADDRESS},$((BASE_CLIENT_PORT + i)),$MEMBERS_DIR/${user}/${user}.pubkey" >> "$CLIENT_FILE"

    # generates hash of pubkey for genesis file
    pk_hash=$(python3 ../generate-pk-hash.py "$CLIENTS_DIR/$user/${user}.pubkey" | grep "SHA-256 Hash of Public Key:" | cut -d' ' -f6)
    echo "Generated hash for ${user}'s public key: ${pk_hash}"
    # store user pubkey at the membership directory under the hash
    mkdir -p "$MEMBERS_DIR/$pk_hash"
    cp "$CLIENTS_DIR/$user/${user}.pubkey" "$MEMBERS_DIR/$pk_hash/$pk_hash.pubkey"
    echo "Stored ${pk_hash}'s public key at $MEMBERS_DIR/$pk_hash/$pk_hash.pubkey"

    # we assume first client is the owner
    if [ $i -eq 0 ]; then
            tmp_file=$(mktemp)
            jq --arg owner "$pk_hash" '.state.contract.owner = $owner' "$GENESIS_FILE" > "$tmp_file"
            mv "$tmp_file" "$GENESIS_FILE"
    fi

    # add account entry to genesis file
    if [ $i -gt 0 ]; then
        accounts_json+=","
    fi
    accounts_json+=$(cat << EOF
      {
        "address": "${pk_hash}",
        "name": "${user}",
        "balance": 100
      }
EOF
)
done

# add all accounts to genesis file
tmp_file=$(mktemp)
jq --argjson accounts "[$accounts_json]" '.state.accounts = $accounts' "$GENESIS_FILE" > "$tmp_file"
mv "$tmp_file" "$GENESIS_FILE"

# generate hash for genesis block
genesis_hash=$(python3 ../genesis-hash.py "$GENESIS_FILE" | grep "Genesis Block Hash (Base64):" | cut -d' ' -f5)
tmp_file=$(mktemp)
jq --arg hash "$genesis_hash" '.hash = $hash' "$GENESIS_FILE" > "$tmp_file"
mv "$tmp_file" "$GENESIS_FILE"

# read bytecodes from file
deployment_bytecode=$(cat deploymentBytecode.txt)
runtime_bytecode=$(cat runtimeBytecode.txt)
tmp_file=$(mktemp)
jq --arg deployment_bytecode "$deployment_bytecode" '.state.contract.deployment_bytecode = $deployment_bytecode' "$GENESIS_FILE" > "$tmp_file"
mv "$tmp_file" "$GENESIS_FILE"

tmp_file=$(mktemp)
jq --arg runtime_bytecode "$runtime_bytecode" '.state.contract.runtime_bytecode = $runtime_bytecode' "$GENESIS_FILE" > "$tmp_file"
mv "$tmp_file" "$GENESIS_FILE"


echo "All keys generated and genesis file created successfully."