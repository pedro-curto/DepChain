#!/bin/bash

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
    byzantine_flag=1
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