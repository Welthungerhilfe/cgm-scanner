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
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.balysv.materialripple.MaterialRippleLayout;

import de.welthungerhilfe.cgm.scanner.R;

public class SubmenuButton extends LinearLayout {

    private MaterialRippleLayout mLayout;
    private OnClickListener mListener;
    private TextView mText;

    public SubmenuButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
        getAttributes(context, attrs, defStyle);
    }

    public SubmenuButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
        getAttributes(context, attrs, 0);
    }

    public SubmenuButton(Context context) {
        super(context);
        initView();
    }

    @Override
    public void setOnClickListener(OnClickListener listener) {
        mListener = listener;
        if (mLayout != null) {
            mLayout.setOnClickListener(mListener);
        }
    }

    private void initView() {
        View root = inflate(getContext(), R.layout.lv_submenu_button, this);
        mLayout = root.findViewById(R.id.submenu_layout);
        mLayout.setOnClickListener(mListener);
        mText = root.findViewById(R.id.submenu_text);
    }

    private void getAttributes(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SubmenuButton, defStyleAttr, 0);
        mText.setText(a.getString(R.styleable.SubmenuButton_submenuText));
        a.recycle();
    }
}
