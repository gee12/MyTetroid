package com.gee12.mytetroid.adapters;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
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
import com.gee12.mytetroid.RecordFieldsSelector;
import com.gee12.mytetroid.data.RecordsManager;
import com.gee12.mytetroid.fragments.MainPageFragment;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.utils.Utils;

import java.util.ArrayList;
import java.util.Date;
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
        TextView authorView;
        TextView tagsView;
        TextView createdView;
        TextView editedView;
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
            viewHolder.authorView = convertView.findViewById(R.id.record_view_author);
            viewHolder.tagsView = convertView.findViewById(R.id.record_view_tags);
            viewHolder.createdView = convertView.findViewById(R.id.record_view_created);
            viewHolder.editedView = convertView.findViewById(R.id.record_view_edited);
            viewHolder.attachedView = convertView.findViewById(R.id.record_view_attached);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (RecordViewHolder) convertView.getTag();
        }

        final TetroidRecord record = dataSet.get(position);
        if (record == null) {
            return convertView;
        }
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
        RecordFieldsSelector fieldsSelector = App.RecordFieldsInList;
        // автор
//        if (nonCryptedOrDecrypted && SettingsManager.isShowAuthorInRecordsList(context)) {
        String author = record.getAuthor();
        if (nonCryptedOrDecrypted && fieldsSelector.checkIsAuthor() && !TextUtils.isEmpty(author)) {
            viewHolder.authorView.setVisibility(View.VISIBLE);
            viewHolder.authorView.setText(author);
        } else {
//            viewHolder.authorView.setText(null);
            viewHolder.authorView.setVisibility(View.GONE);
        }
        // метки
        String tags = record.getTagsString();
        if (nonCryptedOrDecrypted && fieldsSelector.checkIsTags() && !TextUtils.isEmpty(tags)) {
            viewHolder.tagsView.setVisibility(View.VISIBLE);
            viewHolder.tagsView.setText(tags);
        } else {
//            viewHolder.tagsView.setText(null);
            viewHolder.tagsView.setVisibility(View.GONE);
        }
        // дата создания
        if (nonCryptedOrDecrypted && fieldsSelector.checkIsCreatedDate()) {
            viewHolder.createdView.setVisibility(View.VISIBLE);
            viewHolder.createdView.setText(record.getCreatedString(dateTimeFormat));
        } else {
//            viewHolder.createdView.setText(null);
            viewHolder.createdView.setVisibility(View.GONE);
        }
        // дата изменения
        if (App.isFullVersion() && nonCryptedOrDecrypted && fieldsSelector.checkIsEditedDate()) {
            viewHolder.editedView.setVisibility(View.VISIBLE);
            Date edited = RecordsManager.getEditedDate(context, record);
            viewHolder.editedView.setText((edited != null) ? Utils.dateToString(edited, dateTimeFormat) : "-");
        } else {
//            viewHolder.editedView.setText(null);
            viewHolder.editedView.setVisibility(View.GONE);
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
