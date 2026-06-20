package com.projectocean.oceancompanion.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.BubbleChart
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.ScreenshotMonitor
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.unit.dp
import com.projectocean.oceancompanion.agent.SharedScreenContext
import com.projectocean.oceancompanion.memory.PreferencesStore
import kotlinx.coroutines.launch

private data class NavItem(val label: String, val icon: ImageVector)

@Composable
fun OceanApp(
    onStartFloating: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onRequestScreenCapture: () -> Unit,
    onPickFile: () -> Unit,
    onPickIconImage: () -> Unit
) {
    var selected by remember { mutableIntStateOf(0) }
    val items = listOf(
        NavItem("Ocean", Icons.Outlined.BubbleChart),
        NavItem("\u8bc6\u5c4f", Icons.Outlined.ScreenshotMonitor),
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
            else -> SettingsScreen(Modifier.padding(padding), onPickIconImage)
        }
    }
}

@Composable
private fun HomeScreen(modifier: Modifier, onStartFloating: () -> Unit) {
    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = Color.Transparent) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(
                        Brush.linearGradient(listOf(Color(0xFF0E6FFF), Color(0xFF00A6A6), Color(0xFFFFB23F)))
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
                        Text("\u957f\u6309\u60ac\u6d6e\u7403\u5c55\u5f00\u4f34\u968f\u9762\u677f\uff0c\u53ef\u6309\u5f53\u524d\u684c\u9762\u4fe1\u606f\u8fdb\u884c\u957f\u5bf9\u8bdd\u3002", color = Color.White.copy(alpha = 0.92f))
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
        MutedText("\u5148\u5f00\u542f\u65e0\u969c\u788d\u670d\u52a1\u4ee5\u8bfb\u53d6\u53ef\u89c1\u6587\u672c\uff1b\u957f\u65f6\u4f34\u968f\u671f\u95f4\u4f1a\u7ed3\u5408\u8fd9\u4e9b\u4e0a\u4e0b\u6587\u8bf4\u8bdd\u3002")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconTextButton("\u65e0\u969c\u788d", Icons.Outlined.AccessibilityNew, onOpenAccessibility, Modifier.weight(1f))
            IconTextButton("OCR\u6388\u6743", Icons.Outlined.ScreenshotMonitor, onRequestScreenCapture, Modifier.weight(1f))
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
                MutedText("OCR \u6309\u94ae\u76ee\u524d\u53ea\u68c0\u67e5\u622a\u5c4f\u6388\u6743\uff0c\u8fd8\u6ca1\u6709\u771f\u6b63\u628a\u5c4f\u5e55\u753b\u9762\u6293\u53d6\u6210\u56fe\u5e76\u4ea4\u7ed9 ML Kit OCR\u3002", small = true)
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
private fun SettingsScreen(modifier: Modifier, onPickIconImage: () -> Unit) {
    val context = LocalContext.current
    val store = remember { PreferencesStore(context) }
    val scope = rememberCoroutineScope()
    val provider by store.provider.collectAsState("openai")
    val apiBaseUrl by store.apiBaseUrl.collectAsState("")
    val apiKey by store.apiKey.collectAsState("")
    val modelName by store.modelName.collectAsState("")
    val userName by store.userName.collectAsState("\u4f60")
    val companionName by store.companionName.collectAsState("Ocean")
    val personaPrompt by store.customPersonaPrompt.collectAsState("")
    val iconText by store.iconText.collectAsState("Ocean")
    val speechInterval by store.speechIntervalMinutes.collectAsState(15)
    val triggerApps by store.triggerAppNames.collectAsState("")
    val panelRatio by store.panelRatio.collectAsState(0.5f)
    val proactive by store.proactiveReminders.collectAsState(true)
    val installedApps = remember { loadInstalledApps(context).take(24) }

    var providerDraft by remember { mutableStateOf(provider) }
    var apiBaseUrlDraft by remember { mutableStateOf(apiBaseUrl) }
    var apiKeyDraft by remember { mutableStateOf(apiKey) }
    var modelNameDraft by remember { mutableStateOf(modelName) }
    var userNameDraft by remember { mutableStateOf(userName) }
    var companionNameDraft by remember { mutableStateOf(companionName) }
    var personaPromptDraft by remember { mutableStateOf(personaPrompt) }
    var iconTextDraft by remember { mutableStateOf(iconText) }
    var speechIntervalDraft by remember { mutableStateOf(speechInterval) }
    var triggerAppsDraft by remember { mutableStateOf(triggerApps) }
    var panelRatioDraft by remember { mutableStateOf(panelRatio) }
    var proactiveDraft by remember { mutableStateOf(proactive) }
    var savedNotice by remember { mutableStateOf("") }
    var draftsLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(provider, apiBaseUrl, apiKey, modelName, userName, companionName, personaPrompt, iconText, speechInterval, triggerApps, panelRatio, proactive) {
        if (draftsLoaded) return@LaunchedEffect
        providerDraft = provider
        apiBaseUrlDraft = apiBaseUrl
        apiKeyDraft = apiKey
        modelNameDraft = modelName
        userNameDraft = userName
        companionNameDraft = companionName
        personaPromptDraft = personaPrompt
        iconTextDraft = iconText
        speechIntervalDraft = speechInterval
        triggerAppsDraft = triggerApps
        panelRatioDraft = panelRatio
        proactiveDraft = proactive
        draftsLoaded = true
    }

    LazyColumn(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("\u81ea\u5b9a\u4e49\u4e0e AI API", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item {
            OceanCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("\u4f7f\u7528\u8bf4\u660e", fontWeight = FontWeight.Bold)
                    MutedText("\u5148\u586b\u5199 API Base URL\u3001API Key \u548c\u6a21\u578b\u540d\u79f0\uff0c\u518d\u70b9\u51fb\u4e0b\u65b9\u4fdd\u5b58\u8bbe\u7f6e\u3002\u957f\u6309\u60ac\u6d6e\u7403\u5f00\u542f\u957f\u65f6\u4f34\u968f\uff1b\u5355\u51fb\u8be2\u95ee\u5206\u6790\uff0c\u53cc\u51fb\u52a0\u5165\u5feb\u901f\u8bc6\u5c4f\u961f\u5217\u3002")
                    MutedText("\u5df2\u4fdd\u5b58\u7684\u8bbe\u7f6e\u4f1a\u5728\u4e0b\u4e00\u6b21 AI \u56de\u590d\u3001\u81ea\u52a8\u53d1\u8a00\u6216\u91cd\u542f\u60ac\u6d6e\u670d\u52a1\u65f6\u751f\u6548\u3002", small = true)
                }
            }
        }
        item {
            OceanCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("AI \u63d0\u4f9b\u5546\u9884\u8bbe", fontWeight = FontWeight.Bold)
                    providerPresets.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { preset ->
                                OutlinedButton(onClick = {
                                    providerDraft = preset.provider
                                    apiBaseUrlDraft = preset.baseUrl
                                    modelNameDraft = preset.model
                                }, modifier = Modifier.weight(1f)) { Text(preset.label, maxLines = 1) }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    MutedText("\u9884\u8bbe\u53ea\u586b\u5145 Base URL \u548c\u6a21\u578b\u540d\uff0cAPI Key \u4ecd\u9700\u81ea\u5df1\u586b\u5199\u3002", small = true)
                }
            }
        }
        item { ConfigField("AI \u63d0\u4f9b\u5546", providerDraft) { providerDraft = it } }
        item { ConfigField("API Base URL", apiBaseUrlDraft) { apiBaseUrlDraft = it } }
        item { ConfigField("API Key", apiKeyDraft) { apiKeyDraft = it } }
        item { ConfigField("\u6a21\u578b\u540d\u79f0", modelNameDraft) { modelNameDraft = it } }
        item { ConfigField("\u7528\u6237\u6635\u79f0", userNameDraft) { userNameDraft = it.take(20) } }
        item { ConfigField("AI \u4f19\u4f34\u540d\u79f0", companionNameDraft) { companionNameDraft = it.take(20) } }
        item { ConfigField("\u60ac\u6d6e\u7403\u56fe\u6807\u6587\u5b57", iconTextDraft) { iconTextDraft = it.take(8) } }
        item {
            OceanCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("\u60ac\u6d6e\u667a\u80fd\u4f53\u56fe\u6807", fontWeight = FontWeight.Bold)
                    MutedText("\u53ef\u9009\u62e9\u672c\u5730\u56fe\u7247\u5e76\u8c03\u7528\u7cfb\u7edf\u88c1\u5207\u4e3a\u6b63\u65b9\u5f62\u56fe\u6807\u3002")
                    IconTextButton("\u4e0a\u4f20\u5e76\u88c1\u5207\u56fe\u6807", Icons.Outlined.Image, onPickIconImage, Modifier.fillMaxWidth())
                }
            }
        }
        item {
            OceanCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("\u4eba\u683c\u9884\u8bbe", fontWeight = FontWeight.Bold)
                    personaPresets.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { preset ->
                                OutlinedButton(onClick = { personaPromptDraft = preset.prompt }, modifier = Modifier.weight(1f)) { Text(preset.label, maxLines = 1) }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        item { ConfigField("\u4eba\u683c\u63d0\u793a\u8bcd", personaPromptDraft) { personaPromptDraft = it } }
        item { ConfigField("\u8f6f\u4ef6\u540d\u81ea\u52a8\u89e6\u53d1\uff08\u9017\u53f7\u5206\u9694\uff09", triggerAppsDraft) { triggerAppsDraft = it } }
        item {
            OceanCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("\u5e94\u7528\u5feb\u901f\u9009\u62e9", fontWeight = FontWeight.SemiBold)
                    MutedText("\u70b9\u51fb\u5e94\u7528\u540d\u53ef\u52a0\u5165\u81ea\u52a8\u89e6\u53d1\u5173\u952e\u8bcd\u3002", small = true)
                    installedApps.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { app ->
                                OutlinedButton(onClick = {
                                    triggerAppsDraft = listOf(triggerAppsDraft, app.label).filter { it.isNotBlank() }.joinToString(",")
                                }, modifier = Modifier.weight(1f)) { Text(app.label, maxLines = 1) }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        item {
            OceanCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("\u4e3b\u52a8\u81ea\u52a8\u53d1\u8a00", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Switch(checked = proactiveDraft, onCheckedChange = { proactiveDraft = it })
                    }
                    MutedText("\u8fdb\u5165\u547d\u4e2d\u7684\u5e94\u7528\u4f1a\u7acb\u5373\u4e3b\u52a8\u53d1\u8a00\uff1b\u4e0b\u65b9\u95f4\u9694\u53ea\u7528\u4e8e\u65e5\u5e38\u5de1\u68c0\u3002", small = true)
                    Text("\u53d1\u8a00\u95f4\u9694\uff1a${speechIntervalDraft} \u5206\u949f")
                    Slider(value = speechIntervalDraft.toFloat(), onValueChange = { speechIntervalDraft = it.toInt().coerceIn(1, 120) }, valueRange = 1f..120f)
                    Text("\u4f34\u968f\u9762\u677f\u5360\u5c4f\u6bd4\u4f8b\uff1a${(panelRatioDraft * 100).toInt()}%")
                    Slider(value = panelRatioDraft, onValueChange = { panelRatioDraft = it.coerceIn(0.35f, 0.8f) }, valueRange = 0.35f..0.8f)
                }
            }
        }
        item {
            OceanCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        scope.launch {
                            store.saveSettings(
                                provider = providerDraft.trim().ifBlank { "openai" },
                                apiBaseUrl = apiBaseUrlDraft.trim(),
                                apiKey = apiKeyDraft.trim(),
                                modelName = modelNameDraft.trim(),
                                userName = userNameDraft.trim().ifBlank { "\u4f60" },
                                companionName = companionNameDraft.trim().ifBlank { "Ocean" },
                                iconText = iconTextDraft.trim().ifBlank { "Ocean" }.take(8),
                                customPersonaPrompt = personaPromptDraft,
                                triggerAppNames = triggerAppsDraft.trim(),
                                speechIntervalMinutes = speechIntervalDraft.coerceIn(1, 120),
                                panelRatio = panelRatioDraft.coerceIn(0.35f, 0.8f),
                                proactiveReminders = proactiveDraft
                            )
                            savedNotice = "\u8bbe\u7f6e\u5df2\u4fdd\u5b58"
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("\u4fdd\u5b58\u8bbe\u7f6e")
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
            OceanCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("\u76f8\u5173\u8bf4\u660e", fontWeight = FontWeight.Bold)
                    listOf(
                        "\u957f\u65f6\u4f34\u968f\uff1a\u7ad6\u5c4f\u4e3a\u4e0b\u534a\u5c4f\uff0c\u6a2a\u5c4f\u4e3a\u53f3\u4fa7\u7a97\uff0c\u53ef\u5728\u4e0a\u65b9\u8c03\u8282\u5360\u5c4f\u6bd4\u4f8b\u3002",
                        "AI \u5bf9\u8bdd\uff1a\u4f7f\u7528 OpenAI \u517c\u5bb9 /chat/completions \u63a5\u53e3\uff0c\u9700\u8981 Base URL\u3001API Key \u548c\u6a21\u578b\u540d\u79f0\u3002",
                        "\u957f\u671f\u8bb0\u5fc6\uff1a\u4f34\u968f\u7a97\u5185\u7684\u7528\u6237\u53d1\u8a00\u548c AI \u56de\u590d\u4f1a\u5199\u5165\u672c\u5730\u6570\u636e\u5e93\uff0c\u4e0b\u6b21\u56de\u590d\u4f1a\u5e26\u4e0a\u6700\u8fd1\u8bb0\u5fc6\u3002",
                        "\u8bc6\u5c4f\uff1a\u65e0\u969c\u788d\u670d\u52a1\u63d0\u4f9b\u5f53\u524d\u9875\u9762\u6587\u672c\u4e0a\u4e0b\u6587\uff0cOCR \u548c\u6587\u4ef6\u9009\u62e9\u4f5c\u4e3a\u540e\u7eed\u5206\u6790\u5165\u53e3\u3002",
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

private fun loadInstalledApps(context: Context): List<InstalledApp> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return context.packageManager.queryIntentActivities(intent, 0)
        .map { InstalledApp(it.loadLabel(context.packageManager).toString(), it.activityInfo.packageName) }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

@Composable
private fun ConfigField(label: String, value: String, onChange: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = text,
        onValueChange = {
            text = it
            onChange(it)
        },
        label = { Text(label) },
        singleLine = label != "\u4eba\u683c\u63d0\u793a\u8bcd"
    )
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
