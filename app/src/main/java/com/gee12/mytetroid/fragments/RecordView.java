package com.gee12.mytetroid.fragments;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.core.view.GestureDetectorCompat;

import com.gee12.mytetroid.LogManager;
import com.gee12.mytetroid.activities.IMainView;
import com.gee12.mytetroid.data.DataManager;
import com.gee12.mytetroid.data.TetroidRecord;
import com.gee12.mytetroid.data.TetroidRecordExt;

public abstract class RecordView extends View implements View.OnTouchListener {//TetroidFragment {

    protected GestureDetectorCompat gestureDetector;
    protected IMainView mainView;
    protected TetroidRecordExt recordExt;

    public RecordView(Context context) {
        super(context);
    }


    public void init(IMainView mainView, GestureDetectorCompat detector) {
        this.mainView = mainView;
        this.gestureDetector = detector;
    }

    public void openRecord(final TetroidRecord record) {
        this.recordExt = readRecord(record);
        if (recordExt == null) {
            return;
        }
        openRecord(recordExt);
        setFullscreen(mainView.isFullscreen());
    }

    /**
     * Чтение записи.
     * @param record Запись
     */
    public static TetroidRecordExt readRecord(final TetroidRecord record) {
        if (record == null)
            return null;
        TetroidRecordExt recordExt = new TetroidRecordExt(record);

        LogManager.addLog("Чтение файла записи: id=" + record.getId());
        String text = DataManager.getRecordHtmlTextDecrypted(record);
        if (text == null) {
            LogManager.addLog("Ошибка чтения записи", Toast.LENGTH_LONG);
            return null;
        }
        recordExt.setTextHtml(text);
        recordExt.setTagsHtml(record.getTagsLinksString());
        return recordExt;
    }


    protected abstract void openRecord(final TetroidRecordExt record);


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
