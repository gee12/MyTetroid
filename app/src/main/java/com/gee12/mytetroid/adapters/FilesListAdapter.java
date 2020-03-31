package com.gee12.mytetroid.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.model.TetroidFile;
import com.gee12.mytetroid.model.TetroidRecord;

import java.util.ArrayList;
import java.util.List;

public class FilesListAdapter extends BaseAdapter {

    /**
     *
     */
    private class FileViewHolder {
        TextView lineNumView;
        TextView nameView;
        TextView sizeView;
    }

    private LayoutInflater inflater;
    private List<TetroidFile> dataSet;
    private TetroidRecord record;
    private Context context;

    public FilesListAdapter(Context context) {
        this.context = context;
        this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.dataSet = new ArrayList<>();
    }

    public void reset(List<TetroidFile> data, TetroidRecord record) {
        this.dataSet = data;
        this.record = record;
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
        FileViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new FileViewHolder();
            convertView = inflater.inflate(R.layout.list_item_file, null);
            viewHolder.lineNumView = convertView.findViewById(R.id.file_view_line_num);
            viewHolder.nameView = convertView.findViewById(R.id.file_view_name);
            viewHolder.sizeView = convertView.findViewById(R.id.file_view_size);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (FileViewHolder) convertView.getTag();
        }

        final TetroidFile file = dataSet.get(position);
        // номер строки
        viewHolder.lineNumView.setText(String.valueOf(position + 1));
        // название файла
        viewHolder.nameView.setText(file.getName());
        // размер файла
        String fileSize = DataManager.getFileSize(context, record, file);
        if (fileSize != null) {
            viewHolder.sizeView.setText(fileSize);
        } else {
            ImageView icon = convertView.findViewById(R.id.file_view_icon);
            icon.setImageResource(R.drawable.ic_file_missing);
            viewHolder.sizeView.setText(R.string.file_is_missing);
        }

        return convertView;
    }
}
