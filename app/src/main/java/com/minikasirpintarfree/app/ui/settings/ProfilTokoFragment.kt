package com.minikasirpintarfree.app.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.minikasirpintarfree.app.databinding.FragmentProfilTokoBinding
import com.minikasirpintarfree.app.utils.StoreProfile
import com.minikasirpintarfree.app.utils.StoreProfileHelper

class ProfilTokoFragment : Fragment() {

    private var _binding: FragmentProfilTokoBinding? = null
    private val binding get() = _binding!!

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

        loadStoreProfile()
        loadNotificationSettings()

        binding.btnSave.setOnClickListener {
            saveStoreProfile()
            saveNotificationSettings()
            Toast.makeText(requireContext(), "Pengaturan berhasil disimpan", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
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
    }
}
