package com.osudroid.multiplayer

import android.util.Log
import com.reco1l.toolkt.kotlin.fromDate
import java.io.BufferedWriter
import java.io.File
import java.text.SimpleDateFormat
import java.util.TimeZone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.MainActivity

/**
 * Logger for multiplayer events.
 */
class MultiplayerLogger : AutoCloseable {
    private val dispatcher = Dispatchers.IO.limitedParallelism(1)
    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    @Volatile
    private var isClosed = false

    private var writer: BufferedWriter? = null

    private val timestampFormat = SimpleDateFormat("HH:mm:ss").apply {
        timeZone = TimeZone.getTimeZone("GMT+0")
    }

    /**
     * Initializes the logger by creating the log file and writing the initial log entry.
     */
    fun init() {
        writer = File("${Config.getDefaultCorePath()}/Log", "multi_log.txt").apply {
            parentFile?.mkdirs()

            if (!exists()) {
                createNewFile()
            }
        }.bufferedWriter()

        write("[${"yyyy/MM/dd hh:mm:ss".fromDate()}] Client ${MainActivity.versionName} started.")
    }

    /**
     * Logs a message to the log file.
     *
     * @param text The message to log.
     */
    fun log(text: String) {
        if (isClosed) {
            return
        }

        val timestamp = timestampFormat.format(System.currentTimeMillis())

        Log.i("Multiplayer", text)
        write("\n[$timestamp] $text")
    }

    /**
     * Logs a [Throwable] to the log file.
     *
     * @param e The [Throwable] to log.
     */
    fun log(e: Throwable) {
        if (isClosed) {
            return
        }

        val time = timestampFormat.format(System.currentTimeMillis())
        val stacktrace = Log.getStackTraceString(e)

        Log.e("Multiplayer", "An exception has been thrown.", e)
        write("\n[$time] EXCEPTION: ${e.javaClass.simpleName}\n$stacktrace")
    }

    /**
     * Flushes the underlying buffer.
     */
    fun flush() {
        if (isClosed) {
            return
        }

        scope.launch {
            try {
                writer?.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun write(str: String) {
        if (isClosed) {
            return
        }

        scope.launch {
            try {
                writer?.write(str)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    override fun close() {
        if (isClosed) {
            return
        }

        isClosed = true

        scope.launch {
            try {
                writer?.flush()
                writer?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}