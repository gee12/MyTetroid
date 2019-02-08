package com.gee12.mytetroid.crypt;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PBKDF2 {

//    public static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";   // Supported API Levels 26+
    public static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA1";     // Supported API Levels 10+

    public static byte[] encrypt(String password, byte[] salt, int iterations, int derivedKeyLength) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, derivedKeyLength*8);
        SecretKeyFactory f = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        return f.generateSecret(spec).getEncoded();
    }


    /**
     * Использование классов BouncyCastle напрямую
     */
   /* public static byte[] encrypt(String password, byte[] salt,  int iterations,  int derivedKeyLength) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PKCS5S2ParametersGenerator gen = new PKCS5S2ParametersGenerator(new SHA256Digest());
        gen.init("password".getBytes("UTF-8"), "salt".getBytes(), 4096);
        byte[] dk = ((KeyParameter) gen.generateDerivedParameters(256)).getKey();

        return f.generateSecret(spec).getEncoded();
    }*/
}
