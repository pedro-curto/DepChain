package depchain.common;

import com.google.gson.*;
import depchain.common.domain.*;
import java.util.List;
import java.util.stream.Collectors;

public class JsonAdapter {

    public static Block parseBlock(JsonObject json) {
       String hash = json.get("hash").getAsString();
       String previousHash = json.get("previous_hash").getAsString();

        List<Transaction> transactions = json.getAsJsonArray("transactions")
                .asList()
                .stream()
                .map(JsonAdapter::parseTransaction)
                .collect(Collectors.toList());

        BlockChainState blockChainState = parseBlockChainState(json.getAsJsonObject("state"));
        return new Block(hash, previousHash, transactions, blockChainState);
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
        // TODO
        return new Transaction(null, null, 0.0, null, 0);
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

        JsonArray transactions = new JsonArray();
        block.getTransactions()
                .stream()
                .map(JsonAdapter::serializeTransaction)
                .forEach(transactions::add);
        json.add("transactions", transactions);

        json.add("state", JsonAdapter.serializeBlockChainState(block.getState()));
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
        return json;
    }

}