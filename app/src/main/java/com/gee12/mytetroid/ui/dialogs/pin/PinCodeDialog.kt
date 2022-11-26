package com.gee12.mytetroid.ui.dialogs.pin

import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import com.andrognito.pinlockview.IndicatorDots
import com.andrognito.pinlockview.PinLockListener
import com.andrognito.pinlockview.PinLockView
import com.gee12.mytetroid.R
import com.gee12.mytetroid.viewmodels.StorageViewModel
import com.gee12.mytetroid.ui.dialogs.TetroidDialogFragment
import org.koin.java.KoinJavaComponent.get

class PinCodeDialog(
    private val length: Int,
    private val isSetup: Boolean,
    private val isConfirm: Boolean,
    private val firstPin: String?,
    private val callback: IPinInputResult
) : TetroidDialogFragment<StorageViewModel>() {

    interface IPinInputResult {
        fun onApply(pin: String): Boolean
        fun onCancel()
    }

    override fun getRequiredTag() = TAG

    override fun isPossibleToShow() = true

    override fun getLayoutResourceId() = R.layout.dialog_pin_code

    override fun createViewModel() {
        this.viewModel = get(StorageViewModel::class.java)
    }

    override fun onDialogCreated(dialog: AlertDialog, view: View) {
        dialog.setCanceledOnTouchOutside(false)
        val title = getString(
            when {
                isConfirm -> R.string.title_pin_confirm
                isSetup -> R.string.title_pin_set
                else -> R.string.title_pin_enter
            }
        )
        setTitle(title)

        val pinLockView: PinLockView = dialogView.findViewById(R.id.pin_lock_view)
        val indicatorDots: IndicatorDots = dialogView.findViewById(R.id.indicator_dots)
        pinLockView.attachIndicatorDots(indicatorDots)
        pinLockView.pinLength = length

        val pinLockListener = object : PinLockListener {
            override fun onComplete(pin: String) {
                if (isSetup) {
                    pinLockView.tag = pin
                    setEnabledOk(true)
                } else {
                    if (callback.onApply(pin)) {
                        dialog.dismiss()
                    } else {
                        // запускаем анимацию дрожания
                        pinLockView.shake()
                        // очищаем введенные значения
                        pinLockView.resetPinLockView()
                    }
                }
            }

            override fun onEmpty() {
                setEnabledOk(false)
            }

            override fun onPinChange(pinLength: Int, intermediatePin: String) {
                setEnabledOk(false)
            }

            private fun setEnabledOk(isEnabled: Boolean) {
                if (isSetup) {
                    getPositiveButton()?.isEnabled = isEnabled
                }
            }
        }
        pinLockView.setPinLockListener(pinLockListener)

        if (isSetup) {
            setPositiveButton(R.string.answer_ok, false) { _, _ ->
                val pin = pinLockView.tag as String
                if (isConfirm) {
                    // если это запрос подтверждения ввода, то сравниванием коды
                    if (firstPin == pin) {
                        callback.onApply(firstPin)
                        dialog.dismiss()
                    } else {
                        // запускаем анимацию дрожания
                        pinLockView.shake()
                        // очищаем введенные значения
                        pinLockView.resetPinLockView()
                        viewModel.showMessage(R.string.log_pin_confirm_not_match)
                    }
                } else {
                    // запрашиваем подтверждение ввода
                    PinCodeDialog(
                        length = length,
                        isSetup = true,
                        isConfirm = true,
                        firstPin = pin,
                        callback = object : IPinInputResult {
                            override fun onApply(pin: String): Boolean {
                                callback.onApply(pin)
                                dialog.dismiss()
                                return true
                            }

                            override fun onCancel() {
                                callback.onCancel()
                                dialog.dismiss()
                            }
                        }
                    ).showIfPossible(parentFragmentManager)
                }
            }
        }
        setNegativeButton(R.string.answer_cancel) { _, _ -> callback.onCancel() }
    }

    override fun onDialogShowed(dialog: AlertDialog, view: View) {
        if (isSetup) {
            getPositiveButton()?.isEnabled = false
        }
    }

    companion object {
        const val TAG = "PINCodeDialog"

        /**
         * Диалог установки/ввода ПИН-кода.
         * @param isSetup
         * @param callback
         */
        fun showDialog(
            length: Int,
            isSetup: Boolean,
            fragmentManager: FragmentManager,
            callback: IPinInputResult
        ) {
            PinCodeDialog(length, isSetup, false, null, callback)
                .showIfPossible(fragmentManager)
        }

    }
}