package com.ahr.solartime

import com.google.android.gms.maps.model.LatLng
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

/**
 * Calculator for sunrise, sunset, and other sun-related astronomical data.
 */
object SunCalculator {
    private const val TO_RAD = Math.PI / 180.0
    private const val TO_DEG = 180.0 / Math.PI
    
    // Standard sun altitude for sunrise/sunset (-0.833° accounts for refraction and sun's diameter)
    private const val SUN_ALTITUDE_SUNRISE_SUNSET = -0.833
    
    /**
     * Calculate sunrise and sunset times for a location.
     * Returns a pair of Calendar objects (sunrise, sunset) in local time.
     */
    fun calculateSunriseSunset(latLng: LatLng, date: Calendar = Calendar.getInstance()): Pair<Calendar?, Calendar?> {
        val latitude = latLng.latitude
        val longitude = latLng.longitude
        
        // Make a copy of the calendar to avoid modifying the original
        val cal = date.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 12) // Set to noon to avoid DST transition issues
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        
        // Get day of year
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        
        // Calculate solar declination (approx)
        val declination = 23.45 * sin(TO_RAD * 0.9863 * (dayOfYear - 81))
        val declinationRad = declination * TO_RAD
        
        // Convert latitude to radians
        val latitudeRad = latitude * TO_RAD
        
        // Calculate hour angle for sunrise/sunset
        // cos(hour angle) = (sin(sun altitude) - sin(latitude) * sin(declination)) / (cos(latitude) * cos(declination))
        val cosHourAngle = (sin(TO_RAD * SUN_ALTITUDE_SUNRISE_SUNSET) - 
                           sin(latitudeRad) * sin(declinationRad)) / 
                           (cos(latitudeRad) * cos(declinationRad))
        
        // Check if sun never rises/sets at this location on this day
        if (cosHourAngle > 1.0) {
            // Sun never rises
            return Pair(null, null)
        } else if (cosHourAngle < -1.0) {
            // Sun never sets
            return Pair(null, null)
        }
        
        // Calculate hour angle in degrees
        val hourAngle = acos(cosHourAngle) * TO_DEG
        
        // Calculate equation of time correction in minutes
        val b = 2 * Math.PI * (dayOfYear - 81) / 365.0
        val eot = 9.87 * sin(2 * b) - 7.53 * cos(b) - 1.5 * sin(b)  // in minutes
        
        // Get solar noon in minutes from local midnight
        val solarNoonMinutes = (720 - 4 * longitude - eot) // in minutes from midnight UTC
        
        // Convert to sunrise and sunset in minutes from midnight UTC
        val sunriseMinutes = solarNoonMinutes - 4 * hourAngle
        val sunsetMinutes = solarNoonMinutes + 4 * hourAngle
        
        // Create sunrise and sunset calendars in local time zone
        val sunrise = cal.clone() as Calendar
        val sunset = cal.clone() as Calendar
        
        // Convert minutes to hours and minutes, ensuring we stay within the day
        val sunriseHours = ((sunriseMinutes / 60.0) % 24).toInt()
        val sunriseMinutesOnly = (sunriseMinutes % 60.0).toInt()
        
        val sunsetHours = ((sunsetMinutes / 60.0) % 24).toInt()
        val sunsetMinutesOnly = (sunsetMinutes % 60.0).toInt()
        
        // Set the calculated hours and minutes (UTC time)
        sunrise.set(Calendar.HOUR_OF_DAY, sunriseHours)
        sunrise.set(Calendar.MINUTE, sunriseMinutesOnly)
        
        sunset.set(Calendar.HOUR_OF_DAY, sunsetHours)
        sunset.set(Calendar.MINUTE, sunsetMinutesOnly)
        
        // Convert to local time zone from UTC
        // No need to manually adjust for time zone - just use the right time zone
        val timeZoneOffset = cal.timeZone.getOffset(cal.timeInMillis) / (60 * 1000) // in minutes
        sunrise.add(Calendar.MINUTE, timeZoneOffset)
        sunset.add(Calendar.MINUTE, timeZoneOffset)
        
        return Pair(sunrise, sunset)
    }
    
    /**
     * Format a Calendar time as HH:MM string
     */
    fun formatTime(time: Calendar?): String {
        if (time == null) return "N/A"
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(time.time)
    }
    
    /**
     * Calculate day length in hours and minutes
     */
    fun getDayLength(sunrise: Calendar?, sunset: Calendar?): String {
        if (sunrise == null || sunset == null) return "N/A"
        
        val dayLengthMillis = sunset.timeInMillis - sunrise.timeInMillis
        val dayLengthHours = dayLengthMillis / (1000 * 60 * 60)
        val dayLengthMinutes = (dayLengthMillis / (1000 * 60)) % 60
        
        return String.format("%dh %02dm", dayLengthHours, dayLengthMinutes)
    }
    
    /**
     * Calculate solar noon as the midpoint between sunrise and sunset
     */
    fun calculateSolarNoon(sunrise: Calendar?, sunset: Calendar?): Calendar? {
        if (sunrise == null || sunset == null) return null
        
        val noonMillis = sunrise.timeInMillis + (sunset.timeInMillis - sunrise.timeInMillis) / 2
        return Calendar.getInstance().apply { timeInMillis = noonMillis }
    }
} 