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
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import cc.bbq.xq.data.proto.UserCredentials
import cc.bbq.xq.data.proto.UserCredentialsSerializer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileNotFoundException

private const val ENCRYPTED_FILE_NAME = "user_credentials_encrypted.pb"

object AuthManager {

    private lateinit var masterKey: MasterKey
    private var encryptedFile: EncryptedFile? = null
    private lateinit var dataStoreFile: File

    // --- 初始化加密 ---
    fun initialize(context: Context) {
        masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        // 使用新的文件名
        dataStoreFile = File(context.filesDir, ENCRYPTED_FILE_NAME)
        
        // 只有在文件不存在时才创建 EncryptedFile 实例
        // 如果文件已存在，我们将在需要时再创建 EncryptedFile
    }

    // --- 获取或创建 EncryptedFile ---
    private fun getOrCreateEncryptedFile(context: Context): EncryptedFile {
        return encryptedFile ?: run {
            val file = EncryptedFile.Builder(
                context,
                dataStoreFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
            encryptedFile = file
            file
        }
    }


    // --- 保存凭证 ---
    suspend fun saveCredentials(
        context: Context,
        username: String,
        password: String,
        token: String,
        userId: Long
    ) {
        val currentCredentials = readCredentials(context)
        val newCredentials = UserCredentials.newBuilder()
            .setUsername(username)
            .setPassword(password)
            .setToken(token)
            .setUserId(userId)
            .setDeviceId(currentCredentials?.deviceId?.ifEmpty { generateDeviceId() } ?: generateDeviceId())
            .setSineMarketToken(currentCredentials?.sineMarketToken ?: "") // 保留现有的弦应用商店token
            .setSineOpenMarketToken(currentCredentials?.sineOpenMarketToken ?: "") // 保留现有的弦应用商店开放平台token
            .build()
        
        writeCredentials(context, newCredentials)
    }

    // --- 新增：保存弦应用商店token（api.market.sineworld.cn）---
    suspend fun saveSineMarketToken(context: Context, token: String) {
        val currentCredentials = readCredentials(context) ?: UserCredentials.getDefaultInstance()
        val newCredentials = currentCredentials.toBuilder()
            .setSineMarketToken(token)
            .build()
        
        writeCredentials(context, newCredentials)
    }

    // --- 新增：保存弦应用商店开放平台token（open.market.sineworld.cn）---
    suspend fun saveSineOpenMarketToken(context: Context, token: String) {
        val currentCredentials = readCredentials(context) ?: UserCredentials.getDefaultInstance()
        val newCredentials = currentCredentials.toBuilder()
            .setSineOpenMarketToken(token)
            .build()
        
        writeCredentials(context, newCredentials)
    }

    // --- 获取凭证 ---
    fun getCredentials(context: Context): Flow<UserCredentials?> {
        return flow {
            emit(readCredentials(context))
        }
    }

    // 获取弦应用商店token（api.market.sineworld.cn）
    fun getSineMarketToken(context: Context): Flow<String> {
        return getCredentials(context).map { userCredentials ->
            userCredentials?.sineMarketToken ?: ""
        }
    }

    // 新增方法：获取弦应用商店开放平台token（open.market.sineworld.cn）
    fun getSineOpenMarketToken(context: Context): Flow<String> {
        return getCredentials(context).map { userCredentials ->
            userCredentials?.sineOpenMarketToken ?: ""
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
        writeCredentials(context, UserCredentials.getDefaultInstance())
    }

    // --- 获取设备ID ---
    fun getDeviceId(context: Context): Flow<String> {
        return getCredentials(context).map { userCredentials ->
            userCredentials?.deviceId ?: generateDeviceId()
        }
    }

    // --- 私有方法：读取凭证 ---
    private suspend fun readCredentials(context: Context): UserCredentials? {
        return try {
            if (!::masterKey.isInitialized) {
                return null
            }
            
            // 检查文件是否存在
            if (!dataStoreFile.exists()) {
                return null
            }
            
            val encryptedFile = getOrCreateEncryptedFile(context)
            encryptedFile.openFileInput().use { inputStream ->
                UserCredentialsSerializer.readFrom(inputStream)
            }
        } catch (e: FileNotFoundException) {
            // 文件不存在，返回null
            null
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果读取失败，尝试删除损坏的文件并返回null
            try {
                if (dataStoreFile.exists()) {
                    dataStoreFile.delete()
                }
                // 重置 encryptedFile 引用
                encryptedFile = null
            } catch (deleteException: Exception) {
                deleteException.printStackTrace()
            }
            null
        }
    }

    // --- 私有方法：写入凭证 ---
    private suspend fun writeCredentials(context: Context, credentials: UserCredentials) {
        if (!::masterKey.isInitialized) {
            throw IllegalStateException("AuthManager not initialized. Call initialize() first.")
        }
        
        try {
            // 如果文件已存在，先删除它
            if (dataStoreFile.exists()) {
                dataStoreFile.delete()
                // 重置 encryptedFile 引用，因为文件已被删除
                encryptedFile = null
            }
            
            val encryptedFile = getOrCreateEncryptedFile(context)
            encryptedFile.openFileOutput().use { outputStream ->
                UserCredentialsSerializer.writeTo(credentials, outputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果写入失败，尝试删除损坏的文件
            try {
                if (dataStoreFile.exists()) {
                    dataStoreFile.delete()
                }
                // 重置 encryptedFile 引用
                encryptedFile = null
            } catch (deleteException: Exception) {
                deleteException.printStackTrace()
            }
            throw e // 重新抛出异常
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
                .setSineOpenMarketToken("")
                .build()
            
            writeCredentials(context, credentials)

            // 清除 SharedPreferences 中的数据
            sharedPrefs.edit().clear().apply()
            
            // 删除旧的未加密 DataStore 文件（如果存在）
            val oldDataStoreFile = File(context.filesDir.parent, "datastore/auth_preferences.pb")
            if (oldDataStoreFile.exists()) {
                oldDataStoreFile.delete()
            }
        }
    }

    // --- 新增：检查文件是否损坏 ---
    suspend fun isFileCorrupted(context: Context): Boolean {
        return try {
            val credentials = readCredentials(context)
            // 如果文件存在但无法读取有效数据，则认为文件损坏
            dataStoreFile.exists() && credentials == null
        } catch (e: Exception) {
            true
        }
    }

    // --- 新增：重置加密文件 ---
    suspend fun resetEncryptedFile(context: Context) {
        try {
            if (dataStoreFile.exists()) {
                dataStoreFile.delete()
            }
            // 重置 encryptedFile 引用
            encryptedFile = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}