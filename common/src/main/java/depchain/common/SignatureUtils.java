package depchain.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

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

	public static boolean verifyDS(byte[] receivedSignature, byte[] bytes, PublicKey publicKey)
			throws Exception {
		// verify the signature with the public key
		Signature sig = Signature.getInstance(SIGNATURE_ALGO);
		sig.initVerify(publicKey);
		sig.update(bytes);
		try {
			return sig.verify(receivedSignature);
		} catch (SignatureException se) {
			System.err.println("Caught exception while verifying " + se);
			return false;
		}
	}


	public static KeyPair getMemberKeyPair(String member) {

		String publicKeyPath = "keys/" + member + "/" + member + ".pubkey";
		String privateKeyPath = "keys/" + member + "/" + member + ".privkey";

		try {
			byte[] pubEncoded = readFile(publicKeyPath);
			X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(pubEncoded);
			KeyFactory keyFacPub = KeyFactory.getInstance(ASYM_ALGO);
			PublicKey pub = keyFacPub.generatePublic(pubSpec);

			byte[] privEncoded = readFile(privateKeyPath);
			PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privEncoded);
			KeyFactory keyFacPriv = KeyFactory.getInstance(ASYM_ALGO);
			PrivateKey priv = keyFacPriv.generatePrivate(privSpec);

			return new KeyPair(pub, priv);
		} catch(Exception e) {
			System.err.println("Error reading key pair: " + member);
			e.printStackTrace();
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
}
