package cn.yiiguxing.sample;

import android.databinding.BindingAdapter;
import android.graphics.drawable.Drawable;

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

}
