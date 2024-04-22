package com.reco1l.legacy.ui.beatmapdownloader

import android.graphics.BitmapFactory
import android.util.Log
import android.view.*
import android.view.View.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.widget.TextView.OnEditorActionListener
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.edlplan.ui.fragment.BaseFragment
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.reco1l.framework.bass.URLBassStream
import com.reco1l.framework.extensions.forEach
import com.reco1l.framework.lang.mainThread
import com.reco1l.framework.net.IDownloaderObserver
import com.reco1l.framework.net.JsonRequester
import com.reco1l.framework.net.QueryContent
import com.reco1l.legacy.ui.OsuColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.nsu.ccfit.zuev.audio.Status
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.MainScene.MusicOption
import ru.nsu.ccfit.zuev.osuplus.R
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*


object BeatmapListing : BaseFragment(),
    IDownloaderObserver,
    OnEditorActionListener,
    OnKeyListener {


    var mirror = BeatmapMirror.OSU_DIRECT

    var isPlayingMusic = false
        private set


    override val layoutID = R.layout.beatmap_downloader_fragment


    private val adapter = BeatmapSetAdapter()

    private val searchScope = CoroutineScope(Dispatchers.IO)

    private val scrollListener = object : OnScrollListener() {

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {

            // When the end is reach, load more.
            if (!recyclerView.canScrollVertically(1) && dy > 0) {
                offset += 50
                search(true)
            }

            super.onScrolled(recyclerView, dx, dy)
        }
    }


    private var pendingRequest: Job? = null

    private var offset = 0


    private lateinit var indicator: CircularProgressIndicator

    private lateinit var recyclerView: RecyclerView

    private lateinit var searchBox: EditText


    override fun onLoadView() {


        recyclerView = findViewById(R.id.beatmap_list)!!
        recyclerView.layoutManager = GridLayoutManager(context, 2)
        recyclerView.addOnScrollListener(scrollListener)
        recyclerView.adapter = adapter

        searchBox = findViewById(R.id.search)!!
        searchBox.setOnEditorActionListener(this)
        searchBox.setOnKeyListener(this)

        indicator = findViewById(R.id.indicator)!!

        val close = findViewById<ImageButton>(R.id.close)!!
        close.setOnClickListener {
            dismiss()
        }

        search(false)
    }


    fun search(keepData: Boolean) {

        mainThread { indicator.visibility = VISIBLE }

        pendingRequest?.cancel()
        pendingRequest = searchScope.launch {

            if (!keepData) {
                offset = 0
                adapter.data.clear()
                mainThread { adapter.notifyDataSetChanged() }
            }

            JsonRequester(mirror.search.endpoint).use { request ->

                request.query = QueryContent().apply {

                    put("mode", 0)
                    put("query", searchBox.text)
                    put("offset", offset)
                }

                val beatmapSets = request.executeAndGetJson().toArray()!!
                beatmapSets.forEach { beatmapSet ->
                    adapter.data.add(
                        mirror.search.mapResponse(
                            beatmapSet
                        )
                    )
                }

                mainThread {
                    adapter.notifyItemRangeChanged(offset, 50)
                    indicator.visibility = GONE
                }
            }

            pendingRequest = null
        }
    }

    fun stopPreviews(shouldResumeMusic: Boolean) {
        if (!::recyclerView.isInitialized) {
            return
        }

        recyclerView.forEach { view ->

            val holder = recyclerView.getChildViewHolder(view) as BeatmapSetViewHolder
            holder.stopPreview(shouldResumeMusic)
        }
    }


    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {

        if (actionId == EditorInfo.IME_ACTION_SEND) {
            search(false)
            return true
        }

        return false
    }

    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {

        if (keyCode == KeyEvent.KEYCODE_ENTER && v is TextView) {
            onEditorAction(v, EditorInfo.IME_ACTION_SEND, event)
            return true
        }

        return false
    }


    override fun show() {
        isPlayingMusic = GlobalManager.getInstance().songService.status == Status.PLAYING
        super.show()
    }

    override fun dismiss() {

        stopPreviews(true)

        pendingRequest?.cancel()
        pendingRequest = null

        offset = 0
        adapter.data.clear()

        mainThread {
            searchBox.text = null
            adapter.notifyDataSetChanged()
            super.dismiss()
        }
    }
}



// Information

class BeatmapSetDetails(val beatmapSet: BeatmapSetModel, val holder: BeatmapSetViewHolder) :
    BaseFragment() {


    override val layoutID = R.layout.beatmap_downloader_details


    lateinit var previewButton: Button


    private lateinit var cover: ImageView

    private lateinit var status: TextView

    private lateinit var metadata: TextView

    private lateinit var details: TextView

    private lateinit var downloadButton: Button

    private lateinit var creator: TextView

    private lateinit var difficulty: LinearLayout


    init {
        holder.detailsFragment = this
    }


    override fun onLoadView() {

        (root as ViewGroup)[0].apply {
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
        }

        cover = findViewById(R.id.cover)!!
        status = findViewById(R.id.status)!!
        details = findViewById(R.id.details)!!
        creator = findViewById(R.id.creator)!!
        metadata = findViewById(R.id.metadata)!!
        difficulty = findViewById(R.id.difficulty)!!
        previewButton = findViewById(R.id.preview_button)!!
        downloadButton = findViewById(R.id.download_button)!!

        status.text = holder.status.text
        creator.text = holder.creator.text
        metadata.setText(holder.metadata.text, TextView.BufferType.SPANNABLE)


        val beatmaps = beatmapSet.beatmaps
        for (i in beatmaps.indices) {

            val beatmap = beatmaps[i]
            val button = TextView(ContextThemeWrapper(context, R.style.beatmap_difficulty_icon))
            difficulty.addView(button)

            button.setTextColor(OsuColors.getStarRatingColor(beatmap.starRating))
            button.setOnClickListener { selectDifficulty(button, beatmap) }

        }
        selectDifficulty(difficulty[0] as TextView, beatmaps[0])


        // If it's already playing when shown.
        if (holder.previewStream != null) {
            previewButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.pause_24px, 0, 0, 0)
        }

        previewButton.setOnClickListener {

            if (holder.previewStream == null) {
                BeatmapListing.stopPreviews(false)
                holder.playPreview(beatmapSet)
            } else {
                holder.stopPreview(true)
            }
        }

        downloadButton.setOnClickListener {
            val url = BeatmapListing.mirror.downloadEndpoint(beatmapSet.id)
            BeatmapDownloader.download(url, "${beatmapSet.id} ${beatmapSet.artist} - ${beatmapSet.title}.osz")
        }

        cover.setImageDrawable(holder.cover.drawable)
        root!!.setOnClickListener { dismiss() }
    }


    private fun selectDifficulty(button: TextView, beatmap: BeatmapModel) {

        difficulty.forEach {
            if (it == button) {
                it.setBackgroundResource(R.drawable.rounded_rect)
                it.background.setTint(0xFF363653.toInt())
            } else {
                it.background = null
            }
        }

        val sdf = SimpleDateFormat(
            if (beatmap.lengthSec >= 3600) "HH:mm:ss"
            else "mm:ss"
        ).also { it.timeZone = TimeZone.getTimeZone("GMT+0") }

        details.text = """
            ${beatmap.version}
            Star rating: ${beatmap.starRating}
            AR: ${beatmap.ar} - OD: ${beatmap.od} - CS: ${beatmap.cs} - HP: ${beatmap.hp}
            Circles: ${beatmap.circleCount} - Sliders: ${beatmap.sliderCount} - Spinners: ${beatmap.spinnerCount}
            Length: ${sdf.format(beatmap.lengthSec * 1000)} - BPM: ${beatmap.bpm}
        """.trimIndent()
    }


    override fun dismiss() {
        holder.detailsFragment = null
        super.dismiss()
    }

}



// List

class BeatmapSetAdapter : RecyclerView.Adapter<BeatmapSetViewHolder>() {


    var data = mutableListOf<BeatmapSetModel>()


    private val mediaScope = CoroutineScope(Dispatchers.IO)


    init {
        setHasStableIds(true)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeatmapSetViewHolder {

        val inflater = LayoutInflater.from(parent.context)

        return BeatmapSetViewHolder(
            inflater.inflate(R.layout.beatmap_downloader_set_item, parent, false),
            mediaScope
        )
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: BeatmapSetViewHolder, position: Int) {
        holder.bind(data[position])
    }

}

class BeatmapSetViewHolder(itemView: View, private val mediaScope: CoroutineScope)
    : RecyclerView.ViewHolder(itemView) {


    var detailsFragment: BeatmapSetDetails? = null

    var previewStream: URLBassStream? = null


    val cover: ImageView = itemView.findViewById(R.id.cover)

    val status: TextView = itemView.findViewById(R.id.status)

    val creator: TextView = itemView.findViewById(R.id.creator)

    val metadata: TextView = itemView.findViewById(R.id.metadata)

    val difficulty: TextView = itemView.findViewById(R.id.difficulty)

    val previewButton: Button = itemView.findViewById(R.id.preview_button)

    val downloadButton: Button = itemView.findViewById(R.id.download_button)


    private var coverJob: Job? = null

    private var previewJob: Job? = null


    init {
        itemView.outlineProvider = ViewOutlineProvider.BACKGROUND
        itemView.clipToOutline = true
    }


    fun bind(beatmapSet: BeatmapSetModel) {

        status.text = beatmapSet.status.capitalize()
        creator.text = "Mapped by ${beatmapSet.creator}"


        metadata.setText(buildSpannedString {

            append(if (Config.isForceRomanized()) beatmapSet.title else beatmapSet.titleUnicode)
            appendLine()
            color(0xFFB2B2CC.toInt()) {
                append(if (Config.isForceRomanized()) beatmapSet.artist else beatmapSet.artistUnicode)
            }

        }, TextView.BufferType.SPANNABLE)

        difficulty.setText(buildSpannedString {

            val beatmaps = beatmapSet.beatmaps

            for (i in beatmaps.indices) {
                val beatmap = beatmaps[i]

                color(OsuColors.getStarRatingColor(beatmap.starRating)) {
                    append("⦿")
                }

                if (i != beatmaps.size - 1) {
                    append(" ")
                }
            }

        }, TextView.BufferType.SPANNABLE)

        coverJob?.cancel()

        if (beatmapSet.thumbnail != null) {
            coverJob = mediaScope.launch {

                try {
                    URL(beatmapSet.thumbnail).openStream().use {
                        val bitmap = BitmapFactory.decodeStream(it)

                        mainThread { cover.setImageBitmap(bitmap) }
                    }

                } catch (e: Exception) {
                    Log.e("BeatmapDownloader", "Failed to load cover.", e)

                    mainThread { cover.setImageDrawable(null) }
                }

                coverJob = null
            }
        } else {
            cover.setImageDrawable(null)
        }

        previewButton.setOnClickListener {

            if (previewStream == null) {
                BeatmapListing.stopPreviews(false)
                playPreview(beatmapSet)
            } else {
                stopPreview(true)
            }
        }

        downloadButton.setOnClickListener {
            val url = BeatmapListing.mirror.downloadEndpoint(beatmapSet.id)
            BeatmapDownloader.download(url, "${beatmapSet.id} ${beatmapSet.artist} - ${beatmapSet.title}.osz")
        }


        itemView.setOnClickListener {
            BeatmapSetDetails(beatmapSet, this).show()
        }
    }


    fun playPreview(beatmapSet: BeatmapSetModel) {
        previewJob = mediaScope.launch {

            BeatmapListing.stopPreviews(true)

            try {
                previewStream = URLBassStream(BeatmapListing.mirror.previewEndpoint(beatmapSet.beatmaps[0].id)) {
                    stopPreview(true)

                    if (BeatmapListing.isPlayingMusic) {
                        GlobalManager.getInstance().mainScene.musicControl(MusicOption.PLAY)
                    }
                }

                GlobalManager.getInstance().mainScene.musicControl(MusicOption.PAUSE)

                previewStream!!.setVolume(Config.getBgmVolume())
                previewStream!!.play()

                mainThread {
                    previewButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.pause_24px, 0, 0, 0)
                    detailsFragment?.previewButton?.setCompoundDrawablesWithIntrinsicBounds(R.drawable.pause_24px, 0, 0, 0)
                }

            } catch (e: Exception) {
                Log.e("BeatmapListing", "Failed to load preview", e)
            }

            previewJob = null
        }
    }


    fun stopPreview(shouldResumeMusic: Boolean) {

        try {
            previewJob?.cancel()
            previewJob = null
            previewStream?.free()
            previewStream = null

        } catch (e: Exception) {
            Log.e("BeatmapListing", "Failed to stop preview", e)
        }

        mainThread {
            previewButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.play_arrow_24px, 0, 0, 0)
            detailsFragment?.previewButton?.setCompoundDrawablesWithIntrinsicBounds(R.drawable.play_arrow_24px, 0, 0, 0)
        }

        if (shouldResumeMusic && BeatmapListing.isPlayingMusic) {
            GlobalManager.getInstance().mainScene.musicControl(MusicOption.PLAY)
        }
    }

}