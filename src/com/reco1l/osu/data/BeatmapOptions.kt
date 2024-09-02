package com.reco1l.osu.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update

@Entity
data class BeatmapOptions(

    /**
     * The beatmap set directory.
     */
    @PrimaryKey
    val setDirectory: String,

    /**
     * Whether the beatmap is marked as favorite.
     */
    var isFavorite: Boolean = false,

    /**
     * The beatmap offset.
     */
    var offset: Int = 0

)

@Dao interface IBeatmapOptionsDAO {

    @Query("SELECT * FROM BeatmapOptions WHERE setDirectory = :setDirectory")
    fun getOptions(setDirectory: String): BeatmapOptions?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(options: BeatmapOptions)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(options: List<BeatmapOptions>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(options: BeatmapOptions)

    @Query("DELETE FROM BeatmapOptions")
    fun deleteAll()

}

