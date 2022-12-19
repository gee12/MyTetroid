package com.gee12.mytetroid.ui.node.icon

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.extensions.buildIntent
import com.gee12.mytetroid.model.TetroidIcon
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.ui.base.TetroidStorageActivity
import com.gee12.mytetroid.ui.base.BaseEvent
import java.util.stream.IntStream

/**
 * Активность для выбора иконки ветки.
 */
class IconsActivity : TetroidStorageActivity<IconsViewModel>() {

    private lateinit var spFolders: Spinner
    private lateinit var iconsListView: ListView
    private lateinit var foldersAdapter: ArrayAdapter<String>
    private lateinit var iconsAdapter: IconsListAdapter
    private lateinit var btnSubmit: Button
    private lateinit var btnClear: Button
    private var selectedIcon: TetroidIcon? = null


    override fun getLayoutResourceId() = R.layout.activity_icons

    override fun getViewModelClazz() = IconsViewModel::class.java


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        btnSubmit = findViewById(R.id.button_submit)
        btnSubmit.setOnClickListener {
            setResult(selectedIcon)
        }

        btnClear = findViewById(R.id.button_clear_icon)
        btnClear.setOnClickListener {
            setResult(null)
        }

        // список иконок
        iconsListView = findViewById(R.id.list_view_icons)
        initIconsList()
        iconsListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, view: View?, i: Int, _ ->
                (iconsAdapter.getItem(i) as? TetroidIcon)?.let { icon ->
                    onIconSelected(icon, view)
                }
            }

        // список каталогов
        spFolders = findViewById(R.id.spinner_folders)

        spFolders.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            @RequiresApi(api = Build.VERSION_CODES.N)
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                foldersAdapter.getItem(position)?.let { folder ->
                    onFolderSelected(folder)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // загрузка
        val nodeId = intent.extras?.getString(Constants.EXTRA_NODE_ID).orEmpty()
        val iconPath = intent.extras?.getString(Constants.EXTRA_NODE_ICON_PATH).orEmpty()
        viewModel.init(nodeId, iconPath)
    }

    override fun onBaseEvent(event: BaseEvent) {
        when (event) {
            is IconsEvent.IconsFolders -> {
                loadIconsFolders(event.folders)
            }
            is IconsEvent.CurrentIcon -> {
                selectIcon(event.icon)
            }
            is IconsEvent.IconsFromFolder -> {
                loadIconsFromFolder(event.folder, event.icons)
            }
            else -> super.onBaseEvent(event)
        }
    }

    private fun loadIconsFolders(folders: List<String>) {
        if (folders.isEmpty()) {
            viewModel.logWarning(R.string.log_icons_dirs_absent)
        } else {
            foldersAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                folders
            )
            foldersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spFolders.adapter = foldersAdapter
        }
    }

    private fun loadIconsFromFolder(folder: String, icons: List<TetroidIcon>?) {
        if (icons != null) {
            // пришлось заново пересоздавать адаптер, т.к. при простой установке dataSet
            // с помощью reset(icons)
            // не сбрасывалось выделение от предыдущего списка icons (из другого раздела)
            initIconsList()
            if (selectedIcon != null && folder == selectedIcon!!.folder) {
                val selectedIconName = selectedIcon!!.name
                // TODO: переписать с использованием Kotlin
                val iconPosition = IntStream.range(0, icons.size)
                    .filter { i: Int -> selectedIconName == icons[i].name }
                    .findFirst()
                if (iconPosition.isPresent) {
                    // выделяем существующую иконку
                    val pos = iconPosition.asInt
                    val icon = icons[pos]
                    selectedIcon = icon
                    iconsAdapter.selectedIcon = icon
                    btnSubmit.isEnabled = true
                }
                iconsAdapter.setData(icons)
                if (iconPosition.isPresent) {
                    val pos = iconPosition.asInt
                    iconsListView.setSelection(pos)
                }
            } else {
//              mIconsAdapter.setSelectedIcon(null);
//              mIconsAdapter.setSelectedView(null);
                iconsAdapter.setData(icons)
//              mListView.setItemChecked(-1, true);
//              mListView.setSelection(-1);
//              mListView.setSelectionAfterHeaderView();
            }
        }
    }

    private fun initIconsList() {
        iconsAdapter = IconsListAdapter(
            context = this,
            onLoadIconIfNeed = { icon ->
                viewModel.loadIconIfNeed(icon)
            }
        )
        iconsListView.adapter = iconsAdapter
    }

    private fun onIconSelected(icon: TetroidIcon, view: View?) {
        selectedIcon = icon
        iconsAdapter.selectedIcon = icon
        iconsAdapter.setSelectedView(view)
        btnSubmit.isEnabled = true
    }

    private fun onFolderSelected(folder: String) {
        viewModel.getIconsFromFolder(folder)
    }

    private fun selectIcon(icon: TetroidIcon) {
        selectedIcon = icon
        val folderPosition = foldersAdapter.getPosition(icon.folder)
        if (folderPosition >= 0) {
            spFolders.setSelection(folderPosition)
        }
    }

    private fun setResult(icon: TetroidIcon?) {
        val intent = buildIntent {
            putExtra(Constants.EXTRA_NODE_ID, viewModel.nodeId)
            putExtra(Constants.EXTRA_NODE_ICON_PATH, icon?.path)
            putExtra(Constants.EXTRA_IS_DROP, icon == null)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    companion object {

        fun start(activity: Activity, node: TetroidNode, requestCode: Int) {
            val intent = Intent(activity, IconsActivity::class.java)
            intent.putExtra(Constants.EXTRA_NODE_ID, node.id)
            intent.putExtra(Constants.EXTRA_NODE_ICON_PATH, node.iconName)
            activity.startActivityForResult(intent, requestCode)
        }

    }

}