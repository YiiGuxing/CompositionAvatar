package cn.yiiguxing.sample;

import android.databinding.BindingAdapter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BaseTarget;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.SquaringDrawable;
import com.bumptech.glide.request.target.Target;

import cn.yiiguxing.compositionavatar.CompositionAvatarView;

public final class BindingUtil {

    private BindingUtil() {
        //no instance
    }

    public static Drawable[] getDrawables(Drawable... drawables) {
        return drawables;
    }

    @BindingAdapter({"drawables"})
    public static void addDrawable(CompositionAvatarView view, Drawable... drawables) {
        for (Drawable drawable : drawables) {
            view.addDrawable(drawable);
        }
    }

    /**
     * drawable id，用于标识drawable，以便通过此id找到或者替换drawable。
     *
     * @see CompositionAvatarView#addDrawable(int, Drawable)
     * @see CompositionAvatarView#findDrawableById(int) (int, Drawable)
     */
    private static final int[] DRAWABLE_IDS = {
            R.id.composition_drawable_1,
            R.id.composition_drawable_2,
            R.id.composition_drawable_3,
            R.id.composition_drawable_4,
            R.id.composition_drawable_5,
    };

    /**
     * 异步加载图片
     *
     * @param view   the view
     * @param resIds 图片资源ID
     */
    static void loadDrawable(CompositionAvatarView view, int... resIds) {
        int length = Math.min(resIds.length, CompositionAvatarView.MAX_DRAWABLE_COUNT);
        for (int i = 0; i < length; i++) {
            // 图片布局的顺序是按添加的顺序以顺时针方向布局的，所以可以添加点位图以固定布局顺序
            view.addDrawable(DRAWABLE_IDS[i], new ColorDrawable(0xffdddddd));
            Glide.with(view.getContext())
                    .fromResource()
                    .load(resIds[i])
                    // .placeholder(new ColorDrawable(0xffdddddd)) // 由于异步，这里并不能保证布局的顺序
                    .into(new CompositionAvatarViewTarget(view, DRAWABLE_IDS[i]));
        }
    }

    private static class CompositionAvatarViewTarget
            extends BaseTarget<GlideDrawable> {

        private final CompositionAvatarView mView;
        private final int mId;
        private GlideDrawable mResource;

        CompositionAvatarViewTarget(CompositionAvatarView view, int id) {
            mView = view;
            mId = id;
        }

        @Override
        public Request getRequest() {
            return (Request) mView.getTag(mId);
        }

        @Override
        public void setRequest(Request request) {
            mView.setTag(mId, request);
        }

        @Override
        public void getSize(SizeReadyCallback cb) {
            // FIXME 这里为了图方面，直接加载原图了，生产环境上应该是高和宽都取mView.getDrawableSize()。
            // 但是这里直接取的话也不一定能取到正确的值，所以建义在
            // android.view.ViewTreeObserver.OnPreDrawListener中做处理。
            // 另外，DrawableSize会因图片数量改变而改变，所以建义异步加载图像之前
            // 应当先设置占位图。如果图片的数量是动态可变的的话，也建义做针对性处理。
            cb.onSizeReady(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
        }

        @Override
        public void onLoadStarted(Drawable placeholder) {
            setDrawable(placeholder);
        }

        @Override
        public void onLoadFailed(Exception e, Drawable errorDrawable) {
            setDrawable(errorDrawable);
        }

        @Override
        public void onLoadCleared(Drawable placeholder) {
            setDrawable(placeholder);
        }

        private void setDrawable(Drawable drawable) {
            if (drawable != null) {
                mView.addDrawable(mId, drawable);
            }
        }

        @Override
        public void onResourceReady(GlideDrawable resource,
                                    GlideAnimation<? super GlideDrawable> glideAnimation) {
            if (!resource.isAnimated()) {
                resource = new SquaringDrawable(resource, mView.getDrawableSize());
            }
            this.mResource = resource;
            setDrawable(resource);
            resource.setLoopCount(GlideDrawable.LOOP_FOREVER);
            resource.start();
        }

        @Override
        public void onStart() {
            if (mResource != null) {
                mResource.start();
            }
        }

        @Override
        public void onStop() {
            if (mResource != null) {
                mResource.stop();
            }
        }
    }

}
