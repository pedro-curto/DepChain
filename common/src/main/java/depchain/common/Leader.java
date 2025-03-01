package depchain.common;

public class Leader {
	private String name;
	private String address;
	private int port;

	public Leader(String name, String address, int port) {
		this.name = name;
		this.address = address;
		this.port = port;
	}

	public String getName() {
		return name;
	}

	public String getAddress() {
		return address;
	}

	public int getPort() {
		return port;
	}

	@Override
	public String toString() {
		return name + " " + address + " " + port;
	}

}
