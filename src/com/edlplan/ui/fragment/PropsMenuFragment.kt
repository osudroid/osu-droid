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
import com.reco1l.osu.data.BeatmapOptions
import com.reco1l.osu.data.DatabaseManager
import com.reco1l.osu.ui.MessageDialog
import com.reco1l.toolkt.android.cornerRadius
import com.reco1l.toolkt.android.dp
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.menu.IPropsMenu
import ru.nsu.ccfit.zuev.osu.menu.BeatmapSetItem
import ru.nsu.ccfit.zuev.osu.menu.SongMenu
import ru.nsu.ccfit.zuev.osuplus.R
import kotlin.math.abs

/// Converted to Kotlin ///
class PropsMenuFragment : BaseFragment(), IPropsMenu {

    var menu: SongMenu? = null
    var item: BeatmapSetItem? = null
    var beatmapOptions: BeatmapOptions? = null

    private var offset: EditText? = null
    private var isFav: CheckBox? = null

    init {
        isDismissOnBackgroundClick = true
    }

    override val layoutID: Int
        get() = R.layout.beatmap_options_fragment

    override fun onLoadView() {
        findViewById<View>(R.id.fullLayout)!!.cornerRadius = 14f.dp

        offset = findViewById<EditText>(R.id.offsetBox)
        isFav = findViewById<CheckBox>(R.id.addToFav)

        offset!!.setText(beatmapOptions!!.offset.toString())
        isFav!!.isChecked = beatmapOptions!!.isFavorite

        isFav!!.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            beatmapOptions!!.isFavorite = isChecked
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
                        o = 250 * if (o > 0) 1 else -1
                        needRest = true
                    }
                    if (needRest) {
                        offset!!.removeTextChangedListener(this)
                        offset!!.setText(o.toString())
                        offset!!.setSelection(pos)
                        offset!!.addTextChangedListener(this)
                    }
                    beatmapOptions!!.offset = o
                    saveProp()
                } catch (e: NumberFormatException) {
                    if (s.length == 0) {
                        beatmapOptions!!.offset = 0
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
            dialog.showToAddToFolder(GlobalManager.getInstance().selectedBeatmap!!.setDirectory)
        }

        findViewById<View>(R.id.deleteBeatmap)!!.setOnClickListener { v: View? ->

            MessageDialog().apply {

                setTitle("Delete beatmap")
                setMessage("Are you sure?")

                addButton("Yes") {
                    if (menu != null) {
                        menu!!.scene.postRunnable { item!!.delete() }
                    }
                    dismiss()
                }

                addButton("No") { it.dismiss() }

            }.show()

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

    override fun show(menu: SongMenu, item: BeatmapSetItem) {
        this.menu = menu
        this.item = item
        beatmapOptions = DatabaseManager.beatmapOptionsTable.getOptions(item.beatmapSetInfo.directory)
        if (beatmapOptions == null) {
            beatmapOptions = BeatmapOptions(item.beatmapSetInfo.directory)
            DatabaseManager.beatmapOptionsTable.insert(beatmapOptions!!)
        }
        show()
    }

    fun saveProp() {
        item!!.isFavorite = beatmapOptions!!.isFavorite
        DatabaseManager.beatmapOptionsTable.update(beatmapOptions!!)
    }
}
