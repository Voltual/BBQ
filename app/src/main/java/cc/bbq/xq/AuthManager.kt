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
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_preferences")

object AuthManager {
    // --- 键 ---
    private val USER_TOKEN = stringPreferencesKey("usertoken")
    private val USER_USERNAME = stringPreferencesKey("username")
    private val USER_PASSWORD = stringPreferencesKey("password")
    private val USER_ID = longPreferencesKey("userid")
    private val DEVICE_ID = stringPreferencesKey("device_id")

    // --- 数据类 ---
    data class UserCredentials(
        val username: String,
        val password: String,
        val token: String,
        val userId: Long,
        val deviceId: String
    )

    // --- 保存凭证 ---
    suspend fun saveCredentials(
        context: Context,
        username: String,
        password: String,
        token: String,
        userId: Long
    ) {
        context.authDataStore.edit { preferences ->
            preferences[USER_USERNAME] = Base64.encodeToString(username.toByteArray(), Base64.DEFAULT)
            preferences[USER_PASSWORD] = Base64.encodeToString(password.toByteArray(), Base64.DEFAULT)
            preferences[USER_TOKEN] = token
            preferences[USER_ID] = userId
            preferences[DEVICE_ID] = generateDeviceId()
        }
    }

    // --- 获取凭证 ---
    fun getCredentials(context: Context): Flow<UserCredentials?> {
        return context.authDataStore.data
            .map { preferences ->
                val encodedUser = preferences[USER_USERNAME]
                val encodedPass = preferences[USER_PASSWORD]
                val token = preferences[USER_TOKEN]
                val userId = preferences[USER_ID] ?: -1
                val deviceId = preferences[DEVICE_ID] ?: generateDeviceId()

                if (encodedUser != null && encodedPass != null && token != null && userId != -1L) {
                    val username = String(Base64.decode(encodedUser, Base64.DEFAULT))
                    val password = String(Base64.decode(encodedPass, Base64.DEFAULT))
                    UserCredentials(username, password, token, userId, deviceId)
                } else {
                    null
                }
            }
    }

    // 新增方法：单独获取userid
    fun getUserId(context: Context): Flow<Long?> {
        return context.authDataStore.data
            .map { preferences ->
                preferences[USER_ID]
            }
    }

    // --- 清除凭证 ---
    suspend fun clearCredentials(context: Context) {
        context.authDataStore.edit { preferences ->
            preferences.remove(USER_USERNAME)
            preferences.remove(USER_PASSWORD)
            preferences.remove(USER_TOKEN)
            preferences.remove(USER_ID)
        }
    }

    // --- 获取设备ID ---
    fun getDeviceId(context: Context): Flow<String> {
        return context.authDataStore.data
            .map { preferences ->
                preferences[DEVICE_ID] ?: generateDeviceId()
            }
    }

    // --- 生成设备ID ---
    private fun generateDeviceId(): String {
        return (1..15).joinToString("") { (0..9).random().toString() }
    }

    // --- 迁移 SharedPreferences 到 DataStore ---
    suspend fun migrateFromSharedPreferences(context: Context) {
        val sharedPrefs = context.getSharedPreferences("bbq_auth", Context.MODE_PRIVATE)

        val encodedUser = sharedPrefs.getString("username", null)
        val encodedPass = sharedPrefs.getString("password", null)
        val token = sharedPrefs.getString("usertoken", null)
        val userId = sharedPrefs.getLong("userid", -1)
        val deviceId = sharedPrefs.getString("device_id", null) ?: generateDeviceId()

        if (encodedUser != null && encodedPass != null && token != null && userId != -1L) {
            context.authDataStore.edit { preferences ->
                preferences[USER_USERNAME] = encodedUser
                preferences[USER_PASSWORD] = encodedPass
                preferences[USER_TOKEN] = token
                preferences[USER_ID] = userId
                preferences[DEVICE_ID] = deviceId
            }

            // 清除 SharedPreferences 中的数据
            sharedPrefs.edit().clear().apply()
        }
    }
}