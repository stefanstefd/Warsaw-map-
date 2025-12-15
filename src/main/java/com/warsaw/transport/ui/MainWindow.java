package com.warsaw.transport.ui;

import com.warsaw.transport.model.TransportStop;
import com.warsaw.transport.model.Vehicle;
import com.warsaw.transport.service.TransportDataService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.warsaw.transport.api.WarsawApiClient;

/**
 * Main window for the Warsaw Transport Interactive Map application
 */
public class MainWindow {

    private static final Logger logger = LoggerFactory.getLogger(MainWindow.class);

    private final BorderPane root;
    private final WebView webView;
    private final WebEngine webEngine;
    private final TransportDataService dataService;
    private ScheduledExecutorService scheduler;

    // Menu UI
    private final VBox menuPanel;
    private final Button realTimeButton;
    private final Button stopsMapButton;
    private final Button backToMenuButton;

    // Real-time mode UI Controls
    private final CheckBox showBusesCheckBox;
    private final CheckBox showTramsCheckBox;
    private final Button refreshButton;
    private final Label statusLabel;

    // Current mode
    private boolean isRealTimeMode = false;
    private boolean isStopsMode = false;

    public MainWindow() {
        this.dataService = new TransportDataService();
        this.scheduler = Executors.newScheduledThreadPool(2);

        // Create main layout
        this.root = new BorderPane();
        this.root.setStyle("-fx-background-color: #101820;");

        // Create web view for map
        this.webView = new WebView();
        this.webEngine = webView.getEngine();
        this.webView.setStyle("-fx-background-color: #101820;");
        this.webView.setZoom(1.4);

        // Create menu controls
        this.menuPanel = new VBox(20);
        this.realTimeButton = new Button("Real-time Buses & Trams");
        this.stopsMapButton = new Button("Transport Stops Map");
        this.backToMenuButton = new Button("Back to Menu");

        // Create real-time mode controls
        this.showBusesCheckBox = new CheckBox("Show Buses");
        this.showTramsCheckBox = new CheckBox("Show Trams");
        this.refreshButton = new Button("Refresh Data");
        this.statusLabel = new Label("Choose an option above");

        setupUI();
        setupEventHandlers();
        showMenu();
    }

    private void setupUI() {
        // Style the menu buttons
        realTimeButton.setPrefSize(420, 120);
        stopsMapButton.setPrefSize(420, 120);
        backToMenuButton.setPrefSize(240, 80);
        realTimeButton.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-background-color: #005BBB; -fx-text-fill: #FFFFFF; -fx-background-radius: 16; -fx-padding: 25 30;");
        stopsMapButton.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-background-color: #FFD500; -fx-text-fill: #101820; -fx-background-radius: 16; -fx-padding: 25 30;");
        backToMenuButton.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-background-color: #F2545B; -fx-text-fill: #FFFFFF; -fx-background-radius: 16; -fx-padding: 20 26;");
        backToMenuButton.setAlignment(Pos.CENTER);

        // Configure real-time checkboxes
        showBusesCheckBox.setSelected(true);
        showTramsCheckBox.setSelected(true);
        showBusesCheckBox.setStyle("-fx-font-size: 26px; -fx-text-fill: #FFFFFF; -fx-font-weight: bold;");
        showTramsCheckBox.setStyle("-fx-font-size: 26px; -fx-text-fill: #FFFFFF; -fx-font-weight: bold;");

        refreshButton.setPrefSize(260, 80);
        refreshButton.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-background-color: #00916E; -fx-text-fill: #FFFFFF; -fx-background-radius: 16; -fx-padding: 20 26;");
        statusLabel.setStyle("-fx-font-size: 26px; -fx-text-fill: #FEE715; -fx-font-weight: bold;");
        statusLabel.setWrapText(true);

        // Create menu panel
        menuPanel.setAlignment(Pos.CENTER);
        menuPanel.setPadding(new Insets(60));
        menuPanel.setSpacing(40);
        menuPanel.setStyle("-fx-background-color: #101820;");
        Label titleLabel = new Label("Warsaw Transport Interactive Map");
        titleLabel.setStyle("-fx-font-size: 42px; -fx-font-weight: bold; -fx-text-fill: #FEE715;");
        titleLabel.setWrapText(true);
        Label subtitleLabel = new Label("Pick what you want to see on the map");
        subtitleLabel.setStyle("-fx-font-size: 28px; -fx-text-fill: #FFFFFF;");
        subtitleLabel.setWrapText(true);
        menuPanel.getChildren().addAll(
            titleLabel,
            subtitleLabel,
            realTimeButton,
            stopsMapButton
        );

        // Create real-time control panel
        HBox realTimeControls = new HBox(10);
        realTimeControls.setPadding(new Insets(20));
        realTimeControls.setSpacing(30);
        realTimeControls.setAlignment(Pos.CENTER_LEFT);
        realTimeControls.setStyle("-fx-background-color: #1A1A2E; -fx-border-color: #FEE715; -fx-border-width: 0 0 4 0;");
        realTimeControls.getChildren().addAll(
            backToMenuButton,
            new Separator(),
            showBusesCheckBox, 
            showTramsCheckBox,
            new Separator(), 
            refreshButton, 
            new Separator(), 
            statusLabel
        );

        // Initial layout - show menu
        root.setCenter(menuPanel);
    }

    private void setupEventHandlers() {
        // Menu buttons
        realTimeButton.setOnAction(e -> startRealTimeMode());
        stopsMapButton.setOnAction(e -> startStopsMode());
        backToMenuButton.setOnAction(e -> showMenu());

        // Real-time mode controls
        showBusesCheckBox.setOnAction(e -> updateMapDisplay());
        showTramsCheckBox.setOnAction(e -> updateMapDisplay());
        refreshButton.setOnAction(e -> {
            statusLabel.setText("Refreshing data...");
            refreshButton.setDisable(true);

            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.submit(() -> {
                    try {
                        dataService.refreshVehiclePositions();
                        Platform.runLater(() -> {
                            updateMapDisplay();
                            statusLabel.setText("Data refreshed successfully");
                            refreshButton.setDisable(false);
                        });
                    } catch (Exception ex) {
                        logger.error("Failed to refresh data", ex);
                        Platform.runLater(() -> {
                            statusLabel.setText("Error refreshing data");
                            refreshButton.setDisable(false);
                        });
                    }
                });
            } else {
                // If scheduler is not available, refresh directly
                try {
                    dataService.refreshVehiclePositions();
                    updateMapDisplay();
                    statusLabel.setText("Data refreshed successfully");
                    refreshButton.setDisable(false);
                } catch (Exception ex) {
                    logger.error("Failed to refresh data", ex);
                    statusLabel.setText("Error refreshing data");
                    refreshButton.setDisable(false);
                }
            }
        });
    }

    private void showMenu() {
        isRealTimeMode = false;
        isStopsMode = false;
        
        // Stop any ongoing data refresh but don't shutdown the scheduler
        // Just cancel any scheduled tasks
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            // Create a new scheduler for next use
            scheduler = Executors.newScheduledThreadPool(2);
        }
        
        root.setTop(null);
        root.setCenter(menuPanel);
        statusLabel.setText("Choose an option above");
    }

    private void startRealTimeMode() {
        isRealTimeMode = true;
        isStopsMode = false;
        
        logger.info("Starting real-time mode");
        
        // Show real-time controls
        HBox realTimeControls = new HBox(10);
        realTimeControls.setPadding(new Insets(20));
        realTimeControls.setSpacing(30);
        realTimeControls.setAlignment(Pos.CENTER_LEFT);
        realTimeControls.setStyle("-fx-background-color: #1A1A2E; -fx-border-color: #FEE715; -fx-border-width: 0 0 4 0;");
        Label realTimeTitle = new Label("Live Vehicles View");
        realTimeTitle.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");
        realTimeTitle.setWrapText(true);
        realTimeControls.getChildren().addAll(
            backToMenuButton,
            new Separator(),
            realTimeTitle,
            new Separator(),
            showBusesCheckBox, 
            showTramsCheckBox,
            new Separator(), 
            refreshButton, 
            new Separator(), 
            statusLabel
        );
        
        root.setTop(realTimeControls);
        root.setCenter(webView);
        
        // Load the working map (buses and trams only)
        loadRealTimeMap();
        
        // Start data refresh
        startPeriodicDataRefresh();
        
        // Initial data load
        statusLabel.setText("Loading real-time data...");
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.submit(() -> {
                try {
                    dataService.refreshVehiclePositions();
                    Platform.runLater(() -> {
                        updateMapDisplay();
                        statusLabel.setText("Real-time data loaded");
                    });
                } catch (Exception ex) {
                    logger.error("Failed to load initial data", ex);
                    Platform.runLater(() -> {
                        statusLabel.setText("Error loading data");
                    });
                }
            });
        } else {
            // If scheduler is not available, load directly
            try {
                dataService.refreshVehiclePositions();
                updateMapDisplay();
                statusLabel.setText("Real-time data loaded");
            } catch (Exception ex) {
                logger.error("Failed to load initial data", ex);
                statusLabel.setText("Error loading data");
            }
        }
    }

    private void startStopsMode() {
        isRealTimeMode = false;
        isStopsMode = true;

        logger.info("Starting stops mode");

        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newScheduledThreadPool(2);
            logger.info("Recreated scheduler for stops mode");
        }
        
        // Show stops controls
        HBox stopsControls = new HBox(10);
        stopsControls.setPadding(new Insets(20));
        stopsControls.setSpacing(30);
        stopsControls.setAlignment(Pos.CENTER_LEFT);
        stopsControls.setStyle("-fx-background-color: #1A1A2E; -fx-border-color: #FEE715; -fx-border-width: 0 0 4 0;");
        Label stopsTitle = new Label("Transport Stops Map");
        stopsTitle.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");
        stopsTitle.setWrapText(true);
        stopsControls.getChildren().addAll(
            backToMenuButton,
            new Separator(),
            stopsTitle,
            new Separator(),
            statusLabel
        );
        
        root.setTop(stopsControls);
        root.setCenter(webView);
        
        // Load stops map
        loadStopsMap();
        
        // Load stops data
        statusLabel.setText("Loading transport stops...");
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.submit(() -> {
                try {
                    List<TransportStop> stops = dataService.refreshTransportStops();
                    Platform.runLater(() -> {
                        updateStopsDisplay(stops);
                        statusLabel.setText("Loaded " + stops.size() + " transport stops");
                    });
                } catch (Exception ex) {
                    logger.error("Failed to load stops", ex);
                    List<TransportStop> cachedStops = dataService.getTransportStops();
                    Platform.runLater(() -> {
                        if (!cachedStops.isEmpty()) {
                            updateStopsDisplay(cachedStops);
                            statusLabel.setText("Showing cached " + cachedStops.size() + " stops (offline)");
                        } else {
                            statusLabel.setText("Error loading stops");
                        }
                    });
                }
            });
        } else {
            // If scheduler is not available, load directly
            try {
                List<TransportStop> stops = dataService.refreshTransportStops();
                updateStopsDisplay(stops);
                statusLabel.setText("Loaded " + stops.size() + " transport stops");
            } catch (Exception ex) {
                logger.error("Failed to load stops", ex);
                List<TransportStop> cachedStops = dataService.getTransportStops();
                if (!cachedStops.isEmpty()) {
                    updateStopsDisplay(cachedStops);
                    statusLabel.setText("Showing cached " + cachedStops.size() + " stops (offline)");
                } else {
                    statusLabel.setText("Error loading stops");
                }
            }
        }
    }

    private void loadRealTimeMap() {
        String html = generateRealTimeMapHtml();
        webEngine.loadContent(html);
        
        // Wait for map to load before updating data
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                logger.info("Real-time map loaded successfully");
            }
        });
    }

    private void loadStopsMap() {
        String html = generateStopsMapHtml();
        webEngine.loadContent(html);
        
        // Wait for map to load before updating data
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                logger.info("Stops map loaded successfully");
                try {
                    // Expose Java bridge object to JavaScript
                    JSObject window = (JSObject) webEngine.executeScript("window");
                    window.setMember("bridge", new TimetableBridge());
                    logger.info("TimetableBridge exposed to JavaScript as 'bridge'");
                    
                    // Test if the bridge is available
                    Object bridgeType = webEngine.executeScript("typeof bridge");
                    logger.info("bridge object type: {}", bridgeType);
                } catch (Exception e) {
                    logger.error("Failed to expose TimetableBridge to JavaScript", e);
                }
            }
        });
    }

    private String generateRealTimeMapHtml() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Warsaw Transport Map</title>
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    body { margin: 0; padding: 0; background: #0D0D0D; color: #FFFFFF; font-family: 'Arial', sans-serif; }
                    #map { height: 100vh; width: 100%; background: #000000; }
                    .leaflet-control { font-size: 24px; }
                    .leaflet-control-zoom a, .leaflet-bar a { font-size: 28px; width: 54px; height: 54px; line-height: 50px; }
                    .leaflet-popup-content { font-size: 28px; line-height: 1.4; }
                    .leaflet-popup-content-wrapper { background: #202040; color: #FEE715; border: 4px solid #FEE715; border-radius: 16px; }
                    .leaflet-popup-tip { background: #202040; }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    // Initialize map centered on Warsaw
                    var map = L.map('map').setView([52.2297, 21.0122], 11);
                    
                    // Add OpenStreetMap tiles
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        attribution: '© OpenStreetMap contributors'
                    }).addTo(map);
                    
                    // Create layer groups for vehicles
                    var busesLayer = L.layerGroup().addTo(map);
                    var tramsLayer = L.layerGroup().addTo(map);
                    
                    // Function to update buses
                    function updateBuses(buses) {
                        busesLayer.clearLayers();
                        buses.forEach(function(bus) {
                            if (bus.Lat && bus.Lon) {
                                var marker = L.circleMarker([bus.Lat, bus.Lon], {
                                    radius: 14,
                                    fillColor: '#FF595A',
                                    color: '#FEE715',
                                    weight: 3,
                                    opacity: 1,
                                    fillOpacity: 0.9
                                });
                                
                                var popupContent = '<b>Bus Line ' + bus.Lines + '</b><br>' +
                                                 'Vehicle: ' + bus.VehicleNumber + '<br>' +
                                                 'Brigade: ' + bus.Brigade + '<br>' +
                                                 'Time: ' + bus.Time;
                                marker.bindPopup(popupContent);
                                marker.addTo(busesLayer);
                            }
                        });
                    }
                    
                    // Function to update trams
                    function updateTrams(trams) {
                        tramsLayer.clearLayers();
                        trams.forEach(function(tram) {
                            if (tram.Lat && tram.Lon) {
                                var marker = L.circleMarker([tram.Lat, tram.Lon], {
                                    radius: 14,
                                    fillColor: '#4D7CFE',
                                    color: '#FEE715',
                                    weight: 3,
                                    opacity: 1,
                                    fillOpacity: 0.9
                                });
                                
                                var popupContent = '<b>Tram Line ' + tram.Lines + '</b><br>' +
                                                 'Vehicle: ' + tram.VehicleNumber + '<br>' +
                                                 'Brigade: ' + tram.Brigade + '<br>' +
                                                 'Time: ' + tram.Time;
                                marker.bindPopup(popupContent);
                                marker.addTo(tramsLayer);
                            }
                        });
                    }
                    
                    // Functions called from Java
                    function toggleLayer(layerName, visible) {
                        switch(layerName) {
                            case 'buses':
                                if (visible) map.addLayer(busesLayer);
                                else map.removeLayer(busesLayer);
                                break;
                            case 'trams':
                                if (visible) map.addLayer(tramsLayer);
                                else map.removeLayer(tramsLayer);
                                break;
                        }
                    }
                </script>
            </body>
            </html>
            """;
    }

    private String generateStopsMapHtml() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Warsaw Transport Stops</title>
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    body { margin: 0; padding: 0; background: #0D0D0D; color: #FFFFFF; font-family: 'Arial', sans-serif; }
                    #map { height: 100vh; width: 100%; background: #000000; }
                    .leaflet-control { font-size: 24px; }
                    .leaflet-control-zoom a, .leaflet-bar a { font-size: 28px; width: 54px; height: 54px; line-height: 50px; }
                    .stop-popup { max-width: 420px; font-size: 28px; line-height: 1.4; }
                    .stop-popup h3 { margin: 0 0 16px 0; color: #FEE715; }
                    .stop-popup p { margin: 8px 0; color: #FFFFFF; }
                    .timetable-popup { max-width: 540px; max-height: 520px; overflow-y: auto; font-size: 28px; line-height: 1.4; color: #FFFFFF; }
                    .timetable-popup h3 { margin: 0 0 16px 0; color: #FEE715; }
                    .timetable-popup .line-section { margin: 14px 0; padding: 14px; background: #1A1A2E; border-radius: 16px; border: 3px solid #FEE715; }
                    .timetable-popup .line-header { font-weight: bold; color: #FFD500; margin-bottom: 8px; }
                    .timetable-popup .departure { margin: 6px 0; padding: 6px 0; border-bottom: 1px solid #394867; }
                    .timetable-popup .time { font-weight: bold; color: #00FFB2; }
                    .timetable-popup .direction { color: #E5E5E5; font-style: italic; }
                    .loading { text-align: center; color: #FEE715; font-style: italic; font-size: 28px; }
                    .stop-tooltip { background: #005BBB; color: #FFFFFF; border: 3px solid #FEE715; font-weight: bold; font-size: 26px; border-radius: 12px; padding: 6px 12px; }
                    .stop-tooltip::before { border-top-color: #005BBB; }
                    .loading-indicator { 
                        position: fixed; 
                        top: 50%; 
                        left: 50%; 
                        transform: translate(-50%, -50%); 
                        background: rgba(16, 24, 32, 0.92); 
                        color: #FEE715; 
                        padding: 28px 34px; 
                        border-radius: 16px; 
                        z-index: 10000; 
                        font-size: 30px; 
                        text-align: center; 
                        font-weight: bold;
                    }
                    .leaflet-popup-content-wrapper { background: #202040; color: #FEE715; border: 4px solid #FEE715; border-radius: 16px; }
                    .leaflet-popup-tip { background: #202040; }
                    .stop-search-control { 
                        background: rgba(16, 24, 32, 0.95);
                        padding: 10px;
                        border-radius: 12px;
                        border: 2px solid #FEE715;
                        box-shadow: 0 4px 10px rgba(0,0,0,0.35);
                        width: 220px;
                        max-width: 75vw;
                    }
                    .stop-search-label { display: block; font-size: 16px; color: #FEE715; font-weight: bold; margin-bottom: 4px; text-align: left; }
                    .stop-search-input { width: 100%; font-size: 16px; padding: 6px 8px; border-radius: 8px; border: 2px solid #FEE715; background: #0D0D0D; color: #FFFFFF; }
                    .stop-search-input::placeholder { color: #9DA5B4; }
                    .stop-search-button { margin-top: 6px; width: 100%; font-size: 14px; font-weight: bold; padding: 6px 8px; border-radius: 8px; border: 2px solid #FEE715; background: #005BBB; color: #FFFFFF; cursor: pointer; }
                    .stop-search-suggestions { margin-top: 6px; max-height: 160px; overflow-y: auto; }
                    .stop-search-suggestion { width: 100%; text-align: left; font-size: 14px; padding: 6px 8px; margin-bottom: 4px; background: #1A1A2E; color: #FFFFFF; border: 2px solid transparent; border-radius: 8px; cursor: pointer; }
                    .stop-search-suggestion:hover, .stop-search-suggestion.active { border-color: #FEE715; background: #2C2C54; }
                    .stop-home-control { background: rgba(16,24,32,0.95); padding: 8px; border-radius: 12px; border: 2px solid #FEE715; box-shadow: 0 4px 10px rgba(0,0,0,0.35); }
                    .stop-home-button { font-size: 14px; font-weight: bold; padding: 6px 12px; border-radius: 8px; border: 2px solid #FEE715; background: #00916E; color: #FFFFFF; cursor: pointer; }
                    .stop-home-button:hover { background: #00b085; }
                    .timetable-modal-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(8,12,18,0.85); display: flex; align-items: center; justify-content: center; z-index: 12000; }
                    .timetable-modal { background: #101820; padding: 18px 20px 22px; border: 2px solid #FEE715; border-radius: 18px; box-shadow: 0 12px 24px rgba(0,0,0,0.5); width: 320px; max-width: 90vw; max-height: 80vh; overflow-y: auto; position: relative; }
                    .timetable-modal h2 { font-size: 20px; margin: 0 0 12px 0; color: #FEE715; text-align: center; }
                    .timetable-modal-close { position: absolute; top: 8px; right: 8px; width: 32px; height: 32px; border: none; background: transparent; color: #FEE715; font-size: 24px; cursor: pointer; }
                    .timetable-modal-close:hover { color: #FFFFFF; }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    var map = L.map('map').setView([52.2297, 21.0122], 11);
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        attribution: '© OpenStreetMap contributors'
                    }).addTo(map);
                    
                    var stopsLayer = L.layerGroup().addTo(map);

                    var stopLookup = [];
                    var stopMarkers = {};
                    var DEFAULT_STOP_NAME = 'Groszówka';
                    var defaultStopTarget = null;
                    var stopMapInitialized = false;
                    var timetableModalOverlay = null;

                    var searchControl = L.control({ position: 'topright' });
                    searchControl.onAdd = function () {
                        var div = L.DomUtil.create('div', 'stop-search-control');
                        div.innerHTML = '' +
                            '<label class="stop-search-label" for="stop-search-input">Find stop</label>' +
                            '<input id="stop-search-input" class="stop-search-input" type="text" placeholder="Start typing name..." autocomplete="off" />' +
                            '<div id="stop-search-suggestions" class="stop-search-suggestions"></div>' +
                            '<button id="stop-search-button" class="stop-search-button" type="button">Go to stop</button>';
                        L.DomEvent.disableClickPropagation(div);
                        L.DomEvent.disableScrollPropagation(div);
                        return div;
                    };
                    searchControl.addTo(map);

                    var searchInput = document.getElementById('stop-search-input');
                    var searchButton = document.getElementById('stop-search-button');
                    var searchSuggestions = document.getElementById('stop-search-suggestions');
                    var suggestionIndex = -1;

                    var homeControl = L.control({ position: 'topright' });
                    homeControl.onAdd = function () {
                        var container = L.DomUtil.create('div', 'stop-home-control');
                        var button = L.DomUtil.create('button', 'stop-home-button', container);
                        button.type = 'button';
                        button.textContent = 'Home';
                        L.DomEvent.disableClickPropagation(container);
                        L.DomEvent.disableScrollPropagation(container);
                        L.DomEvent.on(button, 'click', function(){
                            if (defaultStopTarget) {
                                focusStopById(defaultStopTarget.id);
                            } else {
                                map.setView([52.2297, 21.0122], 11, { animate: true });
                            }
                        });
                        return container;
                    };
                    homeControl.addTo(map);

                    function updateStopSearchControl() {
                        renderSuggestions(searchInput ? searchInput.value : '');
                    }

                    function renderSuggestions(query) {
                        if (!searchSuggestions) { return; }
                        var term = (query || '').trim().toLowerCase();
                        if (!term) {
                            searchSuggestions.innerHTML = '';
                            suggestionIndex = -1;
                            return;
                        }

                        var matches = [];
                        for (var i = 0; i < stopLookup.length && matches.length < 8; i++) {
                            var item = stopLookup[i];
                            if (!item || !item.name) { continue; }
                            if (item.name.toLowerCase().indexOf(term) !== -1) {
                                matches.push(item);
                            }
                        }

                        if (!matches.length) {
                            searchSuggestions.innerHTML = '<div class="stop-search-suggestion" style="cursor: default;">No matches</div>';
                            suggestionIndex = -1;
                            return;
                        }

                        if (suggestionIndex >= matches.length) {
                            suggestionIndex = -1;
                        }

                        searchSuggestions.innerHTML = matches.map(function(match, idx){
                            return '<button type="button" class="stop-search-suggestion' + (idx === suggestionIndex ? ' active' : '') + '" data-id="' + match.id + '">' +
                                escapeHtml(match.name) + '</button>';
                        }).join('');

                        Array.prototype.forEach.call(searchSuggestions.querySelectorAll('.stop-search-suggestion'), function(button, idx){
                            button.addEventListener('click', function(){
                                focusStopById(this.getAttribute('data-id'));
                                renderSuggestions('');
                                suggestionIndex = -1;
                            });
                        });

                        highlightSuggestion();
                    }

                    function highlightSuggestion() {
                        if (!searchSuggestions) { return; }
                        var buttons = searchSuggestions.querySelectorAll('.stop-search-suggestion');
                        Array.prototype.forEach.call(buttons, function(button, idx){
                            if (idx === suggestionIndex) {
                                button.classList.add('active');
                                button.scrollIntoView({ block: 'nearest' });
                            } else {
                                button.classList.remove('active');
                            }
                        });
                    }

                    function escapeHtml(str) {
                        if (!str) { return ''; }
                        return str.replace(/[&<>"']/g, function(character){
                            switch (character) {
                                case '&': return '&amp;';
                                case '<': return '&lt;';
                                case '>': return '&gt;';
                                case '"': return '&quot;';
                                case "'": return '&#39;';
                                default: return character;
                            }
                        });
                    }

                    function focusStopById(id) {
                        if (!id || !stopMarkers[id]) { return; }
                        var entry = stopMarkers[id];
                        var marker = entry.marker;
                        var stop = entry.data;

                        if (!marker || !stop) { return; }
                        map.setView([stop.szer_geo, stop.dlug_geo], 16, { animate: true });
                        try {
                            marker.setRadius(20);
                            marker.setStyle({ fillColor: '#FF595A', color: '#FEE715' });
                            marker.closePopup();
                            marker.openPopup();
                        } catch (e) {
                            console.warn('Failed to highlight marker', e);
                        }

                        setTimeout(function(){
                            try {
                                marker.setRadius(14);
                                marker.setStyle({ fillColor: '#4D7CFE', color: '#FEE715' });
                            } catch (e) {}
                        }, 2000);

                        if (searchInput) {
                            searchInput.value = stop.nazwa_zespolu || '';
                        }

                        renderSuggestions('');
                        suggestionIndex = -1;
                    }

                    function focusStopByName(name) {
                        if (!name) { return; }
                        var lowered = name.trim().toLowerCase();
                        if (!lowered) { return; }
                        var exact = stopLookup.find(function(item){ return item.name && item.name.toLowerCase() === lowered; });
                        var target = exact || stopLookup.find(function(item){ return item.name && item.name.toLowerCase().indexOf(lowered) !== -1; });
                        if (target) {
                            focusStopById(target.id);
                        } else {
                            console.warn('Stop not found for search term:', name);
                        }
                    }

                    if (searchInput) {
                        L.DomEvent.disableClickPropagation(searchInput);
                        searchInput.addEventListener('input', function(){
                            suggestionIndex = -1;
                            renderSuggestions(this.value);
                        });
                        searchInput.addEventListener('keydown', function(evt){
                            var buttons = searchSuggestions ? searchSuggestions.querySelectorAll('.stop-search-suggestion') : [];
                            if (evt.key === 'ArrowDown') {
                                if (buttons.length) {
                                    evt.preventDefault();
                                    suggestionIndex = (suggestionIndex + 1) % buttons.length;
                                    highlightSuggestion();
                                }
                            } else if (evt.key === 'ArrowUp') {
                                if (buttons.length) {
                                    evt.preventDefault();
                                    suggestionIndex = suggestionIndex <= 0 ? buttons.length - 1 : suggestionIndex - 1;
                                    highlightSuggestion();
                                }
                            } else if (evt.key === 'Enter') {
                                evt.preventDefault();
                                if (buttons.length && suggestionIndex >= 0 && suggestionIndex < buttons.length) {
                                    var selected = buttons[suggestionIndex];
                                    focusStopById(selected.getAttribute('data-id'));
                                    renderSuggestions('');
                                    suggestionIndex = -1;
                                } else {
                                    focusStopByName(searchInput.value);
                                }
                            }
                        });
                        searchInput.addEventListener('blur', function(){
                            setTimeout(function(){
                                renderSuggestions('');
                                suggestionIndex = -1;
                            }, 150);
                        });
                    }

                    if (searchButton) {
                        L.DomEvent.disableClickPropagation(searchButton);
                        searchButton.addEventListener('click', function(){
                            focusStopByName(searchInput ? searchInput.value : '');
                            renderSuggestions('');
                            suggestionIndex = -1;
                        });
                    }

                    function updateStops(stopsData) {
                        console.log('updateStops called with', stopsData.length, 'stops');
                        stopsLayer.clearLayers();
                        stopLookup = stopsData.map(function(stop){
                            return {
                                id: stop.zespol + '_' + stop.slupek,
                                groupId: stop.zespol,
                                stopId: stop.slupek,
                                name: stop.nazwa_zespolu,
                                lat: stop.szer_geo,
                                lon: stop.dlug_geo
                            };
                        });
                        stopMarkers = {};

                        if (!defaultStopTarget) {
                            var desiredName = DEFAULT_STOP_NAME.toLowerCase();
                            defaultStopTarget = stopLookup.find(function(item){
                                return item.name && item.name.toLowerCase() === desiredName;
                            }) || stopLookup.find(function(item){
                                return item.name && item.name.toLowerCase().indexOf(desiredName) !== -1;
                            }) || null;
                        }

                        stopsData.forEach(function(stop) {
                            // Fix coordinate order: Leaflet expects [lat, lng] but we have [lng, lat]
                            var marker = L.circleMarker([stop.szer_geo, stop.dlug_geo], {
                                radius: 14,
                                fillColor: "#4D7CFE",
                                color: "#FEE715",
                                weight: 3,
                                opacity: 1,
                                fillOpacity: 0.95
                            }).addTo(stopsLayer);

                            // Make the marker larger and more visible
                            marker.setRadius(14);
                            
                            // Add a tooltip that shows immediately on hover
                            marker.bindTooltip('<strong>' + stop.nazwa_zespolu + '</strong><br>Click for timetable', {
                                permanent: false,
                                direction: 'top',
                                className: 'stop-tooltip'
                            });
                            
                            // Add click handler for timetable - this will be the main interaction
                            marker.on('click', function() {
                                console.log('=== STOP CLICKED ===');
                                console.log('Stop clicked:', stop.nazwa_zespolu);
                                console.log('Stop details:', stop.zespol, stop.slupek);
                                console.log('Calling showTimetable function...');
                                
                                // Show immediate visual feedback
                                marker.setRadius(18);
                                marker.setStyle({fillColor: "#FF595A", color: "#FEE715"});
                                // Open a loading popup at this marker
                                try {
                                    L.popup({autoClose: false, closeButton: true})
                                        .setLatLng(marker.getLatLng())
                                        .setContent('Loading timetable for <strong>' + (stop.nazwa_zespolu||'Stop') + '</strong>...')
                                        .openOn(map);
                                } catch (e) { console.warn('Failed to open loading popup', e); }
                                
                                showTimetable(stop.zespol, stop.slupek, stop.nazwa_zespolu);
                                
                                // Reset marker after 1 second
                                setTimeout(function() {
                                    marker.setRadius(14);
                                    marker.setStyle({fillColor: "#4D7CFE", color: "#FEE715"});
                                }, 1500);
                            });

                            stopMarkers[stop.zespol + '_' + stop.slupek] = {
                                marker: marker,
                                data: stop
                            };
                        });

                        updateStopSearchControl();

                        if (defaultStopTarget && !stopMapInitialized) {
                            map.setView([defaultStopTarget.lat, defaultStopTarget.lon], 16, { animate: false });
                            stopMapInitialized = true;
                        }
                    }
                    
                                                window.showTimetable = function(stopGroupId, stopId, stopName) {
                                console.log('=== SHOW TIMETABLE CALLED ===');
                                console.log('showTimetable called for:', stopName);
                                console.log('Parameters:', stopGroupId, stopId, stopName);
                                
                                window.timetableState = {
                                    stopGroupId: stopGroupId,
                                    stopId: stopId,
                                    stopName: stopName,
                                    lines: [],
                                    departuresByLine: {},
                                    showAll: false,
                                    pendingLine: null,
                                    departureCallback: null
                                };

                                // Show loading indicator
                                var loadingDiv = document.createElement('div');
                                loadingDiv.className = 'loading-indicator';
                                loadingDiv.innerHTML = 'Loading timetable for<br><strong>' + stopName + '</strong><br>Please wait...';
                                document.body.appendChild(loadingDiv);

                                // Call Java method to get timetable data
                                try {
                                    if (typeof bridge !== 'undefined' && bridge && typeof bridge.getTimetableData === 'function') {
                                        console.log('Calling bridge.getTimetableData...');
                                        bridge.getTimetableData(stopGroupId, stopId, stopName);
                                    } else if (bridge && bridge.getTimetableData) {
                                        console.warn('bridge detected without function type; attempting call anyway.');
                                        bridge.getTimetableData(stopGroupId, stopId, stopName);
                                    } else {
                                        console.warn('bridge not available; showing empty timetable.');
                                        window.displayTimetable(stopName, { lines: [] });
                                    }
                                } catch (err) {
                                    console.error('Error invoking bridge.getTimetableData:', err);
                                    window.displayTimetable(stopName, { lines: [] });
                                }
                            };

                    function closeTimetableModal() {
                        if (timetableModalOverlay && timetableModalOverlay.parentNode) {
                            timetableModalOverlay.parentNode.removeChild(timetableModalOverlay);
                        }
                        timetableModalOverlay = null;
                        if (window.timetableState) {
                            window.timetableState.modalElements = null;
                            window.timetableState.renderSelectedLine = null;
                            if (window.timetableState.modalKeyHandler) {
                                document.removeEventListener('keydown', window.timetableState.modalKeyHandler);
                                window.timetableState.modalKeyHandler = null;
                            }
                        }
                    }

                    window.displayTimetable = function(stopName, timetableData) {
                        console.log('Displaying timetable for:', stopName, timetableData);
                        
                        var loadingDiv = document.querySelector('.loading-indicator');
                        if (loadingDiv) { document.body.removeChild(loadingDiv); }

                        var payload = timetableData || {};
                        var lines = [];
                        var preloadedDepartures = {};

                        if (Array.isArray(payload)) {
                            payload.forEach(function(item) {
                                if (!item) { return; }
                                if (item.line) {
                                    lines.push(item.line);
                                    if (Array.isArray(item.departures)) {
                                        preloadedDepartures[item.line] = item.departures;
                                    }
                                }
                            });
                        } else {
                            if (Array.isArray(payload.lines)) {
                                lines = payload.lines.slice();
                            }
                            if (payload.departures && typeof payload.departures === 'object') {
                                preloadedDepartures = payload.departures;
                            }
                        }

                        window.timetableState.lines = lines;
                        window.timetableState.departuresByLine = window.timetableState.departuresByLine || {};
                        Object.keys(preloadedDepartures).forEach(function(lineKey) {
                            window.timetableState.departuresByLine[lineKey] = preloadedDepartures[lineKey];
                        });


                        closeTimetableModal();

                        var overlay = document.createElement('div');
                        overlay.className = 'timetable-modal-overlay';
                        var modal = document.createElement('div');
                        modal.className = 'timetable-modal';
                        overlay.appendChild(modal);

                        var closeBtn = document.createElement('button');
                        closeBtn.className = 'timetable-modal-close';
                        closeBtn.type = 'button';
                        closeBtn.setAttribute('aria-label', 'Close timetable');
                        closeBtn.innerHTML = '&times;';
                        closeBtn.addEventListener('click', closeTimetableModal);
                        modal.appendChild(closeBtn);

                        overlay.addEventListener('click', function(evt){
                            if (evt.target === overlay) {
                                closeTimetableModal();
                            }
                        });

                        var title = document.createElement('h2');
                        title.textContent = 'Stop: ' + stopName;
                        modal.appendChild(title);

                        var hasLines = Array.isArray(lines) && lines.length > 0;

                        if (hasLines) {
                            var summaryEl = document.createElement('div');
                            summaryEl.className = 'line-summary';
                            summaryEl.textContent = 'Select a line to view upcoming departures';
                            modal.appendChild(summaryEl);

                            var controls = document.createElement('div');
                            controls.className = 'line-controls';
                            modal.appendChild(controls);

                            var label = document.createElement('label');
                            label.className = 'line-select-label';
                            label.textContent = 'Choose line';
                            controls.appendChild(label);

                            var selectEl = document.createElement('select');
                            selectEl.className = 'line-select';
                            controls.appendChild(selectEl);

                            var toggleButton = document.createElement('button');
                            toggleButton.className = 'toggle-departures';
                            toggleButton.type = 'button';
                            toggleButton.textContent = 'Show full day';
                            controls.appendChild(toggleButton);

                            var departuresContainer = document.createElement('div');
                            departuresContainer.className = 'departures-container';
                            modal.appendChild(departuresContainer);

                            initializeTimetableInteraction(lines, selectEl, toggleButton, departuresContainer, summaryEl);
                        } else {
                            var noData = document.createElement('div');
                            noData.className = 'loading-large';
                            noData.innerHTML = '<div style="font-size: 16px; margin-bottom: 6px;">No timetable data available</div>' +
                                '<div style="font-size: 14px; color: #FFFFFF;">Try clicking on a different stop</div>';
                            modal.appendChild(noData);
                        }

                        document.body.appendChild(overlay);
                        timetableModalOverlay = overlay;

                        if (window.timetableState) {
                            var modalKeyHandler = function(evt) {
                                if (evt.key === 'Escape') {
                                    closeTimetableModal();
                                }
                            };
                            document.addEventListener('keydown', modalKeyHandler);
                            window.timetableState.modalKeyHandler = modalKeyHandler;
                        }

                        console.log('Timetable modal opened for:', stopName);
                    };

                    function initializeTimetableInteraction(lineList, selectEl, toggleButton, departuresContainer, summaryEl) {
                        if (!selectEl || !toggleButton || !departuresContainer) { return; }

                        var uniqueLines = Array.isArray(lineList) ? lineList.slice() : [];

                        uniqueLines.sort(function(a, b) {
                            var aNum = parseInt(a, 10);
                            var bNum = parseInt(b, 10);
                            if (!isNaN(aNum) && !isNaN(bNum)) {
                                return aNum - bNum;
                            }
                            return a.localeCompare(b);
                        });

                        uniqueLines.forEach(function(line) {
                            var option = document.createElement('option');
                            option.value = line;
                            option.textContent = 'Line ' + line;
                            selectEl.appendChild(option);
                        });

                        if (summaryEl) {
                            summaryEl.innerHTML = 'Lines: <strong>' + uniqueLines.join(', ') + '</strong>';
                        }

                        var updateToggleLabel = function() {
                            toggleButton.textContent = window.timetableState.showAll ? 'Show upcoming only' : 'Show full day';
                        };

                        var renderSelectedLine = function() {
                            var selectedLine = selectEl.value || uniqueLines[0];
                            if (!selectedLine) {
                                renderDeparturesList(departuresContainer, [], window.timetableState.showAll, false);
                                return;
                            }

                            var cached = window.timetableState.departuresByLine[selectedLine];
                            if (Array.isArray(cached)) {
                                var filtered = filterDeparturesForDisplay(cached, window.timetableState.showAll);
                                renderDeparturesList(departuresContainer, filtered, window.timetableState.showAll, cached.length > 0);
                                return;
                            }

                            renderDeparturesList(departuresContainer, null, window.timetableState.showAll, false);
                            ensureDeparturesLoaded(selectedLine, function(fetched) {
                                var filteredFetched = filterDeparturesForDisplay(fetched, window.timetableState.showAll);
                                renderDeparturesList(departuresContainer, filteredFetched, window.timetableState.showAll, fetched.length > 0);
                            });
                        };

                        selectEl.addEventListener('change', function() {
                            renderSelectedLine();
                        });

                        toggleButton.addEventListener('click', function() {
                            window.timetableState.showAll = !window.timetableState.showAll;
                            updateToggleLabel();
                            renderSelectedLine();
                        });

                        updateToggleLabel();

                        if (uniqueLines.length > 0) {
                            selectEl.value = uniqueLines[0];
                        }

                        renderSelectedLine();

                        if (window.timetableState) {
                            window.timetableState.modalElements = {
                                selectEl: selectEl,
                                toggleButton: toggleButton,
                                departuresContainer: departuresContainer,
                                summaryEl: summaryEl
                            };
                            window.timetableState.renderSelectedLine = renderSelectedLine;
                        }
                    }

                    function filterDeparturesForDisplay(departures, showAll) {
                        if (!Array.isArray(departures)) { return []; }
                        if (showAll) { return departures.slice(); }

                        var now = new Date();
                        var currentMinutes = now.getHours() * 60 + now.getMinutes();

                        return departures.filter(function(dep) {
                            var minutes = parseTimeToMinutes(dep ? dep.time : null);
                            return minutes === null || minutes >= currentMinutes;
                        });
                    }

                    function renderDeparturesList(container, departures, showAll, hasScheduled) {
                        if (!container) { return; }

                        if (departures === null) {
                            container.innerHTML = '<div style="text-align: center; color: #FEE715; font-style: italic; padding: 14px; background: rgba(30,30,60,0.8); border-radius: 10px; border: 2px solid #FEE715;">Loading departures...</div>';
                            return;
                        }

                        if (!departures || departures.length === 0) {
                            if (hasScheduled && !showAll) {
                                container.innerHTML = '<div style="text-align: center; color: #FEE715; font-style: italic; padding: 14px; background: rgba(30,30,60,0.8); border-radius: 10px; border: 2px solid #FEE715;">No upcoming departures. Tap "Show full day" to see earlier trips.</div>';
                            } else if (hasScheduled) {
                                container.innerHTML = '<div style="text-align: center; color: #FEE715; font-style: italic; padding: 14px; background: rgba(30,30,60,0.8); border-radius: 10px; border: 2px solid #FEE715;">No departures found for the selected line.</div>';
                            } else {
                                container.innerHTML = '<div style="text-align: center; color: #FEE715; font-style: italic; padding: 14px; background: rgba(30,30,60,0.8); border-radius: 10px; border: 2px solid #FEE715;">This line has no departures scheduled today.</div>';
                            }
                            return;
                        }

                        var html = departures.map(function(dep) {
                            var directionText = dep && dep.direction ? dep.direction : '—';
                            var brigadeText = dep && dep.brigade ? '<span style="display: block; font-size: 16px; color: #FFFFFF; margin-top: 4px;">Brigade ' + dep.brigade + '</span>' : '';
                            var timeText = dep && dep.time ? dep.time : '—';
                            return '<div class="departure-large" style="margin: 8px 0; padding: 12px; background: #1A1A2E; border-radius: 12px; border: 2px solid #FEE715;">' +
                                '<span style="display: block; font-size: 20px; font-weight: bold; color: #00FFB2;">' + timeText + '</span>' +
                                '<span style="display: block; font-size: 18px; color: #FEE715; margin-top: 4px;">Direction: ' + directionText + '</span>' +
                                brigadeText +
                                '</div>';
                        }).join('');

                        container.innerHTML = html;
                    }

                    function ensureDeparturesLoaded(line, onComplete) {
                        if (!line) { onComplete([]); return; }
                        if (window.timetableState.departuresByLine && window.timetableState.departuresByLine[line]) {
                            onComplete(window.timetableState.departuresByLine[line]);
                            return;
                        }

                        window.timetableState.pendingLine = line;

                        if (typeof bridge !== 'undefined' && bridge && typeof bridge.getLineDepartures === 'function') {
                            try {
                                bridge.getLineDepartures(window.timetableState.stopGroupId, window.timetableState.stopId, line);
                            } catch (err) {
                                console.error('Error invoking bridge.getLineDepartures:', err);
                                window.displayLineDepartures(line, []);
                            }
                        } else if (bridge && bridge.getLineDepartures) {
                            try {
                                bridge.getLineDepartures(window.timetableState.stopGroupId, window.timetableState.stopId, line);
                            } catch (err2) {
                                console.error('Bridge call failed; showing empty departures.', err2);
                                window.displayLineDepartures(line, []);
                            }
                        } else {
                            window.displayLineDepartures(line, []);
                        }

                        window.timetableState.departureCallback = onComplete;
                    }

                    window.displayLineDepartures = function(line, departures) {
                        if (!window.timetableState.departuresByLine) {
                            window.timetableState.departuresByLine = {};
                        }
                        window.timetableState.departuresByLine[line] = Array.isArray(departures) ? departures : [];

                        if (window.timetableState.pendingLine && window.timetableState.pendingLine !== line) {
                            return;
                        }

                        var callback = window.timetableState.departureCallback;
                        window.timetableState.pendingLine = null;
                        window.timetableState.departureCallback = null;

                        if (typeof callback === 'function') {
                            callback(window.timetableState.departuresByLine[line]);
                        }

                        if (window.timetableState && typeof window.timetableState.renderSelectedLine === 'function') {
                            window.timetableState.renderSelectedLine();
                        }
                    };

                    function parseTimeToMinutes(timeStr) {
                        if (!timeStr) { return null; }
                        var parts = timeStr.split(':');
                        if (parts.length < 2) { return null; }
                        var hours = parseInt(parts[0], 10);
                        var minutes = parseInt(parts[1], 10);
                        if (isNaN(hours) || isNaN(minutes)) { return null; }
                        return (hours * 60) + minutes;
                    }

                </script>
            </body>
            </html>
            """;
    }

    private void updateMapDisplay() {
        if (!isRealTimeMode) return;
        
        try {
            List<Vehicle> buses = dataService.getBuses();
            List<Vehicle> trams = dataService.getTrams();
            
            // Update buses
            if (showBusesCheckBox.isSelected() && !buses.isEmpty()) {
                String busesJson = dataService.getObjectMapper().writeValueAsString(buses);
                webEngine.executeScript("updateBuses(" + busesJson + ")");
                webEngine.executeScript("toggleLayer('buses', true)");
            } else {
                webEngine.executeScript("toggleLayer('buses', false)");
            }
            
            // Update trams
            if (showTramsCheckBox.isSelected() && !trams.isEmpty()) {
                String tramsJson = dataService.getObjectMapper().writeValueAsString(trams);
                webEngine.executeScript("updateTrams(" + tramsJson + ")");
                webEngine.executeScript("toggleLayer('trams', true)");
            } else {
                webEngine.executeScript("toggleLayer('trams', false)");
            }
            
        } catch (Exception e) {
            logger.error("Failed to update map display", e);
        }
    }

    private void updateStopsDisplay(List<TransportStop> stops) {
        if (!isStopsMode) return;
        
        try {
            logger.info("Updating stops display with {} stops", stops.size());
            if (stops.size() > 0) {
                logger.info("First stop: {}", stops.get(0));
            }
            
            String stopsJson = dataService.getObjectMapper().writeValueAsString(stops);
            logger.info("Stops JSON (first 200 chars): {}", stopsJson.substring(0, Math.min(200, stopsJson.length())));
            
            // Add a small delay to ensure JavaScript is ready
            Platform.runLater(() -> {
                try {
                    // Check if the function exists before calling it
                    Object result = webEngine.executeScript("typeof updateStops");
                    logger.info("updateStops function type: {}", result);
                    
                    if ("function".equals(result)) {
                        webEngine.executeScript("updateStops(" + stopsJson + ")");
                        logger.info("Stops update script executed");
                    } else {
                        logger.error("updateStops function not found in JavaScript, result: {}", result);
                        // Try to call it anyway, but handle the error
                        try {
                            webEngine.executeScript("updateStops(" + stopsJson + ")");
                        } catch (Exception e) {
                            logger.error("Failed to execute updateStops: {}", e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to update stops display in Platform.runLater", e);
                }
            });
        } catch (Exception e) {
            logger.error("Failed to update stops display", e);
        }
    }

    private void startPeriodicDataRefresh() {
        if (!isRealTimeMode) return;
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                dataService.refreshVehiclePositions();
                Platform.runLater(this::updateMapDisplay);
            } catch (Exception e) {
                logger.error("Failed to refresh data", e);
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    public Parent getRoot() {
        return root;
    }

    public void initialize() {
        // Nothing to initialize - menu is shown by default
    }

    public void shutdown() {
        logger.info("Shutting down MainWindow");
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (dataService != null) {
            dataService.shutdown();
        }
    }
    
    /**
     * Bridge class to handle timetable requests from JavaScript
     */
    public class TimetableBridge {
        public void getTimetableData(String stopGroupId, String stopId, String stopName) {
            logger.info("=== TIMETABLE BRIDGE CALLED ===");
            logger.info("Timetable request for stop {}/{}: {}", stopGroupId, stopId, stopName);
            logger.info("Current thread: {}", Thread.currentThread().getName());
            
            if (stopGroupId == null || stopGroupId.isEmpty() || stopId == null || stopId.isEmpty()) {
                logger.warn("Missing stop identifiers: busstopId='{}', busstopNr='{}'", stopGroupId, stopId);
                Platform.runLater(() -> {
                    try {
                        webEngine.executeScript("displayTimetable('" + stopName.replace("'", "\\'") + "', [])");
                    } catch (Exception ex) {
                        logger.error("Failed to display empty timetable for missing IDs", ex);
                    }
                });
                return;
            }

            // Run in background thread to avoid blocking UI
            ScheduledExecutorService executor = scheduler;
            if (executor == null || executor.isShutdown()) {
                executor = Executors.newScheduledThreadPool(1);
                scheduler = executor;
                logger.info("Scheduler was not available; created a new one for timetable requests");
            }

            if (executor != null && !executor.isShutdown()) {
                logger.info("Scheduler is available, submitting timetable request");
                executor.submit(() -> {
                    try {
                        List<String> lines = dataService.getLinesForStop(stopGroupId, stopId);
                        logger.info("Found {} lines for stop {}/{}", lines.size(), stopGroupId, stopId);

                        List<String> limitedLines = new ArrayList<>();
                        int maxLines = Math.min(lines.size(), 15);
                        for (int i = 0; i < maxLines; i++) {
                            String line = lines.get(i);
                            if (line != null && !line.isBlank()) {
                                limitedLines.add(line.trim());
                            }
                        }

                        TimetableLinesResponse payload = new TimetableLinesResponse();
                        payload.lines = limitedLines;

                        String payloadJson = dataService.getObjectMapper().writeValueAsString(payload);

                        Platform.runLater(() -> {
                            try {
                                webEngine.executeScript("window.displayTimetable('" + escapeForJs(stopName) + "', " + payloadJson + ")");
                            } catch (Exception e) {
                                logger.error("Failed to display timetable lines", e);
                            }
                        });
                    } catch (Exception e) {
                        logger.error("Failed to get lines for stop {}/{}", stopGroupId, stopId, e);
                        Platform.runLater(() -> {
                            try {
                                webEngine.executeScript("window.displayTimetable('" + escapeForJs(stopName) + "', {\"lines\":[]})");
                            } catch (Exception ex) {
                                logger.error("Failed to display error timetable", ex);
                            }
                        });
                    }
                });
            } else {
                logger.error("Scheduler is not available for timetable request" );
            }
        }

        public void getLineDepartures(String stopGroupId, String stopId, String line) {
            if (line == null || line.isBlank()) {
                logger.warn("Line identifier is missing for departures request");
                Platform.runLater(() -> sendDeparturesToWeb(line, new ArrayList<>())) ;
                return;
            }

            ScheduledExecutorService executor = scheduler;
            if (executor == null || executor.isShutdown()) {
                executor = Executors.newScheduledThreadPool(1);
                scheduler = executor;
                logger.info("Scheduler was not available; created a new one for departure requests");
            }

            if (executor != null && !executor.isShutdown()) {
                executor.submit(() -> {
                    try {
                        List<WarsawApiClient.DepartureTime> departures = dataService.getDepartureTimes(stopGroupId, stopId, line);
                        List<TimetableDepartureData> departurePayload = new ArrayList<>();

                        for (WarsawApiClient.DepartureTime departure : departures) {
                            TimetableDepartureData depData = new TimetableDepartureData();
                            depData.time = departure.getTime();
                            depData.direction = departure.getDirection();
                            depData.brigade = departure.getBrigade();
                            departurePayload.add(depData);
                        }

                        departurePayload.sort(MainWindow.this::compareDepartureTimes);
                        Platform.runLater(() -> sendDeparturesToWeb(line, departurePayload));
                    } catch (Exception e) {
                        logger.error("Failed to get departures for line {} at stop {}/{}", line, stopGroupId, stopId, e);
                        Platform.runLater(() -> sendDeparturesToWeb(line, new ArrayList<>()));
                    }
                });
            } else {
                logger.error("Scheduler is not available for departures request");
            }
        }
    }

    private int compareDepartureTimes(TimetableDepartureData first, TimetableDepartureData second) {
        LocalTime firstTime = parseNullableTime(first != null ? first.time : null);
        LocalTime secondTime = parseNullableTime(second != null ? second.time : null);

        if (firstTime != null && secondTime != null) {
            return firstTime.compareTo(secondTime);
        }
        if (firstTime == null && secondTime == null) {
            return 0;
        }
        if (firstTime == null) {
            return 1;
        }
        return -1;
    }

    private void sendDeparturesToWeb(String line, List<TimetableDepartureData> departures) {
        try {
            String json = dataService.getObjectMapper().writeValueAsString(departures);
            webEngine.executeScript("window.displayLineDepartures('" + escapeForJs(line) + "', " + json + ")");
        } catch (Exception e) {
            logger.error("Failed to send departures to WebView for line {}", line, e);
        }
    }

    private String escapeForJs(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    private LocalTime parseNullableTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException ex) {
            logger.debug("Unable to parse time value '{}': {}", value, ex.getMessage());
            return null;
        }
    }
    
    /**
     * Data class for timetable departure information
     */
    static class TimetableDepartureData {
        public String time;
        public String direction;
        public String brigade;
    }

    /**
     * Data class for line list payload
     */
    static class TimetableLinesResponse {
        public List<String> lines;
    }
}
