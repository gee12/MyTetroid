package com.gee12.mytetroid.data;

import android.content.Context;
import android.text.TextUtils;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.logs.ILogger;
import com.gee12.mytetroid.logs.LogManager;
import com.gee12.mytetroid.model.TetroidIcon;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.model.TetroidTag;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Deprecated
public class IconsManager extends DataManager {

//    /**
//     * Получение списка каталогов с иконками в каталоге "icons/".
//     * @param context
//     * @return
//     */
//    public static List<String> getIconsFolders(Context context) {
//        final File folder = new File(getIconsFolderPath());
//        if (!folder.exists()) {
//            return null;
//        }
//        List<String> res = new ArrayList<>();
//        for (final File fileEntry : folder.listFiles()) {
//            if (fileEntry.isDirectory()) {
//                res.add(fileEntry.getName());
//            }
//        }
//        return res;
//    }
//
//    /**
//     * Получение списка иконок (файлов .svg) в подкаталоге каталога "icons/".
//     * @param folderName
//     * @return
//     */
//    public static List<TetroidIcon> getIconsFromFolder(Context context, String folderName) {
//        if (TextUtils.isEmpty(folderName)) {
//            return null;
//        }
//        String iconsFolderFullName = getIconsFolderPath() + SEPAR + folderName;
//        final File folder = new File(iconsFolderFullName);
//        if (!folder.exists()) {
//            return null;
//        }
//        List<TetroidIcon> res = new ArrayList<>();
//        for (final File fileEntry : folder.listFiles()) {
//            if (fileEntry.isFile()) {
//                String name = fileEntry.getName();
//                if (!name.toLowerCase().endsWith(".svg"))
//                    continue;
//                TetroidIcon icon = new TetroidIcon(folderName, name);
////                icon.loadIcon(context, getIconsFolderPath());
//                res.add(icon);
//            }
//        }
//        return res;
//    }
//
//    public static String getIconsFolderPath() {
//        return DataManager.Instance.mStoragePath + SEPAR + ICONS_FOLDER_NAME;
//    }
}
