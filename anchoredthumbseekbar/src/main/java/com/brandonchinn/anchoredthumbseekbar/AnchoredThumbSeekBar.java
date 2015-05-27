package com.brandonchinn.anchoredthumbseekbar;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;

/**
 * Created by brandonchinn on 5/26/15.
 */
public class AnchoredThumbSeekBar extends View {

    private int barHeight;
    private int thumbWidth;
    private int maxValue;
    private int progress;

    private Drawable elapsedDrawable;
    private Drawable remainingDrawable;

    public AnchoredThumbSeekBar(Context context) {
        super(context);
    }

    public AnchoredThumbSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AnchoredThumbSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initDefaultAttributes();

        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.AnchoredThumbSeekBar);

        barHeight = a.getDimensionPixelSize(R.styleable.AnchoredThumbSeekBar_barHeight, barHeight);
        thumbWidth = a.getDimensionPixelSize(R.styleable.AnchoredThumbSeekBar_thumbWidth, thumbWidth);
        setMax(a.getInt(R.styleable.AnchoredThumbSeekBar_max, maxValue));
        setProgress(a.getInt(R.styleable.AnchoredThumbSeekBar_progress, progress));

        initializeProgressDrawables(barHeight);

        a.recycle();
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawTrack(canvas);
    }

    private void drawTrack(Canvas c) {
        elapsedDrawable.setLevel(5000);
        elapsedDrawable.draw(c);
    }


    private void initializeProgressDrawables(int barHeight) {

        final ShapeDrawable shapeDrawable = new ShapeDrawable(new RectShape());
        shapeDrawable.getPaint().setColor(Color.GREEN);

        // Ensure the tint and filter are propagated in the correct order.
        /*
        shapeDrawable.setTintList(bitmap.getTint());
        shapeDrawable.setTintMode(bitmap.getTintMode());*/
        //shapeDrawable.setColorFilter(Color.WHITE);

        elapsedDrawable = new ClipDrawable(shapeDrawable, Gravity.RIGHT, ClipDrawable.HORIZONTAL);
        shapeDrawable.getPaint().setColor(Color.RED);
        remainingDrawable = new ClipDrawable(shapeDrawable, Gravity.LEFT, ClipDrawable.HORIZONTAL);
    }


    public synchronized void setMax(int max) {
        if (max < 0) {
            max = 0;
        }
        if (max != maxValue) {
            maxValue = max;
            postInvalidate();

            if (progress > max) {
                progress = max;
            }
            //refreshProgress(R.id.progress, mProgress, false);
        }
    }

    public synchronized void setProgress(int progress) {
        setProgress(progress, true);
    }

    private synchronized void setProgress(int progress, boolean fromUser) {
        if (progress < 0) {
            progress = 0;
        }

        if (progress > maxValue) {
            progress = maxValue;
        }

        if (progress != this.progress) {
            this.progress = progress;
            //refreshProgress(R.id.progress, mProgress, fromUser);
        }
    }

    private void initDefaultAttributes() {
        Resources r = getResources();
        barHeight = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, r.getDisplayMetrics()));
        thumbWidth = barHeight;
        maxValue = 100;
        progress = 0;
    }

}
