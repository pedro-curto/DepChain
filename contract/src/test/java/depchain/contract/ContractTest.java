package depchain.contract;

import depchain.common.CommonUtils;
import depchain.common.DCLogger;
import depchain.common.domain.ContractData;
import depchain.common.domain.GenesisBlock;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;
import org.junit.jupiter.api.*;
import org.hyperledger.besu.datatypes.Address;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;

public class ContractTest {
    private static final String SENDER_STR = "deadbeefdeadbeefdeadbeefdeadbeefdeadbeef";
    private static final String CONTRACT_STR = "1234567891234567891234567891234567891234";
    private static final String RECIPIENT_STR = "1111111111111111111111111111111111111111";
    private static final String SPENDER_STR = "2222222222222222222222222222222222222222";
    private static final String OWNER_STR = "3333333333333333333333333333333333333333";
    private static final Address SENDER = Address.fromHexString(SENDER_STR);
    private static final Address CONTRACT = Address.fromHexString(CONTRACT_STR);
    private static final Address RECIPIENT = Address.fromHexString(RECIPIENT_STR);
    private static final Address SPENDER = Address.fromHexString(SPENDER_STR);
    private static final Address OWNER = Address.fromHexString(OWNER_STR);

    private SimpleWorld world;
    private EVMExecutor executor;
    private ByteArrayOutputStream bos;
    private PrintStream printStream;
    private StandardJsonTracer tracer;
    private String deploymentBytecode;
    private String runtimeBytecode;

    @BeforeEach
    void setup() {
        this.world = new SimpleWorld();
        this.bos = new ByteArrayOutputStream();
        this.printStream = new PrintStream(bos);
        this.tracer = new StandardJsonTracer(printStream, true, true, true, true);
        GenesisBlock genesisBlock = CommonUtils.loadGenesisBlock();
        ContractData contractData = genesisBlock.getContractData();
        this.deploymentBytecode = contractData.getDeploymentBytecode();
        this.runtimeBytecode = contractData.getRuntimeBytecode();
    }

    @AfterEach
    void tearDown() {
        // probably we won't need this but just in case
    }




    @Test
    public void testGeneralContractInfos() {
        /*
         * Asserts if basic information from the contract is fetched correctly
         * (name, symbol, totalSupply, decimals)
         */
        SimpleWorld world = this.world;
        EVMExecutor executor = ContractFunctions.deployContract(SENDER_STR, CONTRACT_STR, world, this.tracer,
                this.deploymentBytecode, this.runtimeBytecode);
        String name = ContractFunctions.callName(executor, this.bos);
        String symbol = ContractFunctions.callSymbol(executor, this.bos);
        BigInteger totalSupply = ContractFunctions.callTotalSupply(executor, this.bos);
        int decimals = ContractFunctions.callDecimals(executor, this.bos);

        Assertions.assertEquals("ISTCoin", name);
        Assertions.assertEquals("IST", symbol);
        Assertions.assertEquals(new BigInteger("10000000000"), totalSupply);
        Assertions.assertEquals(2, decimals);
    }

    @Test
    public void testBasicTransfers() {
        /*
        * Uses the sender and recipient defined above and transfers 2000 tokens from sender to recipient.
        * Then, checks if the balances reflect this.
         */
        SimpleWorld world = this.world;
        EVMExecutor executor = ContractFunctions.deployContract(SENDER_STR, CONTRACT_STR, world, this.tracer,
                this.deploymentBytecode, this.runtimeBytecode);

        ContractFunctions.transferTokens(executor, bos, SENDER, RECIPIENT, BigInteger.valueOf(2000));
        BigInteger recpBalance = ContractFunctions.callBalanceOf(executor, this.bos, RECIPIENT);
        BigInteger senderBalance = ContractFunctions.callBalanceOf(executor, this.bos, SENDER);

        Assertions.assertEquals(new BigInteger("2000"), recpBalance);
        Assertions.assertEquals(new BigInteger("9999998000"), senderBalance);
    }

    @Test
    public void testBlacklistedTransfer() {
        /*
         * Blacklists a given user and tries a transfer to him, that should fail.
         * Then, removes the user from blacklist and retries the transfer.
         */
        EVMExecutor executor = ContractFunctions.deployContract(SENDER_STR, CONTRACT_STR, world, tracer,
                this.deploymentBytecode, this.runtimeBytecode);

        // blacklist and attempt transfer
        ContractFunctions.addToBlacklist(executor, SENDER, SENDER);
        ContractFunctions.transferTokens(executor, bos, SENDER, RECIPIENT, BigInteger.valueOf(100));
        BigInteger senderBalance = ContractFunctions.callBalanceOf(executor, bos, SENDER);
        Assertions.assertEquals(new BigInteger("10000000000"), senderBalance); // Balance unchanged
        boolean isBlacklisted = ContractFunctions.callIsBlacklisted(executor, bos, SENDER);
        Assertions.assertTrue(isBlacklisted);

        // remove from blacklist and retry
        ContractFunctions.removeFromBlacklist(executor, SENDER, SENDER);
        ContractFunctions.transferTokens(executor, bos, SENDER, RECIPIENT, BigInteger.valueOf(100));
        BigInteger newBalance = ContractFunctions.callBalanceOf(executor, bos, RECIPIENT);
        Assertions.assertEquals(BigInteger.valueOf(100), newBalance);
    }

    @Test
    public void testTransferFrom() {
        /*
         * Tries to perform a transferFrom transfer with no allowance (fails).
         * After, sets allowance, retries, and checks the new balances and allowance.
         */
        EVMExecutor executor = ContractFunctions.deployContract(SENDER_STR, CONTRACT_STR, world, tracer,
                this.deploymentBytecode, this.runtimeBytecode);

        // try to do a transfer from an account with no allowance
        boolean result = ContractFunctions.transferFrom(executor, bos, SPENDER, SENDER, RECIPIENT, BigInteger.valueOf(100));
        Assertions.assertFalse(result);

        // approve 1000 tokens and transfer 500
        ContractFunctions.approve(executor, bos, SENDER, SPENDER, BigInteger.valueOf(1000));
        BigInteger allowance = ContractFunctions.callAllowance(executor, bos, SENDER, SPENDER);
        Assertions.assertEquals(BigInteger.valueOf(1000), allowance);
        ContractFunctions.transferFrom(executor, bos, SPENDER, SENDER, RECIPIENT, BigInteger.valueOf(500));

        // check balances/allowance
        BigInteger senderBalance = ContractFunctions.callBalanceOf(executor, bos, SENDER);
        BigInteger recipientBalance = ContractFunctions.callBalanceOf(executor, bos, RECIPIENT);
        BigInteger newAllowance = ContractFunctions.callAllowance(executor, bos, SENDER, SPENDER);
        Assertions.assertEquals(new BigInteger("9999999500"), senderBalance);
        Assertions.assertEquals(BigInteger.valueOf(500), recipientBalance);
        Assertions.assertEquals(BigInteger.valueOf(500), newAllowance);
    }

    @Test
    public void testBlacklistFunctionalities() {
        /*
         * Starts by checking if we can blacklist while not being the owner, and if the
         * blacklist and unblacklist functionalities work.
         * For transfer, transferFrom and approve, individually blocks a sender and recipient
         * (and spender when applied) and checks if the transfer fails.
         */
        EVMExecutor executor = ContractFunctions.deployContract(OWNER_STR, CONTRACT_STR, world, tracer,
                this.deploymentBytecode, this.runtimeBytecode);
        // gives some money to sender, spender, recipient
        ContractFunctions.transferTokens(executor, bos, OWNER, SENDER, BigInteger.valueOf(1000));
        ContractFunctions.transferTokens(executor, bos, OWNER, SPENDER, BigInteger.valueOf(1000));
        ContractFunctions.transferTokens(executor, bos, OWNER, RECIPIENT, BigInteger.valueOf(1000));

        // attempt blacklist operation without being an owner
        ContractFunctions.addToBlacklist(executor, SENDER, RECIPIENT);
        boolean result = ContractFunctions.callIsBlacklisted(executor, bos, RECIPIENT);
        Assertions.assertFalse(result);

        // blacklist operations with owner
        ContractFunctions.addToBlacklist(executor, OWNER, RECIPIENT);
        result = ContractFunctions.callIsBlacklisted(executor, bos, RECIPIENT);
        Assertions.assertTrue(result);
        ContractFunctions.removeFromBlacklist(executor, OWNER, RECIPIENT);
        result = ContractFunctions.callIsBlacklisted(executor, bos, RECIPIENT);
        Assertions.assertFalse(result);

        // owner gives an allowance to sender
        ContractFunctions.approve(executor, bos, OWNER, SENDER, BigInteger.valueOf(1000));
        BigInteger allowance = ContractFunctions.callAllowance(executor, bos, OWNER, SENDER);
        System.out.println("Allowance from owner to sender: " + allowance);
        Assertions.assertEquals(BigInteger.valueOf(1000), allowance);

        // block sender and check result of three operations
        ContractFunctions.addToBlacklist(executor, OWNER, SENDER);
        boolean transferResult = ContractFunctions.transferTokens(executor, bos, SENDER, RECIPIENT, BigInteger.valueOf(100));
        Assertions.assertFalse(transferResult);
        boolean approveResult = ContractFunctions.approve(executor, bos, SENDER, SPENDER, BigInteger.valueOf(100));
        Assertions.assertFalse(approveResult);
        // sender is spender
        boolean transferFromResult = ContractFunctions.transferFrom(executor, bos, OWNER, SENDER, RECIPIENT,
                BigInteger.valueOf(100));
        Assertions.assertFalse(transferFromResult);

        // unblock sender and block recipient, check result
        ContractFunctions.removeFromBlacklist(executor, OWNER, SENDER);
        ContractFunctions.addToBlacklist(executor, OWNER, RECIPIENT);
        transferResult = ContractFunctions.transferTokens(executor, bos, SENDER, RECIPIENT, BigInteger.valueOf(100));
        Assertions.assertFalse(transferResult);
        approveResult = ContractFunctions.approve(executor, bos, OWNER, RECIPIENT, BigInteger.valueOf(100));
        Assertions.assertFalse(approveResult);
        // sender is spender
        transferFromResult = ContractFunctions.transferFrom(executor, bos, OWNER, SENDER, RECIPIENT,
                BigInteger.valueOf(100));
        Assertions.assertFalse(transferFromResult);

        // 1. unblocks recipient 2. sender approves to spender 3. blocks sender 4. spender tries to transfer to recipient
        ContractFunctions.removeFromBlacklist(executor, OWNER, RECIPIENT);
        ContractFunctions.approve(executor, bos, SENDER, SPENDER, BigInteger.valueOf(1000));
        ContractFunctions.addToBlacklist(executor, OWNER, SENDER);
        transferResult = ContractFunctions.transferFrom(executor, bos, SPENDER, SENDER, RECIPIENT,
                BigInteger.valueOf(100));
        Assertions.assertFalse(transferResult);
    }

    @Test
    public void testTransferWithNegativeValue() {
        /*
         * Tries to transfer negative tokens from sender to recipient, and checks if the operation
         * is successful.
         */
        SimpleWorld world = this.world;
        EVMExecutor executor = ContractFunctions.deployContract(SENDER_STR, CONTRACT_STR, world, this.tracer,
                this.deploymentBytecode, this.runtimeBytecode);
        boolean result = ContractFunctions.transferTokens(executor, bos, SENDER, RECIPIENT, new BigInteger("-5"));
		Assertions.assertFalse(result);
    }

}
