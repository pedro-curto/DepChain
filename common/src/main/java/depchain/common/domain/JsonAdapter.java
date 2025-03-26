package depchain.common.domain;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
        return new Transaction();
    }

}