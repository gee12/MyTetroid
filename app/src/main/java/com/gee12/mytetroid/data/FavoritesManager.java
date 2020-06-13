package com.gee12.mytetroid.data;

import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.utils.Utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FavoritesManager extends RecordsManager {

    public static final TetroidNode FAVORITES_NODE = new TetroidNode("", "", 0);

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
            saveFavorites();
        }
        record.setIsFavorite(true);
        return true;
    }

    public static boolean remove(TetroidRecord record) {
        if (record == null)
            return false;
        if (instance.mFavoritesRecords.remove(record) && mFavoritesIds.remove(record.getId())) {
            saveFavorites();
            record.setIsFavorite(false);
        } else {
            return false;
        }
        return true;
    }

    public static boolean isCryptedAndNonDecrypted() {
        for (TetroidRecord record : instance.mFavoritesRecords) {
            if (!record.isNonCryptedOrDecrypted())
                return true;
        }
        return false;
    }

    /**
     * Замена местами 2 изранных записи в списке.
     * @param pos
     * @param isUp
     * @return 1 - успешно
     *         0 - перемещение невозможно (пограничный элемент)
     *        -1 - ошибка
     */
    public static int swapRecords(int pos, boolean isUp) {
        boolean isSwapped = Utils.swapListItems(instance.mFavoritesRecords, pos, isUp);
        if (isSwapped) {
            Collections.swap(mFavoritesIds, pos - ((isUp) ? 1 : 0), pos + ((isUp) ? 0 : 1));
            saveFavorites();
            return 1;
        }
        return 0;
    }

    protected static void saveFavorites() {
        SettingsManager.setFavorites(mFavoritesIds.toArray(new String[1]));
    }

    public static List<TetroidRecord> getFavoritesRecords() {
        return instance.mFavoritesRecords;
    }

}
