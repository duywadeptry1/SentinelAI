package com.duy.sentinelai

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnOverlay: Button
    private lateinit var btnAccessibility: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Ánh xạ View từ giao diện
        tvStatus = findViewById(R.id.tvStatus)
        btnOverlay = findViewById(R.id.btnOverlayPermission)
        btnAccessibility = findViewById(R.id.btnAccessibilityPermission)

        // Bấm nút 1: Xin quyền vẽ đè màn hình
        btnOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }

        // Bấm nút 2: Chuyển vào cài đặt Trợ năng (Accessibility)
        btnAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Lưu ý: Nếu đã Bật, hãy Tắt đi rồi Bật lại để làm mới!", Toast.LENGTH_LONG).show()
        }

        // Nút hỗ trợ Oppo/Realme: Tự động khởi chạy
        findViewById<Button>(R.id.btnAutoStart)?.setOnClickListener {
            try {
                val intent = Intent()
                intent.component = ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")
                startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent()
                    intent.component = ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")
                    startActivity(intent)
                } catch (e2: Exception) {
                    Toast.makeText(this, "Hãy tìm mục 'Tự động khởi chạy' trong Cài đặt", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 🔥 Vòng đời: Tự động chạy lại mỗi khi ông từ màn hình Cài đặt quay về App
    override fun onResume() {
        super.onResume()
        checkPermissionsAndUpdateUI()
    }

    private fun checkPermissionsAndUpdateUI() {
        val hasOverlay = Settings.canDrawOverlays(this)
        val hasAccessibility = isAccessibilityEnabled(this, SentinelService::class.java)

        // 1. Cập nhật giao diện nút Vẽ đè
        if (hasOverlay) {
            btnOverlay.isEnabled = false
            btnOverlay.text = "✅ Đã cấp quyền Vẽ đè"
        } else {
            btnOverlay.isEnabled = true
            btnOverlay.text = "1. Cấp quyền Hiển thị cảnh báo"
        }

        // 2. Cập nhật giao diện nút Trợ năng
        if (hasAccessibility) {
            btnAccessibility.isEnabled = false
            btnAccessibility.text = "✅ Đã bật Lõi phân tích"
        } else {
            btnAccessibility.isEnabled = true
            btnAccessibility.text = "2. Bật Lõi phân tích (Trợ năng)"
        }

        // 3. Cập nhật Trạng thái Tổng: Bắt buộc phải có CẢ 2 quyền mới bật khiên
        if (hasOverlay && hasAccessibility) {
            tvStatus.text = "ĐANG BẢO VỆ"
            tvStatus.setTextColor(Color.parseColor("#388E3C")) // Màu xanh lá
        } else {
            tvStatus.text = "CHƯA KÍCH HOẠT"
            tvStatus.setTextColor(Color.parseColor("#D32F2F")) // Màu đỏ
        }
    }

    // 🧠 Hàm kiểm tra xem Quyền Trợ Năng đã thực sự được bật chưa (Dùng cho mọi dòng máy, kể cả ColorOS)
    private fun isAccessibilityEnabled(context: Context, service: Class<*>): Boolean {
        val expectedComponentName = ComponentName(context, service)
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)

        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledComponent = ComponentName.unflattenFromString(componentNameString)
            if (enabledComponent != null && enabledComponent == expectedComponentName) {
                return true
            }
        }
        return false
    }
}