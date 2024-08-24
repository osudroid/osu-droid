package com.reco1l.osu.ui

import android.view.LayoutInflater
import android.view.MotionEvent.*
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.updateLayoutParams
import com.edlplan.ui.fragment.BaseFragment
import com.reco1l.osu.data.BlockArea
import com.reco1l.osu.data.DatabaseManager
import com.reco1l.toolkt.android.dp
import com.reco1l.toolkt.android.removeSelf
import com.rian.osu.math.Vector2
import ru.nsu.ccfit.zuev.osuplus.R


class BlockAreaFragment : BaseFragment() {


    override val layoutID
        get() = if (isEditing) R.layout.block_area_editor_fragment else R.layout.block_area_layer_fragment


    private val toolbarMoveVector = Vector2(0f, 0f)

    private val blockAreaItems = mutableListOf<BlockAreaItem>()


    private var isEditing = true


    override fun onLoadView() {

        DatabaseManager.blockAreaTable.getAll().forEach {
            blockAreaItems.add(BlockAreaItem(it, isEditing))
        }

        if (!isEditing) {
            return
        }

        val toolbarView = findViewById<LinearLayout>(R.id.toolbar)!!

        // Setting the toolbar position to the center of the screen initially.
        toolbarView.post {
            toolbarView.x = (root!!.width - toolbarView.width) / 2f
            toolbarView.y = (root!!.height - toolbarView.height) / 2f
        }

        findViewById<View>(R.id.drag)!!.setOnTouchListener { _, event ->

            when(event.action) {

                ACTION_DOWN -> {
                    toolbarMoveVector.x = event.x
                    toolbarMoveVector.y = event.y
                    true
                }

                ACTION_MOVE -> {
                    val deltaX = event.x - toolbarMoveVector.x
                    val deltaY = event.y - toolbarMoveVector.y

                    toolbarView.x = (toolbarView.x + deltaX).coerceIn(0f, root!!.width - toolbarView.width.toFloat())
                    toolbarView.y = (toolbarView.y + deltaY).coerceIn(0f, root!!.height - toolbarView.height.toFloat())
                    true
                }

                else -> false
            }

        }

        findViewById<View>(R.id.close)!!.setOnClickListener {
            dismiss()
        }

        findViewById<View>(R.id.add)!!.setOnClickListener {
            val blockArea = BlockArea(
                width = 128f.dp,
                height = 128f.dp
            )
            blockAreaItems.add(BlockAreaItem(blockArea, true))
            DatabaseManager.blockAreaTable.insert(blockArea)
        }

        findViewById<View>(R.id.reset)!!.setOnClickListener {
            DatabaseManager.blockAreaTable.deleteAll()

            blockAreaItems.forEach {
                it.itemView.removeSelf()
            }
        }

    }

    fun show(isEditing: Boolean) {
        this.isEditing = isEditing
        isDismissOnBackPress = !isEditing
        interceptBackPress = isEditing
        super.show()
    }


    inner class BlockAreaItem(private val data: BlockArea, isEditing: Boolean) {

        val itemView = LayoutInflater.from(context).inflate(
            if (isEditing) R.layout.block_area_editor_item else R.layout.block_area_layer_item,
            root as ViewGroup,
            false
        )!!


        init {

            val block = itemView.findViewById<View>(R.id.block)!!

            itemView.x = data.x
            itemView.y = data.y
            block.updateLayoutParams {
                width = data.width.toInt()
                height = data.height.toInt()
            }

            if (isEditing) {

                val move = itemView.findViewById<ImageView>(R.id.move)
                val remove = itemView.findViewById<ImageButton>(R.id.remove)
                val resize = itemView.findViewById<ImageButton>(R.id.resize)

                val moveVector = Vector2(0f, 0f)
                val resizeVector = Vector2(0f, 0f)

                // The margin between the block and the actual view bound, because the Remove and Resize
                // buttons are slightly outside of the block we have to account for that when setting limits.
                val blockMargin = 8f.dp

                move.setOnTouchListener { _, event ->

                    when(event.action) {

                        ACTION_DOWN -> {
                            moveVector.x = event.x
                            moveVector.y = event.y
                            true
                        }

                        ACTION_MOVE -> {

                            val deltaX = event.x - moveVector.x
                            val deltaY = event.y - moveVector.y

                            itemView.x = (itemView.x + deltaX).coerceIn(-blockMargin, root!!.width - block.width - blockMargin)
                            itemView.y = (itemView.y + deltaY).coerceIn(-blockMargin, root!!.height - block.height - blockMargin)

                            data.x = itemView.x
                            data.y = itemView.y
                            true
                        }

                        ACTION_UP -> {
                            DatabaseManager.blockAreaTable.update(data)
                            true
                        }

                        else -> false
                    }
                }

                remove.setOnClickListener {
                    itemView.removeSelf()
                    DatabaseManager.blockAreaTable.delete(data)
                }

                resize.setOnTouchListener { _, event ->

                    when(event.action) {

                        ACTION_DOWN -> {
                            resizeVector.x = event.x
                            resizeVector.y = event.y
                            true
                        }

                        ACTION_MOVE -> {

                            val deltaWidth = resizeVector.x - event.x

                            // Inverted because the Y axis starts from the top left corner in Android.
                            val deltaHeight = event.y - resizeVector.y

                            block.updateLayoutParams {
                                width = (block.width + deltaWidth).coerceAtLeast(MINIMUM_AREA_SIZE.dp).toInt()
                                height = (block.height + deltaHeight).coerceAtLeast(MINIMUM_AREA_SIZE.dp).toInt()
                            }

                            data.width = block.width.toFloat()
                            data.height = block.height.toFloat()
                            true
                        }

                        ACTION_UP -> {
                            DatabaseManager.blockAreaTable.update(data)
                            true
                        }

                        else -> false
                    }
                }
            } else {
                // Intercepting touch events.
                block.setOnTouchListener { _, _ -> true }
            }

            (root as ViewGroup).addView(itemView)
        }

    }


    companion object {

        private const val MINIMUM_AREA_SIZE = 64f

    }

}


