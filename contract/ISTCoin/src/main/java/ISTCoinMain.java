import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.*;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.EvmSpecVersion;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;


public class ISTCoinMain {

    // from solc .bin output
    private static final String BLACKLIST_BYTECODE = "6080604052348015600e575f5ffd5b503360015f6101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055506105548061005c5f395ff3fe608060405234801561000f575f5ffd5b506004361061004a575f3560e01c806344337ea11461004e578063537df3b61461006a578063b2bdfa7b14610086578063fe575a87146100a4575b5f5ffd5b610068600480360381019061006391906103f9565b6100d4565b005b610084600480360381019061007f91906103f9565b6101fd565b005b61008e610325565b60405161009b9190610433565b60405180910390f35b6100be60048036038101906100b991906103f9565b61034a565b6040516100cb9190610466565b60405180910390f35b60015f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff1614610163576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161015a906104d9565b60405180910390fd5b60015f5f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f6101000a81548160ff0219169083151502179055508073ffffffffffffffffffffffffffffffffffffffff167fffa4e6181777692565cf28528fc88fd1516ea86b56da075235fa575af6a4b85560405160405180910390a250565b60015f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff161461028c576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610283906104d9565b60405180910390fd5b5f5f5f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f6101000a81548160ff0219169083151502179055508073ffffffffffffffffffffffffffffffffffffffff167f117e3210bb9aa7d9baff172026820255c6f6c30ba8999d1c2fd88e2848137c4e60405160405180910390a250565b60015f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681565b5f5f5f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f9054906101000a900460ff169050919050565b5f5ffd5b5f73ffffffffffffffffffffffffffffffffffffffff82169050919050565b5f6103c88261039f565b9050919050565b6103d8816103be565b81146103e2575f5ffd5b50565b5f813590506103f3816103cf565b92915050565b5f6020828403121561040e5761040d61039b565b5b5f61041b848285016103e5565b91505092915050565b61042d816103be565b82525050565b5f6020820190506104465f830184610424565b92915050565b5f8115159050919050565b6104608161044c565b82525050565b5f6020820190506104795f830184610457565b92915050565b5f82825260208201905092915050565b7f4f776e61626c653a2063616c6c6572206973206e6f7420746865206f776e65725f82015250565b5f6104c360208361047f565b91506104ce8261048f565b602082019050919050565b5f6020820190508181035f8301526104f0816104b7565b905091905056fea26469706673582212206e306fb8113426d985b1609049f61fc0922e2d32e0fcbecb16fd520a128fbfe764736f6c637829302e382e32382d646576656c6f702e323032342e31302e31302b636f6d6d69742e3738393336313461005a";
    private static final String ISTCOIN_BYTECODE = "608060405234801561000f575f80fd5b5060043610610091575f3560e01c8063313ce56711610064578063313ce5671461013157806370a082311461014f57806395d89b411461017f578063a9059cbb1461019d578063dd62ed3e146101cd57610091565b806306fdde0314610095578063095ea7b3146100b357806318160ddd146100e357806323b872dd14610101575b5f80fd5b61009d6101fd565b6040516100aa9190610c0d565b60405180910390f35b6100cd60048036038101906100c89190610cbe565b61028d565b6040516100da9190610d16565b60405180910390f35b6100eb6102af565b6040516100f89190610d3e565b60405180910390f35b61011b60048036038101906101169190610d57565b6102b8565b6040516101289190610d16565b60405180910390f35b6101396103bf565b6040516101469190610dc2565b60405180910390f35b61016960048036038101906101649190610ddb565b6103c7565b6040516101769190610d3e565b60405180910390f35b61018761040c565b6040516101949190610c0d565b60405180910390f35b6101b760048036038101906101b29190610cbe565b61049c565b6040516101c49190610d16565b60405180910390f35b6101e760048036038101906101e29190610e06565b610597565b6040516101f49190610d3e565b60405180910390f35b60606003805461020c90610e71565b80601f016020809104026020016040519081016040528092919081815260200182805461023890610e71565b80156102835780601f1061025a57610100808354040283529160200191610283565b820191905f5260205f20905b81548152906001019060200180831161026657829003601f168201915b5050505050905090565b5f80610297610619565b90506102a4818585610620565b600191505092915050565b5f600254905090565b5f60055f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1663fe575a87856040518263ffffffff1660e01b81526004016103139190610eb0565b602060405180830381865afa15801561032e573d5f803e3d5ffd5b505050506040513d601f19601f820116820180604052508101906103529190610ef3565b15610392576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161038990610f8e565b60405180910390fd5b5f61039b610619565b90506103a8858285610632565b6103b38585856106c5565b60019150509392505050565b5f6002905090565b5f805f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f20549050919050565b60606004805461041b90610e71565b80601f016020809104026020016040519081016040528092919081815260200182805461044790610e71565b80156104925780601f1061046957610100808354040283529160200191610492565b820191905f5260205f20905b81548152906001019060200180831161047557829003601f168201915b5050505050905090565b5f806104a6610619565b905060055f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1663fe575a87826040518263ffffffff1660e01b81526004016105029190610eb0565b602060405180830381865afa15801561051d573d5f803e3d5ffd5b505050506040513d601f19601f820116820180604052508101906105419190610ef3565b15610581576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016105789061101c565b60405180910390fd5b61058c8185856106c5565b600191505092915050565b5f60015f8473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f2054905092915050565b5f33905090565b61062d83838360016107b5565b505050565b5f61063d8484610597565b90507fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff8110156106bf57818110156106b0578281836040517ffb8f41b20000000000000000000000000000000000000000000000000000000081526004016106a79392919061103a565b60405180910390fd5b6106be84848484035f6107b5565b5b50505050565b5f73ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff1603610735575f6040517f96c6fd1e00000000000000000000000000000000000000000000000000000000815260040161072c9190610eb0565b60405180910390fd5b5f73ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff16036107a5575f6040517fec442f0500000000000000000000000000000000000000000000000000000000815260040161079c9190610eb0565b60405180910390fd5b6107b0838383610984565b505050565b5f73ffffffffffffffffffffffffffffffffffffffff168473ffffffffffffffffffffffffffffffffffffffff1603610825575f6040517fe602df0500000000000000000000000000000000000000000000000000000000815260040161081c9190610eb0565b60405180910390fd5b5f73ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff1603610895575f6040517f94280d6200000000000000000000000000000000000000000000000000000000815260040161088c9190610eb0565b60405180910390fd5b8160015f8673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f8573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f2081905550801561097e578273ffffffffffffffffffffffffffffffffffffffff168473ffffffffffffffffffffffffffffffffffffffff167f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b925846040516109759190610d3e565b60405180910390a35b50505050565b5f73ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff16036109d4578060025f8282546109c8919061109c565b92505081905550610aa2565b5f805f8573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f2054905081811015610a5d578381836040517fe450d38c000000000000000000000000000000000000000000000000000000008152600401610a549392919061103a565b60405180910390fd5b8181035f808673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f2081905550505b5f73ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff1603610ae9578060025f8282540392505081905550610b33565b805f808473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f82825401925050819055505b8173ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef83604051610b909190610d3e565b60405180910390a3505050565b5f81519050919050565b5f82825260208201905092915050565b8281835e5f83830152505050565b5f601f19601f8301169050919050565b5f610bdf82610b9d565b610be98185610ba7565b9350610bf9818560208601610bb7565b610c0281610bc5565b840191505092915050565b5f6020820190508181035f830152610c258184610bd5565b905092915050565b5f80fd5b5f73ffffffffffffffffffffffffffffffffffffffff82169050919050565b5f610c5a82610c31565b9050919050565b610c6a81610c50565b8114610c74575f80fd5b50565b5f81359050610c8581610c61565b92915050565b5f819050919050565b610c9d81610c8b565b8114610ca7575f80fd5b50565b5f81359050610cb881610c94565b92915050565b5f8060408385031215610cd457610cd3610c2d565b5b5f610ce185828601610c77565b9250506020610cf285828601610caa565b9150509250929050565b5f8115159050919050565b610d1081610cfc565b82525050565b5f602082019050610d295f830184610d07565b92915050565b610d3881610c8b565b82525050565b5f602082019050610d515f830184610d2f565b92915050565b5f805f60608486031215610d6e57610d6d610c2d565b5b5f610d7b86828701610c77565b9350506020610d8c86828701610c77565b9250506040610d9d86828701610caa565b9150509250925092565b5f60ff82169050919050565b610dbc81610da7565b82525050565b5f602082019050610dd55f830184610db3565b92915050565b5f60208284031215610df057610def610c2d565b5b5f610dfd84828501610c77565b91505092915050565b5f8060408385031215610e1c57610e1b610c2d565b5b5f610e2985828601610c77565b9250506020610e3a85828601610c77565b9150509250929050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52602260045260245ffd5b5f6002820490506001821680610e8857607f821691505b602082108103610e9b57610e9a610e44565b5b50919050565b610eaa81610c50565b82525050565b5f602082019050610ec35f830184610ea1565b92915050565b610ed281610cfc565b8114610edc575f80fd5b50565b5f81519050610eed81610ec9565b92915050565b5f60208284031215610f0857610f07610c2d565b5b5f610f1584828501610edf565b91505092915050565b7f495354436f696e3a207370656e64657220697320626c61636b6c6973746564205f8201527f616e642063616e6e6f74207472616e7366657200000000000000000000000000602082015250565b5f610f78603383610ba7565b9150610f8382610f1e565b604082019050919050565b5f6020820190508181035f830152610fa581610f6c565b9050919050565b7f495354436f696e3a2073656e64657220697320626c61636b6c697374656420615f8201527f6e642063616e6e6f74207472616e736665720000000000000000000000000000602082015250565b5f611006603283610ba7565b915061101182610fac565b604082019050919050565b5f6020820190508181035f83015261103381610ffa565b9050919050565b5f60608201905061104d5f830186610ea1565b61105a6020830185610d2f565b6110676040830184610d2f565b949350505050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52601160045260245ffd5b5f6110a682610c8b565b91506110b183610c8b565b92508282019050808211156110c9576110c861106f565b5b9291505056fea2646970667358221220e8855fd9cafa36ae37a644ae669fd332a920231bab11a0780cca9ebb341884bd64736f6c634300081a0033";
    public static void main(String[] args) {
        // create the world
        SimpleWorld world = new SimpleWorld();

        // creates sender account
        Address sender = Address.fromHexString("deadbeefdeadbeefdeadbeefdeadbeefdeadbeef");
        world.createAccount(sender,0, Wei.fromEth(100));

        // deploy istcoin contract
        Address istCoinAddr = Address.fromHexString("1234567891234567891234567891234567891234");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bos);
        StandardJsonTracer tracer = new StandardJsonTracer(ps, true, true, true, true);

        var executor = EVMExecutor.evm(EvmSpecVersion.CANCUN)
                .tracer(tracer)
                .code(Bytes.fromHexString(ISTCOIN_BYTECODE))
                .sender(sender)
                .receiver(istCoinAddr)
                .worldUpdater(world.updater())
                .commitWorldState();
        executor.execute();

//        "methodIdentifiers": {
//            "allowance(address,address)": "dd62ed3e",
//                    "approve(address,uint256)": "095ea7b3",
//                    "balanceOf(address)": "70a08231",
//                    "decimals()": "313ce567",
//                    "name()": "06fdde03",
//                    "symbol()": "95d89b41",
//                    "totalSupply()": "18160ddd",
//                    "transfer(address,uint256)": "a9059cbb",
//                    "transferFrom(address,address,uint256)": "23b872dd"

        // call balanceOf(sender)
        String callData = "70a08231" + padHexStringTo256Bit(sender.toHexString());
        executor.callData(Bytes.fromHexString("0x" + callData));
        executor.execute();
        int balance = extractIntegerFromReturnData(bos);
        System.out.println("Sender balance: " + balance);

        // call name() and parse result (a string)
        bos.reset();
        executor.callData(Bytes.fromHexString("06fdde03"));
        executor.execute();
        String name = extractStringFromReturnData(bos);
        System.out.println("Name: " + name);

        // call symbol() and parse result (a string)
//        executor.callData(Bytes.fromHexString("95d89b41"));
//        executor.execute();
//        String symbol = extractStringFromReturnData(bos);
//        System.out.println("Symbol: " + symbol);

        // call decimals() and parse result (a string)
//        executor.callData(Bytes.fromHexString("313ce567"));
//        executor.execute();
//        String decimals = extractStringFromReturnData(bos);
//        System.out.println("Decimals: " + decimals);




//        // 2. Create a test account with some Ether-like balance
//        //updater.createAccount(sen<derAddress, 0, Wei.fromEth(100));
//        //updater.commit();
//
//        // Just to illustrate
//        MutableAccount senderAccount = (MutableAccount) world.get(senderAddress);
//        System.out.println("== SENDER ACCOUNT ==");
//        System.out.println("Address: " + senderAccount.getAddress());
//        System.out.println("Balance: " + senderAccount.getBalance());
//        System.out.println();
//
//        // 3. We'll pick an address for the Blacklist contract
//        //    Then "deploy" it in the EVM by setting code to that address
//        Address blacklistAddress = Address.fromHexString("1234567891234567891234567891234567891234");
//        updater.createAccount(blacklistAddress, 0, Wei.ZERO);
//        updater.commit();
//
//        // 4. We'll pick an address for the ISTCoin contract
//        Address istCoinAddress = Address.fromHexString("abababababababababababababababababababab");
//        updater.createAccount(istCoinAddress, 0, Wei.ZERO);
//        updater.commit();
//
//        // Setup tracer for debugging
//        ByteArrayOutputStream bos = new ByteArrayOutputStream();
//        PrintStream ps = new PrintStream(bos);
//        StandardJsonTracer tracer = new StandardJsonTracer(ps, true, true, true, true);
//
//        // 5. Deploy the “Blacklist” bytecode
//        //    In your real code, you might do the entire creation flow (constructor)
//        //    For simplicity, we’ll just set the code directly (like your lab).
//        EVMExecutor executor = EVMExecutor.evm(EvmSpecVersion.CANCUN)
//                .tracer(tracer)
//                .worldUpdater(world.updater())
//                .sender(senderAddress)
//                .receiver(blacklistAddress)
//                .code(Bytes.fromHexString(BLACKLIST_BYTECODE))
//                .commitWorldState();
//
//        // Execute "deployment"
//        executor.execute();
//
//        // 6. Deploy ISTCoin (which also internally deploys a Blacklist, per your constructor)
//        //    But your constructor does “new Blacklist()” – that will happen in code.
//        //    If you want the separate addresses for each, that’s a more advanced scenario.
//        //    For now we do the same approach: just store the ISTCoin bytecode at the address.
//        executor = EVMExecutor.evm(EvmSpecVersion.CANCUN)
//                .tracer(tracer)
//                .worldUpdater(world.updater())
//                .sender(senderAddress)
//                .receiver(istCoinAddress)
//                .code(Bytes.fromHexString(ISTCOIN_BYTECODE))
//                .commitWorldState();
//
//        // Execute "deployment"
//        executor.execute();
//
//        // Clear out the old logs from bos
//        bos.reset();
//
//        // *** Next calls: we typically want to see if blacklisting works,
//        //     or if the minted tokens are there, etc.
//
//        // 7. Let’s read the balanceOf(sender). The function signature for
//        //    `balanceOf(address)` is 0x70a08231. Then we pass the address
//        //    in 256-bit hex.
//        String functionSelectorBalanceOf = "70a08231";
//        String paddedAddress = padHexStringTo256Bit(senderAddress.toHexString());
//        String callData = functionSelectorBalanceOf + paddedAddress;
//
//        // We'll call that on the ISTCoin address
//        executor = EVMExecutor.evm(EvmSpecVersion.CANCUN)
//                .tracer(tracer)
//                .worldUpdater(world.updater())
//                .sender(senderAddress)
//                .receiver(istCoinAddress)
//                .commitWorldState();
//        executor.callData(Bytes.fromHexString("0x" + callData));
//        executor.execute();
//
//        // parse result
//        BigInteger balanceSender = extractBigIntegerFromReturnData(bos);
//        System.out.println("Initial sender's ISTCoin balance = " + balanceSender);
//
//        // 8. We'll attempt a "transfer(...)"
//        //    The function signature for transfer(address,uint256) is "a9059cbb"
//        //    Then we pass the _to address in 256 bits, and the amount in 256 bits.
//
//        // Let's define a "to" address
//        Address toAddress = Address.fromHexString("5555555555555555555555555555555555555555");
//        // We'll transfer 100 tokens
//        String functionSelectorTransfer = "a9059cbb";
//        String paddedTo = padHexStringTo256Bit(toAddress.toHexString());
//        String paddedValue = convertIntegerToHex256Bit(100);
//
//        String callDataTransfer = functionSelectorTransfer + paddedTo + paddedValue;
//
//        bos.reset();
//        executor = EVMExecutor.evm(EvmSpecVersion.CANCUN)
//                .tracer(tracer)
//                .worldUpdater(world.updater())
//                .sender(senderAddress)
//                .receiver(istCoinAddress)
//                .commitWorldState();
//        executor.callData(Bytes.fromHexString("0x" + callDataTransfer));
//        executor.execute();
//        // The result should be "true" if successful, or revert if blacklisted.
//
//        // Now let's read the new balance
//        bos.reset();
//        executor = EVMExecutor.evm(EvmSpecVersion.CANCUN)
//                .tracer(tracer)
//                .worldUpdater(world.updater())
//                .sender(senderAddress)
//                .receiver(istCoinAddress)
//                .commitWorldState();
//        // call again balanceOf(toAddress)
//        callData = functionSelectorBalanceOf + padHexStringTo256Bit(toAddress.toHexString());
//        executor.callData(Bytes.fromHexString("0x" + callData));
//        executor.execute();
//
//        BigInteger balanceTo = extractBigIntegerFromReturnData(bos);
//        System.out.println("After transfer, 'toAddress' balance = " + balanceTo);
//
//        // 9. We can simulate blacklisting the sender if your
//        //    constructor was separate. Right now, your ISTCoin constructor
//        //    does new Blacklist internally, so let's just see that we do
//        //    call "addToBlacklist(senderAddress)" on that internal contract
//        //    if you had an external function.
//        //    This is more advanced because your contract doesn't expose
//        //    direct references to the inside Blacklist, but you get the idea.
//
//        // For demonstration, let's just show the final state of the storage
//        // for the ISTCoin contract
//        var coinAcct = (MutableAccount) world.get(istCoinAddress);
//        System.out.println("== ISTCoin Contract Account ==");
//        System.out.println("Address: " + coinAcct.getAddress());
//        System.out.println("Balance: " + coinAcct.getBalance());
//        System.out.println("Nonce:   " + coinAcct.getNonce());
//
//        // Possibly dump a known slot if you want
//        // e.g. we can see some storage
//        System.out.println("Slot 0: " + world.get(istCoinAddress).getStorageValue(UInt256.ZERO));
}



    public static int extractIntegerFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();

        String memory = jsonObject.get("memory").getAsString();

        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());

        String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);
        return Integer.decode("0x"+returnData);
    }

    public static String extractStringFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length-1]).getAsJsonObject();

        String memory = jsonObject.get("memory").getAsString();

        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size()-1).getAsString());
        int size = Integer.decode(stack.get(stack.size()-2).getAsString());

        String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);

        int stringOffset = Integer.decode("0x"+returnData.substring(0, 32 * 2));
        int stringLength = Integer.decode("0x"+returnData.substring(stringOffset * 2, stringOffset * 2 + 32 * 2));
        String hexString = returnData.substring(stringOffset * 2 + 32 * 2, stringOffset * 2 + 32 * 2 + stringLength * 2);

        return new String(hexStringToByteArray(hexString), StandardCharsets.UTF_8);
    }

    public static String convertIntegerToHex256Bit(int number) {
        BigInteger bigInt = BigInteger.valueOf(number);

        return String.format("%064x", bigInt);
    }

    public static String padHexStringTo256Bit(String hexString) {
        if (hexString.startsWith("0x")) {
            hexString = hexString.substring(2);
        }

        int length = hexString.length();
        int targetLength = 64;

        if (length >= targetLength) {
            return hexString.substring(0, targetLength);
        }

        return "0".repeat(targetLength - length) +
                hexString;
    }

    public static byte[] hexStringToByteArray(String hexString) {
        int length = hexString.length();
        byte[] byteArray = new byte[length / 2];

        for (int i = 0; i < length; i += 2) {
            int value = Integer.parseInt(hexString.substring(i, i + 2), 16);
            byteArray[i / 2] = (byte) value;
        }

        return byteArray;
    }

    // TODO -> remove below

    /**
     * Utility: parse the last line of the JSON tracer to get the 'returnData',
     * interpret it as a 256-bit integer, and return as BigInteger.
     */
    public static BigInteger extractBigIntegerFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        if (lines.length == 0) {
            System.out.println("No lines from tracer!");
            return BigInteger.ZERO;
        }
        String lastLine = lines[lines.length - 1];
        JsonObject jsonObject = JsonParser.parseString(lastLine).getAsJsonObject();

        // get "memory"
        String memory = jsonObject.get("memory").getAsString();

        // get top-of-stack offset, size
        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());

        // get the substring
        String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);
        if (returnData.isEmpty()) {
            return BigInteger.ZERO;
        }
        BigInteger val = new BigInteger(returnData, 16);
        return val;
    }

}
