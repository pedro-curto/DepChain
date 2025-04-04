# DepChain

## TODO
**Urgente**
- Permitir receber mensagens de épocas e instâncias no futuro (pode estar relacionado com o issue abaixo)
- Perceber o que se passa quando se dá spam a transações (começa a dar signature fail check e erros no log)
- Implementar testes bizantinos (Step 5)

**Se acabarmos o que está acima**
- Implement approve
  - Beware that: "changing an allowance with this method (approve) brings the risk that someone may use both the old and the new allowance by unfortunate transaction ordering. One possible solution to mitigate this race condition is to first reduce the spender’s allowance to 0 and set the desired value afterwards: https://github.com/ethereum/EIPs/issues/20#issuecomment-263524729"
- Fazer pré-seleção das transações (retornar se o balance for insuficiente)
- Limitar nº de mensagens que um cliente pode ter num bloco
- Limitar nº de mensagens que podem existir num bloco

## Configuration

**First of all, you need to generate the configuration files** (with the membership). 
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
This can take some time (around 1 minute) since it will run the tests.
We only observed this once, but in the eventuality of a test failing within `mvn clean install`, run the command below to ignore the tests.

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
**Member Tests**
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
- `testConsensusWithWrongStateMessage`: tests the consensus algorithm with a Byzantine member that sends a wrong state message in the second instance. We assert if the consensus still works (that the same message isn't decided twice)
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

Afterwards (very important to be **paulo** or some client that you generated the keys for), run the client:
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