package com.lizongying.mytv.source

import android.os.Bundle
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

        // Google TV leanback-preference 满行宽行：行确认进编辑，开关行内切换
        for (source in sources) {
            val item = SourceItemBinding.inflate(layoutInflater, container, false)
            item.itemName.text = source.name
            item.itemUrl.text = source.url
            item.itemStatus.text = statusText(statuses[source.id])
            item.itemEnabled.isChecked = source.enabled
            item.itemEnabled.setOnCheckedChangeListener { _, checked ->
                SourceStore.setEnabled(source.id, checked)
                refresh()
            }
            item.root.setOnClickListener { openEdit(source) }
            container.addView(item.root)
        }

        // 列表末尾固定「＋ 添加直播源」虚线行
        val addRow = TextView(requireContext())
        addRow.text = getString(R.string.source_add)
        addRow.setTextColor(0xFF8FD3B5.toInt())
        addRow.textSize = 16f
        addRow.setBackgroundResource(R.drawable.bg_source_add_row)
        addRow.gravity = android.view.Gravity.CENTER_VERTICAL
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
