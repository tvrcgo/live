package com.lizongying.mytv

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.ListRowPresenter.SelectItemViewHolderTask
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.lifecycle.lifecycleScope
import com.lizongying.mytv.models.ChannelGroup
import com.lizongying.mytv.models.ChannelItem
import com.lizongying.mytv.models.ChannelList
import com.lizongying.mytv.source.ChannelListBuilder
import com.lizongying.mytv.source.SourceRepository
import com.lizongying.mytv.source.SourceStore
import kotlinx.coroutines.launch

class MainFragment : BrowseSupportFragment() {

    private var itemPosition = 0

    private var rowsAdapter: ArrayObjectAdapter? = null

    private var channelList = ChannelList()

    var lastStatuses: List<SourceRepository.SourceStatus> = emptyList()
        private set

    private var playingKey: String? = null

    private var rowsSignature: List<String> = emptyList()

    private var rows: Map<String, List<TV>> = emptyMap()

    private var refreshed = false

    private val repository: SourceRepository by lazy {
        SourceRepository(requireContext().applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        headersState = HEADERS_DISABLED
    }

    override fun onStart() {
        Log.i(TAG, "onStart")
        super.onStart()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        bindRows(repository.loadCachedRows())
        setupEventListeners()
        refresh()
        (activity as MainActivity).fragmentReady("MainFragment")
    }

    override fun startHeadersTransition(withHeaders: Boolean) {
    }

    private fun bindRows(rows: Map<String, List<TV>>?) {
        val list = rows.orEmpty()
        this.rows = list
        val signature = ChannelListBuilder.signature(list)
        if (signature == rowsSignature) {
            (activity as? MainActivity)?.showEmpty(emptyMessage(list))
            return
        }

        val previousKey = channelList.get(itemPosition)?.getTV()?.let { keyOf(it) }

        rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        channelList = ChannelList()
        val cardPresenter = CardPresenter(requireContext())

        var idx: Long = 0
        for ((k, v) in list) {
            val listRowAdapter = ArrayObjectAdapter(cardPresenter)
            for ((idx2, v1) in v.withIndex()) {
                val channelItem = ChannelItem(v1)
                channelItem.rowPosition = idx.toInt()
                channelItem.itemPosition = idx2
                channelList.add(channelItem)
                listRowAdapter.add(channelItem)
            }
            rowsAdapter!!.add(ListRow(HeaderItem(idx, k), listRowAdapter))
            idx++
        }

        adapter = rowsAdapter
        rowsSignature = signature

        val restored = previousKey?.let { indexOf(it) } ?: -1
        itemPosition = if (restored >= 0) {
            restored
        } else {
            SP.itemPosition.coerceIn(0, (channelList.size() - 1).coerceAtLeast(0))
        }
        channelList.setCurrent(itemPosition)

        val playing = playingKey
        if (playing != null && indexOf(playing) < 0) {
            playingKey = null
            (activity as? MainActivity)?.stopPlay()
        } else if (playingKey == null
            && channelList.size() > 0
            && (activity as? MainActivity)?.isReady() == true
        ) {
            channelList.get(itemPosition)?.let { selectChannel(it) }
        }

        (activity as? MainActivity)?.showEmpty(emptyMessage(list))
        (activity as? MainActivity)?.onChannelsRebound()
    }

    fun refresh(onDone: ((List<SourceRepository.SourceStatus>) -> Unit)? = null) {
        if (!isAdded) {
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val result = repository.refresh()
            lastStatuses = result.statuses
            refreshed = true
            bindRows(result.rows)
            onDone?.invoke(result.statuses)
        }
    }

    private fun selectChannel(channelItem: ChannelItem) {
        val title = channelItem.getTV().title
        Log.i(TAG, "switch $title")
        if (check(channelItem)) {
            playingKey = keyOf(channelItem.getTV())
            (activity as? MainActivity)?.play(channelItem)
            (activity as? MainActivity)?.showInfoFragment(channelItem)
        }
        setSelectedPosition(
            channelItem.rowPosition, true,
            SelectItemViewHolderTask(channelItem.itemPosition)
        )
    }

    private fun keyOf(tv: TV): String {
        return "${tv.channel}\u0000${tv.title}\u0000${tv.videoUrl.firstOrNull().orEmpty()}"
    }

    private fun indexOf(key: String): Int {
        for (i in 0 until channelList.size()) {
            val tv = channelList.get(i)?.getTV() ?: continue
            if (keyOf(tv) == key) {
                return i
            }
        }
        return -1
    }

    private fun emptyMessage(list: Map<String, List<TV>>): String {
        if (list.isNotEmpty()) {
            return ""
        }
        return when {
            SourceStore.enabled().isEmpty() -> getString(R.string.empty_no_source)
            refreshed -> getString(R.string.empty_load_failed)
            else -> getString(R.string.empty_loading)
        }
    }

    private fun setupEventListeners() {
        onItemViewClickedListener = ItemViewClickedListener()
        onItemViewSelectedListener = ItemViewSelectedListener()
    }

    private inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder,
            item: Any,
            rowViewHolder: RowPresenter.ViewHolder,
            row: Row
        ) {
            if (item is ChannelItem) {
                if (itemPosition != item.getTV().id) {
                    itemPosition = item.getTV().id
                    channelList.setCurrent(itemPosition)
                    channelList.get(itemPosition)?.let { selectChannel(it) }
                }
                (activity as? MainActivity)?.switchMainFragment()
            }
        }
    }

    private inner class ItemViewSelectedListener : OnItemViewSelectedListener {
        override fun onItemSelected(
            itemViewHolder: Presenter.ViewHolder?, item: Any?,
            rowViewHolder: RowPresenter.ViewHolder, row: Row
        ) {
            if (item is ChannelItem) {
                channelList.setCurrent(item.getTV().id)
                (activity as MainActivity).mainActive()
            }
        }
    }

    fun check(channelItem: ChannelItem): Boolean {
        val title = channelItem.getTV().title
        if (channelItem.currentUrl().isEmpty()) {
            Log.e(TAG, "$title videoUrl is empty")
            return false
        }

        return true
    }

    fun fragmentReady() {
        if (channelList.size() == 0) {
            return
        }
        channelList.get(itemPosition)?.let { selectChannel(it) }
    }

    fun channelGroups(): List<ChannelGroup> =
        rows.map { (title, channels) -> ChannelGroup(title, channels) }

    fun currentTVId(): Int =
        channelList.get(itemPosition)?.getTV()?.id ?: -1

    fun play(itemPosition: Int) {
        view?.post {
            if (itemPosition > -1 && itemPosition < channelList.size()) {
                this.itemPosition = itemPosition
                channelList.setCurrent(itemPosition)
                channelList.get(itemPosition)?.let { selectChannel(it) }
            } else {
                Toast.makeText(context, "频道不存在", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun prev() {
        view?.post {
            if (channelList.size() == 0) {
                return@post
            }
            itemPosition--
            if (itemPosition == -1) {
                itemPosition = channelList.size() - 1
            }
            channelList.setCurrent(itemPosition)
            channelList.get(itemPosition)?.let { selectChannel(it) }
        }
    }

    fun next() {
        view?.post {
            if (channelList.size() == 0) {
                return@post
            }
            itemPosition++
            if (itemPosition == channelList.size()) {
                itemPosition = 0
            }
            channelList.setCurrent(itemPosition)
            channelList.get(itemPosition)?.let { selectChannel(it) }
        }
    }

    override fun onResume() {
        Log.i(TAG, "onResume")
        super.onResume()
    }

    override fun onStop() {
        Log.i(TAG, "onStop")
        super.onStop()
        SP.itemPosition = itemPosition
        Log.i(TAG, "$POSITION $itemPosition saved")
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MainFragment"
        private const val POSITION = "position"
    }
}
