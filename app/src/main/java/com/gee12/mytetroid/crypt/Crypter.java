package com.gee12.mytetroid.crypt;

import android.widget.Toast;

import com.gee12.mytetroid.LogManager;
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

public class Crypter {

    private static final int CRYPT_CHECK_ROUNDS = 1000;
    private static final int  CRYPT_CHECK_HASH_LEN = 160;
    private static final String SAVED_PASSWORD_CHECKING_LINE = "This string is used for checking middle hash";

    private static RC5Simple rc5 = new RC5Simple();
    protected static int[] mCryptKey;
    private static final Charset CHARSET_UTF_8 = Charset.forName("UTF-8");
    private static final Charset CHARSET_ISO_8859_1 = Charset.forName("ISO-8859-1");

    /**
     * Для будущего сохранения в настройках.
     * Если поставили галку "Сохранять хеш пароля локально" уже после того,
     * как ввели пароль
     */
    private static String middlePassHash;


    /***
     * Сверяем пароль с проверочным хешем в database.ini
     * @param pass Введенный пароль в виде строки
     * @param salt Сохраненная соль в Base64
     * @param checkHash Сохраненный хеш пароля в Base64
     * @return
     */
    public static boolean checkPass(String pass, String salt, String checkHash) {
        if (salt == null || checkHash == null)
            return false;
        byte[] saltSigned = decodeBase64(salt);
        byte[] checkHashSigned = decodeBase64(checkHash);
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
        byte[] checDataSigned = decodeBase64(checkData);
        String line = decryptString(key, checDataSigned);
        // сравнение проверочных данных
        return (SAVED_PASSWORD_CHECKING_LINE.equals(line));
    }

    /**
     * Шифрование строки.
     * @param text
     * @return
     */
    public static String encryptTextBase64(String text) {
        if (text == null)
            return null;
        byte[] out = encrypt(mCryptKey, text.getBytes(CHARSET_UTF_8));
        if (out != null)
            return Base64.encodeToString(out, false);
        return null;
    }

    public static byte[] encryptTextBase64Bytes(String text) {
        if (text == null)
            return null;
        return encryptBytesBase64Bytes(text.getBytes(CHARSET_UTF_8));
    }

    public static byte[] encryptTextBytes(String text) {
        if (text == null)
            return null;
        byte[] out = encrypt(mCryptKey, text.getBytes(CHARSET_UTF_8));
        return out;
    }

    public static byte[] encryptBytes(byte[] bytes) {
        return encrypt(mCryptKey, bytes);
    }

    public static byte[] encryptBytesBase64Bytes(byte[] bytes) {
        if (bytes == null)
            return null;
        byte[] out = encrypt(mCryptKey, bytes);
        if (out != null)
            return Base64.encodeToByte(out, true);
        return null;
    }

    public static byte[] encrypt(int[] key, byte[] bytes) {
        if (bytes == null)
            return null;
        byte[] res = null;
        rc5.setKey(key);
        try {
            res = rc5.encrypt(bytes);
        } catch (Exception e) {
            addLog(e);
        }
        return res;
    }

    /**
     * Побайтовая зашифровка файла.
     *
     * @param srcFile Исходный файл, который нужно зашифровать
     * @param destFile Результирующий файл, который должен быть зашифрован
     * @return
     * @throws IOException
     */
    public static boolean encryptFile(File srcFile, File destFile) throws IOException {
        int size = (int) srcFile.length();
        byte[] bytes = new byte[size];

        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(srcFile));
        int readed = bis.read(bytes, 0, size);
        bis.close();

        if (readed > 0) {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destFile));
            byte[] data = encryptBytes(bytes);
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
     * Побайтовая "поблочная" зашифровка файла (работает неверно).
     *
     * FIXME: Написать метод RC5Simple.encryptByBlocks() для "поблочного" шифрования массива байт,
     *  когда заранее неизвестно количество блоков (если такое вообще возможно в RC5Simple)
     *
     * @param srcFile
     * @param destFile
     * @return
     * @throws IOException
     */
    public static boolean encryptFileByBlocks(File srcFile, File destFile) throws IOException {
        if (srcFile == null || destFile == null)
            return false;
        try (FileInputStream fis = new FileInputStream(srcFile);
             FileOutputStream fos = new FileOutputStream(destFile)) {

            byte[] buffer = new byte[1024];
            byte[] res;
            rc5.setKey(mCryptKey);
            while (fis.read(buffer) > 0) {
                try {
                    // неверно, т.к. перед каждым блоком вставляется "преамбула",
                    // которая должна быть только 1 раз в начале файла
                    res = rc5.encrypt(buffer);
                } catch (Exception e) {
                    addLog(e);
                    return false;
                }
                if (res != null) {
                    fos.write(res);
                }
            }
        }
        return true;
    }

    /**
     * Расшифровка текста.
     * @param text
     * @return
     */
    public static String decryptText(byte[] text) {
        return decryptString(mCryptKey, text);
    }

    /**
     * Расшифровка массива байт.
     * @param bytes
     * @return
     */
    public static byte[] decryptBytes(byte[] bytes) {
        return decrypt(mCryptKey, bytes);
    }

    /**
     * Расшифровка файла.
     *
     * TODO: Возможно, нужно изменить, чтобы процесс расшифровки происходил поблочно, а не сразу целиком.
     *
     * @param srcFile Исходный зашифрованный файл
     * @param destFile Результирующий файл, который должен быть расшифрован
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
     * Создание хэша пароля для сохранения в файле.
     * @param pass
     * @return
     */
    public static String passToHash(String pass) {
        String res =  null;
        try {
            byte[] passHashSigned = calculateMiddleHash(pass);
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

    protected static int[] passToKey(String pass) {
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

    protected static int[] middlePassHashToKey(String passHash) {
        int[] res = null;
        try {
            byte[] passHashSigned = decodeBase64(passHash);
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
     * Расшифровка строки в base64
     * @param line Строка в base64
     */
    public static String decryptBase64(int[] key, String line) {
        if (line == null || line.length() == 0) {
            return "";
        }
        byte[] bytes = decodeBase64(line);
        return decryptString(key, bytes);
    }

    public static String decryptBase64(String line) {
        return decryptBase64(mCryptKey, line);
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
        } catch (Exception ex) {
            addLog(ex);
        }
        return res;
    }

    public static byte[] decodeBase64(String s) {
        try {
            return Base64.decode(s.toCharArray());
        } catch (Exception ex) {
            addLog(ex);
        }
        return null;
    }

    static void setCryptKey(int[] key) {
        Crypter.mCryptKey = key;
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

    public static int getErrorCode() {
        return rc5.getErrorCode();
    }
}
