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
                (activity as MainActivity).settingDelayHide()
            }
        }

        binding.switchChannelNum.run {
            isChecked = SP.channelNum
            setOnCheckedChangeListener { _, isChecked ->
                SP.channelNum = isChecked
                (activity as MainActivity).settingDelayHide()
            }
        }

        bindChannelLayout()

        binding.switchTime.run {
            isChecked = SP.time
            setOnCheckedChangeListener { _, isChecked ->
                SP.time = isChecked
                (activity as MainActivity).settingDelayHide()
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
                    (activity as MainActivity).settingDelayHide()
                }
            }
        }

        binding.sourceEntry.setOnClickListener {
            (activity as? MainActivity)?.let { activity ->
                dismissAllowingStateLoss()
                activity.supportFragmentManager.executePendingTransactions()
                activity.openSources()
            }
        }

        return binding.root
    }

    private fun bindChannelLayout() {
        fun render() {
            binding.channelLayoutHorizontal.isSelected = SP.channelLayout == SP.LAYOUT_HORIZONTAL
            binding.channelLayoutVertical.isSelected = SP.channelLayout == SP.LAYOUT_VERTICAL
        }

        binding.channelLayout.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) {
                return@setOnKeyListener false
            }
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    SP.channelLayout = SP.LAYOUT_HORIZONTAL
                    render()
                    (activity as MainActivity).settingDelayHide()
                    true
                }

                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    SP.channelLayout = SP.LAYOUT_VERTICAL
                    render()
                    (activity as MainActivity).settingDelayHide()
                    true
                }

                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> true
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
            (activity as MainActivity).settingDelayHide()
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

