package com.warsaw.transport.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    // Default constructor for JSON deserialization
    public TransportStop() { }
    
    public TransportStop(String stopId, String stopGroupId, String stopGroupName, 
                        double latitude, double longitude) {
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
    
    @Override
    public String toString() {
        return String.format("TransportStop{id='%s', name='%s', lat=%.6f, lon=%.6f}",
                           stopId, stopGroupName, latitude, longitude);
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
