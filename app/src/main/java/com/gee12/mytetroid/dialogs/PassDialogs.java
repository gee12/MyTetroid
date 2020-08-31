package com.gee12.mytetroid.dialogs;

import android.content.Context;
import android.text.InputType;
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

public class PassDialogs {

    /**
     *
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
     *
     * @param context
     * @param node
     * @param passResult
     */
    public static void showPassEnterDialog(Context context, final TetroidNode node, final IPassInputResult passResult) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.title_password_enter));
        builder.setPositiveButton(R.string.answer_ok, null);
        builder.setNegativeButton(R.string.answer_cancel, null);

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton(R.string.answer_ok, (dialog1, which) -> {
            passResult.applyPass(input.getText().toString(), node);
        });
        builder.setNegativeButton(R.string.answer_cancel, (dialog1, which) -> {
            passResult.cancelPass();
        });

        final AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        // проверка на пустоту пароля
        input.addTextChangedListener(new ViewUtils.TextChangedListener(newText -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!(TextUtils.isEmpty(newText)));
        }));

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        });

        dialog.show();
    }

    /**
     *
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
        });

        dialog.show();
    }

    /**
     *
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
        });

        dialog.show();
    }

    /**
     * Диалог установки/ввода ПИН-кода.
     * @param context
     * @param isSetup
     * @param callback
     */
    public static void showPINCodeDialog(Context context, boolean isSetup, IPinInputResult callback) {
        showPINCodeDialog(context, isSetup, false, null, callback);
    }

    public static void showPINCodeDialog(Context context, boolean isSetup, boolean isConfirm, String firstPin,
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
//        IndicatorDots deleteButton = builder.getView().findViewById(R.id.indicator_dots);
//        Button okButton;

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
                        // TODO: здесь нужен эффект дрожания

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
                            Message.show(context, context.getString(R.string.log_pin_confirm_not_match), Toast.LENGTH_SHORT);
                        }
                    } else {
                        // запрашиваем подтверждение ввода
                        showPINCodeDialog(context, true, true, pin, new IPinInputResult() {
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

    public interface IPinInputResult {
        boolean onApply(String code);
        void onCancel();
    }

    public interface IPassInputResult {
        void applyPass(String pass, TetroidNode node);
        void cancelPass();
    }

    public interface IPassChangeResult {
        boolean applyPass(String curPass, String newPass);
    }
}
