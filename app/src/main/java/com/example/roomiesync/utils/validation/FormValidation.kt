package com.example.roomiesync.utils.validation

import android.util.Patterns

object FormValidation {
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPassword(password: String): Boolean {
        // Example: minimum 6 characters
        return password.length >= 6
    }
    
    fun isValidDisplayName(displayName: String): Boolean {
        return displayName.isNotBlank() && displayName.length <= 50
    }
}
