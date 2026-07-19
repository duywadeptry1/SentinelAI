package com.duy.sentinelai

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

@SuppressLint("AccessibilityService")
class SentinelService : AccessibilityService() {

    private lateinit var windowManager: WindowManager
    private var overlayView: android.view.View? = null
    private var isOverlayShowing = false

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = OkHttpClient()

    private var lastAnalyzedText: String = ""
    private var lastAnalyzedTime: Long = 0L
    private var lastScanTime: Long = 0

    private var suppressAlertUntil = 0L


    private val BACKEND_URL = "http://192.168.0.101:8000/api/v1/analyze"

    // Bộ nhớ đệm cho từ khóa - Khởi tạo với các giá trị mặc định
    private var dynamicKeywordScores = mutableMapOf<String, Int>(
        "chuyển tiền" to 40, "otp" to 50, "trúng thưởng" to 60,
        "công an" to 50, "mật khẩu" to 40, "tài khoản" to 30, "nạp" to 40, "đăng nhập" to 40,
        "chuyển gấp" to 50, "viện phí" to 50, "stk" to 40, "bị khóa" to 50,
        "mạo danh" to 50, "lừa đảo" to 50, "triệu" to 20, "mẹ" to 10, "con" to 10
    )

    private fun startForegroundService() {
        try {
            val channelId = "sentinel_service_channel"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, "Sentinel Security Service", NotificationManager.IMPORTANCE_LOW)
                val manager = getSystemService(NotificationManager::class.java)
                manager?.createNotificationChannel(channel)
            }

            val notification: Notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("Sentinel AI Đang Bảo Vệ")
                .setContentText("Hệ thống đang quét các mối đe dọa lừa đảo...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build()

            startForeground(1, notification)
            Log.d("SentinelAI", "[LIFECYCLE] Foreground Service started")
        } catch (e: Exception) {
            Log.e("SentinelAI", "[ERROR] startForeground: ${e.message}")
        }
    }

    private fun loadKeywordsFromAssets() {
        try {
            assets.open("default_keywords.csv").bufferedReader().useLines { lines ->
                lines.drop(1).forEach { line ->
                    val parts = line.split(",")
                    if (parts.size == 2) {
                        dynamicKeywordScores[parts[0].trim()] = parts[1].trim().toInt()
                    }
                }
            }
            Log.i("SentinelAI", "[DATA] Loaded ${dynamicKeywordScores.size} keywords from Assets")
        } catch (e: Exception) {
            Log.e("SentinelAI", "[ERROR] loadKeywordsFromAssets: ${e.message}")
        }
    }

    private fun syncKeywordsFromServer() {
        serviceScope.launch {
            try {
                val request = Request.Builder()
                    .url("http://192.168.0.101:8000/api/v1/sync/keywords")
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val json = JSONObject(response.body?.string() ?: "{}")
                        val updated = json.optJSONArray("updated_keywords")
                        if (updated != null) {
                            for (i in 0 until updated.length()) {
                                val item = updated.getJSONObject(i)
                                dynamicKeywordScores[item.getString("keyword")] = item.getInt("score")
                            }
                            Log.i("SentinelAI", "[DATA] Synced ${updated.length()} keywords from Server")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SentinelAI", "[ERROR] syncKeywordsFromServer: ${e.message}")
            }
        }
    }

    private fun calculateRiskScore(text: String): Int {
        var totalScore = 0
        val lowerText = text.lowercase()
        Log.d("SentinelAI", "--- Analyzing Risk Score ---")
        for ((keyword, score) in dynamicKeywordScores) {
            if (lowerText.contains(keyword.lowercase())) {
                totalScore += score
                Log.d("SentinelAI", "  [MATCH] '$keyword' -> +$score")
            }
        }
        Log.d("SentinelAI", "--- Total Score: $totalScore ---")
        return totalScore
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("SentinelAI", "[LIFECYCLE] Service Created")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            Log.d("SentinelAI", "[LIFECYCLE] Service Connected")
            
            loadKeywordsFromAssets()
            syncKeywordsFromServer()
            startForegroundService()
            
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(this, "Sentinel AI: Đã kết nối bảo vệ!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("SentinelAI", "[ERROR] onServiceConnected: ${e.message}")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        // ⚡ TRÁNH VÒNG LẶP: Không quét chính nội dung của ứng dụng Sentinel AI
        val packageName = event.packageName?.toString() ?: ""
        if (packageName == "com.duy.sentinelai") return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastScanTime < 2000) return
        
        val eventType = event.eventType
        if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || 
            eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
            eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            
            lastScanTime = currentTime
            
            val node = rootInActiveWindow ?: event.source
            if (node == null) return
            
            val screenText = extractTextFromNode(node, 0).trim()
            node.recycle()
            
            if (screenText.isNotEmpty()) {
                Log.d("SentinelAI", "[SCAN] Reading screen...")
                handleSuspectContent(screenText, currentTime)
            }
        }
    }

    private fun handleSuspectContent(textToAnalyze: String, currentTime: Long) {

        // Người dùng vừa bấm "Bỏ qua" -> chưa được hiện cảnh báo lại
        if (currentTime < suppressAlertUntil) {
            Log.d("SentinelAI", "[ALERT] Suppressed for ${suppressAlertUntil - currentTime} ms")
            return
        }

        // Chuẩn hóa nội dung trước khi so sánh
        val normalizedText = textToAnalyze
            .trim()
            .replace("\\s+".toRegex(), " ")

        // Nếu cùng nội dung và mới phân tích gần đây thì bỏ qua
        if (normalizedText == lastAnalyzedText &&
            currentTime - lastAnalyzedTime < 30_000) {
            return
        }

        // Cập nhật lịch sử phân tích
        lastAnalyzedText = normalizedText
        lastAnalyzedTime = currentTime

        val score = calculateRiskScore(normalizedText)

        if (score < 60) return

        Log.w("SentinelAI", "[ALERT] TRIGGERED! Score: $score")

        showWarningOverlay(
            "PHÁT HIỆN DẤU HIỆU LỪA ĐẢO!",
            "Hệ thống nhận diện nội dung này có rủi ro cao ($score điểm).",
            "Tuyệt đối không làm theo yêu cầu. Hãy xác thực lại thông tin.",
            normalizedText
        )

        serviceScope.launch {
            try {
                val jsonObject = JSONObject().apply {
                    put("content", normalizedText)
                    put("score", score)
                    put("device_hash", "oppo_device")
                }

                val request = Request.Builder()
                    .url(BACKEND_URL)
                    .post(
                        jsonObject.toString()
                            .toRequestBody("application/json".toMediaType())
                    )
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        val jsonResponse = JSONObject(body ?: "{}")

                        if (jsonResponse.optBoolean("is_risk", false)) {
                            val meta = jsonResponse.optJSONObject("display_meta")

                            updateOverlayWithServerData(
                                meta?.optString("title"),
                                meta?.optString("reason"),
                                meta?.optString("recommendation")
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SentinelAI", "[ERROR] Server API: ${e.message}")
            }
        }
    }

    private fun updateOverlayWithServerData(title: String?, reason: String?, rec: String?) {
        Handler(Looper.getMainLooper()).post {
            overlayView?.let { view ->
                title?.let { view.findViewById<TextView>(R.id.tvAlertTitle)?.text = it }
                reason?.let { view.findViewById<TextView>(R.id.tvAlertReason)?.text = it }
                rec?.let { view.findViewById<TextView>(R.id.tvRecommendation)?.text = it }
            }
        }
    }

    private fun showWarningOverlay(title: String, reason: String, recommendation: String, content: String) {
        if (!::windowManager.isInitialized) return
        
        // Luôn gửi notification dự phòng
        sendImmediateNotification(title, reason)

        Handler(Looper.getMainLooper()).post {
            if (isOverlayShowing) {
                Log.d("SentinelAI", "[UI] Overlay already showing, skipping addView")
                return@post
            }
            
            try {
                val layoutParams = WindowManager.LayoutParams().apply {
                    width = WindowManager.LayoutParams.MATCH_PARENT
                    height = WindowManager.LayoutParams.MATCH_PARENT
                    type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                    flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_DIM_BEHIND
                    dimAmount = 0.7f
                    format = PixelFormat.TRANSLUCENT
                    gravity = Gravity.CENTER
                }

                val view = LayoutInflater.from(this).inflate(R.layout.layout_alert_card, null)
                overlayView = view

                view.findViewById<TextView>(R.id.tvAlertTitle)?.text = title
                view.findViewById<TextView>(R.id.tvAlertReason)?.text = reason
                view.findViewById<TextView>(R.id.tvRecommendation)?.text = recommendation

                view.findViewById<ImageButton>(R.id.btnClose)?.setOnClickListener {
                    suppressAlertUntil = System.currentTimeMillis() + 30_000
                    removeWarningOverlay()
                }

                view.findViewById<Button>(R.id.btnSafeBack)?.setOnClickListener {
                    suppressAlertUntil = System.currentTimeMillis() + 30_000
                    removeWarningOverlay()
                }

                view.findViewById<Button>(R.id.btnReport)?.setOnClickListener {
                    reportToBackend(content)
                    removeWarningOverlay()
                }

                windowManager.addView(view, layoutParams)
                isOverlayShowing = true
                Log.d("SentinelAI", "[UI] ✅ Warning Overlay ADDED to WindowManager")
            } catch (e: Exception) {
                Log.e("SentinelAI", "[ERROR] WindowManager Add: ${e.message}")
            }
        }
    }

    private fun sendImmediateNotification(title: String, message: String) {
        val channelId = "sentinel_alert_channel"
        val manager = getSystemService(NotificationManager::class.java) ?: return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Cảnh báo lừa đảo", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            manager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            Log.e("SentinelAI", "[ERROR] Notification: ${e.message}")
        }
    }

    private fun removeWarningOverlay() {
        if (isOverlayShowing) {
            overlayView?.let {
                try {
                    windowManager.removeView(it)
                    Log.d("SentinelAI", "[UI] ❌ Alert Overlay REMOVED")
                } catch (e: Exception) {
                    Log.e("SentinelAI", "[ERROR] removeView: ${e.message}")
                }
            }
            overlayView = null
            isOverlayShowing = false
        }
    }

    private fun reportToBackend(content: String) {
        serviceScope.launch {
            try {
                val jsonObject = JSONObject().apply {
                    put("content", content)
                    put("report_type", "false_positive")
                }
                val request = Request.Builder()
                    .url("http://192.168.0.101:8000/api/v1/report")
                    .post(jsonObject.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(request).execute()
            } catch (e: Exception) { }
        }
    }

    private fun extractTextFromNode(node: AccessibilityNodeInfo?, depth: Int = 0): String {
        if (node == null || depth > 50) return ""
        val textBuilder = StringBuilder()
        if (node.text != null) textBuilder.append(node.text).append(" ")
        else if (node.contentDescription != null) textBuilder.append(node.contentDescription).append(" ")
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                textBuilder.append(extractTextFromNode(child, depth + 1))
                child.recycle()
            }
        }
        return textBuilder.toString()
    }

    override fun onInterrupt() {}
    override fun onDestroy() { 
        super.onDestroy()
        removeWarningOverlay()
        serviceScope.cancel() 
    }
}