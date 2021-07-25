package com.gee12.mytetroid.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.logs.ILogger;
import com.gee12.mytetroid.logs.LogManager;
import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParseException;
import com.larvalabs.svgandroid.SVGParser;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class FileUtils {

    /**
     * Загрузка .svg файла в объект Drawable.
     * @param fullFileName
     * @return
     * @throws FileNotFoundException
     * @throws SVGParseException
     * @throws NullPointerException
     */
    public static Drawable loadSVGFromFile(String fullFileName) throws FileNotFoundException, SVGParseException, NullPointerException {
        File file = new File(fullFileName);
        FileInputStream fileInputStream = new FileInputStream(file);
        SVG svg = SVGParser.getSVGFromInputStream(fileInputStream);
        return svg.createPictureDrawable();
    }

    /**
     * Построчное чтение текстового файла.
     * @param fileUri
     * @return
     * @throws IOException
     */
    public static String readTextFile(Uri fileUri) throws IOException {
        if (fileUri == null)
            return null;
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(new File(fileUri.getPath())));
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append('\n');
        }
        br.close();
        return sb.toString();
    }

    /**
     * Построчное чтение текстового файла с формированием результата в виде блоков.
     * @param fileUri
     * @return
     * @throws IOException
     */
    public static ArrayList<String> readTextFile(Uri fileUri, int linesInBlock) throws IOException {
        if (fileUri == null)
            return null;
        /*ArrayList<String> blocks = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(new File(fileUri.getPath())));
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
        br.close();*/
        return readToBlocks(new BufferedReader(new FileReader(new File(fileUri.getPath()))), linesInBlock);
    }

    /**
     * Разбиение строки на блоки.
     * @param s
     * @param linesInBlock
     * @return
     * @throws IOException
     */
    public static  ArrayList<String> splitToBlocks(String s, int linesInBlock) throws IOException {
        if (s == null)
            return null;
        return readToBlocks(new BufferedReader(new StringReader(s)), linesInBlock);
    }

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
     * Чтение файла.
     * @param fileUri
     * @return
     * @throws IOException
     */
    public static byte[] readFile(Uri fileUri) throws IOException {
        if (fileUri == null)
            return null;
        File file = new File(fileUri.getPath());
        byte[] data = new byte[(int) file.length()];
        DataInputStream dis = new DataInputStream(new FileInputStream(file));
        dis.readFully(data);
        dis.close();
        return data;
    }

    /**
     * Запись файла.
     * @param fileUri
     * @param text
     * @return
     * @throws IOException
     */
    public static boolean writeFile(Uri fileUri, String text) throws IOException {
        if (fileUri == null || text == null)
            return false;
        return writeFile(fileUri, text.getBytes());
    }

    /**
     * Запись файла.
     * @param fileUri
     * @param bytes
     * @return
     * @throws IOException
     */
    public static boolean writeFile(Uri fileUri, byte[] bytes) throws IOException {
        if (fileUri == null || bytes == null)
            return false;
        File file = new File(fileUri.getPath());
        DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));
        dos.write(bytes);
        dos.flush();
        dos.close();
        return true;
    }

    /**
     * Побайтовое копирование файла.
     * @param srcFileUri
     * @param destFileUri
     * @return
     * @throws IOException
     */
    public static boolean copyFile(Uri srcFileUri, Uri destFileUri) throws IOException {
        if (srcFileUri == null || destFileUri == null)
            return false;
        return copyFile(new File(srcFileUri.getPath()), new File(destFileUri.getPath()));
    }

    public static boolean copyFile(File srcFile, File destFile) throws IOException {
        if (srcFile == null || destFile == null)
            return false;
        try (FileInputStream fis = new FileInputStream(srcFile);
             FileOutputStream fos = new FileOutputStream(destFile)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        }
        return true;
    }

    /**
     * Удаление файла или каталога с файлами/подкаталогами.
     * @param fileOrDirectory
     */
    public static boolean deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory == null)
            return false;
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles())
                if (!deleteRecursive(child))
                    return false;
        }
        return fileOrDirectory.delete();
    }

    /**
     * Очистка содержимого каталога.
     * @param dir
     * @return
     */
    public static boolean clearDir(File dir) {
        if (dir == null)
            return false;
        if (dir.isDirectory()) {
            for (File child : dir.listFiles())
                if (!deleteRecursive(child))
                    return false;
        }
        return true;
    }

    /**
     * Перемещение файла или каталога с файлами/подкаталогами в указанный каталог (родительский).
     * @param srcFile Исходный файл/каталог
     * @param destDir Каталог назначения (родительский)
     */
    public static boolean moveToDirRecursive(File srcFile, File destDir) /*throws IOException*/ {
        if (srcFile == null || destDir == null)
            return false;
        if (srcFile.isDirectory()) {
            if (!destDir.exists() && !destDir.mkdirs()) {
//            throw new IOException("Cannot create directory " + destDir.getAbsolutePath());
                return false;
            }
            for (String child : srcFile.list())
                if (!moveToDirRecursive(new File(srcFile, child), new File(destDir, srcFile.getName())))
                    return false;
            return srcFile.delete();
        } else {
            if (!destDir.exists() && !destDir.mkdirs()) {
//            throw new IOException("Cannot create directory " + destDir.getAbsolutePath());
                return false;
            }
//        Files.move(srcFile.toPath(), destDir.toPath(), StandardCopyOption.REPLACE_EXISTING);
            File destFile = new File(destDir, srcFile.getName());
            return srcFile.renameTo(destFile);
        }
    }

    /**
     * Копирование файла или каталога с файлами/подкаталогами в указанный файл/каталог (не родительский).
     * @param srcFile Исходный файл/каталог
     * @param destFile Файл/каталог назначения (не родительский)
     * @throws IOException
     */
    public static boolean copyDirRecursive(File srcFile , File destFile) throws IOException {
        if (srcFile == null || destFile == null)
            return false;
        if (srcFile.isDirectory()) {
            if (!destFile.exists() && !destFile.mkdirs()) {
                throw new IOException("Cannot create directory " + destFile.getAbsolutePath());
            }
            for (String child : srcFile.list()) {
                if (!copyDirRecursive(new File(srcFile, child), new File(destFile, child)))
                    return false;
            }
        } else {
            File dir = destFile.getParentFile();
            if (dir != null && !dir.exists() && !dir.mkdirs()) {
                throw new IOException("Cannot create directory " + dir.getAbsolutePath());
            }
            InputStream in = new FileInputStream(srcFile);
            OutputStream out = new FileOutputStream(destFile);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
        return true;
    }

    /**
     * Создание каталога, если его еще не существует.
     * @param dir
     * @return
     */
    public static boolean createDirIfNeed(File dir) {
        if (dir == null)
            return false;
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                return false;
            }
        }
        return true;
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
     * Получение каталога приложения во внешнем хранилище (удаляется вместе с приложением).
     * @param context
     * @return
     */
    public static String getAppExternalFilesDir(Context context) {
        File file = context.getExternalFilesDir(null);
        return (file != null) ? file.getAbsolutePath() : null;
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
     * Получение общедоступного каталога "Документы" или каталога приложения, если первый недоступен.
     * @param context
     * @param forWrite
     * @return
     */
    public static String getExternalPublicDocsOrAppDir(Context context, boolean forWrite) {
        String externalState = Environment.getExternalStorageState();
        if ((!forWrite || !Environment.MEDIA_MOUNTED_READ_ONLY.equals(externalState))
                && Environment.MEDIA_MOUNTED.equals(externalState)) {
            return getExternalPublicDocsDir();
        } else {
            return getAppExternalFilesDir(context);
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
     * Проверка, пуст ли каталог.
     * @param dir
     * @return
     */
    public static boolean isDirEmpty(File dir) {
        if (dir == null)
            return true;
        File[] childs = dir.listFiles();
        return (childs == null || childs.length == 0);
    }

    /**
     * Получение размера файла/каталога.
     * @param context
     * @param fullFileName
     * @return
     */
    public static String getFileSize(Context context, String fullFileName) {
        long size;
        try {
            File file = new File(fullFileName);
            if (!file.exists()) {
                LogManager.log(context, context.getString(R.string.log_file_is_missing) + fullFileName, ILogger.Types.ERROR);
                return null;
            }
            size = getFileSize(file);
        } catch (SecurityException ex) {
            LogManager.log(context, context.getString(R.string.log_denied_read_file_access) + fullFileName, ex);
            return null;
        } catch (Exception ex) {
            LogManager.log(context, context.getString(R.string.log_get_file_size_error) + fullFileName, ex);
            return null;
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
                if (listFiles == null || listFiles.length == 0)
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
    public static Date getFileModifiedDate(Context context, String fullFileName) {
        Date date;
        try {
            File file = new File(fullFileName);
            if (!file.exists()) {
                LogManager.log(context, context.getString(R.string.log_file_is_missing) + fullFileName, ILogger.Types.ERROR);
                return null;
            }
            date = getFileLastModifiedDate(file);
        } catch (SecurityException ex) {
            LogManager.log(context, context.getString(R.string.log_denied_read_file_access) + fullFileName, ex);
            return null;
        } catch (Exception ex) {
            LogManager.log(context, context.getString(R.string.log_get_file_size_error) + fullFileName, ex);
            return null;
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
