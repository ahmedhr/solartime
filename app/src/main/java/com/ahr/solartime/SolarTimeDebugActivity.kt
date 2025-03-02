package com.ahr.solartime

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.model.LatLng
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

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
    private lateinit var dayLengthTextView: TextView
    private lateinit var solarNoonTextView: TextView
    private lateinit var eotValueTextView: TextView
    private lateinit var componentsTextView: TextView
    private lateinit var showDetailsButton: Button
    private lateinit var detailsContainer: View
    
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
        
        try {
            // These may be added in layout later
            dayLengthTextView = findViewById(R.id.dayLengthTextView)
            solarNoonTextView = findViewById(R.id.solarNoonTextView)
            eotValueTextView = findViewById(R.id.eotValueTextView)
            componentsTextView = findViewById(R.id.componentsTextView)
            detailsContainer = findViewById(R.id.detailsContainer)
            showDetailsButton = findViewById(R.id.showDetailsButton)
            
            // Setup details toggle
            showDetailsButton.setOnClickListener {
                if (detailsContainer.visibility == View.VISIBLE) {
                    detailsContainer.visibility = View.GONE
                    showDetailsButton.text = "Show Advanced Details"
                } else {
                    detailsContainer.visibility = View.VISIBLE
                    showDetailsButton.text = "Hide Advanced Details"
                }
            }
        } catch (e: Exception) {
            // If these views don't exist yet, we'll skip them
        }
        
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
        
        // Try to update advanced details if available
        try {
            // Calculate day length (if both sunrise and sunset exist)
            if (sunrise != null && sunset != null) {
                val dayLengthMillis = sunset.timeInMillis - sunrise.timeInMillis
                val dayLengthHours = dayLengthMillis / (1000 * 60 * 60)
                val dayLengthMinutes = (dayLengthMillis / (1000 * 60)) % 60
                dayLengthTextView.text = String.format(
                    "Day Length: %dh %dm",
                    dayLengthHours, dayLengthMinutes
                )
                
                // Calculate solar noon (midpoint between sunrise and sunset)
                val solarNoonMillis = sunrise.timeInMillis + (dayLengthMillis / 2)
                val solarNoonCal = Calendar.getInstance().apply { timeInMillis = solarNoonMillis }
                solarNoonTextView.text = String.format(
                    "Solar Noon: %02d:%02d",
                    solarNoonCal.get(Calendar.HOUR_OF_DAY),
                    solarNoonCal.get(Calendar.MINUTE)
                )
            } else {
                dayLengthTextView.text = "Day Length: N/A"
                solarNoonTextView.text = "Solar Noon: N/A"
            }
            
            // Calculate Equation of Time value in minutes
            val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
            val b = 2 * PI * (dayOfYear - 81) / 365.0
            val eot = 9.87 * sin(2 * b) - 7.53 * cos(b) - 1.5 * sin(b)
            eotValueTextView.text = String.format("EoT Value: %.2f minutes", eot)
            
            // Add calculation components to debug information
            val longitude = currentLocation.longitude
            
            // Create a UTC calendar to show UTC time
            val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            utcCalendar.timeInMillis = calendar.timeInMillis
            
            val utcTimeInSeconds = utcCalendar.get(Calendar.HOUR_OF_DAY) * 3600 +
                                   utcCalendar.get(Calendar.MINUTE) * 60 +
                                   utcCalendar.get(Calendar.SECOND)
                                   
            val longitudeOffsetInSeconds = (longitude * 240.0)
            val eotAdjustmentInSeconds = calculateEquationOfTime(calendar) * 60.0
            
            componentsTextView.text = String.format(
                "Calculation Components:\n" +
                "- UTC time: %d seconds (%02d:%02d:%02d)\n" +
                "- Longitude offset: %.1f seconds (%.1f minutes)\n" +
                "- EoT adjustment: %.1f seconds (%.1f minutes)\n" +
                "- Longitude factor: 1° = 240 seconds\n" +
                "- Current longitude: %.4f°",
                utcTimeInSeconds,
                utcCalendar.get(Calendar.HOUR_OF_DAY),
                utcCalendar.get(Calendar.MINUTE),
                utcCalendar.get(Calendar.SECOND),
                longitudeOffsetInSeconds,
                longitudeOffsetInSeconds / 60.0,
                eotAdjustmentInSeconds,
                eotAdjustmentInSeconds / 60.0,
                longitude
            )
        } catch (e: Exception) {
            // If these views don't exist yet, we'll skip them
        }
        
        // Update location display
        updateLocationDisplay()
    }
    
    private fun calculateEquationOfTime(calendar: Calendar): Double {
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        val fractionalDay = (hour * 3600 + minute * 60 + second) / 86400.0
        val dayDecimal = dayOfYear + fractionalDay
        
        val year = calendar.get(Calendar.YEAR)
        val isLeapYear = (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0))
        val daysInYear = if (isLeapYear) 366 else 365
        
        val gamma = 2.0 * PI * (dayDecimal - 1) / daysInYear
        
        return 229.18 * (
                0.000075 +
                0.001868 * cos(gamma) -
                0.032077 * sin(gamma) -
                0.014615 * cos(2 * gamma) -
                0.040849 * sin(2 * gamma)
        ) / 60.0  // Return EoT in minutes
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }
} 