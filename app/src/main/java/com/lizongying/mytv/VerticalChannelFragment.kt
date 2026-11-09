package com.lizongying.mytv

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.leanback.widget.OnChildViewHolderSelectedListener
import androidx.leanback.widget.ViewHolderTask
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lizongying.mytv.databinding.FragmentVerticalChannelsBinding
import com.lizongying.mytv.databinding.ItemChannelBinding
import com.lizongying.mytv.databinding.ItemChannelGroupBinding
import com.lizongying.mytv.models.ChannelGroup
import com.lizongying.mytv.models.ChannelSelection

class VerticalChannelFragment : Fragment() {

    private var _binding: FragmentVerticalChannelsBinding? = null
    private val binding get() = _binding!!

    private var groups: List<ChannelGroup> = emptyList()
    private var activeGroup = 0
    private var currentId = -1
    private var selectedChannel = 0

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
            setOnKeyInterceptListener { onChannelKey(it) }
            addOnChildViewHolderSelectedListener(object : OnChildViewHolderSelectedListener() {
                override fun onChildViewHolderSelected(
                    parent: RecyclerView,
                    child: RecyclerView.ViewHolder?,
                    position: Int,
                    subposition: Int,
                ) {
                    if (position >= 0) {
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
        activeGroup = ChannelSelection.initialGroupIndex(groups, currentId)
        selectedChannel = ChannelSelection.channelIndexForGroup(groups[activeGroup], currentId)

        groupAdapter.submit(groups, activeGroup)
        channelAdapter.submit(groups[activeGroup].channels, currentId)
        binding.root.post {
            binding.groupGrid.setSelectedPosition(activeGroup)
            focusChannelRow(selectedChannel)
        }
    }

    // 确定性定位：task 在布局完成、滚动到位后才执行，消除时序竞态
    private fun focusChannelRow(position: Int) {
        binding.channelGrid.setSelectedPosition(position, object : ViewHolderTask {
            override fun run(holder: RecyclerView.ViewHolder) {
                holder.itemView.requestFocus()
            }
        })
    }

    private fun onGroupSelected(index: Int) {
        if (index !in groups.indices || index == activeGroup) return
        activeGroup = index
        val group = groups[index]
        val current = currentId
        val channelIndex = ChannelSelection.channelIndexForGroup(group, current)
        selectedChannel = channelIndex
        groupAdapter.setActive(index)
        binding.groupGrid.post { groupAdapter.notifyDataSetChanged() }
        binding.channelGrid.post {
            channelAdapter.submit(group.channels, current)
            // 焦点仍在分类栏，仅滚动预定位；setSelectedPosition 在 pending 更新时
            // 由布局完成后的对齐逻辑保证滚动到位
            binding.channelGrid.setSelectedPosition(channelIndex)
        }
    }

    private fun onGroupClicked(index: Int) {
        onGroupSelected(index)
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
                if (event.action == KeyEvent.ACTION_DOWN) focusChannelRow(selectedChannel)
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
            holder.binding.indicator.isVisible = position == active
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
            holder.binding.logo.setImageDrawable(null)
            if (tv.logo.isNotEmpty()) {
                val context = holder.itemView.context
                Glide.with(context)
                    .load(tv.logo)
                    .override(
                        context.resources.getDimensionPixelSize(R.dimen.channel_logo_width),
                        context.resources.getDimensionPixelSize(R.dimen.channel_logo_height),
                    )
                    .centerInside()
                    .into(holder.binding.logo)
            }
            holder.binding.indicator.isVisible = playing
            holder.binding.playingDot.isVisible = playing
            holder.binding.playing.isVisible = playing
            holder.itemView.setOnClickListener { onClick(tv) }
        }

        inner class Holder(val binding: ItemChannelBinding) :
            RecyclerView.ViewHolder(binding.root)
    }
}
