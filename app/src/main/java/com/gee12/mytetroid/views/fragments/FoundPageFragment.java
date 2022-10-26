package com.gee12.mytetroid.views.fragments;

import static org.koin.java.KoinJavaComponent.get;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.GestureDetectorCompat;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.common.Constants;
import com.gee12.mytetroid.model.SearchProfile;
import com.gee12.mytetroid.viewmodels.MainViewModel;
import com.gee12.mytetroid.views.adapters.FoundListAdapter;
import com.gee12.mytetroid.views.adapters.RecordsBaseListAdapter;
import com.gee12.mytetroid.model.FoundType;
import com.gee12.mytetroid.model.ITetroidObject;

import java.util.HashMap;

public class FoundPageFragment extends TetroidFragment<MainViewModel> {

    private ListView lvFound;
    private FoundListAdapter listAdapterFound;
    private TextView tvEmpty;
    private int foundCount;

    @Override
    protected Class<MainViewModel> getViewModelClazz() {
        return MainViewModel.class;
    }


    public FoundPageFragment(GestureDetectorCompat detector) {
        super(detector);
    }

    public FoundPageFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, R.layout.fragment_found);

        // ?
//        view.setOnTouchListener(this);
//        RelativeLayout rl = view.findViewById(R.id.layout_found);
//        rl.setOnTouchListener(this);

        // пустое пространство под списками
        View footerView =  ((LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.list_view_empty_footer, null, false);

        this.lvFound = view.findViewById(R.id.list_view_found);
        this.tvEmpty = view.findViewById(R.id.text_view_empty_found);
        lvFound.setEmptyView(tvEmpty);
        lvFound.addFooterView(footerView, null, false);
        registerForContextMenu(lvFound);
        lvFound.setOnItemClickListener((parent, view1, position, id) -> openFoundObject(position));
        lvFound.setOnTouchListener(this);

//        this.mListAdapter = new FoundListAdapter(mContext);
//        mListViewFound.setAdapter(mListAdapter);

        view.findViewById(R.id.button_research).setOnClickListener(view1 -> viewModel.research());
        view.findViewById(R.id.button_close).setOnClickListener(view1 -> viewModel.closeFoundFragment());

        return view;
    }

    @Override
    protected void initViewModel() {
        viewModel = get(MainViewModel.class);
    }

    public void setFounds(HashMap<ITetroidObject, FoundType> found, SearchProfile profile) {
        RecordsBaseListAdapter.OnRecordAttachmentClickListener mOnAttachmentClickListener = record -> {
            viewModel.showRecordAttaches(record, false);
        };
        this.listAdapterFound = new FoundListAdapter(
                getContext(),
                viewModel.getRecordsInteractor(),
                viewModel.getCommonSettingsInteractor(),
                mOnAttachmentClickListener
        );
        lvFound.setAdapter(listAdapterFound);
        listAdapterFound.setDataItems(found);
        this.foundCount = found.size();
        if (found.isEmpty()) {
            if (profile.isSearchInNode() && profile.getNode() != null) {
                tvEmpty.setText(String.format(getString(R.string.global_search_not_found_in_node),
                        profile.getQuery(), profile.getNode().getName()));
            } else {
                tvEmpty.setText(String.format(getString(R.string.global_search_not_found), profile.getQuery()));
            }
        }
    }

    private void openFoundObject(int position) {
        ITetroidObject found = (ITetroidObject) listAdapterFound.getItem(position);
        viewModel.openFoundObject(found);
    }

    @NonNull
    @Override
    public String getTitle() {
        if (getContext() != null)
            return String.format(getString(R.string.search_found_mask), foundCount);
        else
            return "";
    }

    /**
     * Обработчик нажатия кнопки Назад
     */
    public boolean onBackPressed() {
        viewModel.openPage(Constants.PAGE_MAIN);
        return true;
    }

}
