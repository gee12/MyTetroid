package com.gee12.mytetroid.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.Toast;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.logs.ILogger;
import com.gee12.mytetroid.logs.LogManager;
import com.gee12.mytetroid.logs.TetroidLog;
import com.gee12.mytetroid.model.TetroidImage;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.utils.ImageUtils;

import java.io.File;

public class ImagesManager extends DataManager {

    /**
     * Сохранение файла изображения в каталог записи.
     * @param context
     * @param record
     * @param srcUri
     * @param deleteSrcFile Нужно ли удалить исходный файл после сохранения файла назначения
     * @return
     */
    public static TetroidImage saveImage(Context context, TetroidRecord record, Uri srcUri, boolean deleteSrcFile) {
        if (record == null || srcUri == null) {
            LogManager.emptyParams(context, "DataManager.saveImage()");
            return null;
        }
        String srcPath = srcUri.getPath();
        LogManager.log(context, String.format(context.getString(R.string.log_start_image_file_saving_mask),
                srcPath, record.getId()), ILogger.Types.DEBUG);

        // генерируем уникальное имя файла
        String nameId = createUniqueImageName();

        TetroidImage image = new TetroidImage(nameId, record);

        // проверяем существование каталога записи
        String dirPath = RecordsManager.getPathToRecordFolder(context, record);
        int dirRes = RecordsManager.checkRecordFolder(context, dirPath, true);
        if (dirRes <= 0) {
            return null;
        }

        String destFullName = RecordsManager.getPathToFileInRecordFolder(context, record, nameId);
        LogManager.log(context, String.format(context.getString(R.string.log_start_image_file_converting_mask),
                destFullName), ILogger.Types.DEBUG);
        try {
            // конвертируем изображение в формат PNG и сохраняем в каталог записи
            ImageUtils.convertImage(context, srcUri, destFullName, Bitmap.CompressFormat.PNG, 100);
            File destFile = new File(destFullName);
            if (destFile.exists()) {
                if (deleteSrcFile) {
                    LogManager.log(context, String.format(context.getString(R.string.log_start_image_file_deleting_mask),
                            srcUri), ILogger.Types.DEBUG);
                    File srcFile = new File(srcPath);
                    // удаляем исходный файл за ненадобностью
                    if (!srcFile.delete()) {
                        LogManager.log(context, context.getString(R.string.log_error_deleting_src_image_file)
                                + srcPath, ILogger.Types.WARNING, Toast.LENGTH_LONG);
                    }
                }
            } else {
                LogManager.log(context, context.getString(R.string.log_error_image_file_saving), ILogger.Types.ERROR);
                return null;
            }
        } catch (Exception ex) {
            LogManager.log(context, context.getString(R.string.log_error_image_file_saving), ex);
            return null;
        }
        return image;
    }

    /**
     * Сохранение изображения в каталог записи.
     * @param context
     * @param record
     * @param bitmap
     * @return
     */
    public static TetroidImage saveImage(Context context, TetroidRecord record, Bitmap bitmap) {
        if (record == null || bitmap == null) {
            LogManager.emptyParams(context, "DataManager.saveImage()");
            return null;
        }
        TetroidLog.logOperRes(context, TetroidLog.Objs.IMAGE, TetroidLog.Opers.SAVE, record, -1);

        // генерируем уникальное имя файла
        String nameId = createUniqueImageName();

        TetroidImage image = new TetroidImage(nameId, record);

        // проверяем существование каталога записи
        String dirPath = RecordsManager.getPathToRecordFolder(context, record);
        int dirRes = RecordsManager.checkRecordFolder(context, dirPath, true);
        if (dirRes <= 0) {
            return null;
        }

        String destFullName = RecordsManager.getPathToFileInRecordFolder(context, record, nameId);
        LogManager.log(context, String.format(context.getString(R.string.log_start_image_file_converting_mask),
                destFullName), ILogger.Types.DEBUG);
        try {
            // конвертируем изображение в формат PNG и сохраняем в каталог записи
            ImageUtils.saveBitmap(bitmap, destFullName, Bitmap.CompressFormat.PNG, 100);
            File destFile = new File(destFullName);
            if (!destFile.exists()) {
                LogManager.log(context, context.getString(R.string.log_error_image_file_saving), ILogger.Types.ERROR);
                return null;
            }
        } catch (Exception ex) {
            LogManager.log(context, context.getString(R.string.log_error_image_file_saving), ex);
            return null;
        }
        return image;
    }

}
