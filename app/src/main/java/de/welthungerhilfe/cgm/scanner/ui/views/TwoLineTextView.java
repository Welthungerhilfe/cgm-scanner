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
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;

import com.balysv.materialripple.MaterialRippleLayout;

import de.welthungerhilfe.cgm.scanner.R;

public class TwoLineTextView extends LinearLayout {

    private OnClickListener mListener;

    private MaterialRippleLayout mLayout;
    private ImageView mCheckIcon;
    private ImageView mSubmenu;
    private TextView mTitleLine;
    private TextView mDescriptionLine;
    private TextView mValue;
    private View mSeparator;
    private SwitchCompat mSwitch;

    public TwoLineTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
        getAttributes(context, attrs, defStyle);
    }

    public TwoLineTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
        getAttributes(context, attrs, 0);
    }

    public TwoLineTextView(Context context) {
        super(context);
        initView();
    }

    private void initView() {
        View root = inflate(getContext(), R.layout.lv_two_line_view, this);
        mTitleLine = root.findViewById(R.id.twoLineFirst);
        mDescriptionLine = root.findViewById(R.id.twoLineSecond);
        mCheckIcon = root.findViewById(R.id.checkIcon);
        mSeparator = root.findViewById(R.id.separator);
        mSubmenu = root.findViewById(R.id.submenu_button);
        mSwitch = root.findViewById(R.id.toggle_switch);
        mValue = root.findViewById(R.id.text_value);

        mLayout = root.findViewById(R.id.item_layout);
        setOnClickListener(mListener);
    }

    private void getAttributes(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TwoLineTextView, defStyleAttr, 0);

        //set texts
        setText(1, a.getString(R.styleable.TwoLineTextView_titleText));
        setText(2, a.getString(R.styleable.TwoLineTextView_descriptionText));
        mTitleLine.setTextColor(a.getColor(R.styleable.TwoLineTextView_titleTextColor, Color.BLACK));
        float titleSize = a.getDimensionPixelSize(R.styleable.TwoLineTextView_titleTextSize, 0);
        if (titleSize > 0) {
            mTitleLine.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleSize);
        }

        //set objects visibility
        boolean check = a.getBoolean(R.styleable.TwoLineTextView_checkVisible, false);
        mCheckIcon.setVisibility(check ? View.INVISIBLE : View.GONE);
        boolean separator = a.getBoolean(R.styleable.TwoLineTextView_separatorVisible, false);
        mSeparator.setVisibility(separator ? View.VISIBLE : View.GONE);
        boolean submenu = a.getBoolean(R.styleable.TwoLineTextView_submenuVisible, false);
        mSubmenu.setVisibility(submenu ? View.VISIBLE : View.GONE);
        boolean toggle = a.getBoolean(R.styleable.TwoLineTextView_toggleVisible, false);
        mSwitch.setVisibility(toggle ? View.VISIBLE : View.GONE);
        boolean value = a.getBoolean(R.styleable.TwoLineTextView_valueVisible, false);
        mValue.setVisibility(value ? View.VISIBLE : View.GONE);
        if (value) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mLayout.getLayoutParams();
            lp.topMargin = 0;
            mLayout.setLayoutParams(lp);
        }

        a.recycle();
    }

    public void setChecked(boolean checked) {
        mSwitch.setChecked(checked);
    }

    public void setSelected(boolean selected) {
        mCheckIcon.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
    }

    public void setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener listener) {
        mSwitch.setOnCheckedChangeListener(listener);
    }

    @Override
    public void setOnClickListener(OnClickListener listener) {
        mListener = listener;
        if (mLayout != null) {
            mLayout.setOnClickListener(mListener);
            mTitleLine.setOnClickListener(mListener);
            mDescriptionLine.setOnClickListener(mListener);
            mCheckIcon.setOnClickListener(mListener);
        }
    }

    public void setText(int line, String text) {
        TextView view;
        switch (line) {
            case 1:
                view = mTitleLine;
                break;
            case 2:
                view = mDescriptionLine;
                break;
            default:
                return;
        }

        if (text != null) {
            view.setText(text);
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    public void setValue(String value) {
        mValue.setText(value);
    }
}
