# DepChain

## Building the Project
From the root directory of the project, run the following command:
```bash
mvn clean install
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

To run a client (it's **very important** to pass it the client name specifically)
```bash
cd client
mvn compile exec:java -Dexec.args="2000 client"
```

Now, at the client, you can send messages to the server:
```bash
foo 
bar
```

## Tests
- TODO: Add tests