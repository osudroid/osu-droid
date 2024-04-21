package com.reco1l.legacy.ui.beatmapdownloader

import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
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
import com.reco1l.framework.extensions.forEach
import com.reco1l.framework.lang.uiThread
import com.reco1l.framework.net.IDownloaderObserver
import com.reco1l.framework.net.JsonRequester
import com.reco1l.framework.net.QueryContent
import com.reco1l.legacy.ui.OsuColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osuplus.R
import java.net.URL


object BeatmapListing : BaseFragment(),
    IDownloaderObserver,
    OnEditorActionListener,
    OnKeyListener {


    /**
     * The mirror to use.
     */
    var mirror = BeatmapMirror.OSU_DIRECT


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

        uiThread { indicator.visibility = VISIBLE }

        pendingRequest?.cancel()
        pendingRequest = searchScope.launch {

            if (!keepData) {
                offset = 0
                adapter.data.clear()
                uiThread { adapter.notifyDataSetChanged() }
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

                uiThread {
                    adapter.notifyItemRangeChanged(offset, 50)
                    indicator.visibility = GONE
                }
            }

            pendingRequest = null
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


    override fun dismiss() {

        pendingRequest?.cancel()
        offset = 0
        adapter.data.clear()

        uiThread {
            searchBox.text = null
            adapter.notifyDataSetChanged()
            super.dismiss()
        }
    }
}



// Information

class BeatmapInformationDetails(

    val beatmapSet: BeatmapSetModel,

    val coverDrawable: Drawable

) : BaseFragment() {


    override val layoutID = R.layout.beatmap_downloader_details

    private lateinit var cover: ImageView
    private lateinit var status: TextView
    private lateinit var metadata: TextView
    private lateinit var details: TextView
    private lateinit var button: Button
    private lateinit var creator: TextView
    private lateinit var difficulty: LinearLayout


    override fun onLoadView() {

        (root as ViewGroup)[0].apply {
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
        }

        cover = findViewById(R.id.cover)!!
        status = findViewById(R.id.status)!!
        metadata = findViewById(R.id.metadata)!!
        details = findViewById(R.id.details)!!
        button = findViewById(R.id.download_button)!!
        creator = findViewById(R.id.creator)!!
        difficulty = findViewById(R.id.difficulty)!!

        status.text = beatmapSet.status.capitalize()
        creator.text = "Mapped by ${beatmapSet.creator}"


        metadata.setText(buildSpannedString {

            append(if (Config.isForceRomanized()) beatmapSet.title else beatmapSet.titleUnicode)
            appendLine()
            color(0xFFB2B2CC.toInt()) {
                append(if (Config.isForceRomanized()) beatmapSet.artist else beatmapSet.artistUnicode)
            }

        }, TextView.BufferType.SPANNABLE)


        val beatmaps = beatmapSet.beatmaps
        for (i in beatmaps.indices) {

            val beatmap = beatmaps[i]
            val button = TextView(ContextThemeWrapper(context, R.style.beatmap_difficulty_icon))
            difficulty.addView(button)

            button.setTextColor(OsuColors.getStarRatingColor(beatmap.starRating.toFloat()))
            button.setOnClickListener { selectDifficulty(button, beatmap) }

        }
        selectDifficulty(difficulty[0] as TextView, beatmaps[0])

        button.setOnClickListener {
            val url = BeatmapListing.mirror.downloadEndpoint(beatmapSet.id)
            BeatmapDownloader.download(url, "${beatmapSet.id} ${beatmapSet.artist} - ${beatmapSet.title}.osz")
        }

        cover.setImageDrawable(coverDrawable)
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

        details.text = """
            ${beatmap.version}
            Star rating: ${beatmap.starRating}
            AR: ${beatmap.ar} - OD: ${beatmap.od} - CS: ${beatmap.cs} - HP drain: ${beatmap.hp}
            Circles: ${beatmap.circleCount} - Sliders: ${beatmap.sliderCount} - Spinners: ${beatmap.spinnerCount}
            Length: ${beatmap.lengthSec} - BPM: ${beatmap.bpm}
        """.trimIndent()
    }

}



// List

class BeatmapSetAdapter : RecyclerView.Adapter<BeatmapSetViewHolder>() {


    var data = mutableListOf<BeatmapSetModel>()


    private val coversScope = CoroutineScope(Dispatchers.IO)


    init {
        setHasStableIds(true)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeatmapSetViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return BeatmapSetViewHolder(inf.inflate(R.layout.beatmap_downloader_set_item, parent, false))
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: BeatmapSetViewHolder, position: Int) {
        holder.bind(data[position], coversScope)
    }

}

class BeatmapSetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val cover: ImageView = itemView.findViewById(R.id.cover)

    private val status: TextView = itemView.findViewById(R.id.status)

    private val metadata: TextView = itemView.findViewById(R.id.metadata)

    private val button: Button = itemView.findViewById(R.id.download_button)

    private val creator: TextView = itemView.findViewById(R.id.creator)

    private val difficulty: TextView = itemView.findViewById(R.id.difficulty)


    private var coverRequest: Job? = null


    init {
        itemView.outlineProvider = ViewOutlineProvider.BACKGROUND
        itemView.clipToOutline = true
    }


    fun bind(beatmapSet: BeatmapSetModel, coversScope: CoroutineScope) {

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

                color(OsuColors.getStarRatingColor(beatmap.starRating.toFloat())) {
                    append("⦿")
                }

                if (i != beatmaps.size - 1) {
                    append(" ")
                }
            }

        }, TextView.BufferType.SPANNABLE)

        coverRequest?.cancel()

        if (beatmapSet.thumbnail != null) {
            coverRequest = coversScope.launch {

                try {
                    URL(beatmapSet.thumbnail).openStream().use {
                        val bitmap = BitmapFactory.decodeStream(it)

                        uiThread { cover.setImageBitmap(bitmap) }
                    }

                } catch (e: Exception) {
                    Log.e("BeatmapDownloader", "Failed to load cover.", e)

                    uiThread { cover.setImageDrawable(null) }
                }

                coverRequest = null
            }
        } else {
            cover.setImageDrawable(null)
        }

        button.setOnClickListener {
            val url = BeatmapListing.mirror.downloadEndpoint(beatmapSet.id)
            BeatmapDownloader.download(url, "${beatmapSet.id} ${beatmapSet.artist} - ${beatmapSet.title}.osz")
        }


        itemView.setOnClickListener {
            BeatmapInformationDetails(beatmapSet, cover.drawable).show()
        }
    }


}