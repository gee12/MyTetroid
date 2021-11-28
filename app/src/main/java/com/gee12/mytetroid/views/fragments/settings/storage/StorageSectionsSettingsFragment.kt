package com.gee12.mytetroid.views.fragments.settings.storage

import android.os.Bundle
import com.gee12.mytetroid.R
import com.gee12.mytetroid.model.TetroidStorage

/**
 * Фрагмент с разделами настроек хранилища.
 */
class StorageSectionsSettingsFragment : TetroidStorageSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.storage_prefs, rootKey)

//        val storageId = arguments?.get(EXTRA_STORAGE_ID) as Int
//        mViewModel.loadStorage(storageId)
    }

    override fun onStorageInited(storage: TetroidStorage) {
        setTitle(R.string.title_storage_settings, storage.name)
    }

//        companion object {
//        fun newInstance(storageId: Int): StorageSettingsFragment {
//            val args = Bundle()
//            args.putInt(EXTRA_STORAGE_ID, storageId)
//            val fragment = StorageSettingsFragment()
//            fragment.arguments = args
//            return fragment
//        }
//    }
}