package com.minikasirpintarfree.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.minikasirpintarfree.app.databinding.ActivitySplashBinding
import com.minikasirpintarfree.app.ui.login.LoginActivity
import com.minikasirpintarfree.app.viewmodel.LoginViewModel

class SplashActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySplashBinding
    private val SPLASH_DURATION = 1500L // 1.5 seconds
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Hide action bar
        supportActionBar?.hide()
        
        // Apply fade-in animation to logo
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        binding.ivLogo.startAnimation(fadeIn)
        binding.tvAppName.startAnimation(fadeIn)
        binding.tvSubtitle.startAnimation(fadeIn)
        
        // Navigate after delay
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, SPLASH_DURATION)
    }
    
    private fun navigateToNextScreen() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val isLoggedIn = sharedPreferences.getBoolean(LoginViewModel.PREF_IS_LOGGED_IN, false)
        
        // Check if user is logged in
        val intent = if (isLoggedIn) {
            // User is logged in, go to MainActivity
            Intent(this, MainActivity::class.java)
        } else {
            // User is not logged in, go to LoginActivity
            Intent(this, LoginActivity::class.java)
        }
        
        startActivity(intent)
        finish()
        
        // Apply fade transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
