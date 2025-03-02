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
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import com.google.android.libraries.places.api.model.Place // Add this import
import com.google.android.libraries.places.api.net.FetchPlaceRequest // Add this import
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var solarTimeTextView: TextView
    private lateinit var currentTimeTextView: TextView
    private lateinit var latitudeLongitudeTextView: TextView
    private lateinit var sunriseTextView: TextView
    private lateinit var sunsetTextView: TextView
    private lateinit var searchEditText: EditText
    private lateinit var recyclerViewPredictions: RecyclerView
    private lateinit var predictionAdapter: PredictionAdapter
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    private val referenceMeridian = 82.5
    private lateinit var placesClient: PlacesClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyDLcq3aNTfKAud-ZAi-Ajqg6Wp3-aNzaI4") // Replace with your API key
        }
        placesClient = Places.createClient(this)

        // Initialize views and components
        initializeViews()
        initializeMap()
        initializeSearchView()
        setupDebugButton()
        startDynamicTimeUpdates()
        checkAndEnableLocation()
    }

    private fun initializeViews() {
        solarTimeTextView = findViewById(R.id.solarTimeTextView)
        currentTimeTextView = findViewById(R.id.currentTimeTextView)
        latitudeLongitudeTextView = findViewById(R.id.latitudeLongitudeTextView)
        sunriseTextView = findViewById(R.id.sunriseTextView)
        sunsetTextView = findViewById(R.id.sunsetTextView)
        searchEditText = findViewById(R.id.searchEditText)
        recyclerViewPredictions = findViewById(R.id.recyclerViewPredictions)
        recyclerViewPredictions.layoutManager = LinearLayoutManager(this)
        predictionAdapter = PredictionAdapter(emptyList()) { prediction ->
            fetchPlaceDetails(prediction.placeId)
            recyclerViewPredictions.visibility = View.GONE // Hide after selection
        }
        recyclerViewPredictions.adapter = predictionAdapter
    }

    private fun initializeMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun initializeSearchView() {
        val searchEditText = findViewById<EditText>(R.id.searchEditText)

        // Handle search when the user presses the search button on the keyboard
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = searchEditText.text.toString()
                if (query.isNotEmpty()) {
                    searchLocation(query)
                }
                true
            } else {
                false
            }
        }

        // Handle clicks on the search container - fixed to use a proper approach
        val searchContainer = findViewById<RelativeLayout>(R.id.searchContainer)
        searchContainer.setOnClickListener {
            val query = searchEditText.text.toString()
            if (query.isNotEmpty()) {
                searchLocation(query)
            }
        }

        // Listen for text changes and fetch autocomplete predictions dynamically
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                if (query.isNotEmpty()) {
                    fetchAutocompletePredictions(query)
                } else {
                    recyclerViewPredictions.visibility = View.GONE // Hide if no text
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun fetchAutocompletePredictions(query: String) {
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
            val predictions = response.autocompletePredictions
            if (predictions.isNotEmpty()) {
                predictionAdapter.predictions = predictions
                predictionAdapter.notifyDataSetChanged()
                recyclerViewPredictions.visibility = View.VISIBLE // Show RecyclerView
            } else {
                recyclerViewPredictions.visibility = View.GONE // Hide if no predictions
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

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

    private fun fetchPlaceDetails(placeId: String) {
        val placeFields = listOf(Place.Field.LAT_LNG, Place.Field.NAME)
        val request = FetchPlaceRequest.builder(placeId, placeFields).build()

        placesClient.fetchPlace(request).addOnSuccessListener { response ->
            val place = response.place
            val latLng = place.latLng
            if (latLng != null) {
                updateMapWithLocation(latLng, place.name ?: "Selected Location")
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateMapWithLocation(latLng: LatLng, title: String) {
        map.clear()
        map.addMarker(MarkerOptions().position(latLng).title(title))
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
            map.uiSettings.isCompassEnabled = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }

        val initialLocation = LatLng(0.0, 0.0)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, 2f))
        map.uiSettings.isZoomControlsEnabled = true
    }

    private fun startDynamicTimeUpdates() {
        runnable = Runnable {
            if (::map.isInitialized) {
                val center = map.cameraPosition.target
                calculateAndDisplaySolarTime(center)
            }
            handler.postDelayed(runnable, 1000)
        }
        handler.post(runnable)
    }

    private fun calculateAndDisplaySolarTime(latLng: LatLng) {
        val latitude = latLng.latitude
        val longitude = latLng.longitude

        latitudeLongitudeTextView.text = String.format(Locale.getDefault(), "Lat: %.4f, Lon: %.4f", latitude, longitude)

        val calendar = Calendar.getInstance()
        
        // Use the more accurate solar time calculation from SolarTimeCalculator
        val (solarHours, solarMinutes, solarSeconds) = SolarTimeCalculator.calculateSolarTime(latLng, calendar)

        solarTimeTextView.visibility = View.VISIBLE
        currentTimeTextView.visibility = View.VISIBLE

        solarTimeTextView.text = String.format(Locale.getDefault(), "Solar Time: %02d:%02d:%02d", solarHours, solarMinutes, solarSeconds)
        currentTimeTextView.text = String.format(Locale.getDefault(), "Current Time: %02d:%02d:%02d",
            calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND))
            
        // Calculate and display sunrise and sunset times
        calculateAndDisplaySunriseSunset(latLng)
    }
    
    private fun calculateAndDisplaySunriseSunset(latLng: LatLng) {
        val calendar = Calendar.getInstance()
        val (sunrise, sunset) = SunCalculator.calculateSunriseSunset(latLng, calendar)
        
        // Format and display the times
        val sunriseTime = SunCalculator.formatTime(sunrise)
        val sunsetTime = SunCalculator.formatTime(sunset)
        
        sunriseTextView.text = "Sunrise: $sunriseTime"
        sunsetTextView.text = "Sunset: $sunsetTime"
    }

    private fun checkAndEnableLocation() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Please enable location services", Toast.LENGTH_SHORT).show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                map.isMyLocationEnabled = true
                map.uiSettings.isMyLocationButtonEnabled = true
            }
        } else {
            Toast.makeText(this, "Permission denied to access location", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }

    private fun setupDebugButton() {
        val debugButton = findViewById<Button>(R.id.debugButton)
        debugButton.setOnClickListener {
            val intent = Intent(this, SolarTimeDebugActivity::class.java)
            startActivity(intent)
        }
    }
}

class PredictionAdapter(
    var predictions: List<AutocompletePrediction>,
    private val onItemClick: (AutocompletePrediction) -> Unit
) : RecyclerView.Adapter<PredictionAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewPrediction: TextView = itemView.findViewById(R.id.textViewPrediction)
        val textViewSecondaryText: TextView = itemView.findViewById(R.id.textViewSecondaryText)

        init {
            itemView.setOnClickListener {
                onItemClick(predictions[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_prediction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val prediction = predictions[position]
        holder.textViewPrediction.text = prediction.getPrimaryText(null)
        holder.textViewSecondaryText.text = prediction.getSecondaryText(null)

        // Apply subtle fade-in animation
        holder.itemView.alpha = 0f
        holder.itemView.animate().alpha(1f).setDuration(300).start()
    }

    override fun getItemCount(): Int = predictions.size
}