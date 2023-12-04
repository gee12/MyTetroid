package com.gee12.mytetroid.ui.base

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.anggrayudi.storage.SimpleStorageHelper
import com.anggrayudi.storage.file.FileFullPath
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.extensions.hideKeyboard
import com.gee12.mytetroid.common.utils.ViewUtils
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.domain.manager.CommonSettingsManager
import com.gee12.mytetroid.domain.provider.BuildInfoProvider
import com.gee12.mytetroid.domain.provider.IAppPathProvider
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.logs.ITetroidLogger
import com.gee12.mytetroid.logs.LogType
import com.gee12.mytetroid.logs.Message
import com.gee12.mytetroid.model.permission.PermissionRequestCode
import com.gee12.mytetroid.model.permission.TetroidPermission
import com.gee12.mytetroid.ui.TetroidMessage
import com.gee12.mytetroid.ui.about.AboutAppActivity
import com.gee12.mytetroid.ui.base.views.ActivityDoubleTapListener
import com.gee12.mytetroid.ui.record.RecordActivity
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.scope.Scope
import org.koin.core.scope.get

abstract class TetroidActivity<VM : BaseViewModel>
    : AppCompatActivity(), ITetroidComponent, View.OnTouchListener {

    interface IDownloadFileResult {
        fun onSuccess(uri: Uri)
        fun onError(ex: Exception)
    }

    protected lateinit var scopeSource: ScopeSource
    protected val koinScope: Scope
        get() = scopeSource.scope

    lateinit var viewModel: VM

    val resourcesProvider: IResourcesProvider by inject()
    val settingsManager: CommonSettingsManager by inject()
    val appPathProvider: IAppPathProvider by inject()
    val buildInfoProvider: BuildInfoProvider by inject()
    val logger: ITetroidLogger by inject()

    protected var receivedIntent: Intent? = null
    var optionsMenu: Menu? = null
    // toolbar
    protected var toolbar: Toolbar? = null
    protected var tvSingleTitle: TextView? = null
    protected var tvTitle: TextView? = null
    protected var tvSubtitle: TextView? = null
    // progress bar
    protected var layoutProgress: LinearLayout? = null
    protected var tvProgress: TextView? = null

    protected lateinit var gestureDetector: GestureDetectorCompat
    protected var isFullScreen = false
    protected var isOnCreateProcessed = false
    protected var isUiCreated = false

    var fileStorageHelper: SimpleStorageHelper? = null

    private var requestCodeForStorageAccess: PermissionRequestCode? = null

    // region Create

    protected abstract fun getLayoutResourceId(): Int

    protected abstract fun getViewModelClazz(): Class<VM>

    protected open fun hasToolbar() = true

    protected open fun hasProgressBar() = true

    protected open fun isSingleTitle() = true

    protected open fun isAppearInLogs() = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutResourceId())

        receivedIntent = intent

        if (hasToolbar()) {
            toolbar = findViewById(R.id.toolbar)
            setSupportActionBar(toolbar)
            setVisibilityActionHome(true)

            tvSingleTitle = toolbar?.findViewById(R.id.text_view_single_title)
            tvTitle = toolbar?.findViewById(R.id.text_view_title)
            tvSubtitle = toolbar?.findViewById(R.id.text_view_subtitle)

            setTitle(title)
        }

        if (hasProgressBar()) {
            layoutProgress = findViewById(R.id.layout_progress_bar)
            tvProgress = findViewById(R.id.progress_text)
        }

        // обработчик нажатия на экране
        gestureDetector = GestureDetectorCompat(this,
            ActivityDoubleTapListener {
                toggleFullscreen(true)
            })
        isOnCreateProcessed = false
        isUiCreated = false

        if (isUseFileStorage()) {
            fileStorageHelper = SimpleStorageHelper(this, savedInstanceState)
            // ради оптимизации удаляются "повторные" persistable uri, к которым юзер предоставил доступ в SAF,
            // и оставляются только "родительские", из-за чего происходит циклический запрос доступа на Android >= 11
            fileStorageHelper?.storage?.isCleanupRedundantUriPermissions = false
            initFileStorage(savedInstanceState)
        }

        createDependencyScope()
        afterCreateDependencyScope()
        createViewModel()
        initViewModel()
    }

    protected open fun createDependencyScope() {
        scopeSource = ScopeSource.createNew()
    }

    protected open fun afterCreateDependencyScope() {
    }

    protected open fun createViewModel() {
        viewModel = koinScope.get(getViewModelClazz())
    }

    protected open fun initViewModel() {
        viewModel.initialize()

        if (isAppearInLogs()) {
            logger.logDebug(getString(R.string.log_activity_opened_mask, javaClass.simpleName))
        }

        lifecycleScope.launch {
            viewModel.eventFlow.collect { event -> onBaseEvent(event) }
        }
        lifecycleScope.launch {
            viewModel.messageEventFlow.collect { message -> showMessage(message) }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onStart() {
        super.onStart()
        viewModel.setNotificatorCallbacks()
    }

    /**
     * Установка пометки, что обработчик OnCreate был вызван, и можно вызвать другие обработчики,
     * следующие за ним (а не вразнобой на разных устройствах).
     */
    protected fun afterOnCreate() {
        isOnCreateProcessed = true
        optionsMenu?.let {
            onCreateOptionsMenu(it)
            onPrepareOptionsMenu(it)
        }
    }

    /**
     * Обработчик события, когда создались все элементы интерфейса.
     * Вызывается из onCreateOptionsMenu(), который, в свою очередь, принудительно вызывается после onCreate().
     */
    protected open fun onUiCreated() {
    }

    // endregion Create

    // region Events

    /**
     * Обработчик изменения состояния View.
     */
    protected open fun onBaseEvent(event: BaseEvent) {
        when (event) {
            is BaseEvent.ShowProgress -> setProgressVisibility(true)
            is BaseEvent.HideProgress -> setProgressVisibility(false)
            is BaseEvent.ShowProgressWithText -> showProgress(event.message)
            is BaseEvent.TaskStarted -> {
                taskPreExecute(event.titleResId ?: R.string.state_loading)
            }
            BaseEvent.TaskFinished -> taskPostExecute()
            BaseEvent.ShowMoreInLogs -> showSnackMoreInLogs()
            else -> Unit
        }
    }

    // endregion Events

    // region Permissions

    protected open fun onPermissionGranted(
        permission: TetroidPermission?,
        requestCode: PermissionRequestCode,
    ) = Unit

    protected open fun onPermissionCanceled(
        requestCode: PermissionRequestCode
    ) = Unit

    // endregion Permissions

    // region File

    override fun isUseFileStorage() = false

    protected fun requestFileStorageAccess(
        uri: Uri,
        requestCode: PermissionRequestCode,
    ) {
        requestCodeForStorageAccess = requestCode
        fileStorageHelper?.requestStorageAccess(
        //fileStorageHelper.openFolderPicker(
            requestCode = requestCode.code,
            initialPath = FileFullPath(this, fullPath = uri.toString()),
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        fileStorageHelper?.storage?.checkIfFileReceived(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        fileStorageHelper?.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        fileStorageHelper?.onRestoreInstanceState(savedInstanceState)
    }

    // TODO: в отдельный FileStorageManager
    private fun initFileStorage(savedInstanceState: Bundle?) {
        savedInstanceState?.let { fileStorageHelper?.onRestoreInstanceState(it) }
        fileStorageHelper?.onStorageAccessGranted = { requestCode, root ->
            onStorageAccessGranted(requestCode, root)
        }
        fileStorageHelper?.onFileSelected = { requestCode, files ->
            onFileSelected(requestCode, files)
        }
        fileStorageHelper?.onFolderSelected = { requestCode, folder ->
            // сохраняем путь
            settingsManager.setLastSelectedFolderPath(path = folder.uri.toString())

            if (requestCodeForStorageAccess?.code == requestCode) {
                onStorageAccessGranted(requestCode, folder)
            } else {
                onFolderSelected(requestCode, folder)
            }
        }
        /*fileStorageHelper.onFileCreated = { requestCode, file ->
            onFileCreated(requestCode, file)
        }
        fileStorageHelper.onFileReceived = object : SimpleStorageHelper.OnFileReceived {
            override fun onFileReceived(files: List<DocumentFile>) {
                val names = files.joinToString(", ") { it.fullName }
                Toast.makeText(baseContext, "File received: $names", Toast.LENGTH_SHORT).show()
            }

            override fun onNonFileReceived(intent: Intent) {
                Toast.makeText(baseContext, "Non-file is received", Toast.LENGTH_SHORT).show()
            }
        }*/
        if (savedInstanceState == null) {
            fileStorageHelper?.storage?.checkIfFileReceived(intent)
        }
    }

    override fun onStorageAccessGranted(requestCode: Int, root: DocumentFile) {}

    override fun onFolderSelected(requestCode: Int, folder: DocumentFile) {}

    override fun onFileSelected(requestCode: Int, files: List<DocumentFile>) {}

    //override fun onFileCreated(requestCode: Int, file: DocumentFile) {}

    // TODO: в отдельный FileStorageManager или TetroidActivityComponent (для android-зависимой логики)
    fun openFilePicker(
        requestCode: PermissionRequestCode,
        allowMultiple: Boolean = false,
        initialPath: String? = null,
        filterMimeTypes: Array<String> = emptyArray(),
    ) {
        val path = initialPath ?: settingsManager.getLastSelectedFolderPathOrDefault(true)
        try {
            fileStorageHelper?.openFilePicker(
                requestCode = requestCode.code,
                allowMultiple = allowMultiple,
                initialPath = path?.let { FileFullPath(this, fullPath = it) },
                filterMimeTypes = filterMimeTypes,
            )
        } catch (ex: Exception) {
            try {
                fileStorageHelper?.openFilePicker(
                    requestCode = requestCode.code,
                    allowMultiple = allowMultiple,
                    initialPath = null,
                    filterMimeTypes = filterMimeTypes,
                )
                viewModel.logWarning(getString(R.string.error_incorrent_initial_path_for_picker_mask, initialPath), show = false)
            } catch (ex: Exception) {
                viewModel.logError(getString(R.string.error_open_file_folder_picker), show = true)
            }
        }
    }

    // TODO: в отдельный FileStorageManager
    fun openFolderPicker(
        requestCode: PermissionRequestCode,
        initialPath: String? = null,
    ) {
        val path = initialPath ?: settingsManager.getLastSelectedFolderPathOrDefault(true)
        try {
            fileStorageHelper?.openFolderPicker(
                requestCode = requestCode.code,
                initialPath = path?.let { FileFullPath(this, fullPath = it) },
            )
        } catch (ex: Exception) {
            try {
                fileStorageHelper?.openFolderPicker(
                    requestCode = requestCode.code,
                    initialPath = null,
                )
                viewModel.logWarning(getString(R.string.error_incorrent_initial_path_for_picker_mask, initialPath), show = false)
            } catch (ex: Exception) {
                viewModel.logError(getString(R.string.error_open_file_folder_picker), show = true)
            }
        }
    }

    // endregion File

    /**
     * Установка заголовка активности.
     */
    override fun setTitle(title: CharSequence?) {
        val isSingleTitle = isSingleTitle()
        tvTitle?.isVisible = !isSingleTitle
        tvSubtitle?.isVisible = !isSingleTitle
        tvSingleTitle?.isVisible = isSingleTitle
        if (isSingleTitle) {
            tvSingleTitle?.text = title
        } else {
            tvTitle?.text = title
        }
    }

    /**
     * Установка подзаголовка активности.
     */
    fun setSubtitle(title: CharSequence?) {
        tvSubtitle?.text = title
    }

    //    /**
    //     * Если потеряли фокус на активности, то выходим их полноэкранного режима
    //     * (например, при нажатии на "физическую" кнопку вызова меню).
    //     * @param hasFocus
    //     */
    //    @Override
    //    public void onWindowFocusChanged(boolean hasFocus) {
    //        super.onWindowFocusChanged(hasFocus);
    //        if (!hasFocus) {
    //            setFullscreen(false);
    //        }
    //    }

    /**
     * Включение/отключение полноэкранного режима.
     * Оставлен только в RecordActivity.
     */
    open fun toggleFullscreen(fromDoubleTap: Boolean): Int {
        if (this is RecordActivity) {
            if (!fromDoubleTap || CommonSettings.isDoubleTapFullscreen(this)) {
                val newValue = !isFullScreen
                ViewUtils.setFullscreen(this, newValue)
                isFullScreen = newValue
                return if (newValue) 1 else 0
            }
        } else {
            return -1
        }
        return -1
    }

    /**
     * Переопределяем обработчик нажатия на экране
     * для обработки перехода в полноэкранный режим.
     */
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return false
    }

    fun onBeforeCreateOptionsMenu(menu: Menu?): Boolean {
        val onCreateCalled = isOnCreateProcessed
        if (!onCreateCalled) {
            optionsMenu = menu
        }
        return onCreateCalled
    }

    @SuppressLint("RestrictedApi")
    fun onAfterCreateOptionsMenu(menu: Menu?): Boolean {
        // запускаем только 1 раз
        if (!isUiCreated) {
            // для отображения иконок
            if (menu is MenuBuilder) {
                menu.setOptionalIconsVisible(true)
            }
        }
        // устанавливаем флаг, что стандартные элементы активности созданы
        onUiCreated()
        isUiCreated = true
        return true
    }

    fun updateOptionsMenu() {
        optionsMenu?.let {
            onPrepareOptionsMenu(it)
        }
    }

    /**
     * Обработчик выбора пунктов системного меню.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_fullscreen -> {
                toggleFullscreen(false)
                return true
            }
            R.id.action_about_app -> {
                AboutAppActivity.start(this)
                return true
            }
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Обработчик результата активити.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // TODO: ?
        // разрешение на запись в память на Android 11 запрашивается с помощью Intent
        if (requestCode == PermissionRequestCode.OPEN_STORAGE_FOLDER.code) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val isGranted = (viewModel.permissionManager.hasWriteExtStoragePermission(this))
                onRequestPermissionsResult(
                    requestCode = requestCode,
                    permissions = emptyArray(),
                    grantResults = IntArray(1) {
                        if (isGranted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED }
                )
            }
        }
    }

    /**
     * Обработчик запроса разрешения.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val isGranted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        val permissionRequestCode = PermissionRequestCode.fromCode(requestCode)
        permissionRequestCode?.also {
            if (isGranted) {
                viewModel.log(getString(R.string.log_permission_granted_mask, it.name))
                onPermissionGranted(permission = null, requestCode = it)
            } else {
                viewModel.log(getString(R.string.log_permission_canceled_mask, it.name))
                onPermissionCanceled(requestCode = it)
            }
        }
    }

    fun taskPreExecute(progressTextResId: Int) {
        blockInterface()
        showProgress(progressTextResId)
        window.decorView.hideKeyboard()
    }

    fun taskPostExecute() {
        unblockInterface()
        setProgressVisibility(isVisible = false)
    }

    fun blockInterface() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    fun unblockInterface() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    /**
     * Обработчик, вызываемый перед запуском кода в обработчике onBackPressed().
     * @return true - можно продолжить работу обработчика onBackPressed(), иначе - прервать
     */
    fun onBeforeBackPressed(): Boolean {
        // если выполняется задание, то не реагируем на нажатие кнопки Back
        return !viewModel.isBusy
    }

    protected fun setVisibilityActionHome(isVisible: Boolean) {
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(isVisible)
    }

    // region IAndroidComponent

    override fun setProgressVisibility(isVisible: Boolean, text: String?) {
        layoutProgress?.isVisible = isVisible
        tvProgress?.text = text
    }

    override fun showProgress(textResId: Int) {
        showProgress(text = getString(textResId))
    }

    override fun showProgress(text: String?) {
        setProgressVisibility(isVisible = true, text)
    }

    override fun hideProgress() {
        setProgressVisibility(isVisible = false)
    }

    /**
     * Публикация сообщений.
     */
    fun showMessage(message: Message) {
        TetroidMessage.show(this, message)
    }

    protected fun showMessage(messageResId: Int, type: LogType = LogType.INFO) {
        showMessage(getString(messageResId), type)
    }

    protected fun showMessage(message: String, type: LogType = LogType.INFO) {
        showMessage(Message(message, type))
    }

    // endregion IAndroidComponent

    /**
     * Вывод интерактивного уведомления SnackBar "Подробнее в логах".
     */
    override fun showSnackMoreInLogs() {
        TetroidMessage.showSnackMoreInLogs(this, R.id.layout_coordinator)
    }
    /**
     * Установка видимости пункта меню.
     */
    protected fun visibleMenuItem(menuItem: MenuItem?, isVisible: Boolean) {
        ViewUtils.setVisibleIfNotNull(menuItem, isVisible)
    }

    /**
     * Установка активности пункта меню.
     */
    protected fun enableMenuItem(menuItem: MenuItem?, isEnabled: Boolean) {
        ViewUtils.setEnabledIfNotNull(menuItem, isEnabled)
    }

    /**
     * Принудительное отображение иконок у пунктов меню.
     */
    @SuppressLint("RestrictedApi")
    protected fun setForceShowMenuIcons(v: View, menu: MenuBuilder) {
        val menuHelper = MenuPopupHelper(this, menu, v)
        menuHelper.setForceShowIcon(true)
        menuHelper.show()
    }

    /**
     * Установка подзаголовка активности.
     */
    fun setSubtitle(subtitle: String?) {
        tvSubtitle?.visibility = View.VISIBLE
        tvSubtitle?.textSize = 16f
        tvSubtitle?.text = subtitle
    }

    open fun showActivityForResult(cls: Class<*>?, requestCode: Int) {
        val intent = Intent(this, cls)
        startActivityForResult(intent, requestCode)
    }

}