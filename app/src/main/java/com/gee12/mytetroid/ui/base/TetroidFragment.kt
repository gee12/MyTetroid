package com.gee12.mytetroid.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import com.gee12.mytetroid.R
import com.gee12.mytetroid.ui.TetroidMessage

abstract class TetroidFragment<VM : BaseStorageViewModel> : Fragment, View.OnTouchListener {

    protected var gestureDetector: GestureDetectorCompat? = null

    protected lateinit var viewModel: VM


    abstract fun getTitle(): String

    constructor() {
        createViewModel()
    }

    constructor(detector: GestureDetectorCompat?) {
        gestureDetector = detector
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        createViewModel()
        initViewModel()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, resId: Int): View? {
        createViewModel()
        initViewModel()
        return inflater.inflate(resId, container, false)
    }

    abstract fun createViewModel()

    protected open fun initViewModel() {
        viewModel.logDebug(getString(R.string.log_fragment_opened_mask, javaClass.simpleName))
    }

    /**
     * Переопределяем обработчик нажатия на экране
     * для обработки перехода в полноэкранный режим.
     * @param v
     * @param event
     * @return
     */
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (gestureDetector != null) gestureDetector!!.onTouchEvent(event)
        return false
    }

    /**
     * Вывод интерактивного уведомления SnackBar "Подробнее в логах".
     */
    protected fun showSnackMoreInLogs() {
        TetroidMessage.showSnackMoreInLogs(this, R.id.layout_coordinator)
    }
}