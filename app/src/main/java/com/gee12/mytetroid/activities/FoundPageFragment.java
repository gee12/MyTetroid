package com.gee12.mytetroid.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    public FoundPageFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_found, container, false);
        this.lvFound = rootView.findViewById(R.id.list_view_found);
        this.tvEmpty = rootView.findViewById(R.id.text_view_empty_found);
        lvFound.setEmptyView(tvEmpty);
//        registerForContextMenu(lvFound);

        // ...
        String query = null;
        List<FoundObject> found = null;
        this.listAdapter = new FoundListAdapter(this.getContext(), found);
        lvFound.setAdapter(listAdapter);
        if (found.isEmpty()) {
            tvEmpty.setText(String.format("", query));
        }

        return rootView;
    }

    @Override
    public String getTitle() {
        return String.format("Найдено: %d", 42);
    }
}
