package cn.yiiguxing.compositionavatar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class CompositionAvatarView extends View {

    public static final int MAX_DRAWABLE_COUNT = 5;
    public static final float DEFAULT_GAP = 0.25f;

    private final List<AvatarDrawable> mDrawables = new ArrayList<>(MAX_DRAWABLE_COUNT);
    private final Paint mMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix mLayoutMatrix = new Matrix();
    private final float[] mPointsTemp = new float[2];

    private int mContentSize;
    private float mSteinerCircleRadius;
    private float mOffsetY;

    private float mGap = DEFAULT_GAP;

    public CompositionAvatarView(Context context) {
        super(context);
        init(null, 0);
    }

    public CompositionAvatarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public CompositionAvatarView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        final TypedArray a = getContext().obtainStyledAttributes(attrs,
                R.styleable.CompositionAvatarView, defStyle, 0);
        float gap = a.getFloat(R.styleable.CompositionAvatarView_gap, DEFAULT_GAP);
        mGap = Math.max(0f, Math.min(gap, 1f));
        a.recycle();

        setLayerType(LAYER_TYPE_SOFTWARE, null);

        mMaskPaint.setColor(Color.BLACK);
        mMaskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        initForEditMode();
    }

    private void initForEditMode() {
        if (!isInEditMode()) return;

        mMaskPaint.setXfermode(null);
        mMaskPaint.setColor(0xff0577fc);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int width, height;
        if (widthMode == MeasureSpec.EXACTLY && heightMode != MeasureSpec.EXACTLY) {
            width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
            height = width - getPaddingLeft() - getPaddingRight()
                    + getPaddingTop() + getPaddingBottom();
        } else if (widthMode != MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
            height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
            width = height + getPaddingLeft() + getPaddingRight()
                    - getPaddingTop() - getPaddingBottom();
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        setMeasuredDimension(width, height);
    }

    public void setGap(@FloatRange(from = 0.f, to = 1.f) float gap) {
        gap = Math.max(0f, Math.min(gap, 1f));
        if (mGap != gap) {
            mGap = gap;
            invalidate();
        }
    }

    @FloatRange(from = 0.f, to = 1.f)
    public float getGap() {
        return mGap;
    }

    @IntRange(from = 0, to = MAX_DRAWABLE_COUNT)
    public int getNumberOfDrawables() {
        return mDrawables.size();
    }

    public int getDrawableSize() {
        return Math.round(mSteinerCircleRadius * 2);
    }

    @Nullable
    public Drawable findDrawableById(int id) {
        for (AvatarDrawable drawable : mDrawables) {
            if (drawable.mId == id) {
                return drawable.mDrawable;
            }
        }

        return null;
    }

    @Nullable
    public Drawable getDrawableAt(int index) {
        return mDrawables.get(index).mDrawable;
    }

    @Nullable
    private AvatarDrawable findAvatarDrawableById(int id) {
        for (AvatarDrawable drawable : mDrawables) {
            if (drawable.mId == id) {
                return drawable;
            }
        }

        return null;
    }

    public void addDrawable(@NonNull Drawable drawable) {
        addDrawable(NO_ID, drawable);
    }

    public void addDrawable(int id, @NonNull Drawable drawable) {
        AvatarDrawable old = id != NO_ID ? findAvatarDrawableById(id) : null;
        if (old != null) {
            old.mDrawable.setCallback(null);
            unscheduleDrawable(old.mDrawable);

            old.mDrawable = drawable;
        } else {
            if (getNumberOfDrawables() >= MAX_DRAWABLE_COUNT) {
                return;
            }

            mDrawables.add(crateAvatarDrawable(id, drawable));
            layoutDrawables();
        }

        drawable.setCallback(this);
        if (drawable.isStateful()) {
            drawable.setState(getDrawableState());
        }
    }

    private AvatarDrawable crateAvatarDrawable(int id, Drawable drawable) {
        AvatarDrawable avatar = new AvatarDrawable();
        avatar.mId = id;
        avatar.mDrawable = drawable;
        return avatar;
    }

    public void removeDrawable(@NonNull Drawable drawable) {
        List<AvatarDrawable> drawables = this.mDrawables;
        for (int i = drawables.size() - 1; i >= 0; i--) {
            if (drawables.get(i).mDrawable == drawable) {
                removeDrawableAt(i);
            }
        }
    }

    @Nullable
    public Drawable removeDrawableById(int id) {
        List<AvatarDrawable> drawables = this.mDrawables;
        for (int i = 0; i < drawables.size(); i++) {
            if (drawables.get(i).mId == id) {
                return removeDrawableAt(i);
            }
        }

        return null;
    }

    public Drawable removeDrawableAt(int index) {
        AvatarDrawable drawable = mDrawables.remove(index);
        drawable.mDrawable.setCallback(null);
        unscheduleDrawable(drawable.mDrawable);
        layoutDrawables();
        return drawable.mDrawable;
    }

    private void layoutDrawables() {
        mSteinerCircleRadius = 0;
        mOffsetY = 0;

        int width = getWidth() - getPaddingLeft() - getPaddingRight();
        int height = getHeight() - getPaddingTop() - getPaddingBottom();

        mContentSize = Math.min(width, height);
        final List<AvatarDrawable> drawables = mDrawables;
        final int N = drawables.size();
        float center = mContentSize / 2.f;
        if (mContentSize > 0 && N > 0) {
            final float r;
            if (N == 1) {
                r = mContentSize / 2.f;
            } else if (N == 2) {
                r = (float) (mContentSize / (2 + 2 * Math.sin(Math.PI / 4)));
            } else if (N == 4) {
                r = mContentSize / 4.f;
            } else {
                r = (float) (mContentSize / (2 * (2 * Math.sin(((N - 2) * Math.PI)
                        / (2 * N)) + 1)));
                final double sinN = Math.sin(Math.PI / N);
                final float R = (float) (r * ((sinN + 1) / sinN));
                mOffsetY = (float)
                        ((mContentSize - R - r * (1 + 1 / Math.tan(Math.PI / N))) / 2f);
            }

            final float startX, startY;
            if (N % 2 == 0) {
                startX = startY = r;
            } else {
                startX = center;
                startY = r;
            }

            final Matrix matrix = mLayoutMatrix;
            final float[] pointsTemp = this.mPointsTemp;

            matrix.reset();

            for (int i = 0; i < drawables.size(); i++) {
                AvatarDrawable drawable = drawables.get(i);
                drawable.reset();

                drawable.haveGap = i > 0;
                if (drawable.haveGap) {
                    drawable.gapCenterX = pointsTemp[0];
                    drawable.gapCenterY = pointsTemp[1];
                }

                pointsTemp[0] = startX;
                pointsTemp[1] = startY;
                if (i > 0) {
                    matrix.postRotate(360.f / N, center, center + mOffsetY);
                    matrix.mapPoints(pointsTemp);
                }

                drawable.centerX = pointsTemp[0];
                drawable.centerY = pointsTemp[1];

                drawable.mBounds.inset(-r, -r);
                drawable.mBounds.offset(drawable.centerX, drawable.centerY);

                drawable.mMaskPath.addCircle(drawable.centerX, drawable.centerY, r,
                        Path.Direction.CW);
                drawable.mMaskPath.setFillType(Path.FillType.INVERSE_WINDING);
            }

            if (N > 2) {
                AvatarDrawable first = drawables.get(0);
                AvatarDrawable last = drawables.get(N - 1);
                first.haveGap = true;
                first.gapCenterX = last.centerX;
                first.gapCenterY = last.centerY;
            }

            mSteinerCircleRadius = r;
        }

        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        layoutDrawables();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final List<AvatarDrawable> drawables = mDrawables;
        final int N = drawables.size();

        if (!isInEditMode() && (mContentSize <= 0 || N <= 0)) {
            return;
        }

        canvas.translate(getPaddingLeft(), getPaddingTop());
        int width = getWidth() - getPaddingLeft() - getPaddingRight();
        int height = getHeight() - getPaddingTop() - getPaddingBottom();
        if (width > height) {
            canvas.translate((width - height) * .5f, 0);
        } else {
            canvas.translate(0, (height - width) * .5f);
        }

        if (isInEditMode()) {
            float cr = Math.min(width, height) * .5f;
            canvas.drawCircle(cr, cr, cr, mMaskPaint);
            return;
        }

        canvas.translate(0, mOffsetY);

        final Paint maskPaint = mMaskPaint;
        final float gapRadius = mSteinerCircleRadius * (mGap + 1f);
        for (AvatarDrawable drawable : drawables) {
            RectF bounds = drawable.mBounds;
            final int savedLayer = canvas.saveLayer(0, 0, mContentSize, mContentSize,
                    null, Canvas.ALL_SAVE_FLAG);

            drawable.mDrawable.setBounds((int) bounds.left, (int) bounds.top,
                    Math.round(bounds.right), Math.round(bounds.bottom));
            drawable.mDrawable.draw(canvas);

            canvas.drawPath(drawable.mMaskPath, maskPaint);
            if (drawable.haveGap && mGap > 0f) {
                canvas.drawCircle(drawable.gapCenterX, drawable.gapCenterY, gapRadius, maskPaint);
            }

            canvas.restoreToCount(savedLayer);
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        boolean invalidate = false;
        for (AvatarDrawable drawable : mDrawables) {
            Drawable d = drawable.mDrawable;
            if (d.isStateful() && d.setState(getDrawableState())) {
                invalidate = true;
            }
        }

        if (invalidate) {
            invalidate();
        }
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        for (AvatarDrawable drawable : mDrawables) {
            drawable.mDrawable.jumpToCurrentState();
        }
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable drawable) {
        for (AvatarDrawable d : mDrawables) {
            if (d.mDrawable == drawable) {
                invalidate();
                return;
            }
        }
        super.invalidateDrawable(drawable);
    }

    private static class AvatarDrawable {
        int mId = View.NO_ID;
        Drawable mDrawable;
        float centerX;
        float centerY;
        float gapCenterX;
        float gapCenterY;
        boolean haveGap;
        final RectF mBounds = new RectF();
        final Path mMaskPath = new Path();

        void reset() {
            centerX = 0;
            centerY = 0;
            gapCenterX = 0;
            gapCenterY = 0;
            haveGap = false;
            mBounds.setEmpty();
            mMaskPath.reset();
        }

    }

}
