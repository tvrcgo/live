package com.lizongying.mytv

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.lizongying.mytv.databinding.SettingBinding
import com.lizongying.mytv.update.UpdateManager


class SettingFragment : FlatDialogFragment(0.6f, 0.9f) {

    private var _binding: SettingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext() // It‘s safe to get context here.
        _binding = SettingBinding.inflate(inflater, container, false)
        bindVersionRow(context)

        binding.switchChannelReversal.run {
            isChecked = SP.channelReversal
            setOnCheckedChangeListener { _, isChecked ->
                SP.channelReversal = isChecked
            }
        }

        binding.switchChannelNum.run {
            isChecked = SP.channelNum
            setOnCheckedChangeListener { _, isChecked ->
                SP.channelNum = isChecked
            }
        }

        bindChannelLayout()

        binding.switchTime.run {
            isChecked = SP.time
            setOnCheckedChangeListener { _, isChecked ->
                SP.time = isChecked
                // 开关即时生效：时间浮层随 SP 显隐（原 settingDelayHide 副作用链已删）
                (activity as? MainActivity)?.showTime()
            }
        }

        binding.switchBootStartup.run {
            isChecked = SP.bootStartup
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                isEnabled = false
                binding.titleBootStartup.alpha = 0.5f
                binding.hintBootStartup.visibility = View.VISIBLE
            } else {
                setOnCheckedChangeListener { _, isChecked ->
                    SP.bootStartup = isChecked
                    }
            }
        }

        // 整行聚焦：确认键即开关（toggle 触发 listener 写 SP）；禁用态保持 no-op
        listOf(
            binding.rowChannelReversal to binding.switchChannelReversal,
            binding.rowChannelNum to binding.switchChannelNum,
            binding.rowTime to binding.switchTime,
            binding.rowBootStartup to binding.switchBootStartup,
        ).forEach { (row, switch) ->
            row.setOnClickListener {
                if (switch.isEnabled) {
                    switch.toggle()
                }
            }
        }

        binding.sourceEntry.setOnClickListener {
            // 不销毁设置页：二级弹窗层叠覆盖，返回键 dismiss 后逐级回到设置页
            (activity as? MainActivity)?.openSources()
        }

        return binding.root
    }

    private fun bindChannelLayout() {
        // leanback ListPreference 同款：值文字 + 左右 chevron，随 SP 渲染
        fun render() {
            binding.channelLayoutValue.setText(
                if (SP.channelLayout == SP.LAYOUT_HORIZONTAL) {
                    R.string.channel_layout_horizontal
                } else {
                    R.string.channel_layout_vertical
                }
            )
        }

        // 整行聚焦：按键处理挂行上，左右/确认切换选项
        binding.rowChannelLayout.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) {
                return@setOnKeyListener false
            }
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    SP.channelLayout = SP.LAYOUT_HORIZONTAL
                    render()
                        true
                }

                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    SP.channelLayout = SP.LAYOUT_VERTICAL
                    render()
                        true
                }

                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    // 二选一场景：确认键切换到另一项
                    SP.channelLayout =
                        if (SP.channelLayout == SP.LAYOUT_HORIZONTAL) SP.LAYOUT_VERTICAL
                        else SP.LAYOUT_HORIZONTAL
                    render()
                        true
                }

                else -> false
            }
        }
        render()
    }

    private fun bindVersionRow(context: Context) {
        fun render(state: UpdateManager.State) {
            val (textRes, textArg, highlight) = when (state) {
                is UpdateManager.State.CheckFailed ->
                    Triple(R.string.update_check_failed, null, false)

                is UpdateManager.State.Available ->
                    Triple(R.string.update_new_version, state.remote.name, true)

                is UpdateManager.State.Downloading ->
                    Triple(R.string.update_downloading, state.percent, true)

                is UpdateManager.State.DownloadFailed ->
                    Triple(R.string.update_download_failed, null, true)

                is UpdateManager.State.Downloaded ->
                    Triple(R.string.update_new_version, state.remote.name, true)

                else -> Triple(R.string.update_current_version, context.appVersionName, false)
            }
            binding.versionStatus.text =
                if (textArg == null) getString(textRes) else getString(textRes, textArg)
            binding.versionStatus.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (highlight) R.color.version_highlight else R.color.version_text
                )
            )
        }

        UpdateManager.listener = { state ->
            if (_binding != null) {
                render(state)
            }
        }
        render(UpdateManager.state)
        UpdateManager.check(context)

        binding.versionRow.setOnClickListener {
            when (UpdateManager.state) {
                is UpdateManager.State.Available,
                is UpdateManager.State.DownloadFailed -> UpdateManager.download(context)

                is UpdateManager.State.Downloaded -> UpdateManager.install(context)

                is UpdateManager.State.CheckFailed -> UpdateManager.check(context)

                else -> Unit
            }
        }
    }

    override fun onDestroyView() {
        UpdateManager.listener = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SettingFragment"
    }
}

