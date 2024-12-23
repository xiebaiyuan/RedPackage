package com.nsyw.realpack.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.nsyw.base.base.BaseActivity
import com.nsyw.realpack.R
import com.nsyw.realpack.databinding.RealActivityMainBinding
import com.nsyw.realpack.vm.MainViewModel

class MainActivity : BaseActivity() {

    private val TAG = MainActivity::class.java.simpleName

    private val vm: MainViewModel by lazy {
        getViewModel()
    }

    private val binding by lazy {
        RealActivityMainBinding.inflate(layoutInflater)
    }

    private val accessibilityManager by lazy {
        getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    }

    private val listener by lazy {
        object : OnMainViewClickListener {
            override fun onNotificationOpenBtnClick() {
                startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
            }

            override fun onAccessibilityOpenBtnClick() {
                val agree = ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.WRITE_SECURE_SETTINGS
                )
                if (agree == PackageManager.PERMISSION_GRANTED) {
                    Settings.Secure.putString(
                        contentResolver,
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                        "com.nsyw.autoredpack/com.nsyw.realpack.service.AutoOpenLuckyMoneyService"
                    )
                    Settings.Secure.putString(
                        contentResolver,
                        Settings.Secure.ACCESSIBILITY_ENABLED,
                        if (vm.accessibilityOpenStatus.value == true) "0" else "1"
                    )
                } else {
                    startActivityForResult(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                        REQUEST_ACCESSIBILITY_CODE
                    )
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.lifecycleOwner = this
        binding.vm = vm
        binding.listener = listener
        binding.cbBackHome.setOnCheckedChangeListener { buttonView, isChecked ->
            com.nsyw.realpack.service.Runtime.backHome = isChecked
        }

        val tvDelayTime = findViewById<TextView>(R.id.tv_delay_time)
        val sbDelayTime = findViewById<SeekBar>(R.id.sb_delay_time)

        sbDelayTime.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                delayTime = progress * 500 // Convert progress to milliseconds
                tvDelayTime.text = "延迟时间: ${delayTime / 1000.0}s"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }


    override fun onStart() {
        super.onStart()
        vm.checkStatus(context = this, accessibilityManager = accessibilityManager)
    }

    override fun onDestroy() {
        binding.unbind()
        super.onDestroy()
    }

    interface OnMainViewClickListener {
        fun onNotificationOpenBtnClick()
        fun onAccessibilityOpenBtnClick()
    }

    companion object {
        var delayTime: Int = 0

        private const val REQUEST_NOTIFICATION_CODE = 1001
        private const val REQUEST_ACCESSIBILITY_CODE = 1002
        private const val REQUEST_PERMISSION_WRITE_SECURE_SETTINGS = 1003
    }

}