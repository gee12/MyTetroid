package com.gee12.mytetroid.crypt;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.RC5ParameterSpec;

/**
 * Исходный код на C# был взят отсюда:
 * https://ru.wikibooks.org/wiki/%D0%A0%D0%B5%D0%B0%D0%BB%D0%B8%D0%B7%D0%B0%D1%86%D0%B8%D0%B8_%D0%B0%D0%BB%D0%B3%D0%BE%D1%80%D0%B8%D1%82%D0%BC%D0%BE%D0%B2/RC5
 * Были изменены константы.
 */
public class RC5 {
    public static final int W = 32;                            // половина длины блока в битах.
    // Возможные значения 16, 32 и 64.
    // Для эффективной реализации величину W
    // рекомендуют брать равным машинному слову.
    // Например, для 64-битных платформ оптимальным будет
    // выбор W=64, что соответствует размеру блока в 128 бит.

    public static final int R = 12;                            // число раундов. Возможные значения 0…255.
    // Увеличение числа раундов обеспечивает увеличение
    // уровня безопасности шифра. Так, если R = 0,
    // то информация шифроваться не будет.

    private static final long PW = 0xb7e15163;        // 64-битная константа
    private static final long QW = 0x9e3779b9;        // 64-битная константа

    long[] S;                                  // таблица расширенных ключей

    /**
     *  Перед непосредственно шифрованием или расшифровкой данных выполняется процедура расширения ключа.
     *  Процедура генерации ключа состоит из четырех этапов:
     *      1. Генерация констант
     *      2. Разбиение ключа на слова
     *      3. Построение таблицы расширенных ключей
     *      4. Перемешивание
     * @param key Ключ
     */
    public RC5(byte[] key) {
        // основные переменные
        long[] L;                                  // массив слов для секретного ключа пользователя
        int t;                                     // размер таблицы
        int b;                                     // длина ключа в байтах. Возможные значения 0…255.
        int u;                                     // кол-во байтов в одном машинном слове
        int c;                                     // размер массива слов L

        long x, y;
        int i, j, n;

        /*
         * Этап 1. Генерация констант
         * Для заданного параметра W генерируются две псевдослучайные величины,
         * используя две математические константы: e (экспонента) и f (Golden ratio).
         * Qw = Odd((e - 2) * 2^W;
         * Pw = Odd((f - 1) * 2^W;
         * где Odd() - это округление до ближайшего нечетного целого.
         *
         * Для оптимизации алгоритмы эти 2 величины определены заранее (см. константы выше).
         */

        /*
         * Этап 2. Разбиение ключа на слова
         * На этом этапе происходит копирование ключа K[0]..K[255] в массив слов L[0]..L[c-1], где
         * c = b/u, а u = W/8. Если b не кратен W/8, то L[i] дополняется нулевыми битами до ближайшего
         * большего размера c, при котором длина ключа b будет кратна W/8.
         */

        u = W >> 3;
        b = key.length;
        c = b % u > 0 ? b / u + 1 : b / u;
        L = new long[c];

        for (i = b - 1; i >= 0; i--)
        {
            L[i / u] = ROL(L[i / u], 8) + key[i];
        }

        /* Этап 3. Построение таблицы расширенных ключей
         * На этом этапе происходит построение таблицы расширенных ключей S[0]..S[2(R + 1)],
         * которая выполняется следующим образом:
         */

        t = 2 * (R + 1);
        S = new long[t];
        S[0] = PW;
        for (i = 1; i < t; i++)
        {
            S[i] = S[i - 1] + QW;
        }

        /* Этап 4. Перемешивание
         * Циклически выполняются следующие действия:
         */

        x = y = 0;
        i = j = 0;
        n = 3 * Math.max(t, c);

        for (int k = 0; k < n; k++)
        {
            x = S[i] = ROL((S[i] + x + y), 3);
            y = L[j] = ROL((L[j] + x + y), (int)(x + y));
            i = (i + 1) % t;
            j = (j + 1) % c;
        }
    }

    /**
     * Циклический сдвиг битов слова влево
     * @param a Машинное слово: 64 бита
     * @param offset Смещение
     * @return Машинное слово: 64 бита
     */
    private long ROL(long a, int offset)
    {
        long r1, r2;
        r1 = a << offset;
        r2 = a >> (W - offset);
        return (r1 | r2);

    }

    /**
     * Циклический сдвиг битов слова вправо
     * @param a Машинное слово: 64 бита
     * @param offset Смещение
     * @return Машинное слово: 64 бита
     */
    private long ROR(long a, int offset)
    {
        long r1, r2;
        r1 = a >> offset;
        r2 = a << (W - offset);
        return (r1 | r2);

    }

    /**
     * Свертка слова (64 бит) по 8-ми байтам
     * @param b Массив байтов
     * @param p Позиция
     * @return
     */
    private static long bytesToUInt64(byte[] b, int p)
    {
        long r = 0;
        for (int i = p + 7; i > p; i--)
        {
            r |= (long)b[i];
            r <<= 8;
        }
        r |= (long)b[p];
        return r;
    }

    /**
     * Развертка слова (64 бит) по 8-ми байтам
     * @param a 64-битное слово
     * @param b Массив байтов
     * @param p Позиция
     */
    private static void uint64ToBytes(long a, byte[] b, int p)
    {
        for (int i = 0; i < 7; i++)
        {
            b[p + i] = (byte)(a & 0xFF);
            a >>= 8;
        }
        b[p + 7] = (byte)(a & 0xFF);
    }

    /**
     * Операция шифрования
     * @param inBuf Входной буфер для шифруемых данных (64 бита)
     * @param outBuf Выходной буфер (64 бита)
     */
    public void cipher(byte[] inBuf, byte[] outBuf)
    {
        long a = bytesToUInt64(inBuf, 0);
        long b = bytesToUInt64(inBuf, 8);

        a = a + S[0];
        b = b + S[1];

        for (int i = 1; i < R + 1; i++)
        {
            a = ROL((a ^ b), (int)b) + S[2 * i];
            b = ROL((b ^ a), (int)a) + S[2 * i + 1];
        }

        uint64ToBytes(a, outBuf, 0);
        uint64ToBytes(b, outBuf, 8);
    }

    /**
     * Операция расшифрования
     * @param inBuf Входной буфер для шифруемых данных (64 бита)
     * @param outBuf Выходной буфер (64 бита)
     */
    public void decipher(byte[] inBuf, byte[] outBuf)
    {
        long a = bytesToUInt64(inBuf, 0);
        long b = bytesToUInt64(inBuf, 8);

        for (int i = R; i > 0; i--)
        {
            b = ROR((b - S[2 * i + 1]), (int)a) ^ a;
            a = ROR((a - S[2 * i]), (int)b) ^ b;
        }

        b = b - S[1];
        a = a - S[0];

        uint64ToBytes(a, outBuf, 0);
        uint64ToBytes(b, outBuf, 8);
    }

    /**
     * ХЕРНЯ ..!!
     * Операция расшифрования строки
     * @param inBuf Входной буфер строки
     * @param outBuf Выходной буфер
     */
    public void decipherString(byte[] inBuf, byte[] outBuf)
    {
        for (int i = 0; i < inBuf.length; i+=8) {
            byte[] eightBytesBits = new byte[64];
            for (int j = 0; j < 8; j++) {
                setByteBits(inBuf, i + j, eightBytesBits, j*8);
            }
            byte[] eightDecipheredBytesBits = new byte[64];
            decipher(eightBytesBits, eightDecipheredBytesBits);
            for (int j = 0; j < 8; j++) {
                setByteFromBits(eightDecipheredBytesBits, j*8, outBuf, i+j);
            }
        }
    }

    public static void setByteBits(byte[] srcBytes, int srcPos, byte[] destBitsBytes, int destFrom) {
        for (int i = 0; i < 8; i++) {
            destBitsBytes[destFrom + i] = (byte)((srcBytes[srcPos] >> (7 - i)) & 1);
        }
    }

    public static void setByteFromBits(byte[] srcBitsBytes, int srcFrom, byte[] destBytes, int destPos) {
        for (int i = 0; i < 8; i++) {
            destBytes[destPos] |= (byte)(srcBitsBytes[srcFrom + i] << (7 - i));
        }
    }

    /*private static String algorithm = "RC5";
    public static String decryptBase64(byte[] toDecrypt, String key) throws Exception {
        // create a binary key from the argument key (seed)
        SecureRandom sr = new SecureRandom(key.getBytes());
        KeyGenerator kg = KeyGenerator.getInstance(algorithm);
        kg.init(sr);
        SecretKey sk = kg.generateKey();

        // do the decryption with that key
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, sk);
        byte[] decrypted = cipher.doFinal(toDecrypt);

        return new String(decrypted);
    }*/


    /*private int roundNumer;
    private int wordSize;

    private SecretKey key;
    private RC5ParameterSpec RC5params;
    private Cipher rc5;

    public void RC5moje(int roundNumer, int wordSize) {
        this.roundNumer = roundNumer;
        this.wordSize = wordSize;

        Security.addProvider(new FlexiCoreProvider());
        this.RC5params = new RC5ParameterSpec(roundNumer, wordSize);
        try {
            this.rc5 = Cipher.getInstance("RC5", "FlexiCore");
        } catch (NoSuchAlgorithmException ex) {
//            Logger.getLogger(RC5moje.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchProviderException ex) {
//            Logger.getLogger(RC5moje.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchPaddingException ex) {
//            Logger.getLogger(RC5moje.class.getName()).log(Level.SEVERE, null, ex);
        }
        RC5KeyGenerator rC5KeyGenerator = new RC5KeyGenerator();
        this.key = rC5KeyGenerator.generateKey();

    }*/
}
