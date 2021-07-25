package com.gee12.mytetroid.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;

public class MainViewPager extends ViewPager {

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
