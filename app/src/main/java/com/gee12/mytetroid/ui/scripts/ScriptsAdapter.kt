package com.gee12.mytetroid.ui.scripts

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.orFalse
import com.gee12.mytetroid.domain.IFailureHandler
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.model.ITetroidObject
import com.gee12.mytetroid.model.TetroidScript
import com.gee12.mytetroid.model.TetroidScriptToObject
import com.gee12.mytetroid.model.enums.TetroidObjectType

class ScriptsAdapter(
    context: Context,
    private val resourcesProvider: IResourcesProvider,
    private val failureHandler: IFailureHandler,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    enum class ItemViewType(val id: Int) {
        Script(0),
        ScriptToObject(1),
        Footer(2)
    }

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    val data = mutableListOf<Any>()
    var currentObject: ITetroidObject? = null

    var onItemClickListener: ((TetroidScript, View) -> Unit)? = null
    var onItemLongClickListener: ((TetroidScript, View) -> Boolean)? = null
    var onItemSwitchClickListener: ((TetroidScript, Boolean, View) -> Unit)? = null
    var onItemMenuClickListener: ((TetroidScript, View) -> Unit)? = null
    var onObjectItemSwitchClickListener: ((TetroidScriptToObject, Boolean, View) -> Unit)? = null

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(list: List<TetroidScript>, obj: ITetroidObject?) {
        currentObject = obj
        data.clear()
        data.addAll(buildList(list, obj))
        notifyDataSetChanged()
    }

    private fun buildList(list: List<TetroidScript>, obj: ITetroidObject?): List<Any> {
        return buildList {
            list.forEach { script ->
                add(script)
                script.getObjects(obj)?.also { objects ->
                    addAll(objects)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ItemViewType.Script.id -> {
                val view = inflater.inflate(R.layout.list_item_script, parent, false)
                ScriptViewHolder(view)
            }
            ItemViewType.ScriptToObject.id -> {
                val view = inflater.inflate(R.layout.list_item_script_to_object, parent, false)
                ScriptToObjectViewHolder(view)
            }
            ItemViewType.Footer.id -> {
                val view = inflater.inflate(R.layout.recycler_view_empty_footer, parent, false)
                FooterViewHolder(view)
            }
            else -> throw Exception("Unknown viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ScriptViewHolder -> {
                (getItem(position) as? TetroidScript)?.also {
                    holder.bind(it)
                }
            }
            is ScriptToObjectViewHolder -> {
                (getItem(position) as? TetroidScriptToObject)?.also {
                    holder.bind(it)
                }
            }
            is FooterViewHolder -> Unit
        }
    }

    override fun getItemCount(): Int {
        return data.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position == data.size -> {
                ItemViewType.Footer.id
            }
            getItem(position) is TetroidScriptToObject -> {
                ItemViewType.ScriptToObject.id
            }
            else -> {
                ItemViewType.Script.id
            }
        }
    }

    fun getItem(position: Int): Any? {
        return data.getOrNull(position)
    }

    fun getItemPositionById(scriptId: Int): Int {
        return data.indexOfFirst { it is TetroidScript && scriptId == it.id }
    }

    inner class ScriptViewHolder internal constructor(
        private val view: View,
    ) : RecyclerView.ViewHolder(view) {

        private val tvFileName: TextView = itemView.findViewById(R.id.text_view_file_name)
        private val tvDescription: TextView = itemView.findViewById(R.id.text_view_description)
        private val switch: SwitchCompat = itemView.findViewById(R.id.switch_script_is_active)
        private val ivError: ImageView = itemView.findViewById(R.id.image_view_error)
        private val tvError: TextView = itemView.findViewById(R.id.text_view_error)
        private val ivMenu: ImageView = itemView.findViewById(R.id.image_view_menu)

        fun bind(script: TetroidScript) {
            view.setOnClickListener {
                onItemClickListener?.invoke(script, view)
            }
            view.setOnLongClickListener {
                onItemLongClickListener?.invoke(script, view) ?: false
            }
            ivMenu.setOnClickListener {
                onItemMenuClickListener?.invoke(script, ivMenu)
            }

            switch.isVisible = script.isCanSwitchActivity(currentObject)
            switch.isChecked = script.isActiveByErrors()
            switch.setOnCheckedChangeListener { button, isChecked ->
                if (button.isPressed) {
                    onItemSwitchClickListener?.invoke(script, isChecked, switch)
                }
            }

            tvFileName.text = script.fileName
            tvDescription.isVisible = !script.description.isNullOrBlank()
            tvDescription.text = script.description

            val isError = !script.errors.isNullOrEmpty()
            ivError.isVisible = isError
            tvError.isVisible = isError
            tvError.text = script.errors?.joinToString(separator = "\n") {
                failureHandler.getFailureMessage(it).title
            }
        }
    }

    inner class ScriptToObjectViewHolder internal constructor(
        private val view: View,
    ) : RecyclerView.ViewHolder(view) {

        private val ivIcon: ImageView = itemView.findViewById(R.id.image_view_icon)
        private val tvObjectName: TextView = itemView.findViewById(R.id.text_view_object_name)
        private val switch: SwitchCompat = itemView.findViewById(R.id.switch_script_is_active)

        fun bind(scriptToObject: TetroidScriptToObject) {
            val isForAllStorage = currentObject == null
            view.isEnabled = isForAllStorage

            switch.isVisible = scriptToObject.script?.isActiveByErrors().orFalse()
            switch.isEnabled = isForAllStorage
            switch.isChecked = true
            switch.setOnCheckedChangeListener { button, isChecked ->
                if (button.isPressed) {
                    onObjectItemSwitchClickListener?.invoke(scriptToObject, isChecked, switch)
                }
            }

            tvObjectName.text = scriptToObject.stringTitle()
            scriptToObject.objectType.iconResId()?.also {
                ivIcon.setImageResource(it)
            }
        }

        private fun TetroidObjectType?.iconResId(): Int? {
            return when (this) {
                TetroidObjectType.RECORD -> R.drawable.ic_record
                TetroidObjectType.NODE -> R.drawable.ic_tree
                TetroidObjectType.TAG -> R.drawable.ic_tag_2
                TetroidObjectType.NONE, null -> R.drawable.ic_storage
                else -> null
            }
        }

        private fun TetroidScriptToObject.stringTitle(): String? {
            return when (objectType) {
                TetroidObjectType.RECORD -> {
                    resourcesProvider.getString(R.string.title_script_for_record_masked, objectName.orEmpty())
                }
                TetroidObjectType.NODE -> {
                    resourcesProvider.getString(R.string.title_script_for_node_masked, objectName.orEmpty())
                }
                TetroidObjectType.TAG -> {
                    resourcesProvider.getString(R.string.title_script_for_tag_masked, objectName.orEmpty())
                }
                TetroidObjectType.NONE, null -> {
                    resourcesProvider.getString(R.string.title_script_for_all_storage)
                }
                else -> null
            }
        }

    }

    inner class FooterViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view)

}