package com.gee12.mytetroid.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.core.view.GestureDetectorCompat;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.adapters.FoundListAdapter;
import com.gee12.mytetroid.adapters.RecordsBaseListAdapter;
import com.gee12.mytetroid.data.ScanManager;
import com.gee12.mytetroid.model.FoundType;
import com.gee12.mytetroid.model.ITetroidObject;

import java.util.HashMap;

public class FoundPageFragment extends TetroidFragment {

    private ListView mListViewFound;
    private FoundListAdapter mListAdapter;
    private TextView mTextViewEmpty;
    private int mFoundCount;

    public FoundPageFragment(GestureDetectorCompat detector) {
        super(detector);
    }

    public FoundPageFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_found, container, false);
        // ?
//        rootView.setOnTouchListener(this);
//        RelativeLayout rl = rootView.findViewById(R.id.layout_found);
//        rl.setOnTouchListener(this);

        // пустое пространство под списками
        View footerView =  ((LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.list_view_empty_footer, null, false);


        this.mListViewFound = rootView.findViewById(R.id.list_view_found);
        this.mTextViewEmpty = rootView.findViewById(R.id.text_view_empty_found);
        mListViewFound.setEmptyView(mTextViewEmpty);
        mListViewFound.addFooterView(footerView, null, false);
        registerForContextMenu(mListViewFound);
        mListViewFound.setOnItemClickListener((parent, view, position, id) -> openFoundObject(position));
        mListViewFound.setOnTouchListener(this);

//        this.mListAdapter = new FoundListAdapter(mContext);
//        mListViewFound.setAdapter(mListAdapter);
        setMainView(getArguments());

        rootView.findViewById(R.id.button_research).setOnClickListener(view -> mMainView.research());
        rootView.findViewById(R.id.button_close).setOnClickListener(view -> mMainView.closeFoundFragment());



        return rootView;
    }

    public void setFounds(HashMap<ITetroidObject, FoundType> found, ScanManager scan) {
        RecordsBaseListAdapter.OnRecordAttachmentClickListener mOnAttachmentClickListener = record -> {
            if (mMainView != null) {
                mMainView.openRecordAttaches(record);
            }
        };
        this.mListAdapter = new FoundListAdapter(mContext, mOnAttachmentClickListener);
        mListViewFound.setAdapter(mListAdapter);
        mListAdapter.setDataItems(found);
        this.mFoundCount = found.size();
        if (found.isEmpty()) {
            if (scan.isSearchInNode() && scan.getNode() != null) {
                mTextViewEmpty.setText(String.format(getString(R.string.global_search_not_found_in_node),
                        scan.getQuery(), scan.getNode().getName()));
            } else {
                mTextViewEmpty.setText(String.format(getString(R.string.global_search_not_found), scan.getQuery()));
            }
        }
    }

    private void openFoundObject(int position) {
        ITetroidObject found = (ITetroidObject) mListAdapter.getItem(position);
        mMainView.openFoundObject(found);
    }

    @Override
    public String getTitle() {
        if (mContext != null)
            return String.format(getString(R.string.search_found_mask), mFoundCount);
        else
            return null;
    }

    /**
     * Обработчик нажатия кнопки Назад
     */
    public boolean onBackPressed() {
        mMainView.openMainPage();
        return true;
    }

}
