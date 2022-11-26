package com.gee12.mytetroid.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import android.widget.TextView
import androidx.core.view.GestureDetectorCompat
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.model.FoundType
import com.gee12.mytetroid.model.ITetroidObject
import com.gee12.mytetroid.model.SearchProfile
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.viewmodels.MainViewModel
import com.gee12.mytetroid.ui.adapters.FoundListAdapter
import org.koin.java.KoinJavaComponent.get

class FoundPageFragment : TetroidFragment<MainViewModel> {
    
    private lateinit var lvFound: ListView
    private lateinit var listAdapterFound: FoundListAdapter
    private lateinit var tvEmpty: TextView
    private var foundCount = 0

    constructor(viewModel: MainViewModel, detector: GestureDetectorCompat) : super(detector) {
        this.viewModel = viewModel
    }

    constructor() : super() {}

    override fun createViewModel() {
        if (viewModel == null) {
            viewModel = get(MainViewModel::class.java)
        }
    }

    override fun getTitle(): String {
        return viewModel.getString(R.string.search_found_mask, foundCount)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = super.onCreateView(inflater, container, R.layout.fragment_found)!!

        // ?
//        view.setOnTouchListener(this);
//        RelativeLayout rl = view.findViewById(R.id.layout_found);
//        rl.setOnTouchListener(this);

        // пустое пространство под списками
        val footerView = (requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
            .inflate(R.layout.list_view_empty_footer, null, false)
        lvFound = view.findViewById(R.id.list_view_found)
        tvEmpty = view.findViewById(R.id.text_view_empty_found)
        lvFound.emptyView = tvEmpty
        lvFound.addFooterView(footerView, null, false)
        registerForContextMenu(lvFound)
        lvFound.setOnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            openFoundObject(position)
        }
        lvFound.setOnTouchListener(this)

//        this.mListAdapter = new FoundListAdapter(mContext);
//        mListViewFound.setAdapter(mListAdapter);
        view.findViewById<View>(R.id.button_research).setOnClickListener { 
            viewModel.research() 
        }
        view.findViewById<View>(R.id.button_close).setOnClickListener { 
            viewModel.closeFoundFragment() 
        }
        return view
    }

    fun setFounds(found: HashMap<ITetroidObject, FoundType>, profile: SearchProfile) {
        listAdapterFound = FoundListAdapter(
            context,
            viewModel.recordsInteractor,
            viewModel.commonSettingsProvider
        ) { record: TetroidRecord? ->
            viewModel.showRecordAttaches(record, false)
        }
        lvFound.adapter = listAdapterFound
        listAdapterFound.setDataItems(found)
        foundCount = found.size
        if (found.isEmpty()) {
            if (profile.isSearchInNode && profile.node != null) {
                tvEmpty.text = getString(R.string.global_search_not_found_in_node,
                    profile.query,
                    profile.node?.name
                )
            } else {
                tvEmpty.text = getString(R.string.global_search_not_found, profile.query)
            }
        }
    }

    private fun openFoundObject(position: Int) {
        val found = listAdapterFound.getItem(position) as ITetroidObject
        viewModel.openFoundObject(found)
    }

    fun onBackPressed(): Boolean {
        viewModel.openPage(Constants.PAGE_MAIN)
        return true
    }

}