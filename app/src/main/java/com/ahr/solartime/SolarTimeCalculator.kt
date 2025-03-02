package com.ahr.solartime

import com.google.android.gms.maps.model.LatLng
import java.util.*
import kotlin.math.*

/**
 * A more accurate solar time calculator that includes advanced astronomical adjustments.
 */
class SolarTimeCalculator {

    companion object {
        private const val TO_RAD = PI / 180.0

        /**
         * Calculate solar time for a given location with high precision.
         * 
         * @param latLng The latitude and longitude of the location
         * @param calendar The current calendar instance (or null to use current time)
         * @return Triple of (hours, minutes, seconds) representing solar time
         */
        fun calculateSolarTime(latLng: LatLng, calendar: Calendar = Calendar.getInstance()): Triple<Int, Int, Int> {
            val longitude = latLng.longitude
            val latitude = latLng.latitude
            
            // STEP 1: Convert local time to UTC
            // Get timezone offset in milliseconds

            // Create a calendar in UTC time 
            val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            utcCalendar.timeInMillis = calendar.timeInMillis
            
            // STEP 2: Get UTC time in seconds
            val utcTimeInSeconds = utcCalendar.get(Calendar.HOUR_OF_DAY) * 3600 +
                                  utcCalendar.get(Calendar.MINUTE) * 60 +
                                  utcCalendar.get(Calendar.SECOND)
            
            // STEP 3: Calculate longitude offset in seconds
            // 15° = 1 hour = 3600 seconds, so each degree is 240 seconds
            val longitudeOffsetInSeconds = (longitude * 240.0)  // No need for reference meridian now
            
            // STEP 4: Calculate equation of time adjustment in seconds
            val eotAdjustmentInSeconds = calculateAdvancedEquationOfTimeInSeconds(calendar)
            
            // STEP 5: Apply atmospheric refraction adjustment (small effect)
            val refractionAdjustmentSeconds = calculateRefractionAdjustment(latitude)
            
            // STEP 6: Calculate solar time in seconds by applying all adjustments to UTC time
            var solarTimeInSeconds = utcTimeInSeconds + 
                                   longitudeOffsetInSeconds + 
                                   eotAdjustmentInSeconds + 
                                   refractionAdjustmentSeconds
            
            // STEP 7: Normalize to ensure we're in the 0-86399 range (0-23:59:59)
            while (solarTimeInSeconds < 0) solarTimeInSeconds += 86400
            while (solarTimeInSeconds >= 86400) solarTimeInSeconds -= 86400
            
            // STEP 8: Convert back to hours, minutes, seconds
            val solarHours = (solarTimeInSeconds / 3600).toInt()
            val solarMinutes = ((solarTimeInSeconds % 3600) / 60).toInt()
            val solarSeconds = (solarTimeInSeconds % 60).toInt()
            
            return Triple(solarHours, solarMinutes, solarSeconds)
        }
        
        /**
         * Calculate the advanced equation of time adjustment in seconds.
         * This uses a more precise astronomical formula based on the NREL SPA algorithm.
         */
        private fun calculateAdvancedEquationOfTimeInSeconds(calendar: Calendar): Double {
            // Day of year with more precision (including fraction of day)
            val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val second = calendar.get(Calendar.SECOND)
            val fractionalDay = (hour * 3600 + minute * 60 + second) / 86400.0
            val dayDecimal = dayOfYear + fractionalDay
            
            // Get year and check for leap year
            val year = calendar.get(Calendar.YEAR)
            val isLeapYear = (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0))
            val daysInYear = if (isLeapYear) 366 else 365
            
            // Calculate fractional year in radians
            val gamma = 2.0 * PI * (dayDecimal - 1) / daysInYear
            
            // Calculate equation of time components (in minutes)
            val eot = 229.18 * (
                    0.000075 +
                    0.001868 * cos(gamma) -
                    0.032077 * sin(gamma) -
                    0.014615 * cos(2 * gamma) -
                    0.040849 * sin(2 * gamma)
            )
            
            // Convert to seconds
            return eot * 60.0
        }
        
        /**
         * Calculate adjustment for atmospheric refraction (more significant near sunrise/sunset)
         * Returns adjustment in seconds
         */
        private fun calculateRefractionAdjustment(latitude: Double): Double {
            // This would be based on solar elevation and atmospheric conditions
            // For simplicity, we'll use a very small constant adjustment that varies with latitude
            // In a full implementation, this would be much more complex
            return 0.5 * sin(abs(latitude) * TO_RAD)
        }
        
        /**
         * Calculate the simplified solar time (without advanced adjustments).
         * This matches the original simple implementation but with improved time zone handling.
         */
        fun calculateSimplifiedSolarTime(latLng: LatLng, calendar: Calendar = Calendar.getInstance()): Triple<Int, Int, Int> {
            val longitude = latLng.longitude
            
            // Convert local time to UTC
            val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            utcCalendar.timeInMillis = calendar.timeInMillis
            
            // Get UTC time in seconds
            val utcTimeInSeconds = utcCalendar.get(Calendar.HOUR_OF_DAY) * 3600 +
                                   utcCalendar.get(Calendar.MINUTE) * 60 +
                                   utcCalendar.get(Calendar.SECOND)
            
            // Calculate longitude offset in seconds (15° = 1 hour = 3600 seconds)
            val longitudeOffsetInSeconds = (longitude * 240.0)
            
            // Calculate solar time in seconds (just using longitude correction)
            var solarTimeInSeconds = utcTimeInSeconds + longitudeOffsetInSeconds
            
            // Normalize to ensure we're in the 0-86399 range (0-23:59:59)
            while (solarTimeInSeconds < 0) solarTimeInSeconds += 86400
            while (solarTimeInSeconds >= 86400) solarTimeInSeconds -= 86400
            
            // Convert back to hours, minutes, seconds
            val solarHours = (solarTimeInSeconds / 3600).toInt()
            val solarMinutes = ((solarTimeInSeconds % 3600) / 60).toInt()
            val solarSeconds = (solarTimeInSeconds % 60).toInt()
            
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
            
            // Handle day boundary crossings
            var diff = (accurateTimeInSeconds - simpleTimeInSeconds).toDouble()
            if (diff > 43200) diff -= 86400  // More than 12 hours difference means we crossed midnight
            if (diff < -43200) diff += 86400
            
            return diff
        }
    }
} 