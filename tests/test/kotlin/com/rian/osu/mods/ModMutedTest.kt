package com.rian.osu.mods

import com.rian.osu.mods.settings.*
import org.junit.Assert
import org.junit.Test

class ModMutedTest {
    @Test
    fun `Test inverse muting affecting mute combo count minimum value`() {
        ModMuted().apply {
            inverseMuting = false

            Assert.assertEquals(
                0,
                getModSettingDelegate<IntegerModSetting>(::muteComboCount).minValue
            )

            inverseMuting = true

            Assert.assertEquals(
                1,
                getModSettingDelegate<IntegerModSetting>(::muteComboCount).minValue
            )
        }
    }

    @Test
    fun `Test volumeAt`() {
        ModMuted().apply {
            inverseMuting = false
            muteComboCount = 100

            Assert.assertEquals(1f, volumeAt(-1), 1e-2f)
            Assert.assertEquals(1f, volumeAt(0), 1e-2f)
            Assert.assertEquals(0.75f, volumeAt(25), 1e-2f)
            Assert.assertEquals(0f, volumeAt(100), 1e-2f)
            Assert.assertEquals(0f, volumeAt(150), 1e-2f)

            inverseMuting = true

            Assert.assertEquals(0f, volumeAt(-1), 1e-2f)
            Assert.assertEquals(0f, volumeAt(0), 1e-2f)
            Assert.assertEquals(0.5f, volumeAt(50), 1e-2f)
            Assert.assertEquals(1f, volumeAt(100), 1e-2f)
            Assert.assertEquals(1f, volumeAt(150), 1e-2f)
        }
    }
}