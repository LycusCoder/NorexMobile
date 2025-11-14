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
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
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
    private lateinit var appBarConfiguration: AppBarConfiguration
    private var notificationBadge: TextView? = null
    private lateinit var notificationsViewModel: NotificationsViewModel
    private val sharedViewModel: SharedViewModel by viewModels()
    private var notificationMenuItem: MenuItem? = null
    private var transactionHistoryMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (!sharedPreferences.getBoolean("is_logged_in", false)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setSupportActionBar(binding.toolbar)

            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navController = navHostFragment.navController

            val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)
            val fabNewTransaksi: FloatingActionButton = findViewById(R.id.fab_new_transaksi)
            val bottomAppBar: BottomAppBar = findViewById(R.id.bottom_app_bar)

            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.dashboardFragment,
                    R.id.produkFragment,
                    R.id.laporanFragment,
                    R.id.settingsFragment
                )
            )

            setupActionBarWithNavController(navController, appBarConfiguration)
            NavigationUI.setupWithNavController(bottomNavigation, navController)

            fabNewTransaksi.setOnClickListener {
                navController.navigate(R.id.transaksiFragment)
            }

            navController.addOnDestinationChangedListener { _, destination, _ ->
                val isTopLevelDestination = appBarConfiguration.topLevelDestinations.contains(destination.id)

                if (isTopLevelDestination) {
                    bottomAppBar.visibility = View.VISIBLE
                    fabNewTransaksi.visibility = View.VISIBLE
                } else {
                    bottomAppBar.visibility = View.GONE
                    fabNewTransaksi.visibility = View.GONE
                }

                when (destination.id) {
                    R.id.transaksiFragment -> {
                        notificationMenuItem?.isVisible = false
                        transactionHistoryMenuItem?.isVisible = true
                    }
                    R.id.notificationsFragment -> {
                        notificationMenuItem?.isVisible = false
                        transactionHistoryMenuItem?.isVisible = false
                    }
                    else -> {
                        notificationMenuItem?.isVisible = true
                        transactionHistoryMenuItem?.isVisible = false
                    }
                }
            }

            val database = AppDatabase.getDatabase(this)
            val notifikasiRepository = NotifikasiRepository(database.notifikasiDao())
            notificationsViewModel = ViewModelProvider(this, NotificationsViewModelFactory(notifikasiRepository))[NotificationsViewModel::class.java]
            observeNotificationCount()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in onCreate", e)
            android.widget.Toast.makeText(this, "Terjadi kesalahan: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun observeNotificationCount() {
        lifecycleScope.launch {
            notificationsViewModel.notifikasiList.collect { list ->
                val unreadCount = list.count { !it.isRead }
                updateNotificationBadge(unreadCount)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_dashboard, menu)
        notificationMenuItem = menu.findItem(R.id.menu_notifications)
        transactionHistoryMenuItem = menu.findItem(R.id.menu_transaction_history)
        
        val actionView = notificationMenuItem?.actionView
        actionView?.setOnClickListener {
            navController.navigate(R.id.notificationsFragment)
        }
        notificationBadge = actionView?.findViewById(R.id.notification_badge)
        
        // Initial visibility setup
        val currentId = navController.currentDestination?.id
        notificationMenuItem?.isVisible = currentId != R.id.notificationsFragment && currentId != R.id.transaksiFragment
        transactionHistoryMenuItem?.isVisible = currentId == R.id.transaksiFragment
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_transaction_history -> {
                navController.navigate(R.id.transactionHistoryFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
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
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }
}