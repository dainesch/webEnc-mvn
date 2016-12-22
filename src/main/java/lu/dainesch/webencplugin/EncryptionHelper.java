package lu.dainesch.webencplugin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class EncryptionHelper {

    private static final int SALTSIZE = 8;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PROVIDER = "BC";
    private static final String ENCALG = "AES";
    private static final String KEYALG = "PBKDF2WithHmacSHA256";
    private static final String CIPHER = "AES/CBC/PKCS5Padding";
    private static final String HASHALG = "SHA-256";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final int iterations;
    private final int keySize;

    public EncryptionHelper(int iterations, int keySize) {
        this.iterations = iterations;
        this.keySize = keySize;
    }

    public void encrypt(String passphrase, Path input, Path output) throws IOException {
        if (passphrase == null || Files.notExists(input)) {
            return;
        }

        byte[] salt = new byte[SALTSIZE];
        RANDOM.nextBytes(salt);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KEYALG);
            KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, iterations, keySize);
            SecretKey key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), ENCALG);

            Cipher cipher = Cipher.getInstance(CIPHER, PROVIDER);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            AlgorithmParameters params = cipher.getParameters();

            byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();

            byte[] buff = new byte[PluginConstants.BUFFSIZE];

            Files.deleteIfExists(output);

            try (InputStream in = Files.newInputStream(input);
                    OutputStream out = Files.newOutputStream(output, StandardOpenOption.CREATE)) {
                out.write(salt);
                out.write(iv);
                int read;
                while ((read = in.read(buff)) != -1) {
                    byte[] toWrite = cipher.update(buff, 0, read);
                    if (toWrite!=null) {
                        out.write(toWrite);
                    }
                }
                out.write(cipher.doFinal());

            }

        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | NoSuchPaddingException
                | InvalidParameterSpecException | UnsupportedEncodingException | IllegalBlockSizeException
                | BadPaddingException | NoSuchProviderException ex) {
            throw new IOException(ex);
        }
    }

    public byte[] encrypt(String passphrase, byte[] input) throws IOException {
        if (passphrase == null || input == null || input.length <= 0) {
            return null;
        }

        byte[] salt = new byte[SALTSIZE];
        RANDOM.nextBytes(salt);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KEYALG);
            KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, iterations, keySize);
            SecretKey key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), ENCALG);

            Cipher cipher = Cipher.getInstance(CIPHER, PROVIDER);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            AlgorithmParameters params = cipher.getParameters();

            byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                out.write(salt);
                out.write(iv);
                out.write(cipher.doFinal(input));
                out.flush();
                return out.toByteArray();
            }

        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | NoSuchPaddingException
                | InvalidParameterSpecException | UnsupportedEncodingException | IllegalBlockSizeException
                | BadPaddingException | NoSuchProviderException ex) {
            throw new IOException(ex);
        }
    }

    public byte[] getRandomArray(int size) {
        byte[] ret = new byte[size];
        RANDOM.nextBytes(ret);
        return ret;
    }

    public String hashBytes(byte[] input) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASHALG);
            return Base64.getEncoder().encodeToString(digest.digest(input));
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException("Error hashing", ex);
        }
    }

}
