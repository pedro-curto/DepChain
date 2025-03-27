package depchain.common;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class KeyUtils {
	public static PrivateKey readPrivateKey(String pathToFile) throws Exception {
		File file = new File(pathToFile);
		String key = new String(Files.readAllBytes(file.toPath()), Charset.defaultCharset());
		String privateKeyPEM = key
				.replace("-----BEGIN PRIVATE KEY-----", "")
				.replaceAll(System.lineSeparator(), "")
				.replace("-----END PRIVATE KEY-----", "");
		//System.out.println("Private key:" + privateKeyPEM);
		byte[] encoded = java.util.Base64.getDecoder().decode(privateKeyPEM);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
		return keyFactory.generatePrivate(keySpec);
	}

	public static PublicKey readPublicKey(String pubKeyPath) throws Exception {
		File file = new File(pubKeyPath);
		String key = new String(Files.readAllBytes(file.toPath()), Charset.defaultCharset());
		String publicKeyPEM = key
				.replace("-----BEGIN PUBLIC KEY-----", "")
				.replaceAll(System.lineSeparator(), "")
				.replace("-----END PUBLIC KEY-----", "");
		//System.out.println("Public key:" + publicKeyPEM);
		byte[] encoded = java.util.Base64.getDecoder().decode(publicKeyPEM);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
		return keyFactory.generatePublic(keySpec);
	}

	public static PublicKey getPublicKeyFromString(String publicKeyString) throws Exception {
		byte[] encoded = java.util.Base64.getDecoder().decode(publicKeyString);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		return keyFactory.generatePublic(new X509EncodedKeySpec(encoded));
	}

	public static String hashPublicKey(PublicKey publicKey) throws Exception {
		byte[] encoded = publicKey.getEncoded();
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		digest.update(encoded);
		byte[] hash = digest.digest();
		return java.util.Base64.getEncoder().encodeToString(hash);
	}
}
