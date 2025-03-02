package com.ahr.solartime

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import java.util.*

/**
 * Main activity for the Solar Time application.
 *
 * This activity displays solar time calculations based on the user's selected location
 * on a Google Map. It shows the difference between standard time and solar time,
 * as well as sunrise and sunset times for the selected location.
 */
class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    // Map and Places API
    private lateinit var map: GoogleMap
    private lateinit var placesClient: PlacesClient

    // UI elements
    private lateinit var solarTimeTextView: TextView
    private lateinit var currentTimeTextView: TextView
    private lateinit var latitudeLongitudeTextView: TextView
    private lateinit var sunriseTextView: TextView
    private lateinit var sunsetTextView: TextView
    private lateinit var recyclerViewPredictions: RecyclerView
    private lateinit var predictionAdapter: PredictionAdapter

    // Handlers for different tasks
    private val timeUpdateHandler = Handler(Looper.getMainLooper())
    private val searchHandler = Handler(Looper.getMainLooper())
    private lateinit var timeUpdateRunnable: Runnable

    // Add currentLocation variable
    private var currentLocation: LatLng? = null
    private var selectedPrecisionLevel = SolarTimeUtil.PRECISION_ULTRA

    /**
     * Called when the activity is first created.
     * Initializes the UI, map, Places API, and location services.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set up the toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Solar Time"

        // Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(
                applicationContext,
                "AIzaSyDLcq3aNTfKAud-ZAi-Ajqg6Wp3-aNzaI4"
            )
        }
        placesClient = Places.createClient(this)

        // Initialize views and components
        initializeViews()
        initializeMap()
        startDynamicTimeUpdates()
        checkAndEnableLocation()
    }

    /**
     * Initializes all view elements and sets up the predictions RecyclerView.
     */
    private fun initializeViews() {
        solarTimeTextView = findViewById(R.id.solarTimeTextView)
        currentTimeTextView = findViewById(R.id.currentTimeTextView)
        latitudeLongitudeTextView = findViewById(R.id.latitudeLongitudeTextView)
        sunriseTextView = findViewById(R.id.sunriseTextView)
        sunsetTextView = findViewById(R.id.sunsetTextView)

        // Set up the predictions RecyclerView
        recyclerViewPredictions = findViewById(R.id.recyclerViewPredictions)
        recyclerViewPredictions.layoutManager = LinearLayoutManager(this)
        predictionAdapter = PredictionAdapter(emptyList()) { prediction ->
            fetchPlaceDetails(prediction.placeId)
            recyclerViewPredictions.visibility = View.GONE // Hide after selection
        }
        recyclerViewPredictions.adapter = predictionAdapter

        // Setup info icon click listener
        val infoIcon = findViewById<ImageView>(R.id.infoIcon)
        infoIcon.setOnClickListener {
            showSolarTimeInfoDialog()
        }
    }

    /**
     * Initializes the Google Map fragment.
     */
    private fun initializeMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Fetches autocomplete predictions for the given query.
     *
     * @param query The search query text
     */
    private fun fetchAutocompletePredictions(query: String) {
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
            val predictions = response.autocompletePredictions
            if (predictions.isNotEmpty()) {
                // Update the adapter with the new predictions
                predictionAdapter.updatePredictionsList(predictions.toList())
                recyclerViewPredictions.visibility = View.VISIBLE
            } else {
                recyclerViewPredictions.visibility = View.GONE
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Searches for a location based on the given query and updates the map.
     * Uses the first prediction from the autocomplete API.
     *
     * @param query The search query text
     */
    private fun searchLocation(query: String) {
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
            if (response.autocompletePredictions.isNotEmpty()) {
                val prediction = response.autocompletePredictions[0]
                val placeId = prediction.placeId
                fetchPlaceDetails(placeId)
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Fetches detailed place information for a given placeId.
     * Updates the map with the retrieved location.
     *
     * @param placeId The Google Place ID for the location
     */
    private fun fetchPlaceDetails(placeId: String) {
        val placeFields = listOf(Place.Field.LAT_LNG, Place.Field.NAME)
        val request = FetchPlaceRequest.builder(placeId, placeFields).build()

        placesClient.fetchPlace(request).addOnSuccessListener { response ->
            val place = response.place
            val latLng = place.location
            val name = place.displayName
            if (latLng != null) {
                updateMapWithLocation(latLng, name ?: "Selected Location")
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Updates the map with a marker at the specified location and animates the camera to it.
     *
     * @param latLng The latitude and longitude of the location
     * @param title The title to display for the marker
     */
    private fun updateMapWithLocation(latLng: LatLng, title: String) {
        map.clear()
        map.addMarker(MarkerOptions().position(latLng).title(title))
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }

    /**
     * Called when the map is ready to be used.
     * Sets up map UI and location settings.
     *
     * @param googleMap The GoogleMap object that is ready
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
            map.uiSettings.isCompassEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }

        val initialLocation = LatLng(0.0, 0.0)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, 2f))
        map.uiSettings.isZoomControlsEnabled = true
    }

    /**
     * Starts the dynamic updates of solar time calculations.
     * Updates occur every second based on the current map center location.
     */
    private fun startDynamicTimeUpdates() {
        timeUpdateRunnable = Runnable {
            try {
                if (::map.isInitialized) {
                    val center = map.cameraPosition.target
                    calculateAndDisplaySolarTime(center)
                }
            } catch (e: Exception) {
                // Log the error but don't stop updates
                e.printStackTrace()
            }
            // Always schedule the next update
            timeUpdateHandler.postDelayed(timeUpdateRunnable, 1000)
        }
        timeUpdateHandler.post(timeUpdateRunnable)
    }

    /**
     * Calculates and displays the solar time for a given location.
     * Updates the UI with solar time, current time, and coordinates.
     *
     * @param latLng The latitude and longitude to calculate solar time for
     */
    private fun calculateAndDisplaySolarTime(latLng: LatLng) {
        val latitude = latLng.latitude
        val longitude = latLng.longitude

        // Store the current location for use in dialogs
        currentLocation = latLng

        latitudeLongitudeTextView.text =
            String.format(Locale.getDefault(), "Lat: %.4f, Lon: %.4f", latitude, longitude)

        val calendar = Calendar.getInstance()

        // Use the SolarTimeUtil to calculate solar time with the selected precision level
        val solarTime = SolarTimeUtil.calculateSolarTime(
            latLng,
            calendar,
            selectedPrecisionLevel
        )
        val (solarHours, solarMinutes, solarSeconds) = solarTime

        solarTimeTextView.visibility = View.VISIBLE
        currentTimeTextView.visibility = View.VISIBLE

        solarTimeTextView.text = String.format(
            Locale.getDefault(),
            "Solar Time: %02d:%02d:%02d",
            solarHours,
            solarMinutes,
            solarSeconds
        )
        currentTimeTextView.text = String.format(
            Locale.getDefault(),
            "Current Time: %02d:%02d:%02d",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            calendar.get(Calendar.SECOND)
        )

        // Calculate and display sunrise and sunset times
        calculateAndDisplaySunriseSunset(latLng)
    }

    /**
     * Calculates and displays sunrise and sunset times for a given location.
     *
     * @param latLng The latitude and longitude to calculate sunrise/sunset for
     */
    private fun calculateAndDisplaySunriseSunset(latLng: LatLng) {
        val calendar = Calendar.getInstance()
        val (sunrise, sunset) = SunCalculator.calculateSunriseSunset(latLng, calendar)

        // Format and display the times
        val sunriseTime = SunCalculator.formatTime(sunrise)
        val sunsetTime = SunCalculator.formatTime(sunset)

        sunriseTextView.text = buildString {
            append("Sunrise: ")
            append(sunriseTime)
        }
        sunsetTextView.text = buildString {
            append("Sunset: ")
            append(sunsetTime)
        }
    }

    /**
     * Checks if location services are enabled and prompts the user to enable them if not.
     */
    private fun checkAndEnableLocation() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Please enable location services", Toast.LENGTH_SHORT).show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
    }

    /**
     * Called when the user responds to a permission request.
     * Enables location features if permission is granted.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                map.isMyLocationEnabled = true
                map.uiSettings.isMyLocationButtonEnabled = true
            }
        } else {
            Toast.makeText(this, "Permission denied to access location", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Called when the activity is about to be destroyed.
     * Cleans up all handlers to prevent memory leaks.
     */
    override fun onDestroy() {
        super.onDestroy()
        timeUpdateHandler.removeCallbacks(timeUpdateRunnable)
        searchHandler.removeCallbacksAndMessages(null)
    }

    /**
     * Shows a dialog explaining how solar time is calculated.
     */
    private fun showSolarTimeInfoDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Solar Time Calculation")

        val message = """
            Solar time is based on the sun's position in the sky and varies by location.
            
            The calculation includes:
            • Longitude adjustment (4 minutes per degree from reference meridian)
            • Equation of Time (EoT) - adjusts for Earth's elliptical orbit and axial tilt
            • Time zone offset - converts from UTC to local time
            • Atmospheric refraction - tiny adjustment for atmospheric effects
            • Delta T corrections - accounts for Earth's varying rotation (Ultra mode)
            • Relativistic effects - light-travel time and gravitational effects (Ultra mode)
            • Advanced planetary positions - using VSOP2013-like precision (Ultra mode)
            
            This app implements three precision levels:
            • Standard: Basic solar time calculation
            • High Precision: Advanced astronomical algorithm
            • Ultra Precision: Observatory-grade calculations
            
            Formula: 
            Standard time + longitude adjustment + EoT + atmospheric/relativistic corrections
            
            Solar noon occurs when the sun reaches its highest point in the sky.
        """.trimIndent()

        dialogBuilder.setMessage(message)
        dialogBuilder.setPositiveButton("Got it") { dialog, _ -> dialog.dismiss() }

        val dialog = dialogBuilder.create()
        dialog.show()
    }

    /**
     * Handles selection of menu items.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                // Search action is handled by the SearchView
                true
            }
            R.id.action_precision -> {
                showPrecisionLevelDialog()
                true
            }
            R.id.action_debug -> {
                showDebugInfoDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Creates the options menu and sets up the search functionality.
     * Configures the SearchView with custom styling and debounced search.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        // Find the search item and configure it
        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView

        // Set hint and styling
        searchView.queryHint = "Search location..."

        // Manually set the icon resources to avoid using private Android resources
        val searchIcon = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_button)
        searchIcon?.setImageResource(R.drawable.ic_search)

        val closeIcon = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        closeIcon?.setImageResource(R.drawable.ic_close)

        val voiceIcon = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_voice_btn)
        voiceIcon?.setImageResource(R.drawable.ic_voice_search)

        // Apply white color to search text and hint
        val searchSrcText =
            searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchSrcText?.apply {
            setTextColor(resources.getColor(android.R.color.white, theme))
            setHintTextColor(resources.getColor(android.R.color.white, theme))
        }

        // Add a small delay to prevent too frequent API calls
        var searchFor = ""
        var lastSearch = 0L
        val searchDelayMs = 300L

        // Handle query text changes
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            /**
             * Called when the user submits the query.
             */
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    searchLocation(query)
                    searchItem.collapseActionView() // Collapse after search
                }
                return true
            }

            /**
             * Called when the query text is changed by the user.
             * Implements debouncing to prevent too frequent API calls.
             */
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    recyclerViewPredictions.visibility = View.GONE
                    return true
                }

                // Debounce search queries to prevent too many API calls
                searchFor = newText
                val now = System.currentTimeMillis()

                if (now - lastSearch >= searchDelayMs) {
                    lastSearch = now
                    fetchAutocompletePredictions(searchFor)
                } else {
                    // Remove any pending searches using the searchHandler only
                    searchHandler.removeCallbacksAndMessages(null)
                    // Schedule this search after the delay
                    searchHandler.postDelayed({
                        lastSearch = System.currentTimeMillis()
                        // Only perform search if text hasn't changed
                        if (searchFor == newText) {
                            fetchAutocompletePredictions(searchFor)
                        }
                    }, searchDelayMs)
                }
                return true
            }
        })

        // When search is expanded, show predictions RecyclerView
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                // Consider showing predictions if there's already a query
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                recyclerViewPredictions.visibility = View.GONE
                return true
            }
        })

        return true
    }

    /**
     * Shows a dialog to select the solar time precision level.
     */
    private fun showPrecisionLevelDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Select Precision Level")

        val precisionOptions = arrayOf(
            "Standard - Basic calculation",
            "High Precision - Advanced algorithm",
            "Ultra Precision - Observatory-grade"
        )
        
        // Pre-select the current precision level
        val currentSelection = selectedPrecisionLevel

        // Add debug information to the dialog
        val debugInfo = currentLocation?.let {
            SolarTimeUtil.getSolarTimeDebugInfo(it, Calendar.getInstance())
        } ?: "Location not selected"

        val message = "Current calculation details:\n\n$debugInfo"
        dialogBuilder.setMessage(message)

        dialogBuilder.setSingleChoiceItems(precisionOptions, currentSelection) { dialog, which ->
            selectedPrecisionLevel = which
            dialog.dismiss()
            
            // Recalculate solar time with the new precision level
            currentLocation?.let { 
                calculateAndDisplaySolarTime(it)
                
                // Show a toast to confirm the change
                val precisionName = when (selectedPrecisionLevel) {
                    SolarTimeUtil.PRECISION_STANDARD -> "Standard"
                    SolarTimeUtil.PRECISION_HIGH -> "High Precision"
                    SolarTimeUtil.PRECISION_ULTRA -> "Ultra Precision"
                    else -> "Unknown"
                }
                Toast.makeText(this, "Using $precisionName calculation", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBuilder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val dialog = dialogBuilder.create()
        dialog.show()
    }

    /**
     * Shows a dialog with detailed debug information about the solar time calculation
     */
    private fun showDebugInfoDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Solar Time Debug Information")
        
        val debugInfo = currentLocation?.let {
            SolarTimeUtil.getSolarTimeDebugInfo(it, Calendar.getInstance())
        } ?: "Location not selected"
        
        dialogBuilder.setMessage(debugInfo)
        dialogBuilder.setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
        
        val dialog = dialogBuilder.create()
        dialog.show()
    }
}

/**
 * Adapter for displaying autocomplete prediction results in a RecyclerView.
 *
 * @param _predictions Initial list of predictions to display
 * @param onItemClick Callback function to execute when a prediction is clicked
 */
class PredictionAdapter(
    private var _predictions: List<AutocompletePrediction>,
    private val onItemClick: (AutocompletePrediction) -> Unit
) : RecyclerView.Adapter<PredictionAdapter.ViewHolder>() {

    // Use a custom getter for predictions to avoid clash with updatePredictionsList
    val predictions: List<AutocompletePrediction>
        get() = _predictions

    /**
     * ViewHolder for prediction items. Displays primary and secondary text.
     */
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewPrediction: TextView = itemView.findViewById(R.id.textViewPrediction)
        val textViewSecondaryText: TextView = itemView.findViewById(R.id.textViewSecondaryText)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(_predictions[position])
                }
            }
        }
    }

    /**
     * Creates a new ViewHolder for a prediction item.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_prediction, parent, false)
        return ViewHolder(view)
    }

    /**
     * Binds data to a ViewHolder at the specified position.
     * Applies a fade-in animation to newly bound items.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val prediction = _predictions[position]
        holder.textViewPrediction.text = prediction.getPrimaryText(null)
        holder.textViewSecondaryText.text = prediction.getSecondaryText(null)

        // Apply subtle fade-in animation
        holder.itemView.alpha = 0f
        holder.itemView.animate().alpha(1f).setDuration(300).start()
    }

    /**
     * Returns the total number of items in the data set.
     */
    override fun getItemCount(): Int = _predictions.size

    /**
     * Updates the list of predictions and refreshes the view.
     *
     * @param newPredictions New list of predictions to display
     */
    fun updatePredictionsList(newPredictions: List<AutocompletePrediction>) {
        _predictions = newPredictions
        // Use notifyDataSetChanged for simplicity to avoid animations that might cause crashes
        notifyDataSetChanged()
    }

    /**
     * Legacy method for updating predictions.
     *
     * @param newPredictions New list of predictions to display
     */
    @Deprecated("Use updatePredictionsList instead, this method may cause crashes")
    fun updatePredictions(newPredictions: List<AutocompletePrediction>) {
        updatePredictionsList(newPredictions)
    }
}