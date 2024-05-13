package com.edlplan.ui.fragment

import android.animation.Animator
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.EasingHelper
import ru.nsu.ccfit.zuev.osu.BeatmapProperties
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.PropertiesLibrary
import ru.nsu.ccfit.zuev.osu.menu.IPropsMenu
import ru.nsu.ccfit.zuev.osu.menu.MenuItem
import ru.nsu.ccfit.zuev.osu.menu.SongMenu
import ru.nsu.ccfit.zuev.osu.scoring.ScoreLibrary
import ru.nsu.ccfit.zuev.osuplus.R
import kotlin.math.abs

/// Converted to Kotlin ///
class PropsMenuFragment : BaseFragment(), IPropsMenu {

    var menu: SongMenu? = null
    var item: MenuItem? = null
    var props: BeatmapProperties? = null

    private var offset: EditText? = null
    private var isFav: CheckBox? = null

    init {
        isDismissOnBackgroundClick = true
    }

    override val layoutID: Int
        get() = R.layout.beatmap_options_fragment

    override fun onLoadView() {
        offset = findViewById<EditText>(R.id.offsetBox)
        isFav = findViewById<CheckBox>(R.id.addToFav)

        offset!!.setText(props!!.getOffset().toString())
        isFav!!.isChecked = props!!.isFavorite

        isFav!!.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            props!!.isFavorite = isChecked
            saveProp()
        }

        offset!!.addTextChangedListener(object : TextWatcher {
            var needRest: Boolean = false

            var o: Int = 0

            var pos: Int = 0

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                pos = start
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                try {
                    o = s.toString().toInt()
                    needRest = false
                    if (abs(o.toDouble()) > 250) {
                        o = 250 * (if (o > 0) 1 else -1)
                        needRest = true
                    }
                    if (needRest) {
                        offset!!.removeTextChangedListener(this)
                        offset!!.setText(o.toString())
                        offset!!.setSelection(pos)
                        offset!!.addTextChangedListener(this)
                    }
                    props!!.setOffset(o)
                    saveProp()
                } catch (e: NumberFormatException) {
                    if (s.length == 0) {
                        props!!.setOffset(0)
                        saveProp()
                    }
                }
            }
        })

        findViewById<View>(R.id.offset_plus)!!.setOnClickListener {
            offset!!.setText((offset!!.text.toString().toInt() + 1).coerceIn(-250, 250).toString())
        }

        findViewById<View>(R.id.offset_minus)!!.setOnClickListener {
            offset!!.setText((offset!!.text.toString().toInt() - 1).coerceIn(-250, 250).toString())
        }


        findViewById<View>(R.id.manageFavButton)!!.setOnClickListener { v: View? ->
            val dialog = FavoriteManagerFragment()
            //TODO : 铺面引用还是全局耦合的，需要分离
            dialog.showToAddToFolder(ScoreLibrary.getTrackDir(GlobalManager.getInstance().selectedTrack.filename))
        }

        findViewById<View>(R.id.deleteBeatmap)!!.setOnClickListener { v: View? ->
            val confirm = ConfirmDialogFragment()
            confirm.showForResult { isAccepted: Boolean ->
                if (isAccepted) {
                    if (menu != null) {
                        menu!!.scene.postRunnable { item!!.delete() }
                    }
                    dismiss()
                }
            }
        }

        playOnLoadAnim()
    }

    private fun playOnLoadAnim() {
        val body = findViewById<View>(R.id.fullLayout)
        body!!.alpha = 0f
        body.translationY = 200f
        body.animate().cancel()
        body.animate()
            .translationY(0f)
            .alpha(1f)
            .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
            .setDuration(150)
            .start()
        playBackgroundHideInAnim(150)
    }

    private fun playEndAnim(action: Runnable?) {
        val body = findViewById<View>(R.id.fullLayout)
        body!!.animate().cancel()
        body.animate()
            .translationY(200f)
            .alpha(0f)
            .setDuration(200)
            .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
            .setListener(object : BaseAnimationListener() {
                override fun onAnimationEnd(animation: Animator) {
                    action?.run()
                }
            })
            .start()
        playBackgroundHideOutAnim(200)
    }


    override fun dismiss() {
        playEndAnim { super.dismiss() }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun show(menu: SongMenu, item: MenuItem) {
        this.menu = menu
        this.item = item
        props = PropertiesLibrary.getInstance().getProperties(item.beatmap.path)
        if (props == null) {
            props = BeatmapProperties()
        }
        show()
    }

    fun saveProp() {
        PropertiesLibrary.getInstance().setProperties(
            item!!.beatmap.path, props
        )
        item!!.isFavorite = props!!.favorite
        PropertiesLibrary.getInstance().save()
    }
}
