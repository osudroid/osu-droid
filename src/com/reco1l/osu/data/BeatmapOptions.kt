package com.reco1l.osu.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import java.io.Serial
import java.io.Serializable

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

    @Update
    fun setOptions(options: BeatmapOptions)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAll(options: List<BeatmapOptions>)

    @Query("DELETE FROM BeatmapOptions")
    fun clearOptions()

}


@Deprecated("This class exist only to be able to migrate old beatmap properties to the new system. Should not be used.")
class BeatmapProperties : Serializable {

    var offset = 0

    var favorite = false

    companion object {
        @Serial
        private const val serialVersionUID = -7229486402310659139L
    }
}

