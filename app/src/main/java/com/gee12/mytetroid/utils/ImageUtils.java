package com.gee12.mytetroid.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.gee12.mytetroid.model.TetroidImage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ImageUtils {
    /**
     * Преобразование файла изображения в необходимый формат и сохранение.
     * @param srcImagePath
     * @param destImagePath
     * @param format
     * @param quality
     * @throws IOException
     */
    public static void convertImage(String srcImagePath, String destImagePath, Bitmap.CompressFormat format, int quality)
            throws IOException {
        Bitmap bitmap = BitmapFactory.decodeFile(srcImagePath);
        OutputStream stream = new FileOutputStream(destImagePath);
        bitmap.compress(format, quality, stream);
        stream.flush();
        stream.close();
    }

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
}
