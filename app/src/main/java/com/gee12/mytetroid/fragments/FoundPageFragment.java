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
import com.gee12.mytetroid.data.ScanManager;
import com.gee12.mytetroid.model.FoundType;
import com.gee12.mytetroid.model.ITetroidObject;

import java.util.HashMap;

public class FoundPageFragment extends TetroidFragment {

    private ListView lvFound;
    private FoundListAdapter listAdapter;
    private TextView tvEmpty;
    private int foundCount;

    public FoundPageFragment(GestureDetectorCompat detector) {
        super(detector);
    }

    public FoundPageFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_found, container, false);
        // ?
//        rootView.setOnTouchListener(this);
//        RelativeLayout rl = rootView.findViewById(R.id.layout_found);
//        rl.setOnTouchListener(this);
        this.lvFound = rootView.findViewById(R.id.list_view_found);
        this.tvEmpty = rootView.findViewById(R.id.text_view_empty_found);
        lvFound.setEmptyView(tvEmpty);
        registerForContextMenu(lvFound);
        lvFound.setOnItemClickListener((parent, view, position, id) -> openFoundObject(position));
        lvFound.setOnTouchListener(this);

        this.listAdapter = new FoundListAdapter(getContext());
        lvFound.setAdapter(listAdapter);
        setMainView(getArguments());

        rootView.findViewById(R.id.button_research).setOnClickListener(view -> mMainView.reSearch());
        rootView.findViewById(R.id.button_close).setOnClickListener(view -> mMainView.closeFoundFragment());

        return rootView;
    }

    public void setFounds(HashMap<ITetroidObject, FoundType> found, ScanManager scan) {
        this.listAdapter = new FoundListAdapter(getContext());
        lvFound.setAdapter(listAdapter);
        listAdapter.setDataItems(found);
        this.foundCount = found.size();
        if (found.isEmpty()) {
            if (scan.isSearchInNode() && scan.getNode() != null) {
                tvEmpty.setText(String.format(getString(R.string.global_search_not_found_in_node),
                        scan.getQuery(), scan.getNode().getName()));
            } else {
                tvEmpty.setText(String.format(getString(R.string.global_search_not_found), scan.getQuery()));
            }
        }
    }

    private void openFoundObject(int position) {
        ITetroidObject found = (ITetroidObject) listAdapter.getItem(position);
        mMainView.openFoundObject(found);
    }

    @Override
    public String getTitle() {
        if (getContext() != null)
            return String.format(getString(R.string.search_found_mask), foundCount);
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
