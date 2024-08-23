package com.reco1l.osu.data

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
    val id: Long = 0,

    var x: Float = 0f,

    var y: Float = 0f,

    var width: Float = 0f,

    var height: Float = 0f

)

@Dao
interface IBlockAreaDAO {

    // Sorting because this will affect the attachment order, newer areas will be on top.
    @Query("SELECT * FROM BlockArea ORDER BY id ASC")
    fun getAll(): List<BlockArea>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(blockArea: BlockArea)

    @Update
    fun update(blockArea: BlockArea)

    @Delete
    fun delete(blockArea: BlockArea)

    @Query("DELETE FROM BlockArea")
    fun deleteAll()

}