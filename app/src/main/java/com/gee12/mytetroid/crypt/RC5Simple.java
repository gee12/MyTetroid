package com.gee12.mytetroid.crypt;

import java.util.Arrays;

public class RC5Simple {

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

     public static final int RC5_MODE_ENCODE = 0;
     public static final int RC5_MODE_DECODE = 1;

     public static final int RC5_ERROR_CODE_1 = 1;      // Bad RC5 key length
     public static final int RC5_ERROR_CODE_2 = 2;      // Can't read input file
     public static final int RC5_ERROR_CODE_3 = 3;      // Input file is empty
     public static final int RC5_ERROR_CODE_4 = 4;      // Can't create output file
     public static final int RC5_ERROR_CODE_5 = 5;      // Can't encrypt null data
     public static final int RC5_ERROR_CODE_6 = 6;      // Can't decrypt null data
     public static final int RC5_ERROR_CODE_7 = 7;      // Incorrect data size for decrypt data


    private long[] rc5_s;  // Expanded key table
    private long rc5_p;         // Magic constants one
    private long rc5_q;         // Magic constants two

    private byte[] rc5_key;
    private byte rc5_formatVersion;
    private boolean rc5_isSetFormatVersionForce;

    private int errorCode;


    public RC5Simple(/*boolean enableRandomInit*/)
    {
        // Set magic constants
        rc5_p = 0xb7e15163;
        rc5_q = 0x9e3779b9;

        // Cleaning user key
//        for(int i=0; i<RC5_B; i++)
//            rc5_key[i]=0;
        rc5_key = new byte[RC5_B];

        rc5_formatVersion = RC5_FORMAT_VERSION_CURRENT;
        rc5_isSetFormatVersionForce = false;

        errorCode = 0;

        // Init random generator
//        if(enableRandomInit)
//            srand( time(NULL) );
    }

    // Encrypt data block
// Parameters:
// pt - input data
// ct - encrypt data
    private void RC5_EncryptBlock(long[] pt, long[] ct)
    {
        long a = pt[0] + rc5_s[0];
        long b = pt[1] + rc5_s[1];

        for(int i = 1; i <= RC5_R; i++)
        {
            a = ROTL(a^b, b) + rc5_s[2*i];
            b = ROTL(b^a, a) + rc5_s[2*i+1];
        }

        ct[0] = a;
        ct[1] = b;
    }

    // Decrypt data block
// Parameters:
// pt - input data
// ct - decrypt data
    private void RC5_DecryptBlock(long[] ct, long[] pt)
    {
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


    // Setup secret key
// Parameters:
// key - secret input key[RC5_B]
    private void setup(byte[] key)
    {
//        RC5_LOG(( "setup, set key to: " ));
//        for(int i=0; i<RC5_B; i++)
//            RC5_LOG(( "%.2X", key[i] ));
//        RC5_LOG(( "\n" ));


        int i, j, k, u = RC5_W/8;
        long[] l = new long[RC5_C];

        // Initialize l[], then rc5_s[], then mix key into rc5_s[]
        for(i = RC5_B-1, l[RC5_C-1] = 0; i !=- 1; i--)
            l[i/u] = (l[i/u]<<8)+key[i];

//        RC5_LOG(( "setup, l[]: " ));
//        for(i=0; i<RC5_C; i++)
//            RC5_LOG(( "%.2X", l[i] ));
//        RC5_LOG(( "\n" ));

        rc5_s = new long[RC5_T];
        for(rc5_s[0]=rc5_p,i=1; i < RC5_T; i++)
            rc5_s[i] = rc5_s[i-1]+rc5_q;

//        RC5_LOG(( "setup, rc5_s[]: " ));
//        for(i=0; i<RC5_T; i++)
//            RC5_LOG(( "%.2X", rc5_s[i] ));
//        RC5_LOG(( "\n" ));

        long a, b;
        // 3*t > 3*c
        for(a=b=i=j=k=0; k<3*RC5_T; k++, i=(i+1)%RC5_T, j=(j+1)%RC5_C)
        {
            a = rc5_s[i] = ROTL(rc5_s[i]+(a+b),3);
            b = l[j] = ROTL(l[j]+(a+b),(a+b));
        }

//        RC5_LOG(( "setup, mix rc5_s[]: " ));
//        for(int i=0; i<RC5_T; i++)
//            RC5_LOG(( "%.2X", rc5_s[i] ));
//        RC5_LOG(( "\n" ));
    }


    // Set secret key
    public void setKey(byte[] key)
    {
        if(key.length != RC5_B)
        {
            errorCode = RC5_ERROR_CODE_1;
            return;
        }

        for(int i=0; i < RC5_B; i++)
            rc5_key[i]=key[i];
    }


/*
// Encrypt data in vector
//void RC5Simple::RC5_Encrypt(vector<unsigned char> &in, vector<unsigned char> &out)
public String RC5_Encrypt(String in)
{
    String out = "";
 // Clear output vector
// out.clear();

 // Отвлекли: помой ребенка, потри его мочалкой а то он сам не может
 // Отвлекли: смотри какое платье на кукле
 // No crypt null data
    int length = in.length();
    if(length == 0)
  {
   errorCode = RC5_ERROR_CODE_5;
   return null;
  }


 // Save input data size
// unsigned int clean_data_size=in.size();
// RC5_LOG(( "Input data size: %d\n", clean_data_size ));


 // IV block
// unsigned char iv[RC5_BLOCK_LEN];
 String iv[RC5_BLOCK_LEN];
 for(int i=0; i<RC5_BLOCK_LEN; i++)
//  iv[i]=(unsigned char)rand(0, 0xFF);
  iv[i] = (unsigned char)rand(0, 0xFF);


 // Block with data size
 unsigned char data_size[RC5_BLOCK_LEN];

 for(int i=0; i<RC5_BLOCK_LEN; i++)
  {
   data_size[i]=RC5_GetByteFromInt( clean_data_size, i );
   RC5_LOG(( "Data size byte %d: %.2X\n", i, data_size[i] ));
  }


 // Insert data size to begin data
 in.insert( in.begin(), RC5_BLOCK_LEN, 0);
 for(int i=0; i<RC5_BLOCK_LEN; i++)
  in[i]=data_size[i];


 // Add firsted random data
 unsigned int firsRandomDataBlocks=0;
 if(rc5_formatVersion==RC5_FORMAT_VERSION_2)
  firsRandomDataBlocks=1; // At start at format version 2, in begin data add one block with random data

 if(rc5_formatVersion==RC5_FORMAT_VERSION_3)
  firsRandomDataBlocks=2; // At start at format version 3, in begin data add two blocks with random data (full width is 128 bit)

 if(firsRandomDataBlocks>0)
  for(unsigned int n=1; n<=firsRandomDataBlocks; n++)
   {
    in.insert( in.begin(), RC5_BLOCK_LEN, 0);
    for(int i=0; i<RC5_BLOCK_LEN; i++)
     in[i]=(unsigned char)rand(0, 0xFF);
   }


 // Align end of data to block size
 int last_unalign_len=clean_data_size%RC5_BLOCK_LEN;
 RC5_LOG(( "Last unalign len: %d\n", last_unalign_len ));

 if(last_unalign_len>0)
  {
   // Add random data to end for align to block size
   for(int i=0; i<(RC5_BLOCK_LEN-last_unalign_len); i++)
    {
     RC5_LOG(( "Add byte: %d\n", i ));
     in.push_back( (unsigned char)rand(0, 0xFF) );
    }
  }

// #if RC5_ENABLE_DEBUG_PRINT==1
//  RC5_LOG(( "Data size after crypt setup: %d\n", in.size() ));
//  RC5_LOG(( "Plain byte after crypt setup: " ));
//  for(int i=0; i<in.size(); i++)
//   RC5_LOG(( "%.2X ", in[i] ));
//  RC5_LOG(( "\n" ));
// #endif


 // ------
 // Encode
 // ------

 // Create and fill cell in output vector

 unsigned int notCryptDataSize=0;

 // In format version 1 save only IV as open data in block 0
 if(rc5_formatVersion==RC5_FORMAT_VERSION_1)
  {
   out.resize(in.size()+RC5_BLOCK_LEN, 0);

   // Save start IV to block 0 in output data
   for(int i=0; i<RC5_BLOCK_LEN; i++)
    out[i]=iv[i];

   notCryptDataSize=RC5_BLOCK_LEN;
  }

 // Отвлекли: иди поешь
 // In format version 2 or higth save format header (block 0) and IV (block 1)
 if(rc5_formatVersion>=RC5_FORMAT_VERSION_2)
  {
   out.resize(in.size()+RC5_BLOCK_LEN*2, 0);

   // Signature bytes in header
   const char *signature=RC5_SIMPLE_SIGNATURE;
   for(int i=0; i<(RC5_BLOCK_LEN-1); i++)
    out[i]=signature[i];

   // Format version byte in header
   if(rc5_isSetFormatVersionForce)
    out[RC5_BLOCK_LEN-1]=rc5_formatVersion;
   else
    out[RC5_BLOCK_LEN-1]=RC5_FORMAT_VERSION_CURRENT;

   // Save start IV to second block in output data
   for(int i=0; i<RC5_BLOCK_LEN; i++)
    out[i+RC5_BLOCK_LEN]=iv[i];

   notCryptDataSize=RC5_BLOCK_LEN*2;
  }


 // Set secret key for encrypt
 setup(rc5_key);


 // Encode by blocks
 unsigned int block=0;
 while( (RC5_BLOCK_LEN*(block+1))<=in.size() )
  {
   unsigned int shift=block*RC5_BLOCK_LEN;

   // Temp input buffer for dont modify input data
   unsigned char temp_in[RC5_BLOCK_LEN];

   // XOR block with current IV
   for(int i=0; i<RC5_BLOCK_LEN; i++)
    temp_in[i]=in[shift+i] ^ iv[i];

   RC5_TWORD temp_word_1;
   RC5_TWORD temp_word_2;
   RC5_TWORD pt[RC5_WORDS_IN_BLOCK];
   RC5_TWORD ct[RC5_WORDS_IN_BLOCK];

   RC5_LOG(( "Block num %d, shift %d\n", block, shift ));

   temp_word_1=getWordFromByte(temp_in[0], temp_in[1], temp_in[2], temp_in[3]);

   temp_word_2=getWordFromByte(temp_in[RC5_WORD_LEN+0], temp_in[RC5_WORD_LEN+1], temp_in[RC5_WORD_LEN+2], temp_in[RC5_WORD_LEN+3]);

   pt[0]=temp_word_1;
   pt[1]=temp_word_2;


   // Encode
   RC5_EncryptBlock(pt, ct);


   #if RC5_ENABLE_DEBUG_PRINT==1
    RC5_LOG(( "Pt %.8X, %.8X\n", pt[0], pt[1] ));
    RC5_LOG(( "Ct %.8X, %.8X\n", ct[0], ct[1] ));
   #endif

   // Отвлекли: надо покушать, кончай питаться йогуртами
   // Save crypt data
   for(int i=0; i<RC5_WORD_LEN; i++)
    {
     // Save crypt data with shift to RC5_BLOCK_LEN
     // btw. in first block putted IV
     out[notCryptDataSize+shift+i]=getByteFromWord(ct[0], i);
     out[notCryptDataSize+shift+RC5_WORD_LEN+i]=getByteFromWord(ct[1], i);
    }


   // Generate next IV for Cipher Block Chaining (CBC)
   for(int i=0; i<RC5_BLOCK_LEN; i++)
    iv[i]=out[notCryptDataSize+shift+i];


   block++;
  }


 // Cleaning IV
 for(int i=0; i<RC5_BLOCK_LEN; i++)
  iv[i]=0;


 // -----------------------------------
 // Return input vector to firsted data
 // -----------------------------------

 // Отвлекли: сходи в магазин Магнит, у нас есть нечего
 // Отвлекли: почитай ребенку
 // Remove size block (for all formats)
 in.erase(in.begin(), in.begin()+RC5_BLOCK_LEN);

 // Отвлекли: давай поиграем в Shatter
 // Remove random data blocks
 if(firsRandomDataBlocks>0)
  for(unsigned int n=1; n<=firsRandomDataBlocks; n++)
   in.erase(in.begin(), in.begin()+RC5_BLOCK_LEN);

 // Отвлекли: звонок, проверь ребенку зрение
 // Отвлекли: напечатай таблицу проверки зрения
 // Отвлекли: проверь зрение у ребенка и у себя
 // Remove from input vector last random byte for aligning
 if(in.size()>clean_data_size)
  in.erase(in.begin()+clean_data_size, in.end());

}
     */


    // Decrypt data in vector
//void RC5Simple::RC5_Decrypt(vector<unsigned char> &in, vector<unsigned char> &out)
    public byte[] decrypt(byte[] in) {
        //        RC5_LOG(("\nDecrypt\n"));

//        RC5_LOG(("\nInput data size: %d\n", in.size()));

        // Отвлекли: иди покушай
        // No decrypt null data
        int inSize = in.length;
        if (inSize == 0) {
            errorCode = RC5_ERROR_CODE_6;
            return null;
        }


        // Detect format version
//        unsigned int formatVersion = 0;
        int formatVersion = 0;
        if (rc5_isSetFormatVersionForce)
            formatVersion = rc5_formatVersion; // If format set force, format not autodetected
        else {
            // Signature bytes
//            const char *signature = RC5_SIMPLE_SIGNATURE;
            String signature = RC5_SIMPLE_SIGNATURE;

            // Detect signature
            boolean isSignatureCorrect = true;
            for (int i = 0; i < (RC5_BLOCK_LEN - 1); i++)
                if (in[i] != signature.charAt(i))
                    isSignatureCorrect = false;

            // If not detect signature
            if (!isSignatureCorrect)
                formatVersion = RC5_FORMAT_VERSION_1; // In first version can't signature
            else {
                // Get format version
//                unsigned char readFormatVersion = in[RC5_BLOCK_LEN - 1];
                byte readFormatVersion = in[RC5_BLOCK_LEN - 1];

                // If format version correct
                if (readFormatVersion >= RC5_FORMAT_VERSION_2 && readFormatVersion <= RC5_FORMAT_VERSION_CURRENT)
                    formatVersion = readFormatVersion;
                else
                    formatVersion = RC5_FORMAT_VERSION_1; // If version not correct, may be it format 1 ( probability 0.[hrendesyatih]1 )
            }
        }


//        unsigned int ivShift = 0;
//        unsigned int firstDataBlock = 0;
//        unsigned int removeBlocksFromOutput = 0;
//        unsigned int blockWithDataSize = 0;
        int ivShift = 0;
        int firstDataBlock = 0;
        int removeBlocksFromOutput = 0;
        int blockWithDataSize = 0;

        if (formatVersion == RC5_FORMAT_VERSION_1) {
            ivShift = 0;
            firstDataBlock = 1; // Start decode from block with index 1 (from block 0 read IV)
            removeBlocksFromOutput = 1;
            blockWithDataSize = 1;
        }

        if (formatVersion == RC5_FORMAT_VERSION_2) {
            ivShift = RC5_BLOCK_LEN;
            firstDataBlock = 2; // Start decode from block with index 2 (0 - signature and version, 1 - IV)
            removeBlocksFromOutput = 2; // Random data block and size data block
            blockWithDataSize = 3;
        }

        if (formatVersion == RC5_FORMAT_VERSION_3) {
            ivShift = RC5_BLOCK_LEN;
            firstDataBlock = 2; // Start decode from block with index 2 (0 - signature and version, 1 - IV)
            removeBlocksFromOutput = 3; // Two random data block and size data block
            blockWithDataSize = 4;
        }


        // Get IV
//        unsigned char iv[ RC5_BLOCK_LEN];
        byte[] iv = new byte[ RC5_BLOCK_LEN];
        for (int i = 0; i < RC5_BLOCK_LEN; i++)
            iv[i] = in[i + ivShift];


        // Set secret key for decrypt
        setup(rc5_key);


        // Cleaning output vector
//        out.clear();
//        out.resize(in.size(), 0);
        byte[] out = new byte[inSize];


        // Decode by blocks from started data block
//        unsigned int data_size = 0;
//        unsigned int block = firstDataBlock;
        int data_size = 0;
        int block = firstDataBlock;
        while ((RC5_BLOCK_LEN * (block + 1)) <= inSize) {
//            unsigned int shift = block * RC5_BLOCK_LEN;
            int shift = block * RC5_BLOCK_LEN;

//            RC5_TWORD temp_word_1;
//            RC5_TWORD temp_word_2;
//            RC5_TWORD pt[ RC5_WORDS_IN_BLOCK];
//            RC5_TWORD ct[ RC5_WORDS_IN_BLOCK];
            long temp_word_1;
            long temp_word_2;
            long[] pt = new long[RC5_WORDS_IN_BLOCK];
            long[] ct = new long[RC5_WORDS_IN_BLOCK];

//            RC5_LOG(("Block num %d, shift %d\n", block, shift));

            temp_word_1 = getWordFromByte(in[shift], in[shift + 1], in[shift + 2], in[shift + 3]);

            temp_word_2 = getWordFromByte(in[shift + RC5_WORD_LEN], in[shift + RC5_WORD_LEN + 1], in[shift + RC5_WORD_LEN + 2], in[shift + RC5_WORD_LEN + 3]);

            pt[0] = temp_word_1;
            pt[1] = temp_word_2;

//            RC5_LOG(("Block data. Word 1: %.2X, Word 2: %.2X\n", temp_word_1, temp_word_2));


            // Decode
            RC5_DecryptBlock(pt, ct);


            // ---------------------------
            // Un XOR block with IV vector
            // ---------------------------

            // Convert block words to plain array
//            unsigned char ct_part[ RC5_BLOCK_LEN];
            byte[] ct_part = new byte[ RC5_BLOCK_LEN];

            for (int i = 0; i < RC5_WORD_LEN; i++) {
                ct_part[i] = getByteFromWord(ct[0], i);
                ct_part[i + RC5_WORD_LEN] = getByteFromWord(ct[1], i);
            }

            // Un XOR
            for (int i = 0; i < RC5_BLOCK_LEN; i++)
                ct_part[i] ^= iv[i];

            // Отвлекли: попугаи наверно голодные, кто бы их кормил, надо покормить
            if (block == blockWithDataSize) {
                data_size = RC5_GetIntFromByte(ct_part[0], ct_part[1], ct_part[2], ct_part[3]);

//                RC5_LOG(("Decrypt data size: %d\n", data_size));

                // Uncorrect decrypt data size
//                if ((unsigned int)data_size > (unsigned int)inSize)
                if (data_size > inSize)
                {
                    // RC5_LOG(( "Incorrect data size. Decrypt data size: %d, estimate data size: ~%d\n", data_size,  in.size() ));
                    errorCode = RC5_ERROR_CODE_7;
                    return null;
                }
            }

//   #if RC5_ENABLE_DEBUG_PRINT == 1
//            RC5_LOG(("Pt %.8X, %.8X\n", pt[0], pt[1]));
//            RC5_LOG(("Ct %.8X, %.8X\n", ct[0], ct[1]));
//   #endif


            // Generate next IV for Cipher Block Chaining (CBC)
            for (int i = 0; i < RC5_BLOCK_LEN; i++)
                iv[i] = in[shift + i];


            // Save decrypt data
            for (int i = 0; i < RC5_BLOCK_LEN; i++) {
//                RC5_LOG(("Put decrypt data to vector out[%d] = %.2X\n", shift - (removeBlocksFromOutput * RC5_BLOCK_LEN) + i, ct_part[i]));
                out[shift - (firstDataBlock * RC5_BLOCK_LEN) + i] = ct_part[i];
            }

            block++;
        }


        // Cleaning IV
        for (int i = 0; i < RC5_BLOCK_LEN; i++)
            iv[i] = 0;

        // Remove from output a blocks with technical data (random blocks, size, etc...)
//        out.erase(out.begin(), out.begin() + removeBlocksFromOutput * RC5_BLOCK_LEN);

        // Remove from output a last byte with random byte for aligning
//        if (out.size() > data_size)
//            out.erase(out.begin() + data_size, out.end());

        int to = (out.length > data_size) ? data_size : out.length;
        out = Arrays.copyOfRange(out,  removeBlocksFromOutput * RC5_BLOCK_LEN, to);

        return out;
    }




    /*

void RC5Simple::RC5_EncryptFile(unsigned char *in_name, unsigned char *out_name)
{
 RC5_EncDecFile(in_name, out_name, RC5_MODE_ENCODE);
}


// Отвлекли: позвонил дедушка
void RC5Simple::RC5_EncryptFile(const char *in_name, const char *out_name)
{
 // Отвлекли: виниловая наклейка как мечь, смотри
 RC5_EncDecFile((unsigned char *)in_name, (unsigned char *)out_name, RC5_MODE_ENCODE);
}


void RC5Simple::RC5_DecryptFile(unsigned char *in_name, unsigned char *out_name)
{
 RC5_EncDecFile(in_name, out_name, RC5_MODE_DECODE);
}


void RC5Simple::RC5_DecryptFile(const char *in_name, const char *out_name)
{
 RC5_EncDecFile((unsigned char *)in_name, (unsigned char *)out_name, RC5_MODE_DECODE);
}


void RC5Simple::RC5_EncDecFile(unsigned char *in_name, unsigned char *out_name, int mode)
{
 FILE *in_file;
 FILE *out_file;

 if((in_file=fopen( (const char*)in_name, "rb") )==NULL)
  {
   errorCode=RC5_ERROR_CODE_2;
   return;
  }

 // Get size of input file
 fseek(in_file, 0, SEEK_END);
 unsigned int in_file_length=ftell(in_file);
 RC5_LOG(( "Input file name: %s, size: %d\n", in_name, in_file_length));

 if(in_file_length==0)
  {
   fclose(in_file);
   errorCode=RC5_ERROR_CODE_3;
   return;
  }

 // Return to begin file
 fseek(in_file, 0, SEEK_SET);

 // Create data vectors
 vector<unsigned char> in(in_file_length);
 vector<unsigned char> out;

 // Fill input data vector
 unsigned int byte=0;
 while(byte<in_file_length)
  in[byte++]=fgetc(in_file);

 fclose(in_file);

 if(mode==RC5_MODE_ENCODE)
  RC5_Encrypt(in, out); // Encrypt data vector
 else
  RC5_Decrypt(in, out); // Decrypt data vector

 // Create output file
 if((out_file=fopen( (const char*)out_name, "wb") )==NULL)
  {
   errorCode=RC5_ERROR_CODE_4;
   return;
  }

 // Fill output file
 for(unsigned int i=0; i<out.size(); i++)
  {
   RC5_LOG(( "File byte %d : %.2X \n", i, out[i] ));
   fputc(out[i], out_file);
  }

 fclose(out_file);

}
     */




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
}
*/


// Random generator, from - to inclusive
    /*private int rand(int from, int to)
    {
        int len=to-from+1;
        return (new Random().nextInt()%len)+from;
    }*/

    // Rotation operators. x must be unsigned, to get logical right shift
    public static long ROTL(long x, long y) {
        return (((x) << (y & (RC5_W - 1))) | ((x) >> (RC5_W - (y & (RC5_W - 1)))));
    }

    public static long ROTR(long x, long y) {
        return (((x) >> (y & (RC5_W - 1))) | ((x) << (RC5_W - (y & (RC5_W - 1)))));
    }

    //    #define getByteFromWord(w, n) ((unsigned char)((w) >> ((n)*8) & 0xff))
    public static byte getByteFromWord(long w, int n) {
        return ((byte) (w >> (n * 8) & 0xff));
    }
    //  #define getWordFromByte(b0, b1, b2, b3) ((RC5_TWORD)(b0 + (b1 << 8) + (b2 << 16)+ (b3 << 24)))
    public static long getWordFromByte(byte b0, byte b1, byte b2, byte b3) {
        return (b0 + (b1 << 8) + (b2 << 16) + (b3 << 24));
    }

    public static int RC5_GetIntFromByte(byte b0, byte b1, byte b2, byte b3) {
        return b0 + (b1 << 8) + (b2 << 16) + (b3 << 24);
    }

}
