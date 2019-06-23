package com.gee12.mytetroid.activities;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.FoundObject;
import com.gee12.mytetroid.views.FoundListAdapter;
import com.gee12.mytetroid.views.TetroidFragment;

import java.util.List;

public class FoundPageFragment extends TetroidFragment {

    ListView lvFound;
    FoundListAdapter listAdapter;
    TextView tvEmpty;
    int foundCount;

//    public FoundPageFragment(IMainView mainView) {
//        super(mainView);
//    }

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
        this.lvFound = rootView.findViewById(R.id.list_view_found);
        this.tvEmpty = rootView.findViewById(R.id.text_view_empty_found);
        lvFound.setEmptyView(tvEmpty);
        registerForContextMenu(lvFound);
        lvFound.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openFoundObject(position);
            }
        });

        this.listAdapter = new FoundListAdapter(this.getContext());
        lvFound.setAdapter(listAdapter);
        setMainView(getArguments());

        return rootView;
    }

    public void setFounds(List<FoundObject> found, String query) {
//        this.listAdapter = new FoundListAdapter(this.getContext(), found);
        this.listAdapter.setDataItems(found);
        this.foundCount = found.size();
        if (found.isEmpty()) {
            tvEmpty.setText(String.format("", query));
        }
    }

    private void openFoundObject(int position) {
        FoundObject found = (FoundObject) listAdapter.getItem(position);
        mainView.openFoundObject(found);
    }

    @Override
    public String getTitle() {
        if (getContext() != null)
            return String.format(getString(R.string.found_mask), foundCount);
        else
            return null;
    }
}
