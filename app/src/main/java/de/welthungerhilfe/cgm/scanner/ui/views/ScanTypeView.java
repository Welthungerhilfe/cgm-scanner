package de.welthungerhilfe.cgm.scanner.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.welthungerhilfe.cgm.scanner.R;

public class ScanTypeView extends LinearLayout {

    public interface ScanTypeListener {

        void onScan(int buttonId);
        void onTutorial();
    }

    private LinearLayout mBack;
    private LinearLayout mButtons;
    private ImageView mChildIcon;
    private Button mRetakeButton;
    private Button mScanButton;
    private Button mTutorialButton;
    private TextView mTitle;
    private TextView mText;
    private ScanTypeListener mListener;
    private int mButtonId;

    public ScanTypeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
        getAttributes(context, attrs, defStyle);
    }

    public ScanTypeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
        getAttributes(context, attrs, 0);
    }

    public ScanTypeView(Context context) {
        super(context);
        initView();
    }

    public void finishStep(String issues) {
        mText.setText(issues);
        mChildIcon.setVisibility(View.GONE);
        mScanButton.setVisibility(View.GONE);
        mButtons.setVisibility(View.VISIBLE);
    }

    public void goToNextStep() {
        mScanButton.setVisibility(View.GONE);
    }

    public void setChildIcon(int res) {
        mChildIcon.setImageResource(res);
    }

    public void setTitle(int res) {
        mTitle.setText(res);
    }
    public void setListener(int buttonId, ScanTypeListener listener) {
        mButtonId = buttonId;
        mListener = listener;
    }

    private void initView() {
        View root = inflate(getContext(), R.layout.lv_scan_type, this);
        mBack = root.findViewById(R.id.lytScanStepBack);
        mButtons = root.findViewById(R.id.lytScanAgain);
        mChildIcon = root.findViewById(R.id.imgScanStep);
        mRetakeButton = root.findViewById(R.id.btnRetake);
        mScanButton = root.findViewById(R.id.btnScanStep);
        mTutorialButton = root.findViewById(R.id.btnTutorial);
        mTitle = root.findViewById(R.id.titleScanStep);
        mText = root.findViewById(R.id.txtScanStep);

        mRetakeButton.setOnClickListener(view -> {
            if (mListener != null) {
                mListener.onScan(mButtonId);
            }
        });
        mScanButton.setOnClickListener(view -> {
            if (mListener != null) {
                mListener.onScan(mButtonId);
            }
        });
        mTutorialButton.setOnClickListener(view -> {
            if (mListener != null) {
                mListener.onTutorial();
            }
        });
    }

    private void getAttributes(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ScanType, defStyleAttr, 0);
        mBack.setBackgroundColor(a.getColor(R.styleable.ScanType_scantypecolor, getResources().getColor(R.color.colorGreenLight)));
        mChildIcon.setImageDrawable(a.getDrawable(R.styleable.ScanType_scantypeicon));
        mTitle.setText(a.getString(R.styleable.ScanType_scantypetitle));
        mText.setText(a.getString(R.styleable.ScanType_scantypetext));
        a.recycle();
    }
}
