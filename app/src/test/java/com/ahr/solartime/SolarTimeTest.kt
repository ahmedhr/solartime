package com.ahr.solartime

import org.junit.Test
import org.junit.Assert.*
import java.util.*
import com.google.android.gms.maps.model.LatLng

class SolarTimeTest {

    private val referenceMeridian = 82.5

    @Test
    fun testBasicSolarTimeCalculation() {
        // Create a fixed calendar for testing
        val testCalendar = Calendar.getInstance()
        testCalendar.set(Calendar.HOUR_OF_DAY, 12)
        testCalendar.set(Calendar.MINUTE, 0)
        testCalendar.set(Calendar.SECOND, 0)
        
        // Test locations
        val testCases = listOf(
            // Location exactly at reference meridian should have solar time = standard time
            Triple(LatLng(0.0, 82.5), 12, 0),
            
            // Location 15 degrees east of reference should be 1 hour ahead
            Triple(LatLng(0.0, 97.5), 13, 0),
            
            // Location 15 degrees west of reference should be 1 hour behind
            Triple(LatLng(0.0, 67.5), 11, 0),
            
            // Location 7.5 degrees east should be 30 minutes ahead
            Triple(LatLng(0.0, 90.0), 12, 30)
        )
        
        for ((location, expectedHour, expectedMinute) in testCases) {
            val (solarHour, solarMinute) = calculateSolarTime(location, testCalendar)
            assertEquals("Solar hour incorrect for longitude ${location.longitude}", 
                expectedHour, solarHour)
            assertEquals("Solar minute incorrect for longitude ${location.longitude}", 
                expectedMinute, solarMinute)
        }
    }
    
    // Simplified version of your app's calculation for testing
    private fun calculateSolarTime(latLng: LatLng, calendar: Calendar): Pair<Int, Int> {
        val longitude = latLng.longitude
        
        val standardTimeInSeconds = calendar.get(Calendar.HOUR_OF_DAY) * 3600 +
                calendar.get(Calendar.MINUTE) * 60 +
                calendar.get(Calendar.SECOND)
        
        val longitudeOffsetInSeconds = ((longitude - referenceMeridian) / 15.0) * 3600
        val solarTimeInSeconds = standardTimeInSeconds + longitudeOffsetInSeconds
        
        val solarHours = (solarTimeInSeconds.toInt() / 3600 + 24) % 24
        val solarMinutes = (solarTimeInSeconds.toInt() % 3600) / 60
        
        return Pair(solarHours, solarMinutes)
    }
    
    @Test
    fun testEdgeCases() {
        // Create a fixed calendar for testing
        val testCalendar = Calendar.getInstance()
        testCalendar.set(Calendar.HOUR_OF_DAY, 23)
        testCalendar.set(Calendar.MINUTE, 45)
        testCalendar.set(Calendar.SECOND, 0)
        
        // Test day rollover: 23:45 + 30 minutes should be 00:15
        val location = LatLng(0.0, 90.0) // 7.5 degrees east = +30 minutes
        val (solarHour, solarMinute) = calculateSolarTime(location, testCalendar)
        assertEquals(0, solarHour)
        assertEquals(15, solarMinute)
    }
} 