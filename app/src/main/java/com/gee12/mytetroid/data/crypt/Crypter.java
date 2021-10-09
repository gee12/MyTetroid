package com.gee12.mytetroid.data.crypt;

import com.gee12.mytetroid.logs.ITetroidLogger;
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
    protected static final String SAVED_PASSWORD_CHECKING_LINE = "This string is used for checking middle hash";
    private static final Charset CHARSET_UTF_8 = Charset.forName("UTF-8");
    private static final Charset CHARSET_ISO_8859_1 = Charset.forName("ISO-8859-1");

    /**
     *
     */
    private final ITetroidLogger mLogger;

    /**
     * Реализация алгритма шифрования RC5.
     */
    private final RC5Simple rc5;

    /**
     * Ключ шифрования.
     */
    protected int[] mCryptKey;

    /**
     * Промежуточный хэш пароля для локального хранения на устройстве.
     */
    private String mMiddlePassHash;

    /**
     *
     * @param logger
     */
    public Crypter(ITetroidLogger logger) {
        this.mLogger = logger;
        this.rc5 = new RC5Simple(logger);
    }

    /***
     * Сверяем пароль с проверочным хешем в database.ini
     * @param pass Введенный пароль в виде строки
     * @param checkSalt Сохраненная соль в Base64
     * @param checkHash Сохраненный хеш пароля в Base64
     * @return
     */
    public boolean checkPass(String pass, String checkSalt, String checkHash) {
        if (checkSalt == null || checkHash == null)
            return false;
        byte[] checkSaltSigned = decodeBase64(checkSalt);
        byte[] checkHashSigned = decodeBase64(checkHash);
        // получим хэш пароля и соли
        byte[] passHash = null;
        try {
            passHash = calculatePBKDF2Hash(pass, checkSaltSigned);
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
    public boolean checkMiddlePassHash(String passHash, String checkData) {
        int[] key = middlePassHashToKey(passHash);
        byte[] checkDataSigned = decodeBase64(checkData);
        String line = decryptString(key, checkDataSigned);
        // сравнение проверочных данных
        return (SAVED_PASSWORD_CHECKING_LINE.equals(line));
    }

    /**
     * Получение зашифрованной проверочной строки промежуточного хэша пароля.
     * @param passHash
     * @return
     */
    public String createMiddlePassHashCheckData(String passHash) {
        int[] key = middlePassHashToKey(passHash);
        byte[] out = encrypt(key, SAVED_PASSWORD_CHECKING_LINE.getBytes(CHARSET_UTF_8));
        return  (out != null) ? Base64.encodeToString(out, false) : null;
    }

    /**
     * Шифрование строки.
     * @param text
     * @return
     */
    public String encryptTextBase64(String text) {
        if (text == null)
            return null;
        byte[] out = encrypt(mCryptKey, text.getBytes(CHARSET_UTF_8));
        if (out != null)
            return Base64.encodeToString(out, false);
        return null;
    }

    public byte[] encryptTextBase64Bytes(String text) {
        if (text == null)
            return null;
        return encryptBytesBase64Bytes(text.getBytes(CHARSET_UTF_8));
    }

    public byte[] encryptTextBytes(String text) {
        if (text == null)
            return null;
        byte[] out = encrypt(mCryptKey, text.getBytes(CHARSET_UTF_8));
        return out;
    }

    public byte[] encryptBytes(byte[] bytes) {
        return encrypt(mCryptKey, bytes);
    }

    public byte[] encryptBytesBase64Bytes(byte[] bytes) {
        if (bytes == null)
            return null;
        byte[] out = encrypt(mCryptKey, bytes);
        if (out != null)
            return Base64.encodeToByte(out, true);
        return null;
    }

    public byte[] encrypt(int[] key, byte[] bytes) {
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
     * Расшифровка текста.
     * @param text
     * @return
     */
    public String decryptText(byte[] text) {
        return decryptString(mCryptKey, text);
    }

    /**
     * Расшифровка массива байт.
     * @param bytes
     * @return
     */
    public byte[] decryptBytes(byte[] bytes) {
        return decrypt(mCryptKey, bytes);
    }

    /**
     * Зашифровка или расшифровка файла.
     *
     * TODO: Возможно, нужно изменить, чтобы процесс расшифровки происходил поблочно, а не сразу целиком.
     *
     * @param srcFile Исходный зашифрованный файл
     * @param destFile Результирующий файл, который должен быть расшифрован
     * @param isEncrypt Если true - зашифровываем файл, иначе - расшифровываем
     * @return
     */
    public boolean encryptDecryptFile(File srcFile, File destFile, boolean isEncrypt) throws IOException {
        int size = (int) srcFile.length();
        byte[] bytes = new byte[size];

        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(srcFile));
        int readed = bis.read(bytes, 0, size);
        bis.close();

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destFile))) {
            if (readed > 0) {
                byte[] data = (isEncrypt) ? encryptBytes(bytes) : decryptBytes(bytes);
                if (data == null) {
                    bos.close();
                    return false;
                }
                bos.write(data);
                bos.flush();
            } else {
                addLog("File " + srcFile.getAbsolutePath() + " is empty");
            }
        }
        return true;
    }

    /**
     * Создание хэша пароля для сохранения в файле.
     * @param pass
     * @return
     */
    public String passToHash(String pass) {
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
     * Установка пароля в виде ключа шифрования.
     */
    protected int[] passToKey(String pass) {
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

    protected int[] middlePassHashToKey(String passHash) {
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
    public static byte[] calculatePBKDF2Hash(String pass, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return PBKDF2.encrypt(pass, salt, CRYPT_CHECK_ROUNDS, CRYPT_CHECK_HASH_LEN);
    }

    /**
     * Расшифровка строки в base64
     * @param line Строка в base64
     */
    public String decryptBase64(int[] key, String line) {
        if (line == null || line.length() == 0) {
            return "";
        }
        byte[] bytes = decodeBase64(line);
        return decryptString(key, bytes);
    }

    public String decryptBase64(String line) {
        return decryptBase64(mCryptKey, line);
    }

    public String decryptString(int[] key, byte[] bytes) {
        byte[] out = decrypt(key, bytes);
        if (out != null)
            // java.lang.OutOfMemoryError..
            return new String(out);
        return null;
    }

    public byte[] decrypt(int[] key, byte[] bytes) {
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

    public byte[] decodeBase64(String s) {
        try {
            return Base64.decode(s.toCharArray());
        } catch (Exception ex) {
            addLog(ex);
        }
        return null;
    }

    public void setCryptKey(int[] key) {
        this.mCryptKey = key;
    }

    public void setMiddlePassHash(String passHash) {
        this.mMiddlePassHash = passHash;
    }

    public String getMiddlePassHash() {
        return mMiddlePassHash;
    }

    private void addLog(String s) {
        if (mLogger != null) {
            mLogger.log(s, false);
        }
    }

    private void addLog(Exception ex) {
        if (mLogger != null) {
            mLogger.logError(ex, false);
        }
    }

    public int getErrorCode() {
        return rc5.getErrorCode();
    }
}
