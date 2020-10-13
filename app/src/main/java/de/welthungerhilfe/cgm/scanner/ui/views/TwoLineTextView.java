package de.welthungerhilfe.cgm.scanner.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.welthungerhilfe.cgm.scanner.R;

public class TwoLineTextView extends LinearLayout {

    private TextView mFirstLine;
    private TextView mSecondLine;

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
        mFirstLine = root.findViewById(R.id.twoLineFirst);
        mSecondLine = root.findViewById(R.id.twoLineSecond);
    }

    private void getAttributes(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TwoLineTextView, defStyleAttr, 0);
        mFirstLine.setText(a.getString(R.styleable.TwoLineTextView_firstText));
        mSecondLine.setText(a.getString(R.styleable.TwoLineTextView_secondText));
        a.recycle();
    }

    public void setText(int line, String text) {
        switch (line) {
            case 1:
                mFirstLine.setText(text);
                break;
            case 2:
                mSecondLine.setText(text);
                break;
        }
    }
}
