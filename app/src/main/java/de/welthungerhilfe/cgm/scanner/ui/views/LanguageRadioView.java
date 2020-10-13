package de.welthungerhilfe.cgm.scanner.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatRadioButton;

import de.welthungerhilfe.cgm.scanner.R;

public class LanguageRadioView extends LinearLayout {

    private ImageView mFlag;
    private LinearLayout mLayout;
    private AppCompatRadioButton mRadio;
    private TextView mText;

    public LanguageRadioView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
        getAttributes(context, attrs, defStyle);
    }

    public LanguageRadioView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
        getAttributes(context, attrs, 0);
    }

    public LanguageRadioView(Context context) {
        super(context);
        initView();
    }

    public void setChecked(boolean checked) {
        mRadio.setChecked(checked);
    }

    public void setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener listener) {
        mRadio.setOnCheckedChangeListener(listener);
    }

    private void initView() {
        View root = inflate(getContext(), R.layout.lv_language_radio_view, this);
        mFlag = root.findViewById(R.id.lang_flag);
        mLayout = root.findViewById(R.id.lang_layout);
        mRadio = root.findViewById(R.id.lang_radio);
        mText = root.findViewById(R.id.lang_text);

        mLayout.setOnClickListener(view -> mRadio.setChecked(true));
    }

    private void getAttributes(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LanguageRadioView, defStyleAttr, 0);
        mFlag.setImageDrawable(a.getDrawable(R.styleable.LanguageRadioView_flagImage));
        mText.setText(a.getString(R.styleable.LanguageRadioView_language));
        a.recycle();
    }
}
