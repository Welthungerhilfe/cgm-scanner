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
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.welthungerhilfe.cgm.scanner.R;

public class TestView extends LinearLayout {

    public enum TestState { UNKNOWN, INITIALIZE, TESTING, ERROR, SUCCESS };

    private TestState mState;
    private TextView mResult;
    private TextView mTitle;

    public TestView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    public TestView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public TestView(Context context) {
        super(context);
        initView();
    }

    public boolean isFinished() {
        return mState == TestState.SUCCESS || mState == TestState.ERROR;
    }

    public void setResult(String text) {
        mResult.setText(text);
    }

    public void setState(TestState state) {
        mState = state;
        switch (state) {
            case INITIALIZE:
                mResult.setTextColor(Color.GRAY);
                break;
            case TESTING:
                mResult.setTextColor(Color.rgb(230, 122, 58));
                break;
            case ERROR:
                mResult.setTextColor(Color.rgb(212, 53, 62));
                break;
            case SUCCESS:
                mResult.setTextColor(Color.rgb(55, 129, 69));
                break;
        }
    }

    public void setTitle(String text) {
        mTitle.setText(text);
    }

    private void initView() {
        View root = inflate(getContext(), R.layout.lv_test_view, this);
        mState = TestState.UNKNOWN;
        mResult = root.findViewById(R.id.test_result);
        mTitle = root.findViewById(R.id.test_title);
    }
}
