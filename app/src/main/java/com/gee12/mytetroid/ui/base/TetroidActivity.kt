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
import androidx.lifecycle.lifecycleScope
import com.gee12.mytetroid.R
import com.gee12.mytetroid.common.Constants
import com.gee12.mytetroid.common.extensions.hideKeyboard
import com.gee12.mytetroid.common.utils.ViewUtils
import com.gee12.mytetroid.data.settings.CommonSettings
import com.gee12.mytetroid.di.ScopeSource
import com.gee12.mytetroid.domain.manager.CommonSettingsManager
import com.gee12.mytetroid.domain.provider.IResourcesProvider
import com.gee12.mytetroid.logs.LogType
import com.gee12.mytetroid.logs.Message
import com.gee12.mytetroid.model.enums.TetroidPermission
import com.gee12.mytetroid.ui.TetroidMessage
import com.gee12.mytetroid.ui.about.AboutActivity
import com.gee12.mytetroid.ui.base.views.ActivityDoubleTapListener
import com.gee12.mytetroid.ui.dialogs.AskDialogs
import com.gee12.mytetroid.ui.record.RecordActivity
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.scope.Scope
import org.koin.core.scope.get

abstract class TetroidActivity<VM : BaseViewModel>
    : AppCompatActivity(), View.OnTouchListener, IViewEventListener {

    interface IDownloadFileResult {
        fun onSuccess(uri: Uri)
        fun onError(ex: Exception)
    }

    protected lateinit var scopeSource: ScopeSource
    protected val koinScope: Scope
        get() = scopeSource.scope

    val resourcesProvider: IResourcesProvider by inject()
    val settingsManager: CommonSettingsManager by inject()

    protected var receivedIntent: Intent? = null
    protected var optionsMenu: Menu? = null
    protected lateinit var tvSingleTitle: TextView
    protected lateinit var tvTitle: TextView
    protected lateinit var tvSubtitle: TextView
    protected lateinit var toolbar: Toolbar
    protected lateinit var layoutProgress: LinearLayout
    protected lateinit var tvProgress: TextView

    protected lateinit var gestureDetector: GestureDetectorCompat
    protected var isFullScreen = false
    protected var isOnCreateProcessed = false
    protected var isUICreated = false

    lateinit var viewModel: VM


    protected abstract fun getLayoutResourceId(): Int

    protected abstract fun getViewModelClazz(): Class<VM>

    protected open fun isSingleTitle() = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutResourceId())

        receivedIntent = intent

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        setVisibilityActionHome(true)

        // обработчик нажатия на экране
        gestureDetector = GestureDetectorCompat(this,
            ActivityDoubleTapListener {
                toggleFullscreen(true)
            })
        tvSingleTitle = toolbar.findViewById(R.id.text_view_single_title)
        tvTitle = toolbar.findViewById(R.id.text_view_title)
        tvSubtitle = toolbar.findViewById(R.id.text_view_subtitle)
        layoutProgress = findViewById(R.id.layout_progress_bar)
        tvProgress = findViewById(R.id.progress_text)

        setTitle(title)

        isOnCreateProcessed = false
        isUICreated = false

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

        viewModel.logDebug(getString(R.string.log_activity_opened_mask, javaClass.simpleName))

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
        if (optionsMenu != null) {
            onCreateOptionsMenu(optionsMenu!!)
            onPrepareOptionsMenu(optionsMenu)
        }
    }

    /**
     * Обработчик события, когда создались все элементы интерфейса.
     * Вызывается из onCreateOptionsMenu(), который, в свою очередь, принудительно вызывается после onCreate().
     */
    protected open fun onUICreated(uiCreated: Boolean) {
    }

    /**
     * Обработчик изменения состояния View.
     */
    protected open fun onBaseEvent(event: BaseEvent) {
        when (event) {
            is BaseEvent.ShowProgress -> setProgressVisibility(event.isVisible)
            is BaseEvent.ShowProgressText -> showProgress(event.message)
            is BaseEvent.Permission.ShowRequest -> showPermissionRequest(
                permission = event.permission,
                requestCallback = event.requestCallback,
            )
            is BaseEvent.TaskStarted -> {
                taskPreExecute(event.titleResId ?: R.string.progress_loading)
            }
            BaseEvent.TaskFinished -> taskPostExecute()
            BaseEvent.ShowMoreInLogs -> showSnackMoreInLogs()
            else -> {}
        }
    }

    private fun showPermissionRequest(
        permission: TetroidPermission,
        requestCallback: () -> Unit
    ) {
        // диалог с объяснием зачем нужно разрешение
        AskDialogs.showYesDialog(
            context = this,
            message = permission.getPermissionRequestMessage(resourcesProvider),
            onApply = {
                requestCallback.invoke()
            },
        )
    }

    protected open fun onPermissionGranted(requestCode: Int) {
        // по-умолчанию обрабатываем результат разрешения во ViewModel,
        //  но можем переопределить onPermissionGranted и в активити
        viewModel.onPermissionGranted(requestCode)
    }

    protected fun onPermissionCanceled(requestCode: Int) {
        viewModel.onPermissionCanceled(requestCode)
    }

    /**
     * Установка заголовка активности.
     * @param title
     */
    override fun setTitle(title: CharSequence?) {
        val isSingleTitle = isSingleTitle()
        tvTitle.isVisible = !isSingleTitle
        tvSubtitle.isVisible = !isSingleTitle
        tvSingleTitle.isVisible = isSingleTitle
        if (isSingleTitle) {
            tvSingleTitle.text = title
        } else {
            tvTitle.text = title
        }
    }

    /**
     * Установка подзаголовка активности.
     * @param title
     */
    fun setSubtitle(title: CharSequence?) {
        tvSubtitle.text = title
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
     * @param fromDoubleTap
     * @return
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
     * @param v
     * @param event
     * @return
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
        if (!isUICreated) {
            // для отображения иконок
            if (menu is MenuBuilder) {
                menu.setOptionalIconsVisible(true)
            }
        }
        // устанавливаем флаг, что стандартные элементы активности созданы
        onUICreated(!isUICreated)
        isUICreated = true
        return true
    }

/*    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!isOnCreateCalled())
            return true;
        return false;
    }*/

    fun updateOptionsMenu() {
        if (optionsMenu != null) {
            onPrepareOptionsMenu(optionsMenu)
        } else {
            viewModel.logWarning("TetroidActivity.updateOptionsMenu(): optionsMenu is null", false)
        }
    }

    /**
     * Обработчик выбора пунктов системного меню.
     * @param item
     * @return
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_fullscreen -> {
                toggleFullscreen(false)
                return true
            }
            R.id.action_about_app -> {
                AboutActivity.start(this)
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

        // разрешение на запись в память на Android 11 запрашивается с помощью Intent
        if (requestCode == Constants.REQUEST_CODE_PERMISSION_WRITE_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val isGranted = (viewModel.permissionInteractor.hasWriteExtStoragePermission(this))
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
        when (requestCode) {
            Constants.REQUEST_CODE_PERMISSION_WRITE_STORAGE -> {
                if (isGranted) {
                    viewModel.log(R.string.log_write_ext_storage_perm_granted)
                } else {
                    viewModel.logWarning(R.string.log_missing_read_ext_storage_permissions, true)
                }
            }
            Constants.REQUEST_CODE_PERMISSION_WRITE_TEMP -> {
                if (isGranted) {
                    viewModel.log(R.string.log_write_ext_storage_perm_granted)
                } else {
                    viewModel.logWarning(R.string.log_missing_write_ext_storage_permissions, true)
                }
            }
            Constants.REQUEST_CODE_PERMISSION_TERMUX -> {
                if (isGranted) {
                    viewModel.log(R.string.log_run_termux_commands_perm_granted)
                } else {
                    viewModel.logWarning(R.string.log_missing_run_termux_commands_permissions, true)
                }
            }
            else -> return
        }
        if (isGranted) {
            onPermissionGranted(requestCode)
        } else {
            onPermissionCanceled(requestCode)
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

    override fun setProgressVisibility(isVisible: Boolean, text: String?) {
        layoutProgress.isVisible = isVisible
        tvProgress.text = text
    }

    override fun showProgress(textResId: Int?) {
        showProgress(text = textResId?.let { getString(it) })
    }

    override fun showProgress(text: String?) {
        setProgressVisibility(isVisible = true, text)
    }

    override fun hideProgress() {
        setProgressVisibility(isVisible = false)
    }

    /**
     * Установка видимости пункта меню.
     */
    protected fun visibleMenuItem(menuItem: MenuItem?, isVisible: Boolean) {
        ViewUtils.setVisibleIfNotNull(menuItem, isVisible)
    }

    /**
     * Установка активности пункта меню.
     * @param menuItem
     * @param isEnabled
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
     * @param subtitle
     */
    protected fun setSubtitle(subtitle: String?) {
        tvSubtitle.visibility = View.VISIBLE
        tvSubtitle.textSize = 16f
        tvSubtitle.text = subtitle
    }

    open fun showActivityForResult(cls: Class<*>?, requestCode: Int) {
        val intent = Intent(this, cls)
        startActivityForResult(intent, requestCode)
    }

    /**
     * Публикация сообщений.
     */
    protected fun showMessage(message: Message) {
        TetroidMessage.show(this, message)
    }

    protected fun showMessage(messageResId: Int, type: LogType = LogType.INFO) {
        showMessage(getString(messageResId), type)
    }

    protected fun showMessage(message: String, type: LogType = LogType.INFO) {
        showMessage(Message(message, type))
    }

    /**
     * Вывод интерактивного уведомления SnackBar "Подробнее в логах".
     */
    override fun showSnackMoreInLogs() {
        TetroidMessage.showSnackMoreInLogs(this, R.id.layout_coordinator)
    }
}