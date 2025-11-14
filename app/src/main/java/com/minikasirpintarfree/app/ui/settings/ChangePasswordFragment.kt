package com.minikasirpintarfree.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.minikasirpintarfree.app.R
import com.minikasirpintarfree.app.databinding.FragmentChangePasswordBinding

class ChangePasswordFragment : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            // TODO: Implement password change logic here
            // 1. Get old, new, and confirmation passwords from EditTexts
            // 2. Validate inputs (e.g., not empty, new password matches confirmation)
            // 3. Call ViewModel to perform the password change
            // 4. Show feedback to the user (success or error message)
            // 5. Navigate back if successful
        }
    }
}
