package depchain.common.domain;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import depchain.common.JsonAdapter;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class BlockChain{
    private final String backupFileName = "";

    private final Path backupPath;
    private List<Block> blockchain;
    private BlockChainState state;

    public BlockChain(List<Block> blockchain, BlockChainState state) {
        this.blockchain = blockchain;
        this.state = state;
        this.backupPath = getBackupPath();
    }

    private Path getBackupPath() {
        Path currentDir = Paths.get(System.getProperty("user.dir"));
        Path rootDir = currentDir.getParent();
        return rootDir.resolve(backupFileName);
    }

    public void load() {
        String jsonString = null;
        try {
            jsonString = Files.readString(backupPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        JsonElement jsonElement = JsonParser.parseString(jsonString);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        // load blocks
        jsonObject.getAsJsonArray("blocks")
                .asList()
                .stream()
                .map(JsonElement::getAsJsonObject)
                .map(JsonAdapter::parseBlock)
                .forEach(block -> blockchain.add(block));
        // state is the same as the state of the last block appended
        this.state = blockchain.getLast().getState().copy();
    }

    public void save() {
        JsonObject json = JsonAdapter.serializeBlockChain(this);

        // Convert JSON object to string
        Gson gson = new Gson();
        String jsonString = gson.toJson(json);

        // Save to a file
        try (FileWriter file = new FileWriter("data.json")) {
            file.write(jsonString);
            file.flush();
            System.out.println("JSON saved successfully!");
        } catch (IOException e) {
            System.out.println("JSON save failed!");
            e.printStackTrace();
        }
    }

    public List<Block> getBlocks() {
        return new ArrayList<>(blockchain);
    }
}