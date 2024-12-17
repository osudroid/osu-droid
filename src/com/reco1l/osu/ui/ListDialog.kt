package com.reco1l.osu.ui

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.forEach
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.reco1l.toolkt.android.cornerRadius
import com.reco1l.toolkt.android.dp
import com.reco1l.toolkt.android.drawableLeft
import com.reco1l.toolkt.android.drawableRight
import com.reco1l.toolkt.android.layoutHeight
import com.reco1l.toolkt.android.layoutWidth
import ru.nsu.ccfit.zuev.osuplus.R


/**
 * Represents an option in a select dialog.
 */
data class Option(

    /**
     * The text to be displayed in the option.
     */
    val text: CharSequence,

    /**
     * The value to be returned when the option is selected.
     */
    val value: Any?,

    /**
     * The icon to be displayed in the option.
     */
    val icon: Drawable? = null
)


open class SelectDialog : MessageDialog() {


    override val layoutID = R.layout.dialog_select_fragment


    /**
     * The options to be displayed in the dialog.
     */
    var options = mutableListOf<Option>()
        protected set(value) {

            if (value.none { it.value == selected }) {
                selected = null
            }

            field = value

            if (isLoaded) {
                findViewById<RecyclerView>(R.id.list)!!.adapter!!.notifyDataSetChanged()
            }
        }

    /**
     * The selected value.
     */
    var selected: Any? = null
        set(value) {
            field = value
            if (isLoaded) {
                findViewById<RecyclerView>(R.id.list)!!.adapter!!.notifyDataSetChanged()
            }
        }

    /**
     * The function to be called when an option is selected.
     */
    var onSelect: ((Any?) -> Unit)? = null


    override fun onLoadView() {
        super.onLoadView()

        val recyclerView = findViewById<RecyclerView>(R.id.list)!!
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = Adapter()
    }


    /**
     * Set the options to be displayed in the dialog.
     */
    fun setOptions(value: List<Option>): SelectDialog {
        options = value.toMutableList()
        return this
    }

    /**
     * Add an option to the dialog.
     */
    fun addOption(option: Option): SelectDialog {
        options.add(option)
        return this
    }

    /**
     * Set the selected value.
     */
    fun setSelected(value: Any?): SelectDialog {
        selected = value
        return this
    }

    /**
     * Set the function to be called when the option is selected.
     */
    fun setOnSelectListener(onSelect: (Any?) -> Unit): SelectDialog {
        this.onSelect = onSelect
        return this
    }


    private inner class Adapter : RecyclerView.Adapter<ViewHolder>() {


        init {
            setHasStableIds(true)
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.dialog_select_item, parent, false)
            return ViewHolder(view as TextView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(options[position])
        }

        override fun getItemCount(): Int {
            return options.size
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

    }

    private inner class ViewHolder(val text: TextView) : RecyclerView.ViewHolder(text) {

        fun bind(option: Option) {
            text.text = option.text
            text.drawableLeft = option.icon
            text.compoundDrawablePadding = 12.dp

            text.setOnClickListener {

                findViewById<RecyclerView>(R.id.list)!!.apply {
                    forEach {
                        (getChildViewHolder(it) as ViewHolder).unselect()
                    }
                }

                selected = option.value
                select()

                onSelect?.invoke(option.value)
                dismiss()
            }

            if (selected == option.value) {
                select()
            } else {
                unselect()
            }
        }

        fun select() {
            text.setBackgroundColor(0x29F27272)
            text.drawableRight = context!!.getDrawable(R.drawable.check_24px)!!.apply { setTint(0xFFF37373.toInt()) }
            text.cornerRadius = 15f.dp
        }

        fun unselect() {
            text.setBackgroundColor(Color.TRANSPARENT)
            text.drawableRight = null
            text.cornerRadius = 0f
        }

    }
}

/**
 * A select dialog that opens below the caller view.
 */
open class SelectDropdown(private val caller: View) : SelectDialog() {


    override val layoutID = R.layout.dropdown_fragment


    override fun onLoadView() {
        super.onLoadView()

        val body = findViewById<View>(R.id.frg_body)!!
        val callerLocation = IntArray(2).also(caller::getLocationInWindow)

        body.x = callerLocation[0].toFloat()
        body.y = callerLocation[1].toFloat()

        body.post {

            if (body.x + body.width > root!!.width) {
                body.layoutWidth = root!!.width - body.x.toInt()
            } else if (body.width < caller.width) {
                body.layoutWidth = caller.width
            }

            if (body.y + body.height > root!!.height) {
                body.layoutHeight = root!!.height - body.y.toInt()
            }

        }
    }


    override fun addButton(text: String, tint: Int, clickListener: (MessageDialog) -> Unit): SelectDropdown {
        Log.e("SelectDropdown", "This dialog does not support buttons, ignoring.")
        return this
    }

}
