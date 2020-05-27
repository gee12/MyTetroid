package com.gee12.mytetroid.crypt;

import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Реализация на Java класса RC5Simple, написанного Sergey M. Stepanov (xintrea) на C++.
 * Источник:
 * https://github.com/xintrea/mytetra_dev/blob/experimental/app/src/libraries/crypt/RC5Simple.cpp
 *
 * Требуется оптимизация расшифровки:
 * - в Java нет беззнаковых типов, поэтому операции сложения/вычитания с переменными типа boolean
 * без операции расширения типа (в int, например) выполняются неверно. Выход:
 *     1) делать расширение типа в операциях "на лету" (при чем оно и так делается неявно в int
 *     при выполнении операций сложения/вычитания с типами boolean, short, char)
 *     2) заранее преобразовывать поступающий массив байт из boolean в int и все операции
 *     проводить в int (это долго при большом массиве данных)
 * Сейчас используется вариант 2, что, по идее, не оптимально.
 * - полученный в результате расшифровки массив нужно обрезать вначале и в конце,
 * чтобы оставить только данные. В Java это можно сделать с помощью Arrays.copyOfRange,
 * но при больших массивах (~20Мб) приложение может упасть из-за OutOfMemoryError.
 * Выход:
 *     1) по ходу расшифровки (поблочно) результат записывать в файл. Но (!) это вариант
 *     подходит для расшифровки прикрепленных файлов, но не подходит для расшифровки записей.
 *     Т.к. временный файл неуместно сохранять в каталог записи, а если сохранять отдельно, то
 *     отпадут относительные html-ссылки (к сожалению, метода WebView.setBaseUrl() нету..).
 *     Удалять временный файл после просмотра записи - вариант ненадежный.
 *     2) выводить результат в String, обрезая его сразу при формировании строки
 *     (у String есть специальный конструктор для этого).
 *
 */
public class RC5Simple {

    public static final int RC5_FORMAT_VERSION_1 = 1;
    public static final int RC5_FORMAT_VERSION_2 = 2;
    public static final int RC5_FORMAT_VERSION_3 = 3;
    public static final int RC5_FORMAT_VERSION_CURRENT = RC5_FORMAT_VERSION_3;

    public static final String RC5_SIMPLE_SIGNATURE = "RC5SIMP";

    public static final int RC5_W = 32;           // word size in bits
    public static final int RC5_R = 12;           // number of rounds
    public static final int RC5_B = 16;           // number of bytes in mKey
    public static final int RC5_C = 4;            // number words in mKey = ceil(8*b/rc5_w)
    public static final int RC5_T = 26;           // size of table S = 2*(r+1) words

    public static final int RC5_WORDS_IN_BLOCK = 2;                         // words in block
    public static final int RC5_BLOCK_LEN = (RC5_W*RC5_WORDS_IN_BLOCK/8);   // block size in bytes
    public static final int RC5_WORD_LEN = (RC5_W/8);                       // word size in bytes

    public static final int RC5_ERROR_CODE_1 = 1;      // Bad RC5 key length
    public static final int RC5_ERROR_CODE_2 = 2;      // Can't read input file
    public static final int RC5_ERROR_CODE_3 = 3;      // Input file is empty
    public static final int RC5_ERROR_CODE_4 = 4;      // Can't show output file
    public static final int RC5_ERROR_CODE_5 = 5;      // Can't encrypt null data
    public static final int RC5_ERROR_CODE_6 = 6;      // Can't decryptBase64 null data
    public static final int RC5_ERROR_CODE_7 = 7;      // Incorrect data size for decryptBase64 data
    public static final int RC5_ERROR_CODE_8 = 8;      // Empty incoming data array

    private long[] mRC5_s;       // Expanded mKey table
    private long mRC5_p;         // Magic constants one
    private long mRC5_q;         // Magic constants two

    private int[] mKey;
    private byte mFormatVersion;
    private boolean mIsSetFormatVersionForce;
    private int mErrorCode;

    /**
     * Default constructor.
     */
    public RC5Simple() {
        // Set magic constants
        this.mRC5_p = 0xb7e15163L;
        this.mRC5_q = 0x9e3779b9L;

        this.mKey = new int[RC5_B];
        this.mFormatVersion = RC5_FORMAT_VERSION_CURRENT;
        this.mIsSetFormatVersionForce = false;
        this.mErrorCode = 0;
    }

    /**
     * Setup secret mKey.
     * @param key secret input mKey[RC5_B]
     */
    private void setup(int[] key) {
        int i, j, k, u = RC5_W/8;
        long[] l = new long[RC5_C];

        // Initialize l[], then mRC5_s[], then mix mKey into mRC5_s[]
        for(i = RC5_B-1, l[RC5_C-1] = 0; i !=- 1; i--) {
            l[i / u] = (Utils.toUnsignedInt(l[i / u] << 8)) + key[i];
        }

        mRC5_s = new long[RC5_T];
        for(mRC5_s[0]= mRC5_p,i=1; i < RC5_T; i++) {
            mRC5_s[i] = Utils.toUnsignedInt(mRC5_s[i - 1] + mRC5_q);
        }

        long a, b;
        // 3*t > 3*c
        for(a=b=i=j=k=0; k<3*RC5_T; k++, i=(i+1)%RC5_T, j=(j+1)%RC5_C) {
            a = mRC5_s[i] = ROTL(mRC5_s[i]+(a+b),3);
            b = l[j] = ROTL(l[j]+(a+b),(a+b));
        }
    }


    /**
     * Set secret mKey.
     * @param key
     */
    public void setKey(int[] key) {
        if (key == null) {
            return;
        }
        if (key.length != RC5_B) {
            this.mErrorCode = RC5_ERROR_CODE_1;
            return;
        }

        this.mKey = new int[key.length];
        for (int i=0; i < RC5_B; i++) {
            this.mKey[i] = key[i];
        }
    }

    /**
     * Encrypt data block.
     * @param ct input data
     * @param pt encrypt data
     */
    private void encryptBlock(long[] pt, long[] ct) {
        long a = pt[0]+ mRC5_s[0];
        long b = pt[1]+ mRC5_s[1];

        for(int i = 1; i <= RC5_R; i++) {
            a = ROTL(a^b, b) + mRC5_s[2*i];
            b = ROTL(b^a, a) + mRC5_s[2*i+1];
        }

        ct[0] = a;
        ct[1] = b;
    }

    /**
     * Decrypt data block.
     * @param ct input data
     * @param pt decrypt data
     */
    private void decryptBlock(long[] ct, long[] pt) {
        long b = ct[1];
        long a = ct[0];

        for(int i = RC5_R; i > 0; i--) {
            b = ROTR(b - mRC5_s[2*i+1], a)^a;
            a = ROTR(a - mRC5_s[2*i], b)^b;
        }

        pt[1] = b - mRC5_s[1];
        pt[0] = a - mRC5_s[0];
    }

    /**
     * Encrypt data.
     * @param inSigned
     * @return
     */
    public byte[] encrypt(byte[] inSigned) {
        this.mErrorCode = 0;
        if (inSigned == null) {
            this.mErrorCode = RC5_ERROR_CODE_8;
            return null;
        }
        List<Integer> inUnsigned = Utils.toUnsigned2(inSigned);

        int inSize = inSigned.length;
        if (inSize == 0) {
            this.mErrorCode = RC5_ERROR_CODE_5;
            return null;
        }

        // IV block
        int[] iv = new int[RC5_BLOCK_LEN];
        for (int i = 0; i < RC5_BLOCK_LEN; i++) {
            iv[i] = rand(0, 0xFF);
        }

        List<Integer> in = new ArrayList<>();

        // Block with data size
        for (int i = 0; i < RC5_BLOCK_LEN; i++) {
            in.add(i, getByteFromWord(inSize, i));
        }

        // Add firsted random data
        int firstRandomDataBlocks = 0;
        if (mFormatVersion == RC5_FORMAT_VERSION_2)
            firstRandomDataBlocks = 1; // At start at format version 2, in begin data add one block with random data
        else if(mFormatVersion == RC5_FORMAT_VERSION_3)
            firstRandomDataBlocks = 2; // At start at format version 3, in begin data add two blocks with random data (full width is 128 bit)

        if (firstRandomDataBlocks > 0) {
            for (int n = 1; n <= firstRandomDataBlocks; n++) {
                for (int i = 0; i < RC5_BLOCK_LEN; i++) {
                    in.add(i, rand(0, 0xFF));
                }
            }
        }

        in.addAll(inUnsigned);

        // Align end of data to block size
        int lastUnalignLength = inSize % RC5_BLOCK_LEN;

        if (lastUnalignLength > 0) {
            // Add random data to end for align to block size
            for (int i = 0; i < (RC5_BLOCK_LEN - lastUnalignLength); i++) {
                in.add(rand(0, 0xFF));
            }
        }

        // Create and fill cell in output vector
        int notCryptDataSize = 0;
        byte[] out = null;

        if (mFormatVersion == RC5_FORMAT_VERSION_1) {
            // In format version 1 save only IV as open data in block 0
            notCryptDataSize = RC5_BLOCK_LEN;
            out = new byte[in.size() + notCryptDataSize];

            // Save start IV to block 0 in output data
            for (int i = 0; i < RC5_BLOCK_LEN; i++) {
                out[i] = (byte)iv[i];
            }
        } else if (mFormatVersion >= RC5_FORMAT_VERSION_2) {
            // In format version 2 or higth save format header (block 0) and IV (block 1)
            notCryptDataSize = RC5_BLOCK_LEN * 2;
            out = new byte[in.size() + notCryptDataSize];

            // Signature bytes in header
            for (int i = 0; i < (RC5_BLOCK_LEN-1); i++) {
                out[i] = (byte) RC5_SIMPLE_SIGNATURE.charAt(i);
            }

            // Format version byte in header
            out[RC5_BLOCK_LEN - 1] = (mIsSetFormatVersionForce) ? mFormatVersion : RC5_FORMAT_VERSION_CURRENT;

            // Save start IV to second block in output data
            for (int i=0; i < RC5_BLOCK_LEN; i++) {
                out[RC5_BLOCK_LEN + i] = (byte) iv[i];
            }
        }

        if (out == null) {
            //
            return null;
        }

        // Set secret mKey for encrypt
        setup(mKey);

        // Encode by blocks
        int block = 0;
        while ((RC5_BLOCK_LEN * (block + 1)) <= in.size()) {
            int shift = block * RC5_BLOCK_LEN;

            // Temp input buffer for dont modify input data
            int[] tempIn = new int[RC5_BLOCK_LEN];

            // XOR block with current IV
            for(int i = 0; i < RC5_BLOCK_LEN; i++) {
                tempIn[i] = in.get(shift+i) ^ iv[i];
            }

            long[] pt = new long[RC5_WORDS_IN_BLOCK];
            long[] ct = new long[RC5_WORDS_IN_BLOCK];

            pt[0] = getWordFromByte(tempIn[0], tempIn[1], tempIn[2], tempIn[3]);
            pt[1] = getWordFromByte(tempIn[RC5_WORD_LEN+0], tempIn[RC5_WORD_LEN+1], tempIn[RC5_WORD_LEN+2], tempIn[RC5_WORD_LEN+3]);

            // Encode
            encryptBlock(pt, ct);

            // Save crypt data
            for (int i = 0; i < RC5_WORD_LEN; i++) {
                // Save crypt data with shift to RC5_BLOCK_LEN
                // btw. in first block putted IV
                out[notCryptDataSize+shift+i] = (byte) getByteFromWord(ct[0], i);
                out[notCryptDataSize+shift+RC5_WORD_LEN+i] = (byte) getByteFromWord(ct[1], i);
            }

            // Generate next IV for Cipher Block Chaining (CBC)
            for (int i = 0; i < RC5_BLOCK_LEN; i++) {
                iv[i] = Utils.toUnsigned(out[notCryptDataSize+shift+i]);
            }

            block++;
        }
        return out;
    }

    /**
     * Decrypt data.
     * @param inSigned
     * @return
     */
    public byte[] decrypt(byte[] inSigned) throws OutOfMemoryError {
        this.mErrorCode = 0;
        if (inSigned == null) {
            this.mErrorCode = RC5_ERROR_CODE_8;
            return null;
        }
        int[] in = Utils.toUnsigned(inSigned);

        // No decryptBase64 null data
        int inSize = in.length;
        if (inSize == 0) {
            this.mErrorCode = RC5_ERROR_CODE_6;
            return null;
        }

        // Detect format version
        int formatVersion;
        if (mIsSetFormatVersionForce)
            formatVersion = this.mFormatVersion; // If format set force, format not autodetected
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
        int[] iv = new int[RC5_BLOCK_LEN];
//        for (int i = 0; i < RC5_BLOCK_LEN; i++) {
//            iv[i] = in[i + ivShift];
//        }
        System.arraycopy(in, ivShift, iv, 0, RC5_BLOCK_LEN);

        // Set secret mKey for decryptBase64
        setup(mKey);

        // Cleaning output vector
        byte[] out = null;

        // Decode by blocks from started data block
        int dataSize = 0;
        int block = firstDataBlock;
        while ((RC5_BLOCK_LEN * (block + 1)) <= inSize) {
            int shift = block * RC5_BLOCK_LEN;

            long[] pt = new long[RC5_WORDS_IN_BLOCK];
            long[] ct = new long[RC5_WORDS_IN_BLOCK];

            pt[0] = getWordFromByte(in[shift], in[shift + 1], in[shift + 2], in[shift + 3]);
            pt[1] = getWordFromByte(in[shift + RC5_WORD_LEN], in[shift + RC5_WORD_LEN + 1],
                    in[shift + RC5_WORD_LEN + 2], in[shift + RC5_WORD_LEN + 3]);

            // Decode
            decryptBlock(pt, ct);

            // ---------------------------
            // Un XOR block with IV vector
            // ---------------------------

            // Convert block words size plain array
            int[] ct_part = new int[RC5_BLOCK_LEN];

            for (int i = 0; i < RC5_WORD_LEN; i++) {
                ct_part[i] = getByteFromWord(ct[0], i);
                ct_part[i + RC5_WORD_LEN] = getByteFromWord(ct[1], i);
            }

            // Un XOR
            for (int i = 0; i < RC5_BLOCK_LEN; i++) {
                ct_part[i] ^= iv[i];
            }

            if (block == blockWithDataSize) {
                dataSize = (int)getIntFromByte(ct_part[0], ct_part[1], ct_part[2], ct_part[3]);

                // Uncorrect decryptBase64 data size
                if (dataSize <= 0 || dataSize > inSize)
                {
                    addErrorLog(String.format("Incorrect data size. Decrypt data size: %d, estimate data size: ~%d", dataSize, in.length));
                    this.mErrorCode = RC5_ERROR_CODE_7;
                    return null;
                }
                out = new byte[dataSize];

            }

            // Generate next IV for Cipher Block Chaining (CBC)
//            for (int i = 0; i < RC5_BLOCK_LEN; i++) {
//                iv[i] = in[shift + i];
//            }
            System.arraycopy(in, shift, iv, 0, RC5_BLOCK_LEN);

            if (block >= firstDataBlock + removeBlocksFromOutput) {
                // Save decryptBase64 data
                int curBlockLen = ((inSize - shift) <= RC5_BLOCK_LEN && dataSize % RC5_BLOCK_LEN != 0)
                        ? dataSize % RC5_BLOCK_LEN : RC5_BLOCK_LEN;
                for (int i = 0; i < curBlockLen; i++) {
                    out[(block - (firstDataBlock + removeBlocksFromOutput)) * RC5_BLOCK_LEN + i] = (byte) ct_part[i];
                }
            }

            block++;
        }
        return out;
    }

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

//    private int getByteFromInt(long w, int n) {
//        int b = 0;
//        switch (n) {
//            case 0:
//                b = (int) (w & 0x000000FF);
//                break;
//            case 1:
//                b = (int) (w & 0x0000FF00) >> 8;
//                break;
//            case 2:
//                b = (int) (w & 0x00FF0000) >> 16;
//                break;
//            case 3:
//                b = (int) (w & 0xFF000000) >> 24;
//        }
//        return b;
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

    /**
     * Random generator, from - to inclusive
     */
    private int rand(long from, long to) {
        long len = to - from + 1;

        // FIXME: создать 1 экземпляр rand ?
        Random rand = new Random();

        return (int)(Math.abs(rand.nextInt()) % len + from);
    }

    private static void addErrorLog(Exception e) {
        LogManager.log(e);
    }

    private static void addErrorLog(String s) {
        LogManager.log(s, LogManager.Types.ERROR);
    }

    private static void addDebugLog(String s) {
        LogManager.log(s, LogManager.Types.DEBUG);
    }

    public int getErrorCode() {
        return mErrorCode;
    }
}
