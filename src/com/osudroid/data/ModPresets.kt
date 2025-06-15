package com.osudroid.data

import androidx.room.*
import com.rian.osu.utils.*

/**
 * Represents a mod preset.
 */
@Entity
data class ModPreset(
    @PrimaryKey(autoGenerate = true)
    @JvmField
    var id: Long = 0,

    /**
     * The name of this [ModPreset].
     */
    @JvmField
    var name: String,

    /**
     * The mods that are in this [ModPreset] in serialized form.
     */
    @JvmField
    var serializedMods: String
) {

    /**
     * The mods that are in this [ModPreset] in deserialized form.
     */
    @Ignore
    lateinit var mods: ModHashMap

}

@Dao
interface IModPresetDAO {
    @Query("SELECT * FROM ModPreset")
    fun getAll(): List<ModPreset>

    @Query("SELECT * FROM ModPreset WHERE id = :id")
    fun getById(id: Long): ModPreset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(modPreset: ModPreset): Long

    @Update
    fun update(modPreset: ModPreset)

    @Delete
    fun delete(modPreset: ModPreset)

    @Query("DELETE FROM ModPreset")
    fun deleteAll()
}