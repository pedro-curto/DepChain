package depchain.common;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import depchain.common.domain.Block;
import depchain.common.domain.Entity;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

public class CommonUtils {

	public static Entity leaderLoader(String filename) {
		Entity leader = null;
		// reads leader from a file with a single line
		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
			String line = br.readLine();
			if (line != null) {
				String[] tokens = line.split(",");
				// leaderName,leaderAddress,leaderPort
				if (tokens.length != 3) {
					System.err.println("Invalid leader entry: " + line);
				} else {
					String name = tokens[0].trim();
					String address = tokens[1].trim();
					int port = Integer.parseInt(tokens[2].trim());
					leader = new Entity(name, null, address, port);
				}
			} else {
				System.err.println("Leader file is empty");
			}
		} catch (Exception e) {
			System.err.println("Error reading leader file: " + e.getMessage());
		}
		return leader;
	}

	public static Entity getLeader(String filename) {
		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
			String line = br.readLine();
			if (line == null) {
				System.err.println("Leader file is empty");
				return null;
			}
			String[] tokens = line.split(",");
			// format of leader.txt is pedrocurto,localhost,5001, e.g.
			if (tokens.length != 3) {
				System.err.println("Invalid leader entry: " + line);
				return null;
			}
			String memberName = tokens[0].trim();
			String address = tokens[1].trim();
			int port = Integer.parseInt(tokens[2].trim());
			//String pubKeyPath = tokens[3].trim();
			//PublicKey pubKey = KeyUtils.readPublicKey(pubKeyPath);
			return new Entity(memberName, null, address, port);
		} catch (Exception e) {
			System.err.println("Error reading leader file: " + e.getMessage());
			return null;
		}
	}

	public static List<Entity> loadMembership(String filename) {
		List<Entity> members = new ArrayList<>();
		//System.out.println("Trying to load from: " + filename);
		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
			String line;
			while ((line = br.readLine()) != null) {
				// skips empty lines and comments
				if (line.trim().isEmpty() || line.startsWith("#"))
					continue;
				// format of membership is name,address,port,pubKeyPath (many lines)
				String[] tokens = line.split(",");
				if (tokens.length != 4) {
					System.err.println("Invalid member entry: " + line);
					continue;
				}
				// gets name, address, port and public key
				String memberName = tokens[0].trim();
				String address = tokens[1].trim();
				int port = Integer.parseInt(tokens[2].trim());
				String pubKeyPath = tokens[3].trim();
				PublicKey pubKey = KeyUtils.readPublicKey(pubKeyPath);
				Entity entity = new Entity(memberName, pubKey, address, port);
				members.add(entity);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		System.out.println("Loaded members: " + members);
		return members;
	}

	public static Block loadGenesisBlock() {
		Path currentDir = Paths.get(System.getProperty("user.dir"));
		Path rootDir = currentDir.getParent();
		Path genesisPath = rootDir.resolve("genesis-file.json");

		// load the json file
		String jsonString = null;
		try {
			jsonString = Files.readString(genesisPath);
		} catch (IOException e) {
			System.err.println("Error reading genesis file: " + e.getMessage());
			return null;
		}
		JsonElement jsonElement = JsonParser.parseString(jsonString);
		JsonObject jsonObject = jsonElement.getAsJsonObject();
		return JsonAdapter.parseBlock(jsonObject, true);
	}

	public static JsonObject getGenesisJsonObject() {
		Path currentDir = Paths.get(System.getProperty("user.dir"));
		Path rootDir = currentDir.getParent();
		Path genesisPath = rootDir.resolve("genesis-file.json");
		try {
			String jsonString = Files.readString(genesisPath);
			return JsonParser.parseString(jsonString).getAsJsonObject();
		} catch (IOException e) {
			System.err.println("Error reading JSON file: " + e.getMessage());
			return null;
		}
	}

	public static JsonObject jsonGetter(JsonObject json, String key) {
		if (json == null) {
			System.err.println("JSON object is null");
			throw new IllegalArgumentException("JSON object is null");
		}
		if (json.has(key)) {
			return json.getAsJsonObject(key);
		} else {
			System.err.println("Key " + key + " not found in JSON object");
			throw new IllegalArgumentException("Key " + key + " not found in JSON object");
		}
	}

	public static boolean saveJsonToFile(JsonObject json, String filename) {
		// convert JSON to string
		Gson gson = new Gson();
		String jsonString = gson.toJson(json);

		// Save to a file
		try (FileWriter file = new FileWriter(filename)) {
			file.write(jsonString);
			file.flush();
			System.out.println("JSON saved successfully!");
			return true;
		} catch (IOException e) {
			System.err.println("JSON save failed!");
			return false;
		}
	}

}
