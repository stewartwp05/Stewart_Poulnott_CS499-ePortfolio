package com.stewartpoulnott.weighttracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var username: String
    private var userId: Int = -1

    private lateinit var btnBack: Button
    private lateinit var btnEnableSMS: Button
    private lateinit var tvPermissionStatus: TextView
    private lateinit var switchGoal: SwitchCompat
    private lateinit var switchWeekly: SwitchCompat
    private lateinit var switchMilestone: SwitchCompat
    private lateinit var etPhoneNumber: EditText
    private lateinit var btnSavePhone: Button

    private val SMS_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        dbHelper = DatabaseHelper(this)
        username = intent.getStringExtra("username") ?: ""
        userId = dbHelper.getUserId(username)

        initViews()
        setupListeners()
        updatePermissionStatus()
        loadUserPhone()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnEnableSMS = findViewById(R.id.btnEnableSMS)
        tvPermissionStatus = findViewById(R.id.tvPermissionStatus)
        switchGoal = findViewById(R.id.switchGoal)
        switchWeekly = findViewById(R.id.switchWeekly)
        switchMilestone = findViewById(R.id.switchMilestone)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        btnSavePhone = findViewById(R.id.btnSavePhone)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnEnableSMS.setOnClickListener {
            handleSMSPermissionToggle()
        }

        btnSavePhone.setOnClickListener {
            savePhoneNumber()
        }
    }

    private fun loadUserPhone() {
        val phoneNumber = dbHelper.getUserPhone(userId)
        if (phoneNumber.isNotEmpty()) {
            etPhoneNumber.setText(phoneNumber)
        }
    }

    private fun savePhoneNumber() {
        val phoneNumber = etPhoneNumber.text.toString().trim()

        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show()
            return
        }

        if (phoneNumber.length < 10) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
            return
        }

        if (dbHelper.updateUserPhone(userId, phoneNumber)) {
            Toast.makeText(this, "Phone number saved successfully!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to save phone number", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleSMSPermissionToggle() {
        val isGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

        if (isGranted) {
            openAppSettings()
        } else {
            requestSMSPermission()
        }
    }

    private fun requestSMSPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.SEND_SMS),
            SMS_PERMISSION_REQUEST_CODE
        )
    }

    private fun openAppSettings() {
        Toast.makeText(this, "To disable SMS notifications, turn off SMS permission in app settings", Toast.LENGTH_LONG).show()

        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open app settings", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS notifications enabled! You'll receive goal achievement alerts.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "SMS notifications disabled. The app will continue to work normally.", Toast.LENGTH_LONG).show()
            }
            updatePermissionStatus()
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun updatePermissionStatus() {
        val isGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

        if (isGranted) {
            tvPermissionStatus.text = "✅ Enabled - You'll receive goal notifications!"
            tvPermissionStatus.setTextColor(ContextCompat.getColor(this, R.color.primary_green))
            btnEnableSMS.text = "Disable SMS Notifications"
            btnEnableSMS.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
        } else {
            tvPermissionStatus.text = "❌ Disabled - No SMS notifications will be sent"
            tvPermissionStatus.setTextColor(ContextCompat.getColor(this, R.color.red))
            btnEnableSMS.text = "Enable SMS Notifications"
            btnEnableSMS.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_green))
        }
    }
}