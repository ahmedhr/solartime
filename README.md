# Solar Time

A precision Android application that calculates and displays solar time based on your geographical location. The app integrates Google Maps for location selection and provides accurate solar time calculations, sunrise, and sunset times.

## Features

- **Real-time Solar Time Calculation**: Displays accurate solar time based on your location using advanced astronomical formulas
- **Interactive Map**: Powered by Google Maps for precise location selection
- **Location Search**: Search for any location worldwide using the Google Places API
- **Sunrise & Sunset Times**: View accurate daily sunrise and sunset times
- **Information Panel**: Learn about how solar time is calculated
- **Responsive UI**: Clean, intuitive interface with visual indicators

## Solar Time Explained

Solar time is based on the apparent motion of the Sun across the sky. Unlike standard time, which is fixed within time zones, solar time varies continuously with longitude and throughout the year due to Earth's elliptical orbit and axial tilt.

The app calculates solar time using:
- **Longitude Adjustment**: 4 minutes of time for each degree of longitude
- **Equation of Time (EoT)**: Accounts for Earth's elliptical orbit and axial tilt
- **Time Zone Conversion**: Adjusts from UTC to local time
- **Atmospheric Refraction**: Small adjustment for atmospheric effects

Solar noon occurs when the sun reaches its highest point in the sky at your location.

## How to Use

1. **Open the app**: The app will initialize with a world map view
2. **Select a location**: Either:
   - Search for a location using the search bar
   - Navigate the map and tap a location
   - Use your current location (requires location permissions)
3. **View Solar Information**: The bottom panel will display:
   - Current standard time
   - Calculated solar time
   - Sunrise and sunset times for the selected location
4. **Learn More**: Tap the info icon next to solar time for more details about the calculation

## Technical Details

### Solar Time Calculation

The app uses a sophisticated algorithm to calculate precise solar time:

```
Solar Time = UTC Time + (Longitude × 240 seconds) + Equation of Time + Refraction Adjustment
```

The Equation of Time uses the NREL SPA formula:
```
EoT = 229.18 × (0.000075 + 0.001868×cos(γ) - 0.032077×sin(γ) - 0.014615×cos(2γ) - 0.040849×sin(2γ))
```
Where γ is the fractional year in radians.

### Sunrise/Sunset Calculation

The app calculates sunrise/sunset using the standard astronomical formula:
```
cos(hour angle) = (sin(-0.833°) - sin(latitude) × sin(declination)) / (cos(latitude) × cos(declination))
```
Where -0.833° accounts for atmospheric refraction and the sun's apparent diameter.

## Setup & Installation

1. Clone the repository
2. Open the project in Android Studio
3. Replace the Google Maps API key in `res/values/strings.xml` with your own key
4. Add Google Maps and Places API to your Google Cloud Console project
5. Build and run the application on your device or emulator

## Requirements

- Android 6.0 (Marshmallow) or higher
- Google Play Services
- Internet connection for maps and location search
- Location permissions for current location functionality

## Libraries Used

- Google Maps Android SDK
- Google Places API
- AndroidX libraries
- Kotlin Standard Library

## Credits

- Solar calculation formulas adapted from the NREL Solar Position Algorithm
- Time calculation references from "Astronomical Algorithms" by Jean Meeus

## License

[Add your license information here] 