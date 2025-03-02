package com.ahr.solartime

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.model.LatLng
import java.text.SimpleDateFormat
import java.util.*

/**
 * Debug activity to compare different solar time calculation methods.
 * This can be used to verify the accuracy of the solar time calculations.
 */
class SolarTimeDebugActivity : AppCompatActivity() {

    private lateinit var currentTimeTextView: TextView
    private lateinit var simpleSolarTimeTextView: TextView
    private lateinit var accurateSolarTimeTextView: TextView
    private lateinit var differenceTextView: TextView
    private lateinit var locationTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var sunriseTextView: TextView
    private lateinit var sunsetTextView: TextView
    
    private lateinit var eastButton: Button
    private lateinit var westButton: Button
    private lateinit var northButton: Button
    private lateinit var southButton: Button
    
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable
    
    private var currentLocation = LatLng(0.0, 82.5) // Start at reference meridian
    private val locationStep = 5.0 // Move 5 degrees at a time
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_solar_time_debug)
        
        initializeViews()
        setupButtons()
        startTimeUpdates()
    }
    
    private fun initializeViews() {
        currentTimeTextView = findViewById(R.id.currentTimeTextView)
        simpleSolarTimeTextView = findViewById(R.id.simpleSolarTimeTextView)
        accurateSolarTimeTextView = findViewById(R.id.accurateSolarTimeTextView)
        differenceTextView = findViewById(R.id.differenceTextView)
        locationTextView = findViewById(R.id.locationTextView)
        dateTextView = findViewById(R.id.dateTextView)
        sunriseTextView = findViewById(R.id.sunriseTextView)
        sunsetTextView = findViewById(R.id.sunsetTextView)
        
        eastButton = findViewById(R.id.eastButton)
        westButton = findViewById(R.id.westButton)
        northButton = findViewById(R.id.northButton)
        southButton = findViewById(R.id.southButton)
    }
    
    private fun setupButtons() {
        eastButton.setOnClickListener {
            currentLocation = LatLng(currentLocation.latitude, currentLocation.longitude + locationStep)
            updateLocationDisplay()
        }
        
        westButton.setOnClickListener {
            currentLocation = LatLng(currentLocation.latitude, currentLocation.longitude - locationStep)
            updateLocationDisplay()
        }
        
        northButton.setOnClickListener {
            currentLocation = LatLng(currentLocation.latitude + locationStep, currentLocation.longitude)
            updateLocationDisplay()
        }
        
        southButton.setOnClickListener {
            currentLocation = LatLng(currentLocation.latitude - locationStep, currentLocation.longitude)
            updateLocationDisplay()
        }
    }
    
    private fun updateLocationDisplay() {
        locationTextView.text = String.format(
            Locale.getDefault(),
            "Location: Lat: %.2f, Lon: %.2f",
            currentLocation.latitude,
            currentLocation.longitude
        )
    }
    
    private fun startTimeUpdates() {
        runnable = Runnable {
            updateTimeDisplays()
            handler.postDelayed(runnable, 1000)
        }
        handler.post(runnable)
    }
    
    private fun updateTimeDisplays() {
        val calendar = Calendar.getInstance()
        
        // Update date display
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        dateTextView.text = "Date: ${dateFormat.format(calendar.time)}"
        
        // Update current time
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        currentTimeTextView.text = "Current Time: ${timeFormat.format(calendar.time)}"
        
        // Update simple solar time
        val (simpleHours, simpleMinutes, simpleSeconds) = 
            SolarTimeCalculator.calculateSimplifiedSolarTime(currentLocation, calendar)
        simpleSolarTimeTextView.text = String.format(
            Locale.getDefault(),
            "Simple Solar Time: %02d:%02d:%02d",
            simpleHours, simpleMinutes, simpleSeconds
        )
        
        // Update accurate solar time
        val (accurateHours, accurateMinutes, accurateSeconds) = 
            SolarTimeCalculator.calculateSolarTime(currentLocation, calendar)
        accurateSolarTimeTextView.text = String.format(
            Locale.getDefault(),
            "Accurate Solar Time: %02d:%02d:%02d",
            accurateHours, accurateMinutes, accurateSeconds
        )
        
        // Update difference
        val diffSeconds = SolarTimeCalculator.getTimeDifferenceInSeconds(currentLocation, calendar)
        val diffMinutes = diffSeconds / 60.0
        differenceTextView.text = String.format(
            Locale.getDefault(),
            "Difference: %.2f minutes (%.2f seconds)",
            diffMinutes, diffSeconds
        )
        
        // Update sunrise and sunset times
        val (sunrise, sunset) = SunCalculator.calculateSunriseSunset(currentLocation, calendar)
        sunriseTextView.text = "Sunrise: ${SunCalculator.formatTime(sunrise)}"
        sunsetTextView.text = "Sunset: ${SunCalculator.formatTime(sunset)}"
        
        // Update location display
        updateLocationDisplay()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }
} 