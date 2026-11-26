package com.osudroid.beatmaplisting

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.OnKeyListener
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.edlplan.ui.fragment.BaseFragment
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.osudroid.BuildSettings
import com.reco1l.framework.net.IFileRequestObserver
import com.reco1l.framework.net.JsonArrayRequest
import com.reco1l.framework.net.UnsuccessfulResponseException
import com.reco1l.framework.net.WebRequest
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.OrderType
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType
import com.osudroid.ui.OsuColors
import com.osudroid.utils.mainThread
import com.reco1l.osu.ui.Option
import com.reco1l.osu.ui.SelectDialog
import com.reco1l.osu.ui.SelectDropdown
import com.reco1l.framework.android.cornerRadius
import com.reco1l.framework.android.dp
import com.reco1l.framework.android.drawableLeft
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osuplus.R
import java.text.SimpleDateFormat
import java.util.TimeZone
import kotlinx.coroutines.CancellationException
import ru.nsu.ccfit.zuev.osu.RankedStatus


class BeatmapListing(private val initialLink: BeatmapLink? = null) : BaseFragment(),
    IFileRequestObserver,
    OnEditorActionListener,
    OnKeyListener {


    override val layoutID = R.layout.beatmap_downloader_fragment


    private val previewPlayer = BeatmapPreviewPlayer()

    private val adapter = BeatmapSetAdapter(previewPlayer)

    private val searchScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val filtersFragment = BeatmapListingFiltersFragment(this)

    private val scrollListener = object : OnScrollListener() {

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {

            // When the end is reach, load more.
            if (lookupLink == null && !recyclerView.canScrollVertically(1) && dy > 0) {
                offset += 50
                search(true)
            }

            super.onScrolled(recyclerView, dx, dy)
        }
    }


    private var pendingRequest: Job? = null

    private var offset = 0

    /**
     * Incremented on the main thread whenever a new [search] or [lookup] starts. Results of requests that started
     * before the latest one are discarded.
     */
    private var requestGeneration = 0

    /**
     * The link currently being looked up. Non-null means the listing is in lookup mode.
     */
    private var lookupLink: BeatmapLink? = null


    private lateinit var indicator: CircularProgressIndicator

    private lateinit var recyclerView: RecyclerView

    private lateinit var retryButton: Button

    private lateinit var searchBox: EditText

    private lateinit var logoView: Button


    init {
        current?.dismiss()

        mainThread {
            current = this
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            previewPlayer.beatmapSetId.collect { adapter.updatePreviewButtons() }
        }
    }


    override fun onLoadView() {

        val itemWidth = resources.getDimension(R.dimen.beatmap_downloader_item_width)
        val itemMargin = resources.getDimension(R.dimen.beatmap_downloader_item_margin) * 2

        val spanCount = resources.displayMetrics.widthPixels / (itemWidth + itemMargin)

        recyclerView = findViewById(R.id.beatmap_list)!!
        recyclerView.layoutManager = GridLayoutManager(context, spanCount.toInt())
        recyclerView.addOnScrollListener(scrollListener)
        recyclerView.adapter = adapter

        retryButton = findViewById(R.id.refresh)!!
        retryButton.setOnClickListener { lookupLink?.let(::lookup) ?: search(true) }

        searchBox = findViewById(R.id.search)!!
        searchBox.setOnEditorActionListener(this)
        searchBox.setOnKeyListener(this)

        indicator = findViewById(R.id.indicator)!!

        logoView = findViewById(R.id.logo)!!
        logoView.text = mirror.description
        logoView.drawableLeft = requireContext().getDrawable(mirror.logoResource)
        logoView.setOnClickListener {
            SelectDialog()
                .setOptions(BeatmapMirror.entries.map { mirror ->
                    Option(
                        text = buildSpannedString {
                            append(mirror.description)
                            appendLine()
                            color(0xBFFFFFFF.toInt()) { append(mirror.homeUrl) }
                        },
                        value = mirror.ordinal,
                        icon = requireContext().getDrawable(mirror.logoResource)
                    )
                })
                .setSelected(mirror.ordinal)
                .setOnSelectListener { value ->
                    value as Int
                    if (value != mirror.ordinal) {
                        mirror = BeatmapMirror.entries[value]

                        logoView.text = mirror.description
                        logoView.drawableLeft = requireContext().getDrawable(mirror.logoResource)

                        lookupLink?.let(::lookup) ?: search(false)
                    }
                }
                .setTitle("Select a beatmap mirror")
                .show()
        }

        findViewById<ImageButton>(R.id.close)!!.setOnClickListener {
            dismiss()
        }

        findViewById<TextView>(R.id.filters)!!.also { filtersButton ->

            filtersButton.setOnClickListener {

                if (!filtersFragment.isAdded) {
                    filtersButton.setBackgroundColor(0x29F27272)
                    filtersButton.cornerRadius = 15f.dp
                    filtersFragment.show()
                } else {
                    filtersButton.setBackgroundColor(Color.TRANSPARENT)
                    filtersButton.cornerRadius = 0f
                    filtersFragment.dismiss()
                }
            }
        }


        initialLink?.let(::lookup) ?: search(false)
    }


    private fun lookup(link: BeatmapLink) {
        lookupLink = link
        adapter.pendingAutoOpen = null

        val generation = ++requestGeneration

        mainThread {
            indicator.visibility = VISIBLE
            retryButton.visibility = GONE
        }

        pendingRequest?.cancel()
        pendingRequest = searchScope.launch(CoroutineExceptionHandler { _, throwable ->

            if (throwable is UnsuccessfulResponseException && throwable.code == 404) {
                mainThread {
                    if (generation != requestGeneration) {
                        return@mainThread
                    }

                    ToastLogger.showText("Beatmap not found on ${mirror.description}.", true)
                    indicator.visibility = GONE
                }

                return@CoroutineExceptionHandler
            }

            mainThread {
                if (generation != requestGeneration) {
                    return@mainThread
                }

                ToastLogger.showText("Failed to connect to server, please check your internet connection.", false)

                Log.e("BeatmapListing", "Failed to connect to beatmap mirror.", throwable)

                indicator.visibility = GONE
                retryButton.visibility = VISIBLE
            }
        }) {
            offset = 0

            mainThread {
                if (generation != requestGeneration) {
                    return@mainThread
                }

                val itemCount = adapter.data.size
                adapter.data.clear()
                adapter.notifyItemRangeRemoved(0, itemCount)
            }

            ensureActive()

            val beatmapSet = when (link) {
                is BeatmapLink.Beatmap -> mirror.handleBeatmapLink(link.beatmapId)
                is BeatmapLink.BeatmapSet -> mirror.handleBeatmapsetLink(link.beatmapSetId)
            }

            ensureActive()

            mainThread {
                if (generation != requestGeneration) {
                    return@mainThread
                }

                if (beatmapSet.beatmaps.isEmpty()) {
                    ToastLogger.showText("This beatmap set has no osu!standard difficulties.", true)
                    indicator.visibility = GONE
                    return@mainThread
                }

                if (!adapter.hasAutoOpened) {
                    adapter.pendingAutoOpen = beatmapSet.id to (link as? BeatmapLink.Beatmap)?.beatmapId
                }

                adapter.data.add(beatmapSet)
                adapter.notifyItemInserted(0)
                indicator.visibility = GONE
            }

            pendingRequest = null
        }
    }

    fun search(keepData: Boolean) {
        lookupLink = null
        adapter.pendingAutoOpen = null

        val generation = ++requestGeneration

        mainThread {
            indicator.visibility = VISIBLE
            retryButton.visibility = GONE
        }

        pendingRequest?.cancel()
        pendingRequest = searchScope.launch(CoroutineExceptionHandler { _, throwable ->

            mainThread {
                if (generation != requestGeneration) {
                    return@mainThread
                }

                ToastLogger.showText("Failed to connect to server, please check your internet connection.", false)

                Log.e("BeatmapListing", "Failed to connect to beatmap mirror.", throwable)

                indicator.visibility = GONE
                retryButton.visibility = VISIBLE
            }
        }) {

            if (!keepData) {
                offset = 0

                mainThread {
                    if (generation != requestGeneration) {
                        return@mainThread
                    }

                    val itemCount = adapter.data.size
                    adapter.data.clear()
                    adapter.notifyItemRangeRemoved(0, itemCount)
                }
            }

            ensureActive()

            JsonArrayRequest(
                mirror.search.request(
                    query = searchBox.text.toString(),
                    offset = offset,
                    limit = 50,
                    sort = filtersFragment.sortType,
                    order = filtersFragment.orderType,
                    status = filtersFragment.rankedStatus
                )
            ).use { request ->

                request.buildRequest { header("User-Agent", "Chrome/Android") }
                ensureActive()

                val beatmapSets = mirror.search.response(request.execute().json)
                ensureActive()

                mainThread {
                    if (generation != requestGeneration) {
                        return@mainThread
                    }

                    // Insert at the current size rather than the mirror's paging offset, since filtering can make pages
                    // shorter than the page size.
                    val start = adapter.data.size
                    adapter.data.addAll(beatmapSets)
                    adapter.notifyItemRangeInserted(start, beatmapSets.size)
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

        mainThread {
            previewPlayer.stop()
            super.dismiss()
            current = null
        }
    }


    companion object {

        /**
         * The current [BeatmapListing] instance.
         */
        var current: BeatmapListing? = null

        /**
         * The current selected beatmap mirror.
         */
        var mirror
            get() = BeatmapMirror.entries[Config.getInt("beatmapMirror", 0)]
            set(value) {
                Config.setInt("beatmapMirror", value.ordinal)
            }

    }

}



// Information

class BeatmapSetDetails(
    val beatmapSet: BeatmapSetModel,
    val holder: BeatmapSetViewHolder,
    private val initialBeatmapId: Long? = null
) : BaseFragment() {


    override val layoutID = R.layout.beatmap_downloader_details


    private lateinit var previewButton: Button


    private lateinit var cover: ImageView

    private lateinit var status: TextView

    private lateinit var metadata: TextView

    private lateinit var details: TextView

    private lateinit var downloadButton: Button

    private lateinit var downloadNoVideoButton: Button

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
        downloadNoVideoButton = findViewById(R.id.download_no_video_button)!!

        status.text = holder.status.text
        creator.text = holder.creator.text
        metadata.setText(holder.metadata.text, TextView.BufferType.SPANNABLE)


        val beatmaps = beatmapSet.beatmaps

        for (i in beatmaps.indices) {
            val beatmap = beatmaps[i]
            val button = TextView(ContextThemeWrapper(context, R.style.beatmap_difficulty_icon))
            difficulty.addView(button)

            button.setTextColor(OsuColors.getStarRatingColor(beatmap.starRating).toInt())
            button.setOnClickListener { selectDifficulty(button, beatmap) }
        }

        val initialIndex = beatmaps.indexOfFirst { it.id == initialBeatmapId }.coerceAtLeast(0)
        selectDifficulty(difficulty[initialIndex] as TextView, beatmaps[initialIndex])

        updatePreviewButton()
        previewButton.setOnClickListener { holder.previewPlayer.toggle(beatmapSet) }

        val suggestedDownloadFilename = "${beatmapSet.id} ${beatmapSet.artist} - ${beatmapSet.title}"

        downloadButton.setOnClickListener {
            BeatmapDownloader.download(
                url = BeatmapListing.mirror.download.request(beatmapSet.id, true).toString(),
                suggestedFilename = suggestedDownloadFilename
            )
        }

        downloadNoVideoButton.apply {
            if (BeatmapListing.mirror.supportsNoVideoDownloads && beatmapSet.hasVideo) {
                visibility = VISIBLE
                setOnClickListener {
                    BeatmapDownloader.download(
                        url = BeatmapListing.mirror.download.request(beatmapSet.id, false).toString(),
                        suggestedFilename = "$suggestedDownloadFilename [no video]"
                    )
                }
            } else {
                visibility = GONE
                setOnClickListener(null)
            }
        }

        cover.setImageDrawable(holder.cover.drawable)
        root!!.setOnClickListener { dismiss() }
    }


    fun setCover(bitmap: Bitmap?) {
        if (::cover.isInitialized) {
            cover.setImageBitmap(bitmap)
        }
    }

    fun updatePreviewButton() {
        if (::previewButton.isInitialized) {
            previewButton.setPreviewIcon(holder.previewPlayer.beatmapSetId.value == beatmapSet.id)
        }
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

        val sdf = SimpleDateFormat(if (beatmap.lengthSec >= 3600) "HH:mm:ss" else "mm:ss").also {

            it.timeZone = TimeZone.getTimeZone("GMT+0")
        }

        details.setText(buildSpannedString {

            append(beatmap.version)
            appendLine()

            color(0xFFB2B2CC.toInt()) {

                append("Star rating: ${beatmap.starRating}")
                appendLine()

                append("AR: ${beatmap.ar} - OD: ${beatmap.od} - CS: ${beatmap.cs} - HP: ${beatmap.hp}")
                appendLine()

                append("Circles: ${beatmap.circleCount} - Sliders: ${beatmap.sliderCount} - Spinners: ${beatmap.spinnerCount}")
                appendLine()

                append("Length: ${sdf.format(beatmap.lengthSec * 1000)} - BPM: ${beatmap.bpm}")
            }

        }, TextView.BufferType.SPANNABLE)
    }


    override fun dismiss() {
        holder.detailsFragment = null
        super.dismiss()
    }

}


// List

class BeatmapSetAdapter(private val previewPlayer: BeatmapPreviewPlayer) : RecyclerView.Adapter<BeatmapSetViewHolder>() {


    var data = mutableListOf<BeatmapSetModel>()

    /**
     * The beatmap set ID and the optional beatmap ID whose details should be opened once the set is bound.
     */
    var pendingAutoOpen: Pair<Long, Long?>? = null

    /**
     * Whether [pendingAutoOpen] has already opened details. Once it has, re-running the lookup (mirror switch, retry)
     * must not reopen details the user has closed.
     */
    var hasAutoOpened = false
        private set


    private val mediaScope = CoroutineScope(Dispatchers.IO)


    init {
        setHasStableIds(true)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeatmapSetViewHolder {

        val inflater = LayoutInflater.from(parent.context)

        return BeatmapSetViewHolder(
            inflater.inflate(R.layout.beatmap_downloader_set_item, parent, false),
            mediaScope,
            previewPlayer
        )
    }


    override fun getItemCount() = data.size

    override fun getItemId(position: Int) = position.toLong()


    override fun onBindViewHolder(holder: BeatmapSetViewHolder, position: Int) {
        val beatmapSet = data[position]
        holder.bind(beatmapSet)

        pendingAutoOpen?.let {
            if (it.first == beatmapSet.id) {
                pendingAutoOpen = null
                hasAutoOpened = true
                BeatmapSetDetails(beatmapSet, holder, it.second).show()
            }
        }
    }

    override fun onBindViewHolder(holder: BeatmapSetViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && payloads.all { it === PreviewChanged }) {
            holder.updatePreviewButtons()
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    /**
     * Updates the preview buttons of every bound card, including cards that are cached off-screen.
     */
    fun updatePreviewButtons() = notifyItemRangeChanged(0, itemCount, PreviewChanged)

    /**
     * A payload that only updates the preview buttons, without binding the whole card again.
     */
    private object PreviewChanged

}

class BeatmapSetViewHolder(
    itemView: View,
    private val mediaScope: CoroutineScope,
    val previewPlayer: BeatmapPreviewPlayer
) : RecyclerView.ViewHolder(itemView) {


    var detailsFragment: BeatmapSetDetails? = null


    val cover: ImageView = itemView.findViewById(R.id.cover)

    val status: TextView = itemView.findViewById(R.id.status)

    val creator: TextView = itemView.findViewById(R.id.creator)

    val metadata: TextView = itemView.findViewById(R.id.metadata)

    val difficulty: TextView = itemView.findViewById(R.id.difficulty)

    val previewButton: Button = itemView.findViewById(R.id.preview_button)

    val downloadButton: Button = itemView.findViewById(R.id.download_button)

    val downloadNoVideoButton: Button = itemView.findViewById(R.id.download_no_video_button)


    private var coverJob: Job? = null

    private var beatmapSetId: Long? = null


    init {
        itemView.outlineProvider = ViewOutlineProvider.BACKGROUND
        itemView.clipToOutline = true
    }


    fun bind(beatmapSet: BeatmapSetModel) {

        beatmapSetId = beatmapSet.id
        status.text = itemView.context.getText(beatmapSet.status.stringId)
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

                color(OsuColors.getStarRatingColor(beatmap.starRating).toInt()) {
                    append("⦿")
                }

                if (i != beatmaps.size - 1) {
                    append(" ")
                }
            }

        }, TextView.BufferType.SPANNABLE)

        coverJob?.cancel()

        if (beatmapSet.thumbnail != null && !BuildSettings.NO_TEXTURES_MODE) {
            coverJob = mediaScope.launch {

                try {
                    WebRequest(beatmapSet.thumbnail).use { request ->
                        request.execute()
                        ensureActive()

                        val bitmap = BitmapFactory.decodeStream(request.responseBody.byteStream())

                        mainThread {
                            cover.setImageBitmap(bitmap)
                            detailsFragment?.setCover(bitmap)
                        }
                    }

                } catch (e: Exception) {
                    mainThread { cover.setImageDrawable(null) }

                    if (e is CancellationException) {
                        throw e
                    }

                    Log.e("BeatmapDownloader", "Failed to load cover.", e)
                }

                coverJob = null
            }
        } else {
            cover.setImageDrawable(null)
        }

        updatePreviewButtons()
        previewButton.setOnClickListener { previewPlayer.toggle(beatmapSet) }

        val suggestedDownloadFilename = "${beatmapSet.id} ${beatmapSet.artist} - ${beatmapSet.title}"

        downloadButton.setOnClickListener {
            BeatmapDownloader.download(
                url = BeatmapListing.mirror.download.request(beatmapSet.id, true).toString(),
                suggestedFilename = suggestedDownloadFilename
            )
        }

        downloadNoVideoButton.apply {
            if (BeatmapListing.mirror.supportsNoVideoDownloads && beatmapSet.hasVideo) {
                visibility = VISIBLE
                setOnClickListener {
                    BeatmapDownloader.download(
                        url = BeatmapListing.mirror.download.request(beatmapSet.id, false).toString(),
                        suggestedFilename = "$suggestedDownloadFilename [no video]"
                    )
                }
            } else {
                visibility = GONE
                setOnClickListener(null)
            }
        }

        itemView.setOnClickListener {
            BeatmapSetDetails(beatmapSet, this).show()
        }
    }

    /**
     * Shows whether the bound beatmap set is being previewed on this card and its details.
     */
    fun updatePreviewButtons() {
        previewButton.setPreviewIcon(beatmapSetId != null && previewPlayer.beatmapSetId.value == beatmapSetId)
        detailsFragment?.updatePreviewButton()
    }

}


private fun Button.setPreviewIcon(isPreviewing: Boolean) {
    val icon = if (isPreviewing) R.drawable.pause_24px else R.drawable.play_arrow_24px
    setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0)
}


// Filters

class BeatmapListingFiltersFragment(private val beatmapListing: BeatmapListing) : BaseFragment() {


    override val layoutID = R.layout.beatmap_downloader_filters


    var sortType = SortType.RankedDate

    var orderType = OrderType.Descending

    var rankedStatus: RankedStatus? = null


    override fun onLoadView() {

        findViewById<View>(R.id.frg_body)!!.y = beatmapListing.findViewById<View>(R.id.bar)!!.height.toFloat()
        findViewById<View>(R.id.frg_background)!!.setOnClickListener { callDismissOnBackPress() }

        findViewById<Button>(R.id.sort)!!.also { sortButton ->

            sortButton.text = sortType.description
            sortButton.setOnClickListener {

                SelectDropdown(sortButton)
                    .setOptions(SortType.entries.map { type -> Option(type.description, type) })
                    .setSelected(sortType)
                    .setOnSelectListener {
                        it as SortType
                        if (it != sortType) {
                            sortType = it
                            sortButton.text = it.description
                            beatmapListing.search(false)
                        }
                    }
                    .show()
            }
        }

        findViewById<Button>(R.id.order)!!.also { orderButton ->

            orderButton.text = orderType.description
            orderButton.setOnClickListener {
                orderType = if (orderType == OrderType.Ascending) OrderType.Descending else OrderType.Ascending
                orderButton.text = orderType.description
                beatmapListing.search(false)
            }
        }

        findViewById<Button>(R.id.status)!!.also { statusButton ->

            if (rankedStatus == null) {
                statusButton.text = "All"
            } else {
                statusButton.setText(rankedStatus!!.stringId)
            }

            statusButton.setOnClickListener {

                val options = RankedStatus.entries.map { status ->
                    Option(requireContext().getString(status.stringId), status)
                }.toMutableList()
                options.add(0, Option("All", null))

                SelectDropdown(statusButton)
                    .setOptions(options)
                    .setSelected(rankedStatus)
                    .setOnSelectListener {
                        it as RankedStatus?
                        if (it != rankedStatus) {
                            rankedStatus = it

                            if (it == null) {
                                statusButton.text = "All"
                            } else {
                                statusButton.setText(it.stringId)
                            }

                            beatmapListing.search(false)
                        }
                    }
                    .show()
            }
        }

    }

    override fun dismiss() {
        beatmapListing.findViewById<TextView>(R.id.filters)!!.also { filtersButton ->
            filtersButton.setBackgroundColor(Color.TRANSPARENT)
            filtersButton.cornerRadius = 0f
        }
        super.dismiss()
    }


}