package com.warsaw.transport.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warsaw.transport.model.TransportStop;
import com.warsaw.transport.model.Vehicle;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Client for accessing Warsaw public transport API
 */
public class WarsawApiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(WarsawApiClient.class);
    
    // Base URL for Warsaw Open Data API
    private static final String BASE_URL = "https://api.um.warszawa.pl/api/action";
    
    // Common parameters
    private static final String RESOURCE_ID_BUSES_1 = "f2e5503e-927d-4ad3-9500-4ab9e55deb59";
    private static final String RESOURCE_ID_TRAMS = "13ce234d-3a8e-44ad-8c3c-6e77b3f99816";
    private static final String RESOURCE_ID_LINES = "88cd555f-6f31-43ca-9de4-66c479ad5942";
    private static final String RESOURCE_ID_TIMETABLE = "e923fa0e-d96c-43f9-ae6e-60518c9f3238";
    
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String apikey;
    
    public WarsawApiClient() {
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper();
        this.apikey = "1027ad84-8a3e-49fa-9ddd-01956a251694"; // Built-in API key
    }
    
    public WarsawApiClient(String apikey) {
        this();
        this.apikey = apikey;
    }
    
    /**
     * Set the API key for authenticated requests
     */
    public void setApikey(String apikey) {
        this.apikey = apikey;
    }
    
    /**
     * Get real-time positions of all buses
     */
    public List<Vehicle> getBusPositions() throws IOException, ApiException, ParseException {
        List<Vehicle> buses = getVehiclePositions(RESOURCE_ID_BUSES_1, Vehicle.VehicleType.BUS, 1);
        logger.info("Retrieved {} bus positions", buses.size());
        return buses;
    }
    
    /**
     * Get real-time positions of all trams
     */
    public List<Vehicle> getTramPositions() throws IOException, ApiException, ParseException {
        List<Vehicle> trams = getVehiclePositions(RESOURCE_ID_TRAMS, Vehicle.VehicleType.TRAM, 2);
        logger.info("Retrieved {} tram positions", trams.size());
        return trams;
    }
    
    /**
     * Get all public transport stops  
     */
    public List<TransportStop> getTransportStops() throws IOException, ApiException, ParseException {
       
       
       
        String url = buildUrlForStops();
        logger.info("Fetching transport stops from: {}", url);
        
        try (CloseableHttpResponse response = httpClient.execute(new HttpGet(url))) {
            
            String responseBody = EntityUtils.toString(response.getEntity());
            logger.info("Stops API response (first 500 chars): {}", responseBody.substring(0, Math.min(500, responseBody.length())));
            
            if (response.getCode() != 200) {
                throw new ApiException("Failed to fetch stops: HTTP " + response.getCode());
            }
            
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode resultNode = rootNode.get("result");
            
            if (resultNode == null || !resultNode.isArray()) {
                throw new ApiException("Invalid API response format - result not an array");
            }
            
            List<TransportStop> stops = new ArrayList<>();
            for (JsonNode stopNode : resultNode) {
                try {
                    // Check if this is the expected format with "values" array
                    if (stopNode.has("values") && stopNode.get("values").isArray()) {
                        // Parse the "values" format where each stop has a values array
                        JsonNode valuesNode = stopNode.get("values");
                        TransportStop stop = new TransportStop();
                        
                        for (JsonNode valueNode : valuesNode) {
                            if (valueNode.has("key") && valueNode.has("value")) {
                                String key = valueNode.get("key").asText();
                                String value = valueNode.get("value").asText();
                                
                                switch (key) {
                                    case "zespol":
                                        stop.setStopGroupId(value);
                                        break;
                                    case "slupek":
                                        stop.setStopId(value);
                                        break;
                                    case "nazwa_zespolu":
                                        stop.setStopGroupName(value);
                                        break;
                                    case "szer_geo":
                                        try {
                                            stop.setLatitude(Double.parseDouble(value));
                                        } catch (NumberFormatException e) {
                                            logger.warn("Invalid latitude: {}", value);
                                        }
                                        break;
                                    case "dlug_geo":
                                        try {
                                            stop.setLongitude(Double.parseDouble(value));
                                        } catch (NumberFormatException e) {
                                            logger.warn("Invalid longitude: {}", value);
                                        }
                                        break;
                                }
                            }
                        }
                        
                        // Only add stops with valid coordinates
                        if (stop.getLatitude() != 0 && stop.getLongitude() != 0) {
                            stops.add(stop);
                            // Log first few stops for debugging
                            if (stops.size() <= 3) {
                                logger.info("Parsed stop {}: {}", stops.size(), stop);
                            }
                        }
                    } else {
                        // Try direct parsing as before
                        TransportStop stop = objectMapper.treeToValue(stopNode, TransportStop.class);
                        if (stop.getLatitude() != 0 && stop.getLongitude() != 0) {
                            stops.add(stop);
                            // Log first few stops for debugging
                            if (stops.size() <= 3) {
                                logger.info("Parsed stop {}: {}", stops.size(), stop);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Failed to parse stop: {}", stopNode, e);
                }
            }
            
            logger.info("Retrieved {} transport stops", stops.size());
            if (stops.size() > 0) {
                logger.info("First stop details: {}", stops.get(0));
            }
            return stops;
            
        } catch (Exception e) {
            logger.error("Failed to fetch transport stops", e);
            throw new ApiException("Failed to fetch transport stops", e);
        }
    }
    
    /**
     * Generic method to get vehicle positions
     */
    private List<Vehicle> getVehiclePositions(String resourceId, Vehicle.VehicleType vehicleType, int type) 
            throws IOException, ApiException, ParseException {
        
        String url = buildUrl("busestrams_get", resourceId, type);
        
        try (CloseableHttpResponse response = httpClient.execute(new HttpPost(url))) {
            String jsonResponse = EntityUtils.toString(response.getEntity());
            
            if (response.getCode() != 200) {
                throw new ApiException("API request failed with status: " + response.getCode());
            }
            
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode resultNode = rootNode.path("result");
            
            // Check if this is an error response
            if (resultNode.isTextual()) {
                throw new ApiException("API error: " + resultNode.asText());
            }
            
            // For busestrams_get, data is directly in result array
            if (!resultNode.isArray()) {
                // Log the actual response for debugging
                logger.error("API returned non-array result: {}", jsonResponse);
                throw new ApiException("Invalid API response format - result not an array");
            }
            
            List<Vehicle> vehicles = objectMapper.convertValue(
                resultNode, 
                new TypeReference<List<Vehicle>>() {}
            );
            
            // Set the vehicle type for all vehicles
            vehicles.forEach(vehicle -> vehicle.setType(vehicleType));
            
            return vehicles;
        }
    }
    
    /**
     * Build API URL with parameters
     */
    private String buildUrl(String action, String resourceId) {
        StringBuilder url = new StringBuilder(BASE_URL)
            .append("/").append(action)
            .append("/?resource_id=").append(resourceId);
        
        if (apikey != null && !apikey.isEmpty()) {
            url.append("&apikey=").append(encodeParam(apikey));
        }
        
        return url.toString();
    }
    
    /**
     * Build API URL with parameters including type
     */
    private String buildUrl(String action, String resourceId, int type) {
        StringBuilder url = new StringBuilder(BASE_URL)
            .append("/").append(action)
            .append("/?resource_id=").append(resourceId);
        
        if (apikey != null && !apikey.isEmpty()) {
            url.append("&apikey=").append(URLEncoder.encode(apikey, StandardCharsets.UTF_8));
        }
        
        url.append("&type=").append(type);
        
        return url.toString();
    }
    
    /**
     * Build URL for transport stops API
     */
    private String buildUrlForStops() {
        StringBuilder url = new StringBuilder(BASE_URL)
            .append("/dbtimetable_get/")
            .append("?id=ab75c33d-3a26-4342-b36a-6e5fef0a3ac3");
        
        if (apikey != null && !apikey.isEmpty()) {
            url.append("&apikey=").append(URLEncoder.encode(apikey, StandardCharsets.UTF_8));
        }
        
        return url.toString();
    }
    
    /**
     * Close the HTTP client when done
     */
    public void close() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
    }
    
    /**
     * Get available lines for a specific stop
     */
    public List<String> getLinesForStop(String stopGroupId, String stopId) throws IOException, ApiException, ParseException {
        String url = buildUrlForLines(stopGroupId, stopId);
        logger.info("Fetching lines for stop {}/{} from: {}", stopGroupId, stopId, url);
        
        try (CloseableHttpResponse response = httpClient.execute(new HttpGet(url))) {
            String responseBody = EntityUtils.toString(response.getEntity());
            
            if (response.getCode() != 200) {
                throw new ApiException("Failed to fetch lines: HTTP " + response.getCode());
            }
            
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode resultNode = rootNode.get("result");
            
            if (resultNode == null || !resultNode.isArray()) {
                throw new ApiException("Invalid API response format - result not an array");
            }
            
            List<String> lines = new ArrayList<>();
            for (JsonNode lineNode : resultNode) {
                try {
                    if (lineNode.has("values") && lineNode.get("values").isArray()) {
                        extractLinesFromArray(lineNode.get("values"), lines);
                    } else if (lineNode.isArray()) {
                        extractLinesFromArray(lineNode, lines);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to parse line: {}", lineNode, e);
                }
            }
            
            logger.info("Retrieved {} lines for stop {}/{}", lines.size(), stopGroupId, stopId);
            return lines;
            
        } catch (Exception e) {
            logger.error("Failed to fetch lines for stop {}/{}", stopGroupId, stopId, e);
            throw new ApiException("Failed to fetch lines for stop", e);
        }
    }
    
    /**
     * Get departure times for a specific line at a specific stop
     */
    public List<DepartureTime> getDepartureTimes(String stopGroupId, String stopId, String line) throws IOException, ApiException, ParseException {
        String url = buildUrlForTimetable(stopGroupId, stopId, line);
        logger.info("Fetching departure times for line {} at stop {}/{} from: {}", line, stopGroupId, stopId, url);
        
        try (CloseableHttpResponse response = httpClient.execute(new HttpGet(url))) {
            String responseBody = EntityUtils.toString(response.getEntity());
            
            if (response.getCode() != 200) {
                throw new ApiException("Failed to fetch departure times: HTTP " + response.getCode());
            }
            
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode resultNode = rootNode.get("result");
            
            if (resultNode == null || !resultNode.isArray()) {
                throw new ApiException("Invalid API response format - result not an array");
            }
            
            List<DepartureTime> departures = new ArrayList<>();
            for (JsonNode departureNode : resultNode) {
                try {
                    DepartureTime departure = new DepartureTime();

                    if (departureNode.has("values") && departureNode.get("values").isArray()) {
                        // API variant where each entry is an object containing "values" array
                        extractDepartureFromArray(departureNode.get("values"), departure);
                    } else if (departureNode.isArray()) {
                        // API variant where each entry is already an array of key/value objects
                        extractDepartureFromArray(departureNode, departure);
                    }

                    if (departure.getTime() != null && !departure.getTime().isEmpty()) {
                        departures.add(departure);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to parse departure: {}", departureNode, e);
                }
            }
            
            logger.info("Retrieved {} departure times for line {} at stop {}/{}", departures.size(), line, stopGroupId, stopId);
            return departures;
            
        } catch (Exception e) {
            logger.error("Failed to fetch departure times for line {} at stop {}/{}", line, stopGroupId, stopId, e);
            throw new ApiException("Failed to fetch departure times", e);
        }
    }
    
    /**
     * Build URL for getting lines at a stop
     */
    private String buildUrlForLines(String stopGroupId, String stopId) {
        return String.format("%s/dbtimetable_get/?id=%s&busstopId=%s&busstopNr=%s&apikey=%s",
                BASE_URL,
                RESOURCE_ID_LINES,
                encodeParam(stopGroupId),
                encodeParam(stopId),
                encodeParam(apikey));
    }

    /**
     * Build URL for getting departure times
     */
    private String buildUrlForTimetable(String stopGroupId, String stopId, String line) {
        return String.format("%s/dbtimetable_get/?id=%s&busstopId=%s&busstopNr=%s&line=%s&apikey=%s",
                BASE_URL,
                RESOURCE_ID_TIMETABLE,
                encodeParam(stopGroupId),
                encodeParam(stopId),
                encodeParam(line),
                encodeParam(apikey));
    }

    private static String encodeParam(String value) {
        return value == null ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void extractDepartureFromArray(JsonNode arrayNode, DepartureTime departure) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return;
        }

        for (JsonNode valueNode : arrayNode) {
            if (valueNode.has("key") && valueNode.has("value")) {
                String key = valueNode.get("key").asText();
                String value = valueNode.get("value").isNull() ? null : valueNode.get("value").asText();

                switch (key) {
                    case "brygada":
                        departure.setBrigade(value);
                        break;
                    case "kierunek":
                        departure.setDirection(value);
                        break;
                    case "trasa":
                        departure.setRoute(value);
                        break;
                    case "czas":
                        departure.setTime(value);
                        break;
                    case "symbol_1":
                        departure.setSymbol1(value);
                        break;
                    case "symbol_2":
                        departure.setSymbol2(value);
                        break;
                }
            }
        }
    }

    private void extractLinesFromArray(JsonNode arrayNode, List<String> lines) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return;
        }

        for (JsonNode valueNode : arrayNode) {
            if (valueNode.has("key") && valueNode.has("value")) {
                String key = valueNode.get("key").asText();
                if ("linia".equals(key)) {
                    lines.add(valueNode.get("value").asText());
                    break;
                }
            }
        }
    }
    
    /**
     * Data class for departure times
     */
    public static class DepartureTime {
        private String brigade;
        private String direction;
        private String route;
        private String time;
        private String symbol1;
        private String symbol2;
        
        // Getters and setters
        public String getBrigade() { return brigade; }
        public void setBrigade(String brigade) { this.brigade = brigade; }
        
        public String getDirection() { return direction; }
        public void setDirection(String direction) { this.direction = direction; }
        
        public String getRoute() { return route; }
        public void setRoute(String route) { this.route = route; }
        
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
        
        public String getSymbol1() { return symbol1; }
        public void setSymbol1(String symbol1) { this.symbol1 = symbol1; }
        
        public String getSymbol2() { return symbol2; }
        public void setSymbol2(String symbol2) { this.symbol2 = symbol2; }
        
        @Override
        public String toString() {
            return String.format("DepartureTime{time='%s', direction='%s', brigade='%s'}", time, direction, brigade);
        }
    }
    
    /**
     * Custom exception for API-related errors
     */
    public static class ApiException extends Exception {
        public ApiException(String message) {
            super(message);
        }
        
        public ApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
