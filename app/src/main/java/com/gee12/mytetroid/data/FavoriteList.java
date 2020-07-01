package com.gee12.mytetroid.data;

import androidx.annotation.Nullable;

import com.gee12.mytetroid.model.TetroidRecord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Вспомогательный класс для хранения списка избранных записей.
 * Особенности:
 *   * наследование от List<TetroidRecord>, чтобы использовать список напрямую в ListViewAdapter
 *   * внутренний список mIds первоначально хранит Id записей, сохраненые в настройках,
 *  в нужном порядке. Этот список важен в процессе загрузки хранилища, т.к. с помощью него формируется сам
 *  список объектов избранных записей в правильной последовательности.
 *   * изменение порядка следования элементов реализуется просто с помощью Collections.swap(),
 *  что не получилось бы при использовании, например, HashMap вместо List
 */
public class FavoriteList extends ArrayList<TetroidRecord> {

    protected List<String> mIds = new ArrayList<>();

    public FavoriteList(Collection<String> ids) {
        if (ids != null) {
            for (String id : ids) {
                add(id);
            }
        }
    }

    protected int getPosition(String id) {
        if (id == null)
            return -1;
        for (int i = 0; i < mIds.size(); i++) {
            if (id.equals(mIds.get(i)))
                return i;
        }
        return -1;
    }

    public boolean set(TetroidRecord record) {
        if (record == null)
            return false;
        int pos = getPosition(record.getId());
        if (pos >= 0) {
            super.set(pos, record);
            return true;
        }
        return false;
    }

    public void add(String id) {
        mIds.add(id);
        super.add(null);
    }

    @Override
    public boolean add(TetroidRecord record) {
        if (record == null)
            return false;
        String id = record.getId();
        if (!mIds.contains(id)) {
            if (super.add(record)) {
                mIds.add(id);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean remove(@Nullable Object o) {
        TetroidRecord record = (TetroidRecord) o;
        if (record == null)
            return false;
        String id = record.getId();
        if (mIds.remove(id)) {
            if (super.remove(o)) {
                return true;
            }
        }
        return false;
    }

    public void removeNull() {
        for (int i = 0; i < mIds.size(); i++) {
            if (get(i) == null) {
                mIds.remove(i);
                super.remove(i);
            }
        }
    }

    public boolean swap(int pos, boolean isUp) {
        if (isUp) {
            if (pos > 0) {
                Collections.swap(mIds, pos-1, pos);
                Collections.swap(this, pos-1, pos);
                return true;
            }
        } else {
            if (pos < size() - 1) {
                Collections.swap(mIds, pos, pos+1);
                Collections.swap(this, pos, pos+1);
                return true;
            }
        }
        return false;
    }

    public List<String> getIds() {
        return mIds;
    }
}
