package com.gee12.mytetroid.ui.scripts

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.*
import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.logs.LogObj
import com.gee12.mytetroid.model.*
import com.gee12.mytetroid.model.enums.Tense
import com.gee12.mytetroid.model.enums.TetroidObjectType
import com.gee12.mytetroid.model.permission.PermissionRequestCode
import com.gee12.mytetroid.ui.base.BaseEvent
import com.gee12.mytetroid.ui.base.TetroidActivity
import com.gee12.mytetroid.ui.dialogs.AskDialogs
import com.gee12.mytetroid.ui.dialogs.script.DefaultScriptsDialog
import com.gee12.mytetroid.ui.dialogs.script.ScriptFieldsDialog
import com.github.clans.fab.FloatingActionMenu


class ScriptsActivity : TetroidActivity<ScriptsViewModel>() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ScriptsAdapter

    override fun getLayoutResourceId() = R.layout.activity_scripts

    override fun getViewModelClazz() = ScriptsViewModel::class.java

    override fun isSingleTitle() = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        recyclerView = findViewById(R.id.recycle_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(
            com.gee12.mytetroid.ui.base.views.DividerItemDecoration(
                context = recyclerView.context,
                orientation = DividerItemDecoration.VERTICAL,
                spaceBetweenRes = R.dimen.recycler_view_item_top_spacing
            )
        )
        recyclerView.addOnSwipeRefreshListener {
            viewModel.loadScripts()
        }

        adapter = ScriptsAdapter(
            context = this,
            resourcesProvider = resourcesProvider,
            failureHandler = failureHandler,
            isLoadedFavoritesOnly = viewModel.isLoadedFavoritesOnly()
        )
        adapter.onItemClickListener = { script, _ ->
            viewModel.openScriptForEdit(script)
        }
        adapter.onItemLongClickListener = { script, view ->
            showScriptPopupMenu(view, script)
            true
        }
        adapter.onItemSwitchClickListener = { script, isChecked, _ ->
            viewModel.setScriptIsActiveForCurrentObject(script, isActive = isChecked)
        }
        adapter.onItemMenuClickListener = { script, view ->
            showScriptPopupMenu(view, script)
        }
        adapter.onObjectItemSwitchClickListener = { scriptToObject, isChecked, _ ->
            viewModel.setScriptToObjectActivated(scriptToObject, isActive = isChecked)
        }
        recyclerView.adapter = adapter

        findViewById<FloatingActionMenu>(R.id.fab_add_script).also { fabAddScript ->
            fabAddScript.isVisible = !viewModel.isScriptForObject()
            fabAddScript.setClosedOnTouchOutside(true)

            findViewById<com.github.clans.fab.FloatingActionButton>(R.id.fab_create_new).also {
                it.setOnClickListener {
                    fabAddScript.close(true)
                    showScriptFieldsDialog(isNew = true)
                }
            }
            findViewById<com.github.clans.fab.FloatingActionButton>(R.id.fab_pick_file).also {
                it.setOnClickListener {
                    fabAddScript.close(true)
                    openFilePickerForAttachScript()
                }
            }
            findViewById<com.github.clans.fab.FloatingActionButton>(R.id.fab_choose_default).also {
                it.setOnClickListener {
                    fabAddScript.close(true)
                    showDefaultScriptsDialog()
                }
            }
        }

        initData(receivedIntent)
    }

    override fun createDependencyScope() {
        scopeSource = ScopeSource.current
    }

    override fun onBaseEvent(event: BaseEvent) {
        when (event) {
            is ScriptsEvent -> {
                onStoragesEvent(event)
            }
            is BaseEvent.Permission.Granted -> {
            }
            else -> super.onBaseEvent(event)
        }
    }

    private fun onStoragesEvent(event: ScriptsEvent) {
        when (event) {
            is ScriptsEvent.LoadScripts -> {
                setData(
                    scripts = event.scripts,
                    tetroidObject = event.tetroidObject,
                    isMoveToLastItem = event.isMoveToLastItem,
                )
            }
            ScriptsEvent.ShowRequestForDefaultScripts -> {
                showRequestForDefaultScriptsDialog()
            }
            is ScriptsEvent.ShowScriptDialog -> {
                showScriptFieldsDialog(
                    script = event.script,
                    text = event.scriptText,
                    isNew = event.isNew,
                )
            }
            is ScriptsEvent.OpenScriptFile -> {
                openScriptFile(
                    uri = event.uri,
                    mimeType = event.mimeType,
                )
            }
        }
    }

    private fun initData(intent: Intent?) {
        val objectTypeId = intent?.getIntExtra(EXTRA_OBJECT_TYPE_ID, TetroidObjectType.NONE.id)
        val objectId = intent?.getStringExtra(EXTRA_OBJECT_ID)

        viewModel.init(objectTypeId, objectId)
    }

    private fun setData(
        scripts: List<TetroidScript>,
        tetroidObject: ITetroidObject?,
        isMoveToLastItem: Boolean,
    ) {
        val subtitle = if (tetroidObject != null) {
            tvSubtitle?.applyTextColor(R.color.warning_2)
            val logObj = FoundType(tetroidObject.type).toLogObj() ?: LogObj.NONE
            val typeName = logObj.getString(Tense.PRESENT_CONTINUOUS, resourcesProvider)
            resourcesProvider.getString(R.string.subtitle_scripts_for_object_masked, typeName, tetroidObject.name )
        } else {
            resourcesProvider.getString(R.string.subtitle_scripts_for_all_storage_masked, viewModel.getStorageName())
        }
        setSubtitle(subtitle)

        showWarnings()

        adapter.submitList(scripts, tetroidObject)
        findViewById<TextView>(R.id.text_view_empty_scripts)?.isVisible = scripts.isEmpty()

        if (isMoveToLastItem) {
            scrollToLastScript()
        }
    }

    private fun showWarnings() {
        val warnings = buildString {
            if (viewModel.isLoadedFavoritesOnly()) {
                appendLine(resourcesProvider.getString(R.string.title_load_all_nodes_for_scripts))
            }
        }
        findViewById<ImageView>(R.id.image_view_warning)?.isVisible = warnings.isNotEmpty()
        findViewById<TextView>(R.id.text_view_warning)?.apply {
            isVisible = warnings.isNotEmpty()
            text = warnings
        }
    }

    fun scrollToScript(script: TetroidScript) {
        val position = adapter.getItemPositionById(scriptId = script.id.orZero())
        if (position > -1) {
            recyclerView.postDelayed(
                { recyclerView.smoothScrollToPosition(position) }, 300
            )
        }
    }

    private fun scrollToLastScript() {
        recyclerView.postDelayed(
            { recyclerView.smoothScrollToPosition(adapter.itemCount - 1) }, 300
        )
    }

    private fun showRequestForDefaultScriptsDialog() {
        AskDialogs.showYesDialog(
            context = this,
            message = getString(R.string.ask_script_add_default),
            onApply = {
                showDefaultScriptsDialog()
            }
        )
    }

    private fun showDefaultScriptsDialog() {
        DefaultScriptsDialog(
            resourcesProvider = resourcesProvider,
            onItemClick = {
                viewModel.addDefaultScript(script = it)
            },
        ).showIfPossibleAndNeeded(supportFragmentManager)
    }

    private fun showDeleteScriptDialog(script: TetroidScript) {
        AskDialogs.showYesDialog(
            context = this,
            message = getString(R.string.ask_script_delete_mask, script.fileName),
            onApply = {
                viewModel.deleteScript(script, withFile = true)
            }
        )
    }

    private fun showScriptFieldsDialog(
        script: TetroidScript? = null,
        text: String? = null,
        isNew: Boolean,
    ) {
        ScriptFieldsDialog(
            script = script,
            scriptText = text,
            storageId = viewModel.getStorageId(),
            onApply = { name, fileName, description, updatedText ->
                if (isNew) {
                    viewModel.addNewScript(
                        name = name,
                        fileName = fileName,
                        description = description,
                        scriptText = updatedText,
                    )
                } else if (script != null) {
                    viewModel.editScript(
                        script = script,
                        name = name,
                        fileName = fileName,
                        description = description,
                        scriptText = updatedText,
                    )
                }
            }
        ).showIfPossibleAndNeeded(supportFragmentManager)
    }

    // region File

    private fun openFilePickerForAttachScript() {
        openFilePicker(
            requestCode = PermissionRequestCode.PICK_SCRIPT_FILE,
            allowMultiple = false,
            // FIXME: не работает, не дает вообще выбрать файлы
            //filterMimeTypes = arrayOf("text/javascript"),
        )
    }

    override fun isUseFileStorage() = true

    override fun onFileSelected(requestCode: Int, files: List<DocumentFile>) {
        when (PermissionRequestCode.fromCode(requestCode)) {
            PermissionRequestCode.PICK_SCRIPT_FILE -> {
                files.firstOrNull()?.also {
                    viewModel.addNewScriptFromFile(scriptFile = it)
                }
            }
            else -> Unit
        }
    }

    private fun openScriptFile(uri: Uri, mimeType: String) {
        interactionManager.openFile(
            activity = this,
            uri = uri,
            mimeType = mimeType,
        )
    }

    // endregion File

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.scripts, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_activate_all -> {
                viewModel.setAllScriptsIsActiveForAllStorage(isActive = true)
                return true
            }
            R.id.action_deactivate_all -> {
                viewModel.setAllScriptsIsActiveForAllStorage(isActive = false)
                return true
            }
            R.id.action_search -> {

                return true
            }
            R.id.action_choice_mode -> {
                // TODO: включаем режим множественного выбора (ActionMode) для RecyclerView
//                ActionModeController(R.menu.storage_actions, ActionMode.TYPE_PRIMARY, a).startActionMode(this)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("RestrictedApi")
    private fun showScriptPopupMenu(anchorView: View, script: TetroidScript) {
        val popupMenu = PopupMenu(this, anchorView)
        popupMenu.inflate(R.menu.script_context)

        val menu = popupMenu.menu
        val isFileExist = script.isFileExist()
        (menu.findItem(R.id.action_open_file))?.isVisible = isFileExist
        (menu.findItem(R.id.action_duplicate))?.isVisible = isFileExist
        val isScriptForObject = viewModel.isScriptForObject() && !script.isHasErrors()
        val isScriptActiveForStorage = script.isActiveForObject(null)
        (menu.findItem(R.id.action_activate_for_storage))?.isVisible = isScriptForObject && !isScriptActiveForStorage
        (menu.findItem(R.id.action_deactivate_for_storage))?.isVisible = isScriptForObject && isScriptActiveForStorage

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    viewModel.openScriptForEdit(script)
                    true
                }
                R.id.action_duplicate -> {
                    viewModel.duplicateScript(script)
                    true
                }
                R.id.action_open_file -> {
                    viewModel.prepareScriptFileForOpen(script)
                    true
                }
                R.id.action_activate_for_storage -> {
                    viewModel.setScriptIsActiveForAllStorage(script, isActive = true)
                    true
                }
                R.id.action_deactivate_for_storage -> {
                    viewModel.setScriptIsActiveForAllStorage(script, isActive = false)
                    true
                }
                R.id.action_delete -> {
                    showDeleteScriptDialog(script)
                    true
                }
                else -> false
            }
        }
        (popupMenu.menu as MenuBuilder).showForcedWithIcons(anchorView)
    }

    override fun finish() {
        setResult()
        super.finish()
    }

    private fun setResult() {
        val intent = buildIntent {
            putExtra(EXTRA_IS_SCRIPTS_CHANGED, viewModel.isScriptsChanged())
        }
        setResult(Activity.RESULT_OK, intent)
    }

    companion object {

        private const val EXTRA_OBJECT_TYPE_ID = "EXTRA_OBJECT_TYPE_ID"
        private const val EXTRA_OBJECT_ID = "EXTRA_OBJECT_ID"
        const val EXTRA_IS_SCRIPTS_CHANGED = "IS_SCRIPTS_CHANGED"

        fun start(activity: Activity, obj: TetroidObject?, requestCode: Int) {
            val intent = buildIntent {
                setClass(activity, ScriptsActivity::class.java)
                obj?.also {
                    putExtra(EXTRA_OBJECT_TYPE_ID, obj.type)
                    putExtra(EXTRA_OBJECT_ID, obj.id)
                }
            }
            activity.startActivityForResult(intent, requestCode)
        }
    }
}