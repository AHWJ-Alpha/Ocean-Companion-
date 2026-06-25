package com.projectocean.oceancompanion.service

import android.app.Service
import android.app.usage.UsageStatsManager
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.RelativeSizeSpan
import android.text.style.TypefaceSpan
import android.text.style.ForegroundColorSpan
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.graphics.Typeface
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.projectocean.oceancompanion.R
import com.projectocean.oceancompanion.ai.FallbackAIClient
import com.projectocean.oceancompanion.ai.PromptEngine
import com.projectocean.oceancompanion.ai.SearchClient
import com.projectocean.oceancompanion.ai.SpeechClient
import com.projectocean.oceancompanion.ai.normalizeSpeechProvider
import com.projectocean.oceancompanion.agent.SharedScreenContext
import com.projectocean.oceancompanion.memory.ConversationHistory
import com.projectocean.oceancompanion.memory.LongTermMemory
import com.projectocean.oceancompanion.memory.OceanDatabase
import com.projectocean.oceancompanion.memory.PreferencesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class FloatingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var windowManager: WindowManager
    private lateinit var preferences: PreferencesStore
    private lateinit var database: OceanDatabase
    private lateinit var speechClient: SpeechClient
    private lateinit var wavVoiceRecorder: WavVoiceRecorder
    private val promptEngine = PromptEngine()
    private var bubble: View? = null
    private var companionPanel: View? = null
    private var proactiveBanner: View? = null
    private var proactiveBannerParams: WindowManager.LayoutParams? = null
    private var pendingProactiveLine: String? = null
    private var bubbleX = 28
    private var bubbleY = 220
    private var schedulerJob: Job? = null
    private var lastTap = 0L
    private var lastTriggeredPackage = ""
    private var lastSeenPackage = ""
    private var lastImmediateSpeakAt = 0L
    private var lastRoutineSpeakAt = 0L
    private var lastAnyProactiveAt = 0L
    private var lastProactiveText = ""
    private var panelAnimating = false
    private var companionPanelParams: WindowManager.LayoutParams? = null
    private var currentUserName = "\u4f60"
    private var currentCompanionName = "Ocean"
    private var currentProactiveBannerMaxChars = 60
    private var currentProactiveBannerOffsetDp = 12
    private var currentProactiveMuteMinutes = 30
    private var currentCompanionOpenGesture = "double_tap"
    private var currentTtsEnabled = false
    private var currentTtsProvider = "system"
    private var currentSttProvider = "system"
    private var currentTtsVoice = ""
    private var currentSttLanguage = "zh-CN"
    private var textToSpeech: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private var ttsReady = false
    private var speechRecognizer: SpeechRecognizer? = null
    private var voiceListening = false
    private var systemSpeechStarting = false
    private var systemSpeechReady = false
    private var voicePulseJob: Job? = null
    private var cloudVoiceStartJob: Job? = null
    private var cloudVoiceFile: File? = null
    private var currentCompanionTheme = CompanionTheme.default()
    private var currentSessionId = "session-${System.currentTimeMillis()}"
    private var currentSessionTopic = "Ocean Companion"
    private val conversationLines = mutableListOf<String>()
    private var manualReplyJob: Job? = null
    private var panelVisibleState = mutableStateOf(false)
    private var bubbleManuallyHidden = false

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_USER_PRESENT -> ensureBubbleVisible()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        preferences = PreferencesStore(this)
        database = OceanDatabase.create(this)
        speechClient = SpeechClient(preferences)
        wavVoiceRecorder = WavVoiceRecorder(this)
        startForeground(NotificationService.FLOATING_NOTIFICATION_ID, NotificationService.floatingNotification(this))
        scope.launch { preferences.userName.collect { currentUserName = it.ifBlank { "\u4f60" } } }
        scope.launch { preferences.companionName.collect { currentCompanionName = it.ifBlank { "Ocean" } } }
        scope.launch { preferences.proactiveBannerMaxChars.collect { currentProactiveBannerMaxChars = it } }
        scope.launch { preferences.proactiveBannerOffsetDp.collect { currentProactiveBannerOffsetDp = it.coerceIn(0, 160) } }
        scope.launch { preferences.proactiveMuteMinutes.collect { currentProactiveMuteMinutes = it.coerceIn(5, 240) } }
        scope.launch { preferences.companionOpenGesture.collect { currentCompanionOpenGesture = it.ifBlank { "double_tap" } } }
        scope.launch { preferences.ttsEnabled.collect { currentTtsEnabled = it; ensureTts() } }
        scope.launch { preferences.ttsProvider.collect { currentTtsProvider = it.normalizeSpeechProvider(); ensureTts() } }
        scope.launch { preferences.sttProvider.collect { currentSttProvider = it.normalizeSpeechProvider() } }
        scope.launch { preferences.ttsVoice.collect { currentTtsVoice = it; applyTtsVoice() } }
        scope.launch { preferences.sttLanguage.collect { currentSttLanguage = it.ifBlank { "zh-CN" } } }
        runCatching {
            registerReceiver(
                screenStateReceiver,
                IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_ON)
                    addAction(Intent.ACTION_USER_PRESENT)
                }
            )
        }
        if (Settings.canDrawOverlays(this)) showBubble()
        startAutoSpeechScheduler()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_FILE_CONTEXT_READY -> {
                val title = intent.getStringExtra(EXTRA_FILE_TITLE).orEmpty().ifBlank { "\u6587\u4ef6" }
                val readable = intent.getBooleanExtra(EXTRA_FILE_READABLE, false)
                val preview = intent.getStringExtra(EXTRA_FILE_PREVIEW).orEmpty()
                handleFileContextReady(title, readable, preview)
            }
            ACTION_SCREEN_ANALYSIS_READY -> {
                handleScreenAnalysisReady(intent.getStringExtra(EXTRA_SCREEN_ANALYSIS).orEmpty())
            }
        }
        return START_STICKY
    }
    override fun onDestroy() {
        schedulerJob?.cancel()
        stopVoiceFeedback(success = true, vibrate = false)
        cloudVoiceStartJob?.cancel()
        manualReplyJob?.cancel()
        wavVoiceRecorder.stop()
        destroySystemSpeechRecognizer()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        stopCloudTts()
        if (::windowManager.isInitialized) {
            bubble?.let { runCatching { windowManager.removeView(it) } }
            companionPanel?.let { runCatching { windowManager.removeView(it) } }
            proactiveBanner?.let { runCatching { windowManager.removeView(it) } }
        }
        runCatching { unregisterReceiver(screenStateReceiver) }
        scope.cancel()
        super.onDestroy()
    }

    private fun ensureBubbleVisible() {
        if (bubbleManuallyHidden) return
        if (!Settings.canDrawOverlays(this)) return
        if (bubble != null) return
        if (!::windowManager.isInitialized) return
        showBubble()
    }

    private fun showBubble() {
        bubbleManuallyHidden = false
        if (bubble != null) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            148,
            148,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = bubbleX
            y = bubbleY
        }

        val labelView = TextView(this).apply {
            text = "Ocean"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(this@FloatingService, android.R.color.white))
        }
        val imageView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            visibility = View.GONE
        }
        val view = FrameLayout(this).apply {
            background = ContextCompat.getDrawable(this@FloatingService, R.drawable.ocean_bubble)
            elevation = 12f
            addView(imageView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            addView(labelView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            clipToOutline = true
        }

        scope.launch { preferences.iconText.collect { labelView.text = it.ifBlank { "Ocean" } } }
        scope.launch {
            preferences.iconImageUri.collect { uriText ->
                if (uriText.isBlank()) {
                    imageView.visibility = View.GONE
                    labelView.visibility = View.VISIBLE
                } else {
                    imageView.setImageURI(Uri.parse(uriText))
                    imageView.visibility = View.VISIBLE
                    labelView.visibility = View.GONE
                }
            }
        }

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var dragging = false
        var longPressTriggered = false
        var longPressJob: Job? = null
        var singleTapJob: Job? = null
        var tapCount = 0
        var lastTapAt = 0L
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    singleTapJob?.cancel()
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    dragging = false
                    longPressTriggered = false
                    longPressJob?.cancel()
                    longPressJob = scope.launch {
                        delay(ViewConfiguration.getLongPressTimeout().toLong())
                        if (!dragging && bubble === view) {
                            longPressTriggered = true
                            startVoiceConversation()
                        }
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchX
                    val dy = event.rawY - touchY
                    if (!dragging && (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop)) {
                        dragging = true
                        longPressJob?.cancel()
                    }
                    if (dragging) {
                        params.x = startX + dx.toInt()
                        params.y = startY + dy.toInt()
                        bubbleX = params.x
                        bubbleY = params.y
                        windowManager.updateViewLayout(view, params)
                        updateProactiveBannerPosition()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    longPressJob?.cancel()
                    if (longPressTriggered) {
                        stopVoiceConversation()
                        longPressTriggered = false
                        return@setOnTouchListener true
                    }
                    if (dragging) {
                        dragging = false
                        return@setOnTouchListener true
                    }
                    val now = System.currentTimeMillis()
                    tapCount = if (now - lastTapAt < 360) tapCount + 1 else 1
                    lastTapAt = now
                    singleTapJob?.cancel()
                    if (tapCount >= 3) {
                        tapCount = 0
                        clearProactiveBanner(import = false)
                        hideBubbleWithAnimation(view)
                        return@setOnTouchListener true
                    }
                    singleTapJob = scope.launch {
                        delay(370)
                        val count = tapCount
                        tapCount = 0
                        if (count == 1) {
                            rotateBubble(view)
                            if (currentCompanionOpenGesture == "single_tap") toggleCompanionPanel()
                        } else if (count == 2) {
                            if (currentCompanionOpenGesture == "double_tap") {
                                toggleCompanionPanel()
                            } else {
                                Toast.makeText(this@FloatingService, "${companionName()}：双击可在设置里绑定长对话。", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    lastTap = now
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    longPressJob?.cancel()
                    singleTapJob?.cancel()
                    if (longPressTriggered) stopVoiceConversation()
                    dragging = false
                    longPressTriggered = false
                    true
                }
                else -> false
            }
        }

        bubble = view
        windowManager.addView(view, params)
    }

    private fun rotateBubble(view: View) {
        view.animate()
            .rotationBy(360f)
            .setDuration(720)
            .setInterpolator(DecelerateInterpolator(1.4f))
            .start()
    }

    private fun hideBubbleWithAnimation(view: View) {
        bubbleManuallyHidden = true
        Toast.makeText(this, "${companionName()}：悬浮球已隐藏，回到应用可重新启动。", Toast.LENGTH_SHORT).show()
        view.animate()
            .alpha(0f)
            .scaleX(0.62f)
            .scaleY(0.62f)
            .setDuration(260)
            .setInterpolator(AccelerateInterpolator(1.5f))
            .withEndAction {
                runCatching { windowManager.removeView(view) }
                if (bubble === view) bubble = null
            }
            .start()
    }

    private fun startVoiceConversation() {
        if (voiceListening) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pulseVoiceFailure()
            showProactiveBannerIfFresh("${companionName()}：还没有麦克风权限。请到系统权限里允许录音后，再长按悬浮球语音对话。")
            return
        }
        if (currentSttProvider != "system") {
            startCloudVoiceConversation()
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            pulseVoiceFailure()
            showProactiveBannerIfFresh("${companionName()}：当前系统没有可用的语音识别服务。")
            return
        }
        destroySystemSpeechRecognizer()
        voiceListening = true
        systemSpeechStarting = true
        systemSpeechReady = false
        startVoiceFeedback()
        showProactiveBanner("${companionName()}：正在启动系统语音识别。")
        val recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { speechRecognizer = it }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {
                systemSpeechStarting = false
                systemSpeechReady = true
                showProactiveBanner("${companionName()}：正在听，松手发送。")
            }
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() {
                voiceListening = false
                systemSpeechStarting = false
                systemSpeechReady = false
            }
            override fun onPartialResults(partialResults: android.os.Bundle?) = Unit
            override fun onEvent(eventType: Int, params: android.os.Bundle?) = Unit
            override fun onError(error: Int) {
                val failedBeforeReady = systemSpeechStarting && !systemSpeechReady
                voiceListening = false
                systemSpeechStarting = false
                systemSpeechReady = false
                stopVoiceFeedback(success = false)
                val reason = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "没有听清。"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "没有检测到说话。"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "麦克风权限不足。"
                    SpeechRecognizer.ERROR_CLIENT -> if (failedBeforeReady) {
                        "系统语音识别服务启动即失败。请在设置里切换云端 STT，或检查系统语音输入服务。"
                    } else {
                        "系统语音识别被系统中断。"
                    }
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "系统语音识别正忙，请稍后再试。"
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "系统语音识别网络不可用。"
                    else -> "语音识别失败：$error"
                }
                destroySystemSpeechRecognizer()
                showProactiveBannerIfFresh("${companionName()}：$reason")
            }
            override fun onResults(results: android.os.Bundle?) {
                voiceListening = false
                systemSpeechStarting = false
                systemSpeechReady = false
                stopVoiceFeedback(success = true)
                destroySystemSpeechRecognizer()
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().trim()
                if (text.isBlank()) {
                    pulseVoiceFailure()
                    showProactiveBannerIfFresh("${companionName()}：没有识别到有效语音。")
                    return
                }
                handleVoiceUserText(text)
            }
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentSttLanguage)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        runCatching { recognizer.startListening(intent) }.onFailure {
            voiceListening = false
            systemSpeechStarting = false
            systemSpeechReady = false
            stopVoiceFeedback(success = false)
            destroySystemSpeechRecognizer()
            showProactiveBannerIfFresh("${companionName()}：语音识别启动失败，请检查系统语音服务。")
        }
    }

    private fun stopVoiceConversation() {
        if (!voiceListening) return
        if (currentSttProvider != "system") {
            stopCloudVoiceConversation()
            return
        }
        if (systemSpeechStarting && !systemSpeechReady) {
            showProactiveBanner("${companionName()}：系统语音还在启动，已取消本次识别。")
            voiceListening = false
            systemSpeechStarting = false
            systemSpeechReady = false
            stopVoiceFeedback(success = false)
            destroySystemSpeechRecognizer()
            return
        }
        stopVoiceFeedback(success = true)
        runCatching { speechRecognizer?.stopListening() }
    }

    private fun destroySystemSpeechRecognizer() {
        runCatching { speechRecognizer?.cancel() }
        runCatching { speechRecognizer?.destroy() }
        speechRecognizer = null
    }

    private fun startCloudVoiceConversation() {
        voiceListening = true
        cloudVoiceStartJob?.cancel()
        cloudVoiceStartJob = scope.launch {
            val missing = withContext(Dispatchers.IO) {
                val keyBlank = preferences.sttApiKey.first().isBlank()
                val modelBlank = preferences.sttModel.first().isBlank()
                val baseBlank = preferences.sttApiBaseUrl.first().isBlank()
                when {
                    keyBlank -> "请先在语音设置里保存 STT API Key。"
                    modelBlank -> "请先在语音设置里填写 STT 模型。"
                    baseBlank -> "请先在语音设置里填写 STT Base URL。"
                    else -> ""
                }
            }
            if (!voiceListening) return@launch
            if (missing.isNotBlank()) {
                voiceListening = false
                pulseVoiceFailure()
                showProactiveBannerIfFresh("${companionName()}：$missing")
                return@launch
            }
            val started = wavVoiceRecorder.start()
            if (!voiceListening) {
                wavVoiceRecorder.stop()
                runCatching { started.getOrNull()?.delete() }
                return@launch
            }
            started.onSuccess { file ->
                cloudVoiceFile = file
                startVoiceFeedback()
                showProactiveBanner("${companionName()}：正在录音，松手后交给云端识别。")
            }.onFailure { error ->
                cloudVoiceFile = null
                voiceListening = false
                stopVoiceFeedback(success = false)
                showProactiveBannerIfFresh("${companionName()}：云端语音录制失败：${error.message.orEmpty().take(60)}")
            }
        }
    }

    private fun stopCloudVoiceConversation() {
        voiceListening = false
        cloudVoiceStartJob?.cancel()
        cloudVoiceStartJob = null
        stopVoiceFeedback(success = true)
        val file = wavVoiceRecorder.stop() ?: cloudVoiceFile
        cloudVoiceFile = null
        if (file == null || !file.exists() || file.length() <= 48L) {
            pulseVoiceFailure()
            showProactiveBannerIfFresh("${companionName()}：没有录到有效语音。")
            return
        }
        showProactiveBanner("${companionName()}：正在识别语音。")
        scope.launch {
            val result = speechClient.transcribe(file)
            runCatching { file.delete() }
            result.onSuccess { text ->
                val cleaned = text.trim()
                if (cleaned.isBlank()) {
                    pulseVoiceFailure()
                    showProactiveBannerIfFresh("${companionName()}：没有识别到有效语音。")
                } else {
                    handleVoiceUserText(cleaned)
                }
            }.onFailure { error ->
                pulseVoiceFailure()
                showProactiveBannerIfFresh("${companionName()}：云端语音识别失败：${error.message.orEmpty().take(72)}")
            }
        }
    }

    private fun startVoiceFeedback() {
        vibrate(35)
        bubble?.animate()?.cancel()
        voicePulseJob?.cancel()
        voicePulseJob = scope.launch {
            while (voiceListening) {
                bubble?.animate()
                    ?.scaleX(1.12f)
                    ?.scaleY(1.12f)
                    ?.alpha(0.86f)
                    ?.setDuration(240)
                    ?.setInterpolator(DecelerateInterpolator(1.2f))
                    ?.start()
                delay(260)
                bubble?.animate()
                    ?.scaleX(0.96f)
                    ?.scaleY(0.96f)
                    ?.alpha(1f)
                    ?.setDuration(240)
                    ?.setInterpolator(DecelerateInterpolator(1.2f))
                    ?.start()
                delay(260)
            }
        }
    }

    private fun stopVoiceFeedback(success: Boolean, vibrate: Boolean = true) {
        voicePulseJob?.cancel()
        voicePulseJob = null
        if (vibrate) vibrate(if (success) 24 else 90)
        bubble?.animate()
            ?.scaleX(1f)
            ?.scaleY(1f)
            ?.alpha(1f)
            ?.setDuration(180)
            ?.setInterpolator(DecelerateInterpolator(1.4f))
            ?.start()
    }

    private fun pulseVoiceFailure() {
        vibrate(90)
        bubble?.animate()
            ?.scaleX(1.04f)
            ?.scaleY(1.04f)
            ?.setDuration(90)
            ?.withEndAction {
                bubble?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(120)?.start()
            }
            ?.start()
    }

    private fun vibrate(durationMs: Long) {
        if (durationMs <= 0) return
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }
            if (!vibrator.hasVibrator()) return@runCatching
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        }
    }

    private fun handleVoiceUserText(text: String) {
        showProactiveBanner("${companionName()}：听到了「${text.take(40)}」，正在思考。")
        appendConversationLine("${userName()}：$text", remember = true)
        scope.launch {
            val reply = generateManualReply(text)
            showProactiveBannerIfFresh(reply)
            appendConversationLine(reply, remember = true)
            speakBannerIfEnabled(reply)
        }
    }

    private fun ensureTts() {
        if (!currentTtsEnabled) {
            textToSpeech?.stop()
            stopCloudTts()
            return
        }
        if (currentTtsProvider != "system") return
        if (textToSpeech != null) return
        textToSpeech = TextToSpeech(this) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                textToSpeech?.language = Locale.CHINESE
                applyTtsVoice()
            }
        }
    }

    private fun applyTtsVoice() {
        val tts = textToSpeech ?: return
        if (!ttsReady || currentTtsVoice.isBlank()) return
        val selected = tts.voices?.firstOrNull { voice ->
            voice.name.equals(currentTtsVoice, ignoreCase = true) || voice.locale.toLanguageTag().equals(currentTtsVoice, ignoreCase = true)
        }
        if (selected != null) tts.voice = selected
    }

    private fun speakBannerIfEnabled(message: String) {
        if (!currentTtsEnabled) return
        val text = message.substringAfter('：', message).trim().ifBlank { message }
        if (currentTtsProvider != "system") {
            speakCloudTts(text)
            return
        }
        ensureTts()
        if (!ttsReady) return
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ocean-banner-${System.currentTimeMillis()}")
    }

    private fun speakCloudTts(text: String) {
        scope.launch {
            speechClient.synthesize(text).onSuccess { audio ->
                runCatching {
                    val file = File(cacheDir, "ocean_tts.${audio.extension}")
                    file.writeBytes(audio.bytes)
                    stopCloudTts(showToast = false)
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(file.absolutePath)
                        setOnCompletionListener { stopCloudTts(showToast = false) }
                        setOnErrorListener { _, _, _ ->
                            stopCloudTts(showToast = false)
                            true
                        }
                        prepare()
                        start()
                    }
                }
            }.onFailure { error ->
                Toast.makeText(this@FloatingService, "${companionName()}：云端朗读失败：${error.message.orEmpty().take(48)}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopTtsIfSpeaking(): Boolean {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                stopCloudTts()
                return true
            }
        }
        val tts = textToSpeech ?: return false
        if (!tts.isSpeaking) return false
        tts.stop()
        Toast.makeText(this, "${companionName()}：已停止朗读。", Toast.LENGTH_SHORT).show()
        return true
    }

    private fun stopCloudTts(showToast: Boolean = true) {
        val player = mediaPlayer ?: return
        runCatching { if (player.isPlaying) player.stop() }
        runCatching { player.release() }
        mediaPlayer = null
        if (showToast) Toast.makeText(this, "${companionName()}：已停止朗读。", Toast.LENGTH_SHORT).show()
    }

    private fun toggleCompanionPanel() {
        if (panelAnimating) return
        companionPanel?.let { panel ->
            panelAnimating = true
            panelVisibleState.value = false
            hideKeyboardFromPanel(panel)
            panel.animate().alpha(0f).translationY(if (isPortrait()) 90f else 0f).translationX(if (isPortrait()) 0f else 90f).setDuration(220).withEndAction {
                runCatching { windowManager.removeView(panel) }
                companionPanel = null
                companionPanelParams = null
                panelAnimating = false
            }.start()
            return
        }
        scope.launch { showCompanionPanel(importProactive = false) }
    }

    private suspend fun showCompanionPanel(importProactive: Boolean = true) {
        if (panelAnimating || companionPanel != null) return
        panelAnimating = true
        runCatching {
            val ratio = preferences.panelRatio.first().coerceIn(0.35f, 0.8f)
            val metrics = resources.displayMetrics
            val portrait = isPortrait()
            val theme = loadCompanionTheme()
            currentCompanionTheme = theme
            val width = if (portrait) WindowManager.LayoutParams.MATCH_PARENT else (metrics.widthPixels * ratio).toInt()
            val height = if (portrait) (metrics.heightPixels * ratio).toInt() else WindowManager.LayoutParams.MATCH_PARENT
            val panelGravity = if (portrait) Gravity.BOTTOM else Gravity.END

            val panel = buildNativeCompanionPanel(portrait, theme).apply {
                alpha = 0f
                translationY = if (portrait) 90f else 0f
                translationX = if (portrait) 0f else 90f
            }

            companionPanel = panel
            val params = WindowManager.LayoutParams(
                width,
                height,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = panelGravity
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            }
            companionPanelParams = params
            windowManager.addView(panel, params)
            panel.animate().alpha(1f).translationY(0f).translationX(0f).setDuration(320).setInterpolator(DecelerateInterpolator(1.8f)).withEndAction {
                panelAnimating = false
            }.start()
            panelVisibleState.value = true
            if (importProactive) importVisibleProactiveLine()
        }.onFailure {
            companionPanel = null
            companionPanelParams = null
            panelAnimating = false
            Toast.makeText(this, "${companionName()}\uff1a\u9762\u677f\u542f\u52a8\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5\u60ac\u6d6e\u7a97\u6743\u9650\u3002", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleFileContextReady(title: String, readable: Boolean, preview: String) {
        val line = if (readable) {
            "${companionName()}：已读取文件《$title》，接下来长对话会自动结合它回答。\n\n预览：\n${preview.take(360)}"
        } else {
            "${companionName()}：已接收文件《$title》，但当前版本还不能直接提取正文。\n\n$preview"
        }
        appendConversationLine(line, remember = false)
        if (companionPanel == null) scope.launch { showCompanionPanel(importProactive = false) }
    }

    private fun handleScreenAnalysisReady(analysis: String) {
        if (analysis.isBlank()) return
        showProactiveBannerIfFresh("${companionName()}\uff1a$analysis")
    }

    private fun buildNativeCompanionPanel(portrait: Boolean, theme: CompanionTheme): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 24, 30, 26)
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(theme.surfaceStart, theme.surfaceMid, theme.surfaceEnd)
            ).apply {
                cornerRadius = if (portrait) 34f else 30f
                setStroke(2, theme.stroke)
            }
            clipToOutline = true
            elevation = 26f
        }

        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(2, 0, 2, 6)
        }
        val title = TextView(this).apply {
            text = "${companionName()} \u957f\u65f6\u4f34\u968f"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(theme.textPrimary)
        }
        val close = TextView(this).apply {
            text = "×"
            contentDescription = "退出长时伴随"
            textSize = 22f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(theme.textPrimary)
            background = GradientDrawable().apply {
                setColor(theme.inputBackground)
                cornerRadius = 20f
                setStroke(1, theme.stroke)
            }
            setOnClickListener { toggleCompanionPanel() }
        }
        titleRow.addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        titleRow.addView(close, LinearLayout.LayoutParams(54, 54))

        val hint = TextView(this).apply {
            text = "已连接屏幕/文件上下文和长时记忆；竖屏为下半屏，横屏为右半屏。"
            textSize = 13f
            setTextColor(theme.textSecondary)
            setPadding(0, 4, 0, 12)
        }

        val messages = LinearLayout(this).apply {
            tag = MESSAGE_LIST_TAG
            orientation = LinearLayout.VERTICAL
            setPadding(12, 16, 12, 16)
        }
        val scroll = ScrollView(this).apply {
            tag = SCROLL_VIEW_TAG
            isFillViewport = true
            clipToOutline = true
            clipToPadding = false
            background = GradientDrawable().apply {
                setColor(theme.glassPanel)
                cornerRadius = 24f
                setStroke(1, theme.stroke)
            }
            addView(messages)
        }
        renderConversationMessages(messages, scroll, theme)

        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 12, 0, 0)
        }
        val input = EditText(this).apply {
            setHint("继续对话")
            setSingleLine(true)
            setTextColor(theme.textPrimary)
            setHintTextColor(theme.textSecondary)
            background = GradientDrawable().apply {
                setColor(theme.inputBackground)
                cornerRadius = 18f
                setStroke(1, theme.primarySoft)
            }
            setPadding(18, 0, 18, 0)
            setOnFocusChangeListener { view, hasFocus ->
                updateCompanionPanelFocusable(hasFocus)
                if (hasFocus) {
                    view.post {
                        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                            .showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
                    }
                }
            }
        }
        val send = TextView(this).apply {
            setText("➤")
            contentDescription = "\u53d1\u9001\u6d88\u606f"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(theme.primary, theme.accent)
            ).apply { cornerRadius = 22f }
            setOnClickListener {
                val text = input.text.toString().trim()
                if (text.isNotBlank()) {
                    input.setText("")
                    input.clearFocus()
                    appendConversationLine("${userName()}\uff1a$text", remember = true)
                    appendConversationLine("${companionName()}\uff1a\u6b63\u5728\u601d\u8003...", remember = false)
                    scope.launch {
                        removeThinkingLine()
                        speak(generateManualReply(text))
                    }
                }
            }
        }
        inputRow.addView(input, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        inputRow.addView(send, LinearLayout.LayoutParams(72, 64).apply { leftMargin = 10 })

        container.addView(titleRow)
        container.addView(hint)
        container.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        container.addView(inputRow)
        return container
    }

    private fun updateCompanionPanelFocusable(focusable: Boolean) {
        val panel = companionPanel ?: return
        val params = companionPanelParams ?: return
        val nextFlags = if (focusable) {
            params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        if (nextFlags == params.flags) return
        params.flags = nextFlags
        runCatching { windowManager.updateViewLayout(panel, params) }
    }

    private fun focusInputAndShowKeyboard(view: View) {
        updateCompanionPanelFocusable(true)
        view.isFocusableInTouchMode = true
        view.requestFocus()
        view.postDelayed({
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(view, InputMethodManager.SHOW_FORCED)
        }, 80)
    }

    private fun hideKeyboardFromPanel(view: View) {
        runCatching {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(view.windowToken, 0)
        }
        updateCompanionPanelFocusable(false)
    }

    private fun isPortrait(): Boolean = resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE

    private fun startAutoSpeechScheduler() {
        schedulerJob = scope.launch {
            while (true) {
                runCatching { checkAutoSpeechOnce() }
                delay(ACTIVE_APP_CHECK_INTERVAL_MS)
            }
        }
    }

    private suspend fun checkAutoSpeechOnce() = withContext(Dispatchers.Default) {
        val enabled = preferences.proactiveReminders.first()
        if (!enabled) return@withContext
        val now = System.currentTimeMillis()
        if (preferences.proactiveMutedUntil.first() > now) return@withContext
        val triggerApps = preferences.triggerAppNames.first().ifBlank { "\u5b66\u4e60,PDF,PPT,Word,Chrome" }
        val currentPackage = resolveCurrentPackage()
        val matchedApp = currentPackage.isNotBlank() && matchesTriggerApp(currentPackage, triggerApps)
        val enteredMatchedApp = matchedApp && currentPackage != lastTriggeredPackage
        val canGlobalSpeak = now - lastAnyProactiveAt > GLOBAL_PROACTIVE_COOLDOWN_MS
        val canImmediateSpeak = canGlobalSpeak && now - lastImmediateSpeakAt > IMMEDIATE_APP_COOLDOWN_MS
        if (enteredMatchedApp && canImmediateSpeak) {
            lastTriggeredPackage = currentPackage
            lastSeenPackage = currentPackage
            lastImmediateSpeakAt = now
            lastAnyProactiveAt = now
            val line = generateCompanionLine(triggerApps, reason = "app_enter", packageName = currentPackage)
            withContext(Dispatchers.Main) { showProactiveBannerIfFresh(line) }
        } else if (currentPackage.isNotBlank()) {
            lastSeenPackage = currentPackage
        }

        val minutes = preferences.speechIntervalMinutes.first().coerceIn(1, 120)
        val routineDue = now - lastRoutineSpeakAt >= minutes * 60_000L
        if (routineDue && canGlobalSpeak && currentPackage.isBlank()) {
            lastRoutineSpeakAt = now
            lastAnyProactiveAt = now
            val line = generateCompanionLine(triggerApps, reason = "routine_check", packageName = currentPackage)
            withContext(Dispatchers.Main) { showProactiveBannerIfFresh(line) }
        }

        if (currentPackage.isBlank() || !matchedApp) {
            lastTriggeredPackage = ""
        }
    }

    private fun resolveCurrentPackage(): String {
        val contextPackage = SharedScreenContext.packageName
        val contextFresh = System.currentTimeMillis() - SharedScreenContext.updatedAt < SCREEN_CONTEXT_FRESH_MS
        if (contextPackage.isNotBlank() && contextFresh) return contextPackage

        val usagePackage = recentForegroundPackage()
        if (usagePackage.isNotBlank()) {
            SharedScreenContext.updatePackage(usagePackage, source = "usage_stats")
            return usagePackage
        }
        return contextPackage.ifBlank { lastSeenPackage }
    }

    private fun recentForegroundPackage(): String {
        return runCatching {
            val manager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - USAGE_STATS_LOOKBACK_MS, now)
                .orEmpty()
                .filter { it.packageName != packageName && it.lastTimeUsed > 0L }
                .maxByOrNull { it.lastTimeUsed }
                ?.packageName
                .orEmpty()
        }.getOrDefault("")
    }

    private fun matchesTriggerApp(currentPackage: String, triggerApps: String): Boolean {
        val appLabel = currentAppLabel(currentPackage)
        return triggerApps.split(',', '\uff0c').map { it.trim() }.filter { it.isNotBlank() }.any { keyword ->
            currentPackage.contains(keyword, ignoreCase = true) || appLabel.contains(keyword, ignoreCase = true)
        }
    }

    private fun currentAppLabel(packageName: String): String {
        if (packageName.isBlank()) return ""
        return runCatching {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault("")
    }

    private suspend fun loadCompanionTheme(): CompanionTheme = withContext(Dispatchers.IO) {
        val mode = preferences.themeMode.first()
        val animePrimary = parseColorOrNull(preferences.animePrimaryColor.first()) ?: Color.rgb(57, 197, 187)
        val animeSecondary = parseColorOrNull(preferences.animeSecondaryColor.first()) ?: Color.rgb(0, 174, 239)
        if (mode == "anime") return@withContext CompanionTheme.fromPalette(animePrimary, animeSecondary)
        val uriText = preferences.iconImageUri.first()
        val dominant = dominantColorFromUri(uriText) ?: return@withContext CompanionTheme.default()
        CompanionTheme.fromPrimary(dominant)
    }

    private fun parseColorOrNull(value: String): Int? = runCatching {
        val normalized = value.trim().removePrefix("#")
        val number = normalized.toLong(16)
        when (normalized.length) {
            6 -> Color.rgb(((number shr 16) and 0xFF).toInt(), ((number shr 8) and 0xFF).toInt(), (number and 0xFF).toInt())
            8 -> Color.argb(((number shr 24) and 0xFF).toInt(), ((number shr 16) and 0xFF).toInt(), ((number shr 8) and 0xFF).toInt(), (number and 0xFF).toInt())
            else -> null
        }
    }.getOrNull()

    private fun dominantColorFromUri(uriText: String): Int? {
        if (uriText.isBlank()) return null
        return runCatching {
            contentResolver.openInputStream(Uri.parse(uriText))?.use { input ->
                BitmapFactory.decodeStream(input)?.let(::dominantColorFromBitmap)
            }
        }.getOrNull()
    }

    private fun dominantColorFromBitmap(bitmap: Bitmap): Int? {
        val scaled = Bitmap.createScaledBitmap(bitmap, 28, 28, true)
        var red = 0L
        var green = 0L
        var blue = 0L
        var count = 0L
        for (x in 0 until scaled.width) {
            for (y in 0 until scaled.height) {
                val color = scaled.getPixel(x, y)
                if (Color.alpha(color) < 96) continue
                val r = Color.red(color)
                val g = Color.green(color)
                val b = Color.blue(color)
                val max = maxOf(r, g, b)
                val min = minOf(r, g, b)
                if (max < 36 || max - min < 10) continue
                red += r
                green += g
                blue += b
                count++
            }
        }
        if (scaled !== bitmap) scaled.recycle()
        return if (count > 0) Color.rgb((red / count).toInt(), (green / count).toInt(), (blue / count).toInt()) else null
    }

    private fun blendColors(from: Int, to: Int, ratio: Float): Int {
        val inverse = 1f - ratio
        return Color.rgb(
            (Color.red(from) * inverse + Color.red(to) * ratio).toInt().coerceIn(0, 255),
            (Color.green(from) * inverse + Color.green(to) * ratio).toInt().coerceIn(0, 255),
            (Color.blue(from) * inverse + Color.blue(to) * ratio).toInt().coerceIn(0, 255)
        )
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun showProactiveBannerIfFresh(message: String) {
        val normalized = message.trim().replace(Regex("\\s+"), " ")
        if (normalized.isBlank()) return
        if (normalized == lastProactiveText) return
        lastProactiveText = normalized
        showProactiveBanner(message)
    }

    private fun showProactiveBanner(message: String) {
        if (!::windowManager.isInitialized || message.isBlank()) return
        clearProactiveBanner(import = false)
        pendingProactiveLine = message
        val maxChars = currentProactiveBannerMaxChars
        val unlimited = maxChars <= 0

        val text = TextView(this).apply {
            this.text = message
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(28, 18, 28, 18)
            if (!unlimited) maxLines = 3
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.argb(230, 16, 28, 45),
                    withAlpha(currentCompanionTheme.primary, 222),
                    withAlpha(currentCompanionTheme.accent, 210)
                )
            ).apply {
                cornerRadius = 34f
                setStroke(2, currentCompanionTheme.stroke)
            }
            elevation = 24f
            alpha = 0f
            scaleX = 0.96f
            scaleY = 0.96f
            translationY = -42f
            var bannerTapCount = 0
            var bannerLastTapAt = 0L
            var bannerTapJob: Job? = null
            setOnClickListener {
                if (stopTtsIfSpeaking()) return@setOnClickListener
                val now = System.currentTimeMillis()
                bannerTapCount = if (now - bannerLastTapAt < 360) bannerTapCount + 1 else 1
                bannerLastTapAt = now
                bannerTapJob?.cancel()
                if (bannerTapCount >= 3) {
                    bannerTapCount = 0
                    muteProactiveBanners()
                } else {
                    bannerTapJob = scope.launch {
                        delay(370)
                        if (bannerTapCount == 1) openCompanionPanelFromProactiveBanner()
                        bannerTapCount = 0
                    }
                }
            }
            setOnLongClickListener {
                openCompanionPanelFromProactiveBanner()
                true
            }
        }

        val widthRatio = if (unlimited) 0.82f else 0.72f
        val width = (resources.displayMetrics.widthPixels * widthRatio).toInt().coerceAtLeast(360)
        val params = WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = bannerX(width)
            y = bannerY()
        }

        proactiveBanner = text
        proactiveBannerParams = params
        runCatching { windowManager.addView(text, params) }
            .onSuccess {
                text.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setInterpolator(DecelerateInterpolator(1.7f))
                    .setDuration(360)
                    .start()
                text.postDelayed({
                    if (proactiveBanner === text) clearProactiveBanner(import = false)
                }, PROACTIVE_BANNER_DURATION_MS)
            }
            .onFailure {
                proactiveBanner = null
                proactiveBannerParams = null
                pendingProactiveLine = null
            }
    }

    private fun updateProactiveBannerPosition() {
        val banner = proactiveBanner ?: return
        val params = proactiveBannerParams ?: return
        params.x = bannerX(params.width)
        params.y = bannerY()
        runCatching { windowManager.updateViewLayout(banner, params) }
    }

    private fun muteProactiveBanners() {
        val minutes = currentProactiveMuteMinutes.coerceIn(5, 240)
        val until = System.currentTimeMillis() + minutes * 60_000L
        scope.launch { preferences.setProactiveMutedUntil(until) }
        Toast.makeText(this, "${companionName()}：主动弹幕已暂停 $minutes 分钟。", Toast.LENGTH_SHORT).show()
        clearProactiveBanner(import = false)
    }

    private fun bannerX(width: Int): Int {
        val screenWidth = resources.displayMetrics.widthPixels
        val offset = (currentProactiveBannerOffsetDp * resources.displayMetrics.density).toInt()
        val desired = bubbleX + 96 + offset
        return desired.coerceIn(12, (screenWidth - width - 12).coerceAtLeast(12))
    }

    private fun bannerY(): Int {
        val screenHeight = resources.displayMetrics.heightPixels
        val offset = (currentProactiveBannerOffsetDp * resources.displayMetrics.density * 0.35f).toInt()
        return (bubbleY + 8 + offset).coerceIn(32, (screenHeight - 220).coerceAtLeast(32))
    }

    private fun importVisibleProactiveLine() {
        clearProactiveBanner(import = true)
    }

    private fun openCompanionPanelFromProactiveBanner() {
        if (companionPanel != null) {
            importVisibleProactiveLine()
            return
        }
        scope.launch { showCompanionPanel(importProactive = true) }
    }

    private fun clearProactiveBanner(import: Boolean) {
        val line = pendingProactiveLine
        if (import && !line.isNullOrBlank()) appendConversationLine(line, remember = true)
        pendingProactiveLine = null
        val banner = proactiveBanner ?: return
        proactiveBanner = null
        proactiveBannerParams = null
        banner.animate()
            .alpha(0f)
            .translationY(-36f)
            .scaleX(0.98f)
            .scaleY(0.98f)
            .setInterpolator(AccelerateInterpolator(1.4f))
            .setDuration(260)
            .withEndAction { runCatching { windowManager.removeView(banner) } }
            .start()
    }

    private suspend fun generateCompanionLine(triggerApps: String, reason: String, packageName: String = SharedScreenContext.packageName): String {
        val screenText = SharedScreenContext.visibleText.ifBlank { "屏幕文字为空或暂不可读。" }
        val memoryText = recentMemoryText()
        val customPersona = preferences.customPersonaPrompt.first()
        val name = companionName()
        val user = userName()
        val currentPackage = packageName.ifBlank { SharedScreenContext.packageName }
        val appLabel = currentAppLabel(currentPackage)
        val source = SharedScreenContext.source.ifBlank { "unknown" }
        val appName = appLabel.ifBlank { currentPackage.ifBlank { "\u5f53\u524d\u684c\u9762" } }
        val maxChars = preferences.proactiveBannerMaxChars.first()
        val hint = screenText.lineSequence().map { it.trim() }.firstOrNull { it.length >= 4 }?.take(18)
        val localFallback = fitProactiveLength(
            if (hint != null) "$name：$user，屏幕里提到「$hint」，我先按这个线索帮你盯下一步。" else "$name：$user，当前画面文字不足，我先安静观察，不贸然下判断。",
            maxChars,
            name,
            user,
            appName,
            screenText
        )
        if (preferences.resolvedApiProfiles().none { it.isUsable() }) return localFallback
        return try {
            val request = promptEngine.buildProactiveCompanionPrompt(
                screenText = screenText,
                customPersona = "AI name: $name\nUser name: $user\nCurrent app: $appName\nContext source: $source\nTrigger reason: $reason\n$customPersona",
                memory = memoryText,
                operationHistory = proactiveHistoryText(),
                triggerApps = triggerApps,
                currentPackage = currentPackage,
                maxChars = maxChars
            )
            val response = FallbackAIClient(preferences).complete(request)
            fitProactiveLength(response.text.ifBlank { localFallback }.withCompanionName(name), maxChars, name, user, appName, screenText)
        } catch (_: Exception) {
            localFallback
        }
    }

    private fun fitProactiveLength(message: String, maxChars: Int, name: String, user: String, appName: String, screenText: String): String {
        if (maxChars <= 0 || message.length <= maxChars) return message
        val prefix = "$name\uff1a"
        val hint = screenText.lineSequence().map { it.trim() }.firstOrNull { it.length >= 4 }?.take(12)
        val candidates = listOf(
            if (hint != null) "$prefix$user，看见「$hint」，先抓这个线索。" else "$prefix$user，画面文字不足，我先不乱判断。",
            if (hint != null) "$prefix「$hint」像是当前重点。" else "${prefix}当前文字不足，先安静观察。"
        )
        return candidates.firstOrNull { it.length <= maxChars } ?: prefix.take(maxChars.coerceAtLeast(1))
    }

    private suspend fun generateManualReply(userText: String): String {
        val context = SharedScreenContext.visibleText.take(MAX_SCREEN_PROMPT_CHARS)
        val customPersona = preferences.customPersonaPrompt.first()
        val memoryText = recentMemoryText()
        val name = companionName()
        val user = userName()
        val fallback = "$name\uff1a\u8fd8\u6ca1\u6709\u914d\u7f6e\u53ef\u7528\u7684 AI API\u3002\u8bf7\u5728\u8bbe\u7f6e\u91cc\u6dfb\u52a0\u81f3\u5c11\u4e00\u4e2a\u542f\u7528\u7684 API \u914d\u7f6e\uff0c\u5e76\u586b\u5199 Base URL\u3001API Key \u548c\u6a21\u578b\u540d\u79f0\u3002"
        if (preferences.resolvedApiProfiles().none { it.isUsable() }) return fallback
        return try {
            val searchResults = SearchClient(preferences).searchIfNeeded(userText)
            val searchContext = searchResults.mapIndexed { index, result -> result.asPromptLine(index + 1) }.joinToString("\n\n")
            val request = promptEngine.buildLongConversationPrompt(
                userText = userText,
                screenText = context.ifBlank { "屏幕文字为空或暂不可读。" },
                customPersona = "AI name: $name\nUser name: $user\n$customPersona",
                memory = memoryText.take(MAX_MEMORY_PROMPT_CHARS),
                conversation = conversationText(),
                maxChars = preferences.companionReplyMaxChars.first(),
                searchContext = searchContext
            )
            FallbackAIClient(preferences).complete(request).text.ifBlank { fallback }.withCompanionName(name)
        } catch (_: Exception) {
            "$name\uff1aAI \u8bf7\u6c42\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5 API \u5730\u5740\u3001Key\u3001\u6a21\u578b\u540d\u79f0\u548c\u7f51\u7edc\u8fde\u63a5\u3002"
        }
    }

    private suspend fun recentMemoryText(): String = try {
        database.dao().longTermMemories().first().take(10).joinToString("\n") { memory ->
            "${memory.title}: ${memory.summary}"
        }
    } catch (_: Exception) {
        ""
    }

    private fun rememberCompanionLine(message: String) {
        scope.launch(Dispatchers.IO) {
            database.dao().saveMemory(
                LongTermMemory(
                    title = "Companion line",
                    summary = message.take(500),
                    sourceApp = SharedScreenContext.packageName,
                    importance = 1
                )
            )
        }
    }

    private fun speak(message: String) {
        appendConversationLine(message, remember = true)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun appendConversationLine(message: String, remember: Boolean) {
        conversationLines += message
        if (remember) rememberCompanionLine(message)
        persistConversationLine(message, remember)
        if (conversationLines.size > 40) conversationLines.removeAt(0)
        refreshConversationMessages()
    }

    private fun persistConversationLine(message: String, remember: Boolean) {
        if (!remember || message.isBlank() || message.endsWith("：正在思考...")) return
        val role = when {
            message.startsWith("${userName()}：") -> "user"
            message.startsWith("${companionName()}：") -> "assistant"
            else -> "system"
        }
        val content = message.substringAfter('：', message).trim().ifBlank { message.trim() }
        if (currentSessionTopic == "Ocean Companion" && role == "user") {
            currentSessionTopic = content.take(18).ifBlank { currentSessionTopic }
        }
        scope.launch(Dispatchers.IO) {
            database.dao().insertConversation(
                ConversationHistory(
                    sessionId = currentSessionId,
                    topic = currentSessionTopic,
                    role = role,
                    content = content
                )
            )
        }
    }

    private fun removeThinkingLine() {
        conversationLines.removeAll { it.endsWith("\uff1a\u6b63\u5728\u601d\u8003...") }
        refreshConversationMessages()
    }

    private fun refreshConversationMessages() {
        val list = companionPanel?.findViewWithTag<LinearLayout>(MESSAGE_LIST_TAG) ?: return
        val scroll = companionPanel?.findViewWithTag<ScrollView>(SCROLL_VIEW_TAG)
        renderConversationMessages(list, scroll, currentCompanionTheme)
    }

    private fun renderConversationMessages(list: LinearLayout, scroll: ScrollView?, theme: CompanionTheme) {
        list.removeAllViews()
        if (conversationLines.isEmpty()) {
            list.addView(emptyConversationHint(theme))
            return
        }
        val lines = conversationLines.toList()
        lines.forEach { line -> list.addView(messageBubble(line, theme)) }
        scroll?.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun emptyConversationHint(theme: CompanionTheme): View {
        val text = TextView(this).apply {
            this.text = "\u6682\u65e0\u5bf9\u8bdd\u3002\u4e3b\u52a8\u5f39\u5e55\u663e\u793a\u671f\u95f4\u6253\u5f00\u9762\u677f\u65f6\uff0c\u624d\u4f1a\u5c06\u90a3\u53e5\u8bdd\u5e26\u5165\u3002"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(theme.textSecondary)
            setPadding(22, 30, 22, 30)
            background = GradientDrawable().apply {
                setColor(theme.softOverlay)
                cornerRadius = 22f
                setStroke(1, theme.stroke)
            }
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(4, 18, 4, 18)
            addView(text, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
        }
    }

    private fun messageBubble(message: String, theme: CompanionTheme): View {
        val isUser = message.startsWith("${userName()}\uff1a")
        val isThinking = message.endsWith("\uff1a\u6b63\u5728\u601d\u8003...")
        val text = TextView(this).apply {
            this.text = if (isUser || isThinking) message else renderBasicMarkdown(message, theme)
            textSize = if (isThinking) 13f else 15f
            maxWidth = if (isPortrait()) {
                (resources.displayMetrics.widthPixels * 0.78f).toInt()
            } else {
                (resources.displayMetrics.widthPixels * 0.42f).toInt()
            }
            setTextColor(
                when {
                    isUser -> Color.WHITE
                    isThinking -> theme.textSecondary
                    else -> theme.textPrimary
                }
            )
            setPadding(18, 14, 18, 14)
            setLineSpacing(2f, 1.08f)
            background = GradientDrawable().apply {
                cornerRadius = 22f
                if (isUser) {
                    setColor(theme.primary)
                    setStroke(1, Color.argb(90, 255, 255, 255))
                } else if (isThinking) {
                    setColor(theme.softOverlay)
                    setStroke(1, theme.stroke)
                } else {
                    setColor(theme.messageBackground)
                    setStroke(1, theme.stroke)
                }
            }
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (isUser) Gravity.END else Gravity.START
            setPadding(2, 6, 2, 6)
            addView(text, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
        }
        return row
    }

    private fun renderBasicMarkdown(markdown: String, theme: CompanionTheme): CharSequence {
        val normalized = markdown.replace("\r\n", "\n")
        val builder = SpannableStringBuilder()
        var inCodeBlock = false
        normalized.lines().forEachIndexed { index, rawLine ->
            val line = rawLine.trimEnd()
            if (line.trim().startsWith("```")) {
                inCodeBlock = !inCodeBlock
                return@forEachIndexed
            }
            if (index > 0 && builder.isNotEmpty()) builder.append('\n')
            val start = builder.length
            val displayLine = when {
                inCodeBlock -> line
                line.startsWith("### ") -> line.removePrefix("### ")
                line.startsWith("## ") -> line.removePrefix("## ")
                line.startsWith("# ") -> line.removePrefix("# ")
                line.trimStart().startsWith("- ") -> "• ${line.trimStart().removePrefix("- ")}"
                line.trimStart().matches(Regex("\\d+\\.\\s+.*")) -> line.trimStart()
                else -> line
            }
            builder.append(displayLine)
            val end = builder.length
            when {
                inCodeBlock -> {
                    builder.setSpan(TypefaceSpan("monospace"), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(ForegroundColorSpan(theme.textSecondary), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                line.startsWith("# ") -> {
                    builder.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(RelativeSizeSpan(1.18f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                line.startsWith("## ") || line.startsWith("### ") -> {
                    builder.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(RelativeSizeSpan(1.08f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }
        applyInlineMarkdown(builder, theme)
        return builder
    }

    private fun applyInlineMarkdown(builder: SpannableStringBuilder, theme: CompanionTheme) {
        applyPairedMarker(builder, "**") { start, end ->
            builder.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        applyPairedMarker(builder, "*") { start, end ->
            builder.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        applyPairedMarker(builder, "`") { start, end ->
            builder.setSpan(TypefaceSpan("monospace"), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.setSpan(ForegroundColorSpan(theme.primary), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun applyPairedMarker(
        builder: SpannableStringBuilder,
        marker: String,
        applySpan: (Int, Int) -> Unit
    ) {
        var searchFrom = 0
        while (searchFrom < builder.length) {
            val open = builder.indexOf(marker, searchFrom)
            if (open < 0) break
            val close = builder.indexOf(marker, open + marker.length)
            if (close < 0) break
            builder.delete(close, close + marker.length)
            builder.delete(open, open + marker.length)
            applySpan(open, close - marker.length)
            searchFrom = close - marker.length
        }
    }

    private fun conversationText(): String {
        if (conversationLines.isEmpty()) return "No previous long conversation."
        return conversationLines
            .filterNot { it.endsWith("\uff1a\u6b63\u5728\u601d\u8003...") }
            .takeLast(MAX_PROMPT_MESSAGES)
            .joinToString("\n\n")
    }

    private fun proactiveHistoryText(): String {
        return conversationLines.takeLast(8).joinToString("\n").ifBlank { "No recent conversation." }
    }

    private fun companionName(): String = currentCompanionName.ifBlank { "Ocean" }

    private fun userName(): String = currentUserName.ifBlank { "\u4f60" }

    private fun String.withCompanionName(name: String): String {
        val trimmed = trim()
        if (trimmed.contains('\uff1a') || trimmed.contains(':')) return trimmed
        return "$name\uff1a$trimmed"
    }

    private data class CompanionTheme(
        val primary: Int,
        val accent: Int,
        val primarySoft: Int,
        val surfaceStart: Int,
        val surfaceMid: Int,
        val surfaceEnd: Int,
        val glassPanel: Int,
        val inputBackground: Int,
        val messageBackground: Int,
        val softOverlay: Int,
        val stroke: Int,
        val textPrimary: Int,
        val textSecondary: Int
    ) {
        companion object {
            fun default(): CompanionTheme {
                return fromPrimary(Color.rgb(20, 135, 180))
            }

            fun fromPrimary(primary: Int): CompanionTheme {
                return fromPalette(primary, Color.rgb(
                    (Color.blue(primary) * 0.55f + 82).toInt().coerceIn(0, 255),
                    (Color.red(primary) * 0.45f + 92).toInt().coerceIn(0, 255),
                    (Color.green(primary) * 0.65f + 104).toInt().coerceIn(0, 255)
                ))
            }

            fun fromPalette(primary: Int, accentColor: Int): CompanionTheme {
                val accent = Color.rgb(
                    Color.red(accentColor),
                    Color.green(accentColor),
                    Color.blue(accentColor)
                )
                val darkBase = Color.rgb(13, 19, 30)
                val lightBase = Color.rgb(248, 252, 255)
                val p = primary
                return CompanionTheme(
                    primary = p,
                    accent = accent,
                    primarySoft = withStaticAlpha(p, 175),
                    surfaceStart = withStaticAlpha(blendStatic(darkBase, p, 0.42f), 236),
                    surfaceMid = withStaticAlpha(blendStatic(darkBase, accent, 0.38f), 224),
                    surfaceEnd = withStaticAlpha(blendStatic(lightBase, p, 0.30f), 214),
                    glassPanel = withStaticAlpha(blendStatic(lightBase, p, 0.18f), 112),
                    inputBackground = withStaticAlpha(lightBase, 184),
                    messageBackground = withStaticAlpha(lightBase, 176),
                    softOverlay = withStaticAlpha(lightBase, 94),
                    stroke = withStaticAlpha(Color.WHITE, 158),
                    textPrimary = Color.rgb(18, 28, 42),
                    textSecondary = Color.rgb(78, 91, 108)
                )
            }

            private fun blendStatic(from: Int, to: Int, ratio: Float): Int {
                val inverse = 1f - ratio
                return Color.rgb(
                    (Color.red(from) * inverse + Color.red(to) * ratio).toInt().coerceIn(0, 255),
                    (Color.green(from) * inverse + Color.green(to) * ratio).toInt().coerceIn(0, 255),
                    (Color.blue(from) * inverse + Color.blue(to) * ratio).toInt().coerceIn(0, 255)
                )
            }

            private fun withStaticAlpha(color: Int, alpha: Int): Int {
                return Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
            }
        }
    }

    companion object {
        const val MESSAGE_LIST_TAG = "ocean_message_list"
        const val SCROLL_VIEW_TAG = "ocean_message_scroll"
        const val ACTIVE_APP_CHECK_INTERVAL_MS = 3_000L
        const val IMMEDIATE_APP_COOLDOWN_MS = 45_000L
        const val GLOBAL_PROACTIVE_COOLDOWN_MS = 20_000L
        const val PROACTIVE_BANNER_DURATION_MS = 8_000L
        const val SCREEN_CONTEXT_FRESH_MS = 12_000L
        const val USAGE_STATS_LOOKBACK_MS = 30_000L
        const val MAX_VISIBLE_MESSAGES = 24
        const val MAX_PROMPT_MESSAGES = 10
        const val MAX_SCREEN_PROMPT_CHARS = 3_500
        const val MAX_MEMORY_PROMPT_CHARS = 1_200
        const val ACTION_FILE_CONTEXT_READY = "com.projectocean.oceancompanion.action.FILE_CONTEXT_READY"
        const val ACTION_SCREEN_ANALYSIS_READY = "com.projectocean.oceancompanion.action.SCREEN_ANALYSIS_READY"
        const val EXTRA_FILE_TITLE = "file_title"
        const val EXTRA_FILE_READABLE = "file_readable"
        const val EXTRA_FILE_PREVIEW = "file_preview"
        const val EXTRA_SCREEN_ANALYSIS = "screen_analysis"
    }
}

