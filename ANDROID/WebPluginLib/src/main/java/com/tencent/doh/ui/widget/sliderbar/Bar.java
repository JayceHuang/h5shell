package com.tencent.doh.ui.widget.sliderbar;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextUtils;

/**
 * Created by benpeng on 2016/8/25.
 */
public class Bar {

    private Paint mBarPaint;
    private Paint mTextPaint;

    private final float mLeftX;
    private final float mRightX;
    private final float mY;
    private final float mPadding;

    private int mSegments;
    private float mTickDistance;
    private final float mTickHeight;
    private final float mTickStartY;
    private final float mTickEndY;
    public Bar(float x, float y, float width, int tickCount, float tickHeight,
               float barWidth, int barColor,int textColor, int textSize, int padding) {

        mLeftX = x;
        mRightX = x + width;
        mY = y;
        mPadding = padding;

        mSegments = tickCount - 1;
        mTickDistance = width / mSegments;
        mTickHeight = tickHeight;
        mTickStartY = mY - mTickHeight / 2f;
        mTickEndY = mY + mTickHeight / 2f;

        mBarPaint = new Paint();
        mBarPaint.setColor(barColor);
        mBarPaint.setStrokeWidth(barWidth);
        mBarPaint.setAntiAlias(true);

        mTextPaint = new Paint();
        mTextPaint.setColor(textColor);
        mTextPaint.setTextSize(textSize);
        mTextPaint.setAntiAlias(true);
    }



    public void draw(Canvas canvas) {
        drawLine(canvas);
        drawTicks(canvas);
    }

    public float getLeftX() {
        return mLeftX;
    }

    public float getRightX() {
        return mRightX;
    }

    public float getNearestTickCoordinate(Thumb thumb) {
        final int nearestTickIndex = getNearestTickIndex(thumb);
        final float nearestTickCoordinate = mLeftX + (nearestTickIndex * mTickDistance);
        return nearestTickCoordinate;
    }

    public float getCoordinateByIndex(int index){
        if (indexOutOfRange(index))
            throw new IllegalArgumentException(
                    "A thumb index is out of bounds. Check that it is between 0 and mTickCount - 1");
        return  mLeftX + index * mTickDistance;
    }
    private boolean indexOutOfRange(int thumbIndex) {
        return (thumbIndex < 0 || thumbIndex > mSegments);
    }
    public int getNearestTickIndex(Thumb thumb) {
        return getNearestTickIndex(thumb.getX());
    }

    public int getNearestTickIndex(float x) {
        return (int) ((x - mLeftX + mTickDistance / 2f) / mTickDistance);
    }

    private void drawLine(Canvas canvas) {
        canvas.drawLine(mLeftX, mY, mRightX, mY, mBarPaint);
    }

    private void drawTicks(Canvas canvas) {
        for (int i = 0; i <= mSegments; i++) {
            final float x = i * mTickDistance + mLeftX;
            canvas.drawLine(x, mTickStartY, x, mTickEndY, mBarPaint);
            String text = 0 == i ? "小" : mSegments == i ? "大" : "";
            if(!TextUtils.isEmpty(text)) {
                canvas.drawText(text, x - getTextWidth(text) / 2, mTickStartY - mPadding, mTextPaint);
            }
        }
    }

    float getTextWidth(String text) {
        return mTextPaint.measureText(text);
    }

    public void destroyResources() {
        if(null != mBarPaint) {
            mBarPaint = null;
        }
        if(null != mTextPaint) {
            mTextPaint = null;
        }
    }
}