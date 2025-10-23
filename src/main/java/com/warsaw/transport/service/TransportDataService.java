package com.warsaw.transport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warsaw.transport.api.WarsawApiClient;
import com.warsaw.transport.model.Vehicle;
import com.warsaw.transport.model.TransportStop;
import org.apache.hc.core5.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Service for managing transport data - handles caching, updates, and data transformations
 */
public class TransportDataService {
    
    private static final Logger logger = LoggerFactory.getLogger(TransportDataService.class);
    
    private final WarsawApiClient apiClient;
    private final ObjectMapper objectMapper;
    private final ReentrantReadWriteLock dataLock = new ReentrantReadWriteLock();
    
    // Cached data
    private List<Vehicle> buses = new ArrayList<>();
    private List<Vehicle> trams = new ArrayList<>();
    private List<TransportStop> stops = new ArrayList<>();
    private LocalDateTime lastBusUpdate;
    private LocalDateTime lastTramUpdate;
    private LocalDateTime lastStopsUpdate;
    
    // Statistics
    private final ConcurrentHashMap<String, Integer> lineVehicleCount = new ConcurrentHashMap<>();
    
    public TransportDataService() {
        this.apiClient = new WarsawApiClient();
        this.objectMapper = new ObjectMapper();
    }
    
    public TransportDataService(String apiKey) {
        this.apiClient = new WarsawApiClient(apiKey);
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Set the API key for the Warsaw API
     */
    public void setApiKey(String apiKey) {
        apiClient.setApikey(apiKey);
    }
    
    /**
     * Refresh all data (buses, trams, stops) asynchronously
     */
    public CompletableFuture<Void> refreshAllDataAsync() {
        logger.info("Starting async refresh of all transport data");
        
        CompletableFuture<List<Vehicle>> buseFuture = apiClient.getBusPositionsAsync();
        CompletableFuture<List<Vehicle>> tramFuture = apiClient.getTramPositionsAsync();
        CompletableFuture<List<TransportStop>> stopsFuture = apiClient.getTransportStopsAsync();
        
        return CompletableFuture.allOf(buseFuture, tramFuture, stopsFuture)
            .thenRun(() -> {
                try {
                    dataLock.writeLock().lock();
                    
                    this.buses = buseFuture.get();
                    this.trams = tramFuture.get();
                    this.stops = stopsFuture.get();
                    
                    this.lastBusUpdate = LocalDateTime.now();
                    this.lastTramUpdate = LocalDateTime.now();
                    this.lastStopsUpdate = LocalDateTime.now();
                    
                    updateStatistics();
                    
                    logger.info("Successfully refreshed all data - Buses: {}, Trams: {}, Stops: {}", 
                               buses.size(), trams.size(), stops.size());
                    
                } catch (Exception e) {
                    logger.error("Failed to refresh all data", e);
                    throw new RuntimeException("Data refresh failed", e);
                } finally {
                    dataLock.writeLock().unlock();
                }
            });
    }
    
    /**
     * Refresh all data synchronously
     */
    public void refreshAllData() throws IOException, WarsawApiClient.ApiException, ParseException {
        logger.info("Starting synchronous refresh of all transport data");
        
        try {
            dataLock.writeLock().lock();
            
            this.buses = apiClient.getBusPositions();
            this.trams = apiClient.getTramPositions();
            this.stops = apiClient.getTransportStops();
            
            this.lastBusUpdate = LocalDateTime.now();
            this.lastTramUpdate = LocalDateTime.now();
            this.lastStopsUpdate = LocalDateTime.now();
            
            updateStatistics();
            
            logger.info("Successfully refreshed all data - Buses: {}, Trams: {}, Stops: {}", 
                       buses.size(), trams.size(), stops.size());
            
        } finally {
            dataLock.writeLock().unlock();
        }
    }
    
    /**
     * Refresh only vehicle positions (buses and trams)
     */
    public CompletableFuture<Void> refreshVehiclePositionsAsync() {
        logger.info("Refreshing vehicle positions");
        
        CompletableFuture<List<Vehicle>> buseFuture = apiClient.getBusPositionsAsync();
        CompletableFuture<List<Vehicle>> tramFuture = apiClient.getTramPositionsAsync();
        
        return CompletableFuture.allOf(buseFuture, tramFuture)
            .thenRun(() -> {
                try {
                    dataLock.writeLock().lock();
                    
                    this.buses = buseFuture.get();
                    this.trams = tramFuture.get();
                    
                    this.lastBusUpdate = LocalDateTime.now();
                    this.lastTramUpdate = LocalDateTime.now();
                    
                    updateStatistics();
                    
                    logger.info("Vehicle positions updated - Buses: {}, Trams: {}", 
                               buses.size(), trams.size());
                    
                } catch (Exception e) {
                    logger.error("Failed to refresh vehicle positions", e);
                    throw new RuntimeException("Vehicle position refresh failed", e);
                } finally {
                    dataLock.writeLock().unlock();
                }
            });
    }
    
    /**
     * Get current list of buses (thread-safe)
     */
    public List<Vehicle> getBuses() {
        try {
            dataLock.readLock().lock();
            return new ArrayList<>(buses);
        } finally {
            dataLock.readLock().unlock();
        }
    }
    
    /**
     * Get current list of trams (thread-safe)
     */
    public List<Vehicle> getTrams() {
        try {
            dataLock.readLock().lock();
            return new ArrayList<>(trams);
        } finally {
            dataLock.readLock().unlock();
        }
    }
    
    /**
     * Get current list of transport stops (thread-safe)
     */
    public List<TransportStop> getStops() {
        try {
            dataLock.readLock().lock();
            return new ArrayList<>(stops);
        } finally {
            dataLock.readLock().unlock();
        }
    }
    
    /**
     * Get vehicles for a specific line
     */
    public List<Vehicle> getVehiclesForLine(String lineNumber) {
        List<Vehicle> result = new ArrayList<>();
        
        try {
            dataLock.readLock().lock();
            
            buses.stream()
                .filter(bus -> lineNumber.equals(bus.getLine()))
                .forEach(result::add);
                
            trams.stream()
                .filter(tram -> lineNumber.equals(tram.getLine()))
                .forEach(result::add);
                
        } finally {
            dataLock.readLock().unlock();
        }
        
        return result;
    }
    
    /**
     * Get transport stops near a given location
     */
    public List<TransportStop> getStopsNearLocation(double latitude, double longitude, double radiusKm) {
        List<TransportStop> nearbyStops = new ArrayList<>();
        
        try {
            dataLock.readLock().lock();
            
            for (TransportStop stop : stops) {
                double distance = calculateDistance(latitude, longitude, stop.getLatitude(), stop.getLongitude());
                if (distance <= radiusKm) {
                    nearbyStops.add(stop);
                }
            }
            
        } finally {
            dataLock.readLock().unlock();
        }
        
        return nearbyStops;
    }
    
    /**
     * Convert vehicles list to JSON string
     */
    public String vehiclesToJson(List<Vehicle> vehicles) {
        try {
            return objectMapper.writeValueAsString(vehicles);
        } catch (JsonProcessingException e) {
            logger.error("Failed to convert vehicles to JSON", e);
            return "[]";
        }
    }
    
    /**
     * Convert stops list to JSON string
     */
    public String stopsToJson(List<TransportStop> stops) {
        try {
            return objectMapper.writeValueAsString(stops);
        } catch (JsonProcessingException e) {
            logger.error("Failed to convert stops to JSON", e);
            return "[]";
        }
    }
    
    /**
     * Get statistics about the current data
     */
    public DataStatistics getStatistics() {
        try {
            dataLock.readLock().lock();
            
            return new DataStatistics(
                buses.size(),
                trams.size(),
                stops.size(),
                lineVehicleCount.size(),
                lastBusUpdate,
                lastTramUpdate,
                lastStopsUpdate
            );
            
        } finally {
            dataLock.readLock().unlock();
        }
    }
    
    /**
     * Update internal statistics
     */
    private void updateStatistics() {
        lineVehicleCount.clear();
        
        // Count vehicles per line
        buses.forEach(bus -> 
            lineVehicleCount.merge(bus.getLine(), 1, Integer::sum));
        trams.forEach(tram -> 
            lineVehicleCount.merge(tram.getLine(), 1, Integer::sum));
    }
    
    /**
     * Calculate distance between two points using Haversine formula
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLatRad = Math.toRadians(lat2 - lat1);
        double deltaLonRad = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return 6371.0 * c; // Earth's radius in kilometers
    }
    
    /**
     * Check if data is fresh (within specified minutes)
     */
    public boolean isDataFresh(int maxAgeMinutes) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(maxAgeMinutes);
        
        try {
            dataLock.readLock().lock();
            
            return (lastBusUpdate != null && lastBusUpdate.isAfter(threshold)) ||
                   (lastTramUpdate != null && lastTramUpdate.isAfter(threshold));
                   
        } finally {
            dataLock.readLock().unlock();
        }
    }

    /**
     * Get transport stops
     */
    public List<TransportStop> getTransportStops() {
        try {
            dataLock.readLock().lock();
            return new ArrayList<>(stops);
        } finally {
            dataLock.readLock().unlock();
        }
    }

    /**
     * Force refresh of transport stops only (without pulling vehicle data)
     */
    public List<TransportStop> refreshTransportStops() throws IOException, WarsawApiClient.ApiException, ParseException {
        logger.info("Refreshing transport stops only");
        List<TransportStop> latestStops = apiClient.getTransportStops();

        try {
            dataLock.writeLock().lock();
            this.stops = latestStops;
            this.lastStopsUpdate = LocalDateTime.now();
            logger.info("Transport stops refreshed: {} entries", this.stops.size());
            return new ArrayList<>(this.stops);
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    /**
     * Get the ObjectMapper instance
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
    
    /**
     * Get available lines for a specific stop
     */
    public List<String> getLinesForStop(String stopGroupId, String stopId) {
        try {
            return apiClient.getLinesForStop(stopGroupId, stopId);
        } catch (Exception e) {
            logger.error("Failed to get lines for stop {}/{}", stopGroupId, stopId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get departure times for a specific line at a specific stop
     */
    public List<WarsawApiClient.DepartureTime> getDepartureTimes(String stopGroupId, String stopId, String line) {
        try {
            return apiClient.getDepartureTimes(stopGroupId, stopId, line);
        } catch (Exception e) {
            logger.error("Failed to get departure times for line {} at stop {}/{}", line, stopGroupId, stopId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Shutdown the service and cleanup resources
     */
    public void shutdown() {
        try {
            apiClient.close();
            logger.info("TransportDataService shutdown completed");
        } catch (IOException e) {
            logger.error("Error during shutdown", e);
        }
    }
    
    /**
     * Data statistics container
     */
    public static class DataStatistics {
        private final int busCount;
        private final int tramCount;
        private final int stopCount;
        private final int activeLines;
        private final LocalDateTime lastBusUpdate;
        private final LocalDateTime lastTramUpdate;
        private final LocalDateTime lastStopsUpdate;
        
        public DataStatistics(int busCount, int tramCount, int stopCount, int activeLines,
                            LocalDateTime lastBusUpdate, LocalDateTime lastTramUpdate, 
                            LocalDateTime lastStopsUpdate) {
            this.busCount = busCount;
            this.tramCount = tramCount;
            this.stopCount = stopCount;
            this.activeLines = activeLines;
            this.lastBusUpdate = lastBusUpdate;
            this.lastTramUpdate = lastTramUpdate;
            this.lastStopsUpdate = lastStopsUpdate;
        }
        
        // Getters
        public int getBusCount() { return busCount; }
        public int getTramCount() { return tramCount; }
        public int getStopCount() { return stopCount; }
        public int getActiveLines() { return activeLines; }
        public LocalDateTime getLastBusUpdate() { return lastBusUpdate; }
        public LocalDateTime getLastTramUpdate() { return lastTramUpdate; }
        public LocalDateTime getLastStopsUpdate() { return lastStopsUpdate; }
        
        @Override
        public String toString() {
            return String.format("DataStats{buses=%d, trams=%d, stops=%d, lines=%d}", 
                               busCount, tramCount, stopCount, activeLines);
        }
    }
}
