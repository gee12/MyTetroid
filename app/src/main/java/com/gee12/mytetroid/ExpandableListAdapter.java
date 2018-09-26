package com.gee12.mytetroid;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.gee12.mytetroid.data.TetroidNode;

import java.util.HashMap;
import java.util.List;

public class ExpandableListAdapter extends BaseExpandableListAdapter {
    private Context context;
    private List<TetroidNode> nodesCollection;
    private OnNodeNameClickListener onNodeNameClickListener;

    // child data in format of header title, child title
    private HashMap<TetroidNode, List<TetroidNode>> mListDataChild;
    ExpandableListView expListView;
    public ExpandableListAdapter(Context context, List<TetroidNode> nodesCollection, ExpandableListView expListView, OnNodeNameClickListener onNodeNameClickListener)
    {
        this.context = context;
        this.nodesCollection = nodesCollection;
//        this.mListDataChild = listChildData;
        this.expListView = expListView;
        this.onNodeNameClickListener = onNodeNameClickListener;
    }

    @Override
    public int getGroupCount() {
        int i= nodesCollection.size();
        Log.d("GROUPCOUNT",String.valueOf(i));
        return this.nodesCollection.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        int childCount=0;
        if(groupPosition!=2)
        {
            childCount=this.mListDataChild.get(this.nodesCollection.get(groupPosition))
                    .size();
        }
        return childCount;
    }

    @Override
    public Object getGroup(int groupPosition) {

        return this.nodesCollection.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        Log.d("CHILD",mListDataChild.get(this.nodesCollection.get(groupPosition))
                .get(childPosition).toString());
        return this.mListDataChild.get(this.nodesCollection.get(groupPosition))
                .get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(final int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        TetroidNode headerTitle = (TetroidNode) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater)this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_header, null);
        }
//        ToggleButton toggle = (ToggleButton) convertView.findViewById(R.id.toggle);
        TextView tbName = (TextView) convertView.findViewById(R.id.node_name);
        ImageView ivIcon =    (ImageView)convertView.findViewById(R.id.node_icon);
//        final DrawerLayout drawerLayout = (DrawerLayout) parent.findViewById(R.id.drawer_layout);

//        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                if(isChecked) {
//                    expListView.expandGroup(groupPosition, true);
//                } else {
//                    expListView.collapseGroup(groupPosition);
//                }
//            }
//        });
        tbName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNodeNameClickListener.onClick(groupPosition);
            }
        });
//        headerIcon.setImageDrawable(headerTitle.getIconImg());
        tbName.setTypeface(null, Typeface.BOLD);
        tbName.setText(headerTitle.getName());
        return convertView;
//        TetroidNode holder;
//
//        LayoutInflater infalInflater = (LayoutInflater)this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        if (convertView == null) {
//            holder = (TetroidNode) getGroup(groupPosition);
//            convertView = infalInflater.inflate(R.layout.list_header, null);
//        } else {
//            holder = (TetroidNode) convertView.getTag();
//        }
////        holder.Header = view.FindViewById<TextView>(Resource.Id.DataHeader);
////        holder.Indicator = view.FindViewById<ImageView>(Resource.Id.indicator);
//        final ToggleButton toggle = (ToggleButton) convertView.findViewById(R.id.toggle);
//        TextView lblListHeader = (TextView) convertView.findViewById(R.id.submenu);
////        ImageView headerIcon=    (ImageView)convertView.findViewById(R.id.iconimage);
//        final DrawerLayout drawerLayout = (DrawerLayout) parent.findViewById(R.id.drawer_layout);
//
//        convertView.setTag(holder);
//
////        holder.Header.Text = ((char)(65 + groupPosition)).ToString();
//        lblListHeader.setText(holder.getName());
//
//        toggle.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
////                if (toggle.isChecked()) {
////                    expListView.expandGroup(groupPosition, true);
////                } else {
////                    expListView.collapseGroup(groupPosition);
////                }
//
////                Toast.makeText(context,
////                        nodesCollection.get(groupPosition).getName(),
////                        Toast.LENGTH_SHORT).show();
//        }});
//        lblListHeader.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Toast.makeText(context,
//                        nodesCollection.get(groupPosition).getName(),
//                        Toast.LENGTH_SHORT).show();
////                drawerLayout.closeDrawers();
//            }
//        });
//        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition,  boolean isLastChild, View convertView, ViewGroup parent) {
        final TetroidNode child = (TetroidNode) getChild(groupPosition, childPosition);

        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_submenu, null);
        }

        TextView txtListChild = (TextView) convertView
                .findViewById(R.id.node_name);

        txtListChild.setText(child.getName());

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public interface OnNodeNameClickListener {
        /**
         * Вызывается при клике на имени ветки
         *
         * @param groupPosition Индекс ветки.
         */
        void onClick(int groupPosition);
    }
}
