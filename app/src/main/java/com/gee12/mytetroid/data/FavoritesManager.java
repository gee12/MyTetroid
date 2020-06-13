package com.gee12.mytetroid.data;

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

    public static boolean isFavorite(TetroidRecord record) {
        return instance.mFavoritesRecords.contains(record);
    }

    public static void load() {
        mFavoritesIds = Arrays.asList(SettingsManager.getFavorites());
    }

    public static boolean add(TetroidRecord record) {
        if (record == null)
            return false;
        if (!instance.mFavoritesRecords.contains(record)) {
            instance.mFavoritesRecords.add(record);
            mFavoritesIds.add(record.getId());
            SettingsManager.setFavorites(mFavoritesIds.toArray(new String[1]));
        }
        record.setIsFavorite(true);
        return true;
    }

    public static boolean remove(TetroidRecord record) {
        if (record == null)
            return false;
        if (instance.mFavoritesRecords.remove(record) && mFavoritesIds.remove(record.getId())) {
            SettingsManager.setFavorites(mFavoritesIds.toArray(new String[1]));
            record.setIsFavorite(false);
        } else {
            return false;
        }
        return true;
    }


    public static List<TetroidRecord> getFavoritesRecords() {
        return instance.mFavoritesRecords;
    }

}
