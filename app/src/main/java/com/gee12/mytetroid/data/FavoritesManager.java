package com.gee12.mytetroid.data;

import android.content.Context;

import com.gee12.mytetroid.App;
import com.gee12.mytetroid.logs.LogManager;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidRecord;

import java.util.List;

public class FavoritesManager {

    public static final TetroidNode FAVORITES_NODE = new TetroidNode("FAVORITES_NODE", "", 0);

    protected static FavoriteList mFavorites;

    /**
     * Первоначальное создание списка Id избранных записей.
     */
    public static void create() {
        mFavorites = new FavoriteList(null);
    }

    /**
     * Первоначальная загрузка списка Id избранных записей из настроек.
     */
    public static void load(Context context) {
        mFavorites = new FavoriteList(
                (App.isFullVersion()) ? SettingsManager.getFavorites(context) : null);
    }

    /**
     * Удаление из избранного не найденных в хранилище записей.
     */
    public static void check() {
       mFavorites.removeNull();
    }

    /**
     * Проверка состоит ли запись в списке избранных при загрузке хранилища.
     * @param id
     * @return
     */
    protected static boolean isFavorite(String id) {
        return (mFavorites.getPosition(id) >= 0);
    }

    /**
     * Установка объекта записи из хранилища по id.
     * @param record
     * @return
     */
    public static boolean set(TetroidRecord record) {
        boolean res = mFavorites.set(record);
        if (res) {
            record.setIsFavorite(true);
        }
        return res;
    }

    /**
     * Добавление новой записи в избранное.
     * @param record
     * @return
     */
    public static boolean add(Context context, TetroidRecord record) {
        boolean res = mFavorites.add(record);
        if (res) {
            record.setIsFavorite(true);
            saveFavorites(context);
        }
        return res;
    }

    /**
     * Удаление записи из избранного.
     * @param record
     * @param resetFlag Нужно ли сбрасывать флаг isFavorite у записи
     * @return
     */
    public static boolean remove(Context context, TetroidRecord record, boolean resetFlag) {
        boolean res = mFavorites.remove(record);
        if (res) {
            if (resetFlag) {
                record.setIsFavorite(false);
            }
            saveFavorites(context);
        }
        return res;
    }

    public static boolean addOrRemove(Context context, TetroidRecord record, boolean isFavor) {
        return (isFavor) ? add(context, record) : remove(context, record, true);
    }

    /**
     * Замена местами 2 изранных записи в списке.
     * @param pos
     * @param isUp
     * @return 1 - успешно
     *         0 - перемещение невозможно (пограничный элемент)
     *        -1 - ошибка
     */
    public static int swapRecords(Context context, int pos, boolean isUp, boolean through) {
        boolean isSwapped;
        try {
            isSwapped = mFavorites.swap(pos, isUp, through);
        } catch (Exception ex) {
            LogManager.log(context, ex, -1);
            return -1;
        }

        if (isSwapped) {
            saveFavorites(context);
            return 1;
        }
        return 0;
    }

    /**
     * Сохранение списка Id избранных записей в настройках.
     */
    protected static void saveFavorites(Context context) {
        /*String[] ids = new String[mFavorites.size()];
        for (int i = 0; i < mFavorites.size(); i++) {
            TetroidRecord record = mFavorites.get(i);
            if (record != null) {
                ids[i] = record.getId();
            }
        }
        SettingsManager.setFavorites(ids);*/
        SettingsManager.setFavorites(context, mFavorites.getIds());
    }

    public static List<TetroidRecord> getFavoritesRecords() {
        return mFavorites;
    }

}
