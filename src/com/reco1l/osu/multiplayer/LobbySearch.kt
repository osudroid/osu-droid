package com.reco1l.osu.multiplayer

import android.annotation.SuppressLint
import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.OnKeyListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.fragment.BaseFragment
import com.reco1l.framework.*
import com.reco1l.toolkt.android.*
import com.reco1l.toolkt.animation.*
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osuplus.R

class LobbySearch : BaseFragment(), OnEditorActionListener, OnKeyListener {


    override val layoutID = R.layout.multiplayer_lobby_search


    private lateinit var field: EditText


    private val isExtended: Boolean
        get() {
            val layout = findViewById<View?>(R.id.fullLayout) ?: return false
            return layout.translationY == 0f
        }


    init {
        isDismissOnBackPress = false
    }


    override fun onLoadView() {

        reload()

        field = findViewById(R.id.search_field)!!
        field.setOnEditorActionListener(this)
        field.setOnKeyListener(this)
    }

    override fun onEditorAction(view: TextView?, actionId: Int, event: KeyEvent?): Boolean {

        if (actionId == EditorInfo.IME_ACTION_SEND) {
            hideKeyboard()
            LobbyScene.searchQuery = view?.text?.toString()
            return true
        }

        return false
    }

    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {

        if (keyCode == KeyEvent.KEYCODE_ENTER && v is EditText) {
            return onEditorAction(v, EditorInfo.IME_ACTION_SEND, event)
        }

        return false
    }


    private fun hideKeyboard() {

        field.clearFocus()

        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(field.windowToken, 0)
    }

    private fun reload() {

        findViewById<View>(R.id.showMoreButton)?.setOnTouchListener { v: View, event: MotionEvent ->

            if (event.action == TouchEvent.ACTION_DOWN) {

                v.animate().cancel()
                v.animate().scaleY(0.9f).scaleX(0.9f).translationY(v.height * 0.1f).setDuration(100).start()
                toggleVisibility()

                return@setOnTouchListener true

            } else if (event.action == TouchEvent.ACTION_UP) {

                v.animate().cancel()
                v.animate().scaleY(1f).scaleX(1f).setDuration(100).translationY(0f).start()

                return@setOnTouchListener true
            }

            false
        }

        findViewById<View>(R.id.frg_background)?.isClickable = false
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun toggleVisibility() {

        field.clearFocus()

        val layout = findViewById<View>(R.id.fullLayout)!!
        val background = findViewById<View>(R.id.frg_background)!!

        if (isExtended) {

            layout.clearAnimation()
            layout.toTranslationY(70f.dp, 200, ease = Easing.OutQuad.asTimeInterpolator())

            background.setOnTouchListener(null)
            background.isClickable = false

        } else {

            layout.clearAnimation()
            layout.toTranslationY(0f.dp, 200, ease = Easing.InQuad.asTimeInterpolator())

            background.setOnTouchListener { _, event ->
                if (event.action == TouchEvent.ACTION_DOWN) {
                    if (isExtended) {
                        toggleVisibility()
                        return@setOnTouchListener true
                    }
                }
                false
            }
            background.isClickable = true
        }
    }


    override fun callDismissOnBackPress() {

        if (isExtended) {
            toggleVisibility()
        }

        dismiss()
        LobbyScene.back()
    }
}
