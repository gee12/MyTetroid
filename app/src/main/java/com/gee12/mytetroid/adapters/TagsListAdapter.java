package com.gee12.mytetroid.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.TetroidRecord;
import com.gee12.mytetroid.data.TetroidTag;

import java.util.List;

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
//    private TreeMap<String, TetroidTag> dataHashMap;
    private List<TetroidTag> data;
//    private Object[] keySet;
    private Context context;

//    public TagsListAdapter(Context context, TreeMap<String, List<TetroidRecord>> data) {
//    public TagsListAdapter(Context context, TreeMap<String, TetroidTag> data) {
    public TagsListAdapter(Context context, List<TetroidTag> data) {
        this.context = context;
        this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        this.dataHashMap = data;
//        this.data = new ArrayList<>(data.values());
        this.data = data;
//        this.keySet = dataHashMap.keySet().toArray();
    }

//    public void setDataItems(TreeMap<String, List<TetroidRecord>> data) {
//    public void setDataItems(TreeMap<String, TetroidTag> data) {
    public void setDataItems(List<TetroidTag> data) {
//        this.dataHashMap = data;
//        this.data = new ArrayList<>(data.values());
        this.data = data;
//        onDataSetChanged();
        notifyDataSetChanged();
    }

//    public void onDataSetChanged() {
////        this.keySet = dataHashMap.keySet().toArray();
//        this.keySet = dataHashMap.keySet().toArray();
//        notifyDataSetChanged();
//    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
//        return keySet[position];
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
//        return keySet[position].hashCode();
        return data.get(position).hashCode();
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

//        Object key = keySet[position];
//        final List<TetroidRecord> records = dataHashMap.get(key).getRecords();
        TetroidTag tag = data.get(position);
        final List<TetroidRecord> records = tag.getRecords();
        // номер строки
//        viewHolder.lineNumView.setText(String.valueOf(position + 1));
        // название файла
//        viewHolder.nameView.setText((String)key);
        viewHolder.nameView.setText(tag.getName());
        // размер файла
        viewHolder.recordsCountView.setText(String.format("[%d]", records.size()));

        return convertView;
    }
}
