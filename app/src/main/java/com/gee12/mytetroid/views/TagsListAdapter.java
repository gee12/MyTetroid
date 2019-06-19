package com.gee12.mytetroid.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.TetroidFile;
import com.gee12.mytetroid.data.TetroidRecord;
import com.gee12.mytetroid.data.TetroidTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class TagsListAdapter extends BaseAdapter {

    /**
     *
     */
    private class FileViewHolder {
//        TextView lineNumView;
        TextView nameView;
        TextView recordsCountView;
    }

    private LayoutInflater inflater;
//    private TreeMap<String, List<TetroidRecord>> dataHashMap;
    private TreeMap<String, TetroidTag> dataHashMap;
    private Object[] keySet;
    private Context context;

//    public TagsListAdapter(Context context, TreeMap<String, List<TetroidRecord>> data) {
    public TagsListAdapter(Context context, TreeMap<String, TetroidTag> data) {
        this.context = context;
        this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.dataHashMap = data;
        this.keySet = dataHashMap.keySet().toArray();
    }

//    public void setDataItems(TreeMap<String, List<TetroidRecord>> data) {
    public void setDataItems(TreeMap<String, TetroidTag> data) {
        this.dataHashMap = data;
        onDataSetChanged();
    }

    public void onDataSetChanged() {
        this.keySet = dataHashMap.keySet().toArray();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return dataHashMap.size();
    }

    @Override
    public Object getItem(int position) {
        return keySet[position];
    }

    @Override
    public long getItemId(int position) {
        return keySet[position].hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FileViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new FileViewHolder();
            convertView = inflater.inflate(R.layout.list_item_tag, null);
//            viewHolder.lineNumView = convertView.findViewById(R.id.tag_view_line_num);
            viewHolder.nameView = convertView.findViewById(R.id.tag_view_name);
            viewHolder.recordsCountView = convertView.findViewById(R.id.tag_view_records_count);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (FileViewHolder) convertView.getTag();
        }

        Object key = keySet[position];
        final List<TetroidRecord> records = dataHashMap.get(key).getRecords();
        // номер строки
//        viewHolder.lineNumView.setText(String.valueOf(position + 1));
        // название файла
        viewHolder.nameView.setText((String)key);
        // размер файла
        viewHolder.recordsCountView.setText(String.format("[%d]", records.size()));

        return convertView;
    }
}
