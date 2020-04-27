package com.gee12.mytetroid.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.Html;
import android.text.Spanned;

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

public class FileUtils {

    /**
     *
     * @param htmlText
     * @return
     */
    public static Spanned fromHtml(String htmlText) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(htmlText);
        }
    }
    /**
     *
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
     *
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
     * Перемещение файла или каталога с файлами/подкаталогами в указанный каталог (родительский).
     * @param srcFile Исходный файл/каталог
     * @param destDir Каталог назначения (родительский)
     */
    public static boolean moveToDirRecursive(File srcFile, File destDir) /*throws IOException*/ {
        if (srcFile == null || destDir == null)
            return false;
        if (srcFile.isDirectory()) {
            for (String child : srcFile.list())
                if (!moveToDirRecursive(new File(srcFile, child), new File(destDir, srcFile.getName())))
                    return false;
        }
//        Files.move(srcFile.toPath(), destDir.toPath(), StandardCopyOption.REPLACE_EXISTING);
        File destFile = new File(destDir, srcFile.getName());
        return srcFile.renameTo(destFile);
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

}
