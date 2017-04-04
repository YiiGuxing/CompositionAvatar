package cn.yiiguxing.sample;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatDrawableManager;
import android.view.animation.AccelerateDecelerateInterpolator;

import cn.yiiguxing.compositionavatar.CompositionAvatarView;
import cn.yiiguxing.sample.databinding.ActivitySampleBinding;

public class SampleActivity extends AppCompatActivity {

    private Handler mHandler = new Handler();
    private Runnable mDynamicDrawablesRunnable;
    private Animator mGapAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivitySampleBinding binding = DataBindingUtil.setContentView(this,
                R.layout.activity_sample);

        gif(binding.gifUse);
        svg(binding.svgUse);
        dynamicDrawables(binding.dynamicDrawables);
        dynamicGap(binding.dynamicGap);
    }

    private void gif(CompositionAvatarView view) {
        BindingUtil.loadDrawable(view, R.drawable.image_1, R.drawable.galaxy, R.drawable.nebula);
    }

    private void svg(CompositionAvatarView view) {
        AppCompatDrawableManager drawableManager = AppCompatDrawableManager.get();
        view.addDrawable(drawableManager.getDrawable(this, R.drawable.svg_cloud_circle));
        view.addDrawable(drawableManager.getDrawable(this, R.drawable.svg_album));
        view.addDrawable(drawableManager.getDrawable(this, R.drawable.svg_group_work));
    }

    @SuppressWarnings("deprecation")
    private void dynamicDrawables(CompositionAvatarView view) {
        Resources resources = getResources();
        Drawable[] drawables = {
                resources.getDrawable(R.drawable.image_1),
                resources.getDrawable(R.drawable.image_2),
                resources.getDrawable(R.drawable.image_3),
                resources.getDrawable(R.drawable.image_4),
                resources.getDrawable(R.drawable.image_5)
        };

        view.addDrawable(drawables[0]);

        mDynamicDrawablesRunnable = new Runnable() {
            boolean reverse = false;

            @Override
            public void run() {
                if (!reverse) {
                    view.addDrawable(drawables[view.getNumberOfDrawables()]);
                    if (view.getNumberOfDrawables() == drawables.length)
                        reverse = true;
                } else {
                    view.removeDrawableAt(view.getNumberOfDrawables() - 1);
                    if (view.getNumberOfDrawables() == 1)
                        reverse = false;
                }

                mHandler.postDelayed(this, 1500);
            }
        };
    }

    private void dynamicGap(CompositionAvatarView view) {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(2000);
        animator.setStartDelay(1000);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(animation -> view.setGap((Float) animation.getAnimatedValue()));

        mGapAnimator = animator;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHandler.postDelayed(mDynamicDrawablesRunnable, 1500);
        mGapAnimator.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mDynamicDrawablesRunnable);
        mGapAnimator.cancel();
    }
}
