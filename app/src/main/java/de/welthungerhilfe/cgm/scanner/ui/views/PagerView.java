/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com>
 * Copyright (c) 2018 Welthungerhilfe Innovation
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
