package depchain.client.utils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

public class KeyUtils {
	public static PrivateKey readPrivateKey(String pathToFile) throws Exception {
		File file = new File(pathToFile);
		String key = new String(Files.readAllBytes(file.toPath()), Charset.defaultCharset());
		String privateKeyPEM = key
				.replace("-----BEGIN PRIVATE KEY-----", "")
				.replaceAll(System.lineSeparator(), "")
				.replace("-----END PRIVATE KEY-----", "");
		System.out.println("Private key:" + privateKeyPEM);
		byte[] encoded = java.util.Base64.getDecoder().decode(privateKeyPEM);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
		return keyFactory.generatePrivate(keySpec);
	}
}
