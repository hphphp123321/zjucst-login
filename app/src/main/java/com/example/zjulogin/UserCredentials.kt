package com.example.zjulogin

import android.content.Context

object UserCredentials {
    private const val PREFS_NAME = "user_credentials"
    private const val PREFS_KEY_USERNAME = "user_username"
    private const val PREFS_KEY_PASSWORD = "user_password"

    var username: String = ""
    var password: String = "zjusct"

    fun saveCredentials(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(PREFS_KEY_USERNAME, username)
        editor.putString(PREFS_KEY_PASSWORD, password)
        editor.apply()
    }

    fun loadCredentials(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        username = prefs.getString(PREFS_KEY_USERNAME, "") ?: ""
        password = prefs.getString(PREFS_KEY_PASSWORD, "zjusct") ?: "zjusct"
    }
}


