//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package cc.bbq.xq.ui.compose

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.navigation.NavController
import cc.bbq.xq.ui.Player
import cc.bbq.xq.ui.PostDetail
import java.util.regex.Pattern

private val INTERNAL_POST_LINK_PATTERN: Pattern = Pattern.compile(
    "http://apk\\.xiaoqu\\.online/post/(\\d+)\\.html"
)

private val BILI_VIDEO_LINK_PATTERN: Pattern = Pattern.compile(
    "【视频：([a-zA-Z0-9]+)】"
)

private val GENERAL_URL_PATTERN: Pattern = Pattern.compile(
    "(?:(?:https?|ftp)://|www\\.)[\\w\\-_]+(?:\\.[\\w\\-_]+)+(?:[\\w\\-.,@?^=%&:/~+#]*[\\w\\-@?^=%&;/~+#])?"
)

private data class LinkMatch(
    val start: Int,
    val end: Int,
    val text: String,
    val type: LinkType
)

private enum class LinkType {
    POST,
    BILIVIDEO,
    URL
}

@Composable
fun LinkifyText(
    text: String,
    navController: NavController,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    val context = LocalContext.current
    val linkColor = MaterialTheme.colorScheme.primary

    val textStyle = if (style.color == Color.Unspecified) {
        style.copy(color = MaterialTheme.colorScheme.onSurface)
    } else {
        style
    }

    val annotatedString = remember(text, linkColor) {
        buildAnnotatedString {
            val matches = mutableListOf<LinkMatch>()

            // 首先收集所有匹配的链接
            val postMatcher = INTERNAL_POST_LINK_PATTERN.matcher(text)
            while (postMatcher.find()) {
                postMatcher.group(1)?.let { postId ->
                    matches.add(
                        LinkMatch(
                            start = postMatcher.start(),
                            end = postMatcher.end(),
                            text = postId,
                            type = LinkType.POST
                        )
                    )
                }
            }
            
            val biliVideoMatcher = BILI_VIDEO_LINK_PATTERN.matcher(text)
            while (biliVideoMatcher.find()) {
                biliVideoMatcher.group(1)?.let { bvid ->
                    matches.add(
                        LinkMatch(
                            start = biliVideoMatcher.start(),
                            end = biliVideoMatcher.end(),
                            text = bvid,
                            type = LinkType.BILIVIDEO
                        )
                    )
                }
            }

            val urlMatcher = GENERAL_URL_PATTERN.matcher(text)
            while (urlMatcher.find()) {
                val isAlreadyMatched = matches.any { it.start == urlMatcher.start() && it.end == urlMatcher.end() }
                if (!isAlreadyMatched) {
                    matches.add(
                        LinkMatch(
                            start = urlMatcher.start(),
                            end = urlMatcher.end(),
                            text = urlMatcher.group(),
                            type = LinkType.URL
                        )
                    )
                }
            }

            // 按起始位置排序，确保按顺序处理
            val sortedMatches = matches.sortedBy { it.start }
            
            var lastIndex = 0
            sortedMatches.forEach { match ->
                // 添加匹配前的普通文本
                if (match.start > lastIndex) {
                    append(text.substring(lastIndex, match.start))
                }
                
                // 添加带链接的文本
                withLink(
                    url = match.type.name to match.text,
                    style = SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(text.substring(match.start, match.end))
                }
                
                lastIndex = match.end
            }
            
            // 添加剩余的普通文本
            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
    }

    SelectionContainer(modifier = modifier) {
        BasicText(
            text = annotatedString,
            style = textStyle,
            onTextLayout = { layoutResult ->
                // 可以在这里处理文本布局结果，如果需要的话
            }
        ) { offset, linkAnnotation ->
            // 处理链接点击
            linkAnnotation?.let { (type, data) ->
                when (LinkType.valueOf(type)) {
                    LinkType.POST -> {
                        data.toLongOrNull()?.let { postId ->
                            navController.navigate(PostDetail(postId).createRoute())
                        }
                    }
                    LinkType.BILIVIDEO -> {
                        navController.navigate(Player(data).createRoute())
                    }
                    LinkType.URL -> {
                        var urlToOpen = data
                        if (!urlToOpen.startsWith("http://") && !urlToOpen.startsWith("https://")) {
                            urlToOpen = "http://$urlToOpen"
                        }
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlToOpen))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("LinkifyText", "无法打开URL: $urlToOpen", e)
                        }
                    }
                }
            }
        }
    }
}