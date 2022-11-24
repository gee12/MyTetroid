package com.gee12.mytetroid.views.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.common.Constants;
import com.gee12.mytetroid.helpers.CommonSettingsProvider;
import com.gee12.mytetroid.interactors.RecordsInteractor;
import com.gee12.mytetroid.model.TetroidRecord;

import java.util.ArrayList;
import java.util.List;

public class RecordsListAdapter extends RecordsBaseListAdapter {

    private List<TetroidRecord> dataSet;

    public RecordsListAdapter(
            Context context,
            RecordsInteractor recordsInteractor,
            CommonSettingsProvider settingsProvider,
            OnRecordAttachmentClickListener onAttachmentClickListener
    ) {
        super(context, recordsInteractor, settingsProvider, onAttachmentClickListener);
        this.dataSet = new ArrayList<>();
    }

    public void reset() {
        this.dataSet = new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setDataItems(List<TetroidRecord> dataSet, int viewId) {
        this.dataSet = dataSet;
        this.isShowNodeName = (viewId == Constants.MAIN_VIEW_TAG_RECORDS
                || viewId == Constants.MAIN_VIEW_FAVORITES);
        this.dateTimeFormat = settingsProvider.checkDateFormatString();
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
        prepareView(position, viewHolder, convertView, dataSet.get(position));
        return convertView;
    }

    public List<TetroidRecord> getDataSet() {
        return dataSet;
    }
}
