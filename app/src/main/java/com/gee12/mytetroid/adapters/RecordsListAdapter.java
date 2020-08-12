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

import androidx.core.content.ContextCompat;

import com.gee12.mytetroid.App;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.fragments.MainPageFragment;
import com.gee12.mytetroid.model.TetroidNode;
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
        ImageView iconView;
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
        this.isShowNodeName = (viewId == MainPageFragment.MAIN_VIEW_TAG_RECORDS
                || viewId == MainPageFragment.MAIN_VIEW_FAVORITES);
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
            viewHolder.iconView = convertView.findViewById(R.id.record_view_icon);
            viewHolder.nameView = convertView.findViewById(R.id.record_view_name);
            viewHolder.nodeNameView = convertView.findViewById(R.id.record_view_node);
            viewHolder.infoView = convertView.findViewById(R.id.record_view_created);
            viewHolder.attachedView = convertView.findViewById(R.id.record_view_attached);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (RecordViewHolder) convertView.getTag();
        }

        final TetroidRecord record = dataSet.get(position);
        boolean nonCryptedOrDecrypted = record.isNonCryptedOrDecrypted();
        // иконка
//        viewHolder.iconView.setVisibility((!nonCryptedOrDecrypted) ? View.VISIBLE : View.GONE);
        if (!nonCryptedOrDecrypted) {
            viewHolder.iconView.setVisibility(View.VISIBLE);
            viewHolder.iconView.setImageResource(R.drawable.ic_crypted_node);
        } else if (record.isFavorite()) {
            viewHolder.iconView.setVisibility(View.VISIBLE);
            viewHolder.iconView.setImageResource(R.drawable.ic_favorites_yellow);
        } else {
            viewHolder.iconView.setVisibility(View.GONE);
        }
        // номер строки
        viewHolder.lineNumView.setText(String.valueOf(position + 1));
        // название
        String cryptedName = context.getString(R.string.title_crypted_node_name);
        viewHolder.nameView.setText(record.getCryptedName(cryptedName));
        viewHolder.nameView.setTextColor(ContextCompat.getColor(context,
                (nonCryptedOrDecrypted) ? R.color.colorBaseText : R.color.colorLightText));
        // ветка
        TetroidNode node = record.getNode();
        if (isShowNodeName && nonCryptedOrDecrypted && node != null) {
            viewHolder.nodeNameView.setVisibility(View.VISIBLE);
            viewHolder.nodeNameView.setText(node.getName());
        } else {
            viewHolder.nodeNameView.setVisibility(View.GONE);
        }
        // дата создания
        if (nonCryptedOrDecrypted) {
            viewHolder.infoView.setText(record.getCreatedString(dateTimeFormat));
        } else {
            viewHolder.infoView.setText(null);
        }
        // прикрепленные файлы
        RelativeLayout.LayoutParams nameParams = (RelativeLayout.LayoutParams) viewHolder.nameView.getLayoutParams();
        if (record.getAttachedFilesCount() > 0 && nonCryptedOrDecrypted) {
            // если установлено в настройках, меняем фон
            if (App.IsHighlightAttach) {
                convertView.setBackgroundColor(App.HighlightAttachColor);
            }
            viewHolder.attachedView.setVisibility(View.VISIBLE);
            viewHolder.attachedView.setOnClickListener(v -> onRecordAttachmentClickListener.onClick(record));
            nameParams.setMargins(0,0, context.getResources().getDimensionPixelOffset(R.dimen.record_attached_image_width),0);
        }
        else {
            convertView.setBackgroundColor(Color.TRANSPARENT);
            viewHolder.attachedView.setVisibility(View.GONE);
            nameParams.setMargins(0,0,0,0);
        }

        return convertView;
    }

    public List<TetroidRecord> getDataSet() {
        return dataSet;
    }
}
