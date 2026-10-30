package com.app.zevix.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PatternLockView extends View {

    public interface OnPatternListener {
        void onPatternComplete(String patternString);
        void onPatternStarted();
    }

    private static class Dot {
        float x;
        float y;
        int index;

        Dot(float x, float y, int index) {
            this.x = x;
            this.y = y;
            this.index = index;
        }
    }

    private final Dot[] dots = new Dot[9];
    private final List<Integer> selectedIndices = new ArrayList<>();
    private float currentTouchX = -1;
    private float currentTouchY = -1;
    private boolean isDrawing = false;
    private boolean isError = false;

    private Paint dotNormalPaint;
    private Paint dotSelectedPaint;
    private Paint dotSelectedCenterPaint;
    private Paint linePaint;
    private Paint lineErrorPaint;

    private OnPatternListener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public PatternLockView(Context context) {
        super(context);
        init();
    }

    public PatternLockView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PatternLockView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        dotNormalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotNormalPaint.setColor(0x88ffffff);
        dotNormalPaint.setStyle(Paint.Style.FILL);

        dotSelectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotSelectedPaint.setColor(0x330dac08);
        dotSelectedPaint.setStyle(Paint.Style.FILL);

        dotSelectedCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotSelectedCenterPaint.setColor(0xff0dac08);
        dotSelectedCenterPaint.setStyle(Paint.Style.FILL);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(0xff0dac08);
        linePaint.setStrokeWidth(10f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        lineErrorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lineErrorPaint.setColor(0xffff4444);
        lineErrorPaint.setStrokeWidth(10f);
        lineErrorPaint.setStyle(Paint.Style.STROKE);
        lineErrorPaint.setStrokeCap(Paint.Cap.ROUND);
        lineErrorPaint.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setOnPatternListener(OnPatternListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float size = Math.min(w, h);
        float padding = size * 0.16f;
        float usableSize = size - (2 * padding);
        float step = usableSize / 2f;

        float offsetX = (w - size) / 2f + padding;
        float offsetY = (h - size) / 2f + padding;

        int idx = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                dots[idx] = new Dot(offsetX + col * step, offsetY + row * step, idx);
                idx++;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint activeLinePaint = isError ? lineErrorPaint : linePaint;

        // 1. Draw connecting lines between selected dots
        if (!selectedIndices.isEmpty()) {
            Path path = new Path();
            Dot first = dots[selectedIndices.get(0)];
            path.moveTo(first.x, first.y);
            for (int i = 1; i < selectedIndices.size(); i++) {
                Dot next = dots[selectedIndices.get(i)];
                path.lineTo(next.x, next.y);
            }

            if (isDrawing && currentTouchX >= 0 && currentTouchY >= 0 && !isError) {
                path.lineTo(currentTouchX, currentTouchY);
            }
            canvas.drawPath(path, activeLinePaint);
        }

        // 2. Draw dots
        float normalRadius = 7f * getResources().getDisplayMetrics().density;
        float selectedRadius = 22f * getResources().getDisplayMetrics().density;
        float innerRadius = 8f * getResources().getDisplayMetrics().density;

        for (Dot dot : dots) {
            if (dot == null) continue;
            boolean isSelected = selectedIndices.contains(dot.index);

            if (isSelected) {
                if (isError) {
                    dotSelectedPaint.setColor(0x33ff4444);
                    dotSelectedCenterPaint.setColor(0xffff4444);
                } else {
                    dotSelectedPaint.setColor(0x330dac08);
                    dotSelectedCenterPaint.setColor(0xff0dac08);
                }
                canvas.drawCircle(dot.x, dot.y, selectedRadius, dotSelectedPaint);
                canvas.drawCircle(dot.x, dot.y, innerRadius, dotSelectedCenterPaint);
            } else {
                canvas.drawCircle(dot.x, dot.y, normalRadius, dotNormalPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handler.removeCallbacksAndMessages(null);
                isError = false;
                selectedIndices.clear();
                isDrawing = true;
                if (listener != null) listener.onPatternStarted();
                checkDotHit(x, y);
                currentTouchX = x;
                currentTouchY = y;
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isDrawing) {
                    currentTouchX = x;
                    currentTouchY = y;
                    checkDotHit(x, y);
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDrawing) {
                    isDrawing = false;
                    currentTouchX = -1;
                    currentTouchY = -1;
                    invalidate();
                    if (listener != null && !selectedIndices.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < selectedIndices.size(); i++) {
                            if (i > 0) sb.append(",");
                            sb.append(selectedIndices.get(i));
                        }
                        listener.onPatternComplete(sb.toString());
                    }
                }
                return true;
        }

        return super.onTouchEvent(event);
    }

    private void checkDotHit(float x, float y) {
        float hitRadius = 32f * getResources().getDisplayMetrics().density;
        for (Dot dot : dots) {
            if (dot == null) continue;
            float dx = x - dot.x;
            float dy = y - dot.y;
            if ((dx * dx + dy * dy) <= (hitRadius * hitRadius)) {
                if (!selectedIndices.contains(dot.index)) {
                    selectedIndices.add(dot.index);
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                }
                break;
            }
        }
    }

    public void setErrorState(boolean error) {
        this.isError = error;
        invalidate();
        if (error) {
            handler.postDelayed(this::clearPattern, 750);
        }
    }

    public void clearPattern() {
        selectedIndices.clear();
        isDrawing = false;
        isError = false;
        currentTouchX = -1;
        currentTouchY = -1;
        invalidate();
    }
}

