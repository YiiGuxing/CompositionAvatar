package cn.yiiguxing.sample

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatDrawableManager
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import cn.yiiguxing.compositionavatar.CompositionAvatarView
import cn.yiiguxing.sample.databinding.ActivitySampleBinding

class SampleActivity : AppCompatActivity() {
    private val mHandler = Handler(Looper.getMainLooper())
    private var mDynamicDrawablesRunnable: Runnable? = null
    private var mGapAnimator: Animator? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivitySampleBinding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_sample
        )
        gif(binding.gifUse)
        vector(binding.vectorUse)
        dynamicDrawables(binding.dynamicDrawables)
        dynamicGap(binding.dynamicGap)
    }

    private fun gif(view: CompositionAvatarView) {
        BindingUtil.asyncLoadDrawable(
            view,
            R.drawable.ambilight,
            R.drawable.nebula,
            R.drawable.galaxy
        )
    }

    @SuppressLint("RestrictedApi")
    private fun vector(view: CompositionAvatarView) {
        val drawableManager = AppCompatDrawableManager.get()
        view.addDrawable(drawableManager.getDrawable(this, R.drawable.cloud_circle))
        view.addDrawable(drawableManager.getDrawable(this, R.drawable.album))
        view.addDrawable(drawableManager.getDrawable(this, R.drawable.group_work))
    }

    private fun dynamicDrawables(view: CompositionAvatarView) {
        val resources = resources
        val drawables = arrayOf(
                ResourcesCompat.getDrawable(resources, R.drawable.image_1,null),
                ResourcesCompat.getDrawable(resources, R.drawable.image_2,null),
                ResourcesCompat.getDrawable(resources, R.drawable.image_3,null),
                ResourcesCompat.getDrawable(resources, R.drawable.image_4,null),
                ResourcesCompat.getDrawable(resources, R.drawable.image_5,null),
            )
        drawables[0]?.let { view.addDrawable(it) }
        mDynamicDrawablesRunnable = object : Runnable {
            var reverse = false
            override fun run() {
                if (!reverse) {
                    drawables[view.numberOfDrawables]?.let { view.addDrawable(it) }
                    if (view.numberOfDrawables == drawables.size) reverse = true
                } else {
                    view.removeDrawableAt(view.numberOfDrawables - 1)
                    if (view.numberOfDrawables == 1) reverse = false
                }
                mHandler.postDelayed(this, 1500)
            }
        }
    }

    private fun dynamicGap(view: CompositionAvatarView) {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 2000
        animator.startDelay = 1000
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.repeatMode = ValueAnimator.REVERSE
        animator.repeatCount = ValueAnimator.INFINITE
        animator.addUpdateListener { animation: ValueAnimator ->
            view.gap = (animation.animatedValue as Float)
        }
        mGapAnimator = animator
    }

    override fun onResume() {
        super.onResume()
        mDynamicDrawablesRunnable?.let { mHandler.postDelayed(it, 1500) }
        mGapAnimator?.start()
    }

    override fun onPause() {
        super.onPause()
        mDynamicDrawablesRunnable?.let { mHandler.removeCallbacks(it) }
        mGapAnimator?.cancel()
    }
}