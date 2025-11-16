//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cc.bbq.xq.BuildConfig
import cc.bbq.xq.R
import android.content.Context
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.launch

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState // 添加 SnackbarHostState 参数
) {
    val context = LocalContext.current
    val versionName = BuildConfig.VERSION_NAME
    val versionCode = BuildConfig.VERSION_CODE
    val scope = rememberCoroutineScope() // 创建 CoroutineScope

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.xiaoqu),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(20.dp)),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "版本 $versionName (Build $versionCode)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        LicenseCard(
            onLicenseClick = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://gitee.com/Voltula/bbq/blob/master/LICENSE"))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    //Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show()
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.unable_to_open_browser),
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        )
        Spacer(modifier = Modifier.height(24.dp))
        OpenSourceCard(
            onGiteeClick = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://gitee.com/Voltula/bbq"))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    //Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show()
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.unable_to_open_browser),
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            },
            onReleasesClick = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://gitee.com/Voltula/bbq/releases/"))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    //Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show()
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.unable_to_open_browser),
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        )
        Spacer(modifier = Modifier.height(24.dp))
        DeveloperCard()
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "© 2025 Voltula. 保留所有权利。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LicenseCard(onLicenseClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "许可证",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "本程序采用 GNU 通用公共许可证第3版 (GPLv3) 发布。您可以自由地使用、修改和分发本程序，但必须遵守其条款。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            ClickableTextItem(
                text = "查看项目使用的GPLv3许可证副本",
                onClick = onLicenseClick
            )
        }
    }
}

@Composable
fun OpenSourceCard(onGiteeClick: () -> Unit, onReleasesClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "开源与下载",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            ClickableTextItem(text = "项目源码 (Gitee)", onClick = onGiteeClick)
            Spacer(modifier = Modifier.height(8.dp))
            ClickableTextItem(text = "下载可安装的APK包 (Releases)", onClick = onReleasesClick)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "提示：如果您不了解如何编译代码，请点击上方\"下载可安装的APK包\"链接，下载后缀为 .apk 的文件进行安装。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "本页面基于 Bilimiao 项目的代码修改而来，遵循 GPLv3 协议。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun DeveloperCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "开发者信息",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "开发者: Voltula (Voltual)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "目前由Voltual独立完成整个项目的开发",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClickableTextItem(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        color = MaterialTheme.colorScheme.surface,
        onClick = onClick,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = "打开链接",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}