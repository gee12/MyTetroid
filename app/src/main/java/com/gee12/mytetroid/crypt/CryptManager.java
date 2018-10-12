package com.gee12.mytetroid.crypt;

import android.database.CharArrayBuffer;

import com.gee12.mytetroid.Utils;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class CryptManager {

    public static final int CRYPT_CHECK_ROUNDS = 1000;
    public static final int  CRYPT_CHECK_HASH_LEN = 160;


    private static RC5 rc5;
    private static byte[] cryptKey;

    public static void init(String key) {
        rc5 = new RC5(key.getBytes());
    }


    /**
     * Установка пароля в виде ключа шифрования
     */
    static void setCryptKeyToMemory(String password)
    {
        try {
            // добавляем соль
            byte[] middleHash = calculateMiddleHash(password);
            // преобразуем к MD5 виду
            byte[] key = Utils.getMD5(middleHash);
            // записываем в память
            setCryptKey(key);
        } catch (Exception e) {

        }
    }

    /**
     * Добавление соли к паролю
     */
    static byte[] calculateMiddleHash(String password) throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        byte[] salt = "^1*My2$Tetra3%_4[5]".getBytes();
        // Хеш PBKDF2 от пароля и соли
//        Pbkdf2Qt hashAlgorythm;
//        byte[] middleHash=hashAlgorythm.Pbkdf2(password.toUtf8(),
//                salt,
//                CRYPT_CHECK_ROUNDS,
//                CRYPT_CHECK_HASH_LEN);
        return PBKDF2.getEncryptedPassword(password, salt, CRYPT_CHECK_ROUNDS, CRYPT_CHECK_HASH_LEN);
    }


    /**
     * Шифрование исходной строки в base64
     * @param key
     * @param line Исходня строка
     */
    public static String Encrypt(byte[] key, String line) {
        return null;
    }

    /**
     * Расшифровка строки base64
     * @param key
     * @param line Строка в base64
     */
    public static String Decrypt(byte[] key, String line) {
        byte[] res = new byte[line.length()];
        rc5.Decipher(line.getBytes(), res);
        return new String(res);
    }

    static void setCryptKey(byte[] key) {
        cryptKey = key;
    }
}
