package com.warsaw.transport.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a public transport vehicle (bus, tram, etc.) in Warsaw
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Vehicle {
    
    @JsonProperty("VehicleNumber")
    private String vehicleNumber;
    
    @JsonProperty("Lines")
    private String line;
    
    @JsonProperty("Lon")
    private double longitude;
    
    @JsonProperty("Lat")
    private double latitude;
    
    @JsonProperty("Time")
    private String timeString;
    
    @JsonProperty("Brigade")
    private String brigade;

    private VehicleType type;
    
    public enum VehicleType {
        BUS, TRAM, METRO
    }
    
    // Default constructor for JSON deserialization
    public Vehicle() {}
    
    public Vehicle(String vehicleNumber, String line, double longitude, double latitude, VehicleType type) {
        this.vehicleNumber = vehicleNumber;
        this.line = line;
        this.longitude = longitude;
        this.latitude = latitude;
        this.type = type;
    }
    
    // Getters and Setters
    public String getVehicleNumber() {
        return vehicleNumber;
    }
    
    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }
    
    public String getLine() {
        return line;
    }
    
    public void setLine(String line) {
        this.line = line;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    
    public String getTimeString() {
        return timeString;
    }
    
    public void setTimeString(String timeString) {
        this.timeString = timeString;
    }
    
    public String getBrigade() {
        return brigade;
    }
    
    public void setBrigade(String brigade) {
        this.brigade = brigade;
    }
    
    public VehicleType getType() {
        return type;
    }
    
    public void setType(VehicleType type) {
        this.type = type;
    }
    
    @Override
    public String toString() {
        return String.format("Vehicle{number='%s', line='%s', lat=%.6f, lon=%.6f, type=%s}", 
                           vehicleNumber, line, latitude, longitude, type);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Vehicle vehicle = (Vehicle) obj;
        return vehicleNumber != null ? vehicleNumber.equals(vehicle.vehicleNumber) : vehicle.vehicleNumber == null;
    }
    
    @Override
    public int hashCode() {
        return vehicleNumber != null ? vehicleNumber.hashCode() : 0;
    }
}
