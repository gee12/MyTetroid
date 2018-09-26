package com.gee12.mytetroid.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.TetroidRecord;

import java.util.List;

public class RecordsListAdapter extends BaseAdapter {

    /**
     *
     */
    private class RecordViewHolder {
        TextView lineNumView;
        TextView nameView;
        ImageView attachedFilesView;
    }

    private static LayoutInflater inflater = null;
    private List<TetroidRecord> dataSet;

    public RecordsListAdapter(Context context) {//}, List<TetroidRecord> dataSet) {
        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        this.dataSet = dataSet;
    }

    public void reset(List<TetroidRecord> dataSet) {
        this.dataSet = dataSet;
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

    @Override
    public long getItemId(int position) {
        return dataSet.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RecordViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new RecordViewHolder();
            convertView = inflater.inflate(R.layout.list_item_record, null);
            viewHolder.lineNumView = (TextView) convertView.findViewById(R.id.record_view_line_num);
            viewHolder.nameView = (TextView) convertView.findViewById(R.id.record_view_name);
            viewHolder.attachedFilesView = (ImageView) convertView.findViewById(R.id.record_view_attached);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (RecordViewHolder) convertView.getTag();
        }

        final TetroidRecord record = dataSet.get(position);
        // номер строки
        viewHolder.lineNumView.setText(String.valueOf(position));
        // название записи
        viewHolder.nameView.setText(record.getName());
        // есть ли прикрепленные файлы
        if (record.getAttachedFilesCount() > 0)
            viewHolder.attachedFilesView.setVisibility(View.VISIBLE);
        else
            viewHolder.attachedFilesView.setVisibility(View.GONE);

        return convertView;
    }
}
