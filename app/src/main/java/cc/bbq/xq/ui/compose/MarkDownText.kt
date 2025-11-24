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
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.navigation.NavController
import cc.bbq.xq.ui.Player
import cc.bbq.xq.ui.PostDetail
import java.util.regex.Pattern

private val HEADER_PATTERN: Pattern = Pattern.compile("^(#{1,3})\\s+(.+)$", Pattern.MULTILINE)
private val BOLD_PATTERN: Pattern = Pattern.compile("\\*\\*(.+?)\\*\\*")
private val ITALIC_PATTERN: Pattern = Pattern.compile("\\*(.+?)\\*")
private val LINK_PATTERN: Pattern = Pattern.compile("\\[([^\\]]+)\\]\\(([^\\)]+)\\)")
private val LIST_ITEM_PATTERN: Pattern = Pattern.compile("^\\s*[-*]\\s+(.+)$", Pattern.MULTILINE)

private data class MarkdownElement(
    val start: Int,
    val end: Int,
    val text: String,
    val type: MarkdownType,
    val level: Int = 0, // For headers
    val url: String? = null // For links
)

private enum class MarkdownType {
    HEADER,
    BOLD,
    ITALIC,
    LINK,
    LIST_ITEM
}

@Composable
fun MarkDownText(
    text: String,
    navController: NavController,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    val textStyle = if (style.color == Color.Unspecified) {
        style.copy(color = onSurfaceColor)
    } else {
        style
    }

    val annotatedString = remember(text, primaryColor) {
        buildAnnotatedString {
            // Split text into lines to process headers and list items
            val lines = text.lines()
            val elements = mutableListOf<MarkdownElement>()
            var currentPosition = 0

            lines.forEachIndexed { lineIndex, line ->
                val lineStart = currentPosition
                
                // Check for header
                val headerMatcher = HEADER_PATTERN.matcher(line)
                if (headerMatcher.find()) {
                    val headerLevel = headerMatcher.group(1).length
                    val headerText = headerMatcher.group(2)
                    elements.add(
                        MarkdownElement(
                            start = lineStart,
                            end = lineStart + headerText.length,
                            text = headerText,
                            type = MarkdownType.HEADER,
                            level = headerLevel
                        )
                    )
                    append(headerText)
                } 
                // Check for list item
                else {
                    val listMatcher = LIST_ITEM_PATTERN.matcher(line)
                    if (listMatcher.find()) {
                        val listText = listMatcher.group(1)
                        elements.add(
                            MarkdownElement(
                                start = lineStart,
                                end = lineStart + listText.length,
                                text = listText,
                                type = MarkdownType.LIST_ITEM
                            )
                        )
                        append("• $listText")
                    } else {
                        // Process inline elements in regular lines
                        append(line)
                    }
                }

                // Add newline if not the last line
                if (lineIndex < lines.size - 1) {
                    append("\n")
                    currentPosition += line.length + 1
                } else {
                    currentPosition += line.length
                }
            }

            // Now process inline elements (bold, italic, links) in the built string
            val fullText = toString()
            
            // Process bold
            val boldMatcher = BOLD_PATTERN.matcher(fullText)
            while (boldMatcher.find()) {
                val boldText = boldMatcher.group(1)
                elements.add(
                    MarkdownElement(
                        start = boldMatcher.start(1),
                        end = boldMatcher.end(1),
                        text = boldText,
                        type = MarkdownType.BOLD
                    )
                )
            }

            // Process italic
            val italicMatcher = ITALIC_PATTERN.matcher(fullText)
            while (italicMatcher.find()) {
                val italicText = italicMatcher.group(1)
                elements.add(
                    MarkdownElement(
                        start = italicMatcher.start(1),
                        end = italicMatcher.end(1),
                        text = italicText,
                        type = MarkdownType.ITALIC
                    )
                )
            }

            // Process links
            val linkMatcher = LINK_PATTERN.matcher(fullText)
            while (linkMatcher.find()) {
                val linkText = linkMatcher.group(1)
                val url = linkMatcher.group(2)
                elements.add(
                    MarkdownElement(
                        start = linkMatcher.start(1),
                        end = linkMatcher.end(1),
                        text = linkText,
                        type = MarkdownType.LINK,
                        url = url
                    )
                )
            }

            // Apply all styles
            elements.forEach { element ->
                when (element.type) {
                    MarkdownType.HEADER -> {
                        val fontSize = when (element.level) {
                            1 -> MaterialTheme.typography.headlineLarge.fontSize
                            2 -> MaterialTheme.typography.headlineMedium.fontSize
                            else -> MaterialTheme.typography.headlineSmall.fontSize
                        }
                        addStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = fontSize,
                                color = primaryColor
                            ),
                            start = element.start,
                            end = element.end
                        )
                    }
                    MarkdownType.BOLD -> {
                        addStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Bold
                            ),
                            start = element.start,
                            end = element.end
                        )
                    }
                    MarkdownType.ITALIC -> {
                        addStyle(
                            style = SpanStyle(
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            ),
                            start = element.start,
                            end = element.end
                        )
                    }
                    MarkdownType.LINK -> {
                        addStyle(
                            style = SpanStyle(
                                color = primaryColor,
                                textDecoration = TextDecoration.Underline
                            ),
                            start = element.start,
                            end = element.end
                        )
                        addStringAnnotation(
                            tag = "URL",
                            annotation = element.url ?: "",
                            start = element.start,
                            end = element.end
                        )
                    }
                    MarkdownType.LIST_ITEM -> {
                        // List items don't need special styling, just the bullet point
                    }
                }
            }
        }
    }

    SelectionContainer(modifier = modifier) {
        ClickableText(
            text = annotatedString,
            style = textStyle,
            onClick = { offset ->
                // Handle link clicks
                annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        var urlToOpen = annotation.item
                        if (!urlToOpen.startsWith("http://") && !urlToOpen.startsWith("https://")) {
                            urlToOpen = "http://$urlToOpen"
                        }
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlToOpen))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("MarkDownText", "无法打开URL: $urlToOpen", e)
                        }
                    }
            }
        )
    }
}