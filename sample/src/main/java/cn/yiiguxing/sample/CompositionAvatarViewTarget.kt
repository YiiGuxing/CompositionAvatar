package cn.yiiguxing.sample

import cn.yiiguxing.compositionavatar.CompositionAvatarView
import com.bumptech.glide.request.target.BaseTarget
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.target.SizeReadyCallback
import android.graphics.drawable.Drawable
import com.bumptech.glide.request.Request
import java.lang.Exception
import com.bumptech.glide.request.animation.GlideAnimation

class CompositionAvatarViewTarget internal constructor(
    private val mView: CompositionAvatarView,
    private val mId: Int
) : BaseTarget<GlideDrawable?>() {

    private var mResource: GlideDrawable? = null
    override fun getRequest(): Request? {
        return mView.getTag(mId) as Request?
    }

    override fun setRequest(request: Request?) {
        mView.setTag(mId, request)
    }

    override fun getSize(cb: SizeReadyCallback?) {
        // FIXME 这里为了图方面，直接加载原图了，生产环境上应该是高和宽都取mView.getDrawableSize()。
        // 但是这里直接取的话也不一定能取到正确的值，所以建义在
        // android.view.ViewTreeObserver.OnPreDrawListener中做处理。
        // 另外，DrawableSize会因图片数量改变而改变，所以建义异步加载图像之前
        // 应当先设置占位图。如果图片的数量是动态可变的的话，也建义做针对性处理。
        cb?.onSizeReady(SIZE_ORIGINAL, SIZE_ORIGINAL)
    }

    override fun onLoadStarted(placeholder: Drawable?) {
        setDrawable(placeholder)
    }

    override fun onLoadFailed(e: Exception, errorDrawable: Drawable?) {
        setDrawable(errorDrawable)
    }

    override fun onLoadCleared(placeholder: Drawable?) {
        setDrawable(placeholder)
    }

    private fun setDrawable(drawable: Drawable?) {
        if (drawable != null) {
            mView.addDrawable(mId, drawable)
        }
    }

    override fun onResourceReady(
        resource: GlideDrawable?,
        glideAnimation: GlideAnimation<in GlideDrawable?>?
    ) {
        if(resource!=null) {
            mResource = resource
            setDrawable(resource)
            resource.setLoopCount(GlideDrawable.LOOP_FOREVER)
            resource.start()
        }
    }

    override fun onStart() {
        if (mResource != null) {
            mResource?.start()
        }
    }

    override fun onStop() {
        if (mResource != null) {
            mResource?.stop()
        }
    }
}