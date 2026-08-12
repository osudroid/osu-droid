package com.osudroid.resources

import android.content.Context
import org.andengine.util.debug.Debug
import ru.nsu.ccfit.zuev.audio.BassSoundProvider
import java.util.concurrent.ConcurrentHashMap

/**
 * A named cache of loaded sounds, safe to read and write from any thread. See [TextureStore]'s
 * documentation for why this matters - the same background-thread-writes-while-update-thread-reads
 * hazard applied to `ResourceManager`'s original `sounds`/`customSounds` `HashMap`s.
 *
 * One [SoundStore] instance is one flat cache; the default-skin/beatmap-skin layering is composed
 * by the caller out of two instances, not built into this class.
 */
class SoundStore {

    // Set via init(), mirroring ResourceManager.Init() - see TextureStore's equivalent for why.
    private lateinit var context: Context

    private val sounds = ConcurrentHashMap<String, BassSoundProvider>()

    fun init(context: Context) {
        this.context = context
    }

    //region Bookkeeping

    fun get(name: String): BassSoundProvider? = sounds[name]

    fun containsKey(name: String): Boolean = sounds.containsKey(name)

    fun put(name: String, sound: BassSoundProvider) {
        sounds[name] = sound
    }

    fun remove(name: String): BassSoundProvider? = sounds.remove(name)

    val keys: Set<String> get() = sounds.keys

    val values: Collection<BassSoundProvider> get() = sounds.values

    fun clear() = sounds.clear()

    //endregion

    /**
     * Loads a sound either from an external file (falling back to the bundled sfx asset of the same
     * short filename on failure) or from the app's bundled assets, registers it under [resname], and
     * returns it. Returns `null` on failure - matching `ResourceManager.loadSound`'s original contract.
     */
    fun load(resname: String, file: String, external: Boolean): BassSoundProvider? {
        val snd = BassSoundProvider()

        if (external) {
            try {
                if (!snd.prepare(file)) {
                    val shortName = file.substring(file.lastIndexOf("/") + 1)
                    if (!snd.prepare(context.assets, "sfx/$shortName")) {
                        return null
                    }
                }
            } catch (e: Exception) {
                Debug.e("SoundStore.load (external): " + e.message, e)
                return null
            }
        } else {
            try {
                if (!snd.prepare(context.assets, file)) {
                    return null
                }
            } catch (e: Exception) {
                Debug.e("SoundStore.load: " + e.message, e)
                return null
            }
        }

        sounds[resname] = snd
        return snd
    }
}
