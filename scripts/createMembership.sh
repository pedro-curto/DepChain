#!/bin/bash
# blockchain members
users=("pedroribeiro" "pedrocurto" "rodrigogreedy" "dybizantino")
client=("paulo")
MEMBERS_DIR="../member/membership"
CLIENTS_DIR="../client/membership"
MEMBERSHIP_FILE="$MEMBERS_DIR/membership.txt"
MEMBERS_LEADER_FILE="$MEMBERS_DIR/leader.txt"
CLIENT_LEADER_FILE="$CLIENTS_DIR/leader.txt"
CLIENT_FILE="$MEMBERS_DIR/client.txt"
CLIENT_PORT=2000
BASE_PORT=5001
ADDRESS="localhost"

# creates dirs
mkdir -p "$MEMBERS_DIR"
mkdir -p "$CLIENTS_DIR"
# creates files if they don't exist and clears them
touch "$MEMBERSHIP_FILE"
touch "$CLIENT_LEADER_FILE"
touch "$MEMBERS_LEADER_FILE"
touch "$CLIENT_FILE"
> "$MEMBERSHIP_FILE"
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

    # create client leader file and places leader's public key for client to access
    # (only the first member, if i == 0)
    if [ $i -eq 0 ]; then
      echo "${user},${ADDRESS},${port}" >> "$CLIENT_LEADER_FILE"
      echo "${user},${ADDRESS},${port}" >> "$MEMBERS_LEADER_FILE"
      mkdir -p "$CLIENTS_DIR/$user"
      cp "$MEMBERS_DIR/$user/${user}.pubkey" "$CLIENTS_DIR/$user/${user}.pubkey"
    fi
done

# --- CLIENT KEYS ---
# generate private/public key for client
user=${client[0]}
echo "Generating keys for user: ${user}"
mkdir -p "$CLIENTS_DIR/$user"
mkdir -p "$MEMBERS_DIR/$user"

# generate privkey
openssl genpkey -algorithm RSA -out "$CLIENTS_DIR/$user/${user}.privkey" -pkeyopt rsa_keygen_bits:2048
if [ $? -ne 0 ]; then
    echo "Error: Failed to generate private key for ${user}"
    exit 1
fi
echo "Generated private key for ${user}"

# generate pubkey from privkey
openssl rsa -pubout -in "$CLIENTS_DIR/$user/${user}.privkey" -out "$CLIENTS_DIR/$user/${user}.pubkey"
if [ $? -ne 0 ]; then
    echo "Error: Failed to generate public key for ${user}"
    exit 1
fi
echo "Generated public key for ${user}"

# copy pubkey to members dir
cp "$CLIENTS_DIR/$user/${user}.pubkey" "$MEMBERS_DIR/$user/${user}.pubkey"

# create a client.txt at the membership directory similar to the membership
echo "${user},${ADDRESS},${CLIENT_PORT},$MEMBERS_DIR/${user}/${user}.pubkey" >> "$CLIENT_FILE"

echo "All keys generated successfully."
