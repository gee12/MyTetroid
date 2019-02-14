package com.gee12.mytetroid.views;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.SettingsManager;
import com.gee12.mytetroid.data.TetroidRecord;

import java.util.List;

public class RecordsListAdapter extends BaseAdapter {

    public interface OnRecordAttachmentClickListener {
        /**
         * Вызывается при клике на иконке прикрепленных файлов к записи
         *
         * @param record Объект записи
         */
        void onClick(TetroidRecord record);
    }

    /**
     *
     */
    private class RecordViewHolder {
        TextView lineNumView;
        TextView nameView;
        TextView infoView;
        ImageView attachedView;
    }

    private static LayoutInflater inflater = null;
    private List<TetroidRecord> dataSet;
    private OnRecordAttachmentClickListener onRecordAttachmentClickListener;

    public RecordsListAdapter(Context context, OnRecordAttachmentClickListener onRecordAttachmentClickListener) {
        super();
        this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.onRecordAttachmentClickListener = onRecordAttachmentClickListener;
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
        return dataSet.get(position).getId().hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RecordViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new RecordViewHolder();
            convertView = inflater.inflate(R.layout.list_item_record, null);
            viewHolder.lineNumView = (TextView) convertView.findViewById(R.id.record_view_line_num);
            viewHolder.nameView = (TextView) convertView.findViewById(R.id.record_view_name);
            viewHolder.infoView = (TextView) convertView.findViewById(R.id.record_view_info);
            viewHolder.attachedView = (ImageView) convertView.findViewById(R.id.record_view_attached);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (RecordViewHolder) convertView.getTag();
        }

        final TetroidRecord record = dataSet.get(position);
        // номер строки
        viewHolder.lineNumView.setText(String.valueOf(position));
        // название записи
        viewHolder.nameView.setText(record.getName());
        // другая информация о записи
        if (record.getCreated() != null)
            viewHolder.infoView.setText(record.getCreatedString());
        // есть ли прикрепленные файлы
        if (record.getAttachedFilesCount() > 0) {
            // если установлено в настройках, меняем фон
            if (SettingsManager.isHighlightRecordWithAttach()) {

                // ???
                convertView.setBackgroundColor(SettingsManager.highlightAttachColor());
            }
            viewHolder.attachedView.setVisibility(View.VISIBLE);
            viewHolder.attachedView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onRecordAttachmentClickListener.onClick(record);
                }
            });
        }
        else {
            convertView.setBackgroundColor(Color.TRANSPARENT);
            viewHolder.attachedView.setVisibility(View.GONE);
        }

        return convertView;
    }
}
