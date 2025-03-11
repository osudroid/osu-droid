package com.reco1l.osu.data

import android.view.MotionEvent
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update

@Entity
data class BlockArea(

    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,

    var x: Float = 0f,

    var y: Float = 0f,

    var width: Float = 0f,

    var height: Float = 0f

) {

    operator fun contains(event: MotionEvent): Boolean {
        return contains(event.x, event.y)
    }

    fun contains(x: Float, y: Float): Boolean {
        return x >= this.x && x <= this.x + width && y >= this.y && y <= this.y + height
    }

}

@Dao
interface IBlockAreaDAO {

    // Sorting because this will affect the attachment order, newer areas will be on top.
    @Query("SELECT * FROM BlockArea")
    fun getAll(): List<BlockArea>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(blockArea: BlockArea): Long

    @Update
    fun update(blockArea: BlockArea)

    @Delete
    fun delete(blockArea: BlockArea)

    @Query("DELETE FROM BlockArea")
    fun deleteAll()

}