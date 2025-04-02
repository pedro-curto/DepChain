package depchain.common;

import com.google.gson.*;
import depchain.common.domain.*;
import depchain.common.messaging.CoinType;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

public class JsonAdapter {

    public static Block parseBlock(JsonObject json, boolean isGenesis) {
       String hash = json.get("hash").getAsString();
       String previousHash = json.get("previous_hash").getAsString();

        List<Transaction> transactions = json.getAsJsonArray("transactions")
                .asList()
                .stream()
                .map(JsonAdapter::parseTransaction)
                .collect(Collectors.toList());

        if (isGenesis) {
            BlockChainState blockChainState = parseBlockChainState(json.getAsJsonObject("state"));
            return new Block(hash, previousHash, transactions, blockChainState);
        }
        // String previousHash, List<Transaction> transactions, long blockNumber, long timestamp
        long blockNumber = json.get("block_number").getAsLong();
        long timestamp = json.get("timestamp").getAsLong();
        return new Block(previousHash, transactions, blockNumber, timestamp);
    }

    public static BlockChainState parseBlockChainState(JsonObject json) {
        List<Account> acccounts = json.getAsJsonArray("accounts")
                .asList()
                .stream()
                .map(JsonElement::getAsJsonObject)
                .map(JsonAdapter::parseAccount)
                .collect(Collectors.toList());
        return new BlockChainState(acccounts);
    }

    public static Account parseAccount(JsonObject json) {
        return new Gson().fromJson(json, Account.class);
    }

    public static Transaction parseTransaction(JsonElement json) {
        JsonObject obj = json.getAsJsonObject();
        return new Transaction(
                obj.get("sender").getAsString(),
                obj.get("recipient").getAsString(),
                obj.get("amount").getAsBigInteger(),
                obj.get("signature").getAsString(),
                obj.get("nonce").getAsLong(),
                Transaction.TransactionType.valueOf(obj.get("type").getAsString()),
                CoinType.valueOf(obj.get("coin_type").getAsString())
        );
    }

    public static JsonObject serializeBlockChain(BlockChain blockChain) {
        JsonObject json = new JsonObject();

        JsonArray blocks = new JsonArray();
        blockChain.getBlocks()
                .stream()
                .map(JsonAdapter::serializeBlock)
                .forEach(blocks::add);
        json.add("blocks", blocks);

        return json;
    }

    public static JsonObject serializeBlock(Block block) {
        JsonObject json = new JsonObject();
        json.addProperty("hash", block.getHash());
        json.addProperty("previous_hash", block.getPreviousHash());
        json.addProperty("block_number", block.getBlockNumber());
        json.addProperty("timestamp", block.getTimestamp());

        JsonArray transactions = new JsonArray();
        block.getTransactions()
                .stream()
                .map(JsonAdapter::serializeTransaction)
                .forEach(transactions::add);
        json.add("transactions", transactions);
        // TODO -> i dont think we're going to have a state in the block
        //json.add("state", JsonAdapter.serializeBlockChainState(block.getState()));
        return json;
    }

    public static JsonObject serializeBlockChainState(BlockChainState blockChainState) {
        JsonObject json = new JsonObject();

        JsonArray accounts = new JsonArray();
        blockChainState.getAccounts().values()
                .stream()
                .map(JsonAdapter::serializeAccount)
                .forEach(accounts::add);
        json.add("accounts", accounts);

        return json;
    }

    public static JsonObject serializeAccount(Account account) {
        JsonObject json = new JsonObject();
        json.addProperty("address", account.getAddress());
        json.addProperty("balance", account.getBalance());
        return json;
    }

    public static JsonObject serializeTransaction(Transaction transaction) {
        // TODO
        JsonObject json = new JsonObject();
        json.addProperty("sender", transaction.getSender());
        json.addProperty("recipient", transaction.getRecipient());
        json.addProperty("amount", transaction.getAmount());
        json.addProperty("signature", transaction.getSignature());
        json.addProperty("nonce", transaction.getNonce());
        json.addProperty("type", transaction.getType().toString());
        json.addProperty("coin_type", transaction.getCoinType().toString());
        json.addProperty("success", transaction.getSuccess());
        return json;
    }

}