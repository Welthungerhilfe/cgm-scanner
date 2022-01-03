package de.welthungerhilfe.cgm.scanner.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.ColorFilter;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.welthungerhilfe.cgm.scanner.R;

public class ScanModeView extends LinearLayout {

    private LinearLayout mBack;
    private ImageView mButtonChecked;
    private ImageView mChildIcon;
    private TextView mText;

    public ScanModeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
        getAttributes(context, attrs, defStyle);
    }

    public ScanModeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
        getAttributes(context, attrs, 0);
    }

    public ScanModeView(Context context) {
        super(context);
        initView();
    }

    public void setActive(boolean on) {
        if (on) {
            mChildIcon.setColorFilter(getResources().getColor(R.color.colorPrimary));
            mButtonChecked.setImageResource(R.drawable.radio_active);
            mText.setTextColor(getResources().getColor(R.color.colorBlack));
        } else {
            mChildIcon.setColorFilter(getResources().getColor(R.color.colorGreyLight));
            mButtonChecked.setImageResource(R.drawable.radio_inactive);
            mText.setTextColor(getResources().getColor(R.color.colorGreyDark));
        }
    }

    private void initView() {
        View root = inflate(getContext(), R.layout.lv_scan_mode, this);
        mBack = root.findViewById(R.id.lytScanMode);
        mButtonChecked = root.findViewById(R.id.imgScanModeCheck);
        mChildIcon = root.findViewById(R.id.imgScanMode);
        mText = root.findViewById(R.id.txtScanMode);
    }

    private void getAttributes(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ScanMode, defStyleAttr, 0);
        mChildIcon.setImageDrawable(a.getDrawable(R.styleable.ScanMode_scanmodeicon));
        mText.setText(a.getString(R.styleable.ScanMode_scanmodetext));
        setActive(a.getBoolean(R.styleable.ScanMode_scanmodeactive, false));
        a.recycle();
    }
}
