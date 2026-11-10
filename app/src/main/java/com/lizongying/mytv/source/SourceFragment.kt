package com.lizongying.mytv.source

import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.lizongying.mytv.FlatDialogFragment
import com.lizongying.mytv.MainActivity
import com.lizongying.mytv.R
import com.lizongying.mytv.databinding.SourceBinding
import com.lizongying.mytv.databinding.SourceItemBinding

class SourceFragment : FlatDialogFragment(0.6f, 0.9f) {

    private var _binding: SourceBinding? = null
    private val binding get() = _binding!!

    private var statuses: Map<String, SourceRepository.SourceStatus> = emptyMap()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = SourceBinding.inflate(inflater, container, false)

        statuses = (activity as? MainActivity)?.sourceStatuses().orEmpty().associateBy { it.id }
        render()
        return binding.root
    }

    private fun render() {
        if (_binding == null) {
            return
        }
        val container = binding.sourceList
        container.removeAllViews()

        val sources = SourceStore.list()
        if (sources.isEmpty()) {
            val empty = TextView(requireContext())
            empty.text = getString(R.string.source_empty)
            empty.setTextColor(0xFFEEEEEE.toInt())
            empty.textSize = 16f
            empty.setPadding(
                resources.getDimensionPixelSize(R.dimen.setting_row_padding_h), 0,
                resources.getDimensionPixelSize(R.dimen.setting_row_padding_h), 0
            )
            container.addView(
                empty,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        // 单焦点行模型（leanback-preference）：↑↓ 行间移动，
        // ←→ 切换启用，确认进编辑；开关仅显示状态
        for (source in sources) {
            val item = SourceItemBinding.inflate(layoutInflater, container, false)
            item.itemName.text = source.name
            item.itemUrl.text = source.url
            item.itemStatus.text = statusText(statuses[source.id])
            item.itemEnabled.isChecked = source.enabled
            item.root.setOnClickListener { openEdit(source) }
            item.root.setOnKeyListener { _, keyCode, event ->
                if (event.action != KeyEvent.ACTION_DOWN) {
                    return@setOnKeyListener false
                }
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        val next = !item.itemEnabled.isChecked
                        item.itemEnabled.isChecked = next
                        SourceStore.setEnabled(source.id, next)
                        refresh()
                        true
                    }

                    else -> false
                }
            }
            container.addView(item.root)
        }

        // 列表末尾固定「＋ 添加直播源」行，与源行同款样式
        val addRow = TextView(requireContext())
        addRow.text = getString(R.string.source_add)
        addRow.textSize = 16f
        // 与源行同款（leanback GuidedActionItemContainer：行生而平等，不加按钮隐喻）
        addRow.setBackgroundResource(R.drawable.bg_setting_row)
        addRow.setTextColor(0xFFEEEEEE.toInt())
        addRow.gravity = Gravity.CENTER_VERTICAL
        addRow.minHeight = resources.getDimensionPixelSize(R.dimen.source_row_height)
        addRow.setPadding(
            resources.getDimensionPixelSize(R.dimen.setting_row_padding_h), 0,
            resources.getDimensionPixelSize(R.dimen.setting_row_padding_h), 0
        )
        addRow.isFocusable = true
        addRow.isClickable = true
        addRow.setOnClickListener { openEdit(null) }
        container.addView(
            addRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.source_add_margin_top)
            }
        )
    }

    private fun statusText(status: SourceRepository.SourceStatus?): String {
        if (status == null) {
            return getString(R.string.source_status_unknown)
        }
        return if (status.ok) {
            getString(R.string.source_status_ok, status.count)
        } else {
            getString(R.string.source_status_fail, status.error, status.count)
        }
    }

    private fun refresh() {
        val activity = activity as? MainActivity ?: return
        activity.refreshSources { result ->
            statuses = result.associateBy { it.id }
            render()
        }
    }

    private fun openEdit(source: Source?) {
        val fragment = SourceEditFragment.newInstance(source?.id)
        fragment.onSaved = {
            render()
            refresh()
        }
        fragment.show(requireActivity().supportFragmentManager, SourceEditFragment.TAG)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SourceFragment"
    }
}
