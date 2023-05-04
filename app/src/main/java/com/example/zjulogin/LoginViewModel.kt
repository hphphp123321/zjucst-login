package com.example.zjulogin

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

import android.util.Log
import java.util.concurrent.TimeUnit

sealed class WifiInfo {
    object NotConnected : WifiInfo()
    object ConnectedNotCaptivePortal : WifiInfo()
    object ConnectedCaptivePortalNotSchool : WifiInfo()
    object ConnectedCaptivePortalNotLoggedIn : WifiInfo()
    object ConnectedCaptivePortalLoggedIn : WifiInfo()
}


class LoginViewModel() : ViewModel() {

    val loginStatus = mutableStateOf("检查中...")
    val isUsernamePasswordInputEnabled = MutableLiveData(true)
    val isLoginButtonEnabled = MutableLiveData(true)
    val isLogoutButtonEnabled = MutableLiveData(false)

    private suspend fun login(context: Context, network: Network): Boolean
    {
        if (UserCredentials.username.isEmpty()) {
            loginStatus.value = "请输入用户名"
            return false
        }
        val formBody = FormBody.Builder()
            .add("username", UserCredentials.username)
            .add("password", UserCredentials.password)
            .build()
        val request = Request.Builder()
            .url("http://192.0.0.6/cgi-bin/do_login")
            .post(formBody)
            .build()

        Log.d("loginInfo", formBody.value(1))

        val client = OkHttpClient.Builder()
            .socketFactory(NetworkBoundSocketFactory(network))
            .build()
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        Log.d("loginInfo", response.toString())
        withContext(Dispatchers.Main) {
            Toast.makeText(context, response.toString(), Toast.LENGTH_SHORT).show()
        }
        delay(500)
        return response.isSuccessful
    }



    private suspend fun logout(context: Context, network: Network): Boolean {
        val formBody = FormBody.Builder()
            .add("username", UserCredentials.username)
            .add("password", UserCredentials.password)
            .build()

        val request = Request.Builder()
            .url("http://192.0.0.6/cgi-bin/force_logout")
            .post(formBody)
            .build()

        val client = OkHttpClient.Builder()
            .socketFactory(NetworkBoundSocketFactory(network))
            .build()
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        withContext(Dispatchers.Main) {
            Toast.makeText(context, response.message, Toast.LENGTH_SHORT).show()
        }
        Log.d("logout", response.code.toString())
        delay(500)
        return response.isSuccessful
    }

    fun performLogin(context: Context) {
        viewModelScope.launch {
            val network = getPortalWifi(context)
            if (network == null) {
                Toast.makeText(context, "请连接校园网！", Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (login(context, network)) {
                Toast.makeText(context, "登录成功！", Toast.LENGTH_SHORT).show()
                isUsernamePasswordInputEnabled.value = false
                isLoginButtonEnabled.value = false
                isLogoutButtonEnabled.value = true

                // Save credentials after successful login
                UserCredentials.saveCredentials(context)
            } else {
                Toast.makeText(context, "登录失败！", Toast.LENGTH_SHORT).show()
            }

        }
    }

    fun performLogout(context: Context) {
        viewModelScope.launch {
            val network = getPortalWifi(context)
            if (network == null) {
                Toast.makeText(context, "请连接校园网！", Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (logout(context, network)) {
                Toast.makeText(context, "登出成功！", Toast.LENGTH_SHORT).show()
                isUsernamePasswordInputEnabled.value = true
                isLoginButtonEnabled.value = true
                isLogoutButtonEnabled.value = false

                // Save credentials after successful logout
                UserCredentials.saveCredentials(context)

            } else {
                Toast.makeText(context, "登出失败！", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private suspend fun canAccessUrl(url: String, network: Network): Boolean {
        val client = OkHttpClient.Builder()
            .socketFactory(NetworkBoundSocketFactory(network))
            .connectTimeout(500, TimeUnit.MILLISECONDS) // 设置连接超时时间
            .readTimeout(500, TimeUnit.MILLISECONDS) // 设置读取超时时间
            .writeTimeout(500, TimeUnit.MILLISECONDS) // 设置写入超时时间
            .build()
        val request = Request.Builder().url(url).build()

        return try {
            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }
            response.isSuccessful
        } catch (e: Exception) {
//            Log.e("canAccessUrl", "Exception occurred", e)
            false
        }
    }


    private suspend fun getWifiInfo(context: Context): WifiInfo {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val allNetworks = connectivityManager.allNetworks

        for (network in allNetworks) {
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: continue

            val isWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val isCaptivePortal = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)

            if (isWifi) {
                return if (isCaptivePortal) {
                    if (!canAccessUrl("https://www.baidu.com", network) && canAccessUrl("http://192.0.0.6", network)) {
                        WifiInfo.ConnectedCaptivePortalNotLoggedIn
                    } else {
                        WifiInfo.ConnectedCaptivePortalNotSchool
                    }
                } else {
                    if (canAccessUrl("http://192.0.0.6", network)) {
                        WifiInfo.ConnectedCaptivePortalLoggedIn
                    } else {
                        WifiInfo.ConnectedNotCaptivePortal
                    }
                }
            }
        }
        return WifiInfo.NotConnected
    }

    private suspend fun getPortalWifi(context: Context): Network? {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val allNetworks = connectivityManager.allNetworks

        for (network in allNetworks) {
            if (canAccessUrl("http://192.0.0.6", network)) {
                return network
            }
        }
        return null
    }


    fun checkNetworkStatus(context: Context) {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val wifiInfo = getWifiInfo(context)
                when (wifiInfo) {
                    is WifiInfo.NotConnected -> {
                        loginStatus.value = "未连接 WiFi"
                    }
                    is WifiInfo.ConnectedNotCaptivePortal -> {
                        loginStatus.value = "已连接 WiFi，但不是校园网"
                    }
                    is WifiInfo.ConnectedCaptivePortalNotSchool -> {
                        loginStatus.value = "连接到需要登录的 WiFi，但不是校园网"
                    }
                    is WifiInfo.ConnectedCaptivePortalNotLoggedIn -> {
                        loginStatus.value = "已连接校园网，但未登录"
                    }
                    is WifiInfo.ConnectedCaptivePortalLoggedIn -> {
                        loginStatus.value = "已连接校园网且已登录"
                    }

                }
                isUsernamePasswordInputEnabled.value = wifiInfo == WifiInfo.ConnectedCaptivePortalNotLoggedIn || wifiInfo == WifiInfo.ConnectedCaptivePortalLoggedIn
                isLoginButtonEnabled.value = wifiInfo == WifiInfo.ConnectedCaptivePortalNotLoggedIn
                isLogoutButtonEnabled.value = wifiInfo == WifiInfo.ConnectedCaptivePortalLoggedIn
            }
        }
    }
}
