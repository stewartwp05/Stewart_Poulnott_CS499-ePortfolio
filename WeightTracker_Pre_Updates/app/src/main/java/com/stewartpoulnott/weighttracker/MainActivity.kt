package com.stewartpoulnott.weighttracker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)
        setupViews()
    }

    private fun setupViews() {
        val loginForm = findViewById<LinearLayout>(R.id.loginForm)
        val registerForm = findViewById<LinearLayout>(R.id.registerForm)
        val btnLoginTab = findViewById<Button>(R.id.btnLoginTab)
        val btnRegisterTab = findViewById<Button>(R.id.btnRegisterTab)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        val etRegUsername = findViewById<EditText>(R.id.etRegUsername)
        val etRegPassword = findViewById<EditText>(R.id.etRegPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnLoginTab.setOnClickListener {
            loginForm.visibility = View.VISIBLE
            registerForm.visibility = View.GONE
            btnLoginTab.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_green))
            btnLoginTab.setTextColor(ContextCompat.getColor(this, R.color.white))
            btnRegisterTab.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
            btnRegisterTab.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }

        btnRegisterTab.setOnClickListener {
            loginForm.visibility = View.GONE
            registerForm.visibility = View.VISIBLE
            btnRegisterTab.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_green))
            btnRegisterTab.setTextColor(ContextCompat.getColor(this, R.color.white))
            btnLoginTab.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
            btnLoginTab.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (dbHelper.checkUser(username, password)) {
                val intent = Intent(this, DashboardActivity::class.java)
                intent.putExtra("username", username)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
            }
        }

        btnRegister.setOnClickListener {
            val username = etRegUsername.text.toString().trim()
            val password = etRegPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (dbHelper.addUser(username, password)) {
                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()

                loginForm.visibility = View.VISIBLE
                registerForm.visibility = View.GONE
                etUsername.setText(username)
                etPassword.setText(password)

                btnLoginTab.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_green))
                btnLoginTab.setTextColor(ContextCompat.getColor(this, R.color.white))
                btnRegisterTab.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
                btnRegisterTab.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            } else {
                Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show()
            }
        }
    }
}