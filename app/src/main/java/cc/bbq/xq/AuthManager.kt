//AuthManager.kt
//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package cc.bbq.xq

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit

object AuthManager {
    private const val PREFS_NAME = "bbq_auth"
    
    // --- 用户凭证键 ---
    private const val KEY_USER_TOKEN = "usertoken"
    private const val KEY_USER_USERNAME = "username"
    private const val KEY_USER_PASSWORD = "password"
    private const val KEY_USER_ID = "userid"
    private const val KEY_DEVICE = "device_id"

    // --- 用户凭证方法 ---

    fun saveCredentials(
        context: Context,
        username: String,
        password: String,
        token: String,
        userId: Long
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_USER_USERNAME, Base64.encodeToString(username.toByteArray(), Base64.DEFAULT))
            putString(KEY_USER_PASSWORD, Base64.encodeToString(password.toByteArray(), Base64.DEFAULT))
            putString(KEY_USER_TOKEN, token)
            putLong(KEY_USER_ID, userId)
            putString(KEY_DEVICE, generateDeviceId())
        }
    }

    fun getCredentials(context: Context): Quadruple<String, String, String, Long>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encodedUser = prefs.getString(KEY_USER_USERNAME, null)
        val encodedPass = prefs.getString(KEY_USER_PASSWORD, null)
        val token = prefs.getString(KEY_USER_TOKEN, null)
        val userId = prefs.getLong(KEY_USER_ID, -1)
        
        return if (encodedUser != null && encodedPass != null && token != null && userId != -1L) {
            val username = String(Base64.decode(encodedUser, Base64.DEFAULT))
            val password = String(Base64.decode(encodedPass, Base64.DEFAULT))
            Quadruple(username, password, token, userId)
        } else {
            null
        }
    }

    // 新增方法：单独获取userid
    fun getUserId(context: Context): Long? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userId = prefs.getLong(KEY_USER_ID, -1)
        return if (userId != -1L) userId else null
    }

    fun clearCredentials(context: Context) {
        // 这个方法现在只清除主用户的凭证
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            remove(KEY_USER_USERNAME)
            remove(KEY_USER_PASSWORD)
            remove(KEY_USER_TOKEN)
            remove(KEY_USER_ID)
        }
    }
    
    // --- 通用方法 ---

    fun getDeviceId(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DEVICE, generateDeviceId()) ?: generateDeviceId()
    }

    private fun generateDeviceId(): String {
        return (1..15).joinToString("") { (0..9).random().toString() }
    }
}

// Quadruple 数据类保持不变
data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)