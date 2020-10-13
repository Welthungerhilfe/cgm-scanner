package de.welthungerhilfe.cgm.scanner.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;

import de.welthungerhilfe.cgm.scanner.R;

public class ToggleView extends LinearLayout {

    private SwitchCompat mSwitch;
    private TextView mText;

    public ToggleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
        getAttributes(context, attrs, defStyle);
    }

    public ToggleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
        getAttributes(context, attrs, 0);
    }

    public ToggleView(Context context) {
        super(context);
        initView();
    }

    public void setChecked(boolean checked) {
        mSwitch.setChecked(checked);
    }

    public void setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener listener) {
        mSwitch.setOnCheckedChangeListener(listener);
    }

    private void initView() {
        View root = inflate(getContext(), R.layout.lv_toggle_view, this);
        mSwitch = root.findViewById(R.id.toggle_switch);
        mText = root.findViewById(R.id.toggle_text);
    }

    private void getAttributes(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ToggleView, defStyleAttr, 0);
        mText.setText(a.getString(R.styleable.ToggleView_toggleText));
        a.recycle();
    }
}
