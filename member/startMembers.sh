#!/bin/bash

MEMBERSHIP_FILE="membership/membership.txt"
SKIP_FIRST=false
pids=()
mkdir -p logs

# args
while [[ $# -gt 0 ]]; do
  case $1 in
    --skip-first)
      SKIP_FIRST=true
      shift
      ;;
    *)
      shift
      ;;
  esac
done

cleanup() {
  echo "Cleaning up..."
  # kill servers launched by this script
  for pid in "${pids[@]}"
  do
    kill $pid 2>/dev/null
  done
}

trap cleanup SIGINT

members=0

# read each line from the membership file
while IFS=',' read -r memberName address port pubKeyPath
do
  # skip empty lines or lines starting with a comment
  if [ -z "$memberName" ] || echo "$memberName" | grep -q '^#'; then
    continue
  fi

  valid_members=$((valid_members + 1))
  if [ "$valid_members" -eq 1 ] && [ "$SKIP_FIRST" = true ]; then
    echo "Skipping first member: $memberName"
    continue
  fi

  echo "Starting server for member: $memberName on port: $port"

  # launch servers
  mvn compile exec:java -Dexec.args="$port $address $memberName 0" > "logs/server_${port}_${memberName}.log" 2>&1 &
  pids+=($!)
done < "$MEMBERSHIP_FILE"

wait
cleanup