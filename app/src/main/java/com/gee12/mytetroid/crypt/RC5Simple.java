package com.gee12.mytetroid.crypt;

import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.Utils;

import java.util.Arrays;

/**
 * Реализация на Java класса RC5Simple, написанного Сергеем Степановым (xintrea) на C++.
 * Источник:
 * https://github.com/xintrea/mytetra_dev/blob/experimental/app/src/libraries/crypt/RC5Simple.cpp
 *
 * Требуется оптимизация:
 * - в Java нет беззнаковых типов, поэтому операции сложения/вычитания с переменными типа boolean
 * без операции расширения типа (в int, например) выполняются неверно. Выход:
 *     1) делать расширение типа в операциях "на лету" (при чем оно и так делается неявно в int
 *     при выполнении операций сложения/вычитания с типами boolean, short, char)
 *     2) заранее преобразовывать поступающий массив байт из boolean в int и все операции
 *     проводить в int (это долго при большом массиве данных)
 * Сейчас используется вариант 2, что, по идее, не оптимально.
 * - полученный в результате расшифровки массив нужно обрезать вначале и в конце,
 * чтобы оставить только данные. В Java это можно сделать с помощью Arrays.copyOfRange,
 * но при больших массивах (~20Мб) приложение может упасть из-за OutOfMemoryError. Выход:
 *     1) по ходу расшифровки (поблочно) результат записывать в файл. Но (!) это вариант
 *     подходит для расшифровки прикрепленных файлов, но не подходит для расшифровки записей.
 *     Т.к. временный файл неуместно сохранять в каталог записи, а если сохранять отдельно, то
 *     отпадут относительные html-ссылки (к сожалению, метода WebView.setBaseUrl() нету..).
 *     Удалять временный файл после просмотра записи - вариант ненадежный.
 *     2) выводить результат в String, обрезая его сразу при формировании строки
 *     (у String есть специальный конструктор для этого). Но тут тоже падает из-за
 *     OutOfMemoryError, но, хотя бы, избавляемся от центрального промежуточного этапа в цепочке
 *     Расшифровка=>Byte[]=>String
 *
 */
public class RC5Simple {

//    private class DecryptOutHelper {
//        byte[] out;
//        int dataSize;
//        int removeBlocksFromOutput;
//
//        public DecryptOutHelper(byte[] out, int dataSize, int removeBlocksFromOutput) {
//            this.out = out;
//            this.dataSize = dataSize;
//            this.removeBlocksFromOutput = removeBlocksFromOutput;
//        }
//
//        int getShift() {
//            return removeBlocksFromOutput * RC5_BLOCK_LEN;
//        }
//
//        int getSize() {
//            return (out.length > dataSize) ? dataSize : out.length;
//        }
//    }


//    public static final String RC5_SIMPLE_VERSION = "RC5Simple Ver. 1.31 / 11.12.2013";

    public static final int RC5_FORMAT_VERSION_1 = 1;
    public static final int RC5_FORMAT_VERSION_2 = 2;
    public static final int RC5_FORMAT_VERSION_3 = 3;
    public static final int RC5_FORMAT_VERSION_CURRENT = RC5_FORMAT_VERSION_3;

    public static final String RC5_SIMPLE_SIGNATURE = "RC5SIMP";

//    typedef unsigned long int RC5_TWORD;
//    #define RC5_ARCHITECTURE 32

    public static final int RC5_W = 32;           // word size in bits
    public static final int RC5_R = 12;           // number of rounds
    public static final int RC5_B = 16;           // number of bytes in key
    public static final int RC5_C = 4;            // number words in key = ceil(8*b/rc5_w)
    public static final int RC5_T = 26;           // size of table S = 2*(r+1) words

    public static final int RC5_WORDS_IN_BLOCK = 2;                         // words in block
    public static final int RC5_BLOCK_LEN = (RC5_W*RC5_WORDS_IN_BLOCK/8);   // block size in bytes
    public static final int RC5_WORD_LEN = (RC5_W/8);                       // word size in bytes

//     public static final int RC5_MODE_ENCODE = 0;
//     public static final int RC5_MODE_DECODE = 1;

     public static final int RC5_ERROR_CODE_1 = 1;      // Bad RC5 key length
     public static final int RC5_ERROR_CODE_2 = 2;      // Can't read input file
     public static final int RC5_ERROR_CODE_3 = 3;      // Input file is empty
     public static final int RC5_ERROR_CODE_4 = 4;      // Can't create output file
     public static final int RC5_ERROR_CODE_5 = 5;      // Can't encrypt null data
     public static final int RC5_ERROR_CODE_6 = 6;      // Can't decryptBase64 null data
     public static final int RC5_ERROR_CODE_7 = 7;      // Incorrect data size for decryptBase64 data


    private long[] rc5_s;       // Expanded key table
    private long rc5_p;         // Magic constants one
    private long rc5_q;         // Magic constants two

    private int[] rc5_key;
    private byte rc5_formatVersion;
    private boolean rc5_isSetFormatVersionForce;
    private int errorCode;

    /**
     *
     */
    public RC5Simple() {
        // Set magic constants
        rc5_p = 0xb7e15163L;
        rc5_q = 0x9e3779b9L;

        // Cleaning user key
        rc5_key = new int[RC5_B];

        rc5_formatVersion = RC5_FORMAT_VERSION_CURRENT;
        rc5_isSetFormatVersionForce = false;

        errorCode = 0;
    }

    /**
     * Setup secret key
     * @param key secret input key[RC5_B]
     */
    private void setup(int[] key) {
        int i, j, k, u = RC5_W/8;
        long[] l = new long[RC5_C];

        // Initialize l[], then rc5_s[], then mix key into rc5_s[]
        for(i = RC5_B-1, l[RC5_C-1] = 0; i !=- 1; i--)
            l[i/u] = (Utils.toUnsignedInt(l[i/u]<<8))+key[i];

        rc5_s = new long[RC5_T];
        for(rc5_s[0]=rc5_p,i=1; i < RC5_T; i++)
            rc5_s[i] = Utils.toUnsignedInt(rc5_s[i-1]+rc5_q);

        long a, b;
        // 3*t > 3*c
        for(a=b=i=j=k=0; k<3*RC5_T; k++, i=(i+1)%RC5_T, j=(j+1)%RC5_C)
        {
            a = rc5_s[i] = ROTL(rc5_s[i]+(a+b),3);
            b = l[j] = ROTL(l[j]+(a+b),(a+b));
        }
    }


    /**
     * Set secret key
     * @param key
     */
    public void setKey(int[] key) {
        if(key.length != RC5_B) {
            errorCode = RC5_ERROR_CODE_1;
            return;
        }

        for(int i=0; i < RC5_B; i++)
            rc5_key[i]=key[i];
    }

    /**
     * Decrypt data block
     * @param ct input data
     * @param pt decrypt Base64 data
     */
    private void decryptBlock(long[] ct, long[] pt) {
        long b = ct[1];
        long a = ct[0];

        for(int i = RC5_R; i > 0; i--)
        {
            b = ROTR(b - rc5_s[2*i+1], a)^a;
            a = ROTR(a - rc5_s[2*i], b)^b;
        }

        pt[1] = b - rc5_s[1];
        pt[0] = a - rc5_s[0];
    }

    /**
     *
     * @param inSigned
     * @return
     */
    public byte[] decrypt(byte[] inSigned) {
        int[] in = Utils.toUnsigned(inSigned);
//        addDebugLog(String.format("Input data size: %d", in.length));

        // No decryptBase64 null data
        int inSize = in.length;
        if (inSize == 0) {
            errorCode = RC5_ERROR_CODE_6;
            return null;
        }

        // Detect format version
        int formatVersion;
        if (rc5_isSetFormatVersionForce)
            formatVersion = rc5_formatVersion; // If format set force, format not autodetected
        else {
            // Detect signature
            boolean isSignatureCorrect = true;
            for (int i = 0; i < (RC5_BLOCK_LEN - 1); i++)
                if (in[i] != RC5_SIMPLE_SIGNATURE.charAt(i))
                    isSignatureCorrect = false;

            // If not detect signature
            if (!isSignatureCorrect)
                formatVersion = RC5_FORMAT_VERSION_1; // In first version can't signature
            else {
                // Get format version
                int readFormatVersion = in[RC5_BLOCK_LEN - 1];

                // If format version correct
                if (readFormatVersion >= RC5_FORMAT_VERSION_2 && readFormatVersion <= RC5_FORMAT_VERSION_CURRENT)
                    formatVersion = readFormatVersion;
                else
                    formatVersion = RC5_FORMAT_VERSION_1; // If version not correct, may be it format 1 ( probability 0.[hrendesyatih]1 )
            }
        }

        int ivShift = 0;
        int firstDataBlock = 0;
        int removeBlocksFromOutput = 0;
        int blockWithDataSize = 0;

        switch (formatVersion) {
            case RC5_FORMAT_VERSION_1:
                ivShift = 0;
                firstDataBlock = 1; // Start decode from block with index 1 (from block 0 read IV)
                removeBlocksFromOutput = 1;
                blockWithDataSize = 1;
                break;
            case RC5_FORMAT_VERSION_2:
                ivShift = RC5_BLOCK_LEN;
                firstDataBlock = 2; // Start decode from block with index 2 (0 - signature and version, 1 - IV)
                removeBlocksFromOutput = 2; // Random data block and size data block
                blockWithDataSize = 3;
                break;
            case RC5_FORMAT_VERSION_3:
                ivShift = RC5_BLOCK_LEN;
                firstDataBlock = 2; // Start decode from block with index 2 (0 - signature and version, 1 - IV)
                removeBlocksFromOutput = 3; // Two random data block and size data block
                blockWithDataSize = 4;
                break;
        }

        // Get IV
        int[] iv = new int[ RC5_BLOCK_LEN];
        for (int i = 0; i < RC5_BLOCK_LEN; i++)
            iv[i] = in[i + ivShift];

        // Set secret key for decryptBase64
        setup(rc5_key);

        // Cleaning output vector
//        int[] out = new int[inSize];
//        byte[] out = new byte[inSize];
        byte[] out = null;

        // Decode by blocks from started data block
        int dataSize = 0;
        int block = firstDataBlock;
        while ((RC5_BLOCK_LEN * (block + 1)) <= inSize) {
            int shift = block * RC5_BLOCK_LEN;

            long[] pt = new long[RC5_WORDS_IN_BLOCK];
            long[] ct = new long[RC5_WORDS_IN_BLOCK];

//            addDebugLog(String.format("Block num %d, shift %d", block, shift));

            pt[0] = getWordFromByte(in[shift], in[shift + 1], in[shift + 2], in[shift + 3]);
            pt[1] = getWordFromByte(in[shift + RC5_WORD_LEN], in[shift + RC5_WORD_LEN + 1],
                    in[shift + RC5_WORD_LEN + 2], in[shift + RC5_WORD_LEN + 3]);

//            addDebugLog(String.format("Block data. Word 1: %X, Word 2: %X", pt[0], pt[1]));

            // Decode
            decryptBlock(pt, ct);

            // ---------------------------
            // Un XOR block with IV vector
            // ---------------------------

            // Convert block words size plain array
            int[] ct_part = new int[ RC5_BLOCK_LEN];

            for (int i = 0; i < RC5_WORD_LEN; i++) {
                ct_part[i] = getByteFromWord(ct[0], i);
                ct_part[i + RC5_WORD_LEN] = getByteFromWord(ct[1], i);
            }

            // Un XOR
            for (int i = 0; i < RC5_BLOCK_LEN; i++)
                ct_part[i] ^= iv[i];

            if (block == blockWithDataSize) {
                dataSize = (int)getIntFromByte(ct_part[0], ct_part[1], ct_part[2], ct_part[3]);

                out = new byte[dataSize];

//                addDebugLog(String.format("Decrypt data size: %d", dataSize));

                // Uncorrect decryptBase64 data size
                if (dataSize > inSize)
                {
                    addErrorLog(String.format("Incorrect data size. Decrypt data size: %d, estimate data size: ~%d", dataSize,  in.length ));
                    errorCode = RC5_ERROR_CODE_7;
                    return null;
                }
            }

            // Generate next IV for Cipher Block Chaining (CBC)
            for (int i = 0; i < RC5_BLOCK_LEN; i++)
                iv[i] = in[shift + i];

            if (block >= firstDataBlock + removeBlocksFromOutput) {
                // Save decryptBase64 data
                int curBlockLen = ((inSize - shift) <= RC5_BLOCK_LEN && dataSize % RC5_BLOCK_LEN != 0)
                        ? dataSize % RC5_BLOCK_LEN : RC5_BLOCK_LEN;
                for (int i = 0; i < curBlockLen; i++) {
//                addDebugLog(String.format("Put decryptBase64 data size vector out[%d] = %X", shift - (removeBlocksFromOutput * RC5_BLOCK_LEN) + i, ct_part[i]));
//                out[shift - (firstDataBlock * RC5_BLOCK_LEN) + i] = ct_part[i];
                    out[(block - (firstDataBlock + removeBlocksFromOutput)) * RC5_BLOCK_LEN + i] = (byte) ct_part[i];
                }
            }

            block++;
        }

        // Cleaning IV
//        for (int i = 0; i < RC5_BLOCK_LEN; i++)
//            iv[i] = 0;

//        out.erase(out.begin(), out.begin() + removeBlocksFromOutput * RC5_BLOCK_LEN);

        // Remove from output a last byte with random byte for aligning
//        if (out.size() > dataSize)
//            out.erase(out.begin() + dataSize, out.end());

//        int size = (out.length > dataSize) ? (int)dataSize : out.length;
//        int from = outHelper.removeBlocksFromOutput * RC5_BLOCK_LEN;
//        out = Arrays.copyOfRange(out, from , from  + size);

//        return new DecryptOutHelper(out, (int)dataSize, removeBlocksFromOutput);
        return out;
    }


    /*
    inline unsigned char RC5Simple::RC5_GetByteFromInt(unsigned int w, int n)
    {
     unsigned char b=0;

     switch (n) {
       case 0:
           b=(w & 0x000000FF);
           break;
       case 1:
           b=(w & 0x0000FF00) >> 8;
           break;
       case 2:
           b=(w & 0x00FF0000) >> 16;
           break;
       case 3:
           b=(w & 0xFF000000) >> 24;
     }

     // RC5_LOG(( "GetByteFromWord(%.8X, %d)=%.2X\n", w, n, b ));

     return b;
    }*/


    /**
     * Rotation operators
     * x must be unsigned, to get logical right shift
     * @param x
     * @param y
     * @return
     */
    public static long ROTL(long x, long y) {
        x = 0xFFFFFFFFL & x;
        long res = (((x) << (y & (RC5_W - 1))) | ((x) >> (RC5_W - (y & (RC5_W - 1)))));
        return 0xFFFFFFFFL & res;
    }

    public static long ROTR(long x, long y) {
        x = 0xFFFFFFFFL & x;
        long res = (((x) >> (y & (RC5_W - 1))) | ((x) << (RC5_W - (y & (RC5_W - 1)))));
        return 0xFFFFFFFFL & res;
    }

//    #define getByteFromWord(w, n) ((unsigned char)((w) >> ((n)*8) & 0xff))
//    public static byte getByteFromWord(long w, int n) {
//        return ((byte) (w >> (n * 8) & 0xff));
//    }

    public static int getByteFromWord(long w, int n) {
        return ((int) (w >> (n * 8) & 0xffL));
    }
    //  #define getWordFromByte(b0, b1, b2, b3) ((RC5_TWORD)(b0 + (b1 << 8) + (b2 << 16)+ (b3 << 24)))
    public static long getWordFromByte(byte b0, byte b1, byte b2, byte b3) {
        return (b0 + (b1 << 8) + (b2 << 16) + (b3 << 24));
    }
    public static long getWordFromByte(int i0, int i1, int i2, int i3) {
        return 0xFFFFFFFFL & (i0 + (i1 << 8) + (i2 << 16) + (i3 << 24));
    }


    public static long getIntFromByte(byte b0, byte b1, byte b2, byte b3) {
        return b0 + (b1 << 8) + (b2 << 16) + (b3 << 24);
    }
    public static long getIntFromByte(int i0, int i1, int i2, int i3) {
        return 0xFFFFFFFFL & (i0 + (i1 << 8) + (i2 << 16) + (i3 << 24));
    }

    private static void addErrorLog(Exception e) {
        LogManager.addLog(e);
    }

    private static void addErrorLog(String s) {
        LogManager.addLog(s, LogManager.Types.ERROR);
    }

    private static void addDebugLog(String s) {
        LogManager.addLog(s, LogManager.Types.DEBUG);
    }
}
