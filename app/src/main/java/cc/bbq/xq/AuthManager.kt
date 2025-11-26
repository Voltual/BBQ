//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
@file:Suppress("DEPRECATION")
package cc.bbq.xq

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.dataStoreFile
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import cc.bbq.xq.data.proto.UserCredentials
import cc.bbq.xq.data.proto.UserCredentialsSerializer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

private const val DATA_STORE_FILE_NAME = "auth_preferences.pb"

object AuthManager {

    private lateinit var masterKey: MasterKey
    private lateinit var encryptedFile: EncryptedFile

    // --- 初始化加密 ---
    fun initialize(context: Context) {
        masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val authDataStoreFile = context.dataStoreFile(DATA_STORE_FILE_NAME)

        encryptedFile = EncryptedFile.Builder(
            context,
            authDataStoreFile,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
    }

    // --- 保存凭证 ---
    suspend fun saveCredentials(
        context: Context,
        username: String,
        password: String,
        token: String,
        userId: Long
    ) {
        val currentCredentials = readCredentials()
        val newCredentials = UserCredentials.newBuilder()
            .setUsername(username)
            .setPassword(password)
            .setToken(token)
            .setUserId(userId)
            .setDeviceId(currentCredentials?.deviceId?.ifEmpty { generateDeviceId() } ?: generateDeviceId())
            .setSineMarketToken(currentCredentials?.sineMarketToken ?: "") // 保留现有的弦应用商店token
            .build()
        
        writeCredentials(newCredentials)
    }

    // --- 新增：保存弦应用商店token ---
    suspend fun saveSineMarketToken(context: Context, token: String) {
        val currentCredentials = readCredentials() ?: UserCredentials.getDefaultInstance()
        val newCredentials = currentCredentials.toBuilder()
            .setSineMarketToken(token)
            .build()
        
        writeCredentials(newCredentials)
    }

    // --- 获取凭证 ---
    fun getCredentials(context: Context): Flow<UserCredentials?> {
        return kotlinx.coroutines.flow.flow {
            emit(readCredentials())
        }
    }

    // 新增方法：获取弦应用商店token
    fun getSineMarketToken(context: Context): Flow<String> {
        return getCredentials(context).map { userCredentials ->
            userCredentials?.sineMarketToken ?: ""
        }
    }

    // 新增方法：单独获取userid
    fun getUserId(context: Context): Flow<Long> {
        return getCredentials(context).map { userCredentials ->
            userCredentials?.userId ?: -1L
        }
    }

    // --- 清除凭证 ---
    suspend fun clearCredentials(context: Context) {
        writeCredentials(UserCredentials.getDefaultInstance())
    }

    // --- 获取设备ID ---
    fun getDeviceId(context: Context): Flow<String> {
        return getCredentials(context).map { userCredentials ->
            userCredentials?.deviceId ?: generateDeviceId()
        }
    }

    // --- 私有方法：读取凭证 ---
    private suspend fun readCredentials(): UserCredentials? {
        return try {
            if (!::encryptedFile.isInitialized) {
                return null
            }
            
            if (!File(encryptedFile.file.absolutePath).exists()) {
                return null
            }
            
            encryptedFile.openFileInput().use { inputStream ->
                UserCredentialsSerializer.readFrom(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- 私有方法：写入凭证 ---
    private suspend fun writeCredentials(credentials: UserCredentials) {
        if (!::encryptedFile.isInitialized) {
            throw IllegalStateException("AuthManager not initialized. Call initialize() first.")
        }
        
        encryptedFile.openFileOutput().use { outputStream ->
            UserCredentialsSerializer.writeTo(credentials, outputStream)
        }
    }

    // --- 生成设备ID ---
    private fun generateDeviceId(): String {
        return (1..15).joinToString("") { (0..9).random().toString() }
    }

    // --- 迁移 SharedPreferences 到加密存储 ---
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

            val credentials = UserCredentials.newBuilder()
                .setUsername(username)
                .setPassword(password)
                .setToken(token)
                .setUserId(userId)
                .setDeviceId(deviceId)
                .setSineMarketToken("")
                .build()
            
            writeCredentials(credentials)

            // 清除 SharedPreferences 中的数据
            sharedPrefs.edit().clear().apply()
            
            // 删除未加密的旧 DataStore 文件（如果存在）
            val oldDataStoreFile = context.dataStoreFile(DATA_STORE_FILE_NAME)
            if (oldDataStoreFile.exists()) {
                oldDataStoreFile.delete()
            }
        }
    }
}