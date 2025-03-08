package depchain.common;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.util.Arrays;
import java.util.Base64;

import static depchain.common.KeyUtils.readPrivateKey;
import static depchain.common.KeyUtils.readPublicKey;

public final class Security {

	private static final String SIGNATURE_ALGO = "SHA256withRSA";
	private static final String HMAC_ALGO = "HmacSHA256";
	private static final String ASYM_ALGO = "RSA";

	/** Calculates digital signature from text. */
	public static String makeDS(String data, PrivateKey privateKey) throws Exception {
		// get a signature object and sign the plain text with the private key
		Signature sig = Signature.getInstance(SIGNATURE_ALGO);
		sig.initSign(privateKey);
		sig.update(data.getBytes());
		byte[] signature = sig.sign();
		return Base64.getEncoder().encodeToString(signature);
	}

	public static boolean verifyDS(String receivedSignature, String data, PublicKey publicKey) throws Exception {
		// verify the signature with the public key
		Signature sig = Signature.getInstance(SIGNATURE_ALGO);
		sig.initVerify(publicKey);
		sig.update(data.getBytes());
		byte[] signatureBytes = Base64.getDecoder().decode(receivedSignature);
		try {
			return sig.verify(signatureBytes);
		} catch (SignatureException se) {
			System.err.println("Caught exception while verifying " + se);
			return false;
		}
	}

	public static KeyPair getMemberKeyPair(String member) {
		// reads both PEM keys
		String publicKeyPath = "membership/" + member + "/" + member + ".pubkey";
		String privateKeyPath = "membership/" + member + "/" + member + ".privkey";

		try {
			PublicKey pub = readPublicKey(publicKeyPath);
			PrivateKey priv = readPrivateKey(privateKeyPath);
			return new KeyPair(pub, priv);
		} catch(Exception e) {
			System.err.println("Error reading key pair at " + "membership/" + member + "/" + member + ".pubkey");
		}
		return null;
	}

	private static byte[] readFile(String path) throws IOException {
		FileInputStream fis = new FileInputStream(path);
		byte[] content = new byte[fis.available()];
		fis.read(content);
		fis.close();
		return content;
	}

	public static PublicKey getMemberPublicKey(String senderId) {
		String publicKeyPath = "membership/" + senderId + "/" + senderId + ".pubkey";
		try {
			return readPublicKey(publicKeyPath);
		} catch (Exception e) {
			System.err.println("Error reading public key at " + publicKeyPath);
		}
		return null;
	}

	public static SecretKey generateSecretKey() {
		try {
			KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
			keyGenerator.init(256);
			return keyGenerator.generateKey();
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Error: Algorithm not found.");
		}
		return null;
	}

	public static byte[] encryptSymKeyWithAsymKey(SecretKey symKey, PublicKey pubKey) {
		try {
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, pubKey);
			return cipher.doFinal(symKey.getEncoded());
		} catch (Exception e) {
			System.err.println("Error: Encrypting symmetric key");
		}
		return null;
	}

	public static SecretKey decryptSymKey(String encryptedKey, PrivateKey privateKey) {
		try {
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			byte[] byteKey = cipher.doFinal(Base64.getDecoder().decode(encryptedKey));
			return new SecretKeySpec(byteKey, "AES");
		} catch (Exception e) {
			System.err.println("Error: Decrypting symmetric key");
		}
		return null;
	}

	public static String generateHMAC(String data, SecretKey key) {
		try {
			Mac authenticator = Mac.getInstance(HMAC_ALGO);
			authenticator.init(key);
			byte[] msgAuthenticator = authenticator.doFinal(data.getBytes());
			return Base64.getEncoder().encodeToString(msgAuthenticator);
		} catch (Exception e) {
			System.err.println("Error: Generating HMAC");
		}
		return null;
	}

	public static boolean checkHMAC(String data, SecretKey key, String hmacToCompare) {
		try {
			Mac authenticator = Mac.getInstance(HMAC_ALGO);
			authenticator.init(key);
			byte[] computedHMAC = authenticator.doFinal(data.getBytes());
			byte[] receivedHMAC = Base64.getDecoder().decode(hmacToCompare);
			return Arrays.equals(computedHMAC, receivedHMAC);
		} catch (Exception e) {
			System.err.println("Error: Checking HMAC");
		}
		return false;
	}

}