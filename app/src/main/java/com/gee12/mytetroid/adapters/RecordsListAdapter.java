package com.gee12.mytetroid.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.gee12.mytetroid.data.SettingsManager;
import com.gee12.mytetroid.fragments.MainPageFragment;
import com.gee12.mytetroid.model.TetroidRecord;

import java.util.ArrayList;
import java.util.List;

public class RecordsListAdapter extends RecordsBaseListAdapter {

    private List<TetroidRecord> dataSet;

    public RecordsListAdapter(Context context, OnRecordAttachmentClickListener onAttachmentClickListener) {
        super(context, onAttachmentClickListener);
        this.dataSet = new ArrayList<>();
    }

    public void setDataItems(List<TetroidRecord> dataSet, int viewId) {
        this.dataSet = dataSet;
        this.isShowNodeName = (viewId == MainPageFragment.MAIN_VIEW_TAG_RECORDS
                || viewId == MainPageFragment.MAIN_VIEW_FAVORITES);
        this.dateTimeFormat = SettingsManager.checkDateFormatString(context);
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
        return getView(position, convertView, dataSet.get(position));
    }

    public List<TetroidRecord> getDataSet() {
        return dataSet;
    }
}
