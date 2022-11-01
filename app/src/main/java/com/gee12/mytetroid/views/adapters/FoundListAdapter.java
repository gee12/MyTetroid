package com.gee12.mytetroid.views.adapters;

import android.content.Context;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.helpers.CommonSettingsProvider;
import com.gee12.mytetroid.interactors.RecordsInteractor;
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
        TextView foundInView;
        TextView typeView;
    }

    private final List<ITetroidObject> dataSet;
    private HashMap<ITetroidObject, FoundType> hashMap;

    public FoundListAdapter(
            Context context,
            RecordsInteractor recordsInteractor,
            CommonSettingsProvider settingsProvider,
            OnRecordAttachmentClickListener onAttachmentClickListener
            /*, List<ITetroidObject> dataSet*/
    ) {
        super(context, recordsInteractor, settingsProvider, onAttachmentClickListener);
        this.dataSet = new ArrayList<>();
        this.isShowNodeName = true;
    }

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
        if (convertView == null) {
            viewHolder = new FoundViewHolder();
            convertView = inflater.inflate(R.layout.list_item_found, null);
            viewHolder.lineNumView = convertView.findViewById(R.id.record_view_line_num);
            viewHolder.iconView = convertView.findViewById(R.id.record_view_icon);
            viewHolder.nameView = convertView.findViewById(R.id.record_view_name);
            viewHolder.nodeNameView = convertView.findViewById(R.id.record_view_node);
            viewHolder.authorView = convertView.findViewById(R.id.record_view_author);
            viewHolder.tagsView = convertView.findViewById(R.id.record_view_tags);
            viewHolder.createdView = convertView.findViewById(R.id.record_view_created);
            viewHolder.editedView = convertView.findViewById(R.id.record_view_edited);
            viewHolder.attachedView = convertView.findViewById(R.id.record_view_attached);
            viewHolder.foundInView = convertView.findViewById(R.id.found_view_founded_in);
            viewHolder.typeView = convertView.findViewById(R.id.found_view_type);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (FoundViewHolder) convertView.getTag();
        }

        final ITetroidObject foundObject = dataSet.get(position);
        if (foundObject instanceof TetroidRecord) {
            // если найденный объект - запись, то выводим его в списке как обычную запись
            prepareView(position, viewHolder, convertView, (TetroidRecord) foundObject);
        } else {
            // номер строки
            viewHolder.lineNumView.setText(String.valueOf(position + 1));
            // название найденного объекта
            viewHolder.nameView.setText(foundObject.getName());
        }
        // где найдено
        String foundInString = hashMap.get(foundObject).getFoundTypeString(context);
        viewHolder.foundInView.setText(context.getString(R.string.title_founded_in_mask, foundInString));
        // тип найденного объекта
        String typeName = getTetroidObjectTypeString(context, foundObject);
        viewHolder.typeView.setText(typeName.toUpperCase());

        return convertView;
    }

    public static String getTetroidObjectTypeString(Context context, ITetroidObject obj) {
        int resId;
        int type = obj.getType();
        switch (type) {
            case FoundType.TYPE_RECORD:
                resId = R.string.title_record;
                break;
            case FoundType.TYPE_NODE:
                resId = R.string.title_node;
                break;
            case FoundType.TYPE_TAG:
                resId = R.string.title_tag;
                break;
            default:
                String[] strings = context.getResources().getStringArray(R.array.found_types);
                return (strings != null && strings.length > type) ? strings[type] : "";
        }
        return context.getString(resId);
    }
}
