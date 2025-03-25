package depchain.member.state;

import com.google.gson.JsonObject;
import depchain.member.domain.Transaction;
import java.util.List;

public class Block {
    String hash;
    String previousHash;
    List<Transaction> transactions;
    List<BlockchainState> state; // has to store the state it had when appended

    Block(JsonObject json){}


}