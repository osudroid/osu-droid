package org.andengine.graphics.gles

import org.andengine.engine.Engine

/**
 * Lets AndEngine and app code that reload their own GL objects take part in the
 * [GlesResourceRegistry] restore, as [RestorePhase.Legacy] entries. Entries are keyed by name:
 * registering a name again replaces the action in place (same position), so a recreated activity
 * or engine does not add duplicates.
 */
object GlesLegacyRestorers {

    private val entries = HashMap<String, Entry>()

    /** A snapshot of the registered entry names in no particular order, read by tests. */
    @JvmStatic
    val names: Set<String> get() = synchronized(entries) { HashSet(entries.keys) }

    @JvmStatic
    fun register(name: String, action: Runnable) {
        synchronized(entries) {
            val existing = entries[name]
            if (existing != null) {
                existing.action = action
            } else {
                val entry = Entry(name, action)
                entries[name] = entry
                GlesBackend.registry.register(entry)
            }
        }
    }

    /** Reloads the engine's texture and font managers on every new context. */
    @JvmStatic
    fun registerEngine(engine: Engine) {
        register("texture-manager") { engine.textureManager.onReload() }
        register("font-manager") { engine.fontManager.onReload() }
    }

    /** Removes all entries, so that tests leave no engine registered globally. */
    @JvmStatic
    fun clearForTest() {
        synchronized(entries) {
            entries.values.forEach { GlesBackend.registry.unregister(it) }
            entries.clear()
        }
    }

    private class Entry(val name: String, @Volatile var action: Runnable) : GlesRestorable {
        override val restorePhase: RestorePhase get() = RestorePhase.Legacy

        override fun onContextLost() {}

        override fun restore() = action.run()

        override fun toString() = "Legacy($name)"
    }
}
