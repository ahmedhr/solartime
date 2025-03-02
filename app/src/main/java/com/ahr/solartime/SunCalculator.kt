package com.ahr.solartime

import com.google.android.gms.maps.model.LatLng
import java.util.*
import kotlin.math.*

/**
 * Calculator for sun-related times and positions.
 * Calculates sunrise, sunset, and other solar position data.
 */
class SunCalculator {

    companion object {
        // Constants for calculations
        private const val TO_RAD = PI / 180.0
        private const val TO_DEG = 180.0 / PI
        private const val JULIAN_DATE_2000 = 2451545.0
        
        /**
         * Calculate sunrise and sunset times for a given location and date.
         * 
         * @param latLng The latitude and longitude of the location
         * @param calendar The date for which to calculate (time component is ignored)
         * @param zenith The solar zenith angle used to define sunrise/sunset
         *        - Official: 90.833° (sun's upper edge touches the horizon)
         *        - Civil: 96° (civil twilight)
         *        - Nautical: 102° (nautical twilight)
         *        - Astronomical: 108° (astronomical twilight)
         * @return Pair of sunrise and sunset times as Calendar objects, or null if sun doesn't rise/set
         */
        fun calculateSunriseSunset(
            latLng: LatLng, 
            calendar: Calendar = Calendar.getInstance(),
            zenith: Double = 90.833
        ): Pair<Calendar?, Calendar?> {
            
            val latitude = latLng.latitude
            val longitude = latLng.longitude
            
            // Calculate day of year
            val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
            
            // Convert latitude and longitude to radians
            val latRad = latitude * TO_RAD
            
            // Calculate solar declination
            val declination = calculateDeclination(dayOfYear)
            
            // Calculate hour angle
            val cosHourAngle = (cos(zenith * TO_RAD) - sin(latRad) * sin(declination)) / 
                              (cos(latRad) * cos(declination))
            
            // Check if the sun never rises/sets at this location on this day
            if (cosHourAngle > 1.0) {
                // Sun never rises
                return Pair(null, null)
            } else if (cosHourAngle < -1.0) {
                // Sun never sets
                return Pair(null, null)
            }
            
            // Calculate hour angle in degrees
            val hourAngle = acos(cosHourAngle) * TO_DEG
            
            // Calculate sunrise and sunset times in hours (local solar time)
            val sunriseHour = (360.0 - hourAngle) / 15.0
            val sunsetHour = (hourAngle) / 15.0
            
            // Adjust for longitude and equation of time
            val eot = calculateEquationOfTime(dayOfYear) / 60.0 // Convert to hours
            
            // Adjust for timezone
            val timeZoneOffset = calendar.timeZone.getOffset(calendar.timeInMillis) / 3600000.0 // Convert ms to hours
            val longitudeHour = longitude / 15.0
            
            // Calculate sunrise and sunset in UTC hours
            val sunriseUtc = sunriseHour - longitudeHour - eot
            val sunsetUtc = sunsetHour + longitudeHour - eot
            
            // Convert to local time
            val sunriseLocal = sunriseUtc + timeZoneOffset
            val sunsetLocal = sunsetUtc + timeZoneOffset
            
            // Create Calendar objects for sunrise and sunset
            val sunriseCal = (calendar.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, sunriseLocal.toInt())
                set(Calendar.MINUTE, ((sunriseLocal % 1) * 60).toInt())
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            val sunsetCal = (calendar.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, sunsetLocal.toInt())
                set(Calendar.MINUTE, ((sunsetLocal % 1) * 60).toInt())
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            return Pair(sunriseCal, sunsetCal)
        }
        
        /**
         * Calculate solar declination for a given day of year.
         * This is the angle between the rays of the sun and the plane of the Earth's equator.
         */
        private fun calculateDeclination(dayOfYear: Int): Double {
            // Approximate formula for declination
            val angle = 0.9863 * (dayOfYear - 81) * TO_RAD
            return 23.45 * sin(angle) * TO_RAD
        }
        
        /**
         * Calculate the equation of time for a given day of year.
         * Returns the result in minutes.
         */
        private fun calculateEquationOfTime(dayOfYear: Int): Double {
            // Convert day of year to radians for the formula
            val b = 2 * PI * (dayOfYear - 81) / 365.0
            
            // Spencer's formula for the Equation of Time (in minutes)
            return 9.87 * sin(2 * b) - 7.53 * cos(b) - 1.5 * sin(b)
        }
        
        /**
         * Format a Calendar time as a string in HH:MM format.
         * Returns "N/A" if the Calendar is null.
         */
        fun formatTime(calendar: Calendar?): String {
            if (calendar == null) return "N/A"
            return String.format(
                Locale.getDefault(),
                "%02d:%02d",
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE)
            )
        }
    }
} 