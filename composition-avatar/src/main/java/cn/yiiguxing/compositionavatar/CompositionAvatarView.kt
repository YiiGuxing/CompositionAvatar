package cn.yiiguxing.compositionavatar

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import java.util.*
import kotlin.math.*

/**
 * CompositionAvatarView
 *
 * @author Yii.Guxing
 */
class CompositionAvatarView : View {
    private val mDrawables: MutableList<DrawableInfo> = ArrayList(MAX_DRAWABLE_COUNT)
    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mLayoutMatrix = Matrix()
    private val mTempBounds = RectF()
    private val mPointsTemp = FloatArray(2)
    private var mContentSize = 0
    private var mSteinerCircleRadius = 0f
    private var mOffsetY = 0f

    /**
     * @return Drawable填充类型
     */
    var fitType = FitType.CENTER
        private set
    private var mGap = DEFAULT_GAP

    constructor(context: Context?) : super(context) {
        init(null, 0)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.CompositionAvatarView, defStyle, 0
        )
        val index = a.getInt(R.styleable.CompositionAvatarView_fitType, -1)
        if (index >= 0) {
            setDrawableFitType(sFitTypeArray[index])
        }
        val gap = a.getFloat(R.styleable.CompositionAvatarView_gap, DEFAULT_GAP)
        mGap = max(0f, min(gap, 1f))
        a.recycle()
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        mPaint.color = Color.BLACK
        mPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        initForEditMode()
    }

    private fun initForEditMode() {
        if (!isInEditMode) return
        mPaint.xfermode = null
        mPaint.color = -0xfa8804
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val width: Int
        val height: Int
        if (widthMode == MeasureSpec.EXACTLY && heightMode != MeasureSpec.EXACTLY) {
            width = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
            height = width - paddingLeft - paddingRight + paddingTop + paddingBottom
        } else if (widthMode != MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
            height = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
            width = height + paddingLeft + paddingRight - paddingTop - paddingBottom
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        setMeasuredDimension(width, height)
    }
    /**
     * @return the gap
     */
    /**
     * Set the gap value.
     *
     * @param gap the gap
     */
    @get:FloatRange(from = 0.0, to = 1.0)
    var gap: Float
        get() = mGap
        set(gap) {
            var gap = gap
            gap = max(0f, min(gap, 1f))
            if (mGap != gap) {
                mGap = gap
                invalidate()
            }
        }

    /**
     * @return drawable数量
     */
    @get:IntRange(from = 0, to = MAX_DRAWABLE_COUNT.toLong())
    val numberOfDrawables: Int
        get() = mDrawables.size

    /**
     * @return drawable的大小（高等于宽）
     */
    val drawableSize: Int
        get() = (mSteinerCircleRadius * 2).roundToInt()

    /**
     * 设置Drawable填充类型
     *
     * @param fitType Drawable填充类型
     * @see FitType
     */
    fun setDrawableFitType(fitType: FitType) {
        if (this.fitType !== fitType) {
            this.fitType = fitType
            for (drawableInfo in mDrawables) {
                updateDrawableBounds(drawableInfo)
            }
            invalidate()
        }
    }

    /**
     * 通过ID获取对应的drawable.
     *
     * @param id the id.
     * @return the drawable.
     */
    fun findDrawableById(id: Int): Drawable? {
        for (drawable in mDrawables) {
            if (drawable.mId == id) {
                return drawable.mDrawable
            }
        }
        return null
    }

    /**
     * 通过索引获取对应的drawable.
     *
     * @param index 索引
     * @return the drawable.
     */
    fun getDrawableAt(index: Int): Drawable? {
        return mDrawables[index].mDrawable
    }

    private fun findAvatarDrawableById(id: Int): DrawableInfo? {
        if (id != NO_ID) {
            for (drawable in mDrawables) {
                if (drawable.mId == id) {
                    return drawable
                }
            }
        }
        return null
    }

    private fun hasSameDrawable(drawable: Drawable?): Boolean {
        val drawables: List<DrawableInfo> = mDrawables
        for (i in drawables.indices) {
            if (drawables[i].mDrawable === drawable) {
                return true
            }
        }
        return false
    }

    /**
     * 添加drawable.
     *
     * @param drawable the drawable.
     * @return `true` - 如果添加成功， `false` - 其他
     * @see .addDrawable
     */
    fun addDrawable(drawable: Drawable): Boolean {
        return addDrawable(NO_ID, drawable)
    }

    /**
     * 添加drawable, 如果id已经存在, drawable将会被替换
     *
     * @param id       the drawable id.
     * @param drawable the drawable.
     * @return `true` - 如果添加成功， `false` - 其他
     */
    fun addDrawable(id: Int, drawable: Drawable): Boolean {
        val old = findAvatarDrawableById(id)
        if (old != null) {
            val d = old.mDrawable
            old.mDrawable = drawable
            if (!hasSameDrawable(d)) {
                cleanDrawable(d)
            }
            updateDrawableBounds(old)
        } else {
            if (numberOfDrawables >= MAX_DRAWABLE_COUNT) {
                return false
            }
            mDrawables.add(crateAvatarDrawable(id, drawable))
            layoutDrawables()
        }
        drawable.callback = this
        drawable.setVisible(windowVisibility == VISIBLE && isShown, true)
        if (drawable.isStateful) {
            drawable.state = drawableState
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            drawable.layoutDirection = layoutDirection
        }
        invalidate()
        return true
    }

    private fun crateAvatarDrawable(id: Int, drawable: Drawable): DrawableInfo {
        val avatar = DrawableInfo()
        avatar.mId = id
        avatar.mDrawable = drawable
        return avatar
    }

    /**
     * 移除drawable.
     *
     * @param drawable the drawable.
     * @see .removeDrawableAt
     * @see .removeDrawableById
     */
    fun removeDrawable(drawable: Drawable) {
        val drawables: List<DrawableInfo> = mDrawables
        for (i in drawables.indices.reversed()) {
            if (drawables[i].mDrawable === drawable) {
                removeDrawableAt(i)
            }
        }
    }

    /**
     * 通过id移除drawable.
     *
     * @param id the id.
     * @return 被移除的drawable，`null` - 如果id不存在。
     * @see .removeDrawableAt
     * @see .removeDrawable
     */
    fun removeDrawableById(id: Int): Drawable? {
        val drawables: List<DrawableInfo> = mDrawables
        for (i in drawables.indices) {
            if (drawables[i].mId == id) {
                return removeDrawableAt(i)
            }
        }
        return null
    }

    /**
     * 通过索引移除drawable.
     *
     * @param index 索引
     * @return 被移除的drawable
     * @throws IndexOutOfBoundsException if the index is out of range
     * (<tt>index &lt; 0 || index &gt;= getNumberOfDrawables()</tt>)
     * @see .getNumberOfDrawables
     * @see .removeDrawable
     * @see .removeDrawableById
     */
    fun removeDrawableAt(index: Int): Drawable? {
        val drawable = mDrawables.removeAt(index)
        if (!hasSameDrawable(drawable.mDrawable)) {
            cleanDrawable(drawable.mDrawable)
        }
        layoutDrawables()
        return drawable.mDrawable
    }

    /**
     * 移除所有的drawable.
     */
    fun clearDrawable() {
        if (mDrawables.isNotEmpty()) {
            for (drawable in mDrawables) {
                cleanDrawable(drawable.mDrawable)
            }
            mDrawables.clear()
            layoutDrawables()
        }
    }

    private fun cleanDrawable(drawable: Drawable?) {
        drawable?.callback = null
        unscheduleDrawable(drawable)
    }

    private fun layoutDrawables() {
        mSteinerCircleRadius = 0f
        mOffsetY = 0f
        val width = width - paddingLeft - paddingRight
        val height = height - paddingTop - paddingBottom
        mContentSize = min(width, height)
        val drawables: List<DrawableInfo> = mDrawables
        val N = drawables.size
        val center = mContentSize * .5f
        if (mContentSize > 0 && N > 0) {
            // 图像圆的半径。
            val r: Float
            when (N) {
                1 -> {
                    r = mContentSize * .5f
                }
                2 -> {
                    r = (mContentSize / (2 + 2 * sin(Math.PI / 4))).toFloat()
                }
                4 -> {
                    r = mContentSize / 4f
                }
                else -> {
                    r = (mContentSize / (2 * (2 * sin(
                        (N - 2) * Math.PI
                                / (2 * N)
                    ) + 1))).toFloat()
                    val sinN = sin(Math.PI / N)
                    // 以所有图像圆为内切圆的圆的半径
                    val R = (r * ((sinN + 1) / sinN)).toFloat()
                    mOffsetY = ((mContentSize - R - r * (1 + 1 / tan(Math.PI / N))) / 2f).toFloat()
                }
            }
            mSteinerCircleRadius = r
            val startX: Float
            val startY: Float
            if (N % 2 == 0) {
                startY = r
                startX = startY
            } else {
                startX = center
                startY = r
            }
            val matrix = mLayoutMatrix
            val pointsTemp = mPointsTemp
            matrix.reset()
            for (i in drawables.indices) {
                val drawable = drawables[i]
                drawable.reset()
                drawable.mHasGap = i > 0
                if (drawable.mHasGap) {
                    drawable.mGapCenterX = pointsTemp[0]
                    drawable.mGapCenterY = pointsTemp[1]
                }
                pointsTemp[0] = startX
                pointsTemp[1] = startY
                if (i > 0) {
                    // 以上一个圆的圆心旋转计算得出当前圆的圆位置
                    matrix.postRotate(360f / N, center, center + mOffsetY)
                    matrix.mapPoints(pointsTemp)
                }
                drawable.mCenterX = pointsTemp[0]
                drawable.mCenterY = pointsTemp[1]
                updateDrawableBounds(drawable)
                drawable.mMaskPath.addCircle(
                    drawable.mCenterX, drawable.mCenterY, r,
                    Path.Direction.CW
                )
                drawable.mMaskPath.fillType = Path.FillType.INVERSE_WINDING
            }
            if (N > 2) {
                val first = drawables[0]
                val last = drawables[N - 1]
                first.mHasGap = true
                first.mGapCenterX = last.mCenterX
                first.mGapCenterY = last.mCenterY
            }
        }
        invalidate()
    }

    private fun updateDrawableBounds(drawableInfo: DrawableInfo) {
        val drawable = drawableInfo.mDrawable
        val radius = mSteinerCircleRadius
        if (radius <= 0) {
            drawable?.setBounds(0, 0, 0, 0)
            return
        }
        val dWidth = drawable!!.intrinsicWidth
        val dHeight = drawable.intrinsicHeight
        val bounds = mTempBounds
        bounds.setEmpty()
        if (dWidth <= 0 || dHeight <= 0 || dWidth == dHeight || FitType.FIT === fitType) {
            bounds.inset(-radius, -radius)
        } else {
            val scale: Float = if (dWidth > dHeight) {
                radius / dHeight.toFloat()
            } else {
                radius / dWidth.toFloat()
            }
            bounds.inset(-dWidth * scale, -dHeight * scale)
            if (FitType.START === fitType || FitType.END === fitType) {
                val dir = if (FitType.START === fitType) 1 else -1
                bounds.offset(
                    (bounds.width() * 0.5f - radius) * dir,
                    (bounds.height() * 0.5f - radius) * dir
                )
            }
        }
        bounds.offset(drawableInfo.mCenterX, drawableInfo.mCenterY)
        drawable.setBounds(
            bounds.left.toInt(), bounds.top.toInt(),
            bounds.right.roundToInt(), bounds.bottom.roundToInt()
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        layoutDrawables()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val drawables: List<DrawableInfo> = mDrawables
        val N = drawables.size
        if (!isInEditMode && (mContentSize <= 0 || N <= 0)) {
            return
        }
        canvas.translate(paddingLeft.toFloat(), paddingTop.toFloat())
        val width = width - paddingLeft - paddingRight
        val height = height - paddingTop - paddingBottom
        if (width > height) {
            canvas.translate((width - height) * .5f, 0f)
        } else {
            canvas.translate(0f, (height - width) * .5f)
        }
        if (isInEditMode) {
            val cr = min(width, height) * .5f
            canvas.drawCircle(cr, cr, cr, mPaint)
            return
        }
        canvas.translate(0f, mOffsetY)
        val paint = mPaint
        val gapRadius = mSteinerCircleRadius * (mGap + 1f)
        for (i in drawables.indices) {
            val drawable = drawables[i]
            val savedLayer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                canvas.saveLayer(
                    0f, 0f, mContentSize.toFloat(), mContentSize.toFloat(),
                    null//, Canvas.ALL_SAVE_FLAG
                )
            } else {
                canvas.saveLayer(
                    0f, 0f, mContentSize.toFloat(), mContentSize.toFloat(),
                    null, Canvas.ALL_SAVE_FLAG
                )
            }
            drawable.mDrawable?.draw(canvas)
            canvas.drawPath(drawable.mMaskPath, paint)
            if (drawable.mHasGap && mGap > 0f) {
                canvas.drawCircle(drawable.mGapCenterX, drawable.mGapCenterY, gapRadius, paint)
            }
            canvas.restoreToCount(savedLayer)
        }
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        updateVisible()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        updateVisible()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        updateVisible()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateVisible()
    }

    private fun updateVisible() {
        val isVisible = windowVisibility == VISIBLE && isShown
        for (drawable in mDrawables) {
            drawable.mDrawable?.setVisible(isVisible, false)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        for (drawable in mDrawables) {
            drawable.mDrawable?.setVisible(false, false)
        }
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        var invalidate = false
        for (drawable in mDrawables) {
            val d = drawable.mDrawable
            if (d?.isStateful == true && d.setState(drawableState)) {
                invalidate = true
            }
        }
        if (invalidate) {
            invalidate()
        }
    }

    override fun jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState()
        for (drawable in mDrawables) {
            drawable.mDrawable?.jumpToCurrentState()
        }
    }

    override fun verifyDrawable(drawable: Drawable): Boolean {
        return hasSameDrawable(drawable) || super.verifyDrawable(drawable)
    }

    override fun invalidateDrawable(drawable: Drawable) {
        if (hasSameDrawable(drawable)) {
            invalidate()
        } else {
            super.invalidateDrawable(drawable)
        }
    }

    override fun getAccessibilityClassName(): CharSequence {
        return CompositionAvatarView::class.java.name
    }

    companion object {
        const val MAX_DRAWABLE_COUNT = 5
        const val DEFAULT_GAP = 0.25f
        private val sFitTypeArray = arrayOf(
            FitType.FIT,
            FitType.CENTER,
            FitType.START,
            FitType.END
        )
    }
}