package com.example.blackeye

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.blackeye.network.LoginRequest
import com.example.blackeye.network.LoginResponse
import com.example.blackeye.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class LoginActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var togglePasswordVisibilityButton: ImageButton
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private var originalDrawable: Drawable? = null

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


        usernameEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.password)
        originalDrawable = usernameEditText.background;

        usernameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                usernameEditText.background = originalDrawable


            }
        })

        passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                passwordEditText.background = originalDrawable
            }
        })

        togglePasswordVisibilityButton = findViewById(R.id.togglePasswordVisibility)
        loginButton = findViewById(R.id.loginButton)
        registerButton = findViewById(R.id.registerButton)

        togglePasswordVisibilityButton.setOnClickListener {
            if (isPasswordVisible) {
                passwordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
                togglePasswordVisibilityButton.setImageResource(R.drawable.ic_visibility_off)
            } else {
                passwordEditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                togglePasswordVisibilityButton.setImageResource(R.drawable.ic_visibility)
            }
            isPasswordVisible = !isPasswordVisible
            passwordEditText.setSelection(passwordEditText.text.length) // 确保光标在末尾
        }

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()
            // 调用服务器登录接口
            login(username, password)
        }

        registerButton.setOnClickListener {
            // 切换到注册页面
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun login(username: String, password: String) {
        var isValid = true
        if (TextUtils.isEmpty(username)) {
            usernameEditText.setBackgroundResource(R.drawable.empty_text_error)
            Toast.makeText(this, "Please enter username", Toast.LENGTH_SHORT).show()
            isValid = false
        } else {
            usernameEditText.background = originalDrawable // 恢复默认背景
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setBackgroundResource(R.drawable.empty_text_error)
            Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show()
            isValid = false
        } else {
            passwordEditText.background = originalDrawable // 恢复默认背景
        }

        if (!isValid) return

        val apiService = RetrofitClient.apiService
        val loginRequest = LoginRequest(username, password)
        apiService.login(loginRequest).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val token = response.body()?.token
                    if (token != null) {
                        saveUserInfo(username, password)
                        // Save the token and proceed to the video activity
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        intent.putExtra("TOKEN", token)
                        WebSocketClientManager.getInstance(token)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Login failed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "Login failed: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "An error occurred: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    fun saveUserInfo(username: String, password: String) {
        val sharedPref = this.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putString("username", username)
            putString("password", password)
            apply()
        }
    }
}



