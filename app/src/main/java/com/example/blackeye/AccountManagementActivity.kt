package com.example.blackeye

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.java_websocket.client.WebSocketClient

class AccountManagementActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_management)

        val tvUserInfo: TextView = findViewById(R.id.tv_userInfo)
        val btnLogout: Button = findViewById(R.id.btn_logout)
        val btnReturn: Button = findViewById(R.id.btn_return)

        val userInfo = getCurrentUserInfo()
        tvUserInfo.text = "UserName: ${userInfo.username}\nPassword: ${userInfo.password}"

        btnLogout.setOnClickListener {
            logoutUser() // 实现用户登出逻辑
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        btnReturn.setOnClickListener {
            finish() // 直接关闭当前Activity返回上一界面
        }
    }

    private fun getCurrentUserInfo(): UserInfo {
        val sharedPref = this.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val username = sharedPref.getString("username", "") ?: ""
        val password = sharedPref.getString("password", "") ?: ""
        return UserInfo(username, password)
    }


    private fun logoutUser() {
        // 清除SharedPreferences中的用户信息
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()

        //断开WebSocket连接
        WebSocketClientManager.close()

        // 返回登录界面
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }



}

