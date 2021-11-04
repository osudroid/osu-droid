package com.edlplan.ui.fragment

import android.animation.Animator
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import com.edlplan.framework.easing.Easing
import com.edlplan.framework.utils.Lazy
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.EasingHelper
import com.edlplan.ui.TriangleEffectView
import org.anddev.andengine.util.StreamUtils
import ru.nsu.ccfit.zuev.osuplus.R
import java.io.IOException

class BuildTypeNoticeFragment : BaseFragment() {
    override val layoutID: Int
        get() = R.layout.fragment_build_type_notice

    @RequiresApi(api = Build.VERSION_CODES.O)
    override fun onLoadView() {
        findViewById<TriangleEffectView>(R.id.bg_triangles)!!.setXDistribution { (2f / (1 + Math.exp((Math.random() * 2 - 1) * 10)) - 1).toFloat() }
        findViewById<View>(R.id.button_view_changelist)!!.setOnClickListener {
            var markdown: String?
            try {
                val `in` = requireContext().assets.open("app/changelog.md")
                markdown = StreamUtils.readFully(`in`)
                `in`.close()
            } catch (e: IOException) {
                e.printStackTrace()
                markdown = e.message
            }
            MarkdownFragment()
                    .setTitle(R.string.changelog_title)
                    .setMarkdown(markdown)
                    .show()
        }
        playOnLoadAnim()
    }

    override fun dismiss() {
        playOnDismissAnim(Runnable { super.dismiss() })
    }

    protected fun playOnLoadAnim() {
        val body = findViewById<View>(R.id.frg_body)
        body!!.alpha = 0f
        body.animate().cancel()
        body.animate()
                .alpha(1f)
                .setDuration(1000)
                .start()
        /*View icon = findViewById(R.id.warning_icon);
        Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.warning_rotate);
        anim.setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad));
        icon.startAnimation(anim);*/
        body.postDelayed({ dismiss() }, 6000)
    }

    protected fun playOnDismissAnim(runnable: Runnable?) {
        val body = findViewById<View>(R.id.frg_body)
        body!!.animate().cancel()
        body.animate()
                .scaleX(2f)
                .scaleY(2f)
                .setDuration(1000)
                .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
                .setListener(object : BaseAnimationListener() {
                    override fun onAnimationEnd(animation: Animator) {
                        runnable?.run()
                    }
                })
                .start()
        playBackgroundHideOutAnim(1000)
    }

    companion object {
        @JvmField
        val single = Lazy.create { BuildTypeNoticeFragment() }
    }

    init {
        isDismissOnBackgroundClick = true
        isDismissOnBackPress = true
    }
}