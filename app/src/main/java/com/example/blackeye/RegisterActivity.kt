package com.example.blackeye

import android.content.Context
import android.os.Bundle
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.blackeye.network.RetrofitClient
import com.example.blackeye.network.ApiService
import com.example.blackeye.network.LoginRequest
import com.example.blackeye.network.LoginResponse
import com.example.blackeye.network.RegisterRequest
import com.example.blackeye.network.RegisterResponse

class RegisterActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var secretEditText: EditText
    private lateinit var togglePasswordVisibilityButton: ImageButton
    private lateinit var toggleSecretVisibilityButton: ImageButton
    private lateinit var registerButton: Button

    private var isPasswordVisible = false
    private var isSecretVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        usernameEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.password)
        secretEditText = findViewById(R.id.secret)
        togglePasswordVisibilityButton = findViewById(R.id.togglePasswordVisibility)
        toggleSecretVisibilityButton = findViewById(R.id.toggleSecretVisibility)
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

        toggleSecretVisibilityButton.setOnClickListener {
            if (isSecretVisible) {
                secretEditText.transformationMethod = PasswordTransformationMethod.getInstance()
                toggleSecretVisibilityButton.setImageResource(R.drawable.ic_visibility_off)
            } else {
                secretEditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                toggleSecretVisibilityButton.setImageResource(R.drawable.ic_visibility)
            }
            isSecretVisible = !isSecretVisible
            secretEditText.setSelection(secretEditText.text.length) // 确保光标在末尾
        }

        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()
            val secret = secretEditText.text.toString()
            // 调用服务器注册接口
            register(username, password, secret)
        }
    }

    private fun register(username: String, password: String, secret: String) {

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || TextUtils.isEmpty(secret)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val apiService = RetrofitClient.apiService
        val registerRequest = RegisterRequest(username, password, secret)

        // Log the request data
        Log.d("RegisterUser", "Request: $registerRequest")

        apiService.register(registerRequest).enqueue(object : Callback<RegisterResponse> {
            override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@RegisterActivity, "Registration successful", Toast.LENGTH_SHORT).show()
                    // Automatically login after registration
                    loginUserAfterRegister(username, password)
                } else {
                    Toast.makeText(this@RegisterActivity, "Registration failed: ${response.message()}", Toast.LENGTH_SHORT).show()
                    // Log the error response
                    Log.e("RegisterUser", "Error response: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                Toast.makeText(this@RegisterActivity, "An error occurred: ${t.message}", Toast.LENGTH_SHORT).show()
                // Log the failure message
                Log.e("RegisterUser", "Failure: ${t.message}")
            }
        })
    }

    private fun loginUserAfterRegister(username: String, password: String){
        val apiService = RetrofitClient.apiService
        val loginRequest = LoginRequest(username, password)
        apiService.login(loginRequest).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val token = response.body()?.token
                    if (token != null) {
                        // Save the token and proceed to the video activity
                        saveUserInfo(username, password)
                        val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                        intent.putExtra("TOKEN", token)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@RegisterActivity, "Login after registration failed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@RegisterActivity, "Login after registration failed: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(this@RegisterActivity, "An error occurred during login after registration: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })

    }

    private fun saveUserInfo(username: String, password: String) {
        val sharedPref = this.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putString("username", username)
            putString("password", password)
            apply()
        }
    }
}
