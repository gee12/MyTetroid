package com.gee12.mytetroid.crypt;

import android.widget.Toast;

import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.data.INodeIconLoader;
import com.gee12.mytetroid.data.ITagsParseHandler;
import com.gee12.mytetroid.model.TetroidFile;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.utils.Utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.List;

public class CryptManager {

    private static final int CRYPT_CHECK_ROUNDS = 1000;
    private static final int  CRYPT_CHECK_HASH_LEN = 160;
    private static final String SAVED_PASSWORD_CHECKING_LINE = "This string is used for checking middle hash";

    private static RC5Simple rc5 = new RC5Simple();
    private static int[] cryptKey;
    private static Charset CHARSET_ISO_8859_1 = Charset.forName("ISO-8859-1");
    private static ITagsParseHandler tagsParser;

    /**
     * Для будущего сохранения в настройках.
     * Если поставили галку "Сохранять хеш пароля локально" уже после того,
     * как ввели пароль
     */
    private static String middlePassHash;


    public static void initFromPass(String pass, ITagsParseHandler tagsParser) {
        int[] key = passToKey(pass);
        // записываем в память
        setCryptKey(key);
        CryptManager.tagsParser = tagsParser;
    }

    public static void initFromMiddleHash(String passHash, ITagsParseHandler tagsParser) {
        int[] key = middlePassHashToKey(passHash);
        // записываем в память
        setCryptKey(key);
        CryptManager.tagsParser = tagsParser;
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
        String line = decryptString(key, checDataSigned);
        // сравнение проверочных данных
        return (SAVED_PASSWORD_CHECKING_LINE.equals(line));
    }

    public static boolean decryptAll(List<TetroidNode> nodes, boolean isDecryptSubNodes, INodeIconLoader iconLoader) {
        return decryptNodes(nodes, isDecryptSubNodes, iconLoader);
    }

    public static boolean decryptNodes(List<TetroidNode> nodes, boolean isDecryptSubNodes, INodeIconLoader iconLoader) {
        boolean res = true;
        for (TetroidNode node : nodes) {
            if (node.isCrypted())
                res = res & decryptNode(node, isDecryptSubNodes, iconLoader);
        }
        return res;
    }

    public static boolean decryptNode(TetroidNode node, boolean isDecryptSubNodes, INodeIconLoader iconLoader) {
        boolean res;
        // расшифровываем поля
        res = decryptNodeFields(node);
        // загружаем иконку
        if (iconLoader != null)
            iconLoader.loadIcon(node);
        // расшифровывать список записей сразу или при выделении?
        // пока сразу
        if (node.getRecordsCount() > 0)
            res = res & decryptRecordsFields(node.getRecords());
        // расшифровываем подветки
        if (isDecryptSubNodes && node.getSubNodesCount() > 0)
            res = res & decryptNodes(node.getSubNodes(), isDecryptSubNodes, iconLoader);
        return res;
    }

    /**
     * Расшифровка ветки
     * @param node
     * @return
     */
    public static boolean decryptNodeFields(TetroidNode node) {
        boolean res;
        // name
        String temp = CryptManager.decryptBase64(cryptKey, node.getName());
        res = (temp != null);
        if (res) {
            node.setName(temp);
        }
        // icon
        temp = CryptManager.decryptBase64(cryptKey, node.getIconName());
        res = res & (temp != null);
        if (temp != null) {
            node.setIconName(temp);
        }
        // decryption result
        node.setDecrypted(res);
        return res;
    }

    /**
     * Расшифровка списка записей
     * @param records
     * @return
     */
    public static boolean decryptRecordsFields(List<TetroidRecord> records) {
        boolean res = true;
        for (TetroidRecord record : records) {
            if (record.isCrypted()) {
                res = res & decryptRecordFields(record);
                if (record.getAttachedFilesCount() > 0)
                    for (TetroidFile file : record.getAttachedFiles()) {
                        res = res & decryptFileName(file);
                    }
            }
        }
        return res;
    }

    /**
     * Расшифровка записи
     * Расшифровать сразу и записи, или при выделении ветки?
     * @param record
     * @return
     */
    public static boolean decryptRecordFields(TetroidRecord record) {
        boolean res;
//        record.setName(CryptManager.decryptBase64(cryptKey, record.getName()));
        String temp = CryptManager.decryptBase64(cryptKey, record.getName());
        res = (temp != null);
        if (res) {
            record.setName(temp);
        }
//        record.setTagsString(CryptManager.decryptBase64(cryptKey, record.getTagsString()));
        temp = CryptManager.decryptBase64(cryptKey, record.getTagsString());
        res = res & (temp != null);
        if (temp != null) {
            record.setTagsString(temp);
            tagsParser.parseRecordTags(record, temp);
        }
//        record.setAuthor(CryptManager.decryptBase64(cryptKey, record.getAuthor()));
        temp = CryptManager.decryptBase64(cryptKey, record.getAuthor());
        res = res & (temp != null);
        if (temp != null) {
            record.setAuthor(temp);
        }
//        record.setUrl(CryptManager.decryptBase64(cryptKey, record.getUrl()));
        temp = CryptManager.decryptBase64(cryptKey, record.getUrl());
        res = res & (temp != null);
        if (temp != null) {
            record.setUrl(temp);
        }
        record.setDecrypted(res);
        return res;
    }

    /**
     * Расшифровка имени файла
     * @param file
     * @return
     */
    public static boolean decryptFileName(TetroidFile file) {
//        file.setName(CryptManager.decryptBase64(cryptKey, file.getName()));
        String temp = CryptManager.decryptBase64(cryptKey, file.getName());
        boolean res = (temp != null);
        if (res) {
            file.setName(temp);
        }
        return res;
    }

    /**
     * Расшифровка текста
     * @param text
     * @return
     */
    public static String decryptText(byte[] text) {
        return decryptString(cryptKey, text);
    }

    /**
     * Расшифровка массива байт
     * @param bytes
     * @return
     */
    public static byte[] decryptBytes(byte[] bytes) {
        return decrypt(cryptKey, bytes);
    }

    /**
     * Расшифровка файла
     *
     * Возможно, нужно изменить, чтобы процесс расшифровки происходил поблочно, а не сразу целиком
     *
     * @param srcFile
     * @param destFile
     * @return
     */
    public static boolean decryptFile(File srcFile, File destFile) throws IOException {
        int size = (int) srcFile.length();
        byte[] bytes = new byte[size];

        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(srcFile));
        int readed = bis.read(bytes, 0, size);
        bis.close();

        if (readed > 0) {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destFile));
            byte[] data = decryptBytes(bytes);
            if (data == null) {
                bos.close();
                return false;
            }
            bos.write(data);
            bos.flush();
            bos.close();
        } else {
            addLog("File is empty");
        }
        return true;
    }

    /**
     * Создание хэша пароля для сохранения в файле
     * @param pass
     * @return
     */
    public static String passToHash(String pass) {
        String res =  null;
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
        return PBKDF2.encrypt(pass, salt, CRYPT_CHECK_ROUNDS, CRYPT_CHECK_HASH_LEN);
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
            return "";
        }
        byte[] bytes = Base64.decode(line.toCharArray());
        return decryptString(key, bytes);
    }

    public static String decryptString(int[] key, byte[] bytes) {
        byte[] out = decrypt(key, bytes);
        if (out != null)
            // java.lang.OutOfMemoryError..
            return new String(out);
        return null;
    }

    public static byte[] decrypt(int[] key, byte[] bytes) {
        if (bytes == null)
            return null;
        byte[] res = null;
        rc5.setKey(key);
        try {
            res = rc5.decrypt(bytes);
        } catch (Exception e) {
            addLog(e);
        }
        return res;
    }

//    public static String decryptString(int[] key, byte[] bytes) {
//        if (bytes == null)
//            return null;
//        String res = null;
//        rc5.setKey(key);
//        try {
//            res = rc5.decryptString(bytes);
//        } catch (Exception e) {
//            addLog(e);
//        }
//        return res;
//    }

    /**
     * TODO: реализовать шифровку данных.
     * @param text
     * @return
     */
    public static String cryptText(String text) {

        return text;
    }

    static void setCryptKey(int[] key) {
        CryptManager.cryptKey = key;
    }

    public static void setMiddlePassHash(String passHash) {
        middlePassHash = passHash;
    }

    public static String getMiddlePassHash() {
        return middlePassHash;
    }

    private static void addLog(String s) {
        LogManager.addLog(s, Toast.LENGTH_LONG);
    }

    private static void addLog(Exception e) {
        LogManager.addLog(e, Toast.LENGTH_LONG);
    }
}
