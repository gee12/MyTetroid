package com.gee12.mytetroid.views;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.widget.EditText;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.data.TetroidNode;

public class PassInputDialog {

    public interface IPassInputResult {
        void applyPass(String pass, TetroidNode node);
    }

    public static void showPassDialog(Context context, final TetroidNode node, final IPassInputResult applyPass) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.title_pass_input));

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton(R.string.answer_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                applyPass.applyPass(input.getText().toString(), node);
            }
        });
        builder.setNegativeButton(R.string.answer_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
}
