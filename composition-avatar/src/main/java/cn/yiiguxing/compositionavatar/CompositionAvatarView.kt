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
import android.os.Build;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * CompositionAvatarView
 *
 * @author Yii.Guxing
 */
public class CompositionAvatarView extends View {

    public static final int MAX_DRAWABLE_COUNT = 5;
    public static final float DEFAULT_GAP = 0.25f;

    private final List<DrawableInfo> mDrawables = new ArrayList<>(MAX_DRAWABLE_COUNT);
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix mLayoutMatrix = new Matrix();
    private final RectF mTempBounds = new RectF();
    private final float[] mPointsTemp = new float[2];

    private int mContentSize;
    private float mSteinerCircleRadius;
    private float mOffsetY;

    private FitType mFitType = FitType.CENTER;
    private float mGap = DEFAULT_GAP;

    private static final FitType[] sFitTypeArray = {
            FitType.FIT,
            FitType.CENTER,
            FitType.START,
            FitType.END,
    };

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

        int index = a.getInt(R.styleable.CompositionAvatarView_fitType, -1);
        if (index >= 0) {
            setDrawableFitType(sFitTypeArray[index]);
        }

        float gap = a.getFloat(R.styleable.CompositionAvatarView_gap, DEFAULT_GAP);
        mGap = Math.max(0f, Math.min(gap, 1f));
        a.recycle();

        setLayerType(LAYER_TYPE_SOFTWARE, null);

        mPaint.setColor(Color.BLACK);
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        initForEditMode();
    }

    private void initForEditMode() {
        if (!isInEditMode()) return;

        mPaint.setXfermode(null);
        mPaint.setColor(0xff0577fc);
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

    /**
     * Set the gap value.
     *
     * @param gap the gap
     */
    public void setGap(@FloatRange(from = 0.f, to = 1.f) float gap) {
        gap = Math.max(0f, Math.min(gap, 1f));
        if (mGap != gap) {
            mGap = gap;
            invalidate();
        }
    }

    /**
     * @return the gap
     */
    @FloatRange(from = 0.f, to = 1.f)
    public float getGap() {
        return mGap;
    }

    /**
     * @return drawable数量
     */
    @IntRange(from = 0, to = MAX_DRAWABLE_COUNT)
    public int getNumberOfDrawables() {
        return mDrawables.size();
    }

    /**
     * @return drawable的大小（高等于宽）
     */
    public int getDrawableSize() {
        return Math.round(mSteinerCircleRadius * 2);
    }

    /**
     * Drawable填充类型
     */
    public enum FitType {
        FIT,
        CENTER,
        START,
        END
    }

    /**
     * 设置Drawable填充类型
     *
     * @param fitType Drawable填充类型
     * @see FitType
     */
    public void setDrawableFitType(@NonNull FitType fitType) {
        //noinspection ConstantConditions
        if (fitType == null) {
            throw new NullPointerException();
        }
        if (mFitType != fitType) {
            mFitType = fitType;
            for (DrawableInfo drawableInfo : mDrawables) {
                updateDrawableBounds(drawableInfo);
            }
            invalidate();
        }
    }

    /**
     * @return Drawable填充类型
     */
    @NonNull
    public FitType getFitType() {
        return mFitType;
    }

    /**
     * 通过ID获取对应的drawable.
     *
     * @param id the id.
     * @return the drawable.
     */
    @Nullable
    public Drawable findDrawableById(int id) {
        for (DrawableInfo drawable : mDrawables) {
            if (drawable.mId == id) {
                return drawable.mDrawable;
            }
        }

        return null;
    }

    /**
     * 通过索引获取对应的drawable.
     *
     * @param index 索引
     * @return the drawable.
     */
    @NonNull
    public Drawable getDrawableAt(int index) {
        return mDrawables.get(index).mDrawable;
    }

    @Nullable
    private DrawableInfo findAvatarDrawableById(int id) {
        if (id != NO_ID) {
            for (DrawableInfo drawable : mDrawables) {
                if (drawable.mId == id) {
                    return drawable;
                }
            }
        }

        return null;
    }

    private boolean hasSameDrawable(Drawable drawable) {
        List<DrawableInfo> drawables = this.mDrawables;
        for (int i = 0; i < drawables.size(); i++) {
            if (drawables.get(i).mDrawable == drawable) {
                return true;
            }
        }

        return false;
    }

    /**
     * 添加drawable.
     *
     * @param drawable the drawable.
     * @return <code>true</code> - 如果添加成功， <code>false</code> - 其他
     * @see #addDrawable(int, Drawable)
     */
    public boolean addDrawable(@NonNull Drawable drawable) {
        return addDrawable(NO_ID, drawable);
    }

    /**
     * 添加drawable, 如果id已经存在, drawable将会被替换
     *
     * @param id       the drawable id.
     * @param drawable the drawable.
     * @return <code>true</code> - 如果添加成功， <code>false</code> - 其他
     */
    public boolean addDrawable(int id, @NonNull Drawable drawable) {
        DrawableInfo old = findAvatarDrawableById(id);
        if (old != null) {
            Drawable d = old.mDrawable;
            old.mDrawable = drawable;
            if (!hasSameDrawable(d)) {
                cleanDrawable(d);
            }
            updateDrawableBounds(old);
        } else {
            if (getNumberOfDrawables() >= MAX_DRAWABLE_COUNT) {
                return false;
            }

            mDrawables.add(crateAvatarDrawable(id, drawable));
            layoutDrawables();
        }

        drawable.setCallback(this);
        drawable.setVisible(getWindowVisibility() == VISIBLE && isShown(), true);
        if (drawable.isStateful()) {
            drawable.setState(getDrawableState());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            drawable.setLayoutDirection(getLayoutDirection());
        }
        invalidate();

        return true;
    }

    private DrawableInfo crateAvatarDrawable(int id, Drawable drawable) {
        DrawableInfo avatar = new DrawableInfo();
        avatar.mId = id;
        avatar.mDrawable = drawable;
        return avatar;
    }

    /**
     * 移除drawable.
     *
     * @param drawable the drawable.
     * @see #removeDrawableAt(int)
     * @see #removeDrawableById(int)
     */
    public void removeDrawable(@NonNull Drawable drawable) {
        List<DrawableInfo> drawables = this.mDrawables;
        for (int i = drawables.size() - 1; i >= 0; i--) {
            if (drawables.get(i).mDrawable == drawable) {
                removeDrawableAt(i);
            }
        }
    }

    /**
     * 通过id移除drawable.
     *
     * @param id the id.
     * @return 被移除的drawable，<code>null</code> - 如果id不存在。
     * @see #removeDrawableAt(int)
     * @see #removeDrawable(Drawable)
     */
    @Nullable
    public Drawable removeDrawableById(int id) {
        List<DrawableInfo> drawables = this.mDrawables;
        for (int i = 0; i < drawables.size(); i++) {
            if (drawables.get(i).mId == id) {
                return removeDrawableAt(i);
            }
        }

        return null;
    }

    /**
     * 通过索引移除drawable.
     *
     * @param index 索引
     * @return 被移除的drawable
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   (<tt>index &lt; 0 || index &gt;= getNumberOfDrawables()</tt>)
     * @see #getNumberOfDrawables()
     * @see #removeDrawable(Drawable)
     * @see #removeDrawableById(int)
     */
    @NonNull
    public Drawable removeDrawableAt(int index) {
        DrawableInfo drawable = mDrawables.remove(index);
        if (!hasSameDrawable(drawable.mDrawable)) {
            cleanDrawable(drawable.mDrawable);
        }
        layoutDrawables();
        return drawable.mDrawable;
    }

    /**
     * 移除所有的drawable.
     */
    public void clearDrawable() {
        if (!mDrawables.isEmpty()) {
            for (DrawableInfo drawable : mDrawables) {
                cleanDrawable(drawable.mDrawable);
            }
            mDrawables.clear();
            layoutDrawables();
        }
    }

    private void cleanDrawable(Drawable drawable) {
        drawable.setCallback(null);
        unscheduleDrawable(drawable);
    }

    private void layoutDrawables() {
        mSteinerCircleRadius = 0;
        mOffsetY = 0;

        int width = getWidth() - getPaddingLeft() - getPaddingRight();
        int height = getHeight() - getPaddingTop() - getPaddingBottom();

        mContentSize = Math.min(width, height);
        final List<DrawableInfo> drawables = mDrawables;
        final int N = drawables.size();
        float center = mContentSize * .5f;
        if (mContentSize > 0 && N > 0) {
            // 图像圆的半径。
            final float r;
            if (N == 1) {
                r = mContentSize * .5f;
            } else if (N == 2) {
                r = (float) (mContentSize / (2 + 2 * Math.sin(Math.PI / 4)));
            } else if (N == 4) {
                r = mContentSize / 4.f;
            } else {
                r = (float) (mContentSize / (2 * (2 * Math.sin(((N - 2) * Math.PI)
                        / (2 * N)) + 1)));
                final double sinN = Math.sin(Math.PI / N);
                // 以所有图像圆为内切圆的圆的半径
                final float R = (float) (r * ((sinN + 1) / sinN));
                mOffsetY = (float)
                        ((mContentSize - R - r * (1 + 1 / Math.tan(Math.PI / N))) / 2f);
            }

            mSteinerCircleRadius = r;

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
                DrawableInfo drawable = drawables.get(i);
                drawable.reset();

                drawable.mHasGap = i > 0;
                if (drawable.mHasGap) {
                    drawable.mGapCenterX = pointsTemp[0];
                    drawable.mGapCenterY = pointsTemp[1];
                }

                pointsTemp[0] = startX;
                pointsTemp[1] = startY;
                if (i > 0) {
                    // 以上一个圆的圆心旋转计算得出当前圆的圆位置
                    matrix.postRotate(360.f / N, center, center + mOffsetY);
                    matrix.mapPoints(pointsTemp);
                }

                drawable.mCenterX = pointsTemp[0];
                drawable.mCenterY = pointsTemp[1];

                updateDrawableBounds(drawable);

                drawable.mMaskPath.addCircle(drawable.mCenterX, drawable.mCenterY, r,
                        Path.Direction.CW);
                drawable.mMaskPath.setFillType(Path.FillType.INVERSE_WINDING);
            }

            if (N > 2) {
                DrawableInfo first = drawables.get(0);
                DrawableInfo last = drawables.get(N - 1);
                first.mHasGap = true;
                first.mGapCenterX = last.mCenterX;
                first.mGapCenterY = last.mCenterY;
            }
        }

        invalidate();
    }

    private void updateDrawableBounds(DrawableInfo drawableInfo) {
        final Drawable drawable = drawableInfo.mDrawable;

        final float radius = mSteinerCircleRadius;
        if (radius <= 0) {
            drawable.setBounds(0, 0, 0, 0);
            return;
        }


        final int dWidth = drawable.getIntrinsicWidth();
        final int dHeight = drawable.getIntrinsicHeight();

        final RectF bounds = mTempBounds;
        bounds.setEmpty();

        if (dWidth <= 0 || dHeight <= 0 || dWidth == dHeight || FitType.FIT == mFitType) {
            bounds.inset(-radius, -radius);
        } else {
            float scale;
            if (dWidth > dHeight) {
                scale = radius / (float) dHeight;
            } else {
                scale = radius / (float) dWidth;
            }
            bounds.inset(-dWidth * scale, -dHeight * scale);

            if (FitType.START == mFitType || FitType.END == mFitType) {
                int dir = FitType.START == mFitType ? 1 : -1;
                bounds.offset((bounds.width() * 0.5f - radius) * dir,
                        (bounds.height() * 0.5f - radius) * dir);
            }
        }

        bounds.offset(drawableInfo.mCenterX, drawableInfo.mCenterY);
        drawable.setBounds((int) bounds.left, (int) bounds.top,
                Math.round(bounds.right), Math.round(bounds.bottom));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        layoutDrawables();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final List<DrawableInfo> drawables = mDrawables;
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
            canvas.drawCircle(cr, cr, cr, mPaint);
            return;
        }

        canvas.translate(0, mOffsetY);

        final Paint paint = mPaint;
        final float gapRadius = mSteinerCircleRadius * (mGap + 1f);
        for (int i = 0; i < drawables.size(); i++) {
            DrawableInfo drawable = drawables.get(i);
            final int savedLayer = canvas.saveLayer(0, 0, mContentSize, mContentSize,
                    null, Canvas.ALL_SAVE_FLAG);

            drawable.mDrawable.draw(canvas);

            canvas.drawPath(drawable.mMaskPath, paint);
            if (drawable.mHasGap && mGap > 0f) {
                canvas.drawCircle(drawable.mGapCenterX, drawable.mGapCenterY, gapRadius, paint);
            }

            canvas.restoreToCount(savedLayer);
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        updateVisible();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        updateVisible();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        updateVisible();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateVisible();
    }

    private void updateVisible() {
        boolean isVisible = getWindowVisibility() == VISIBLE && isShown();
        for (DrawableInfo drawable : mDrawables) {
            drawable.mDrawable.setVisible(isVisible, false);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        for (DrawableInfo drawable : mDrawables) {
            drawable.mDrawable.setVisible(false, false);
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        boolean invalidate = false;
        for (DrawableInfo drawable : mDrawables) {
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
        for (DrawableInfo drawable : mDrawables) {
            drawable.mDrawable.jumpToCurrentState();
        }
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable drawable) {
        return hasSameDrawable(drawable) || super.verifyDrawable(drawable);
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable drawable) {
        if (hasSameDrawable(drawable)) {
            invalidate();
        } else {
            super.invalidateDrawable(drawable);
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return CompositionAvatarView.class.getName();
    }

    private static class DrawableInfo {
        int mId = View.NO_ID;
        Drawable mDrawable;
        float mCenterX;
        float mCenterY;
        float mGapCenterX;
        float mGapCenterY;
        boolean mHasGap;
        final Path mMaskPath = new Path();

        void reset() {
            mCenterX = 0;
            mCenterY = 0;
            mGapCenterX = 0;
            mGapCenterY = 0;
            mHasGap = false;
            mMaskPath.reset();
        }
    }

}
