package com.gee12.mytetroid.views;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.activities.MainPageFragment;
import com.gee12.mytetroid.data.FoundType;
import com.gee12.mytetroid.data.ITetroidObject;
import com.gee12.mytetroid.data.TetroidRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FoundListAdapter extends BaseAdapter {

    /**
     *
     */
    private class FoundViewHolder {
        TextView lineNumView;
        TextView nameView;
        TextView nodeNameView;
        TextView typeView;
    }

    private LayoutInflater inflater;
    private List<ITetroidObject> dataSet;
    private HashMap<ITetroidObject, FoundType> hashMap;
    private Context context;

    public FoundListAdapter(Context context/*, List<ITetroidObject> dataSet*/) {
        super();
        this.context = context;
        this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        this.dataSet = dataSet;
//        this.dataSet = new HashMap<>();
        this.dataSet = new ArrayList<>();
    }

//    public void setDataItems(List<ITetroidObject> dataSet) {
    public void setDataItems(HashMap<ITetroidObject, FoundType> hashMap) {
        this.hashMap = hashMap;
        this.dataSet.addAll(hashMap.keySet());
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return dataSet.size();
    }

    @Override
    public Object getItem(int position) {
        return dataSet.get(position);
    }

    public Pair<ITetroidObject,FoundType> getFoundObject(int position) {
        ITetroidObject obj = dataSet.get(position);
        FoundType type = hashMap.get(obj);
        return new Pair<>(obj,type);
    }

    @Override
    public long getItemId(int position) {
        return dataSet.get(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FoundViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new FoundViewHolder();
            convertView = inflater.inflate(R.layout.list_item_found, null);
            viewHolder.lineNumView = convertView.findViewById(R.id.found_view_line_num);
            viewHolder.nameView = convertView.findViewById(R.id.found_view_name);
            viewHolder.nodeNameView = convertView.findViewById(R.id.found_view_node);
            viewHolder.typeView = convertView.findViewById(R.id.found_view_type);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (FoundViewHolder)convertView.getTag();
        }

        final ITetroidObject found = dataSet.get(position);
        // номер строки
        viewHolder.lineNumView.setText(String.valueOf(position + 1));
        // название найденного объекта
        viewHolder.nameView.setText(found.getName());
        if (found.getType() == FoundType.TYPE_RECORD) {
            TetroidRecord record = (TetroidRecord)found;
            if (record != null) {
                viewHolder.nodeNameView.setVisibility(View.VISIBLE);
                viewHolder.nodeNameView.setText(record.getNode().getName());
            }
        } else {
            viewHolder.nodeNameView.setVisibility(View.GONE);
        }
        // тип найденного объекта
        viewHolder.typeView.setText(hashMap.get(found).getTypeString(context));

        return convertView;
    }
}
