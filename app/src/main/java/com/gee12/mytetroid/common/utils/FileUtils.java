package com.gee12.mytetroid.common.utils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import com.gee12.mytetroid.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class FileUtils {

    /**
     * Разбиение потока на блоки.
     * @param br
     * @param linesInBlock
     * @return
     * @throws IOException
     */
    public static ArrayList<String> readToBlocks(BufferedReader br, int linesInBlock) throws IOException {
        if (br == null)
            return null;
        ArrayList<String> blocks = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        String line;
        int counter = 0;
        while ((line = br.readLine()) != null) {
            sb.append(line);
            if (++counter >= linesInBlock) {
                counter = 0;
                blocks.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append('\n');
            }
        }
        if (counter > 0) {
            blocks.add(sb.toString());
        }
        br.close();
        return blocks;
    }

    /**
     * Получение расширения файла с точкой.
     * @param fileFullName
     * @return Расширение файла с точкой, или пустую строку, если расширение отсутствует.
     */
    public static String getExtensionWithComma(String fileFullName) {
        int extIndex = fileFullName.lastIndexOf(".");
        return (extIndex > -1) ? fileFullName.substring(extIndex) : "";
    }

    /**
     * Получение пути к файлу (каталог).
     * @param fileFullName
     * @return
     */
    public static String getFileFolder(String fileFullName) {
        if (fileFullName == null) {
            return null;
        }
        int slashIndex = fileFullName.lastIndexOf("/");
        return (slashIndex > -1) ? fileFullName.substring(0, slashIndex) : "";
    }

    /**
     * Получение общедоступного каталога "Документы" во внешнем хранилище (без проверки состояния).
     * @return
     */
    public static String getExternalPublicDocsDir() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
        } else {
            return Environment.getExternalStorageDirectory() + "/Documents";
        }
    }

    /**
     * Является ли внешнее хранилище только для чтения.
     * @return
     */
    private static boolean isExternalStorageReadOnly() {
        return Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState());
    }

    /**
     * Есть ли доступ к внешнему хранилищу.
     * @return
     */
    private static boolean isExternalStorageAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /**
     * Получение размера файла/каталога.
     * @param context
     * @param fullFileName
     * @return
     */
    public static String getFileSize(Context context, String fullFileName) throws Exception {
        long size;
        try {
            File file = new File(fullFileName);
            if (!file.exists()) {
                throw new Exception(context.getString(R.string.error_file_is_missing_mask, fullFileName));
            }
            size = getFileSize(file);
        } catch (SecurityException ex) {
            throw new Exception(context.getString(R.string.error_denied_read_file_access_mask, fullFileName));
        }
//        return FileUtils.fileSizeToStringBin(context, size);
        return android.text.format.Formatter.formatFileSize(context, size);
    }

    /**
     * Получение размера файла/каталога в байтах.
     * @param file
     * @return
     */
    public static long getFileSize(File file) {
        if (file == null || !file.exists())
            return 0;
        long size = 0;
        if (!file.isDirectory()) {
            size = file.length();
        } else {
            final List<File> dirs = new LinkedList<>();
            dirs.add(file);
            while (!dirs.isEmpty()) {
                final File dir = dirs.remove(0);
                if (!dir.exists())
                    continue;
                final File[] listFiles = dir.listFiles();
                if (listFiles == null)
                    continue;
                for (final File child : listFiles) {
                    size += child.length();
                    if (child.isDirectory())
                        dirs.add(child);
                }
            }
        }
        return size;
    }

    /**
     *
     * @param context
     * @param fullFileName
     * @return
     */
    public static Date getFileModifiedDate(Context context, String fullFileName) throws Exception {
        Date date;
        try {
            File file = new File(fullFileName);
            if (!file.exists()) {
                throw new Exception(context.getString(R.string.error_file_is_missing_mask, fullFileName));
            }
            date = getFileLastModifiedDate(file);
        } catch (SecurityException ex) {
            throw new Exception(context.getString(R.string.error_denied_read_file_access_mask, fullFileName));
        }
        return date;
    }

    /**
     * Получение даты последнего изменения файла.
     * @param file
     * @return
     */
    public static Date getFileLastModifiedDate(File file) {
        if (file == null)
            return null;
        return new Date(file.lastModified());
    }

}
