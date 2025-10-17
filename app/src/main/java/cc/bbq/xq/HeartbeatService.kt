//HeartbeatService.kt
//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。

package cc.bbq.xq

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*

class HeartbeatService : Service() {
    private var heartbeatJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startHeartbeat()
        return START_STICKY
    }

    private fun startHeartbeat() {
        if (heartbeatJob?.isActive == true) return
        
        heartbeatJob = scope.launch {
            while (true) {
                Log.d("HEARTBEAT", "Starting new heartbeat cycle.")
                
                supervisorScope {
                    val userToken = AuthManager.getCredentials(this@HeartbeatService)?.third

                    if (!userToken.isNullOrBlank()) {
                        launch {
                            try {
                                val response = RetrofitClient.instance.heartbeat(token = userToken)
                                Log.d("HEARTBEAT", "User Account Heartbeat Status: ${response.code()}")
                            } catch (e: Exception) {
                                Log.e("HEARTBEAT", "User Account Heartbeat Failed: ${e.message}")
                            }
                        }
                    }
                }
                // =======================================================

                delay(60 * 1000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        heartbeatJob?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}