package org.andengine.graphics.gles

import org.andengine.engine.Engine
import org.andengine.opengl.shader.ShaderProgram

/**
 * Bridges the pre-registry context-loss handling of AndEngine and the app into the
 * [GlesResourceRegistry] as [RestorePhase.Legacy] entries. Entries are keyed by name: registering a
 * name again replaces the action in place (same position), so a recreated activity or engine does
 * not add duplicates.
 */
object GlesLegacyRestorers {

    private val entries = HashMap<String, Entry>()

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

    /** What `EngineRenderer.onSurfaceCreated` and `Engine.onReloadResources` used to do per new context. */
    @JvmStatic
    fun registerEngine(engine: Engine) {
        // Programs are reset before the managers reload.
        register("shader-programs") { ShaderProgram.resetAllForContextLoss() }
        register("vbo-manager") { engine.vertexBufferObjectManager.onReload() }
        register("texture-manager") { engine.textureManager.onReload() }
        register("font-manager") { engine.fontManager.onReload() }
        register("shader-program-manager") { engine.shaderProgramManager.onReload() }
    }

    private class Entry(val name: String, @Volatile var action: Runnable) : GlesRestorable {
        override val restorePhase: RestorePhase get() = RestorePhase.Legacy

        override fun onContextLost() {}

        override fun restore(backend: GlesBackend) = action.run()

        override fun toString() = "Legacy($name)"
    }
}
