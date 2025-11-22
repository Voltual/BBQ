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
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import cc.bbq.xq.data.proto.UserCredentials
import cc.bbq.xq.data.proto.UserCredentialsSerializer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

private const val DATA_STORE_FILE_NAME = "auth_preferences.pb"

object AuthManager {

    private lateinit var encryptedAuthDataStore: DataStore<UserCredentials>

    // --- 初始化加密 DataStore ---
    fun initialize(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val authDataStoreFile = context.dataStoreFile(DATA_STORE_FILE_NAME)

        val encryptedFile = EncryptedFile.Builder(
            authDataStoreFile,
            context,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        encryptedAuthDataStore = DataStoreFactory.create(
            serializer = UserCredentialsSerializer(context = context),
            produceFile = {
                authDataStoreFile
            }
        )
    }

    // --- 保存凭证 ---
    suspend fun saveCredentials(
        context: Context,
        username: String,
        password: String,
        token: String,
        userId: Long
    ) {
        encryptedAuthDataStore.updateData {
            UserCredentials.newBuilder()
                .setUsername(username)
                .setPassword(password)
                .setToken(token)
                .setUserId(userId)
                .setDeviceId(generateDeviceId())
                .build()
        }
    }

    // --- 获取凭证 ---
    fun getCredentials(context: Context): Flow<UserCredentials?> {
        return encryptedAuthDataStore.data
    }

    // 新增方法：单独获取userid
    fun getUserId(context: Context): Flow<Long> {
        return encryptedAuthDataStore.data
            .map { userCredentials ->
                userCredentials?.userId ?: -1L
            }
    }

    // --- 清除凭证 ---
    suspend fun clearCredentials(context: Context) {
        encryptedAuthDataStore.updateData {
            UserCredentials.newBuilder().build() // 设置为空
        }
    }

    // --- 获取设备ID ---
    fun getDeviceId(context: Context): Flow<String> {
        return encryptedAuthDataStore.data
            .map { userCredentials ->
                userCredentials?.deviceId ?: generateDeviceId()
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
            val username = String(Base64.decode(encodedUser, Base64.DEFAULT))
            val password = String(Base64.decode(encodedPass, Base64.DEFAULT))

            encryptedAuthDataStore.updateData {
                UserCredentials.newBuilder()
                    .setUsername(username)
                    .setPassword(password)
                    .setToken(token)
                    .setUserId(userId)
                    .setDeviceId(deviceId)
                    .build()
            }

            // 清除 SharedPreferences 中的数据
            sharedPrefs.edit().clear().apply()
        }
    }
}