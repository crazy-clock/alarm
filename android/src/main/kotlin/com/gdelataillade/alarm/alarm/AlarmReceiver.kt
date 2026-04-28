package com.gdelataillade.alarm.alarm

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager

import io.flutter.Log

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_ALARM_STOP = "com.gdelataillade.alarm.ACTION_STOP"
        const val EXTRA_ALARM_ACTION = "EXTRA_ALARM_ACTION"

        private const val TAG = "AlarmReceiver"

        // 入口 WakeLock 持有时长。
        // 设计目标：从 BroadcastReceiver.onReceive 触发到 AlarmService.onStartCommand
        // 真正抓到自己的 WakeLock，整个过程必须保证 CPU 不进 idle。
        // 国产低电量 / Doze 状态下，pendingIntent.send() 之后到 service 启动之间
        // 实测有时会有 1-3 秒延迟，给 10 秒兜底足够，过长会无谓耗电。
        private const val ENTRY_WAKELOCK_TIMEOUT_MS = 10_000L
    }

    override fun onReceive(context: Context, intent: Intent) {
        // 入口埋点：本来正常路径不打日志，导致排查闹钟"是否真的触发了"非常困难。
        // 这一行一定要保留，是定位"系统是否把广播投递过来"的唯一信号。
        Log.d(TAG, "onReceive START action=${intent.action} extras=${intent.extras?.keySet()}")

        // Step 1: 入口立刻抓 WakeLock，防止 CPU 在 startForegroundService 之前回睡。
        // 用 try-catch + 超时自释放兜底，绝不能因为 WakeLock 抓不到而阻断后面的逻辑。
        val entryWakeLock: PowerManager.WakeLock? = try {
            val pm = context.applicationContext
                .getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "app:AlarmReceiverEntry"
            )?.apply {
                setReferenceCounted(false)
                acquire(ENTRY_WAKELOCK_TIMEOUT_MS)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Acquire entry wakelock failed: ${t.message}")
            null
        }

        // Step 2: 用 goAsync 申请把广播执行延长到异步线程，避免主线程超时。
        // 不论 try 块成功还是抛异常，finally 必须 release WakeLock 并 finish 异步广播。
        val pendingResult = goAsync()

        try {
            val action = intent.action

            // Stop alarm from notification stop button.
            if (action == ACTION_ALARM_STOP) {
                val id = intent.getIntExtra("id", 0)
                Log.d(TAG, "Received stop alarm command from notification, id: $id")
                val instance = AlarmService.instance
                if (instance != null) {
                    instance.handleStopAlarmCommand(id)
                    return
                }
                // 没有 service 实例就走默认路径继续起 service 处理 stop。
            }

            // Start Alarm Service.
            val serviceIntent = Intent(context, AlarmService::class.java)
            serviceIntent.putExtras(intent)

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val pendingIntent = PendingIntent.getForegroundService(
                        context,
                        1,
                        serviceIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    pendingIntent.send()
                    Log.d(TAG, "onReceive: pendingIntent.send() OK (S+)")
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                    Log.d(TAG, "onReceive: startForegroundService OK (O+)")
                } else {
                    context.startService(serviceIntent)
                    Log.d(TAG, "onReceive: startService OK (<O)")
                }
            } catch (t: Throwable) {
                // SecurityException / ForegroundServiceStartNotAllowedException 等都吞掉，
                // 不影响 entry wakelock 的释放。
                Log.e(TAG, "Start AlarmService failed: ${t.message}", t)
            }
        } finally {
            // Step 3: 释放入口 WakeLock。AlarmService 一旦 onStartCommand 跑起来
            // 会在自己内部抓一个独立的 WakeLock，不需要这里继续持有。
            try {
                if (entryWakeLock?.isHeld == true) {
                    entryWakeLock.release()
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Release entry wakelock failed: ${t.message}")
            }
            try {
                pendingResult.finish()
            } catch (t: Throwable) {
                Log.e(TAG, "BroadcastReceiver.PendingResult.finish failed: ${t.message}")
            }
        }
    }
}
