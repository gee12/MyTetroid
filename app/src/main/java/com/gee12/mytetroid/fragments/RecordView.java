package com.gee12.mytetroid.fragments;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.view.GestureDetectorCompat;

import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.R;
import com.gee12.mytetroid.activities.IMainView;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.TetroidRecord;
import com.gee12.mytetroid.data.TetroidRecordExt;

public abstract class RecordView extends FrameLayout implements View.OnTouchListener {//TetroidFragment {

    protected GestureDetectorCompat gestureDetector;
    protected IMainView mainView;
    protected TetroidRecordExt recordExt;

    public RecordView(Context context) {
        super(context);
        initView();
    }

    public RecordView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public RecordView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public RecordView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView();
    }

    private void initView() {
        View view = inflate(getContext(), getViewId(), null);
        addView(view);
    }

    @LayoutRes
    protected abstract int getViewId();


    public void init(IMainView mainView, GestureDetectorCompat detector) {
        this.mainView = mainView;
        this.gestureDetector = detector;
    }

    public void openRecord(final TetroidRecord record) {
        this.recordExt = readRecord(getContext(), record);
        if (recordExt == null) {
            return;
        }
        openRecord();
        setFullscreen(mainView.isFullscreen());
    }

    /**
     * Чтение записи.
     * @param record Запись
     */
    public static TetroidRecordExt readRecord(Context context, final TetroidRecord record) {
        if (record == null)
            return null;
        TetroidRecordExt recordExt = new TetroidRecordExt(record);

        LogManager.addLog(context.getString(R.string.record_file_reading) + record.getId());
        String text = DataManager.getRecordHtmlTextDecrypted(record);
        if (text == null) {
            LogManager.addLog(context.getString(R.string.error_record_reading), Toast.LENGTH_LONG);
            return null;
        }
        recordExt.setTextHtml(text);
        recordExt.setTagsHtml(record.getTagsLinksString());
        return recordExt;
    }


    protected abstract void openRecord();//final TetroidRecordExt record);


    public String getRecordHtml() {
        return null;
    }


    public void setMainView(IMainView mainView) {
        this.mainView = mainView;
    }

    /**
     *
     * @param isFullscreen
     */
    public abstract void setFullscreen(boolean isFullscreen);

    /**
     *
     * @param detector
     */
    public void setGestureDetector(GestureDetectorCompat detector) {
        this.gestureDetector = detector;
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
        if (gestureDetector != null)
            gestureDetector.onTouchEvent(event);
        return false;
    }
}
