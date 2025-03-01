# DepChain

## Building the Project
From the root directory of the project, run the following command:
```bash
mvn clean install
```

## Running the Project
**Server**

To run the server:
```bash
cd server
mvn compile exec:java
```

**Client**

To run the client:
```bash
cd client
mvn compile exec:java -Dexec.args="<client port> <client name>"
```

Now, at the client, you can send messages to the server:
```bash
foo 
bar
```

## Tests
- TODO: Add tests