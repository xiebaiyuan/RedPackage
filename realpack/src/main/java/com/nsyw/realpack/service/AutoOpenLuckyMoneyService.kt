package com.nsyw.realpack.service

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.nsyw.base.base.BaseApp
import com.nsyw.base.utils.DisplayUtil
import com.nsyw.realpack.R
import com.nsyw.realpack.ui.MainActivity

class AutoOpenLuckyMoneyService : AccessibilityService() {

    private val tag = AutoOpenLuckyMoneyService::class.java.simpleName

    /** 是正在开红包 */
    private var isOpening = false

    /** 是否在查看红包 */
    private var isLooking = false

    override fun onServiceConnected() {
        serviceInfo.packageNames = arrayOf(Config.WechatPackageName)
        initView()
        Log.d(tag, "AutoOpenLuckyMoneyService Connected..")
    }

    override fun onDestroy() {
        Log.d(tag, "AutoOpenLuckyMoneyService Destroy..")
    }

    override fun onInterrupt() {
        Log.d(tag, "AutoOpenLuckyMoneyService Interrupt..")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowStateChanged(event)
            }

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                handleWindowContentChanged()
            }
        }
    }

    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        if (!isOpening && event.className == Config.RedPackageReceiveClassName) {
            isOpening = rootInActiveWindow?.let { openRedPackage(it) } ?: false
            if (!isOpening) {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
        } else if (isOpening && (event.className == Config.RedPackageDetailClassName || event.className == Config.LuckyMoneyBeforeDetailUI)) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            isOpening = false
        }
    }

    private fun handleWindowContentChanged() {
        if (!isLooking) {
            rootInActiveWindow?.let { rootNode ->
                when {
                    isChatListPage(rootNode) -> findRedPackageInChatList(rootNode)
                    isChatDetailPage(rootNode) -> {
                        if (!findAndClickRedPackage(rootNode) && Runtime.backHome) {
                            performGlobalAction(GLOBAL_ACTION_BACK)
                        } else {
                        }
                    }
                    isOpening && isRedPackageDialog(rootNode) && redPackageZero(rootNode) -> {
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        isOpening = false
                    }

                    else -> {}
                }
            }
        }
    }

    private fun redPackageZero(nodeInfo: AccessibilityNodeInfo): Boolean {
        val nodes = nodeInfo.findAccessibilityNodeInfosByViewId(Config.RedPackageZeroStrId)
        return nodes.firstOrNull()?.text == "手慢了，红包派完了"
    }

    private fun openRedPackage(nodeInfo: AccessibilityNodeInfo): Boolean {
        val openBtn = nodeInfo.findAccessibilityNodeInfosByViewId(Config.OpenButtonResId).firstOrNull()
        return if (openBtn?.isClickable == true) {
            openBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            true
        } else {
            false
        }
    }

    private fun findAndClickRedPackage(nodeInfo: AccessibilityNodeInfo): Boolean {
        isLooking = true
        val list = nodeInfo.findAccessibilityNodeInfosByViewId(Config.RedPackageLayoutResId)
        if (list.isNullOrEmpty()) {
            isLooking = false
            return false
        }

        val rootRect = Rect()
        nodeInfo.getBoundsInScreen(rootRect)
        val width = rootRect.width()

        for (i in list.size - 1 downTo 0) {
            val node = list[i]
            if (isValidRedPackage(node, width)) {
                clickRedPackage(node)
                isLooking = false
                return true
            }
        }
        isLooking = false
        return false
    }

    private fun isValidRedPackage(node: AccessibilityNodeInfo, width: Int): Boolean {
        if (node.findAccessibilityNodeInfosByViewId(Config.RedPackageTextResId).isEmpty()) return false
        if (node.findAccessibilityNodeInfosByViewId(Config.RedPackageExpiredResId).isNotEmpty()) return false

        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (rect.left > width - rect.right) return false

        return node.isClickable
    }

    private fun clickRedPackage(node: AccessibilityNodeInfo) {
        val delayTime = MainActivity.delayTime.toLong()
        if (delayTime <= 100) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }, delayTime)
        }
    }

    private fun findRedPackageInChatList(nodeInfo: AccessibilityNodeInfo): Boolean {
        isLooking = true
        val list = nodeInfo.findAccessibilityNodeInfosByViewId(Config.HomeRedPackageLayoutResId)
        if (list.isNullOrEmpty()) {
            isLooking = false
            return false
        }

        for (i in list.size - 1 downTo 0) {
            val node = list[i]
            if (isValidChatListRedPackage(node)) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                isLooking = false
                return true
            }
        }
        isLooking = false
        return false
    }

    private fun isValidChatListRedPackage(node: AccessibilityNodeInfo): Boolean {
        val contentView = node.findAccessibilityNodeInfosByViewId(Config.HomeRedPackageResId)
        if (contentView.isEmpty()) return false

        val hasRedPackage = contentView.any { it.text.split(":").getOrNull(1)?.contains("[微信红包]") == true }
        if (!hasRedPackage || !node.isClickable) return false

        val newMessage = node.findAccessibilityNodeInfosByViewId(Config.HomeRedPackageNewMessageResId).firstOrNull()
        return newMessage?.isVisibleToUser == true
    }

    private fun initView() {
        try {
            val wm = getSystemService(WINDOW_SERVICE) as? WindowManager
            val lp = WindowManager.LayoutParams().apply {
                type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
                format = PixelFormat.TRANSLUCENT
                flags = flags or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                width = DisplayUtil.dpToPx(BaseApp.getApp(), 60f)
                height = DisplayUtil.dpToPx(BaseApp.getApp(), 60f)
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
            }
            val view = LayoutInflater.from(this).inflate(R.layout.red_float, null)
            wm?.addView(view, lp)
        } catch (e: Exception) {
            Log.e(tag, e.toString())
        }
    }

    private fun isChatListPage(rootNode: AccessibilityNodeInfo): Boolean {
        return rootNode.findAccessibilityNodeInfosByViewId(Config.HomeRedPackageTitleResId)
            .any { it?.text?.contains("微信") == true }
    }

    private fun isChatDetailPage(rootNode: AccessibilityNodeInfo): Boolean {
        return rootNode.findAccessibilityNodeInfosByViewId(Config.ChatDetailPageLayoutResId).isNotEmpty()
    }

    private fun isRedPackageDialog(rootNode: AccessibilityNodeInfo): Boolean {
        return rootNode.findAccessibilityNodeInfosByViewId(Config.RedPackageDialogResId).isNotEmpty()
    }
}
