package com.gee12.mytetroid.crypt;

import com.gee12.mytetroid.Utils;
import com.gee12.mytetroid.data.TetroidNode;
import com.gee12.mytetroid.data.TetroidRecord;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

public class CryptManager {

    public static final int CRYPT_CHECK_ROUNDS = 1000;
    public static final int  CRYPT_CHECK_HASH_LEN = 160;


//    private static RC5 rc5;
    private static RC5Simple rc5;
    private static int[] cryptKey;
    private static Charset CHARSET_ISO_8859_1 = Charset.forName("ISO-8859-1");

    public static void init(String pass) {
        setCryptKeyToMemory(pass);
//        rc5 = new RC5(key.getBytes());
//        rc5 = new RC5(cryptKey);
        rc5 = new RC5Simple();
        rc5.setKey(cryptKey);
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
            byte[] keySigned = Utils.toMD5(middleHash);
            int[] keyUnsigned = Utils.toUnsigned(keySigned);
            // записываем в память
            setCryptKey(keyUnsigned);
        } catch (Exception e) {

        }
    }

    /**
     * Получение хеша PBKDF2 от пароля и соли
     * @param password Пароль (строки в Java уже в UTF-8)
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    static byte[] calculateMiddleHash(String password) throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        byte[] salt1 = "^1*My2$Tetra3%_4[5]".getBytes();
//        byte[] salt2 = "^1*My2$Tetra3%_4[5]".getBytes(CHARSET_ISO_8859_1);
//        for (int i = 0; i < salt1.length; i++) {
//            if (salt1[i] != salt2[i]) {
//                int o = 0;
//            }
//        }
        byte[] signedBytes = PBKDF2.encrypt(password, salt1, CRYPT_CHECK_ROUNDS, CRYPT_CHECK_HASH_LEN);
//        return Utils.toUnsigned(signedBytes);
        return signedBytes;
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
        // преобразование из base64
//        line.toLatin1()
//        toLatin1() returns a Latin-1 (ISO 8859-1) encoded 8-bit string.

        char[] b1 = line.toCharArray();
//        byte[] b2 = line.getBytes(CHARSET_ISO_8859_1);
//        for (int i = 0; i < b2.length; i++) {
//           if (b2[i] != b1[i]) {
//               int o = 0;
//           }
//        }
//        byte[] bytes = Base64.decode(b2);
        byte[] bytes = Base64.decode(line);
        if (bytes == null)
            return null;
//        byte[] res = new byte[bytes.length];
//        rc5.decipherString(bytes, res);
//        return new String(res);
        String res = null;
        try {
//            res = RC5.decrypt(bytes, new String (cryptKey));
            byte[] out = rc5.decrypt(bytes);
            if (out != null)
                res = new String(out);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    static void setCryptKey(int[] key) {
        cryptKey = key;
    }
}
