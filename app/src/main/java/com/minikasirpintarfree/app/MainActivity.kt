package com.minikasirpintarfree.app

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.preference.PreferenceManager
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.minikasirpintarfree.app.data.database.AppDatabase
import com.minikasirpintarfree.app.data.repository.NotifikasiRepository
import com.minikasirpintarfree.app.databinding.ActivityMainBinding
import com.minikasirpintarfree.app.ui.login.LoginActivity
import com.minikasirpintarfree.app.utils.ThemeHelper
import com.minikasirpintarfree.app.viewmodel.NotificationsViewModel
import com.minikasirpintarfree.app.viewmodel.NotificationsViewModelFactory
import com.minikasirpintarfree.app.viewmodel.SharedViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var notificationBadge: TextView? = null
    private lateinit var notificationsViewModel: NotificationsViewModel
    private val sharedViewModel: SharedViewModel by viewModels()
    private var notificationMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before super.onCreate
        ThemeHelper.applyTheme(this)

        super.onCreate(savedInstanceState)

        // Check login status first
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false)

        if (!isLoggedIn) {
            // Not logged in, redirect to login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // User is logged in, setup main activity
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setSupportActionBar(binding.toolbar)

            // Setup Navigation Component
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navController = navHostFragment.navController

            // Because the included layout uses <merge>, we must find the views manually.
            val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)
            val fabNewTransaksi: FloatingActionButton = findViewById(R.id.fab_new_transaksi)
            val bottomAppBar: BottomAppBar = findViewById(R.id.bottom_app_bar)

            // Wire up BottomNavigationView with NavController
            NavigationUI.setupWithNavController(bottomNavigation, navController)

            fabNewTransaksi.setOnClickListener {
                sharedViewModel.triggerStartScan()
                navController.navigate(R.id.transaksiFragment)
            }

            navController.addOnDestinationChangedListener { _, destination, _ ->
                val isBottomBarVisible = when (destination.id) {
                    R.id.notificationsFragment -> false
                    else -> true
                }
                bottomAppBar.visibility = if (isBottomBarVisible) View.VISIBLE else View.GONE
                fabNewTransaksi.visibility = if (isBottomBarVisible) View.VISIBLE else View.GONE

                notificationMenuItem?.isVisible = destination.id != R.id.notificationsFragment
            }

            // Initialize ViewModel
            val database = AppDatabase.getDatabase(this)
            val notifikasiRepository = NotifikasiRepository(database.notifikasiDao())
            notificationsViewModel = ViewModelProvider(
                this,
                NotificationsViewModelFactory(notifikasiRepository)
            )[NotificationsViewModel::class.java]

            observeNotificationCount()

        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in onCreate", e)
            android.widget.Toast.makeText(
                this,
                "Terjadi kesalahan: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_dashboard, menu)
        notificationMenuItem = menu.findItem(R.id.menu_notifications)
        val actionView = notificationMenuItem?.actionView
        actionView?.setOnClickListener {
            navController.navigate(R.id.notificationsFragment)
        }
        notificationBadge = actionView?.findViewById(R.id.notification_badge)
        // Hide on create if we are already on the notifications screen
        notificationMenuItem?.isVisible = navController.currentDestination?.id != R.id.notificationsFragment
        return true
    }

    private fun observeNotificationCount() {
        lifecycleScope.launch {
            notificationsViewModel.notifikasiList.collect { list ->
                val unreadCount = list.count { !it.isRead }
                updateNotificationBadge(unreadCount)
            }
        }
    }

    fun updateNotificationBadge(count: Int) {
        if (count > 0) {
            notificationBadge?.visibility = View.VISIBLE
            notificationBadge?.text = count.toString()
        } else {
            notificationBadge?.visibility = View.GONE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
