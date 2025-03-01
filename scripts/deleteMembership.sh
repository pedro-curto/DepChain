#!/bin/bash

KEYS_DIR="../member/membership"

# check if the KEYS_DIR exists and remove all contents inside it
if [ -d "$KEYS_DIR" ]; then
    echo "Deleting everything inside $KEYS_DIR..."
    rm -rf "$KEYS_DIR"/*
else
    echo "Directory $KEYS_DIR does not exist."
fi

echo "Cleanup completed."


