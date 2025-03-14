# DepChain

## To Do
- Deal with messages with weird characters (e.g., "º")
- Add remaining tests for Byzantine behaviour

## Building the Project
From the root directory of the project, run the following command:
```bash
mvn clean install
```
If you want to ignore the tests, run the following command instead:
```bash
mvn clean install -DskipTests
```

## Running the Project
### Configuration

First of all, you need to generate the configuration file (with the membership). To do that, run the following command:
```bash
cd scripts
./createMembership.sh
```
### Members

To run the members:
```bash
cd member
./startMembers.sh
```

You can check each member's log at the `member/logs` directory.

### Client

To run a client (it's **very important** to pass it the client name paulo specifically, since it's the one that we have private and public keys for):
```bash
cd client
mvn compile exec:java -Dexec.args="2000 paulo"
```

Now, at the client, you can send messages to the server:
```bash
foo 
bar
```

## Testing the Project
### Automated Tests
Go to the member directory:
```bash
cd member
```

To run all tests:
```bash
mvn test
```

To run a specific test (example with spam Byzantine):
```bash
mvn test -Dtest=ByzantineIgnoreMessagesTest#testConsensusWithSpamByzantine
```

The tests that we have are all contained within `ByzantineBehaviourTest`:
- `testConsensusWithIgnoringByzantine`: tests the consensus algorithm with a Byzantine member that ignores all messages
- `testConsensusWithSpamByzantine`: tests the consensus algorithm with a Byzantine member that sends a lot of equal messages (READs, STATEs, etc...)
- `testConsensusWithFakeSignature`: tests the consensus algorithm with a Byzantine member that sends messages with fake signatures. We assert if the fake signature is detected, and if the consensus still works

### Manual Tests

To manually run tests and test the program, use this command to launch all members (no Byzantine):
```bash
./startMembers.sh
```

Or this command if you want a Byzantine member to be present:
```bash
./membersWithByzatine.sh <byzantineBehaviour
```

Where `<byzantineBehaviour>` can be one of the following:
- '0': normal
- '1': ignore messages
- '2': spam messages
- '3': fake signature

Afterwards (very important to be **paulo**, since it's the one that we have private and public keys for):
```bash
cd client
mvn compile exec:java -Dexec.args="2000 paulo"
```

Send messages:
```bash
foo
bar
```

Check the logs at the `member/logs` directory.