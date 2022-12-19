package com.gee12.mytetroid.ui.main.found

import android.content.Context
import android.util.Pair
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.gee12.mytetroid.R
import com.gee12.mytetroid.providers.IResourcesProvider
import com.gee12.mytetroid.model.FoundType
import com.gee12.mytetroid.model.ITetroidObject
import com.gee12.mytetroid.model.TetroidRecord
import com.gee12.mytetroid.ui.main.RecordsBaseListAdapter
import java.util.*

class FoundListAdapter(
    context: Context,
    resourcesProvider: IResourcesProvider,
    dateTimeFormat: String,
    getEditedDateCallback: (record: TetroidRecord) -> Date?,
    onClick: (record: TetroidRecord) -> Unit,
    /*, List<ITetroidObject> dataSet*/
) : RecordsBaseListAdapter(
    context,
    resourcesProvider,
    dateTimeFormat,
    getEditedDateCallback,
    onClick,
) {

    private class FoundViewHolder : RecordViewHolder() {
        lateinit var foundInView: TextView
        lateinit var typeView: TextView
    }

    private val dataSet: MutableList<ITetroidObject>
    private var hashMap: Map<ITetroidObject, FoundType>? = null

    init {
        dataSet = ArrayList()
        isShowNodeName = true
    }

    fun setDataItems(hashMap: Map<ITetroidObject, FoundType>) {
        this.hashMap = hashMap
        dataSet.addAll(hashMap.keys)
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return dataSet.size
    }

    override fun getItem(position: Int): Any {
        return dataSet[position]
    }

    fun getFoundObject(position: Int): Pair<ITetroidObject, FoundType?> {
        val obj = dataSet[position]
        val type = hashMap!![obj]
        return Pair(obj, type)
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].hashCode().toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val viewHolder: FoundViewHolder
        if (view == null) {
            viewHolder = FoundViewHolder()
            view = inflater.inflate(R.layout.list_item_found, null)
            viewHolder.lineNumView = view.findViewById(R.id.record_view_line_num)
            viewHolder.iconView = view.findViewById(R.id.record_view_icon)
            viewHolder.nameView = view.findViewById(R.id.record_view_name)
            viewHolder.nodeNameView = view.findViewById(R.id.record_view_node)
            viewHolder.authorView = view.findViewById(R.id.record_view_author)
            viewHolder.tagsView = view.findViewById(R.id.record_view_tags)
            viewHolder.createdView = view.findViewById(R.id.record_view_created)
            viewHolder.editedView = view.findViewById(R.id.record_view_edited)
            viewHolder.attachedView = view.findViewById(R.id.record_view_attached)
            viewHolder.foundInView = view.findViewById(R.id.found_view_founded_in)
            viewHolder.typeView = view.findViewById(R.id.found_view_type)
            view.tag = viewHolder
        } else {
            viewHolder = view.tag as FoundViewHolder
        }
        val foundObject = dataSet[position]
        if (foundObject is TetroidRecord) {
            // если найденный объект - запись, то выводим его в списке как обычную запись
            prepareView(position, viewHolder, view!!, foundObject)
        } else {
            // номер строки
            viewHolder.lineNumView.text = (position + 1).toString()
            // название найденного объекта
            viewHolder.nameView.text = foundObject.name
        }
        // где найдено
        val foundInString = hashMap?.get(foundObject)?.getFoundTypeString(resourcesProvider).orEmpty()
        viewHolder.foundInView.text = resourcesProvider.getString(R.string.title_founded_in_mask, foundInString)
        // тип найденного объекта
        val typeName = getTetroidObjectTypeString(resourcesProvider, foundObject)
        viewHolder.typeView.text = typeName.uppercase(Locale.getDefault())

        return view!!
    }

    companion object {

        fun getTetroidObjectTypeString(resourcesProvider: IResourcesProvider, obj: ITetroidObject): String {
            return when (val type = obj.type) {
                FoundType.TYPE_RECORD -> resourcesProvider.getString(R.string.title_record)
                FoundType.TYPE_NODE -> resourcesProvider.getString(R.string.title_node)
                FoundType.TYPE_TAG -> resourcesProvider.getString(R.string.title_tag)
                else -> {
                    val strings = resourcesProvider.getStringArray(R.array.found_types)
                    return if (strings != null && strings.size > type) strings[type] else ""
                }
            }
        }

    }
}