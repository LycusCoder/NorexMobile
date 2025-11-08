package com.minikasirpintarfree.app

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.TypedValue
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.minikasirpintarfree.app.databinding.ActivitySplashBinding
import com.minikasirpintarfree.app.ui.login.LoginActivity
import com.minikasirpintarfree.app.viewmodel.LoginViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val splashDuration = 2000L // 2 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        applyThemeColors()
        setupVersionInfo()
        startAnimations()

        lifecycleScope.launch {
            delay(splashDuration)
            navigateToNextScreen()
        }
    }

    private fun applyThemeColors() {
        val typedValue = TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
        val colorPrimary = typedValue.data
        binding.splashBackground.setBackgroundColor(colorPrimary)
    }

    private fun setupVersionInfo() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            binding.tvVersion.text = getString(R.string.version_format, versionName)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            binding.tvVersion.text = getString(R.string.version_format, "1.2.0") // Fallback
        }
    }

    private fun startAnimations() {
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        binding.ivLogo.startAnimation(fadeIn)
        binding.tvAppName.startAnimation(fadeIn)
        binding.tvSubtitle.startAnimation(fadeIn)
    }

    private fun navigateToNextScreen() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val isLoggedIn = sharedPreferences.getBoolean(LoginViewModel.PREF_IS_LOGGED_IN, false)

        val intent = if (isLoggedIn) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }

        startActivity(intent)
        finish()
    }
}