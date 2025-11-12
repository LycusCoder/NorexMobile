package com.minikasirpintarfree.app.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.minikasirpintarfree.app.R
import com.minikasirpintarfree.app.databinding.FragmentProfilTokoBinding
import com.minikasirpintarfree.app.utils.StoreProfile
import com.minikasirpintarfree.app.utils.StoreProfileHelper

class ProfilTokoFragment : Fragment() {

    private var _binding: FragmentProfilTokoBinding? = null
    private val binding get() = _binding!!

    private var toolbar: Toolbar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilTokoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide bottom navigation
        activity?.findViewById<BottomAppBar>(R.id.bottom_app_bar)?.visibility = View.GONE
        activity?.findViewById<FloatingActionButton>(R.id.fab_new_transaksi)?.visibility = View.GONE

        setupToolbar()
        loadStoreProfile()
        loadNotificationSettings()

        binding.btnSave.setOnClickListener {
            saveStoreProfile()
            saveNotificationSettings()
            Toast.makeText(requireContext(), "Pengaturan berhasil disimpan", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    private fun setupToolbar() {
        toolbar = activity?.findViewById(R.id.toolbar)
        toolbar?.apply {
            title = "Profil Toko"
            setNavigationIcon(R.drawable.ic_arrow_back)
            setNavigationOnClickListener {
                findNavController().navigateUp()
            }
        }
    }

    private fun loadStoreProfile() {
        val profile = StoreProfileHelper.getStoreProfile(requireContext())
        binding.etStoreName.setText(profile.name)
        binding.etStoreAddress.setText(profile.address)
        binding.etStorePhone.setText(profile.phone)
    }

    private fun saveStoreProfile() {
        val name = binding.etStoreName.text.toString().trim()
        val address = binding.etStoreAddress.text.toString().trim()
        val phone = binding.etStorePhone.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Nama toko tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        val newProfile = StoreProfile(name, address, phone)
        StoreProfileHelper.saveStoreProfile(requireContext(), newProfile)
    }

    private fun loadNotificationSettings() {
        val sharedPreferences = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val isEnabled = sharedPreferences.getBoolean("daily_stock_notification", true) // Default to true
        binding.switchNotifikasiStok.isChecked = isEnabled
    }

    private fun saveNotificationSettings() {
        val sharedPreferences = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean("daily_stock_notification", binding.switchNotifikasiStok.isChecked)
            apply()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        // Restore bottom navigation and toolbar
        activity?.findViewById<BottomAppBar>(R.id.bottom_app_bar)?.visibility = View.VISIBLE
        activity?.findViewById<FloatingActionButton>(R.id.fab_new_transaksi)?.visibility = View.VISIBLE

        toolbar?.apply {
            title = getString(R.string.app_name)
            navigationIcon = null
            setNavigationOnClickListener(null)
        }
    }
}
