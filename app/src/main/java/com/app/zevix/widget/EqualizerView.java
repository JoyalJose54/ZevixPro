package com.app.zevix.widget;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;

import java.util.ArrayList;

public class EqualizerView extends View {

    public interface OnEqualizerChangeListener {
        void onProgressChanged(EqualizerView view, int[] progress, int bandIndex);
        void onStartTrackingTouch(EqualizerView view, int[] progress, int bandIndex);
        void onStopTrackingTouch(EqualizerView view, int[] progress, int bandIndex);
    }

    private Paint paint;
    private int bgLineColor;
    private int bgLineWidth;
    private int progressLineColor;
    private int progressLineWidth;
    private int connectLineColor;
    private int connectLineWidth;
    private int thumbStrokeColor;
    private int thumbFillColor;
    private int thumbRadius;
    private int thumbStrokeWidth;
    private int textColor;
    private int textSize;
    private int textVerticalMargin;
    private int topTextMargin;
    private int bandCount = 5;
    private int maxProgress = 3000;
    private int[] progress;
    private String[] freqLabels;

    private int viewHeight;
    private int viewWidth;
    private float activeHeight;
    private float baselineY;

    private int activeTouchIndex = -1;
    private float touchLastY;
    private float touchLastX;
    private int touchStartVal;

    private OnEqualizerChangeListener listener;

    public EqualizerView(Context context) {
        this(context, null);
    }

    public EqualizerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EqualizerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        bgLineColor = Color.parseColor("#444444");
        bgLineWidth = dpToPx(3);
        progressLineColor = Color.parseColor("#25d366");
        progressLineWidth = dpToPx(3);
        connectLineColor = Color.parseColor("#99ffffff");
        connectLineWidth = dpToPx(1.5f);
        thumbStrokeColor = Color.parseColor("#ffffff");
        thumbFillColor = Color.parseColor("#ffffff");
        thumbRadius = dpToPx(7);
        thumbStrokeWidth = dpToPx(2);

        textColor = Color.parseColor("#99ffffff");
        textSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, dm);
        textVerticalMargin = dpToPx(14);
        topTextMargin = dpToPx(14);

        freqLabels = new String[]{"60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz"};
        progress = new int[]{1500, 1500, 1500, 1500, 1500};
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    public void setOnEqualizerChangeListener(OnEqualizerChangeListener listener) {
        this.listener = listener;
    }

    public void setFreqLabels(String[] labels) {
        if (labels != null && labels.length > 0) {
            this.freqLabels = labels;
            this.bandCount = labels.length;
            if (this.progress == null || this.progress.length != bandCount) {
                this.progress = new int[bandCount];
                for (int i = 0; i < bandCount; i++) this.progress[i] = maxProgress / 2;
            }
            invalidate();
        }
    }

    public int[] getProgress() {
        return progress;
    }

    public void setProgress(int[] newProgress) {
        if (newProgress == null) return;
        if (progress == null || progress.length != newProgress.length) {
            progress = new int[newProgress.length];
        }
        for (int i = 0; i < newProgress.length; i++) {
            progress[i] = Math.max(0, Math.min(maxProgress, newProgress[i]));
        }
        invalidate();
    }

    public void setProgressWithAnim(int[] targetProgress) {
        if (targetProgress == null) return;
        if (progress == null || progress.length != targetProgress.length) {
            setProgress(targetProgress);
            return;
        }

        ArrayList<Animator> animators = new ArrayList<>();
        for (int i = 0; i < targetProgress.length; i++) {
            final int band = i;
            ValueAnimator anim = ValueAnimator.ofInt(progress[band], targetProgress[band]);
            anim.addUpdateListener(animation -> {
                progress[band] = (int) animation.getAnimatedValue();
                postInvalidate();
            });
            animators.add(anim);
        }
        AnimatorSet set = new AnimatorSet();
        set.setDuration(350);
        set.playTogether(animators);
        set.start();
    }

    public void setBandProgress(int bandIndex, int val) {
        if (progress == null || bandIndex < 0 || bandIndex >= progress.length) return;
        progress[bandIndex] = Math.max(0, Math.min(maxProgress, val));
        invalidate();
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public void setMaxProgress(int max) {
        this.maxProgress = max;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        if (hMode == MeasureSpec.AT_MOST || hMode == MeasureSpec.UNSPECIFIED) {
            h = dpToPx(190);
        }
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.viewWidth = w;
        this.viewHeight = h;

        int padTop = getPaddingTop();
        int padBottom = getPaddingBottom();

        // Top dB text takes ~20dp, bottom freq text takes ~20dp
        float topSpacing = topTextMargin + textSize + thumbRadius;
        float bottomSpacing = textVerticalMargin + textSize + thumbRadius;

        this.activeHeight = h - padTop - padBottom - topSpacing - bottomSpacing;
        this.baselineY = h - padBottom - bottomSpacing;
    }

    private float getBandCenterX(int index) {
        if (bandCount <= 1) return viewWidth / 2f;
        float usableWidth = viewWidth - getPaddingLeft() - getPaddingRight() - (thumbRadius * 2);
        float step = usableWidth / (bandCount - 1);
        return getPaddingLeft() + thumbRadius + (index * step);
    }

    private PointF[] calculateThumbPoints() {
        PointF[] points = new PointF[bandCount];
        for (int i = 0; i < bandCount; i++) {
            float x = getBandCenterX(i);
            float ratio = (float) progress[i] / (float) maxProgress;
            float y = baselineY - (ratio * activeHeight);
            points[i] = new PointF(x, y);
        }
        return points;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bandCount <= 0 || progress == null) return;

        PointF[] thumbs = calculateThumbPoints();

        // 1. Draw connecting line between thumbs
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(connectLineColor);
        paint.setStrokeWidth(connectLineWidth);
        for (int i = 0; i < bandCount - 1; i++) {
            canvas.drawLine(thumbs[i].x, thumbs[i].y, thumbs[i + 1].x, thumbs[i + 1].y, paint);
        }

        // 2. Draw vertical background tracks and active green lines
        for (int i = 0; i < bandCount; i++) {
            float x = thumbs[i].x;

            // Background gray track
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(bgLineColor);
            paint.setStrokeWidth(bgLineWidth);
            canvas.drawLine(x, baselineY, x, baselineY - activeHeight, paint);

            // Active green track from bottom to thumb
            paint.setColor(progressLineColor);
            paint.setStrokeWidth(progressLineWidth);
            canvas.drawLine(x, baselineY, x, thumbs[i].y, paint);
        }

        // 3. Draw thumbs (white circle)
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(thumbFillColor);
        for (int i = 0; i < bandCount; i++) {
            canvas.drawCircle(thumbs[i].x, thumbs[i].y, thumbRadius, paint);
        }

        // 4. Draw dB label on top of each band
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(textColor);
        paint.setTextSize(textSize);
        paint.setTextAlign(Paint.Align.CENTER);

        int centerVal = maxProgress / 2;
        for (int i = 0; i < bandCount; i++) {
            int db = (progress[i] - centerVal) / 100;
            String dbStr = (db > 0 ? "+" : "") + db + "dB";
            float x = thumbs[i].x;
            float y = getPaddingTop() + textSize;
            canvas.drawText(dbStr, x, y, paint);
        }

        // 5. Draw Frequency labels below each band
        for (int i = 0; i < bandCount; i++) {
            String freqStr = (freqLabels != null && i < freqLabels.length) ? freqLabels[i] : "";
            float x = thumbs[i].x;
            float y = viewHeight - getPaddingBottom() - 4;
            canvas.drawText(freqStr, x, y, paint);
        }
    }

    private int findClosestBand(float touchX, float touchY) {
        float minDist = Float.MAX_VALUE;
        int closestIndex = -1;
        float touchSlop = dpToPx(36);

        for (int i = 0; i < bandCount; i++) {
            float x = getBandCenterX(i);
            float dist = Math.abs(touchX - x);
            if (dist < touchSlop && dist < minDist) {
                minDist = dist;
                closestIndex = i;
            }
        }
        return closestIndex;
    }

    private void requestDisallowIntercept(boolean disallow) {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallow);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                activeTouchIndex = findClosestBand(x, y);
                if (activeTouchIndex >= 0) {
                    requestDisallowIntercept(true);
                    touchLastX = x;
                    touchLastY = y;
                    touchStartVal = progress[activeTouchIndex];

                    updateProgressFromTouch(activeTouchIndex, y);
                    if (listener != null) {
                        listener.onStartTrackingTouch(this, progress, activeTouchIndex);
                    }
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (activeTouchIndex >= 0) {
                    updateProgressFromTouch(activeTouchIndex, y);
                    if (listener != null) {
                        listener.onProgressChanged(this, progress, activeTouchIndex);
                    }
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (activeTouchIndex >= 0) {
                    requestDisallowIntercept(false);
                    if (listener != null) {
                        listener.onStopTrackingTouch(this, progress, activeTouchIndex);
                    }
                    activeTouchIndex = -1;
                    return true;
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    private void updateProgressFromTouch(int bandIndex, float touchY) {
        if (activeHeight <= 0) return;
        float relativeY = baselineY - touchY;
        float ratio = relativeY / activeHeight;
        int val = Math.round(ratio * maxProgress);
        val = Math.max(0, Math.min(maxProgress, val));

        progress[bandIndex] = val;
        invalidate();
    }
}

