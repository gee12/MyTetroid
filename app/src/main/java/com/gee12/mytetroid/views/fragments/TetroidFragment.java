package com.gee12.mytetroid.views.fragments;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.view.GestureDetectorCompat;
import androidx.fragment.app.Fragment;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.views.Message;

import org.jetbrains.annotations.NotNull;

public abstract class TetroidFragment extends Fragment implements View.OnTouchListener {

    protected Context context;
    protected GestureDetectorCompat mGestureDetector;

    public TetroidFragment() {}

    public TetroidFragment(GestureDetectorCompat detector) {
        this.mGestureDetector = detector;
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        this.context = getContext();
    }

    public abstract String getTitle();

    /**
     *
     * @param detector
     */
    public void setGestureDetector(GestureDetectorCompat detector) {
        this.mGestureDetector = detector;
    }

    /**
     * Переопределяем обработчик нажатия на экране
     * для обработки перехода в полноэкранный режим.
     * @param v
     * @param event
     * @return
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mGestureDetector != null)
            mGestureDetector.onTouchEvent(event);
        return false;
    }

    /**
     * Вывод интерактивного уведомления SnackBar "Подробнее в логах".
     */
    protected void showSnackMoreInLogs() {
        Message.showSnackMoreInLogs(this, R.id.layout_coordinator);
    }
}
