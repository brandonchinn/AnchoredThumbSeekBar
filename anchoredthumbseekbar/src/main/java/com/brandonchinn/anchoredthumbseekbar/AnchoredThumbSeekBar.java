package com.brandonchinn.anchoredthumbseekbar;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;

/**
 * Created by brandonchinn on 5/26/15.
 */
public class AnchoredThumbSeekBar extends View {
    private static int MAX_LEVEL=10000;

    private int barHeight;
    private int thumbWidth;
    private int maxValue;
    private int progress;
    private int barOffset;
    private float animationPosition;
    private Drawable elapsedDrawable;
    private Drawable remainingDrawable;
    private Drawable thumbDrawable;

    private boolean isDragging = false;
    private float initialPosX = -1;

    /**
     * A callback that notifies clients when the progress level has been
     * changed. This includes changes that were initiated by the user through a
     * touch gesture or arrow key/trackball as well as changes that were initiated
     * programmatically.
     */
    public interface OnAnchoredThumbSeekBarChangeListener {

        /**
         * Notification that the progress level has changed. Clients can use the fromUser parameter
         * to distinguish user-initiated changes from those that occurred programmatically.
         *
         * @param seekBar The SeekBar whose progress has changed
         * @param progress The current progress level.
         * @param fromUser True if the progress change was initiated by the user.
         */
        void onProgressChanged(AnchoredThumbSeekBar seekBar, int progress, boolean fromUser);

        /**
         * Notification that the user has started a touch gesture. Clients may want to use this
         * to disable advancing the seekbar.
         * @param seekBar The SeekBar in which the touch gesture began
         */
        void onStartTrackingTouch(AnchoredThumbSeekBar seekBar);

        /**
         * Notification that the user has finished a touch gesture. Clients may want to use this
         * to re-enable advancing the seekbar.
         * @param seekBar The SeekBar in which the touch gesture began
         */
        void onStopTrackingTouch(AnchoredThumbSeekBar seekBar);
    }

    private OnAnchoredThumbSeekBarChangeListener mOnSeekBarChangeListener;

    public AnchoredThumbSeekBar(Context context) {
        super(context);
        init(context, null);
    }

    public AnchoredThumbSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AnchoredThumbSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);

    }

    private void init(Context context, AttributeSet attrs) {
        initDefaultAttributes();

        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.AnchoredThumbSeekBar);

        barHeight = a.getDimensionPixelSize(R.styleable.AnchoredThumbSeekBar_barHeight, barHeight);
        thumbWidth = a.getDimensionPixelSize(R.styleable.AnchoredThumbSeekBar_thumbWidth, thumbWidth);
        barOffset = a.getDimensionPixelSize(R.styleable.AnchoredThumbSeekBar_barOffset, barOffset);
        elapsedDrawable = a.getDrawable(R.styleable.AnchoredThumbSeekBar_elapsedDrawable);
        remainingDrawable = a.getDrawable(R.styleable.AnchoredThumbSeekBar_remainingDrawable);
        thumbDrawable = a.getDrawable(R.styleable.AnchoredThumbSeekBar_thumbDrawable);

        initializeProgressDrawables(barHeight);

        setMax(a.getInt(R.styleable.AnchoredThumbSeekBar_max, maxValue));
        setProgress(a.getInt(R.styleable.AnchoredThumbSeekBar_progress, progress), false);

        a.recycle();
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawTrack(canvas);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int left = getPaddingLeft();
        int top = getPaddingTop();
        int right = left + w - getPaddingRight();
        int bottom = top + h - getPaddingBottom();
        int centerWidth = Math.round((right - left)/2.0f);
        int centerHeight = Math.round((bottom - top)/2.0f);
        int progressTop = centerHeight - Math.round(barHeight / 2.0f) + barOffset;
        int progressBottom = centerHeight + Math.round(barHeight / 2.0f) + barOffset;
        elapsedDrawable.setBounds(left, progressTop, centerWidth, progressBottom);
        remainingDrawable.setBounds(centerWidth, progressTop, right, progressBottom);

        int thumbLeft = centerWidth - Math.round(thumbWidth/2.0f);
        int thumbRight = centerWidth + Math.round(thumbWidth/2.0f);
        thumbDrawable.setBounds(thumbLeft, top, thumbRight, bottom);
    }

    private void drawTrack(Canvas c) {
        elapsedDrawable.draw(c);
        remainingDrawable.draw(c);
        thumbDrawable.draw(c);
    }


    private void initializeProgressDrawables(int barHeight) {
        //elapsed portion of the drawable (left side)
        if (elapsedDrawable == null) {
            elapsedDrawable = new ShapeDrawable(getDrawableShape());
            ((ShapeDrawable)elapsedDrawable).getPaint().setColor(Color.WHITE);
        }

        elapsedDrawable = tileify(elapsedDrawable, Gravity.RIGHT, true);

        if (remainingDrawable == null) {
            remainingDrawable = new ShapeDrawable(getDrawableShape());
            ((ShapeDrawable)remainingDrawable).getPaint().setColor(Color.WHITE);
        }

        remainingDrawable = tileify(remainingDrawable, Gravity.LEFT, true);

        //Anchored seek thumb
        if (thumbDrawable == null) {
            thumbDrawable = new ShapeDrawable(getDrawableShape());
            ((ShapeDrawable)thumbDrawable).getPaint().setColor(Color.WHITE);
        }

        thumbDrawable = tileify(thumbDrawable, Gravity.LEFT, false);
    }

    private synchronized void doRefreshProgress(int progress, boolean fromUser) {
        float scale = maxValue > 0 ? (1.0f*progress) / maxValue : 0;
        elapsedDrawable.setLevel(Math.min(Math.round(scale * MAX_LEVEL * 2), MAX_LEVEL));
        remainingDrawable.setLevel(Math.min(Math.round((1 - scale) * MAX_LEVEL * 2), MAX_LEVEL));
        postInvalidate();
        if (mOnSeekBarChangeListener != null) {
            mOnSeekBarChangeListener.onProgressChanged(this, progress, fromUser);
        }
    }

    public synchronized void setMax(int max) {
        if (max < 0) {
            max = 0;
        }
        if (max != maxValue) {
            maxValue = max;

            if (progress > max) {
                progress = max;
                postInvalidate();
            }
        }
    }

    public void setProgress(int progress) {
        setProgress(progress, false);
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
            doRefreshProgress(progress, fromUser);
        }
    }

    public synchronized final void incrementProgressBy(int diff) {
        setProgress(this.progress + diff, false);
    }

    public synchronized int getProgress() {
        return progress;
    }


    private Shape getDrawableShape() {
        final float[] roundedCorners = new float[] { 5, 5, 5, 5, 5, 5, 5, 5 };
        return new RoundRectShape(roundedCorners, null, null);
    }

    private void initDefaultAttributes() {
        Resources r = getResources();
        barHeight = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, r.getDisplayMetrics()));
        thumbWidth = barHeight;
        maxValue = 1000;
        progress = -1;
        barOffset = 0;
    }

    /**
     * Converts a drawable to a tiled version of itself. It will recursively
     * traverse layer and state list drawables.
     */
    private Drawable tileify(Drawable drawable, int gravity, boolean clip) {

        if (drawable instanceof LayerDrawable) {
            LayerDrawable background = (LayerDrawable) drawable;
            final int N = background.getNumberOfLayers();
            Drawable[] outDrawables = new Drawable[N];

            for (int i = 0; i < N; i++) {
                outDrawables[i] = tileify(background.getDrawable(i), gravity, clip);
            }

            LayerDrawable newBg = new LayerDrawable(outDrawables);

            for (int i = 0; i < N; i++) {
                newBg.setId(i, background.getId(i));
            }

            return newBg;

        } else if (drawable instanceof StateListDrawable) {
            StateListDrawable in = (StateListDrawable) drawable;
            StateListDrawable out = new StateListDrawable();
            for (int i = 0; in.selectDrawable(i); i++) {
                Drawable temp = in.getCurrent();
                out.addState(temp.getState(), tileify(temp, gravity, clip));
            }
            return out;

        } else if (drawable instanceof BitmapDrawable) {
            final BitmapDrawable bitmap = (BitmapDrawable) drawable;
            final Bitmap tileBitmap = bitmap.getBitmap();

            final ShapeDrawable shapeDrawable = new ShapeDrawable(getDrawableShape());
            final BitmapShader bitmapShader = new BitmapShader(tileBitmap,
                    Shader.TileMode.REPEAT, Shader.TileMode.CLAMP);
            shapeDrawable.getPaint().setShader(bitmapShader);

            return clip ? new ClipDrawable(shapeDrawable, gravity, ClipDrawable.HORIZONTAL) : shapeDrawable;
        } else if (clip) {
            return new ClipDrawable(drawable, gravity, ClipDrawable.HORIZONTAL);
        }

        return drawable;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setPressed(true);
                onStartTrackingTouch();
                initialPosX = event.getX();
                attemptClaimDrag();
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    trackTouchEvent(event);
                } else {
                    setPressed(true);
                    onStartTrackingTouch();
                    initialPosX = event.getX();
                    attemptClaimDrag();
                }
                break;

            case MotionEvent.ACTION_UP:
                if (isDragging) {
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                    setPressed(false);
                }

            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    onStopTrackingTouch();
                    setPressed(false);
                }
                break;
        }
        return true;
    }

    private void onStartTrackingTouch() {
        isDragging = true;
        if (mOnSeekBarChangeListener != null) {
            mOnSeekBarChangeListener.onStartTrackingTouch(this);
        }
    }

    private void onStopTrackingTouch() {
        isDragging = false;
        if (mOnSeekBarChangeListener != null) {
            mOnSeekBarChangeListener.onStopTrackingTouch(this);
        }
    }

    /**
     * Sets a listener to receive notifications of changes to the SeekBar's progress level. Also
     * provides notifications of when the user starts and stops a touch gesture within the SeekBar.
     *
     * @param l The seek bar notification listener
     *
     * @see SeekBar.OnSeekBarChangeListener
     */
    public void setOnAnchoredThumbSeekBarChangeListener(OnAnchoredThumbSeekBarChangeListener l) {
        mOnSeekBarChangeListener = l;
    }

    private void trackTouchEvent(MotionEvent event) {
        final int currentProgress = getProgress();
        final int width = getWidth() - getPaddingLeft() - getPaddingRight();
        final float x = event.getX();
        final int changed = Math.round(initialPosX - x);
        int newProgress = currentProgress + (changed * Math.round(((1.0f * width) / maxValue)));
        newProgress = Math.max(newProgress, 0);
        newProgress = Math.min(newProgress, maxValue);
        setProgress(newProgress, true);
        initialPosX = x;

    }

    /**
     * Tries to claim the user's drag motion, and requests disallowing any
     * ancestors from stealing events in the drag.
     */
    private void attemptClaimDrag() {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

}
