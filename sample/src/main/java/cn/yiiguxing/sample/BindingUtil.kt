package cn.yiiguxing.sample

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.databinding.BindingAdapter
import cn.yiiguxing.compositionavatar.CompositionAvatarView
import com.bumptech.glide.Glide
import kotlin.math.min

object BindingUtil {
    @JvmStatic
    fun getDrawables(vararg drawables: Drawable): Array<out Drawable> {
        return drawables
    }

    @JvmStatic
    @BindingAdapter("drawables")
    fun addDrawable(view: CompositionAvatarView, vararg drawables: Drawable?) {
        for (drawable in drawables) {
            view.addDrawable(drawable!!)
        }
    }

    /**
     * drawable id，用于标识drawable，以便通过此id找到或者替换drawable。
     *
     * @see CompositionAvatarView.addDrawable
     * @see CompositionAvatarView.findDrawableById
     */
    private val DRAWABLE_IDS = intArrayOf(
        R.id.composition_drawable_1,
        R.id.composition_drawable_2,
        R.id.composition_drawable_3,
        R.id.composition_drawable_4,
        R.id.composition_drawable_5
    )

    /**
     * 异步加载图片
     *
     * @param view   the view
     * @param resIds 图片资源ID
     */
    @JvmStatic
    fun asyncLoadDrawable(view: CompositionAvatarView, vararg resIds: Int) {
        val length = min(resIds.size, CompositionAvatarView.MAX_DRAWABLE_COUNT)
        for (i in 0 until length) {
            // 图片布局的顺序是按添加的顺序以顺时针方向布局的，所以可以添加点位图以固定图片的显示顺序
            view.addDrawable(DRAWABLE_IDS[i], ColorDrawable(-0x222223))
            Glide.with(view.context)
                .fromResource()
                .load(resIds[i]) // .placeholder(new ColorDrawable(0xffdddddd)) // 由于异步，这里并不能保证显示的顺序
                .into(CompositionAvatarViewTarget(view, DRAWABLE_IDS[i]))
        }
    }
}