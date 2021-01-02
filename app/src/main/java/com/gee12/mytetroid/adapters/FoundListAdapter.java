package com.gee12.mytetroid.adapters;

import android.content.Context;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.model.FoundType;
import com.gee12.mytetroid.model.ITetroidObject;
import com.gee12.mytetroid.model.TetroidRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FoundListAdapter extends RecordsBaseListAdapter {

    /**
     *
     */
    private static class FoundViewHolder extends RecordViewHolder {
//        TextView lineNumView;
//        TextView nameView;
//        TextView nodeNameView;
        TextView typeView;
    }

    private final List<ITetroidObject> dataSet;
    private HashMap<ITetroidObject, FoundType> hashMap;

    public FoundListAdapter(Context context, OnRecordAttachmentClickListener onAttachmentClickListener/*, List<ITetroidObject> dataSet*/) {
        super(context, onAttachmentClickListener);
//        this.dataSet = dataSet;
//        this.dataSet = new HashMap<>();
        this.dataSet = new ArrayList<>();
        this.isShowNodeName = true;
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

        ITetroidObject foundObject = dataSet.get(position);
        if (foundObject instanceof TetroidRecord) {
            // если найденный объект - запись, то выводим его в списке как обычную запись
            convertView = getView(position, convertView, (TetroidRecord) foundObject);
            viewHolder = (FoundViewHolder)convertView.getTag();
        } else {

            if (convertView == null) {
                viewHolder = new FoundViewHolder();
                convertView = inflater.inflate(R.layout.list_item_found, null);
                viewHolder.lineNumView = convertView.findViewById(R.id.record_view_line_num);
                viewHolder.nameView = convertView.findViewById(R.id.record_view_name);
                viewHolder.nodeNameView = convertView.findViewById(R.id.record_view_node);
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
            /*if (found.getType() == FoundType.TYPE_RECORD) {
                TetroidRecord record = (TetroidRecord)found;
                if (record != null) {
                    viewHolder.nodeNameView.setVisibility(View.VISIBLE);
                    viewHolder.nodeNameView.setText(record.getNode().getName());
                }
            } else {
                viewHolder.nodeNameView.setVisibility(View.GONE);
            }*/
            viewHolder.nodeNameView.setVisibility(View.GONE);
        }
        // тип найденного объекта
        String foundInString = hashMap.get(foundObject).getTypeString(context);
        viewHolder.typeView.setText(context.getString(R.string.title_founded_in_mask, foundInString));

        return convertView;
    }
}
