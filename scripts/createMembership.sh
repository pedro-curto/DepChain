#!/bin/bash
# blockchain members
users=("pedrocurto" "pedroribeiro" "rodrigogreedy" "dybizantino")
KEYS_DIR="../member/membership"
MEMBERSHIP_FILE="$KEYS_DIR/membership.txt"
BASE_PORT=5001
ADDRESS="localhost"

# creates keys dir and clears membership file
mkdir -p $KEYS_DIR
> "$MEMBERSHIP_FILE"

for i in "${!users[@]}"; do
    user=${users[$i]}
    port=$((BASE_PORT + i))
    echo "Generating keys for user: ${user}"
    mkdir -p "$KEYS_DIR/$user"

    # generate privkey
    openssl genpkey -algorithm RSA -out "$KEYS_DIR/$user/${user}.privkey" -pkeyopt rsa_keygen_bits:2048
    if [ $? -ne 0 ]; then
        echo "Error: Failed to generate private key for ${user}"
        exit 1
    fi
    echo "Generated private key for ${user}"

    # generate pubkey from privkey
    openssl rsa -pubout -in "$KEYS_DIR/$user/${user}.privkey" -out "$KEYS_DIR/$user/${user}.pubkey"
    if [ $? -ne 0 ]; then
        echo "Error: Failed to generate public key for ${user}"
        exit 1
    fi
    echo "Generated public key for ${user}"

    echo "Keys for ${user} are stored in $KEYS_DIR/${user}/"
    echo "  - ${user}.privkey"
    echo "  - ${user}.pubkey"

    # append membership entry: memberName,address,port,publicKeyPath
    echo "${user},${ADDRESS},${port},$KEYS_DIR/${user}/${user}.pubkey" >> "$MEMBERSHIP_FILE"
done

echo "All keys generated successfully."
