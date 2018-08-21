package io.particle.android.sdk.utils;

import org.spongycastle.asn1.ASN1InputStream;
import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.DLSequence;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Locale;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import okio.ByteString;


@ParametersAreNonnullByDefault
public class Crypto {


    public static class CryptoException extends Exception {
        public CryptoException(Throwable cause) {
            super(cause);
        }
    }


    private static final TLog log = TLog.get(Crypto.class);


    public static PublicKey readPublicKeyFromHexEncodedDerString(String hexString)
            throws CryptoException {
        byte[] rawBytes = ByteString.decodeHex(hexString).toByteArray();
        return buildPublicKey(rawBytes);
    }

    public static String encryptAndEncodeToHex(String inputString, PublicKey publicKey)
            throws CryptoException {
        Charset utf8 = Charset.forName("UTF-8");
        byte[] asBytes = inputString.getBytes(utf8);
        byte[] encryptedBytes = encryptWithKey(asBytes, publicKey);
        String hex = ByteString.of(encryptedBytes).hex();
        // forcing lowercase here because of a bug in the early firmware that didn't accept
        // hex encoding in uppercase
        return hex.toLowerCase(Locale.ROOT);
    }

    static byte[] encryptWithKey(byte[] inputData, PublicKey publicKey) throws CryptoException {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(inputData);

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException
                | InvalidKeyException | BadPaddingException e) {
            log.e("Error while encrypting bytes: ", e);
            throw new CryptoException(e);
        }
    }

    static PublicKey buildPublicKey(byte[] rawBytes) throws CryptoException {
        try {
            //FIXME replacing X509EncodedKeySpec because of problem with 8.1
            //Since 8.1 Bouncycastle cryptography was replaced with implementation from Conscrypt
            //https://developer.android.com/about/versions/oreo/android-8.1.html
            //either it's a bug in Conscrypt, our public key DER structure or use of X509EncodedKeySpec changed
            //alternative needed as this adds expensive Spongycastle dependence
            ASN1InputStream bIn = new ASN1InputStream(new ByteArrayInputStream(rawBytes));
            SubjectPublicKeyInfo info = SubjectPublicKeyInfo
                    .getInstance(new ASN1InputStream(bIn.readObject().getEncoded()).readObject());
            DLSequence dlSequence = (DLSequence) ASN1Primitive.fromByteArray(info.getPublicKeyData().getBytes());
            BigInteger modulus = ((ASN1Integer) dlSequence.getObjectAt(0)).getPositiveValue();
            BigInteger exponent = ((ASN1Integer) dlSequence.getObjectAt(1)).getPositiveValue();

            RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
            KeyFactory kf = getRSAKeyFactory();
            return kf.generatePublic(spec);
        } catch (InvalidKeySpecException | IOException e) {
            throw new CryptoException(e);
        }
    }

    static KeyFactory getRSAKeyFactory() {
        try {
            return KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            // I'm ignoring this.  There isn't going to be an Android
            // implementation without RSA.  (In fact, I'm fairly certain
            // that the CDD *requires* it.)
            throw new IllegalStateException(
                    "This should be impossible, but there is no RSA impl on this device", e);
        }
    }

}
