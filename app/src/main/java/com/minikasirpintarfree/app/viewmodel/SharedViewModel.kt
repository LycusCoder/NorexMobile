package com.minikasirpintarfree.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {

    private val _startScanEvent = MutableLiveData<Event<Unit>>()
    val startScanEvent: LiveData<Event<Unit>> = _startScanEvent

    fun triggerStartScan() {
        _startScanEvent.value = Event(Unit)
    }
}

// Event wrapper to handle one-off events
open class Event<out T>(private val content: T) {

    var hasBeenHandled = false
        private set // Allow external read but not write

    /**
     * Returns the content and prevents its use again.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /**
     * Returns the content, even if it's already been handled.
     */
    fun peekContent(): T = content
}