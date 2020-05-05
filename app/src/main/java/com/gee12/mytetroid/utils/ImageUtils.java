package com.gee12.mytetroid.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.gee12.mytetroid.model.TetroidImage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ImageUtils {

    /**
     * Преобразование файла изображения в необходимый формат и сохранение.
     * @param context
     * @param srcUri
     * @param destImagePath
     * @param format
     * @param quality
     * @throws Exception
     */
    public static boolean convertImage(Context context, Uri srcUri, String destImagePath, Bitmap.CompressFormat format, int quality)
            throws Exception {
        Bitmap bitmap = getBitmap(context, srcUri);
        if (bitmap == null)
            return false;
        OutputStream stream = new FileOutputStream(destImagePath);
        bitmap.compress(format, quality, stream);
        stream.flush();
        stream.close();
        return true;
    }

    /**
     *
     * @param storagePath
     * @param image
     */
    public static void setImageDimensions(String storagePath, TetroidImage image) {
        if (image == null)
            return;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        String imageFullName = storagePath + File.separator + image.getRecord().getDirName()
                + File.separator + image.getName();
        BitmapFactory.decodeFile(imageFullName, options);
        image.setWidth(options.outWidth);
        image.setHeight(options.outHeight);
    }

    /**
     *
     * @param context
     * @param uri
     * @return
     * @throws IOException
     */
    public static Bitmap getBitmap(Context context, Uri uri) throws IOException {
        Bitmap bitmap;
        if (uri.getAuthority() != null) {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) {
                inputStream.close();
            }
        } else {
            bitmap = BitmapFactory.decodeFile(uri.getPath());
        }
        return bitmap;
    }
}
