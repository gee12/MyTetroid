package com.gee12.mytetroid.ui.activities

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
import com.gee12.mytetroid.logs.Message
import com.gee12.mytetroid.ui.ActivityDoubleTapListener
import com.gee12.mytetroid.ui.IViewEventListener
import com.gee12.mytetroid.ui.TetroidMessage
import com.gee12.mytetroid.ui.dialogs.AskDialogs
import com.gee12.mytetroid.viewmodels.BaseViewModel
import com.gee12.mytetroid.viewmodels.PermissionRequestParams
import com.gee12.mytetroid.viewmodels.BaseEvent
import com.gee12.mytetroid.viewmodels.*
import kotlinx.coroutines.launch
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

    protected var receivedIntent: Intent? = null
    protected var optionsMenu: Menu? = null
    protected var tvTitle: TextView? = null
    protected var tvSubtitle: TextView? = null
    protected var toolbar: Toolbar? = null
    protected var layoutProgress: LinearLayout? = null
    protected var tvProgress: TextView? = null

    protected lateinit var gestureDetector: GestureDetectorCompat
    protected var isFullScreen = false
    protected var isOnCreateProcessed = false
    protected var isGUICreated = false

    protected lateinit var viewModel: VM


    protected abstract fun getLayoutResourceId(): Int

    protected abstract fun getViewModelClazz(): Class<VM>


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
        tvTitle = toolbar?.findViewById(R.id.text_view_title)
        tvSubtitle = toolbar?.findViewById(R.id.text_view_subtitle)
        layoutProgress = findViewById(R.id.layout_progress_bar)
        tvProgress = findViewById(R.id.progress_text)

        isOnCreateProcessed = false
        isGUICreated = false

        createDependencyScope()
        createViewModel()
        initViewModel()
    }

    protected open fun createDependencyScope() {
        scopeSource = ScopeSource.createNew()
    }

    protected open fun createViewModel() {
        this.viewModel = koinScope.get(getViewModelClazz())
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
     * @param event
     * @param data
     */
    protected open fun onBaseEvent(event: BaseEvent) {
        when (event) {
            is BaseEvent.ShowProgress -> setProgressVisibility(event.isVisible)
            is BaseEvent.ShowProgressText -> setProgressText(event.message)
            is BaseEvent.ShowPermissionRequest -> showPermissionRequest(event.request)
            is BaseEvent.TaskStarted -> {
                taskPreExecute(event.titleResId ?: R.string.progress_loading)
            }
            BaseEvent.TaskFinished -> taskPostExecute()
            BaseEvent.ShowMoreInLogs -> showSnackMoreInLogs()
            else -> {}
        }
    }

    private fun showPermissionRequest(params: PermissionRequestParams) {
        // диалог с объяснием зачем нужно разрешение
        AskDialogs.showYesDialog(
            context = this,
            messageResId = when (params.permission) {
                Constants.TetroidPermission.ReadStorage -> R.string.ask_request_read_ext_storage
                Constants.TetroidPermission.WriteStorage -> R.string.ask_request_write_ext_storage
                Constants.TetroidPermission.Camera -> R.string.ask_request_camera
                Constants.TetroidPermission.Termux -> R.string.ask_permission_termux
            },
            onApply = {
                params.requestCallback.invoke()
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
        tvTitle?.text = title
    }

    /**
     * Установка подзаголовка активности.
     * @param title
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
        if (!isGUICreated) {
            // для отображения иконок
            if (menu is MenuBuilder) {
                menu.setOptionalIconsVisible(true)
            }
        }
        // устанавливаем флаг, что стандартные элементы активности созданы
        onUICreated(!isGUICreated)
        isGUICreated = true
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
        setProgressText(progressTextResId)
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
        if (isVisible) {
            tvProgress?.text = text
            layoutProgress?.visibility = View.VISIBLE
        } else {
            layoutProgress?.visibility = View.GONE
        }
        layoutProgress?.isVisible = isVisible
    }

    override fun setProgressText(progressTextResId: Int) {
        setProgressText(getString(progressTextResId))
    }

    override fun setProgressText(progressText: String?) {
        layoutProgress?.visibility = View.VISIBLE
        tvProgress?.text = progressText
    }

    /**
     * Установка видимости пункта меню.
     * @param menuItem
     * @param isVisible
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
     * @param v
     * @param menu
     */
    @SuppressLint("RestrictedApi")
    protected fun setForceShowMenuIcons(v: View?, menu: MenuBuilder?) {
        val menuHelper = MenuPopupHelper(this, menu!!, v!!)
        menuHelper.setForceShowIcon(true)
        menuHelper.show()
    }

    /**
     * Установка подзаголовка активности.
     * @param subtitle
     */
    protected fun setSubtitle(subtitle: String?) {
        tvSubtitle?.visibility = View.VISIBLE
        tvSubtitle?.textSize = 16f
        tvSubtitle?.text = subtitle
    }

    open fun showActivityForResult(cls: Class<*>?, requestCode: Int) {
        val intent = Intent(this, cls)
        startActivityForResult(intent, requestCode)
    }

    /**
     * Публикация сообщений.
     * @param message
     */
    protected fun showMessage(message: Message?) {
        TetroidMessage.show(this, message)
    }

    /**
     * Вывод интерактивного уведомления SnackBar "Подробнее в логах".
     */
    override fun showSnackMoreInLogs() {
        TetroidMessage.showSnackMoreInLogs(this, R.id.layout_coordinator)
    }
}