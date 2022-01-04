package cn.yiiguxing.compositionavatar

import android.graphics.Path
import android.view.View
import android.graphics.drawable.Drawable

class DrawableInfo {
    @JvmField
    var mId = View.NO_ID
    @JvmField
    var mDrawable: Drawable? = null
    @JvmField
    var mCenterX = 0f
    @JvmField
    var mCenterY = 0f
    @JvmField
    var mGapCenterX = 0f
    @JvmField
    var mGapCenterY = 0f
    @JvmField
    var mHasGap = false
    @JvmField
    val mMaskPath = Path()
    fun reset() {
        mCenterX = 0f
        mCenterY = 0f
        mGapCenterX = 0f
        mGapCenterY = 0f
        mHasGap = false
        mMaskPath.reset()
    }
}