package com.lizongying.mytv

import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.lizongying.mytv.databinding.SettingBinding


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
        binding.version.text = "v${context.appVersionName}"

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SettingFragment"
    }
}

