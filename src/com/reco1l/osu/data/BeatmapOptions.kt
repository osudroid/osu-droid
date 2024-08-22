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
     * The beatmap path.
     */
    @PrimaryKey
    val parentPath: String,

    /**
     * Whether the beatmap is marked as favorite.
     */
    var isFavorite: Boolean = false,

    /**
     * The beatmap offset.
     */
    var offset: Int = 0,

)

@Dao interface IBeatmapOptionsDAO {

    @Query("SELECT * FROM BeatmapOptions WHERE parentPath = :path")
    fun getOptions(path: String): BeatmapOptions?

    @Update
    fun setOptions(options: BeatmapOptions)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAll(options: List<BeatmapOptions>)

    @Query("DELETE FROM BeatmapOptions")
    fun clearOptions()

}
