package com.gee12.mytetroid.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;

public class MainViewPager extends ViewPager /*implements View.OnTouchListener*/ {

    public static int PAGE_MAIN = 0;
    public static int PAGE_FOUND = 1;

//    protected GestureDetectorCompat gestureDetector;
    private boolean isSwipeEnabled;

    public MainViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.isSwipeEnabled = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.isSwipeEnabled) {
            return super.onTouchEvent(event);
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (this.isSwipeEnabled) {
            return super.onInterceptTouchEvent(event);
        }
        return false;
    }

//    /**
//     * Переопределяем обработчик нажатия на экране
//     * для обработки перехода в полноэкранный режим.
//     * @param v
//     * @param event
//     * @return
//     */
//    @Override
//    public boolean onTouch(View v, MotionEvent event) {
//        if (gestureDetector != null)
//            gestureDetector.onTouchEvent(event);
//        return false;
//    }

//    /**
//     *
//     * @param detector
//     */
//    public void setGestureDetector(GestureDetectorCompat detector) {
//        this.gestureDetector = detector;
//    }

    /**
     * On/off swipe
     * @param enabled
     */
    public void setPagingEnabled(boolean enabled) {
        this.isSwipeEnabled = enabled;
    }

    /**
     *
     * @param pageIndex
     */
    public void setCurrent(int pageIndex) {
        setCurrentItem(pageIndex, true);
    }

}
