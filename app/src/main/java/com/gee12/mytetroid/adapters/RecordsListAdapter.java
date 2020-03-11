package com.gee12.mytetroid.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.SettingsManager;
import com.gee12.mytetroid.fragments.MainPageFragment;
import com.gee12.mytetroid.model.TetroidRecord;

import java.util.ArrayList;
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
        TextView nodeNameView;
        TextView infoView;
        ImageView attachedView;
    }

    private LayoutInflater inflater;
    private List<TetroidRecord> dataSet;
    private OnRecordAttachmentClickListener onRecordAttachmentClickListener;
    private Context context;
    boolean isShowNodeName;
    String dateTimeFormat;

    public RecordsListAdapter(Context context, OnRecordAttachmentClickListener onRecordAttachmentClickListener) {
        super();
        this.context = context;
        this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.onRecordAttachmentClickListener = onRecordAttachmentClickListener;
        this.dataSet = new ArrayList<>();
    }

    public void setDataItems(List<TetroidRecord> dataSet, int viewId, String dateTimeFormat) {
        this.dataSet = dataSet;
        this.isShowNodeName = (viewId == MainPageFragment.MAIN_VIEW_TAG_RECORDS);
        this.dateTimeFormat = dateTimeFormat;
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
            viewHolder.lineNumView = convertView.findViewById(R.id.record_view_line_num);
            viewHolder.nameView = convertView.findViewById(R.id.record_view_name);
            viewHolder.nodeNameView = convertView.findViewById(R.id.record_view_node);
            viewHolder.infoView = convertView.findViewById(R.id.record_view_info);
            viewHolder.attachedView = convertView.findViewById(R.id.record_view_attached);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (RecordViewHolder) convertView.getTag();
        }

        final TetroidRecord record = dataSet.get(position);
        // номер строки
        viewHolder.lineNumView.setText(String.valueOf(position + 1));
        // название записи
        viewHolder.nameView.setText(record.getName());
        // название ветки
        if (isShowNodeName) {
            viewHolder.nodeNameView.setVisibility(View.VISIBLE);
            viewHolder.nodeNameView.setText(record.getNode().getName());
        } else {
            viewHolder.nodeNameView.setVisibility(View.GONE);
        }
        // другая информация о записи
        if (record.getCreated() != null)
            viewHolder.infoView.setText(record.getCreatedString(dateTimeFormat));
        // есть ли прикрепленные файлы
        if (record.getAttachedFilesCount() > 0) {
            // если установлено в настройках, меняем фон
            if (SettingsManager.IsHighlightAttachCache) {
                convertView.setBackgroundColor(SettingsManager.HighlightAttachColorCache);
            }
            viewHolder.attachedView.setVisibility(View.VISIBLE);
            viewHolder.attachedView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onRecordAttachmentClickListener.onClick(record);
                }
            });
            // API>=17..
//            ((RelativeLayout.LayoutParams)viewHolder.nameView.getLayoutParams()).setMarginEnd(context.getResources().getDimensionPixelOffset(R.dimen.record_attached_image_width));
            ((RelativeLayout.LayoutParams)viewHolder.nameView.getLayoutParams()).setMargins(0,0,
                    context.getResources().getDimensionPixelOffset(R.dimen.record_attached_image_width),0);
        }
        else {
            convertView.setBackgroundColor(Color.TRANSPARENT);
            viewHolder.attachedView.setVisibility(View.GONE);
            ((RelativeLayout.LayoutParams)viewHolder.nameView.getLayoutParams()).setMargins(0,0,0,0);
        }

        return convertView;
    }

    public List<TetroidRecord> getDataSet() {
        return dataSet;
    }
}
