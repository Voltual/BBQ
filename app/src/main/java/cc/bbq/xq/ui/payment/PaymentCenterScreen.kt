//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.ui.payment

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import coil3.compose.AsyncImage
import androidx.navigation.NavController
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.Button
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cc.bbq.xq.ui.theme.AppShapes
import cc.bbq.xq.ui.theme.BBQButton
import cc.bbq.xq.ui.theme.BBQCard
import cc.bbq.xq.ui.theme.BBQOutlinedButton
import kotlinx.coroutines.launch

@Composable
fun PaymentCenterScreen(
    viewModel: PaymentViewModel,
//    navController: NavController,
    modifier: Modifier = Modifier // 新增：接收外部 modifier
) {
    val isLoadingBalance by viewModel.isLoadingBalance.collectAsState()
    val paymentInfo by viewModel.paymentInfo.collectAsState()
    val coinsBalance by viewModel.coinsBalance.collectAsState()
    val paymentStatus by viewModel.paymentStatus.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current

    // 支付成功后自动触发下载
    LaunchedEffect(paymentStatus) {
        if (paymentStatus == PaymentStatus.SUCCESS) {
            val downloadUrl = viewModel.getDownloadUrl()
            if (!downloadUrl.isNullOrEmpty()) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // 处理异常
                }
            }
        }
    }

    when (paymentStatus) {
        PaymentStatus.SUCCESS -> {
            PaymentResultDialog(
                success = true,
                onDismiss = { viewModel.resetPaymentStatus() },
                onDownload = {
                    val downloadUrl = viewModel.getDownloadUrl()
                    if (!downloadUrl.isNullOrEmpty()) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // 处理异常
                        }
                    }
                },
                showDownloadButton = paymentInfo?.type == PaymentType.APP_PURCHASE
            )
        }
        PaymentStatus.FAILED -> {
            PaymentResultDialog(
                success = false,
                error = errorMessage,
                onDismiss = { viewModel.resetPaymentStatus() },
                showDownloadButton = false
            )
        }
        else -> {
            PaymentContent(
                paymentInfo = paymentInfo,
                coinsBalance = coinsBalance,
                isLoadingBalance = isLoadingBalance,
                errorMessage = errorMessage,
                onFetchBalance = { viewModel.fetchCoinsBalance() },
                onPay = { amount -> viewModel.executePayment(amount) },
                viewModel = viewModel,
                isPaymentProcessing = paymentStatus == PaymentStatus.PROCESSING, // 根据状态判断是否正在处理
                modifier = modifier // 传递 modifier
            )
        }
    }
}

@Composable
fun PaymentContent(
    paymentInfo: PaymentInfo?,
    coinsBalance: Int?,
    isLoadingBalance: Boolean,
    errorMessage: String?,
    onFetchBalance: () -> Unit,
    onPay: (Int) -> Unit,
    viewModel: PaymentViewModel,
    isPaymentProcessing: Boolean = false, // 添加一个参数来表示是否正在支付处理中
    modifier: Modifier = Modifier // 新增：接收外部 modifier
) {
//    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var amount by remember { mutableStateOf(paymentInfo?.price?.toString() ?: "") }

    var postIdInput by remember { mutableStateOf("") }
    val isAdvancedMode = paymentInfo?.type == PaymentType.POST_REWARD && paymentInfo.postId == 0L

    // 移除 Scaffold 包装，直接使用 Column
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isAdvancedMode) {
            // 高级模式界面
            BBQCard(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "高级支付模式",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = postIdInput,
                        onValueChange = { postIdInput = it },
                        label = { Text("输入帖子ID") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val postId = postIdInput.toLongOrNull()
                            if (postId != null && postId > 0) {
                                viewModel.loadPostInfo(postId)
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("请输入有效的帖子ID")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("加载帖子")
                    }
                }
            }
        } else {
            // ==================== 打赏/购买对象信息卡片 ====================
            BBQCard(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.medium
            ) {
                when (paymentInfo?.type) {
                    PaymentType.APP_PURCHASE -> {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 应用图标和名称
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // 显示应用图标
                                AsyncImage(
                                    model = paymentInfo.iconUrl,
                                    contentDescription = "应用图标",
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )

                                Spacer(Modifier.width(16.dp))

                                Column {
                                    Text(
                                        paymentInfo.appName,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "版本: ${paymentInfo.versionId}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // 显示预览内容
                            Text(
                                text = paymentInfo.previewContent,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            // 价格信息
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "价格",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    "${paymentInfo.price} 硬币",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    PaymentType.POST_REWARD -> {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 帖子标题
                            Text(
                                paymentInfo.postTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            // 显示预览内容
                            Text(
                                text = paymentInfo.previewContent,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )

                            // 作者信息
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                AsyncImage(
                                    model = paymentInfo.authorAvatar,
                                    contentDescription = "作者头像",
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )

                                Spacer(Modifier.width(12.dp))

                                Column {
                                    Text(
                                        paymentInfo.authorName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "发布于 ${paymentInfo.postTime}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        // 默认布局
                        Text(
                            "支付信息",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // ==================== 支付金额区域 ====================
            if (paymentInfo?.type == PaymentType.POST_REWARD) {
                BBQCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "打赏金额",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // 金额选择器
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(5, 10, 20, 50, 100).forEach { coinAmount ->
                                CoinAmountChip(
                                    amount = coinAmount,
                                    selected = amount == coinAmount.toString(),
                                    onClick = { amount = coinAmount.toString() }
                                )
                            }
                        }

                        // 自定义金额输入
                        OutlinedTextField(
                            value = amount,
                            onValueChange = {
                                if (it.isEmpty() || it.toIntOrNull() != null) {
                                    amount = it
                                }
                            },
                            label = { Text("自定义金额") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            trailingIcon = {
                                Text("硬币", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        )
                    }
                }
            }
        }

        // ==================== 硬币余额区域 ====================
        BBQCard(
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "我的硬币",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    BBQOutlinedButton(
                        onClick = onFetchBalance,
                        modifier = Modifier.width(120.dp),
                        contentPadding = PaddingValues(vertical = 6.dp),
                        enabled = !isLoadingBalance,
                        text = {
                            if (isLoadingBalance) Text("查询中...")
                            else if (coinsBalance == null) Text("查询余额")
                            else Text("刷新余额")
                        }
                    )
                }

                // 余额显示区域
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "当前余额:",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )

                    // 显示余额或加载状态
                    when {
                        isLoadingBalance -> CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        coinsBalance == null -> Text("--", style = MaterialTheme.typography.bodyLarge)
                        else -> {
                            Text(
                                coinsBalance.toString(),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("硬币", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        // ==================== 支付按钮区域 ====================
        val payAmount = amount.toIntOrNull() ?: 0
        BBQCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            shape = AppShapes.medium
        ) {
            BBQButton(
                onClick = {
                    if (payAmount > 0) {
                        if (coinsBalance != null && payAmount > coinsBalance) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("硬币余额不足，请充值")
                            }
                        } else {
                            onPay(payAmount)
                        }
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("请输入有效的支付金额")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = payAmount > 0 && (coinsBalance == null || payAmount <= coinsBalance) && !isPaymentProcessing, // 禁用条件
                contentPadding = PaddingValues(vertical = 16.dp),
                text = {
                    if (isPaymentProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = when {
                                paymentInfo?.type == PaymentType.APP_PURCHASE ->
                                    "确认支付 ${paymentInfo.price} 硬币购买应用"

                                paymentInfo?.type == PaymentType.POST_REWARD ->
                                    "打赏 $payAmount 硬币"

                                else -> "确认支付 $payAmount 硬币"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            )
        }
    }

    // 错误消息处理
    errorMessage?.let { msg ->
        LaunchedEffect(msg) {
            snackbarHostState.showSnackbar(msg)
        }
    }
}

@Composable
private fun CoinAmountChip(
    amount: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$amount",
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun PaymentResultDialog(
    success: Boolean,
    error: String? = null,
    onDismiss: () -> Unit,
    onDownload: (() -> Unit)? = null,
    showDownloadButton: Boolean = false
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        BBQCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = AppShapes.large
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (success) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "支付成功",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = "支付失败",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(72.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = if (success) "支付成功！" else "支付失败",
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (success) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )

                if (!success && !error.isNullOrEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(32.dp))

                if (success && showDownloadButton) {
                    BBQButton(
                        onClick = { onDownload?.invoke() },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        text = { Text("下载应用") }
                    )
                    Spacer(Modifier.height(12.dp))
                }

                BBQOutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    text = { Text(if (success) "完成" else "重试") }
                )
            }
        }
    }
}