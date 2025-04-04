package depchain.contract;

import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.EVM;
import org.hyperledger.besu.evm.EvmSpecVersion;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;


public class ContractFunctions {
    // -- METHOD IDENTIFIERS --
    private static final String BLACKLISTOWNER_ID = "2db3822b";
    private static final String ADDTOBLACKLIST_ID = "44337ea1";
    private static final String ALLOWANCE_ID = "dd62ed3e";
    private static final String APPROVE_ID = "095ea7b3";
    private static final String BALANCEOF_ID = "70a08231";
    private static final String DECIMALS_ID = "313ce567";
    private static final String ISBLACKLISTED_ID = "fe575a87";
    private static final String NAME_ID = "06fdde03";
    private static final String REMOVEFROMBLACKLIST_ID = "537df3b6";
    private static final String SYMBOL_ID = "95d89b41";
    private static final String TOTAL_SUPPLY_ID = "18160ddd";
    private static final String TRANSFER_ID = "a9059cbb";
    private static final String TRANSFERFROM_ID = "23b872dd";


    /*
        * Deploys the contract to the world state
        * @ownerAddress: address of the sender (owner of the contract)
        * @contractAddress: address of the contract
        * @world: world state
        * @tracer: tracer
        * @deploymentBytecode: deployment bytecode of the contract (from json)
        * @runtimeBytecode: runtime bytecode of the contract (from json)
     */
    public static EVMExecutor deployContract(String ownerAddress,
                                             String contractAddress,
                                             SimpleWorld world,
                                             StandardJsonTracer tracer,
                                             String deploymentBytecode,
                                             String runtimeBytecode) {
        EVMExecutor executor = EVMExecutor.evm(EvmSpecVersion.CANCUN);
        executor.tracer(tracer);
        executor.code(Bytes.fromHexString(deploymentBytecode));
        executor.sender(Address.fromHexString(ownerAddress));
        executor.receiver(Address.fromHexString(contractAddress));
        executor.worldUpdater(world.updater());
        executor.commitWorldState();
        executor.callData(Bytes.EMPTY);
        executor.execute();
        executor.code(Bytes.fromHexString(runtimeBytecode));
        executor.commitWorldState();
        return executor;
    }

    /*----------------------------------------------------------------------------------------------------------- */
    /* ------------------------------------------- UTILITY FUNCTIONS -------------------------------------------- */
    /*----------------------------------------------------------------------------------------------------------- */

    public static String callName(EVMExecutor executor, ByteArrayOutputStream bos) {
        executor.callData(Bytes.fromHexString(NAME_ID));
        executor.execute();
        String name = ContractUtils.extractStringFromReturnData(bos);
        System.out.println("ISTCoin name: " + name);
        return name;
    }

    public static String callSymbol(EVMExecutor executor, ByteArrayOutputStream bos) {
        executor.callData(Bytes.fromHexString(SYMBOL_ID));
        executor.execute();
        String symbol = ContractUtils.extractStringFromReturnData(bos);
        System.out.println("ISTCoin symbol: " + symbol);
        return symbol;
    }

    public static BigInteger callTotalSupply(EVMExecutor executor, ByteArrayOutputStream bos) {
        executor.callData(Bytes.fromHexString(TOTAL_SUPPLY_ID));
        executor.execute();
        BigInteger totalSupply = ContractUtils.extractBigIntegerFromReturnData(bos);
        System.out.println("Output of 'totalSupply():' " + totalSupply);
        return totalSupply;
    }

    public static int callDecimals(EVMExecutor executor, ByteArrayOutputStream bos) {
        executor.callData(Bytes.fromHexString(DECIMALS_ID));
        executor.execute();
        int decimals = ContractUtils.extractIntegerFromReturnData(bos);
        System.out.println("Output of 'decimals():' " + decimals);
        return decimals;
    }

    public static BigInteger callBalanceOf(EVMExecutor executor, ByteArrayOutputStream bos, Address address) {
        executor.callData(Bytes.fromHexString(BALANCEOF_ID + ContractUtils.padHexStringTo256Bit(address.toHexString())));
        executor.execute();
        BigInteger balance = ContractUtils.extractBigIntegerFromReturnData(bos);
        System.out.println("Balance of " + address.toHexString() + ": " + balance);
        return balance;
    }

    public static BigInteger callAllowance(EVMExecutor executor, ByteArrayOutputStream bos, Address owner, Address spender) {
        executor.callData(Bytes.fromHexString(ALLOWANCE_ID
                + ContractUtils.padHexStringTo256Bit(owner.toHexString())
                + ContractUtils.padHexStringTo256Bit(spender.toHexString())));
        executor.execute();
        return ContractUtils.extractBigIntegerFromReturnData(bos);
    }


    /*----------------------------------------------------------------------------------------------------------- */
    /* ------------------------------------------- TRANSFER FUNCTIONS ------------------------------------------- */
    /*----------------------------------------------------------------------------------------------------------- */


    public static boolean transferTokens(EVMExecutor executor,
                                         ByteArrayOutputStream bos,
                                         Address senderAddress,
                                         Address recipientAddress,
                                         BigInteger tokens) {
        // check sender and recipient's initial balance
        executor.callData(Bytes.fromHexString(BALANCEOF_ID + ContractUtils.padHexStringTo256Bit(senderAddress.toHexString())));
        executor.execute();
        BigInteger senderBalance = ContractUtils.extractBigIntegerFromReturnData(bos);
        System.out.println("Sender balance: " + senderBalance);

        executor.callData(Bytes.fromHexString(BALANCEOF_ID + ContractUtils.padHexStringTo256Bit(recipientAddress.toHexString())));
        executor.execute();
        BigInteger recipientBalance = ContractUtils.extractBigIntegerFromReturnData(bos);
        System.out.println("Recipient balance: " + recipientBalance);

        // execute transfer of 100 tokens to recipient
        executor.sender(senderAddress);
        executor.callData(Bytes.fromHexString(TRANSFER_ID
                + ContractUtils.padHexStringTo256Bit(recipientAddress.toHexString())
                + ContractUtils.convertIntegerToHex256Bit(tokens)));
        executor.execute();
        boolean result = ContractUtils.extractBooleanFromReturnData(bos);

        // check new balances
        executor.callData(Bytes.fromHexString(BALANCEOF_ID + ContractUtils.padHexStringTo256Bit(senderAddress.toHexString())));
        executor.execute();
        senderBalance = ContractUtils.extractBigIntegerFromReturnData(bos);
        System.out.println("Sender balance after transfer: " + senderBalance);

        executor.callData(Bytes.fromHexString(BALANCEOF_ID + ContractUtils.padHexStringTo256Bit(recipientAddress.toHexString())));
        executor.execute();
        recipientBalance = ContractUtils.extractBigIntegerFromReturnData(bos);
        System.out.println("Recipient balance after transfer: " + recipientBalance);

        return result;
    }

    public static boolean approve(EVMExecutor executor, ByteArrayOutputStream bos, Address owner, Address spender, BigInteger amount) {
        String callData = APPROVE_ID
                + ContractUtils.padHexStringTo256Bit(spender.toHexString())
                + ContractUtils.convertIntegerToHex256Bit(amount);
        executor.sender(owner);
        executor.callData(Bytes.fromHexString(callData));
        executor.execute();
        return ContractUtils.extractBooleanFromReturnData(bos);
    }

    public static boolean transferFrom(EVMExecutor executor, ByteArrayOutputStream bos, Address spender, Address from, Address to, BigInteger amount) {
        String callData = TRANSFERFROM_ID
                + ContractUtils.padHexStringTo256Bit(from.toHexString())
                + ContractUtils.padHexStringTo256Bit(to.toHexString())
                + ContractUtils.convertIntegerToHex256Bit(amount);
        executor.sender(spender);
        executor.callData(Bytes.fromHexString(callData));
        executor.execute();
        return ContractUtils.extractBooleanFromReturnData(bos);
    }


    /*----------------------------------------------------------------------------------------------------------- */
    /* ------------------------------------------- BLACKLIST FUNCTIONS ------------------------------------------ */
    /*----------------------------------------------------------------------------------------------------------- */

    public static void addToBlacklist(EVMExecutor executor, Address owner, Address account) {
        String callData = ADDTOBLACKLIST_ID
                + ContractUtils.padHexStringTo256Bit(account.toHexString());
        executor.sender(owner);
        executor.callData(Bytes.fromHexString(callData));
        executor.execute();
    }


    public static void removeFromBlacklist(EVMExecutor executor, Address owner, Address account) {
        String callData = REMOVEFROMBLACKLIST_ID
                + ContractUtils.padHexStringTo256Bit(account.toHexString());
        executor.sender(owner);
        executor.callData(Bytes.fromHexString(callData));
        executor.execute();
    }

    public static boolean callIsBlacklisted(EVMExecutor executor, ByteArrayOutputStream bos, Address account) {
        executor.callData(Bytes.fromHexString(ISBLACKLISTED_ID
                + ContractUtils.padHexStringTo256Bit(account.toHexString())));
        executor.execute();
        return ContractUtils.extractBooleanFromReturnData(bos);
    }

}
