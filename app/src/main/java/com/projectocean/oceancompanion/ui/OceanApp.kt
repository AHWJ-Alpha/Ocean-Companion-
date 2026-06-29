package com.projectocean.oceancompanion.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.BubbleChart
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ScreenshotMonitor
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.projectocean.oceancompanion.ai.ApiProfile
import com.projectocean.oceancompanion.ai.ModelCatalogClient
import com.projectocean.oceancompanion.agent.SharedScreenContext
import com.projectocean.oceancompanion.memory.ConversationHistory
import com.projectocean.oceancompanion.memory.OceanDatabase
import com.projectocean.oceancompanion.memory.PreferencesStore
import com.projectocean.oceancompanion.ui.theme.LocalOceanAccent
import com.projectocean.oceancompanion.ui.theme.parseOceanColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class NavItem(val label: String, val icon: ImageVector)

@Composable
fun OceanApp(
    onStartFloating: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onRequestScreenCapture: () -> Unit,
    onPickFile: () -> Unit,
    onPickIconImage: () -> Unit,
    onCheckUpdate: () -> Unit = {}
) {
    var selected by remember { mutableIntStateOf(0) }
    val items = listOf(
        NavItem("Ocean", Icons.Outlined.BubbleChart),
        NavItem("\u8bc6\u5c4f", Icons.Outlined.ScreenshotMonitor),
        NavItem("\u5386\u53f2", Icons.Outlined.History),
        NavItem("\u8bbe\u7f6e", Icons.Outlined.Settings)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        when (selected) {
            0 -> HomeScreen(Modifier.padding(padding), onStartFloating)
            1 -> CaptureScreen(Modifier.padding(padding), onOpenAccessibility, onRequestScreenCapture, onPickFile)
            2 -> HistoryScreen(Modifier.padding(padding))
            else -> SettingsScreen(Modifier.padding(padding), onPickIconImage, onCheckUpdate)
        }
    }
}

@Composable
private fun HomeScreen(modifier: Modifier, onStartFloating: () -> Unit) {
    val accent = LocalOceanAccent.current
    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = Color.Transparent) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(
                        Brush.linearGradient(listOf(accent.primary, accent.secondary, MaterialTheme.colorScheme.tertiary))
                    ).padding(22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) { Text("OC", color = Color.White, fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Ocean Companion", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("\u59cb\u7ec8\u5728\u7ebf\u7684 AI \u684c\u9762\u4f19\u4f34", color = Color.White.copy(alpha = 0.88f))
                            }
                        }
                        Text("双击悬浮球打开长对话；长按悬浮球可按住说话，松手后由 AI 以弹幕回复。", color = Color.White.copy(alpha = 0.92f))
                        Button(onClick = onStartFloating) {
                            Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("\u542f\u52a8\u60ac\u6d6e\u4f19\u4f34")
                        }
                    }
                }
            }
        }
        item {
            OceanCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("\u53ef\u8dd1\u901a\u7248\u672c", fontWeight = FontWeight.Bold)
                    MutedText("\u5f53\u524d\u4e3b\u8981\u805a\u7126\uff1a\u957f\u6309\u60ac\u6d6e\u7403\u6253\u5f00\u534a\u900f\u660e AI \u4fa7\u8fb9\u7a97\uff0c\u586b\u597d API \u540e\u53ef\u8fdb\u884c\u771f\u5b9e AI \u5bf9\u8bdd\uff0c\u5e76\u628a\u5bf9\u8bdd\u5185\u5bb9\u5199\u5165\u957f\u671f\u8bb0\u5fc6\u3002")
                }
            }
        }
    }
}

@Composable
private fun CaptureScreen(
    modifier: Modifier,
    onOpenAccessibility: () -> Unit,
    onRequestScreenCapture: () -> Unit,
    onPickFile: () -> Unit
) {
    val contextSnapshot by SharedScreenContext.snapshot.collectAsState()
    val previewText = contextSnapshot.visibleText.take(700).ifBlank { "\u6682\u672a\u8bfb\u5230\u5c4f\u5e55\u6587\u672c\u3002\u8bf7\u5148\u5f00\u542f\u65e0\u969c\u788d\u670d\u52a1\uff0c\u7136\u540e\u5207\u5230\u9700\u8981\u5206\u6790\u7684\u5e94\u7528\u3002" }
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("\u5c4f\u5e55\u7406\u89e3", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        MutedText("\u5148\u5f00\u542f\u65e0\u969c\u788d\u670d\u52a1\u4ee5\u8bfb\u53d6\u53ef\u89c1\u6587\u672c\uff1b\u4e5f\u53ef\u4e00\u952e\u622a\u56fe\u4ea4\u7ed9\u8bc6\u56fe\u6a21\u578b\u5206\u6790\uff0c\u6216\u9009\u62e9\u6587\u4ef6\u52a0\u5165 AI \u4e0a\u4e0b\u6587\u3002")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconTextButton("\u65e0\u969c\u788d", Icons.Outlined.AccessibilityNew, onOpenAccessibility, Modifier.weight(1f))
            IconTextButton("\u622a\u56fe\u5206\u6790", Icons.Outlined.ScreenshotMonitor, onRequestScreenCapture, Modifier.weight(1f))
            IconTextButton("\u6587\u4ef6", Icons.Outlined.FolderOpen, onPickFile, Modifier.weight(1f))
        }
        OceanCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("\u8bfb\u5c4f\u8bca\u65ad", fontWeight = FontWeight.Bold)
                MutedText("\u6765\u6e90\uff1a${contextSnapshot.source}    \u5305\u540d\uff1a${contextSnapshot.packageName.ifBlank { "-" }}", small = true)
                MutedText("\u5b57\u7b26\u6570\uff1a${contextSnapshot.visibleText.length}    \u66f4\u65b0\uff1a${formatAge(contextSnapshot.updatedAt)}", small = true)
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        text = previewText,
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                MutedText("\u6587\u4ef6\u6309\u94ae\u652f\u6301\u6587\u672c/Markdown/CSV/JSON/XML/\u4ee3\u7801\u6587\u4ef6\u548c\u56fe\u7247 OCR\uff1bPDF/PPT/Word \u9700\u540e\u7eed\u63a5\u5165\u4e13\u95e8\u89e3\u6790\u5668\u3002", small = true)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("PDF", "PPT", "\u6570\u5b66", "\u82f1\u8bed").forEach { label ->
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(modifier: Modifier) {
    val context = LocalContext.current
    val dao = remember { OceanDatabase.create(context).dao() }
    var query by remember { mutableStateOf("") }
    var groupByTopic by remember { mutableStateOf(true) }
    val conversations by if (query.isBlank()) {
        dao.allConversations().collectAsState(emptyList())
    } else {
        dao.searchConversations(query.trim()).collectAsState(emptyList())
    }
    val scope = rememberCoroutineScope()
    val grouped = remember(conversations, groupByTopic) {
        if (groupByTopic) {
            conversations.groupBy { it.topic.ifBlank { autoTopic(it.content) } }
        } else {
            conversations.groupBy { formatDay(it.createdAt) }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("历史", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
        item {
            OceanCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ConfigField("搜索时间、主题或内容", query, leadingIcon = Icons.Outlined.Search) { query = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FullWidthIconButton("按主题", Icons.Outlined.Chat, { groupByTopic = true }, Modifier.weight(1f), selected = groupByTopic)
                        FullWidthIconButton("按天数", Icons.Outlined.History, { groupByTopic = false }, Modifier.weight(1f), selected = !groupByTopic)
                    }
                    MutedText("长对话和导入的主动弹幕会保存在本地数据库中；修改主题或内容不会中断后续记忆继承。", small = true)
                }
            }
        }
        if (grouped.isEmpty()) {
            item {
                OceanCard {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("暂无历史", fontWeight = FontWeight.Bold)
                        MutedText("打开悬浮长对话并发送消息后，这里会按主题和时间自动归档。")
                    }
                }
            }
        }
        grouped.forEach { (title, items) ->
            item {
                HistoryGroupCard(
                    title = title,
                    items = items.sortedBy { it.createdAt },
                    canRename = groupByTopic,
                    onRename = { newTitle ->
                        val sessionIds = items.map { it.sessionId }.distinct()
                        scope.launch { sessionIds.forEach { dao.renameConversationTopic(it, newTitle.trim().ifBlank { title }) } }
                    },
                    onUpdateContent = { id, content ->
                        scope.launch { dao.updateConversationContent(id, content.trim()) }
                    },
                    onDeleteGroup = {
                        val ids = items.map { it.id }
                        scope.launch { dao.deleteConversationsByIds(ids) }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryGroupCard(
    title: String,
    items: List<ConversationHistory>,
    canRename: Boolean,
    onRename: (String) -> Unit,
    onUpdateContent: (Long, String) -> Unit,
    onDeleteGroup: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    var titleDraft by remember(title) { mutableStateOf(title) }
    OceanCard {
        Column(
            Modifier
                .combinedClickable(
                    onClick = { expanded = !expanded },
                    onLongClick = { confirmingDelete = true }
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold)
                    MutedText("${items.size} 条 · ${formatDay(items.lastOrNull()?.createdAt ?: 0L)} · 长按删除本组", small = true)
                }
                OutlinedButton(onClick = { expanded = !expanded }) {
                    Icon(if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, contentDescription = null)
                }
            }
            if (confirmingDelete) {
                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.82f)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("删除这组历史？", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        MutedText("会删除当前${if (canRename) "主题" else "日期"}下显示的 ${items.size} 条对话记录。", small = true)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { confirmingDelete = false }, modifier = Modifier.weight(1f)) {
                                Text("取消")
                            }
                            Button(
                                onClick = {
                                    confirmingDelete = false
                                    onDeleteGroup()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Delete, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("删除")
                            }
                        }
                    }
                }
            }
            if (expanded) {
                if (canRename) {
                    ConfigField("主题名", titleDraft) { titleDraft = it.take(40) }
                    IconTextButton("保存主题名", Icons.Outlined.Save, { onRename(titleDraft) }, Modifier.fillMaxWidth())
                }
                items.forEach { item ->
                    var contentDraft by remember(item.id, item.content) { mutableStateOf(item.content) }
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (item.role == "user") "用户" else if (item.role == "assistant") "AI" else "系统", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            ConfigField("内容", contentDraft) { contentDraft = it }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MutedText(formatTime(item.createdAt), small = true)
                                Spacer(Modifier.weight(1f))
                                OutlinedButton(onClick = { onUpdateContent(item.id, contentDraft) }) {
                                    Icon(Icons.Outlined.Save, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("保存")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(modifier: Modifier, onPickIconImage: () -> Unit, onCheckUpdate: () -> Unit) {
    val context = LocalContext.current
    val store = remember { PreferencesStore(context) }
    val scope = rememberCoroutineScope()
    val provider by store.provider.collectAsState("openai")
    val apiBaseUrl by store.apiBaseUrl.collectAsState("")
    val apiKey by store.apiKey.collectAsState("")
    val modelName by store.modelName.collectAsState("")
    val profiles by store.apiProfiles.collectAsState(emptyList())
    val userName by store.userName.collectAsState("\u4f60")
    val companionName by store.companionName.collectAsState("Ocean")
    val personaPrompt by store.customPersonaPrompt.collectAsState("")
    val iconText by store.iconText.collectAsState("Ocean")
    val speechInterval by store.speechIntervalMinutes.collectAsState(15)
    val triggerApps by store.triggerAppNames.collectAsState("")
    val panelRatio by store.panelRatio.collectAsState(0.5f)
    val proactive by store.proactiveReminders.collectAsState(true)
    val proactiveBannerMaxChars by store.proactiveBannerMaxChars.collectAsState(60)
    val companionReplyMaxChars by store.companionReplyMaxChars.collectAsState(0)
    val proactiveBannerOffset by store.proactiveBannerOffsetDp.collectAsState(12)
    val proactiveMuteMinutes by store.proactiveMuteMinutes.collectAsState(30)
    val companionOpenGesture by store.companionOpenGesture.collectAsState("double_tap")
    val themeMode by store.themeMode.collectAsState("system")
    val animePrimaryColor by store.animePrimaryColor.collectAsState("#39C5BB")
    val animeSecondaryColor by store.animeSecondaryColor.collectAsState("#00AEEF")
    val searchEnabled by store.searchEnabled.collectAsState(false)
    val searchProvider by store.searchProvider.collectAsState("tavily")
    val searchApiBaseUrl by store.searchApiBaseUrl.collectAsState("https://api.tavily.com")
    val searchApiKey by store.searchApiKey.collectAsState("")
    val searchEngineId by store.searchEngineId.collectAsState("")
    val ttsEnabled by store.ttsEnabled.collectAsState(false)
    val ttsProvider by store.ttsProvider.collectAsState("system")
    val ttsApiBaseUrl by store.ttsApiBaseUrl.collectAsState("")
    val ttsApiKey by store.ttsApiKey.collectAsState("")
    val ttsModel by store.ttsModel.collectAsState("tts-1")
    val ttsVoice by store.ttsVoice.collectAsState("")
    val sttProvider by store.sttProvider.collectAsState("system")
    val sttApiBaseUrl by store.sttApiBaseUrl.collectAsState("")
    val sttApiKey by store.sttApiKey.collectAsState("")
    val sttModel by store.sttModel.collectAsState("whisper-1")
    val sttLanguage by store.sttLanguage.collectAsState("zh-CN")
    var installedApps by remember { mutableStateOf(emptyList<InstalledApp>()) }

    var apiProfilesDraft by remember { mutableStateOf(emptyList<ApiProfile>()) }
    var userNameDraft by remember { mutableStateOf(userName) }
    var companionNameDraft by remember { mutableStateOf(companionName) }
    var personaPromptDraft by remember { mutableStateOf(personaPrompt) }
    var iconTextDraft by remember { mutableStateOf(iconText) }
    var speechIntervalDraft by remember { mutableStateOf(speechInterval) }
    var triggerAppsDraft by remember { mutableStateOf(triggerApps) }
    var panelRatioDraft by remember { mutableStateOf(panelRatio) }
    var proactiveDraft by remember { mutableStateOf(proactive) }
    var proactiveBannerMaxCharsDraft by remember { mutableStateOf(proactiveBannerMaxChars) }
    var companionReplyMaxCharsDraft by remember { mutableStateOf(companionReplyMaxChars) }
    var proactiveBannerOffsetDraft by remember { mutableStateOf(proactiveBannerOffset) }
    var proactiveMuteMinutesDraft by remember { mutableStateOf(proactiveMuteMinutes) }
    var companionOpenGestureDraft by remember { mutableStateOf(companionOpenGesture) }
    var themeModeDraft by remember { mutableStateOf(themeMode) }
    var animePrimaryColorDraft by remember { mutableStateOf(animePrimaryColor) }
    var animeSecondaryColorDraft by remember { mutableStateOf(animeSecondaryColor) }
    var searchEnabledDraft by remember { mutableStateOf(searchEnabled) }
    var searchProviderDraft by remember { mutableStateOf(searchProvider) }
    var searchApiBaseUrlDraft by remember { mutableStateOf(searchApiBaseUrl) }
    var searchApiKeyDraft by remember { mutableStateOf(searchApiKey) }
    var searchEngineIdDraft by remember { mutableStateOf(searchEngineId) }
    var ttsEnabledDraft by remember { mutableStateOf(ttsEnabled) }
    var ttsProviderDraft by remember { mutableStateOf(ttsProvider) }
    var ttsApiBaseUrlDraft by remember { mutableStateOf(ttsApiBaseUrl) }
    var ttsApiKeyDraft by remember { mutableStateOf(ttsApiKey) }
    var ttsModelDraft by remember { mutableStateOf(ttsModel) }
    var ttsVoiceDraft by remember { mutableStateOf(ttsVoice) }
    var sttProviderDraft by remember { mutableStateOf(sttProvider) }
    var sttApiBaseUrlDraft by remember { mutableStateOf(sttApiBaseUrl) }
    var sttApiKeyDraft by remember { mutableStateOf(sttApiKey) }
    var sttModelDraft by remember { mutableStateOf(sttModel) }
    var sttLanguageDraft by remember { mutableStateOf(sttLanguage) }
    var savedNotice by remember { mutableStateOf("") }
    var draftsLoaded by remember { mutableStateOf(false) }
    var apiExpanded by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }
    var speechExpanded by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }
    var personaExpanded by remember { mutableStateOf(false) }
    var behaviorExpanded by remember { mutableStateOf(false) }
    var explainExpanded by remember { mutableStateOf(false) }
    var selectionHint by remember { mutableStateOf<SelectionHint?>(null) }

    LaunchedEffect(Unit) {
        installedApps = withContext(Dispatchers.Default) { loadInstalledApps(context).take(18) }
    }

    LaunchedEffect(Unit) {
        if (draftsLoaded) return@LaunchedEffect
        val savedProvider = store.provider.first()
        val savedApiBaseUrl = store.apiBaseUrl.first()
        val savedApiKey = store.apiKey.first()
        val savedModelName = store.modelName.first()
        apiProfilesDraft = store.apiProfiles.first().ifEmpty {
            listOf(ApiProfile(label = savedProvider.ifBlank { "OpenAI" }, provider = savedProvider, baseUrl = savedApiBaseUrl, apiKey = savedApiKey, model = savedModelName))
        }
        userNameDraft = store.userName.first()
        companionNameDraft = store.companionName.first()
        personaPromptDraft = store.customPersonaPrompt.first()
        iconTextDraft = store.iconText.first()
        speechIntervalDraft = store.speechIntervalMinutes.first()
        triggerAppsDraft = store.triggerAppNames.first()
        panelRatioDraft = store.panelRatio.first()
        proactiveDraft = store.proactiveReminders.first()
        proactiveBannerMaxCharsDraft = store.proactiveBannerMaxChars.first()
        companionReplyMaxCharsDraft = store.companionReplyMaxChars.first()
        proactiveBannerOffsetDraft = store.proactiveBannerOffsetDp.first()
        proactiveMuteMinutesDraft = store.proactiveMuteMinutes.first()
        companionOpenGestureDraft = store.companionOpenGesture.first()
        themeModeDraft = store.themeMode.first()
        animePrimaryColorDraft = store.animePrimaryColor.first()
        animeSecondaryColorDraft = store.animeSecondaryColor.first()
        searchEnabledDraft = store.searchEnabled.first()
        searchProviderDraft = store.searchProvider.first()
        searchApiBaseUrlDraft = store.searchApiBaseUrl.first()
        searchApiKeyDraft = store.searchApiKey.first()
        searchEngineIdDraft = store.searchEngineId.first()
        ttsEnabledDraft = store.ttsEnabled.first()
        ttsProviderDraft = store.ttsProvider.first()
        ttsApiBaseUrlDraft = store.ttsApiBaseUrl.first()
        ttsApiKeyDraft = store.ttsApiKey.first()
        ttsModelDraft = store.ttsModel.first()
        ttsVoiceDraft = store.ttsVoice.first()
        sttProviderDraft = store.sttProvider.first()
        sttApiBaseUrlDraft = store.sttApiBaseUrl.first()
        sttApiKeyDraft = store.sttApiKey.first()
        sttModelDraft = store.sttModel.first()
        sttLanguageDraft = store.sttLanguage.first()
        draftsLoaded = true
    }

    LaunchedEffect(userNameDraft, companionNameDraft, personaPromptDraft, iconTextDraft, speechIntervalDraft, triggerAppsDraft, panelRatioDraft, proactiveDraft, proactiveBannerMaxCharsDraft, companionReplyMaxCharsDraft, proactiveBannerOffsetDraft, proactiveMuteMinutesDraft, companionOpenGestureDraft, ttsEnabledDraft) {
        if (!draftsLoaded) return@LaunchedEffect
        store.setUserName(userNameDraft.trim().ifBlank { "你" })
        store.setCompanionName(companionNameDraft.trim().ifBlank { "Ocean" })
        store.setCustomPersonaPrompt(personaPromptDraft)
        store.setIconText(iconTextDraft.trim().ifBlank { "Ocean" }.take(8))
        store.setSpeechIntervalMinutes(speechIntervalDraft.coerceIn(1, 120))
        store.setTriggerAppNames(triggerAppsDraft.trim())
        store.setPanelRatio(panelRatioDraft.coerceIn(0.35f, 0.8f))
        store.setProactiveReminders(proactiveDraft)
        store.setProactiveBannerMaxChars(if (proactiveBannerMaxCharsDraft <= 0) 0 else proactiveBannerMaxCharsDraft.coerceIn(20, 200))
        store.setCompanionReplyMaxChars(if (companionReplyMaxCharsDraft <= 0) 0 else companionReplyMaxCharsDraft.coerceIn(120, 2000))
        store.setProactiveBannerOffsetDp(proactiveBannerOffsetDraft.coerceIn(0, 160))
        store.setProactiveMuteMinutes(proactiveMuteMinutesDraft.coerceIn(5, 240))
        store.setCompanionOpenGesture(companionOpenGestureDraft.ifBlank { "double_tap" })
        store.setTtsEnabled(ttsEnabledDraft)
    }

    LazyColumn(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("\u81ea\u5b9a\u4e49\u4e0e AI API", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item {
            OceanCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("\u4f7f\u7528\u8bf4\u660e", fontWeight = FontWeight.Bold)
                    MutedText("\u591a API \u6309\u4e0a\u4e0b\u987a\u5e8f\u4f9d\u6b21\u5c1d\u8bd5\uff1b\u524d\u4e00\u4e2a\u65e0\u6cd5\u8fde\u901a\u6216\u8fd4\u56de\u7a7a\u6d88\u606f\u65f6\uff0cOcean \u4f1a\u81ea\u52a8\u5207\u6362\u5230\u540e\u4e00\u4e2a\u3002")
                    MutedText("\u4e00\u952e\u622a\u56fe\u5206\u6790\u4f1a\u4f18\u5148\u4f7f\u7528\u5df2\u52fe\u9009\u201c\u8bc6\u56fe\u201d\u7684\u914d\u7f6e\u3002", small = true)
                }
            }
        }
        item {
            ExpandableSection("主题与外观", themeExpanded, { themeExpanded = !themeExpanded }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    themeOptions.forEach { option ->
                        FullWidthIconButton(
                            label = option.label,
                            icon = Icons.Outlined.Palette,
                            selected = option.value == themeModeDraft,
                            onClick = {
                                themeModeDraft = option.value
                                store.setThemeModeAsync(option.value)
                                selectionHint = SelectionHint(option.label, option.description)
                            }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ColorPreview(animePrimaryColorDraft, Modifier.weight(1f))
                        ColorPreview(animeSecondaryColorDraft, Modifier.weight(1f))
                    }
                    ConfigField("二次元主色 #RRGGBB", animePrimaryColorDraft) {
                        val normalized = normalizeColorInput(it)
                        animePrimaryColorDraft = normalized
                        store.setAnimePrimaryColorAsync(normalized.ifBlank { "#39C5BB" })
                    }
                    ConfigField("二次元辅色 #RRGGBB", animeSecondaryColorDraft) {
                        val normalized = normalizeColorInput(it)
                        animeSecondaryColorDraft = normalized
                        store.setAnimeSecondaryColorAsync(normalized.ifBlank { "#00AEEF" })
                    }
                    MutedText("默认主色为 #39C5BB、辅色为 #00AEEF。自定义两种颜色后，系统会按默认蓝青色对应位置映射到主页、主动弹幕和长对话窗口。", small = true)
                }
            }
        }
        item {
            ExpandableSection("AI API \u4e0e\u8bc6\u56fe\u6a21\u578b", apiExpanded, { apiExpanded = !apiExpanded }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    selectionHint?.let { SelectionHintCard(it) }
                    providerPresets.forEach { preset ->
                        FullWidthIconButton(
                            label = preset.label,
                            icon = Icons.Outlined.Add,
                            onClick = {
                                apiProfilesDraft = apiProfilesDraft + preset.toApiProfile()
                                apiExpanded = true
                                selectionHint = SelectionHint(
                                    preset.label,
                                    "\u5df2\u6dfb\u52a0 ${preset.label}\uff1a\u9ed8\u8ba4\u5730\u5740\u4e3a ${preset.baseUrl}\uff0c\u6a21\u578b\u4e3a ${preset.model}\u3002API Key \u4ecd\u9700\u4f60\u81ea\u5df1\u586b\u5199\u3002"
                                )
                            }
                        )
                    }
                    if (apiProfilesDraft.isEmpty()) MutedText("\u6682\u65e0 API \u914d\u7f6e\uff0c\u8bf7\u5148\u70b9\u4e0a\u65b9\u9884\u8bbe\u6dfb\u52a0\u4e00\u4e2a\u3002")
                    apiProfilesDraft.forEachIndexed { index, profile ->
                        ApiProfileEditor(
                            index = index,
                            profile = profile,
                            total = apiProfilesDraft.size,
                            onChange = { changed ->
                                apiProfilesDraft = apiProfilesDraft.toMutableList().also { it[index] = changed }
                                if (changed.supportsVision != profile.supportsVision) {
                                    selectionHint = SelectionHint(
                                        "\u8bc6\u56fe\u6a21\u578b",
                                        if (changed.supportsVision) "${changed.label}\u5df2\u6807\u8bb0\u4e3a\u622a\u56fe\u5206\u6790\u4f18\u5148\u4f7f\u7528\u7684\u6a21\u578b\u3002\u8bf7\u786e\u8ba4\u8be5\u6a21\u578b\u652f\u6301\u56fe\u50cf\u8f93\u5165\u3002" else "${changed.label}\u5df2\u4e0d\u518d\u4f5c\u4e3a\u8bc6\u56fe\u4f18\u5148\u6a21\u578b\uff0c\u622a\u56fe\u5206\u6790\u4f1a\u5c1d\u8bd5\u5176\u4ed6\u53ef\u7528\u914d\u7f6e\u3002"
                                    )
                                }
                            },
                            onMoveUp = { if (index > 0) apiProfilesDraft = apiProfilesDraft.toMutableList().also { java.util.Collections.swap(it, index, index - 1) } },
                            onMoveDown = { if (index < apiProfilesDraft.lastIndex) apiProfilesDraft = apiProfilesDraft.toMutableList().also { java.util.Collections.swap(it, index, index + 1) } },
                            onDelete = { apiProfilesDraft = apiProfilesDraft.filterIndexed { i, _ -> i != index } }
                        )
                    }
                }
            }
        }
        item {
            ExpandableSection("搜索 API", searchExpanded, { searchExpanded = !searchExpanded }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("允许 AI 联网搜索", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Switch(checked = searchEnabledDraft, onCheckedChange = { searchEnabledDraft = it })
                    }
                    MutedText("当用户说“搜索、查一下、最新、资料、论文、来源”等请求时，Ocean 会先调用搜索 API，再把结果交给对话模型整理。", small = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FullWidthIconButton("Tavily", Icons.Outlined.Search, {
                            searchProviderDraft = "tavily"
                            searchApiBaseUrlDraft = "https://api.tavily.com"
                        }, Modifier.weight(1f), selected = searchProviderDraft == "tavily")
                        FullWidthIconButton("SerpAPI", Icons.Outlined.Search, {
                            searchProviderDraft = "serpapi"
                            searchApiBaseUrlDraft = "https://serpapi.com/search.json"
                        }, Modifier.weight(1f), selected = searchProviderDraft == "serpapi")
                    }
                    FullWidthIconButton("自定义 JSON 搜索端点", Icons.Outlined.Settings, { searchProviderDraft = "custom" }, selected = searchProviderDraft == "custom")
                    ConfigField("搜索 Base URL", searchApiBaseUrlDraft, leadingIcon = Icons.Outlined.Settings) { searchApiBaseUrlDraft = it }
                    ConfigField("搜索 API Key", searchApiKeyDraft, leadingIcon = Icons.Outlined.Search, password = true) { searchApiKeyDraft = it }
                    ConfigField("搜索引擎 ID / 可选", searchEngineIdDraft, leadingIcon = Icons.Outlined.Settings) { searchEngineIdDraft = it }
                    IconTextButton("保存搜索 API", Icons.Outlined.Save, {
                        scope.launch {
                            store.saveSearchSettings(
                                enabled = searchEnabledDraft,
                                provider = searchProviderDraft,
                                baseUrl = searchApiBaseUrlDraft.trim(),
                                apiKey = searchApiKeyDraft.trim(),
                                engineId = searchEngineIdDraft.trim()
                            )
                            searchExpanded = false
                            savedNotice = "搜索 API 已保存。"
                        }
                    }, Modifier.fillMaxWidth())
                }
            }
        }
        item {
            ExpandableSection("语音 TTS / STT", speechExpanded, { speechExpanded = !speechExpanded }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("弹幕回复同时朗读", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Switch(checked = ttsEnabledDraft, onCheckedChange = { ttsEnabledDraft = it })
                    }
                    MutedText("长按悬浮球开始语音输入，松手后识别并交给 AI；TTS 开启后，主动弹幕和语音回复会同步朗读。系统语音不需要 Key，云端语音使用 OpenAI 兼容音频接口。", small = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FullWidthIconButton("系统语音", Icons.Outlined.PlayArrow, {
                            sttProviderDraft = "system"
                            ttsProviderDraft = "system"
                            sttApiBaseUrlDraft = ""
                            ttsApiBaseUrlDraft = ""
                        }, Modifier.weight(1f), selected = sttProviderDraft == "system" && ttsProviderDraft == "system")
                        FullWidthIconButton("OpenAI 语音", Icons.Outlined.GraphicEq, {
                            sttProviderDraft = "openai"
                            ttsProviderDraft = "openai"
                            sttApiBaseUrlDraft = "https://api.openai.com/v1"
                            ttsApiBaseUrlDraft = "https://api.openai.com/v1"
                            sttModelDraft = "whisper-1"
                            ttsModelDraft = "tts-1"
                            ttsVoiceDraft = "alloy"
                        }, Modifier.weight(1f), selected = sttProviderDraft == "openai" && ttsProviderDraft == "openai")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FullWidthIconButton("阿里百炼兼容", Icons.Outlined.GraphicEq, {
                            sttProviderDraft = "aliyun"
                            ttsProviderDraft = "aliyun"
                            sttApiBaseUrlDraft = "https://dashscope.aliyuncs.com/compatible-mode/v1"
                            ttsApiBaseUrlDraft = "https://dashscope.aliyuncs.com/compatible-mode/v1"
                            sttModelDraft = "paraformer-realtime-v2"
                            ttsModelDraft = "cosyvoice-v1"
                            ttsVoiceDraft = "longxiaochun"
                        }, Modifier.weight(1f), selected = sttProviderDraft == "aliyun" || ttsProviderDraft == "aliyun")
                        FullWidthIconButton("自定义兼容", Icons.Outlined.Settings, {
                            sttProviderDraft = "custom"
                            ttsProviderDraft = "custom"
                        }, Modifier.weight(1f), selected = sttProviderDraft == "custom" || ttsProviderDraft == "custom")
                    }
                    ConfigField("STT 语言", sttLanguageDraft, leadingIcon = Icons.Outlined.Chat) { sttLanguageDraft = it.ifBlank { "zh-CN" } }
                    ConfigField("STT 模型", sttModelDraft, leadingIcon = Icons.Outlined.GraphicEq) { sttModelDraft = it }
                    ConfigField("STT Base URL", sttApiBaseUrlDraft, leadingIcon = Icons.Outlined.Settings) { sttApiBaseUrlDraft = it }
                    ConfigField("STT API Key", sttApiKeyDraft, leadingIcon = Icons.Outlined.Settings, password = true) { sttApiKeyDraft = it }
                    ConfigField("TTS 模型", ttsModelDraft, leadingIcon = Icons.Outlined.GraphicEq) { ttsModelDraft = it }
                    ConfigField("TTS Base URL", ttsApiBaseUrlDraft, leadingIcon = Icons.Outlined.Settings) { ttsApiBaseUrlDraft = it }
                    ConfigField("TTS API Key", ttsApiKeyDraft, leadingIcon = Icons.Outlined.Settings, password = true) { ttsApiKeyDraft = it }
                    ConfigField("TTS 音色 / 系统 Voice 名称", ttsVoiceDraft, leadingIcon = Icons.Outlined.PlayArrow) { ttsVoiceDraft = it.ifBlank { "alloy" } }
                    IconTextButton("保存语音配置", Icons.Outlined.Save, {
                        scope.launch {
                            store.saveSpeechSettings(
                                ttsEnabled = ttsEnabledDraft,
                                ttsProvider = ttsProviderDraft.trim().ifBlank { "system" },
                                ttsBaseUrl = ttsApiBaseUrlDraft.trim(),
                                ttsApiKey = ttsApiKeyDraft.trim(),
                                ttsModel = ttsModelDraft.trim().ifBlank { "tts-1" },
                                ttsVoice = ttsVoiceDraft.trim(),
                                sttProvider = sttProviderDraft.trim().ifBlank { "system" },
                                sttBaseUrl = sttApiBaseUrlDraft.trim(),
                                sttApiKey = sttApiKeyDraft.trim(),
                                sttModel = sttModelDraft.trim().ifBlank { "whisper-1" },
                                sttLanguage = sttLanguageDraft.trim().ifBlank { "zh-CN" }
                            )
                            speechExpanded = false
                            savedNotice = "语音配置已保存。长按悬浮球即可按住说话，松手发送。"
                        }
                    }, Modifier.fillMaxWidth())
                }
            }
        }
        item {
            ExpandableSection("\u4eba\u683c\u4e0e\u5916\u89c2", personaExpanded, { personaExpanded = !personaExpanded }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ConfigField("\u7528\u6237\u6635\u79f0", userNameDraft) { userNameDraft = it.take(20) }
                    ConfigField("AI \u4f19\u4f34\u540d\u79f0", companionNameDraft) { companionNameDraft = it.take(20) }
                    ConfigField("\u60ac\u6d6e\u7403\u56fe\u6807\u6587\u5b57", iconTextDraft) { iconTextDraft = it.take(8) }
                    IconTextButton("\u4e0a\u4f20\u5e76\u88c1\u5207\u56fe\u6807", Icons.Outlined.Image, onPickIconImage, Modifier.fillMaxWidth())
                    personaPresets.forEach { preset ->
                        FullWidthIconButton(
                            label = preset.label,
                            icon = Icons.Outlined.Chat,
                            onClick = {
                                personaPromptDraft = preset.prompt
                                selectionHint = SelectionHint(preset.label, "\u5df2\u5957\u7528 ${preset.label}\uff1a\u4e0b\u6b21\u957f\u5bf9\u8bdd\u548c\u4e3b\u52a8\u53d1\u8a00\u4f1a\u6309\u8fd9\u4e2a\u98ce\u683c\u751f\u6210\u3002")
                            }
                        )
                    }
                    ConfigField("\u4eba\u683c\u63d0\u793a\u8bcd", personaPromptDraft) { personaPromptDraft = it }
                }
            }
        }
        item {
            ExpandableSection("\u4e3b\u52a8\u53d1\u8a00\u4e0e\u4f34\u968f\u7a97", behaviorExpanded, { behaviorExpanded = !behaviorExpanded }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ConfigField("\u8f6f\u4ef6\u540d\u81ea\u52a8\u89e6\u53d1\uff08\u9017\u53f7\u5206\u9694\uff09", triggerAppsDraft) { triggerAppsDraft = it }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("\u4e3b\u52a8\u81ea\u52a8\u53d1\u8a00", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Switch(checked = proactiveDraft, onCheckedChange = { proactiveDraft = it })
                    }
                    MutedText("\u8fdb\u5165\u547d\u4e2d\u5e94\u7528\u4f1a\u7acb\u5373\u89e6\u53d1\uff0c\u4e0d\u53d7\u4e0b\u65b9\u65e5\u5e38\u95f4\u9694\u9650\u5236\u3002", small = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("自适应弹幕宽度", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = proactiveBannerMaxCharsDraft > 0,
                            onCheckedChange = { checked -> proactiveBannerMaxCharsDraft = if (checked) 60 else 0 }
                        )
                    }
                    if (proactiveBannerMaxCharsDraft > 0) {
                        Text("弹幕最大展示范围：${proactiveBannerMaxCharsDraft.coerceIn(20, 200)}")
                        Slider(
                            value = proactiveBannerMaxCharsDraft.coerceIn(20, 200).toFloat(),
                            onValueChange = { proactiveBannerMaxCharsDraft = it.toInt().coerceIn(20, 200) },
                            valueRange = 20f..200f
                        )
                        MutedText("短句会自动收窄；超出最大展示范围时，弹幕会在该宽度内多行显示，不再硬截断。", small = true)
                    } else {
                        MutedText("当前为不限制：主动弹幕会按内容尽量展开，仍会避免遮挡过多屏幕。", small = true)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("限制长对话回复字数", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = companionReplyMaxCharsDraft > 0,
                            onCheckedChange = { checked -> companionReplyMaxCharsDraft = if (checked) 800 else 0 }
                        )
                    }
                    if (companionReplyMaxCharsDraft > 0) {
                        Text("每次 AI 回复最大长度：${companionReplyMaxCharsDraft.coerceIn(120, 2000)} 字")
                        Slider(
                            value = companionReplyMaxCharsDraft.coerceIn(120, 2000).toFloat(),
                            onValueChange = { companionReplyMaxCharsDraft = it.toInt().coerceIn(120, 2000) },
                            valueRange = 120f..2000f
                        )
                    } else {
                        MutedText("0 表示不限制长对话回复长度；提示词仍会要求 AI 既解释判断，也给出明确行动。", small = true)
                    }
                    Text("\u5f39\u5e55\u8ddd\u79bb\u60ac\u6d6e\u7403\uff1a${proactiveBannerOffsetDraft.coerceIn(0, 160)} dp")
                    Slider(
                        value = proactiveBannerOffsetDraft.coerceIn(0, 160).toFloat(),
                        onValueChange = { proactiveBannerOffsetDraft = it.toInt().coerceIn(0, 160) },
                        valueRange = 0f..160f
                    )
                    MutedText("\u8ddd\u79bb\u8d8a\u5927\uff0c\u5f39\u5e55\u8d8a\u4e0d\u5bb9\u6613\u6321\u4f4f\u60ac\u6d6e\u7403\u9644\u8fd1\u7684\u64cd\u4f5c\u3002", small = true)
                    Text("三击弹幕后暂停主动弹幕：${proactiveMuteMinutesDraft.coerceIn(5, 240)} 分钟")
                    Slider(
                        value = proactiveMuteMinutesDraft.coerceIn(5, 240).toFloat(),
                        onValueChange = { proactiveMuteMinutesDraft = it.toInt().coerceIn(5, 240) },
                        valueRange = 5f..240f
                    )
                    Text("\u957f\u5bf9\u8bdd\u547c\u51fa\u65b9\u5f0f", fontWeight = FontWeight.SemiBold)
                    companionGestureOptions.forEach { option ->
                        FullWidthIconButton(
                            label = option.label,
                            icon = if (option.value == companionOpenGestureDraft) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.Chat,
                            selected = option.value == companionOpenGestureDraft,
                            onClick = {
                                companionOpenGestureDraft = option.value
                                selectionHint = SelectionHint(option.label, option.description)
                            }
                        )
                    }
                    Text("\u53d1\u8a00\u95f4\u9694\uff1a${speechIntervalDraft} \u5206\u949f")
                    Slider(value = speechIntervalDraft.toFloat(), onValueChange = { speechIntervalDraft = it.toInt().coerceIn(1, 120) }, valueRange = 1f..120f)
                    Text("\u4f34\u968f\u9762\u677f\u5360\u5c4f\u6bd4\u4f8b\uff1a${(panelRatioDraft * 100).toInt()}%")
                    Slider(value = panelRatioDraft, onValueChange = { panelRatioDraft = it.coerceIn(0.35f, 0.8f) }, valueRange = 0.35f..0.8f)
                    MutedText("\u5e94\u7528\u5feb\u901f\u9009\u62e9", small = true)
                    installedApps.forEach { app ->
                        FullWidthIconButton(
                            label = app.label,
                            icon = Icons.Outlined.Add,
                            onClick = {
                                triggerAppsDraft = listOf(triggerAppsDraft, app.label).filter { it.isNotBlank() }.joinToString(",")
                                selectionHint = SelectionHint(app.label, "\u5df2\u5c06 ${app.label} \u52a0\u5165\u81ea\u52a8\u89e6\u53d1\u5173\u952e\u8bcd\uff1a\u8fdb\u5165\u547d\u4e2d\u5e94\u7528\u65f6\u4f1a\u7acb\u5373\u751f\u6210\u4e00\u6761\u72ec\u7acb\u5f39\u5e55\u3002")
                            }
                        )
                    }
                }
            }
        }
        item {
            OceanCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        scope.launch {
                            val cleanedProfiles = apiProfilesDraft.mapIndexed { index, profile -> profile.copy(label = profile.label.trim().ifBlank { "API ${index + 1}" }, provider = profile.provider.trim().ifBlank { "custom" }, baseUrl = profile.baseUrl.trim(), apiKey = profile.apiKey.trim(), model = profile.model.trim()) }
                            val primary = cleanedProfiles.firstOrNull() ?: ApiProfile()
                            store.setProvider(primary.provider)
                            store.setApiBaseUrl(primary.baseUrl)
                            store.setApiKey(primary.apiKey)
                            store.setModelName(primary.model)
                            store.setApiProfiles(cleanedProfiles)
                            apiProfilesDraft = cleanedProfiles
                            apiExpanded = false
                            savedNotice = "API 与模型配置已保存。其他模块已改为修改后实时生效。"
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("保存 API 与模型")
                    }
                    if (savedNotice.isNotBlank()) Text(savedNotice, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconTextButton("\u7cfb\u7edf\u8bbe\u7f6e", Icons.Outlined.Settings, { openPackage(context, "com.android.settings") }, Modifier.weight(1f))
                IconTextButton("\u6253\u5f00\u6587\u4ef6", Icons.Outlined.FolderOpen, { openFileManager(context) }, Modifier.weight(1f))
            }
        }
        item {
            IconTextButton("检查更新", Icons.Outlined.Search, onCheckUpdate, Modifier.fillMaxWidth())
        }
        item {
            ExpandableSection("\u76f8\u5173\u8bf4\u660e", explainExpanded, { explainExpanded = !explainExpanded }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "\u957f\u65f6\u4f34\u968f\uff1a\u7ad6\u5c4f\u4e3a\u4e0b\u534a\u5c4f\uff0c\u6a2a\u5c4f\u4e3a\u53f3\u4fa7\u7a97\uff0c\u53ef\u5728\u4e0a\u65b9\u8c03\u8282\u5360\u5c4f\u6bd4\u4f8b\u3002",
                        "AI \u5bf9\u8bdd\uff1a\u4f7f\u7528 OpenAI \u517c\u5bb9 /chat/completions \u63a5\u53e3\uff0c\u652f\u6301\u591a API \u987a\u5e8f fallback\u3002",
                        "\u957f\u671f\u8bb0\u5fc6\uff1a\u4f34\u968f\u7a97\u5185\u7684\u7528\u6237\u53d1\u8a00\u548c AI \u56de\u590d\u4f1a\u5199\u5165\u672c\u5730\u6570\u636e\u5e93\u3002",
                        "\u8bc6\u5c4f\uff1a\u65e0\u969c\u788d\u670d\u52a1\u63d0\u4f9b\u5f53\u524d\u9875\u9762\u6587\u672c\uff1b\u4e00\u952e\u622a\u56fe\u4f1a\u5c1d\u8bd5\u8c03\u7528\u8bc6\u56fe\u6a21\u578b\u5206\u6790\u753b\u9762\u3002",
                        "\u81ea\u52a8\u53d1\u8a00\uff1a\u8fdb\u5165\u547d\u4e2d\u5e94\u7528\u4f1a\u7acb\u5373\u89e6\u53d1\uff0c\u4e0d\u5c55\u5f00\u957f\u5bf9\u8bdd\u9762\u677f\uff1b\u65e5\u5e38\u5de1\u68c0\u624d\u4f7f\u7528\u53d1\u8a00\u95f4\u9694\u3002"
                    ).forEach { line -> MutedText(line) }
                }
            }
        }
    }
}

private data class InstalledApp(val label: String, val packageName: String)

private data class ProviderPreset(val label: String, val provider: String, val baseUrl: String, val model: String)

private data class PersonaPreset(val label: String, val prompt: String)

private data class SelectionHint(val title: String, val body: String)

private data class CompanionGestureOption(val label: String, val value: String, val description: String)

private data class ThemeOption(val label: String, val value: String, val description: String)

@Composable
private fun OceanCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
    ) { content() }
}

@Composable
private fun MutedText(text: String, small: Boolean = false) {
    Text(
        text = text,
        style = if (small) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun IconTextButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(onClick = onClick, modifier = modifier) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(label, maxLines = 1)
    }
}

@Composable
private fun FullWidthIconButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    val scroll = rememberScrollState()
    OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f).horizontalScroll(scroll),
            maxLines = 1,
            softWrap = false,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    OceanCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = onToggle) {
                    Icon(if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(if (expanded) "\u6536\u8d77" else "\u5c55\u5f00")
                }
            }
            if (expanded) content()
        }
    }
}

@Composable
private fun ApiProfileEditor(
    index: Int,
    profile: ApiProfile,
    total: Int,
    onChange: (ApiProfile) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var local by remember(profile.id) { mutableStateOf(profile) }
    var showKey by remember(profile.id) { mutableStateOf(false) }
    var models by remember(profile.id) { mutableStateOf(ModelCatalogClient.fallbackModels(profile.provider)) }
    var loadingModels by remember(profile.id) { mutableStateOf(false) }
    var modelNotice by remember(profile.id) { mutableStateOf("") }
    LaunchedEffect(profile) {
        if (profile != local) local = profile
    }
    fun commit(next: ApiProfile) {
        local = next
        onChange(next)
    }
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("#${index + 1}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(local.label.ifBlank { local.provider }, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                OutlinedButton(onClick = onMoveUp, enabled = index > 0) { Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = null) }
                OutlinedButton(onClick = onMoveDown, enabled = index < total - 1) { Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null) }
                OutlinedButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = null) }
            }
            ConfigField("\u914d\u7f6e\u540d", local.label) { commit(local.copy(label = it)) }
            ConfigField("\u63d0\u4f9b\u5546", local.provider) { commit(local.copy(provider = it)) }
            ConfigField("API Base URL", local.baseUrl) { commit(local.copy(baseUrl = it)) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConfigField("API Key", local.apiKey, password = !showKey, modifier = Modifier.weight(1f)) { value ->
                    local = local.copy(apiKey = value)
                }
                OutlinedButton(onClick = { showKey = !showKey }) { Text(if (showKey) "隐藏" else "显示") }
            }
            OutlinedButton(onClick = { commit(local.copy(apiKey = local.apiKey.trim())) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("确认此 API Key")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        loadingModels = true
                        modelNotice = "正在获取模型列表..."
                        scope.launch {
                            val result = ModelCatalogClient().fetchModels(local.copy(apiKey = local.apiKey.trim()))
                            result.onSuccess { fetched ->
                                models = fetched.ifEmpty { ModelCatalogClient.fallbackModels(local.provider) }
                                modelNotice = if (fetched.isEmpty()) "接口未返回模型，已显示常见模型。" else "已获取 ${fetched.size} 个模型。"
                            }.onFailure { error ->
                                models = ModelCatalogClient.fallbackModels(local.provider)
                                modelNotice = "获取失败，已显示常见模型：${error.message.orEmpty().take(80)}"
                            }
                            loadingModels = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !loadingModels && local.baseUrl.isNotBlank() && local.apiKey.isNotBlank()
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (loadingModels) "获取中" else "获取模型")
                }
                OutlinedButton(onClick = { models = ModelCatalogClient.fallbackModels(local.provider) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("常见模型")
                }
            }
            if (modelNotice.isNotBlank()) MutedText(modelNotice, small = true)
            ConfigField("自定义模型名", local.model) { value ->
                val reasoning = local.supportsReasoning || ModelCatalogClient.looksLikeReasoningModel(value)
                commit(local.copy(model = value, supportsReasoning = reasoning))
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                models.take(24).forEach { model ->
                    OutlinedButton(onClick = {
                        commit(local.copy(model = model, supportsReasoning = local.supportsReasoning || ModelCatalogClient.looksLikeReasoningModel(model)))
                    }) { Text(model, maxLines = 1, softWrap = false) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("\u542f\u7528", modifier = Modifier.weight(1f))
                Switch(checked = local.enabled, onCheckedChange = { commit(local.copy(enabled = it)) })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("\u7528\u4e8e\u622a\u56fe\u8bc6\u56fe", modifier = Modifier.weight(1f))
                Switch(checked = local.supportsVision, onCheckedChange = { commit(local.copy(supportsVision = it)) })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("深度思考 / 推理模型", modifier = Modifier.weight(1f))
                Switch(checked = local.supportsReasoning, onCheckedChange = { commit(local.copy(supportsReasoning = it)) })
            }
            if (local.supportsReasoning) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("low", "medium", "high").forEach { effort ->
                        FullWidthIconButton(
                            label = when (effort) { "low" -> "低"; "high" -> "高"; else -> "中" },
                            icon = Icons.Outlined.Settings,
                            selected = local.reasoningEffort == effort,
                            onClick = { commit(local.copy(reasoningEffort = effort)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionHintCard(hint: SelectionHint) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(hint.title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(hint.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun ColorPreview(value: String, modifier: Modifier = Modifier) {
    val color = parseOceanColor(value, MaterialTheme.colorScheme.primary)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.22f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.72f))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(color))
            Text(value.ifBlank { "#39C5BB" }, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

private fun ProviderPreset.toApiProfile(): ApiProfile = ApiProfile(
    label = label,
    provider = provider,
    baseUrl = baseUrl,
    model = model,
    supportsVision = provider in setOf("openai", "bailian", "zhipu", "openrouter")
)

private val providerPresets = listOf(
    ProviderPreset("OpenAI", "openai", "https://api.openai.com/v1", "gpt-4o-mini"),
    ProviderPreset("DeepSeek", "deepseek", "https://api.deepseek.com/v1", "deepseek-chat"),
    ProviderPreset("\u963f\u91cc\u767e\u70bc", "bailian", "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-plus"),
    ProviderPreset("\u667a\u8c31 GLM", "zhipu", "https://open.bigmodel.cn/api/paas/v4", "glm-4-flash"),
    ProviderPreset("Moonshot", "moonshot", "https://api.moonshot.cn/v1", "moonshot-v1-8k"),
    ProviderPreset("OpenRouter", "openrouter", "https://openrouter.ai/api/v1", "openai/gpt-4o-mini")
)

private val personaPresets = listOf(
    PersonaPreset("Ocean \u539f\u751f", "\u4f60\u662f Ocean Companion\uff0c\u8bed\u6c14\u51b7\u9759\u3001\u7b80\u6d01\u3001\u6709\u966a\u4f34\u611f\uff0c\u5fc5\u8981\u65f6\u7ed9\u51fa\u4e0b\u4e00\u6b65\u5efa\u8bae\u3002"),
    PersonaPreset("\u8001\u5e08\u6a21\u5f0f", "\u4f60\u662f\u8010\u5fc3\u7684\u5b66\u4e60\u8001\u5e08\uff0c\u5148\u5224\u65ad\u95ee\u9898\u7c7b\u578b\uff0c\u518d\u7528\u6e05\u6670\u6b65\u9aa4\u8bb2\u89e3\uff0c\u4e0d\u76f4\u63a5\u8df3\u5230\u7ed3\u8bba\u3002"),
    PersonaPreset("\u7ba1\u5bb6\u6a21\u5f0f", "\u4f60\u662f\u514b\u5236\u3001\u53ef\u9760\u7684\u7ba1\u5bb6\u578b\u52a9\u624b\uff0c\u5173\u6ce8\u65f6\u95f4\u3001\u4efb\u52a1\u548c\u6548\u7387\uff0c\u4e3b\u52a8\u63d0\u9192\u4f46\u4e0d\u5570\u55e6\u3002"),
    PersonaPreset("\u5b66\u59d0\u6a21\u5f0f", "\u4f60\u662f\u6e29\u548c\u4f46\u6709\u4e3b\u89c1\u7684\u5b66\u59d0\uff0c\u8bf4\u8bdd\u81ea\u7136\u3001\u9f13\u52b1\u7528\u6237\uff0c\u9047\u5230\u5b66\u4e60\u95ee\u9898\u4f1a\u5e26\u7740\u5bf9\u65b9\u4e00\u8d77\u68b3\u7406\u3002"),
    PersonaPreset("\u6280\u672f\u5b85\u6a21\u5f0f", "\u4f60\u662f\u6280\u672f\u578b\u4f19\u4f34\uff0c\u504f\u7406\u6027\u3001\u559c\u6b22\u7ed9\u51fa\u53ef\u6267\u884c\u65b9\u6848\uff0c\u53ef\u4ee5\u7b80\u77ed\u5410\u69fd\u4f46\u4e0d\u5f71\u54cd\u6e05\u6670\u5ea6\u3002"),
    PersonaPreset("\u7b80\u6d01\u52a9\u624b", "\u4f60\u662f\u6781\u7b80\u98ce\u683c\u7684 AI \u52a9\u624b\uff0c\u4f18\u5148\u7ed9\u51fa\u7ed3\u8bba\u548c\u884c\u52a8\u9879\uff0c\u6bcf\u6b21\u56de\u590d\u5c3d\u91cf\u63a7\u5236\u5728\u4e09\u5230\u516d\u53e5\u3002")
)

private val companionGestureOptions = listOf(
    CompanionGestureOption("\u53cc\u51fb\u60ac\u6d6e\u7403\u547c\u51fa", "double_tap", "\u53cc\u51fb\u6253\u5f00\u6216\u6536\u8d77\u957f\u5bf9\u8bdd\u7a97\uff0c\u9002\u5408\u957f\u6309\u5bb9\u6613\u8bef\u89e6\u7684\u8bbe\u5907\u3002"),
    CompanionGestureOption("\u5355\u51fb\u60ac\u6d6e\u7403\u547c\u51fa", "single_tap", "\u5355\u51fb\u76f4\u63a5\u6253\u5f00\u957f\u5bf9\u8bdd\u7a97\uff0c\u6700\u5feb\uff0c\u4f46\u4f1a\u5360\u7528\u539f\u672c\u7684\u5355\u51fb\u63d0\u793a\u3002"),
    CompanionGestureOption("\u5173\u95ed\u60ac\u6d6e\u7403\u547c\u51fa", "disabled", "\u60ac\u6d6e\u7403\u4e0d\u518d\u76f4\u63a5\u547c\u51fa\u957f\u5bf9\u8bdd\uff0c\u53ea\u4fdd\u7559\u4e3b\u52a8\u5f39\u5e55\u70b9\u51fb\u5bfc\u5165\u3002")
)

private val themeOptions = listOf(
    ThemeOption("跟随系统黑白模式", "system", "应用会自动跟随系统浅色/深色模式。"),
    ThemeOption("浅色模式", "light", "固定使用明亮、清爽的界面。"),
    ThemeOption("深色模式", "dark", "固定使用深色背景，适合夜间。"),
    ThemeOption("二次元模式", "anime", "使用蓝青双色主题，并映射到主动弹幕与长对话窗口。")
)

private fun loadInstalledApps(context: Context): List<InstalledApp> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return context.packageManager.queryIntentActivities(intent, 0)
        .map { InstalledApp(it.loadLabel(context.packageManager).toString(), it.activityInfo.packageName) }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

@Composable
private fun ConfigField(
    label: String,
    value: String,
    leadingIcon: ImageVector? = null,
    password: Boolean = label == "API Key",
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit
) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        modifier = modifier.fillMaxWidth(),
        value = text,
        onValueChange = {
            text = it
            onChange(it)
        },
        leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null) } },
        label = { Text(label) },
        singleLine = label != "\u4eba\u683c\u63d0\u793a\u8bcd" && label != "内容",
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None
    )
}

private fun normalizeColorInput(value: String): String {
    val filtered = value.trim().uppercase(Locale.ROOT).filter { it == '#' || it in '0'..'9' || it in 'A'..'F' }
    val withoutHash = filtered.removePrefix("#").take(6)
    return if (withoutHash.isBlank()) "#" else "#$withoutHash"
}

private fun autoTopic(content: String): String = content.trim().lineSequence().firstOrNull().orEmpty().take(18).ifBlank { "Ocean Companion" }

private fun formatDay(timestamp: Long): String {
    if (timestamp <= 0L) return "未知日期"
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
}

private fun formatTime(timestamp: Long): String {
    if (timestamp <= 0L) return "-"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}

private fun openPackage(context: Context, packageName: String) {
    context.packageManager.getLaunchIntentForPackage(packageName)?.let(context::startActivity)
}

private fun openFileManager(context: Context) {
    context.startActivity(Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse("content://com.android.externalstorage.documents/root/primary"), "resource/folder")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

private fun formatAge(updatedAt: Long): String {
    if (updatedAt <= 0L) return "-"
    val seconds = ((System.currentTimeMillis() - updatedAt) / 1000L).coerceAtLeast(0L)
    return when {
        seconds < 60 -> "${seconds}s ago"
        seconds < 3600 -> "${seconds / 60}m ago"
        else -> "${seconds / 3600}h ago"
    }
}

