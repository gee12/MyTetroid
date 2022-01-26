package com.gee12.mytetroid.views.adapters;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.gee12.mytetroid.App;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.RecordFieldsSelector;
import com.gee12.mytetroid.interactors.CommonSettingsInteractor;
import com.gee12.mytetroid.interactors.RecordsInteractor;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.model.TetroidRecord;
import com.gee12.mytetroid.common.utils.Utils;

import java.util.Date;

public abstract class RecordsBaseListAdapter extends BaseAdapter {

    /**
     *
     */
    public static class RecordViewHolder {
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

    public interface OnRecordAttachmentClickListener {
        /**
         * Вызывается при клике на иконке прикрепленных файлов к записи
         * @param record Объект записи
         */
        void onClick(TetroidRecord record);
    }

    protected final LayoutInflater inflater;
    protected final RecordsInteractor recordsInteractor;
    protected final CommonSettingsInteractor commonSettingsInteractor;
    protected final OnRecordAttachmentClickListener onAttachmentClickListener;
    protected final Context context;
    protected boolean isShowNodeName;
    protected String dateTimeFormat;


    public RecordsBaseListAdapter(
            Context context,
            RecordsInteractor recordsInteractor,
            CommonSettingsInteractor commonSettingsInteractor,
            OnRecordAttachmentClickListener onAttachmentClickListener
    ) {
        super();
        this.context = context;
        this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.recordsInteractor = recordsInteractor;
        this.commonSettingsInteractor = commonSettingsInteractor;
        this.onAttachmentClickListener = onAttachmentClickListener;
        this.dateTimeFormat = commonSettingsInteractor.checkDateFormatString(context);
    }

    /**
     *
     * @param position
     * @param viewHolder
     * @param convertView
     * @param record
     * @return
     */
    public void prepareView(int position, RecordViewHolder viewHolder, View convertView, TetroidRecord record) {
        if (viewHolder == null || record == null) {
            return;
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
        String author = record.getAuthor();
        if (nonCryptedOrDecrypted && fieldsSelector.checkIsAuthor() && !TextUtils.isEmpty(author)) {
            viewHolder.authorView.setVisibility(View.VISIBLE);
            viewHolder.authorView.setText(author);
        } else {
            viewHolder.authorView.setVisibility(View.GONE);
        }
        // метки
        String tags = record.getTagsString();
        if (nonCryptedOrDecrypted && fieldsSelector.checkIsTags() && !TextUtils.isEmpty(tags)) {
            viewHolder.tagsView.setVisibility(View.VISIBLE);
            viewHolder.tagsView.setText(tags);
        } else {
            viewHolder.tagsView.setVisibility(View.GONE);
        }
        // дата создания
        if (nonCryptedOrDecrypted && fieldsSelector.checkIsCreatedDate()) {
            viewHolder.createdView.setVisibility(View.VISIBLE);
            viewHolder.createdView.setText(record.getCreatedString(dateTimeFormat));
        } else {
            viewHolder.createdView.setVisibility(View.GONE);
        }
        // дата изменения
        if (App.INSTANCE.isFullVersion() && nonCryptedOrDecrypted && fieldsSelector.checkIsEditedDate()) {
            viewHolder.editedView.setVisibility(View.VISIBLE);
            Date edited = recordsInteractor.getEditedDate(context, record);
            viewHolder.editedView.setText((edited != null) ? Utils.dateToString(edited, dateTimeFormat) : "-");
        } else {
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
            viewHolder.attachedView.setOnClickListener(v -> onAttachmentClickListener.onClick(record));
            nameParams.setMargins(0,0, context.getResources().getDimensionPixelOffset(R.dimen.record_attached_image_width),0);
        }
        else {
            convertView.setBackgroundColor(Color.TRANSPARENT);
            viewHolder.attachedView.setVisibility(View.GONE);
            nameParams.setMargins(0,0,0,0);
        }
    }

}
