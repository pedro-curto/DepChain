#!/bin/bash
MEMBERS_DIR="../member/membership"
CLIENTS_DIR="../client/membership"

# check if the KEYS_DIR exists and remove all contents inside it
if [ -d "$MEMBERS_DIR" ] && [ -d "$CLIENTS_DIR" ]; then
    echo "Deleting everything inside $MEMBERS_DIR and $CLIENTS_DIR..."
    rm -rf "$MEMBERS_DIR"/*
    rm -rf "$CLIENTS_DIR"/*
else
    echo "Directory $KEYS_DIR does not exist."
fi

echo "Cleanup completed."