// 文件路径: cc/bbq/xq/util/FileActionUtil.kt
package cc.bbq.xq.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale

object FileActionUtil {

    /**
     * 打开文件
     * 自动识别文件类型并调用系统对应的应用打开
     *
     * @param context 上下文
     * @param file 要打开的文件
     */
    fun openFile(context: Context, file: File) {
        if (!file.exists()) {
            Toast.makeText(context, "文件不存在", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri: Uri
            
            // 获取文件的 MIME 类型
            val mimeType = getMimeType(file)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Android 7.0+ 需要使用 FileProvider
                // 注意：authority 必须与 AndroidManifest.xml 中的 provider authorities 一致
                val authority = "${context.packageName}.provider"
                uri = FileProvider.getUriForFile(context, authority, file)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                uri = Uri.fromFile(file)
            }

            intent.setDataAndType(uri, mimeType)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "未找到可打开此文件的应用", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        } catch (e: Exception) {
            Toast.makeText(context, "打开文件失败: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    /**
     * 获取文件的 MIME 类型
     */
    private fun getMimeType(file: File): String {
        val extension = getExtension(file.name)
        if (extension.isNotEmpty()) {
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase(Locale.getDefault()))
            if (mime != null) {
                return mime
            }
        }
        // 对于 APK 文件，强制返回正确的 MIME 类型
        if (file.name.endsWith(".apk", ignoreCase = true)) {
            return "application/vnd.android.package-archive"
        }
        return "*/*" // 未知类型
    }

    /**
     * 获取文件扩展名
     */
    private fun getExtension(fileName: String): String {
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex >= 0 && dotIndex < fileName.length - 1) {
            fileName.substring(dotIndex + 1)
        } else {
            ""
        }
    }
    
    /**
     * 格式化文件大小
     */
    fun formatFileSize(size: Long): String {
        val kb = 1024
        val mb = kb * 1024
        val gb = mb * 1024

        return when {
            size >= gb -> String.format(Locale.getDefault(), "%.2f GB", size.toFloat() / gb)
            size >= mb -> String.format(Locale.getDefault(), "%.2f MB", size.toFloat() / mb)
            size >= kb -> String.format(Locale.getDefault(), "%.2f KB", size.toFloat() / kb)
            else -> "$size B"
        }
    }
}