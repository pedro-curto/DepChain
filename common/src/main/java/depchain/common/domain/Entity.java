package depchain.common.domain;

import java.security.PublicKey;

public class Entity {

	private String entityName;
	private PublicKey publicKey;
	private String address;
	private int port;

	public Entity(String entityName, PublicKey publicKey, String address, int port) {
		this.publicKey = publicKey;
		this.entityName = entityName;
		this.address = address;
		this.port = port;
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}

	public int getPort() {
		return port;
	}

	public String getEntityName() {
		return entityName;
	}

	public String getAddress() {
		return address;
	}

	@Override
	public String toString() {
		return "entityName='" + entityName + '\'' +
				", address='" + address + '\'' +
				", port=" + port;
	}
}
