package com.gee12.mytetroid.views.fragments.settings.storage

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.model.TetroidStorage
import com.gee12.mytetroid.viewmodels.StorageSettingsViewModel
import com.gee12.mytetroid.viewmodels.factory.TetroidViewModelFactory
import com.gee12.mytetroid.views.fragments.settings.TetroidSettingsFragment

class StorageSettingsFragment : TetroidSettingsFragment() {

    private lateinit var viewModel: StorageSettingsViewModel

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.storage_prefs, rootKey)
        requireActivity().setTitle(R.string.title_storage_settings)

        viewModel = ViewModelProvider(requireActivity(), TetroidViewModelFactory(application))
            .get(StorageSettingsViewModel::class.java)

//        val storageId = arguments?.get(EXTRA_STORAGE_ID) as Int
//        mViewModel.loadStorage(storageId)

        viewModel.storageEvent.observe(this, { event ->
            if (event.state == Constants.StorageEvents.Changed) {
                setTitle(R.string.title_storage_settings, (event.data as? TetroidStorage)?.name)
            }
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