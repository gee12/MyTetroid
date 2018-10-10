package com.gee12.mytetroid.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.TetroidFile;
import com.gee12.mytetroid.data.TetroidRecord;

import java.util.List;

public class FilesListAdapter extends BaseAdapter {

    /**
     *
     */
    private class FileViewHolder {
        TextView lineNumView;
        ImageView iconView;
        TextView nameView;
        TextView sizeView;
    }

    private static LayoutInflater inflater = null;
    private List<TetroidFile> dataSet;

    public FilesListAdapter(Context context) {
        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void reset(List<TetroidFile> dataSet) {
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
        FileViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new FileViewHolder();
            convertView = inflater.inflate(R.layout.list_item_file, null);
            viewHolder.lineNumView = (TextView) convertView.findViewById(R.id.file_view_line_num);
            viewHolder.iconView = (ImageView) convertView.findViewById(R.id.file_view_icon);
            viewHolder.nameView = (TextView) convertView.findViewById(R.id.file_view_name);
            viewHolder.sizeView = (TextView) convertView.findViewById(R.id.file_view_size);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (FileViewHolder) convertView.getTag();
        }

        final TetroidFile file = dataSet.get(position);
        // номер строки
        viewHolder.lineNumView.setText(String.valueOf(position));
        // иконка выводится сама по себе

        // название файла
        viewHolder.nameView.setText(file.getFileName());
        // размер файла
        viewHolder.sizeView.setText(file.getFileSize());

        return convertView;
    }
}
