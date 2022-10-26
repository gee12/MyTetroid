package com.gee12.mytetroid.views.activities

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Spinner
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.model.TetroidIcon
import com.gee12.mytetroid.model.TetroidNode
import com.gee12.mytetroid.viewmodels.IconsViewModel
import com.gee12.mytetroid.views.adapters.IconsListAdapter
import java.io.File
import java.util.stream.IntStream
import org.koin.android.ext.android.inject

/**
 * Активность для выбора иконки ветки.
 */
class IconsActivity : AppCompatActivity() {

    private lateinit var spFolders: Spinner
    private var mFoldersAdapter: ArrayAdapter<String>? = null
    private lateinit var iconsAdapter: IconsListAdapter
    private lateinit var btnSubmit: View
    private var mNodeId: String? = null
    private var mSelectedIcon: TetroidIcon? = null
    private val viewModel: IconsViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_icons)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        viewModel.initialize()

        // список иконок
        val mListView = findViewById<ListView>(R.id.list_view_icons)
        iconsAdapter = IconsListAdapter(this, viewModel.iconsInteractor)
        mListView.adapter = iconsAdapter
        mListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, view: View?, i: Int, _ ->
                val icon = iconsAdapter.getItem(i) as? TetroidIcon
                if (icon != null) {
                    mSelectedIcon = icon
                    iconsAdapter.selectedIcon = icon
                    iconsAdapter.setSelectedView(view)
                    btnSubmit.isEnabled = true
                }
            }

        // список каталогов
        spFolders = findViewById<Spinner>(R.id.spinner_folders)
        val folders = viewModel.getIconsFolders()
        if (folders != null) {
            if (folders.isEmpty()) {
                viewModel.logWarning(R.string.log_icons_dirs_absent)
                return
            }
            mFoldersAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                folders
            )
            mFoldersAdapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spFolders.setAdapter(mFoldersAdapter)
            spFolders.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
                @RequiresApi(api = Build.VERSION_CODES.N)
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val folder = mFoldersAdapter?.getItem(position)
                    if (folder != null) {
                        val icons = viewModel.getIconsFromFolder(folder)
                        if (icons != null) {
                            // пришлось заново пересоздавать адаптер, т.к. при простой установке dataSet
                            // с помощью reset(icons)
                            // не сбрасывалось выделение от предыдущего списка icons (из другого раздела)
                            iconsAdapter = IconsListAdapter(this@IconsActivity, viewModel.iconsInteractor)
                            mListView.adapter = iconsAdapter
                            if (mSelectedIcon != null && folder == mSelectedIcon!!.folder) {
                                val selectedIconName = mSelectedIcon!!.name
                                val iconPos = IntStream.range(0, icons.size)
                                    .filter { i: Int -> selectedIconName == icons[i].name }
                                    .findFirst()
                                if (iconPos.isPresent) {
                                    // выделяем существующую иконку
                                    val pos = iconPos.asInt
                                    val icon = icons[pos]
                                    mSelectedIcon = icon
                                    iconsAdapter.selectedIcon = icon
                                    btnSubmit.isEnabled = true
                                }
                                iconsAdapter.setData(icons)
                                if (iconPos.isPresent) {
                                    val pos = iconPos.asInt
                                    mListView.setSelection(pos)
                                }
                            } else {
//                                mIconsAdapter.setSelectedIcon(null);
//                                mIconsAdapter.setSelectedView(null);
                                iconsAdapter.setData(icons)
//                                mListView.setItemChecked(-1, true);
//                                mListView.setSelection(-1);
//                                mListView.setSelectionAfterHeaderView();
                            }
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            })
        } else {
            viewModel.logWarning(getString(R.string.log_icons_dir_absent_mask, Constants.ICONS_DIR_NAME))
        }
        findViewById<View>(R.id.button_clear_icon).setOnClickListener { _ ->
            setResult(
                null
            )
        }
        btnSubmit = findViewById(R.id.button_submit)
        btnSubmit.setOnClickListener {
            setResult(mSelectedIcon)
        }

        // получаем данные
        val extras = intent.extras
        if (extras != null) {
            mNodeId = extras.getString(Constants.EXTRA_NODE_ID)
            val iconPath = extras.getString(Constants.EXTRA_NODE_ICON_PATH)
            selectCurrentIcon(iconPath!!)
        }
        viewModel.logDebug(getString(R.string.log_activity_opened_mask, this.javaClass.simpleName))
    }

    private fun selectCurrentIcon(iconPath: String) {
        if (!TextUtils.isEmpty(iconPath)) {
            val pathParts = iconPath.split(File.separator).toTypedArray()
            if (pathParts.size >= 2) {
                val name = pathParts[pathParts.size - 1]
                val folder = pathParts[pathParts.size - 2]
                mSelectedIcon = TetroidIcon(folder, name)
                val pos = mFoldersAdapter?.getPosition(folder) ?: -1
                if (pos >= 0) {
                    spFolders.setSelection(pos)
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setResult(icon: TetroidIcon?) {
        val intent = Intent()
        intent.putExtra(Constants.EXTRA_NODE_ID, mNodeId)
        intent.putExtra(Constants.EXTRA_NODE_ICON_PATH, icon?.path)
        intent.putExtra(Constants.EXTRA_IS_DROP, icon == null)
        setResult(RESULT_OK, intent)
        finish()
    }

    companion object {

        fun startIconsActivity(activity: Activity, node: TetroidNode?, requestCode: Int) {
            if (node == null) {
                return
            }
            val intent = Intent(activity, IconsActivity::class.java)
            intent.putExtra(Constants.EXTRA_NODE_ID, node.id)
            intent.putExtra(Constants.EXTRA_NODE_ICON_PATH, node.iconName)
            activity.startActivityForResult(intent, requestCode)
        }

    }

}