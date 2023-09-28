package com.github.tvbox.osc.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.tvbox.osc.event.RefreshEvent
import org.greenrobot.eventbus.EventBus

/**
 * @Author : Liu XiaoRan
 * @Email : 592923276@qq.com
 * @Date : on 2023/1/29 11:45.
 * @Description :
 */
class BatteryReceiver : BroadcastReceiver() {

    var currentBattery: Int = -1
    override fun onReceive(context: Context?, intent: Intent?) {
        if (Intent.ACTION_BATTERY_CHANGED == intent!!.action) {
            val level = intent.getIntExtra("level", 0)
            if (currentBattery != level) { // 电量变化
                currentBattery = level
                EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_BATTERY_CHANGE, currentBattery))
            }
        }
    }
}
