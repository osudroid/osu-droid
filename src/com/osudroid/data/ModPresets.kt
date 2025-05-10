package com.osudroid.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update

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
)

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