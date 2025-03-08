package depchain.common;

import depchain.common.domain.Entity;

import java.io.BufferedReader;
import java.io.FileReader;
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

	public static List<Entity> loadMembership(String filename) throws Exception {
		List<Entity> members = new ArrayList<>();
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
		}
		return members;
	}
}
