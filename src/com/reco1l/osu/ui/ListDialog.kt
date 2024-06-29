package com.reco1l.osu.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.forEach
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.reco1l.toolkt.android.cornerRadius
import com.reco1l.toolkt.android.dp
import com.reco1l.toolkt.android.drawableRight
import ru.nsu.ccfit.zuev.osuplus.R


/**
 * Represents an option in a select dialog.
 */
data class Option(

    /**
     * The text to be displayed in the option.
     */
    val text: String,

    /**
     * The value to be returned when the option is selected.
     */
    val value: Any
)


open class SelectDialog : MessageDialog() {


    override val layoutID = R.layout.dialog_select_fragment


    var options = mutableListOf<Option>()
        protected set(value) {

            if (value.none { it.value == selected }) {
                selected = null
            }

            field = value

            if (::recyclerView.isInitialized) {
                recyclerView.adapter!!.notifyDataSetChanged()
            }
        }


    protected var selected: Any? = null

    protected var onSelect: ((Any) -> Unit)? = null


    private lateinit var recyclerView: RecyclerView


    override fun onLoadView() {
        super.onLoadView()

        recyclerView = findViewById<RecyclerView>(R.id.list)!!.apply {

            layoutManager = LinearLayoutManager(context)
            adapter = Adapter()

        }
    }


    fun setOptions(options: MutableList<Option>): SelectDialog {
        this.options = options
        return this
    }

    /**
     * Add an option to the dialog.
     */
    fun addOption(text: String, value: Any): SelectDialog {
        options.add(Option(text, value))
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
    fun setOnSelectListener(onSelect: (Any) -> Unit): SelectDialog {
        this.onSelect = onSelect
        return this
    }


    private fun unselectAll() {
        recyclerView.forEach { (recyclerView.getChildViewHolder(it) as ViewHolder).unselect() }
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


        override fun getItemCount() = options.size

        override fun getItemId(position: Int) = position.toLong()

    }

    private inner class ViewHolder(val text: TextView) : RecyclerView.ViewHolder(text) {

        fun bind(option: Option) {
            text.text = option.text
            text.setOnClickListener {

                unselectAll()

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