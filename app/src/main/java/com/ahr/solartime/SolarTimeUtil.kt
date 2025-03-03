package com.ahr.solartime

import com.google.android.gms.maps.model.LatLng
import java.util.*

/**
 * Utility class providing access to different solar time calculation methods
 * with varying levels of precision.
 */
object SolarTimeUtil {
    // Precision level constants
    const val PRECISION_STANDARD = 0
    const val PRECISION_HIGH = 1
    const val PRECISION_ULTRA = 2

    /**
     * Calculate solar time based on specified precision level
     * 
     * @param latLng The location coordinates
     * @param calendar The current time
     * @param precision The precision level (PRECISION_STANDARD, PRECISION_HIGH, PRECISION_ULTRA)
     * @return Triple containing hours, minutes, seconds of solar time
     */
    fun calculateSolarTime(
        latLng: LatLng,
        calendar: Calendar,
        precision: Int = PRECISION_ULTRA
    ): Triple<Int, Int, Int> {
        return when (precision) {
            PRECISION_STANDARD -> {
                SolarTimeCalculator.calculateSolarTime(latLng, calendar)
            }
            PRECISION_HIGH -> {
                HighPrecisionSolarTimeCalculator.calculateHighPrecisionSolarTime(latLng, calendar)
            }
            PRECISION_ULTRA -> {
                UltraPrecisionSolarTimeCalculator.calculateUltraPrecisionSolarTime(latLng, calendar)
            }
            else -> {
                // Default to the most accurate
                UltraPrecisionSolarTimeCalculator.calculateUltraPrecisionSolarTime(latLng, calendar)
            }
        }
    }

    /**
     * Format solar time components into a string
     * 
     * @param solarTime Triple of (hours, minutes, seconds)
     * @return Formatted time string in HH:MM:SS format
     */
    fun formatSolarTime(solarTime: Triple<Int, Int, Int>): String {
        val (hours, minutes, seconds) = solarTime
        return String.format(
            Locale.getDefault(),
            "%02d:%02d:%02d",
            hours,
            minutes,
            seconds
        )
    }

    /**
     * Convenience method to get formatted solar time string
     * 
     * @param latLng The location coordinates
     * @param calendar The current time
     * @param precision The precision level (PRECISION_STANDARD, PRECISION_HIGH, PRECISION_ULTRA)
     * @return Formatted time string in HH:MM:SS format
     */
    fun getSolarTimeFormatted(
        latLng: LatLng,
        calendar: Calendar,
        precision: Int = PRECISION_ULTRA
    ): String {
        val solarTime = calculateSolarTime(latLng, calendar, precision)
        return formatSolarTime(solarTime)
    }

    /**
     * Get a descriptive name for the precision level
     * 
     * @param precision The precision level constant
     * @return Human-readable name of the precision level
     */
    fun getPrecisionName(precision: Int): String {
        return when (precision) {
            PRECISION_STANDARD -> "Standard"
            PRECISION_HIGH -> "High Precision"
            PRECISION_ULTRA -> "Ultra Precision"
            else -> "Unknown Precision"
        }
    }

    /**
     * Get the solar time calculation debug information
     * 
     * @param latLng The location coordinates
     * @param calendar The current time
     * @return A string containing details about the calculation
     */
    fun getSolarTimeDebugInfo(
        latLng: LatLng,
        calendar: Calendar
    ): String {
        val longitude = latLng.longitude
        val latitude = latLng.latitude
        val tzOffset = calendar.timeZone.getOffset(calendar.timeInMillis) / (1000.0 * 60 * 60)
        val standardMeridian = 15.0 * tzOffset
        
        val localTime = String.format(
            Locale.getDefault(),
            "%02d:%02d:%02d",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            calendar.get(Calendar.SECOND)
        )
        
        val longitudeDifference = longitude - standardMeridian
        val longitudeCorrection = longitudeDifference * 4.0 // minutes (4 minutes per degree)
        val eotCorrection = SolarTimeCalculator.calculateEquationOfTime(calendar) // minutes
        
        val totalCorrection = longitudeCorrection + eotCorrection // minutes
        
        val sb = StringBuilder()
        sb.append("Solar Time Debug Info:\n")
        sb.append("---------------------\n")
        sb.append("Local Time: $localTime\n")
        sb.append("Timezone: GMT${if (tzOffset >= 0) "+" else ""}$tzOffset\n")
        sb.append("Location: ${latitude}°, ${longitude}°\n")
        sb.append("Standard Meridian: ${standardMeridian}°\n")
        sb.append("Longitude Diff: ${String.format("%.2f", longitudeDifference)}°\n")
        sb.append("Longitude Correction: ${String.format("%.2f", longitudeCorrection)} minutes\n")
        sb.append("Equation of Time: ${String.format("%.2f", eotCorrection)} minutes\n")
        sb.append("Total Correction: ${String.format("%.2f", totalCorrection)} minutes\n")
        
        // Calculate expected solar time
        val localTimeSeconds = calendar.get(Calendar.HOUR_OF_DAY) * 3600 + 
                              calendar.get(Calendar.MINUTE) * 60 + 
                              calendar.get(Calendar.SECOND)
        
        val correctionSeconds = (totalCorrection * 60).toInt()
        var solarTimeSeconds = localTimeSeconds + correctionSeconds
        
        // Normalize
        while (solarTimeSeconds < 0) solarTimeSeconds += 86400
        while (solarTimeSeconds >= 86400) solarTimeSeconds -= 86400
        
        val solarHours = solarTimeSeconds / 3600
        val solarMinutes = (solarTimeSeconds % 3600) / 60
        val solarSeconds = solarTimeSeconds % 60
        
        sb.append("Expected Solar Time: ${String.format("%02d:%02d:%02d", solarHours, solarMinutes, solarSeconds)}\n")
        
        return sb.toString()
    }
} 