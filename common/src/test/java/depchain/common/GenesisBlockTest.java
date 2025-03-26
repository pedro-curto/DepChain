package depchain.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import depchain.common.domain.Block;
import depchain.common.domain.BlockChainState;
import depchain.common.domain.JsonAdapter;
import depchain.common.domain.Account;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;

class GenesisBlockTest {

    @Test
    void itLoadsGenesisBlock() throws IOException {
        Path currentDir = Paths.get(System.getProperty("user.dir"));
        Path rootDir = currentDir.getParent();
        Path genesisPath = rootDir.resolve("genesis-file.json");

        // load the json file
        String jsonString = Files.readString(genesisPath);
        JsonElement jsonElement = JsonParser.parseString(jsonString);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        Block block = JsonAdapter.parseBlock(jsonObject);

        // Block assertions
        assertNotNull(block, "Block should not be null");
        assertEquals("nLiTjQmEv6MSv9Iw7FjGH9rfSFY4RKvqFlA7Vp3Aelw=", block.getHash(), "Block hash should match");
        assertEquals("", block.getPreviousHash(), "Previous hash should be empty for genesis block");

        // Check that the block contains no transactions
        assertTrue(block.getTransactions().isEmpty(), "Transactions should be empty for genesis block");

        // Check that the state contains one account
        BlockChainState blockChainState = block.getState();
        assertNotNull(blockChainState, "BlockChainState should not be null");

        // Check account details
        List<Account> accounts = blockChainState.getAccounts();
        Account account = accounts.getFirst();
        assertEquals("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmE/6r6V8TX1zDIhTxUtz\nzT1vWvfLpbB1czCajImA4vU0xcCEVLDZoi5IWcIsi60aj5YCMqV2APuHyHL8Cwgb\nNwIz0G7sRb1pqFtvbeEdeds5Khz97QMWfPJJxYzV56YRnypJDCjwxowX3yeBmlGI\nHeXG3jSDP03z9PuiMVTRd77Q59yVuqJneyKjYZRgM1UWZ9FaNv69gVaOsa+5rdEx\n1sXFQ/yHAMrWqiBlv+337s4ZA1dGgd9UQ60oxcMi4gBdn5stXX/671zmafkYsuzn\nunSW1vOIrIxO2B71ahKymYTKtS+ylxoL+HI7xGAjlRMzJsIiUMmSdJNDCacGk3fK\nlwIDAQAB\n",
                account.getAddress(), "Account address should match");
        assertEquals(100, account.getBalance(), "Account balance should match");

        System.out.println(block);
    }
}
