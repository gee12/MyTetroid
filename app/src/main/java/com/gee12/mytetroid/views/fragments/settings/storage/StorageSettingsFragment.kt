package com.gee12.mytetroid.views.fragments.settings.storage

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.gee12.mytetroid.R
import com.gee12.mytetroid.viewmodels.StorageSettingsViewModel
import com.gee12.mytetroid.viewmodels.StorageViewModelFactory
import com.gee12.mytetroid.views.fragments.settings.TetroidSettingsFragment

class StorageSettingsFragment : TetroidSettingsFragment() {

    private lateinit var mViewModel: StorageSettingsViewModel

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.storage_prefs, rootKey)
        requireActivity().setTitle(R.string.title_storage_settings)

        mViewModel = ViewModelProvider(requireActivity(), StorageViewModelFactory(application))
            .get(StorageSettingsViewModel::class.java)

//        val storageId = arguments?.get(EXTRA_STORAGE_ID) as Int
//        mViewModel.loadStorage(storageId)

        mViewModel.storage.observe(this, { storage ->
            setTitle(R.string.title_storage_settings, storage.name)
        })
    }

//    companion object {
//        const val EXTRA_STORAGE_ID = "EXTRA_STORAGE_ID"
//
//        const val EXTRA_IS_REINIT_STORAGE = "EXTRA_IS_REINIT_STORAGE"
//        const val EXTRA_IS_CREATE_STORAGE = "EXTRA_IS_CREATE_STORAGE"
//        const val EXTRA_IS_LOAD_STORAGE = "EXTRA_IS_LOAD_STORAGE"
//        const val EXTRA_IS_LOAD_ALL_NODES = "EXTRA_IS_LOAD_ALL_NODES"
//        const val EXTRA_IS_PASS_CHANGED = "EXTRA_IS_PASS_CHANGED"
//        const val REQUEST_CODE_OPEN_STORAGE_PATH = 1
//        const val REQUEST_CODE_CREATE_STORAGE_PATH = 2
//        const val REQUEST_CODE_OPEN_TEMP_PATH = 3
//
////        fun newInstance(storageId: Int): StorageSettingsFragment {
////            val args = Bundle()
////            args.putInt(EXTRA_STORAGE_ID, storageId)
////            val fragment = StorageSettingsFragment()
////            fragment.arguments = args
////            return fragment
////        }
//    }
}