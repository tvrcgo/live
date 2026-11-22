package com.chacha.live

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.chacha.live.models.ChannelItem
import com.chacha.live.source.SourceRepository
import com.chacha.live.source.SourceFragment


class MainActivity : FragmentActivity() {

    private var networkReady = 0
    private var fragmentReadyCount = 0
    private var playStarted = false
    private val playerFragment = PlayerFragment()
    private val mainFragment = MainFragment()
    private val verticalChannelFragment = VerticalChannelFragment()
    private val infoFragment = InfoFragment()
    private val channelFragment = ChannelFragment()
    private var timeFragment = TimeFragment()
    private val settingFragment = SettingFragment()
    private val emptyFragment = EmptyFragment()
    private val sourceFragment = SourceFragment()

    private var doubleBackToExitPressedOnce = false

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private lateinit var gestureDetector: GestureDetector

    private val handler = Handler()
    private val delayHideMain: Long = 10000

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = SYSTEM_UI_FLAG_HIDE_NAVIGATION

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.main_browse_fragment, playerFragment)
                .add(R.id.main_browse_fragment, timeFragment)
                .add(R.id.main_browse_fragment, infoFragment)
                .add(R.id.main_browse_fragment, channelFragment)
                .add(R.id.main_browse_fragment, emptyFragment)
                .hide(emptyFragment)
                .add(R.id.main_browse_fragment, mainFragment)
                .hide(mainFragment)
                .add(R.id.main_browse_fragment, verticalChannelFragment)
                .hide(verticalChannelFragment)
                .commit()
        }
        gestureDetector = GestureDetector(this, GestureListener())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val connectivityManager =
                getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    Log.i(TAG, "net ${Build.VERSION.SDK_INT}")
                    if (this@MainActivity.isNetworkConnected) {
                        Log.i(TAG, "net isNetworkConnected")
                        playerFragment.onNetworkAvailable()
                        networkReady++
                        checkReady()
                    }
                }
            }
            networkCallback = callback
            connectivityManager.registerDefaultNetworkCallback(callback, handler)
        } else {
            Log.i(TAG, "net ${Build.VERSION.SDK_INT}")
            networkReady++
            checkReady()
        }

    }

    fun showInfoFragment(channelItem: ChannelItem) {
        infoFragment.show(channelItem)
        if (SP.channelNum) {
            channelFragment.show(channelItem)
        }
    }

    fun showPlaybackInfo(text: String) {
        infoFragment.updatePlaybackInfo(text)
    }

    fun showEmpty(message: String) {
        if (message.isEmpty()) {
            if (!emptyFragment.isHidden) {
                supportFragmentManager.beginTransaction().hide(emptyFragment).commit()
            }
            return
        }
        emptyFragment.message = message
        if (emptyFragment.isHidden) {
            supportFragmentManager.beginTransaction().show(emptyFragment).commit()
            emptyFragment.view?.post { emptyFragment.focusSetting() }
        }
    }

    fun openSources() {
        if (!sourceFragment.isAdded) {
            sourceFragment.show(supportFragmentManager, SourceFragment.TAG)
        }
    }

    fun sourceStatuses(): List<SourceRepository.SourceStatus> {
        return mainFragment.lastStatuses
    }

    fun refreshSources(onDone: ((List<SourceRepository.SourceStatus>) -> Unit)? = null) {
        mainFragment.refresh(onDone)
    }

    fun stopPlay() {
        playerFragment.stop()
        showChannelList()
        if (!mainFragment.isHidden) {
            mainFragment.view?.requestFocus()
        }
    }

    private fun showChannel(channel: String) {
        if (channelListVisible()) {
            return
        }

        if (settingFragment.isVisible || sourceFragment.isVisible) {
            return
        }

        if (SP.channelNum) {
            channelFragment.show(channel)
        }
    }

    fun play(channelItem: ChannelItem) {
        playerFragment.play(channelItem)
        if (!mainFragment.isHidden) {
            mainFragment.view?.requestFocus()
        }
    }

    fun play(itemPosition: Int) {
        mainFragment.play(itemPosition)
    }

    fun prev() {
        mainFragment.prev()
    }

    fun next() {
        mainFragment.next()
    }

    fun switchMainFragment() {
        if (sourceFragment.isVisible) {
            return
        }
        if (channelListVisible()) {
            hideChannelList()
        } else {
            showChannelList()
        }
    }

    fun closeChannelList() {
        hideChannelList()
    }

    fun onChannelsRebound() {
        if (verticalChannelFragment.isAdded && !verticalChannelFragment.isHidden) {
            val groups = mainFragment.channelGroups()
            if (groups.isEmpty()) {
                hideChannelList()
            } else {
                verticalChannelFragment.show(groups, mainFragment.currentTVId())
            }
        }
    }

    private fun channelListVisible(): Boolean =
        !mainFragment.isHidden ||
                (verticalChannelFragment.isAdded && !verticalChannelFragment.isHidden)

    private fun showChannelList() {
        if (channelListVisible()) {
            return
        }
        if (SP.channelLayout == SP.LAYOUT_VERTICAL) {
            val groups = mainFragment.channelGroups()
            if (groups.isEmpty()) {
                return
            }
            supportFragmentManager.beginTransaction().show(verticalChannelFragment).commit()
            supportFragmentManager.executePendingTransactions()
            verticalChannelFragment.show(groups, mainFragment.currentTVId())
        } else {
            supportFragmentManager.beginTransaction().show(mainFragment).commit()
        }
        mainActive()
    }

    private fun hideChannelList() {
        val transaction = supportFragmentManager.beginTransaction()
        if (!mainFragment.isHidden) {
            transaction.hide(mainFragment)
        }
        if (verticalChannelFragment.isAdded && !verticalChannelFragment.isHidden) {
            transaction.hide(verticalChannelFragment)
        }
        transaction.commit()
    }

    fun mainActive() {
        handler.removeCallbacks(hideMain)
        handler.postDelayed(hideMain, delayHideMain)
    }

    private val hideMain = Runnable {
        hideChannelList()
    }

    fun fragmentReady(tag: String) {
        fragmentReadyCount++
        Log.i(TAG, "ready $tag $fragmentReadyCount ")
        checkReady()
    }

    private fun checkReady() {
        if (playStarted) {
            return
        }
        if (isReady()) {
            playStarted = true
            mainFragment.fragmentReady()
            showTime()
        }
    }

    fun isReady(): Boolean {
        return networkReady > 0 && fragmentReadyCount >= READY_FRAGMENTS
    }

    fun showTime() {
        Log.i(TAG, "showTime ${SP.time}")
        if (SP.time) {
            timeFragment.show()
        } else {
            timeFragment.hide()
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            gestureDetector.onTouchEvent(event)
        }
        return super.onTouchEvent(event)
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            switchMainFragment()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            showSetting()
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (velocityY > 0) {
                if (!channelListVisible()) {
                    prev()
                }
            }
            if (velocityY < 0) {
                if (!channelListVisible()) {
                    next()
                }
            }
            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }

    private fun showSetting() {
        if (channelListVisible()) {
            return
        }

        if (sourceFragment.isVisible) {
            return
        }

        Log.i(TAG, "settingFragment ${settingFragment.isVisible}")
        if (!settingFragment.isVisible) {
            settingFragment.show(supportFragmentManager, "setting")
        } else {
            settingFragment.dismiss()
        }
    }

    private fun channelUp() {
        if (channelListVisible()) {
            return
        }
        if (SP.channelReversal) {
            next()
            return
        }
        prev()
    }

    private fun channelDown() {
        if (channelListVisible()) {
            return
        }
        if (SP.channelReversal) {
            prev()
            return
        }
        next()
    }

    private fun back() {
        if (channelListVisible()) {
            hideChannelList()
            return
        }

        // 弹窗逐级返回：直播源管理 → 设置页 → 退出
        if (sourceFragment.isVisible) {
            sourceFragment.dismiss()
            return
        }

        if (settingFragment.isVisible) {
            settingFragment.dismiss()
            return
        }

        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }

        doubleBackToExitPressedOnce = true
        Toast.makeText(this, "再按一次退出", Toast.LENGTH_SHORT).show()

        Handler(Looper.getMainLooper()).postDelayed({
            doubleBackToExitPressedOnce = false
        }, 2000)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Log.i(TAG, "keyCode $keyCode, event $event")
        // 直播源管理页只响应返回键（逐级返回），其余按键不落到主界面
        if (sourceFragment.isVisible && keyCode != KeyEvent.KEYCODE_BACK
            && keyCode != KeyEvent.KEYCODE_ESCAPE
        ) {
            return super.onKeyDown(keyCode, event)
        }
        when (keyCode) {
            KeyEvent.KEYCODE_0 -> {
                showChannel("0")
                return true
            }

            KeyEvent.KEYCODE_1 -> {
                showChannel("1")
                return true
            }

            KeyEvent.KEYCODE_2 -> {
                showChannel("2")
                return true
            }

            KeyEvent.KEYCODE_3 -> {
                showChannel("3")
                return true
            }

            KeyEvent.KEYCODE_4 -> {
                showChannel("4")
                return true
            }

            KeyEvent.KEYCODE_5 -> {
                showChannel("5")
                return true
            }

            KeyEvent.KEYCODE_6 -> {
                showChannel("6")
                return true
            }

            KeyEvent.KEYCODE_7 -> {
                showChannel("7")
                return true
            }

            KeyEvent.KEYCODE_8 -> {
                showChannel("8")
                return true
            }

            KeyEvent.KEYCODE_9 -> {
                showChannel("9")
                return true
            }

            KeyEvent.KEYCODE_ESCAPE -> {
                back()
                return true
            }

            KeyEvent.KEYCODE_BACK -> {
                back()
                return true
            }

            KeyEvent.KEYCODE_BOOKMARK -> {
                showSetting()
                return true
            }

            KeyEvent.KEYCODE_UNKNOWN -> {
                showSetting()
                return true
            }

            KeyEvent.KEYCODE_HELP -> {
                showSetting()
                return true
            }

            KeyEvent.KEYCODE_SETTINGS -> {
                showSetting()
                return true
            }

            KeyEvent.KEYCODE_MENU -> {
                showSetting()
                return true
            }

            KeyEvent.KEYCODE_ENTER -> {
                switchMainFragment()
            }

            KeyEvent.KEYCODE_DPAD_CENTER -> {
                switchMainFragment()
            }

            KeyEvent.KEYCODE_DPAD_UP -> {
                channelUp()
            }

            KeyEvent.KEYCODE_CHANNEL_UP -> {
                channelUp()
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> {
                channelDown()
            }

            KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                channelDown()
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (!channelListVisible() && !settingFragment.isVisible) {
                    switchMainFragment()
                    return true
                }
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (!channelListVisible() && !settingFragment.isVisible) {
                    showSetting()
                    return true
                }
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onStart() {
        Log.i(TAG, "onStart")
        super.onStart()
    }

    override fun onResume() {
        Log.i(TAG, "onResume")
        super.onResume()
        if (channelListVisible()) {
            handler.postDelayed(hideMain, delayHideMain)
        }
    }

    override fun onPause() {
        Log.i(TAG, "onPause")
        super.onPause()
        handler.removeCallbacks(hideMain)
    }

    override fun onDestroy() {
        networkCallback?.let {
            (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
                .unregisterNetworkCallback(it)
        }
        networkCallback = null
        super.onDestroy()
    }

    private companion object {
        const val TAG = "MainActivity"

        // PlayerFragment、TimeFragment、InfoFragment、ChannelFragment、MainFragment
        const val READY_FRAGMENTS = 5
    }
}