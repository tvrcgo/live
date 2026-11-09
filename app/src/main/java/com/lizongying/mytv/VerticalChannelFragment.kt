package com.lizongying.mytv

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.leanback.widget.OnChildViewHolderSelectedListener
import androidx.recyclerview.widget.RecyclerView
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
                if (hasFocus) {
                    binding.channelGrid.post {
                        channelAdapter.highlight = true
                        channelAdapter.notifyDataSetChanged()
                        binding.channelGrid.setSelectedPosition(selectedChannel)
                    }
                }
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
                        channelAdapter.selected = position
                    }
                }
            })
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    channelAdapter.highlight = false
                    binding.channelGrid
                        .findViewHolderForAdapterPosition(selectedChannel)
                        ?.itemView
                        ?.let { it.isSelected = false }
                }
            }
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
        channelAdapter.submit(groups[activeGroup].channels, currentId, selectedChannel)
        binding.root.post {
            binding.groupGrid.setSelectedPosition(activeGroup)
            binding.channelGrid.setSelectedPosition(selectedChannel)
            binding.channelGrid.requestFocus()
        }
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
            channelAdapter.submit(group.channels, current, channelIndex)
            binding.channelGrid.setSelectedPosition(channelIndex)
        }
    }

    private fun onGroupClicked(index: Int) {
        onGroupSelected(index)
        binding.channelGrid.requestFocus()
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
                if (event.action == KeyEvent.ACTION_DOWN) binding.channelGrid.requestFocus()
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

        var selected = 0
        var highlight = false

        fun submit(channels: List<TV>, currentId: Int, selected: Int) {
            items = channels
            this.currentId = currentId
            this.selected = selected
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
            holder.binding.indicator.isVisible = playing
            holder.binding.playingDot.isVisible = playing
            holder.binding.playing.isVisible = playing
            holder.itemView.isSelected = highlight && position == selected
            holder.itemView.setOnClickListener { onClick(tv) }
        }

        inner class Holder(val binding: ItemChannelBinding) :
            RecyclerView.ViewHolder(binding.root)
    }
}
