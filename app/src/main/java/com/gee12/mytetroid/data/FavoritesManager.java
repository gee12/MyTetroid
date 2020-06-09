package com.gee12.mytetroid.data;

import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.TetroidLog;
import com.gee12.mytetroid.model.TetroidRecord;

import java.util.Arrays;
import java.util.List;

public class FavoritesManager extends RecordsManager {

    protected static List<String> mFavoritesIds;

    protected static boolean isFavorite(String id) {
        if (id == null)
            return false;
        for (String favorId : mFavoritesIds) {
            if (id.equals(favorId))
                return true;
        }
        return false;
    }

    public static void load() {
        mFavoritesIds = Arrays.asList(SettingsManager.getFavorites());
    }

    public static void add(TetroidRecord record) {
        if (record == null)
            return;
        mFavoritesIds.add(record.getId());
        SettingsManager.setFavorites(mFavoritesIds.toArray(new String[1]));
        LogManager.log("" + TetroidLog.getIdNameString(record), LogManager.Types.INFO);
    }

    public static void remove(TetroidRecord record) {
        if (record == null)
            return;
        if (mFavoritesIds.remove(record.getId())) {
            SettingsManager.setFavorites(mFavoritesIds.toArray(new String[1]));
            LogManager.log("" + TetroidLog.getIdNameString(record), LogManager.Types.INFO);
        } else {
            LogManager.log(String.format("Не удалось найти запись с id=%s в списке избранных записей", record.getId()), LogManager.Types.WARNING);
        }
    }
}
