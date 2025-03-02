package depchain.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static depchain.common.KeyUtils.readPrivateKey;
import static depchain.common.KeyUtils.readPublicKey;

public final class SignatureUtils {

	private static final String SIGNATURE_ALGO = "SHA256withRSA";
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
}
