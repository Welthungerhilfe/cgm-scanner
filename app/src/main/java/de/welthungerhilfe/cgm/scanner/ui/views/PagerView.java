package de.welthungerhilfe.cgm.scanner.ui.views;

import android.content.Context;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class PagerView extends ViewPager {

    private boolean mSwipeEnabled;

    public PagerView(Context context) {
        super(context);
        mSwipeEnabled = true;
    }
    public PagerView(Context context, AttributeSet attrs){
        super(context,attrs);
        mSwipeEnabled = true;
    }
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return mSwipeEnabled && super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mSwipeEnabled && super.onTouchEvent(event);
    }

    public void setSwipeEnabled(boolean enabled) {
        mSwipeEnabled = enabled;
    }
}
