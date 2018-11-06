package com.gee12.mytetroid.crypt;

import com.gee12.mytetroid.Utils;
import com.gee12.mytetroid.data.TetroidNode;
import com.gee12.mytetroid.data.TetroidRecord;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

public class CryptManager {

    public static final int CRYPT_CHECK_ROUNDS = 1000;
    public static final int  CRYPT_CHECK_HASH_LEN = 160;


    private static RC5 rc5;
    private static byte[] cryptKey;

    public static void init(String pass) {
        setCryptKeyToMemory(pass);
//        rc5 = new RC5(key.getBytes());
        rc5 = new RC5(cryptKey);
    }

//    public static boolean decryptAll(String pass, List<TetroidNode> nodes) {
    public static boolean decryptAll(List<TetroidNode> nodes) {
        // читаем соль из database.ini ?

//        setCryptKeyToMemory(pass);

        return decryptNodes(nodes);
    }

    public static boolean decryptNodes(List<TetroidNode> nodes) {
        for (TetroidNode node : nodes) {
            if (node.isCrypted())
                decryptNode(node);
        }

        return true;
    }

    public static boolean decryptNode(TetroidNode node) {
        // расшифровываем поля
        decryptNodeFields(node);
        // расшифровывать записи сразу или при выделении?
        if (node.getRecordsCount() > 0)
            decryptRecordsFields(node.getRecords());
        // расшифровываем подветки
        if (node.getSubNodesCount() > 0)
            decryptNodes(node.getSubNodes());
        return true;
    }

    /**
     * Расшифровка ветки
     * @param node
     * @return
     */
    public static boolean decryptNodeFields(TetroidNode node) {
        node.setName(CryptManager.decrypt(node.getName()));
        node.setIconName(CryptManager.decrypt(node.getIconName()));
        node.setDecrypted(true);
        return true;
    }

    /**
     * Расшифровка списка записей
     * @param records
     * @return
     */
    public static boolean decryptRecordsFields(List<TetroidRecord> records) {
        for (TetroidRecord record : records) {
            if (record.isCrypted())
                decryptRecordFields(record);
        }

        return true;
    }

    /**
     * Расшифровка записи
     * Расшифровать сразу и записи, или при выделении ветки?
     * @param record
     * @return
     */
    public static boolean decryptRecordFields(TetroidRecord record) {
        record.setName(CryptManager.decrypt(record.getName()));
        record.setAuthor(CryptManager.decrypt(record.getAuthor()));
        record.setUrl(CryptManager.decrypt(record.getUrl()));
        record.setDecrypted(true);
        return true;
    }

    public static boolean decryptRecordContent(TetroidRecord record) {
        // читаем содержимое файла

        // расшифровуем
        return false;
    }

    /**
     * Установка пароля в виде ключа шифрования
     */
    public static void setCryptKeyToMemory(String password)
    {
        try {
            // добавляем соль
            byte[] middleHash = calculateMiddleHash(password);
            // преобразуем к MD5 виду
            byte[] key = Utils.toMD5(middleHash);
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
    public static String encrypt(byte[] key, String line) {
        return null;
    }

    /**
     * Расшифровка строки в base64
     * @param line Строка в base64
     */
    public static String decrypt(String line) {
    /*    return decryptNodes(cryptKey, line);
    }

    /**
     * Расшифровка строки base64
     * @param key
     * @param line Строка в base64
     */
    /*public static String decryptNodes(byte[] key, String line) {*/
        if (line.length() == 0) {
            return null;
        }
        byte[] res = new byte[line.length()];

        // преобразование из Base64
        byte[] bytes = Base64.decode(line);


        rc5.decipher(bytes, res);
        return new String(res);
    }

    static void setCryptKey(byte[] key) {
        cryptKey = key;
    }
}
