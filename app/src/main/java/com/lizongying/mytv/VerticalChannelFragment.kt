package com.lizongying.mytv

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.leanback.widget.OnChildViewHolderSelectedListener
import androidx.leanback.widget.ViewHolderTask
import androidx.recyclerview.widget.RecyclerView
import com.lizongying.mytv.databinding.FragmentVerticalChannelsBinding
import com.lizongying.mytv.databinding.ItemChannelBinding
import com.lizongying.mytv.databinding.ItemChannelGroupBinding
import com.lizongying.mytv.logo.LogoLoader
import com.lizongying.mytv.models.ChannelGroup
import com.lizongying.mytv.models.ChannelSelection

class VerticalChannelFragment : Fragment() {

    private var _binding: FragmentVerticalChannelsBinding? = null
    private val binding get() = _binding!!

    private var groups: List<ChannelGroup> = emptyList()
    private var activeGroup = 0
    private var currentId = -1
    private var selectedChannel = 0
    // 正在播频道所在分类，粉条指示固定标记该分类
    private var playingGroup = 0

    // 分类连续移动时去抖：停顿后才重建频道列表预览，避免每键全量重绑
    private val listHandler = Handler(Looper.getMainLooper())
    private var pendingGroup = -1

    private val groupAdapter = GroupAdapter { onGroupClicked(it) }
    private val channelAdapter = ChannelAdapter { playChannel(it) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentVerticalChannelsBinding.inflate(inflater, container, false)

        binding.groupGrid.apply {
            setNumColumns(1)
            adapter = groupAdapter
            // 关闭换数据动画：整表重绑时动画只增加开销
            itemAnimator = null
            setOnKeyInterceptListener { onGroupKey(it) }
            addOnChildViewHolderSelectedListener(object : OnChildViewHolderSelectedListener() {
                override fun onChildViewHolderSelected(
                    parent: RecyclerView,
                    child: RecyclerView.ViewHolder?,
                    position: Int,
                    subposition: Int,
                ) {
                    if (parent.hasFocus() && position >= 0 && position != activeGroup) {
                        onGroupSelected(position)
                    }
                }
            })
            setOnFocusChangeListener { _, hasFocus ->
                binding.channelGrid.alpha = if (hasFocus) 0.6f else 1f
            }
        }

        binding.channelGrid.apply {
            setNumColumns(1)
            adapter = channelAdapter
            // 关闭换数据动画：预览重建是整表置换，change 动画在低端电视上拖慢焦点移动
            itemAnimator = null
            setOnKeyInterceptListener { onChannelKey(it) }
            addOnChildViewHolderSelectedListener(object : OnChildViewHolderSelectedListener() {
                override fun onChildViewHolderSelected(
                    parent: RecyclerView,
                    child: RecyclerView.ViewHolder?,
                    position: Int,
                    subposition: Int,
                ) {
                    if (parent.hasFocus() && position >= 0) {
                        selectedChannel = position
                    }
                }
            })
        }

        return binding.root
    }

    fun show(groups: List<ChannelGroup>, currentId: Int) {
        val binding = _binding ?: return
        if (groups.isEmpty()) return
        this.groups = groups
        this.currentId = currentId
        val groupIndex = ChannelSelection.initialGroupIndex(groups, currentId)
        activeGroup = groupIndex
        playingGroup = groupIndex
        val channelIndex = ChannelSelection.channelIndexForGroup(groups[groupIndex], currentId)
        selectedChannel = channelIndex

        // 清除隐藏期间残留的焦点，避免残留选中派发劫持本次定位、焦点闪动
        binding.root.clearFocus()
        pendingGroup = -1
        listHandler.removeCallbacks(pendingListUpdate)
        groupAdapter.submit(groups, groupIndex)
        channelAdapter.submit(groups[groupIndex].channels, currentId)
        // 必须在首个布局遍历前同步设置目标位置：post 会被 vsync 同步屏障
        // 推迟到首次布局之后，残留位置的布局派发会先于定位执行
        binding.groupGrid.setSelectedPosition(groupIndex)
        focusChannelRow(channelIndex)
    }

    // 确定性定位：task 在布局完成、滚动到位后才执行，消除时序竞态
    private fun focusChannelRow(position: Int) {
        binding.channelGrid.setSelectedPosition(position, object : ViewHolderTask {
            override fun run(holder: RecyclerView.ViewHolder) {
                holder.itemView.requestFocus()
            }
        })
    }

    companion object {
        private const val LIST_UPDATE_DELAY_MILLIS = 80L
    }

    private fun onGroupSelected(index: Int) {
        if (index !in groups.indices || index == activeGroup) return
        val previous = activeGroup
        activeGroup = index
        selectedChannel = ChannelSelection.channelIndexForGroup(groups[index], currentId)
        groupAdapter.setActive(index)
        // 同步精准刷新确切的旧行与新行：post 会引入连续移动时的范围错位竞态
        groupAdapter.notifyItemChanged(previous)
        groupAdapter.notifyItemChanged(index)
        // 去抖：焦点快速掠过中间分类时不重建频道列表，停顿后预览最终分类
        pendingGroup = index
        listHandler.removeCallbacks(pendingListUpdate)
        listHandler.postDelayed(pendingListUpdate, LIST_UPDATE_DELAY_MILLIS)
    }

    private val pendingListUpdate = Runnable {
        val index = pendingGroup
        pendingGroup = -1
        if (_binding == null || index !in groups.indices) return@Runnable
        selectedChannel = ChannelSelection.channelIndexForGroup(groups[index], currentId)
        channelAdapter.submit(groups[index].channels, currentId)
        binding.channelGrid.setSelectedPosition(selectedChannel)
    }

    private fun onGroupClicked(index: Int) {
        onGroupSelected(index)
        // 进入列表前强制同步：取消去抖立即刷新，保证定位准确
        listHandler.removeCallbacks(pendingListUpdate)
        if (pendingGroup >= 0) {
            pendingGroup = -1
            channelAdapter.submit(groups[activeGroup].channels, currentId)
        }
        focusChannelRow(selectedChannel)
    }

    private fun playSelected() {
        val tv = channelAdapter.item(binding.channelGrid.selectedPosition) ?: return
        playChannel(tv)
    }

    private fun playChannel(tv: TV) {
        (activity as? MainActivity)?.let { activity ->
            activity.play(tv.id)
            activity.closeChannelList()
        }
    }

    private fun onChannelKey(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            (activity as? MainActivity)?.mainActive()
        }
        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                binding.groupGrid.requestFocus()
                true
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> true

            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (event.action == KeyEvent.ACTION_DOWN) playSelected()
                true
            }

            KeyEvent.KEYCODE_DPAD_UP ->
                event.action == KeyEvent.ACTION_DOWN && binding.channelGrid.selectedPosition == 0

            KeyEvent.KEYCODE_DPAD_DOWN ->
                event.action == KeyEvent.ACTION_DOWN &&
                        binding.channelGrid.selectedPosition == channelAdapter.itemCount - 1

            else -> false
        }
    }

    private fun onGroupKey(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            (activity as? MainActivity)?.mainActive()
        }
        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                if (event.action == KeyEvent.ACTION_DOWN) onGroupClicked(activeGroup)
                true
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> true

            KeyEvent.KEYCODE_DPAD_UP ->
                event.action == KeyEvent.ACTION_DOWN && binding.groupGrid.selectedPosition == 0

            KeyEvent.KEYCODE_DPAD_DOWN ->
                event.action == KeyEvent.ACTION_DOWN &&
                        binding.groupGrid.selectedPosition == groups.lastIndex

            else -> false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listHandler.removeCallbacks(pendingListUpdate)
        _binding = null
    }

    private inner class GroupAdapter(
        private val onClick: (Int) -> Unit,
    ) : RecyclerView.Adapter<GroupAdapter.Holder>() {

        private var items: List<ChannelGroup> = emptyList()
        private var active = 0

        fun submit(groups: List<ChannelGroup>, active: Int) {
            items = groups
            this.active = active
            notifyDataSetChanged()
        }

        fun setActive(index: Int) {
            active = index
        }

        override fun getItemCount(): Int = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val itemBinding = ItemChannelGroupBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return Holder(itemBinding)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val group = items[position]
            // 粉条固定标记正在播频道所在分类，不随浏览焦点移动
            holder.binding.indicator.isVisible = position == playingGroup
            holder.binding.name.text = group.title
            holder.binding.count.text = group.channels.size.toString()
            holder.itemView.isSelected = position == active
            holder.itemView.setOnClickListener { onClick(position) }
        }

        inner class Holder(val binding: ItemChannelGroupBinding) :
            RecyclerView.ViewHolder(binding.root)
    }

    private inner class ChannelAdapter(
        private val onClick: (TV) -> Unit,
    ) : RecyclerView.Adapter<ChannelAdapter.Holder>() {

        private var items: List<TV> = emptyList()
        private var currentId = -1

        fun submit(channels: List<TV>, currentId: Int) {
            items = channels
            this.currentId = currentId
            notifyDataSetChanged()
        }

        fun item(position: Int): TV? = items.getOrNull(position)

        override fun getItemCount(): Int = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val itemBinding = ItemChannelBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return Holder(itemBinding)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val tv = items[position]
            val playing = tv.id == currentId
            holder.binding.number.text =
                holder.itemView.context.getString(R.string.vertical_channel_number, tv.id + 1)
            holder.binding.name.text = tv.title
            val context = holder.itemView.context
            LogoLoader.load(
                holder.binding.logo,
                listOf(tv.logo, tv.logoAlt, tv.sourceLogo),
                context.resources.getDimensionPixelSize(R.dimen.channel_logo_width),
                context.resources.getDimensionPixelSize(R.dimen.channel_logo_height),
            )
            holder.binding.indicator.isVisible = playing
            holder.binding.playingDot.isVisible = playing
            holder.binding.playing.isVisible = playing
            holder.itemView.setOnClickListener { onClick(tv) }
        }

        inner class Holder(val binding: ItemChannelBinding) :
            RecyclerView.ViewHolder(binding.root)
    }
}
