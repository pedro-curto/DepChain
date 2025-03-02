package depchain.member.membership;

import java.io.BufferedReader;
import java.io.FileReader;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import depchain.common.KeyUtils;

public class MembershipManager {
	private final String LEADER_FILE = "membership/leader.txt";

	public static List<MemberData> loadMembership(String filename) throws Exception {
		List<MemberData> members = new ArrayList<>();
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
				MemberData md = new MemberData(memberName, pubKey, address, port);
				members.add(md);
			}
		}
		return members;
	}

	// reads leader from the leader file
	public static MemberData getLeader() {
		try (BufferedReader br = new BufferedReader(new FileReader("membership/leader.txt"))) {
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
			return new MemberData(memberName, null, address, port);
		} catch (Exception e) {
			System.err.println("Error reading leader file: " + e.getMessage());
			return null;
		}
	}
}
