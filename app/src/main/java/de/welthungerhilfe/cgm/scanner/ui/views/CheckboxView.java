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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.welthungerhilfe.cgm.scanner.R;

public class CheckboxView extends LinearLayout {

    private CheckBox mSwitch;
    private TextView mText;

    public CheckboxView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
        getAttributes(context, attrs, defStyle);
    }

    public CheckboxView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
        getAttributes(context, attrs, 0);
    }

    public CheckboxView(Context context) {
        super(context);
        initView();
    }

    public boolean isChecked() {
        return mSwitch.isChecked();
    }

    public void setChecked(boolean checked) {
        mSwitch.setChecked(checked);
    }

    public void setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener listener) {
        mSwitch.setOnCheckedChangeListener(listener);
    }

    public void setText(String text) {
        mText.setText(text);
    }

    private void initView() {
        View root = inflate(getContext(), R.layout.lv_checkbox_view, this);
        mSwitch = root.findViewById(R.id.checkbox_switch);
        mText = root.findViewById(R.id.checkbox_text);
    }

    private void getAttributes(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CheckboxView, defStyleAttr, 0);
        mText.setText(a.getString(R.styleable.CheckboxView_checkboxText));
        a.recycle();
    }
}
