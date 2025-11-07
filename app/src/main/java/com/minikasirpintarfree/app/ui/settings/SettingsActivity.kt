package com.minikasirpintarfree.app.ui.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.minikasirpintarfree.app.R
import com.minikasirpintarfree.app.data.database.AppDatabase
import com.minikasirpintarfree.app.data.repository.ProdukRepository
import com.minikasirpintarfree.app.data.repository.TransaksiRepository
import com.minikasirpintarfree.app.databinding.ActivitySettingsBinding
import com.minikasirpintarfree.app.ui.login.LoginActivity
import com.minikasirpintarfree.app.ui.notifications.NotificationsActivity
import com.minikasirpintarfree.app.viewmodel.SettingsViewModel
import com.minikasirpintarfree.app.viewmodel.SettingsViewModelFactory
import com.minikasirpintarfree.app.viewmodel.LoginViewModel
import com.minikasirpintarfree.app.viewmodel.LoginViewModelFactory

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var viewModel: SettingsViewModel
    private lateinit var loginViewModel: LoginViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivitySettingsBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            val database = AppDatabase.getDatabase(this)
            val produkRepository = ProdukRepository(database.produkDao())
            val transaksiRepository = TransaksiRepository(database.transaksiDao())
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            
            viewModel = ViewModelProvider(this, SettingsViewModelFactory(produkRepository, transaksiRepository, sharedPreferences))[SettingsViewModel::class.java]
            
            // Initialize LoginViewModel untuk fungsi change password
            loginViewModel = ViewModelProvider(this, LoginViewModelFactory(sharedPreferences))[LoginViewModel::class.java]
            
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            
            setupClickListeners()
            loadCurrentSettings()
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "Error in onCreate", e)
            Toast.makeText(this, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun setupClickListeners() {
        binding.cardTheme.setOnClickListener {
            showThemeDialog()
        }
        
        binding.cardChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }
        
        binding.cardResetProduk.setOnClickListener {
            showResetDialog("Produk") {
                viewModel.resetDataProduk {
                    Toast.makeText(this, "Data produk telah direset", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        binding.cardResetTransaksi.setOnClickListener {
            showResetDialog("Transaksi") {
                viewModel.resetDataTransaksi {
                    Toast.makeText(this, "Data transaksi telah direset", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        binding.cardResetAll.setOnClickListener {
            showResetDialog("Semua Data") {
                viewModel.resetAllData {
                    Toast.makeText(this, "Semua data telah direset", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        binding.cardLogout.setOnClickListener {
            logout()
        }
    }
    
    private fun loadCurrentSettings() {
        // Theme feature disabled - always show as off
        binding.switchTheme.isChecked = false
        binding.switchTheme.isEnabled = false
    }
    
    private fun showThemeDialog() {
        AlertDialog.Builder(this)
            .setTitle("Tema Aplikasi")
            .setMessage("Fitur tema aplikasi sedang dalam pengembangan dan akan segera hadir di versi berikutnya.")
            .setPositiveButton("OK", null)
            .setIcon(android.R.drawable.ic_dialog_info)
            .show()
    }
    
    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val etOldPassword = dialogView.findViewById<android.widget.EditText>(R.id.etOldPassword)
        val etNewPassword = dialogView.findViewById<android.widget.EditText>(R.id.etNewPassword)
        val etConfirmPassword = dialogView.findViewById<android.widget.EditText>(R.id.etConfirmPassword)
        
        AlertDialog.Builder(this)
            .setTitle("Ubah Password")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val oldPassword = etOldPassword.text.toString()
                val newPassword = etNewPassword.text.toString()
                val confirmPassword = etConfirmPassword.text.toString()
                
                // Validasi input
                if (oldPassword.isEmpty()) {
                    Toast.makeText(this, "Password lama harus diisi", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                if (newPassword.isEmpty()) {
                    Toast.makeText(this, "Password baru harus diisi", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                if (newPassword != confirmPassword) {
                    Toast.makeText(this, "Password baru tidak cocok", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                if (newPassword.length < 6) {
                    Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                // Panggil LoginViewModel untuk change password
                val success = loginViewModel.changePassword(oldPassword, newPassword)
                
                if (success) {
                    Toast.makeText(this, "Password berhasil diubah", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Password lama salah", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
    
    private fun showResetDialog(dataType: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Reset Data")
            .setMessage("Apakah Anda yakin ingin menghapus semua data $dataType? Tindakan ini tidak dapat dibatalkan.")
            .setPositiveButton("Ya, Hapus") { _, _ ->
                onConfirm()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
    
    private fun logout() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Apakah Anda yakin ingin logout?")
            .setPositiveButton("Ya") { _, _ ->
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
                sharedPreferences.edit().putBoolean("is_logged_in", false).apply()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .setNegativeButton("Tidak", null)
            .show()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.menu_notifications -> {
                startActivity(Intent(this, NotificationsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
}


