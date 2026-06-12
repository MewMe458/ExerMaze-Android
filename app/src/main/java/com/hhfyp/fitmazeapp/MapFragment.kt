package com.hhfyp.fitmazeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

class MapFragment : Fragment() {

    private lateinit var map: MapView
    private var lastPickedCoordinate: GeoPoint? = null
    private var currentMarker: Marker? = null

    private lateinit var saveButton: Button
    private lateinit var clearButton: Button

    private val sharedViewModel: SharedViewModel by activityViewModels()

    companion object {
        // Converted LatLng to OSMdroid GeoPoint
        private val DEFAULT_COORDINATE = GeoPoint(2.924897, 101.641824)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Crucial for OSMdroid to load tiles properly using device cache
        Configuration.getInstance().load(
            requireContext(),
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        )

        // Setting a User Agent is REQUIRED by OSMdroid to prevent tile server blocking
        Configuration.getInstance().userAgentValue = requireContext().packageName

        val view = inflater.inflate(R.layout.fragment_map, container, false)

        // Initialize views
        map = view.findViewById(R.id.map)
        saveButton = view.findViewById(R.id.btn_save_coordinate)
        clearButton = view.findViewById(R.id.btn_clear_picker)

        // Hide buttons initially
        saveButton.visibility = View.GONE
        clearButton.visibility = View.GONE

        // Set up map configuration
        map.setMultiTouchControls(true) // Enable pinch-to-zoom
        val mapController = map.controller
        mapController.setZoom(10.0)

        val initialLocation = lastPickedCoordinate ?: DEFAULT_COORDINATE
        mapController.setCenter(initialLocation)
        addMarker(initialLocation, "Default Location")

        // Click listeners
        saveButton.setOnClickListener {
            lastPickedCoordinate?.let { coordinate ->
                // Fix: No more Google Maps conversion! Pass the OSMdroid GeoPoint directly
                sharedViewModel.updateCoordinate(coordinate)
                sharedViewModel.triggerCoordinateUpdate() // cite: 15

                saveButton.visibility = View.GONE // cite: 16
                clearButton.visibility = View.GONE // cite: 16
            }
        }

        clearButton.setOnClickListener {
            removeCurrentMarker()
            lastPickedCoordinate = null
            saveButton.visibility = View.GONE
            clearButton.visibility = View.GONE
        }

        // Setting up map click interaction (OSMdroid uses MapEventsOverlay)
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                val formattedLat = formatCoordinate(p.latitude).toDouble()
                val formattedLng = formatCoordinate(p.longitude).toDouble()
                val fixedPoint = GeoPoint(formattedLat, formattedLng)

                lastPickedCoordinate = fixedPoint

                // Remove the previous marker (including the default one) before placing a new one
                removeCurrentMarker()
                addMarker(fixedPoint, "${formattedLat}, ${formattedLng}")

                saveButton.visibility = View.VISIBLE
                clearButton.visibility = View.VISIBLE
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                return false
            }
        }

        val mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)
        map.overlays.add(mapEventsOverlay)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainFragActivity)?.hideSystemBars()
    }

    private fun addMarker(geoPoint: GeoPoint, title: String) {
        currentMarker = Marker(map).apply {
            position = geoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            this.title = title
        }
        map.overlays.add(currentMarker)
        map.invalidate() // Refresh map graphics
    }

    private fun removeCurrentMarker() {
        currentMarker?.let {
            map.overlays.remove(it)
            currentMarker = null
            map.invalidate()
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainFragActivity)?.hideSystemBars()
        map.onResume() // Critical for OSMdroid data parsing

        lastPickedCoordinate?.let {
            map.controller.setZoom(15.0)
            map.controller.setCenter(it)
        }
    }

    override fun onPause() {
        super.onPause()
        map.onPause() // Critical for OSMdroid thread handling
    }

    private fun formatCoordinate(value: Double): String {
        return String.format("%.6f", value)
    }
}