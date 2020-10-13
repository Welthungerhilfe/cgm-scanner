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
        mLayout.setOnClickListener(listener);
    }

    private void initView() {
        View root = inflate(getContext(), R.layout.lv_submenu_button, this);
        mLayout = root.findViewById(R.id.submenu_layout);
        mText = root.findViewById(R.id.submenu_text);
    }

    private void getAttributes(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SubmenuButton, defStyleAttr, 0);
        mText.setText(a.getString(R.styleable.SubmenuButton_submenuText));
        a.recycle();
    }
}
