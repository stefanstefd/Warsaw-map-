# Warsaw Public Transport Interactive Map

A Java application that displays real-time public transport information for Warsaw, Poland using the official Warsaw Open Data API. The application shows buses, trams, and transport stops on an interactive map using JavaFX and Leaflet.js.

## Features

- **Real-time Vehicle Tracking**: Display current positions of buses and trams
- **Interactive Map**: Zoom, pan, and click on vehicles for details
- **Layer Control**: Toggle visibility of buses, trams, and stops
- **Auto-refresh**: Automatically updates vehicle positions every 30 seconds
- **API Integration**: Uses official Warsaw Open Data API
- **Modern UI**: Clean JavaFX interface with responsive design

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Internet connection for map tiles and API calls

## Getting Started

### 1. Clone and Build

```bash
git clone <repository-url>
cd warsaw-transport-map
mvn clean compile
```

### 2. Run the Application

```bash
mvn javafx:run
```

Or build and run the JAR:

```bash
mvn clean package
java -jar target/warsaw-transport-map-1.0.0.jar
```

### 3. Optional: Set API Key

While the application works without an API key, you can get one from [Warsaw Open Data](https://api.um.warszawa.pl/) for potentially better rate limits:

1. Register at the Warsaw Open Data portal
2. Get your API key
3. Enter it in the application's API Key field

## Usage

### Main Interface

- **Map**: The main area shows Warsaw with transport overlays
- **Display Controls**: Check/uncheck boxes to show/hide buses, trams, and stops
- **API Key Field**: Optional field for Warsaw API authentication
- **Refresh Button**: Manual data refresh (auto-refresh runs every 30 seconds)
- **Status Bar**: Shows application status and loading indicator

### Map Interactions

- **Zoom**: Use mouse wheel or map controls
- **Pan**: Click and drag to move around
- **Vehicle Info**: Click on any vehicle marker to see details:
  - Line number
  - Vehicle number
  - Brigade information
- **Stop Info**: Click on stop markers to see stop details

### Performance Tips

- Disable stops display when zoomed out (many stops can slow rendering)
- Use the API key for better performance
- The application caches data to reduce API calls

## API Endpoints Used

The application uses the following Warsaw Open Data API endpoints:

- **Bus Positions**: 
  - `resource_id=f2e5503e-927d-4ad3-9500-4ab9e55deb59`
  - `resource_id=36566e32-e31b-4b65-8e58-4cd6b2b8b33a`
- **Tram Positions**: `resource_id=13ce234d-3a8e-44ad-8c3c-6e77b3f99816`
- **Transport Stops**: `resource_id=29f2ea11-7a88-46fc-9ad6-b0e9c96b4b29`

## Project Structure

```
src/
├── main/
│   ├── java/com/warsaw/transport/
│   │   ├── WarsawTransportApp.java          # Main application entry
│   │   ├── api/
│   │   │   └── WarsawApiClient.java         # API communication
│   │   ├── model/
│   │   │   ├── Vehicle.java                # Vehicle data model
│   │   │   └── TransportStop.java          # Stop data model
│   │   ├── service/
│   │   │   └── TransportDataService.java   # Data management service
│   │   └── ui/
│   │       └── MainWindow.java             # Main UI window
│   └── resources/
│       ├── application.properties          # Configuration
│       └── styles.css                      # UI styling
└── pom.xml                                 # Maven configuration
```

## Configuration

Edit `src/main/resources/application.properties` to customize:

- API endpoints and timeouts
- Update intervals
- Map default position and zoom
- UI dimensions
- Cache settings

## Dependencies

- **JavaFX**: UI framework
- **Apache HttpClient**: HTTP communication
- **Jackson**: JSON processing
- **SLF4J + Logback**: Logging
- **Leaflet.js**: Web-based mapping (loaded from CDN)

## Troubleshooting

### Common Issues

1. **Application won't start**
   - Ensure Java 17+ is installed
   - Check JavaFX modules are available

2. **No data displayed**
   - Check internet connection
   - Verify Warsaw API is accessible
   - Try entering an API key

3. **Map not loading**
   - Check internet connection for OpenStreetMap tiles
   - Ensure WebView is supported on your system

4. **Performance issues**
   - Disable stops display when zoomed out
   - Check available memory (increase with `-Xmx` if needed)

### Logging

The application logs to console. To increase verbosity, edit the logging configuration in `application.properties`.

## Development

### Building

```bash
mvn clean compile          # Compile only
mvn clean package          # Build JAR
mvn javafx:run            # Run in development mode
```

### Testing

```bash
mvn test                  # Run unit tests
```

### Adding Features

Key extension points:

- **New Vehicle Types**: Extend `Vehicle.VehicleType` enum
- **Additional APIs**: Add methods to `WarsawApiClient`
- **Map Features**: Modify the HTML/JavaScript in `MainWindow.generateMapHtml()`
- **UI Enhancements**: Extend `MainWindow` class

## License

This project is for educational purposes. Please respect the Warsaw Open Data API terms of use.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## API Data Attribution

Data provided by Warsaw City Hall via the Warsaw Open Data portal. Real-time transport information is subject to the city's data availability and accuracy.
