package com.gee12.mytetroid.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.model.TetroidTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private ArrayList mData;
//    private Object[] keySet;
    private Context context;

//    public TagsListAdapter(Context context, TreeMap<String, List<TetroidRecord>> mData) {
//    public TagsListAdapter(Context context, TreeMap<String, TetroidTag> mData) {
    public TagsListAdapter(Context context, Map<String,TetroidTag> data) {
        this.context = context;
        this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        this.dataHashMap = mData;
//        this.mData = new ArrayList<>(mData.values());
        this.mData = new ArrayList<>(data.entrySet());
//        this.keySet = dataHashMap.keySet().toArray();
    }

//    public void setDataItems(TreeMap<String, List<TetroidRecord>> mData) {
//    public void setDataItems(TreeMap<String, TetroidTag> mData) {
    public void setDataItems(Map<String,TetroidTag> data) {
//        this.dataHashMap = mData;
//        this.mData = new ArrayList<>(mData.values());
        this.mData = new ArrayList<>(data.entrySet());
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
        return mData.size();
    }

    @Override
    public Map.Entry<String, TetroidTag> getItem(int position) {
//        return keySet[position];
        return (Map.Entry) mData.get(position);
    }

    @Override
    public long getItemId(int position) {
//        return keySet[position].hashCode();
        return mData.get(position).hashCode();
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
        Map.Entry<String, TetroidTag> item = getItem(position);
        final List<TetroidRecord> records = item.getValue().getRecords();
        // номер строки
//        viewHolder.lineNumView.setText(String.valueOf(position + 1));
        // название файла
//        viewHolder.nameView.setText((String)key);
        viewHolder.nameView.setText(item.getValue().getName());
        // размер файла
        viewHolder.recordsCountView.setText(String.format(Locale.getDefault(), "[%d]", records.size()));

        return convertView;
    }
}
