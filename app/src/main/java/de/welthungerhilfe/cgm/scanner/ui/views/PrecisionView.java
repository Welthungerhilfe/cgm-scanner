package de.welthungerhilfe.cgm.scanner.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;

import de.welthungerhilfe.cgm.scanner.R;

public class PrecisionView extends View {
    private Paint targetCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint targetCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int mWidth, mHeight = -1;
    private int mCenterRadius = 60;
    private int mCircleCount = 0;

    private ArrayList<Float> dots = new ArrayList<>();

    public PrecisionView(Context context) {
        super(context);

        init(context, null);
    }

    public PrecisionView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init(context, attrs);
    }

    public PrecisionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs);
    }

    public PrecisionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        targetCenterPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        targetCenterPaint.setColor(context.getColor(R.color.colorRed));

        targetCirclePaint.setStyle(Paint.Style.STROKE);
        targetCirclePaint.setColor(context.getColor(R.color.colorRed));
        targetCirclePaint.setStrokeWidth(10);

        dotPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        dotPaint.setColor(context.getColor(R.color.colorBlack));
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int size = Math.min(getMeasuredWidth(), getMeasuredHeight());
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mWidth = w;
        mHeight = h;

        mCircleCount = (mWidth / 2 - mCenterRadius) / 20;

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawCircle(mWidth / 2, mHeight / 2, mCenterRadius, targetCenterPaint);

        for (int i = 1; i <= mCircleCount; i++) {
            canvas.drawCircle(mWidth / 2, mHeight / 2, mCenterRadius + i * 20, targetCirclePaint);
        }

        for (int i = 0; i < dots.size(); i++) {
            float randomX = mWidth * dots.get(i);
            float randomY = mHeight / 2;

            canvas.drawCircle(randomX, randomY, 10, dotPaint);
        }
    }

    public void addDot(float confidence) {
        dots.add(confidence);

        invalidate();
    }
}
