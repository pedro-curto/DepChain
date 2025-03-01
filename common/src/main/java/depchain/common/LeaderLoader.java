package depchain.common;

import java.io.BufferedReader;
import java.io.FileReader;

public class LeaderLoader {

	public static Leader leaderLoader(String filename) {
		Leader leader = null;
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
					leader = new Leader(name, address, port);
				}
			} else {
				System.err.println("Leader file is empty");
			}
		} catch (Exception e) {
			System.err.println("Error reading leader file: " + e.getMessage());
		}
		return leader;
	}
}
