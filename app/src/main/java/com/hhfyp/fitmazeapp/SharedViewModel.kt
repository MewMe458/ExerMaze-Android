package com.hhfyp.fitmazeapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.osmdroid.util.GeoPoint // Changed from Google LatLng to OSMdroid GeoPoint

class SharedViewModel : ViewModel() {

    private val _coordinateUpdateTrigger = MutableLiveData<Boolean>()
    val coordinateUpdateTrigger: LiveData<Boolean> get() = _coordinateUpdateTrigger

    // Use OSMdroid GeoPoint to store the coordinate
    private var pickedCoordinate: GeoPoint? = null

    fun updateCoordinate(coordinate: GeoPoint) {
        pickedCoordinate = coordinate
    }

    fun getCoordinate(): GeoPoint? {
        return pickedCoordinate
    }

    fun triggerCoordinateUpdate() {
        _coordinateUpdateTrigger.value = true
    }

    fun resetTrigger() {
        _coordinateUpdateTrigger.value = false
    }
}