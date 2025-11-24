//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.ui.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.with
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cc.bbq.xq.R
import cc.bbq.xq.data.UserAgreementDataStore
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import cc.bbq.xq.ui.animation.materialSharedAxisX
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun UserAgreementDialog(
    onDismissRequest: () -> Unit,
    onAgreed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val agreementDataStore = remember { UserAgreementDataStore(context) }

    // 当前显示的协议索引
    var currentAgreementIndex by remember { mutableStateOf(0) }

    // 协议列表 - 修复：正确加载raw资源
    val agreements = remember {
        listOf(
            "《OpenQu 用户协议》" to loadRawResourceText(context, R.raw.useragreement),
            "《小趣空间用户协议》" to loadRawResourceText(context, R.raw.xiaoquuseragreement),
            "《弦-应用商店用户协议》" to loadRawResourceText(context, R.raw.sineuseragreement),
            "《弦-应用商店隐私政策》" to loadRawResourceText(context, R.raw.sineprivacypolicy)
        )
    }

    // 动画方向
    var animationForward by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = { /* 禁止点击外部取消 */ }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "请阅读并同意以下协议",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 修复：添加动画切换效果
                AnimatedContent(
                    targetState = currentAgreementIndex,
                    transitionSpec = {
                        materialSharedAxisX(
                            forward = animationForward,
                            slideDistance = 30,
                            durationMillis = 500
                        )
                    },
                    label = "协议切换动画"
                ) { targetIndex ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = agreements[targetIndex].first,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        ) {
                            val scrollState = rememberScrollState()
                            MarkDownText(
                                content = agreements[targetIndex].second,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    if (currentAgreementIndex > 0) {
                        Button(
                            onClick = {
                                animationForward = false
                                currentAgreementIndex--
                            },
                        ) {
                            Text(text = "上一个")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(80.dp)) // 占位保持布局平衡
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                when (currentAgreementIndex) {
                                    0 -> agreementDataStore.setUserAgreementAccepted(true)
                                    1 -> agreementDataStore.setXiaoquUserAgreementAccepted(true)
                                    2 -> agreementDataStore.setSineUserAgreementAccepted(true)
                                    3 -> agreementDataStore.setSinePrivacyPolicyAccepted(true)
                                }

                                if (currentAgreementIndex < agreements.size - 1) {
                                    animationForward = true
                                    currentAgreementIndex++
                                } else {
                                    onAgreed()
                                }
                            }
                        },
                    ) {
                        Text(text = if (currentAgreementIndex < agreements.size - 1) "同意并继续" else "同意")
                    }
                }
            }
        }
    }
}

// 新增：正确加载raw资源文件的函数
@Composable
fun loadRawResourceText(context: android.content.Context, @androidx.annotation.RawRes resId: Int): String {
    var text by remember { mutableStateOf("") }
    
    LaunchedEffect(resId) {
        withContext(Dispatchers.IO) {
            try {
                context.resources.openRawResource(resId).use { inputStream ->
                    text = inputStream.bufferedReader().use { it.readText() }
                }
            } catch (e: Exception) {
                text = "加载协议内容失败: ${e.message}"
            }
        }
    }
    
    return text
}