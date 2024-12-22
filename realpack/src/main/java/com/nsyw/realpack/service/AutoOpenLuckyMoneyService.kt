package com.nsyw.realpack.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.nsyw.realpack.ui.MainActivity

class AutoOpenLuckyMoneyService : AccessibilityService() {

    private val tag = AutoOpenLuckyMoneyService::class.java.simpleName
    private var isOpening = false
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
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> handleWindowStateChanged(event)
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> handleWindowContentChanged()
        }
    }

    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        if (!isOpening && event.className == Config.RedPackageReceiveClassName) {
            isOpening = if (openRedPackage(rootInActiveWindow ?: return)) {
                true
            } else {
                performGlobalAction(GLOBAL_ACTION_BACK)
                false
            }
        } else if (isOpening && (event.className == Config.RedPackageDetailClassName || event.className == Config.LuckyMoneyBeforeDetailUI)) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            isOpening = false
        }
    }

    private fun handleWindowContentChanged() {
        if (!isLooking) {
            val rootNode = rootInActiveWindow ?: return
            when {
                isChatListPage(rootNode) -> findRedPackageInChatList(rootNode)
                isChatDetailPage(rootNode) -> {
                    if (!findAndClickRedPackage(rootNode) && Runtime.backHome) {
                        performGlobalAction(GLOBAL_ACTION_BACK)
                    }
                }

                isOpening && isRedPackageDialog(rootNode) && redPackageZero(rootNode) -> {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    isOpening = false
                }
            }
        }
    }

    private fun redPackageZero(nodeInfo: AccessibilityNodeInfo): Boolean {
        val nodes = nodeInfo.findAccessibilityNodeInfosByViewId(Config.RedPackageZeroStrId)
        return nodes.isNotEmpty() && nodes[0].text == "手慢了，红包派完了"
    }

    private fun openRedPackage(nodeInfo: AccessibilityNodeInfo): Boolean {
        val nodes = nodeInfo.findAccessibilityNodeInfosByViewId(Config.OpenButtonResId)
        if (nodes.isEmpty() || !nodes[0].isClickable) return false

        val delayTime = MainActivity.delayTime.toLong()
        if (delayTime <= 100) {
            nodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                nodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }, delayTime)
        }
        return true
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
                performDelayedClick(node)
                isLooking = false
                return true
            }
        }
        isLooking = false
        return false
    }

    private fun isValidRedPackage(node: AccessibilityNodeInfo, width: Int): Boolean {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return node.findAccessibilityNodeInfosByViewId(Config.RedPackageTextResId).isNotEmpty() &&
                node.findAccessibilityNodeInfosByViewId(Config.RedPackageExpiredResId).isEmpty() &&
                rect.left <= width - rect.right &&
                node.isClickable
    }

    private fun performDelayedClick(node: AccessibilityNodeInfo) {
        // 这里不延迟
        val delayTime = 0L;
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
            val contentView = node.findAccessibilityNodeInfosByViewId(Config.HomeRedPackageResId)
            if (contentView.isNotEmpty() && contentView[0].text.contains("[微信红包]")) {
                val newMessage =
                    node.findAccessibilityNodeInfosByViewId(Config.HomeRedPackageNewMessageResId)
                        .getOrNull(0)
                if (newMessage != null && newMessage.isVisibleToUser && node.isClickable) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    isLooking = false
                    return true
                }
            }
        }
        isLooking = false
        return false
    }

    private fun initView() {
        // 初始化悬浮窗视图的代码
    }

    private fun isChatListPage(rootNode: AccessibilityNodeInfo): Boolean {
        val nodeList = rootNode.findAccessibilityNodeInfosByViewId(Config.HomeRedPackageTitleResId)
        return nodeList.any { it?.text?.contains("微信") == true }
    }

    private fun isChatDetailPage(rootNode: AccessibilityNodeInfo): Boolean {
        val nodeList = rootNode.findAccessibilityNodeInfosByViewId(Config.ChatDetailPageLayoutResId)
        return nodeList.isNotEmpty()
    }

    private fun isRedPackageDialog(rootNode: AccessibilityNodeInfo): Boolean {
        val nodeList = rootNode.findAccessibilityNodeInfosByViewId(Config.RedPackageDialogResId)
        return nodeList.isNotEmpty()
    }
}