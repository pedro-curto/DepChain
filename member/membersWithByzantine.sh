#!/bin/bash

# we must provide exactly one argument
if [ "$#" -ne 1 ]; then
  echo "Usage: $0 <byzantine_flag (0,1,2,3,4,5,6)>"
  echo "0: No Byzantine behavior"
  echo "1: No Answer"
  echo "2: Wrong State"
  echo "3: Fake Signature"
  echo "4: Replay Signature (don't use!)"
  echo "5: Byzantine behavior 5"
  echo "6: Byzantine behavior 6"
  echo "7: Byzantine behavior 7"
  exit 1
fi

BYZANTINE_FLAG="$1"

MEMBERSHIP_FILE="membership/membership.txt"
pids=()
mkdir -p logs

cleanup() {
  echo "Cleaning up..."
  # kill servers launched by this script
  for pid in "${pids[@]}"
  do
    kill $pid 2>/dev/null
  done
}

trap cleanup SIGINT

# Count total number of valid members
total_members=0
while IFS=',' read -r memberName address port pubKeyPath
do
  # skip empty lines or lines starting with a comment
  if [ -z "$memberName" ] || echo "$memberName" | grep -q '^#'; then
    continue
  fi
  total_members=$((total_members + 1))
done < "$MEMBERSHIP_FILE"

# Start members, with the last one being byzantine
current_member=0
while IFS=',' read -r memberName address port pubKeyPath
do
  # skip empty lines or lines starting with a comment
  if [ -z "$memberName" ] || echo "$memberName" | grep -q '^#'; then
    continue
  fi

  current_member=$((current_member + 1))

  # Set byzantine flag to 1 for the last member
  byzantine_flag=0
  if [ $current_member -eq $total_members ]; then
    byzantine_flag=$BYZANTINE_FLAG
    echo "Starting BYZANTINE server for member: $memberName on port: $port"
  else
    echo "Starting server for member: $memberName on port: $port"
  fi

  # launch servers
  mvn compile exec:java -Dexec.args="$port $address $memberName $byzantine_flag" > "logs/server_${port}_${memberName}.log" 2>&1 &
  pids+=($!)
done < "$MEMBERSHIP_FILE"

wait
cleanup