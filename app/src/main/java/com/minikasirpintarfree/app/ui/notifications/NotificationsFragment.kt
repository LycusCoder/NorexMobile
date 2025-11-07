package com.minikasirpintarfree.app.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.minikasirpintarfree.app.R
import com.minikasirpintarfree.app.data.database.AppDatabase
import com.minikasirpintarfree.app.data.repository.NotifikasiRepository
import com.minikasirpintarfree.app.databinding.FragmentNotificationsBinding
import com.minikasirpintarfree.app.viewmodel.NotificationsViewModel
import com.minikasirpintarfree.app.viewmodel.NotificationsViewModelFactory
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {
    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: NotificationsViewModel
    private lateinit var adapter: NotifikasiAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val database = AppDatabase.getDatabase(requireContext())
        val notifikasiRepository = NotifikasiRepository(database.notifikasiDao())
        viewModel = ViewModelProvider(
            this,
            NotificationsViewModelFactory(notifikasiRepository)
        )[NotificationsViewModel::class.java]
        
        setupMenu()
        setupRecyclerView()
        observeViewModel()
    }
    
    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_notifications, menu)
            }
            
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_mark_all_read -> {
                        lifecycleScope.launch {
                            viewModel.markAllAsRead()
                        }
                        true
                    }
                    R.id.menu_clear_all -> {
                        lifecycleScope.launch {
                            viewModel.clearAll()
                        }
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
    
    private fun setupRecyclerView() {
        adapter = NotifikasiAdapter(
            onItemClick = { notifikasi ->
                lifecycleScope.launch {
                    viewModel.markAsRead(notifikasi.id)
                }
            }
        )
        binding.recyclerViewNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewNotifications.adapter = adapter
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.notifikasiList.collect { list ->
                adapter.submitList(list)
                binding.tvEmptyNotifications.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerViewNotifications.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
