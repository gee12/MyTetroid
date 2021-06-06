package com.gee12.mytetroid.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.model.TetroidStorage;

import java.util.ArrayList;
import java.util.List;

public class StoragesViewModel extends BaseViewModel {

    private MutableLiveData<List<TetroidStorage>> mStorages;

    public StoragesViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<TetroidStorage>> getStorages() {
        if (mStorages == null) {
            mStorages = new MutableLiveData<>();
            loadStorages();
        }
        return mStorages;
    }

    public void loadStorages() {
        List<TetroidStorage> storages = new ArrayList<>();
        // FIXME: получать список из базы
        storages.add(new TetroidStorage(DataManager.getStoragePath()));

        mStorages.postValue(storages);
    }
}
