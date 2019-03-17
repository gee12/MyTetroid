package com.gee12.mytetroid.crypt;

import com.gee12.mytetroid.Utils;
import com.gee12.mytetroid.data.INodeIconLoader;
import com.gee12.mytetroid.data.TetroidNode;
import com.gee12.mytetroid.data.TetroidRecord;

import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.List;

public class CryptManager {

    private static final int CRYPT_CHECK_ROUNDS = 1000;
    private static final int  CRYPT_CHECK_HASH_LEN = 160;
    private static final String SAVED_PASSWORD_CHECKING_LINE = "This string is used for checking middle hash";

//    private static RC5 rc5;
    private static RC5Simple rc5 = new RC5Simple();
    private static int[] cryptKey;
    private static Charset CHARSET_ISO_8859_1 = Charset.forName("ISO-8859-1");

    public static void initFromPass(String pass) {
//        setCryptKeyToMemory(pass);
        // ?
        int[] key = passToKey(pass);
//        // записываем в память
        setCryptKey(key);
//        rc5 = new RC5(key.getBytes());
//        rc5 = new RC5(cryptKey);
//        rc5 = new RC5Simple();
//        rc5.setKey(key);
    }

    public static void initFromMiddleHash(String passHash) {
//        setCryptKeyToMemoryFromMiddleHash(passHash);
        int[] key = middlePassHashToKey(passHash);
//        // записываем в память
        setCryptKey(key);
//        rc5 = new RC5Simple();
//        rc5.setKey(key);
    }

    /***
     * Сверяем пароль с проверочным хешем в database.ini
     * @param pass Введенный пароль в виде строки
     * @param salt Сохраненная соль в Base64
     * @param checkHash Сохраненный хеш пароля в Base64
     * @return
     */
    public static boolean checkPass(String pass, String salt, String checkHash) {
        byte[] saltSigned = Base64.decode(salt.toCharArray());
        byte[] checkHashSigned = Base64.decode(checkHash.toCharArray());
        // получим хэш пароля и соли
        byte[] passHash = null;
        try {
            passHash = calculatePBKDF2Hash(pass, saltSigned);
        } catch (Exception e) {
            addLog(e);
        }
        return (Arrays.equals(checkHashSigned, passHash));
    }

    /**
     * Сравнение сохраненного пароля с проверочными данными
     * @param passHash
     * @param checkData Сохраненный хеш данных в Base64
     * @return
     */
    public static boolean checkMiddlePassHash(String passHash, String checkData) {
        int[] key = middlePassHashToKey(passHash);
        byte[] checDataSigned = Base64.decode(checkData.toCharArray());
        String line = decrypt(key, checDataSigned);
        // сравнение проверочных данных
        return (SAVED_PASSWORD_CHECKING_LINE.equals(line));
    }

//        public static boolean decryptAll(String pass, List<TetroidNode> nodes) {
    public static boolean decryptAll(List<TetroidNode> nodes, boolean isDecryptSubNodes, INodeIconLoader iconLoader) {
        return decryptNodes(nodes, isDecryptSubNodes, iconLoader);
    }

    public static boolean decryptNodes(List<TetroidNode> nodes, boolean isDecryptSubNodes, INodeIconLoader iconLoader) {
        for (TetroidNode node : nodes) {
            if (node.isCrypted())
                decryptNode(node, isDecryptSubNodes, iconLoader);
        }

        return true;
    }

    public static boolean decryptNode(TetroidNode node, boolean isDecryptSubNodes, INodeIconLoader iconLoader) {
        // расшифровываем поля
        decryptNodeFields(node);
        // загружаем иконку
        if (iconLoader != null)
            iconLoader.loadIcon(node);
        // расшифровывать записи сразу или при выделении?
        if (node.getRecordsCount() > 0)
            decryptRecordsFields(node.getRecords());
        // расшифровываем подветки
        if (isDecryptSubNodes && node.getSubNodesCount() > 0)
            decryptNodes(node.getSubNodes(), isDecryptSubNodes, iconLoader);
        return true;
    }

    /**
     * Расшифровка ветки
     * @param node
     * @return
     */
    public static boolean decryptNodeFields(TetroidNode node) {
        node.setName(CryptManager.decryptBase64(cryptKey, node.getName()));
        node.setIconName(CryptManager.decryptBase64(cryptKey, node.getIconName()));
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
        record.setName(CryptManager.decryptBase64(cryptKey, record.getName()));
        record.setAuthor(CryptManager.decryptBase64(cryptKey, record.getAuthor()));
        record.setUrl(CryptManager.decryptBase64(cryptKey, record.getUrl()));
        record.setDecrypted(true);
        return true;
    }

    /**
     * Расшифровка текста
     * @param text
     * @return
     */
    public static String decryptText(byte[] text) {
        return decrypt(cryptKey, text);
    }

    /**
     * Создание хэша пароля для сохранения в файле
     * @param pass
     * @return
     */
    public static String passToHash(String pass) {
        String res=  null;
        try {
            byte[] passHashSigned = calculateMiddleHash(pass);
//            byte[] passHash = Utils.toUnsigned(passHashSigned);
            res = Base64.encodeToString(passHashSigned, false);
        } catch (Exception e) {
            addLog(e);
        }
        return res;
    }

    /**
     * Установка пароля в виде ключа шифрования
     */
//    public static void setCryptKeyToMemory(String pass)
//    {
//        int[] key = passToKey(pass);
//        // записываем в память
//        setCryptKey(key);
//    }
//
//    private static void setCryptKeyToMemoryFromMiddleHash(String passHash)
//    {
//        int[] key = middlePassHashToKey(passHash);
//        // записываем в память
//        setCryptKey(key);
//    }

    private static int[] passToKey(String pass) {
        int[] res = null;
        try {
            // добавляем соль
            byte[] passHashSigned = calculateMiddleHash(pass);
            // преобразуем к MD5 виду
            byte[] keySigned = Utils.toMD5(passHashSigned);
            res = Utils.toUnsigned(keySigned);
        } catch (Exception e) {
            addLog(e);
        }
        return res;
    }

    private static int[] middlePassHashToKey(String passHash) {
        int[] res = null;
        try {
            byte[] passHashSigned = Base64.decode(passHash.toCharArray());
            // преобразуем к MD5 виду
            byte[] keySigned = Utils.toMD5(passHashSigned);
            res = Utils.toUnsigned(keySigned);
        } catch (Exception e) {
            addLog(e);
        }
        return res;
    }

    private static byte[] calculateMiddleHash(String pass) throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        byte[] saltBytes = "^1*My2$Tetra3%_4[5]".getBytes();
        return calculatePBKDF2Hash(pass, saltBytes);
    }

    /**
     * Получение хеша PBKDF2 от пароля и соли
     * @param pass Пароль (строки в Java уже в UTF-8)
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    private static byte[] calculatePBKDF2Hash(String pass, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        // хеш PBKDF2 от пароля и соли
        byte[] signedBytes = PBKDF2.encrypt(pass, salt, CRYPT_CHECK_ROUNDS, CRYPT_CHECK_HASH_LEN);
        return signedBytes;
    }


    /**
     * Шифрование исходной строки в base64
     * @param key
     * @param line Исходня строка
     */
    public static String encrypt(int[] key, String line) {
        return null;
    }

    /**
     * Расшифровка строки в base64
     * @param line Строка в base64
     */
    public static String decryptBase64(int[] key, String line) {
        if (line.length() == 0) {
            return null;
        }
        // преобразование из base64
//        line.toLatin1()
//        toLatin1() returns a Latin-1 (ISO 8859-1) encoded 8-bit string.

//        char[] b1 = line.toCharArray();
//        byte[] b2 = line.getBytes(CHARSET_ISO_8859_1);
//        for (int i = 0; i < b2.length; i++) {
//           if (b2[i] != b1[i]) {
//               int o = 0;
//           }
//        }
//        byte[] bytes = Base64.decode(b2);
        byte[] bytes = Base64.decode(line.toCharArray());
        /*if (bytes == null)
            return null;
//        byte[] res = new byte[bytes.length];
//        rc5.decipherString(bytes, res);
//        return new String(res);
        String res = null;
        try {
//            res = RC5.decryptBase64(bytes, new String (cryptKey));
            byte[] out = rc5.decrypt(bytes);
            if (out != null)
                res = new String(out);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;*/
        return decrypt(key, bytes);
    }

    /**
     * Расшифровка строки в base64
     * @param line Строка в base64
     */
    public static String decrypt(int[] key, String line) {
        if (line.length() == 0) {
            return null;
        }
        return decrypt(key, line.getBytes());
    }

    public static String decrypt(int[] key, byte[] bytes) {
        if (bytes == null)
            return null;
        String res = null;
        //
        rc5.setKey(key);
        try {
            //
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

    private static void addLog(Exception e) {
        e.printStackTrace();
    }
}
