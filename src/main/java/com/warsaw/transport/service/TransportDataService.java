package com.warsaw.transport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.warsaw.transport.api.WarsawApiClient;
import com.warsaw.transport.model.Vehicle;
import com.warsaw.transport.model.TransportStop;
import org.apache.hc.core5.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    
    public TransportDataService() {
        this.apiClient = new WarsawApiClient();
        this.objectMapper = new ObjectMapper();
    }
    
    public TransportDataService(String apiKey) {
        this.apiClient = new WarsawApiClient(apiKey);
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Refresh only vehicle positions (buses and trams)
     */
    public void refreshVehiclePositions() throws IOException, WarsawApiClient.ApiException, ParseException {
        logger.info("Refreshing vehicle positions");

        try {
            dataLock.writeLock().lock();
            this.buses = apiClient.getBusPositions();
            this.trams = apiClient.getTramPositions();
            logger.info("Vehicle positions updated - Buses: {}, Trams: {}", buses.size(), trams.size());
        } finally {
            dataLock.writeLock().unlock();
        }
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
}
