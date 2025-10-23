package com.warsaw.transport.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents a public transport stop in Warsaw
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransportStop {
    
    @JsonProperty("zespol")
    private String stopGroupId;
    
    @JsonProperty("slupek")
    private String stopId;
    
    @JsonProperty("nazwa_zespolu")
    private String stopGroupName;
    
    @JsonProperty("szer_geo")
    private double latitude;
    
    @JsonProperty("dlug_geo")
    private double longitude;
    
    private List<String> lines;
    private String district;
    private boolean isActive;
    
    // Default constructor for JSON deserialization
    public TransportStop() {
        this.lines = new ArrayList<>();
        this.isActive = true;
    }
    
    public TransportStop(String stopId, String stopGroupId, String stopGroupName, 
                        double latitude, double longitude) {
        this();
        this.stopId = stopId;
        this.stopGroupId = stopGroupId;
        this.stopGroupName = stopGroupName;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    // Getters and Setters
    public String getStopGroupId() {
        return stopGroupId;
    }
    
    public void setStopGroupId(String stopGroupId) {
        this.stopGroupId = stopGroupId;
    }
    
    public String getStopId() {
        return stopId;
    }
    
    public void setStopId(String stopId) {
        this.stopId = stopId;
    }
    
    public String getStopGroupName() {
        return stopGroupName;
    }
    
    public void setStopGroupName(String stopGroupName) {
        this.stopGroupName = stopGroupName;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    public List<String> getLines() {
        return lines;
    }
    
    public void setLines(List<String> lines) {
        this.lines = lines != null ? lines : new ArrayList<>();
    }
    
    public void addLine(String line) {
        if (line != null && !this.lines.contains(line)) {
            this.lines.add(line);
        }
    }
    
    public String getDistrict() {
        return district;
    }
    
    public void setDistrict(String district) {
        this.district = district;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    /**
     * Calculate distance to another stop in kilometers using Haversine formula
     */
    public double distanceTo(TransportStop other) {
        if (other == null) return Double.MAX_VALUE;
        
        double lat1Rad = Math.toRadians(this.latitude);
        double lat2Rad = Math.toRadians(other.latitude);
        double deltaLatRad = Math.toRadians(other.latitude - this.latitude);
        double deltaLonRad = Math.toRadians(other.longitude - this.longitude);
        
        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return 6371.0 * c; // Earth's radius in kilometers
    }
    
    @Override
    public String toString() {
        return String.format("TransportStop{id='%s', name='%s', lat=%.6f, lon=%.6f, lines=%s}", 
                           stopId, stopGroupName, latitude, longitude, lines);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TransportStop stop = (TransportStop) obj;
        return stopId != null ? stopId.equals(stop.stopId) : stop.stopId == null;
    }
    
    @Override
    public int hashCode() {
        return stopId != null ? stopId.hashCode() : 0;
    }
}
