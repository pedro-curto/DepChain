# DepChain

## Requirements
- This project uses `Java 21`, `Maven`, `Python` (scripts for hashing public key and genesis file hash) and `jq` (for JSON manipulation).

## Configuration

**First of all, you need to generate the configuration files** (with the membership), to generate the membership files and the genesis file.
For this, we assume you have the `jq` command available (we use it for JSON manipulation).

To generate the configuration files:
```bash
cd scripts
./createMembership.sh
```

Without this, `mvn clean install` will fail because it runs tests that require the configuration files.
However, `mvn clean install -DskipTests` will work.
This also generates the `genesis-file.json` that will be necessary to generate the genesis block.


## Building the Project
From the root directory of the project, run the following command:
```bash
mvn clean install
```
This can take some time (around 3 minutes) since it will run the tests.

If you want to ignore the tests, run the following command instead:
```bash
mvn clean install -DskipTests
```

## Running the Project

If you just wish to test the project, skip to the [Testing the Project](#testing-the-project) section.

### Configuration

First of all, **you need to generate the configuration file** (with the membership). To do that, run the following command:
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

To run a client (it's **very important** to pass it a name for a client that exists in the `createMembership.sh`'s 
client list, like paulo, joao or pedro, since they're the ones that we generate private and public keys for).
You can add more clients to the `createMembership.sh` script, but don't forget to run the script to regenerate the configuration files.
```bash
cd client
mvn compile exec:java -Dexec.args="2000 paulo 0"
```

The third argument is the client's byzantine behaviour. It can be:
- 0: no byzantine behaviour
- 1: fakes signatures
- 2: performs replay attacks

Now, at the client, you can send these types of messages to the server (CoinType is either `ISTCoin` or `DepCoin`):
- `QUIT | EXIT`: exits the client
- `HELP`: prints the help info
- `<CoinType> BALANCE <address>`
- `<CoinType> TRANSFER <address> <amount>"`
- `ISTCoin TRANSFER_FROM <owner> <to> <amount>"`
- `ISTCoin APPROVE <spender> <amount>"`
- `ISTCoin ALLOWANCE <owner> <spender>"`
- `ISTCoin BLACKLIST <address>"`
- `ISTCoin UNBLACKLIST <address>"`
- `ISTCoin ISBLACKLISTED <address>"`

Note that the majority of operations are only available for `ISTCoin`.

## Testing the Project

### Automated Tests
**Member Tests**

Go to the member directory:
```bash
cd member
```

To run all tests:
```bash
mvn test
```

To run a specific test (example with the regular test):
```bash
mvn test -Dtest=RegularTest#testBlockChainNormalBehaviour
```

The tests that we have are:

**Byzantine Client**
- `testWithWrongSignatureClient` (in `ByzantineClientTest`): tests the system with a Byzantine client that sends a fake signature
- `testWithReplayAttackClient` (in `ByzantineClientTest`): tests the system with a Byzantine client that waits for a block to be decided, grabs a transaction, and attempts to send a request to replay the transaction

**Byzantine Member**
- `testConsensusWithIgnoringByzantine` (in `ByzantineClientTest`): tests the system with a Byzantine member that ignores all messages
- `testConsensusWithSpamByzantine` (in `SpamMemberTest`): tests the system with a Byzantine member that sends a lot of equal messages (READs, STATEs, etc...)
- `testConsensusWithMemberFakeSignature` (in `FakeSignatureMemberTest`): tests the system with a Byzantine member that sends messages with fake signatures. We assert if the fake signature is detected, if the consensus still works and the transactions are decided correctly
- `testConsensusWithWrongState` (in `WrongStateMemberTest`): tests the system with a Byzantine member that sends a wrong state message in the second instance. We assert if the consensus still works (that the same transaction isn't executed twice)

**Regular Test**
- `testBlockChainNormalBehaviour`: tests the normal behaviour of the blockchain, sending transfers between all clients and checking if the allowance mechanism is working properly

TODO: remove?
- `testConsensusWithWrongWriteAcceptByzantine`: tests the consensus algorithm with a Byzantine member that sends wrong WRITEs and ACCEPTs
- `testConsensusWithByzantinePerfectLink`: tests the consensus algorithm when all members have a Byzantine perfect link, where messages can be lost, duplicated, reordered or corrupted.

**Contract Tests**
From root:
```bash
cd contract
```

To run all tests:
```bash
mvn test
```

The tests that we have check if:
- The contract outputs normal info (name, symbol, decimals)
- The blacklist functionality works
- The `transfer` and `transferFrom` primitives work as expected
- The `blacklist` and `unblacklist` primitives work as expected

### Manual Tests

To manually run tests and test the program, use this command to launch all members (no Byzantine):
```bash
./startMembers.sh
```

Or this command if you want a Byzantine member to be present:
```bash
./membersWithByzatine.sh <byzantineBehaviour>
```

Where `<byzantineBehaviour>` can be one of the following:
- '0': normal
- '1': ignore messages
- '2': send a wrong state message
- '3': fake signature
- '4': replay signature (**Don't** use this one, it's not working as intended)
- '5': spam messages (STATEs, WRITEs...)
- '6': send wrong writes and accepts

Afterwards (very important to be **paulo**, **joao**, **pedro** or some client that you generated the keys for), run the client:
```bash
cd client
mvn compile exec:java -Dexec.args="2000 paulo 0"
```

Send requests:
```bash
ISTCoin transfer <address> <amount>
ISTCoin balance <address>
[...]
```

Check the logs at the `member/logs` directory.