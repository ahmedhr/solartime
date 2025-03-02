package com.ahr.solartime

import com.google.android.gms.maps.model.LatLng
import java.util.*
import kotlin.math.*

/**
 * A more accurate solar time calculator that includes the Equation of Time.
 */
class SolarTimeCalculator {

    companion object {
        private const val REFERENCE_MERIDIAN = 82.5
        
        /**
         * Calculate solar time for a given location.
         * 
         * @param latLng The latitude and longitude of the location
         * @param calendar The current calendar instance (or null to use current time)
         * @return Triple of (hours, minutes, seconds) representing solar time
         */
        fun calculateSolarTime(latLng: LatLng, calendar: Calendar = Calendar.getInstance()): Triple<Int, Int, Int> {
            val longitude = latLng.longitude
            
            // Get current standard time in seconds
            val standardTimeInSeconds = calendar.get(Calendar.HOUR_OF_DAY) * 3600 +
                    calendar.get(Calendar.MINUTE) * 60 +
                    calendar.get(Calendar.SECOND)
            
            // Calculate longitude offset in seconds
            val longitudeOffsetInSeconds = ((longitude - REFERENCE_MERIDIAN) / 15.0) * 3600
            
            // Calculate equation of time adjustment in seconds
            val eotAdjustmentInSeconds = calculateEquationOfTimeInSeconds(calendar)
            
            // Calculate solar time in seconds
            val solarTimeInSeconds = standardTimeInSeconds + longitudeOffsetInSeconds + eotAdjustmentInSeconds
            
            // Convert back to hours, minutes, seconds
            val solarHours = (solarTimeInSeconds.toInt() / 3600 + 24) % 24
            val solarMinutes = (solarTimeInSeconds.toInt() % 3600) / 60
            val solarSeconds = solarTimeInSeconds.toInt() % 60
            
            return Triple(solarHours, solarMinutes, solarSeconds)
        }
        
        /**
         * Calculate the Equation of Time adjustment in seconds.
         * This accounts for the discrepancy between apparent solar time and mean solar time
         * due to Earth's elliptical orbit and axial tilt.
         */
        private fun calculateEquationOfTimeInSeconds(calendar: Calendar): Double {
            // Get day of year (1-366)
            val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
            
            // Convert to radians for the formula
            val b = 2 * PI * (dayOfYear - 81) / 365.0
            
            // Spencer's formula for the Equation of Time (in minutes)
            val eot = 9.87 * sin(2 * b) - 7.53 * cos(b) - 1.5 * sin(b)
            
            // Convert to seconds
            return eot * 60.0
        }
        
        /**
         * Calculate the simplified solar time (without Equation of Time).
         * This matches the current implementation in MainActivity.
         */
        fun calculateSimplifiedSolarTime(latLng: LatLng, calendar: Calendar = Calendar.getInstance()): Triple<Int, Int, Int> {
            val longitude = latLng.longitude
            
            // Get current standard time in seconds
            val standardTimeInSeconds = calendar.get(Calendar.HOUR_OF_DAY) * 3600 +
                    calendar.get(Calendar.MINUTE) * 60 +
                    calendar.get(Calendar.SECOND)
            
            // Calculate longitude offset in seconds
            val longitudeOffsetInSeconds = ((longitude - REFERENCE_MERIDIAN) / 15.0) * 3600
            
            // Calculate solar time in seconds
            val solarTimeInSeconds = standardTimeInSeconds + longitudeOffsetInSeconds
            
            // Convert back to hours, minutes, seconds
            val solarHours = (solarTimeInSeconds.toInt() / 3600 + 24) % 24
            val solarMinutes = (solarTimeInSeconds.toInt() % 3600) / 60
            val solarSeconds = solarTimeInSeconds.toInt() % 60
            
            return Triple(solarHours, solarMinutes, solarSeconds)
        }
        
        /**
         * Get the difference between accurate and simplified solar time in seconds.
         * This can be used to verify how much the current implementation differs from a more accurate one.
         */
        fun getTimeDifferenceInSeconds(latLng: LatLng, calendar: Calendar = Calendar.getInstance()): Double {
            val (accurateHours, accurateMinutes, accurateSeconds) = calculateSolarTime(latLng, calendar)
            val (simpleHours, simpleMinutes, simpleSeconds) = calculateSimplifiedSolarTime(latLng, calendar)
            
            val accurateTimeInSeconds = accurateHours * 3600 + accurateMinutes * 60 + accurateSeconds
            val simpleTimeInSeconds = simpleHours * 3600 + simpleMinutes * 60 + simpleSeconds
            
            return (accurateTimeInSeconds - simpleTimeInSeconds).toDouble()
        }
    }
} 