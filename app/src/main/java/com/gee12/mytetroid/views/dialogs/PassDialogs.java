package com.gee12.mytetroid.views.dialogs;

import android.content.Context;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.andrognito.pinlockview.IndicatorDots;
import com.andrognito.pinlockview.PinLockListener;
import com.andrognito.pinlockview.PinLockView;
import com.gee12.htmlwysiwygeditor.Dialogs;
import com.gee12.htmlwysiwygeditor.ViewUtils;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.model.TetroidNode;
import com.gee12.mytetroid.views.Message;
import com.lumyjuwon.richwysiwygeditor.RichEditor.Utils;

import java.util.Locale;

@Deprecated
public class PassDialogs {

    public interface IPassInputResult {
        void applyPass(String pass, TetroidNode node);
        void cancelPass();
    }

    public interface IPassChangeResult {
        boolean applyPass(String curPass, String newPass);
    }

    public interface IPinInputResult {
        boolean onApply(String pin);
        void onCancel();
    }

    public interface IPinLengthInputResult {
        void onApply(int length);
        void onCancel();
    }

    /**
     * Диалог установки/ввода пароля.
     * @param context
     * @param node
     * @param isNewPass
     * @param passResult
     */
    public static void showPassEnterDialog(Context context, final TetroidNode node, boolean isNewPass, final IPassInputResult passResult) {
        if (isNewPass) {
            showPassSetupDialog(context, node, passResult);
        } else {
            showPassEnterDialog(context, node, passResult);
        }
    }

    /**
     * TODO: Перенести в DialogFragment.
     * Диалог ввода пароля.
     * @param context
     * @param node
     * @param passResult
     */
    public static void showPassEnterDialog(Context context, final TetroidNode node, final IPassInputResult passResult) {
        Dialogs.AskDialogBuilder builder = Dialogs.AskDialogBuilder.create(context, R.layout.dialog_pass_enter);
        builder.setTitle(context.getString(R.string.title_password_enter));
        builder.setPositiveButton(R.string.answer_ok, null);
        builder.setNegativeButton(R.string.answer_cancel, null);

        final EditText tvPass = builder.getView().findViewById(R.id.edit_text_pass);
        builder.setPositiveButton(R.string.answer_ok, (dialog1, which) -> passResult.applyPass(tvPass.getText().toString(), node));
        builder.setNegativeButton(R.string.answer_cancel, (dialog1, which) -> passResult.cancelPass());

        final AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        // проверка на пустоту пароля
        tvPass.addTextChangedListener(new ViewUtils.TextChangedListener(newText -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!(TextUtils.isEmpty(newText)));
        }));

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

            com.gee12.mytetroid.utils.ViewUtils.showKeyboard(context, tvPass, false);
        });

        dialog.show();
    }

    /**
     * TODO: Перенести в DialogFragment.
     * Диалог установки пароля.
     * @param context
     * @param node
     * @param passResult
     */
    public static void showPassSetupDialog(Context context, final TetroidNode node, final IPassInputResult passResult) {
        Dialogs.AskDialogBuilder builder = Dialogs.AskDialogBuilder.create(context, R.layout.dialog_pass_setup);
        builder.setTitle(context.getString(R.string.title_password_set));
        builder.setPositiveButton(R.string.answer_ok, null);
        builder.setNegativeButton(R.string.answer_cancel, null);

        final EditText tvPass = builder.getView().findViewById(R.id.edit_text_pass);
        EditText tvConfirmPass = builder.getView().findViewById(R.id.edit_text_confirm_pass);

        final AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        // проверка на пустоту паролей
        ViewUtils.TextChangedListener listener = new ViewUtils.TextChangedListener(newText -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!(TextUtils.isEmpty(newText)
                    || tvPass.getText().length() == 0
                    || tvConfirmPass.getText().length() == 0));
        });
        tvPass.addTextChangedListener(listener);
        tvConfirmPass.addTextChangedListener(listener);

        dialog.setOnShowListener(dialogInterface -> {
            Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            okButton.setEnabled(false);
            okButton.setOnClickListener(view -> {
                String pass = tvPass.getText().toString();
                String confirmPass = tvConfirmPass.getText().toString();
                // проверка совпадения паролей
                if (!pass.contentEquals(confirmPass)) {
                    Toast.makeText(context, context.getString(R.string.log_pass_confirm_not_match), Toast.LENGTH_SHORT).show();
                    return;
                }
                passResult.applyPass(pass, node);
                dialog.dismiss();
            });

            com.gee12.mytetroid.utils.ViewUtils.showKeyboard(context, tvPass, false);
        });

        dialog.show();
    }

    /**
     * TODO: Перенести в DialogFragment.
     * Диалог изменения пароля.
     * @param context
     * @param passResult
     */
    public static void showPassChangeDialog(Context context, final IPassChangeResult passResult) {
        Dialogs.AskDialogBuilder builder = Dialogs.AskDialogBuilder.create(context, R.layout.dialog_pass_change);
        builder.setTitle(context.getString(R.string.title_password_change));
        builder.setPositiveButton(R.string.answer_ok, null);
        builder.setNegativeButton(R.string.answer_cancel, null);

        EditText tvCurPass = builder.getView().findViewById(R.id.edit_text_cur_pass);
        EditText tvNewPass = builder.getView().findViewById(R.id.edit_text_new_pass);
        EditText tvConfirmPass = builder.getView().findViewById(R.id.edit_text_confirm_pass);

        final AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        // проверка на пустоту паролей
        ViewUtils.TextChangedListener listener = new ViewUtils.TextChangedListener(newText -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!(TextUtils.isEmpty(newText)
                || tvCurPass.getText().length() == 0
                || tvNewPass.getText().length() == 0
                || tvConfirmPass.getText().length() == 0));
        });
        tvCurPass.addTextChangedListener(listener);
        tvNewPass.addTextChangedListener(listener);
        tvConfirmPass.addTextChangedListener(listener);

        dialog.setOnShowListener(dialogInterface -> {
            Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            okButton.setEnabled(false);
            okButton.setOnClickListener(view -> {
                String curPass = tvCurPass.getText().toString();
                String newPass = tvNewPass.getText().toString();
                String confirmPass = tvConfirmPass.getText().toString();
                // проверка совпадения паролей
                if (!newPass.contentEquals(confirmPass)) {
                    Toast.makeText(context, context.getString(R.string.log_pass_confirm_not_match), Toast.LENGTH_SHORT).show();
                    return;
                }
                // проверка текущего пароля
                if (passResult.applyPass(curPass, newPass)) {
                    dialog.dismiss();
                }
            });

            com.gee12.mytetroid.utils.ViewUtils.showKeyboard(context, tvCurPass, false);
        });

        dialog.show();
    }

    /**
     * TODO: Перенести в DialogFragment.
     * Диалог ввода длины ПИН-кода.
     * Значение должно быть в диапазоне 4-8.
     * @param context
     * @param callback
     */
    public static void showPinCodeLengthDialog(Context context, int curSize, IPinLengthInputResult callback) {
        Dialogs.AskDialogBuilder builder = Dialogs.AskDialogBuilder.create(context, R.layout.dialog_pin_code_length);
        builder.setTitle(context.getString(R.string.title_enter_pin_code_length));

        EditText etSize = builder.getView().findViewById(com.lumyjuwon.richwysiwygeditor.R.id.edit_text_size);
        if (curSize >= 4 && curSize <= 8) {
            etSize.setText(String.format(Locale.getDefault(), "%d", curSize));
        }

        builder.setPositiveButton(com.lumyjuwon.richwysiwygeditor.R.string.answer_ok, (dialog1, which) -> {
            String s = etSize.getText().toString();
            Integer size = Utils.parseInt(s);
            if (size != null) {
                callback.onApply(size);
            } else {
                Message.show(context, context.getString(com.lumyjuwon.richwysiwygeditor.R.string.invalid_number) + s,
                        Toast.LENGTH_SHORT);
            }
        });
        builder.setNegativeButton(com.lumyjuwon.richwysiwygeditor.R.string.answer_cancel, (dialog, which) -> callback.onCancel());

        final AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);

        dialog.setOnShowListener(dialog1 -> {
            // получаем okButton уже после вызова show()
            final Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (TextUtils.isEmpty(etSize.getText().toString())) {
                okButton.setEnabled(false);
            }
            etSize.setSelection(etSize.getText().length());
        });

        dialog.show();

        // получаем okButton тут отдельно после вызова show()
        final Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        etSize.addTextChangedListener(new ViewUtils.TextChangedListener(newText -> {
            Integer size = Utils.parseInt(etSize.getText().toString());
            okButton.setEnabled(size != null && size >= 4 && size <= 8);
        }));
    }

    /**
     * Диалог установки/ввода ПИН-кода.
     * @param context
     * @param isSetup
     * @param callback
     */
    public static void showPINCodeDialog(Context context, int length, boolean isSetup, IPinInputResult callback) {
        showPINCodeDialog(context, length, isSetup, false, null, callback);
    }

    /**
     * TODO: Перенести в DialogFragment.
     */
    public static void showPINCodeDialog(Context context, int length, boolean isSetup, boolean isConfirm, String firstPin,
                                         IPinInputResult callback) {
        Dialogs.AskDialogBuilder builder = Dialogs.AskDialogBuilder.create(context, R.layout.dialog_pin_code);
        String title = context.getString((isConfirm) ? R.string.title_pin_confirm
                : (isSetup) ? R.string.title_pin_set
                : R.string.title_pin_enter);
        builder.setTitle(title);
        if (isSetup) {
            builder.setPositiveButton(R.string.answer_ok, null);
        }
        builder.setNegativeButton(R.string.answer_cancel, (dialog1, which) -> callback.onCancel());

        PinLockView pinLockView = builder.getView().findViewById(R.id.pin_lock_view);
        IndicatorDots indicatorDots = builder.getView().findViewById(R.id.indicator_dots);
        pinLockView.attachIndicatorDots(indicatorDots);
        pinLockView.setPinLength(length);

        final AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);

        PinLockListener mPinLockListener = new PinLockListener() {
            @Override
            public void onComplete(String pin) {
                if (isSetup) {
                    pinLockView.setTag(pin);
                    setEnabledOk(true);
                } else {
                    if (callback.onApply(pin)) {
                        dialog.dismiss();
                    } else {
                        // запускаем анимацию дрожания
                        pinLockView.shake();
                        // очищаем введенные значения
                        pinLockView.resetPinLockView();
                    }
                }
            }

            @Override
            public void onEmpty() {
                setEnabledOk(false);
            }

            @Override
            public void onPinChange(int pinLength, String intermediatePin) {
                setEnabledOk(false);
            }

            private void setEnabledOk(boolean isEnabled) {
                if (isSetup) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(isEnabled);
                }
            }
        };
        pinLockView.setPinLockListener(mPinLockListener);

        dialog.setOnShowListener(dialogInterface -> {
            if (isSetup) {
                Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                okButton.setEnabled(false);
                okButton.setOnClickListener(view -> {
                    String pin = (String) pinLockView.getTag();
                    if (isConfirm) {
                        // если это запрос подтверждения ввода, то сравниванием коды
                        if (firstPin.equals(pin)) {
                            callback.onApply(firstPin);
                            dialog.dismiss();
                        } else {
                            // запускаем анимацию дрожания
                            pinLockView.shake();
                            // очищаем введенные значения
                            pinLockView.resetPinLockView();
                            Message.show(context, context.getString(R.string.log_pin_confirm_not_match), Toast.LENGTH_SHORT);
                        }
                    } else {
                        // запрашиваем подтверждение ввода
                        showPINCodeDialog(context, length, true, true, pin, new IPinInputResult() {
                            @Override
                            public boolean onApply(String pass) {
                                callback.onApply(pin);
                                dialog.dismiss();
                                return true;
                            }

                            @Override
                            public void onCancel() {
                                callback.onCancel();
                                dialog.dismiss();
                            }
                        });
                    }
                });
            }
        });

        dialog.show();
    }

    public static void showEmptyPassCheckingFieldDialog(Context context, String fieldName, final Dialogs.IApplyCancelResult callback) {
        Dialogs.showAlertDialog(context, String.format(context.getString(R.string.log_empty_middle_hash_check_data_field), fieldName), callback);
    }
}
