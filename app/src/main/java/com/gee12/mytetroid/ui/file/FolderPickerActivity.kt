package com.gee12.mytetroid.ui.file

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import com.gee12.htmlwysiwygeditor.dialog.AskDialogBuilder
import com.gee12.htmlwysiwygeditor.ext.showKeyboard
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Failure
import com.gee12.mytetroid.common.extensions.buildIntent
import com.gee12.mytetroid.domain.FailureHandler
import com.gee12.mytetroid.ui.base.BaseEvent
import com.gee12.mytetroid.ui.base.TetroidActivity
import org.koin.android.ext.android.inject
import java.io.File

class FolderPickerActivity : TetroidActivity<FolderPickerViewModel>() {

    companion object {
        const val EXTRA_PATH = "path"
        const val EXTRA_TITLE = "title"
        const val EXTRA_DESCRIPTION = "desc"
        const val EXTRA_INITIAL_PATH = "initial_path"
        const val EXTRA_IS_PICK_FILES = "is_pick_files"
        const val EXTRA_IS_EMPTY_FOLDER_ONLY = "is_empty_folder_only"
        const val EXTRA_IS_CHECK_WRITE_PERMISSION = "ischeck_write_permission"

        fun start(
            activity: Activity,
            requestCode: Int,
            initialPath: String?,
            description: String?,
            isNeedEmptyFolder: Boolean,
            isPickFile: Boolean,
            isNeedCheckFolderWritePermission: Boolean,
        ) {
            val intent = buildIntent {
                setClass(activity, FolderPickerActivity::class.java)

                if (!initialPath.isNullOrEmpty()) {
                    putExtra(EXTRA_INITIAL_PATH, initialPath)
                }
                if (!description.isNullOrEmpty()) {
                    putExtra(EXTRA_DESCRIPTION, description)
                }
                putExtra(EXTRA_IS_EMPTY_FOLDER_ONLY, isNeedEmptyFolder)
                putExtra(EXTRA_IS_PICK_FILES, isPickFile)
                putExtra(EXTRA_IS_CHECK_WRITE_PERMISSION, isNeedCheckFolderWritePermission)
            }
            activity.startActivityForResult(intent, requestCode)
        }
    }

    private val failureHandler: FailureHandler by inject()

    private lateinit var tbCurrentPath: TextView
    private lateinit var tbError: TextView
    private lateinit var bApply: Button
    private lateinit var listView: ListView
    private lateinit var listAdapter: FolderAdapter

    override fun getLayoutResourceId() = R.layout.activity_folder_picker

    override fun getViewModelClazz() = FolderPickerViewModel::class.java

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!viewModel.isExternalStorageReadable()) {
            showMessage(R.string.error_no_access_to_storage)
            finish()
        }

        // path
        tbCurrentPath = findViewById(R.id.text_view_path_label)
        tbError = findViewById(R.id.text_view_error)

        // buttons
        bApply = findViewById<Button>(R.id.button_apply).also {
            it.setOnClickListener {
                viewModel.onApply()
            }
        }
        findViewById<Button>(R.id.button_cancel).also {
            it.setOnClickListener {
                exit()
            }
        }
        findViewById<ImageButton>(R.id.button_edit_path).also {
            it.setOnClickListener {
                showEditPathDialog()
            }
        }

        // listview
        listAdapter = FolderAdapter(
            context = this,
            dataList = mutableListOf(),
        )
        listView = findViewById<ListView>(R.id.list_view)
        listView.adapter = listAdapter
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            onListItemClick(position)
        }

        handleIntent(intent)

        handleOnBackPressed()
    }

    private fun handleIntent(intent: Intent) {
        val customTitle = intent.getStringExtra(EXTRA_TITLE)

        if (intent.hasExtra(EXTRA_DESCRIPTION)) {
            val desc = intent.getStringExtra(EXTRA_DESCRIPTION)
            if (!desc.isNullOrEmpty()) {
                findViewById<TextView>(R.id.text_view_desc).apply {
                    isVisible = true
                    text = desc
                }
            }
        }

        val isPickFiles = intent.getBooleanExtra(EXTRA_IS_PICK_FILES, false)

        val isCheckFolderWritePermission = intent.getBooleanExtra(EXTRA_IS_CHECK_WRITE_PERMISSION, false)

        val isEmptyFolderOnly = intent.getBooleanExtra(EXTRA_IS_EMPTY_FOLDER_ONLY, false).also {
            findViewById<View>(R.id.text_view_select_empty_dir_only).isVisible = it
        }

        title = when {
            !customTitle.isNullOrEmpty() -> {
                customTitle
            }
            isPickFiles -> {
                resourcesProvider.getString(R.string.title_pick_file)
            }
            else -> {
                resourcesProvider.getString(R.string.title_pick_folder)
            }
        }

        val initialPath = intent.getStringExtra(EXTRA_INITIAL_PATH).orEmpty()

        viewModel.init(
            isPickFiles = isPickFiles,
            isEmptyFolderOnly = isEmptyFolderOnly,
            isCheckFolderWritePermission = isCheckFolderWritePermission,
            initialPath = initialPath,
        )
    }

    override fun onBaseEvent(event: BaseEvent) {
        if (event is FolderPickerEvent) {
            when (event) {
                is FolderPickerEvent.LoadFolder -> {
                    loadFolder(
                        path = event.path,
                        items = event.items,
                        failures = event.failures,
                    )
                }
                is FolderPickerEvent.ShowErrorMessage -> {
                    showMessage(event.message)
                }
                is FolderPickerEvent.SelectPathAndExit -> {
                    selectPathAndExit(event.path)
                }
                FolderPickerEvent.Exit -> {
                    exit()
                }
            }
        } else {
            super.onBaseEvent(event)
        }
    }

    private fun handleOnBackPressed() {
        val callback = object : OnBackPressedCallback(enabled = true) {
            override fun handleOnBackPressed() {
                viewModel.moveUp()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.folder_picker, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                exit()
                true
            }
            R.id.action_home -> {
                viewModel.moveToHome()
                true
            }
            R.id.action_new_folder -> {
                showNewFolderDialog()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun loadFolder(
        path: String,
        items: List<FileItem>,
        failures: List<Failure>,
    ) {
        tbCurrentPath.text = path

        listAdapter.setData(items)
        listView.setSelectionAfterHeaderView()

        handleFailures(failures)
    }

    private fun handleFailures(failures: List<Failure>) {
        val errorMessages = failures.joinToString(separator = "\n") { failure ->
            when (failure) {
                is Failure.Folder.Write -> resourcesProvider.getString(R.string.error_folder_is_not_writable)
                is Failure.Folder.NotEmpty -> resourcesProvider.getString(R.string.error_folder_is_not_empty)
                else -> failureHandler.getFailureMessage(failure).getFullMessage()
            }
        }
        tbError.text = errorMessages

        val hasError = failures.isNotEmpty()
        tbError.isVisible = hasError
        bApply.isEnabled = !hasError
    }

    private fun onListItemClick(position: Int) {
        val item = listAdapter.getItem(position) as FileItem
        viewModel.onSelectFileItem(position, item)
    }

    private fun showNewFolderDialog() {
        val builder = AskDialogBuilder.create(this, R.layout.dialog_edit_text_with_label).also {
            it.setTitle(R.string.title_enter_folder_name)
        }
        val view = builder.view

        view.findViewById<TextView>(R.id.text_view_label).isVisible = false
        val editText = view.findViewById<EditText>(R.id.edit_text_value)

        val dialog = builder
            .setPositiveButton(R.string.action_create_folder) { _, _ ->
                val name = editText.text.toString()
                viewModel.createNewFolder(name)
            }
            .setNegativeButton(R.string.answer_cancel, null)
            .create()

        dialog.showKeyboard(editText, view)
        dialog.show()
    }

    private fun showEditPathDialog() {
        val builder = AskDialogBuilder.create(this, R.layout.dialog_edit_text_with_label).also {
            it.setTitle(R.string.title_enter_path)
        }
        val view = builder.view

        view.findViewById<TextView>(R.id.text_view_label).isVisible = false
        val editText = view.findViewById<EditText>(R.id.edit_text_value)

        val currentPath = viewModel.currentPath
        editText.setText(currentPath)
        editText.setSelection(currentPath.length)

        val dialog = builder
            .setPositiveButton(R.string.answer_ok) { _, _ ->
                val path = editText.text.toString()
                viewModel.moveToFolder(File(path))
            }
            .setNegativeButton(R.string.answer_cancel, null)
            .create()

        dialog.showKeyboard(editText, view)
        dialog.show()
    }

    private fun selectPathAndExit(path: String) {
        receivedIntent?.putExtra(EXTRA_PATH, path)
        setResult(RESULT_OK, receivedIntent)
        finish()
    }

    private fun exit() {
        setResult(RESULT_CANCELED, receivedIntent)
        finish()
    }

}
