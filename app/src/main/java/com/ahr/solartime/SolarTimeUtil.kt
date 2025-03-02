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
        val (hours, minutes, seconds) = calculateSolarTime(latLng, calendar, precision)
        return String.format(
            Locale.getDefault(),
            "%02d:%02d:%02d",
            hours,
            minutes,
            seconds
        )
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
        val tzOffset = calendar.timeZone.getOffset(calendar.timeInMillis) / (1000.0 * 60 * 60)
        val standardMeridian = 15.0 * tzOffset
        
        val localTime = String.format(
            Locale.getDefault(),
            "%02d:%02d:%02d",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            calendar.get(Calendar.SECOND)
        )
        
        val longitudeCorrection = (longitude - standardMeridian) * 240 // seconds
        val eotCorrection = SolarTimeCalculator.calculateEquationOfTime(calendar) * 60 // seconds
        
        val longitudeCorrectionMinutes = longitudeCorrection / 60.0
        val eotCorrectionMinutes = eotCorrection / 60.0
        
        val totalCorrection = (longitudeCorrection + eotCorrection) / 60.0 // minutes
        
        val sb = StringBuilder()
        sb.append("Solar Time Debug Info:\n")
        sb.append("---------------------\n")
        sb.append("Local Time: $localTime\n")
        sb.append("Timezone: GMT${if (tzOffset >= 0) "+" else ""}$tzOffset\n")
        sb.append("Location: ${latLng.latitude}°, ${latLng.longitude}°\n")
        sb.append("Standard Meridian: ${standardMeridian}°\n")
        sb.append("Longitude Diff: ${longitude - standardMeridian}°\n")
        sb.append("Longitude Correction: ${String.format("%.2f", longitudeCorrectionMinutes)} minutes\n")
        sb.append("Equation of Time: ${String.format("%.2f", eotCorrectionMinutes)} minutes\n")
        sb.append("Total Correction: ${String.format("%.2f", totalCorrection)} minutes\n")
        
        return sb.toString()
    }
} 