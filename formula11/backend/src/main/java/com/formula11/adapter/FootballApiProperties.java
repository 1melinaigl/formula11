package com.formula11.adapter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app.football-api")
public class FootballApiProperties {
    private String apiKey = "";
    private String baseUrl = "https://api.football-data.org/v4";
    private String apiKeyHeader = "X-Auth-Token";
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(10);
    private Duration responseTimeout = Duration.ofSeconds(15);
    private Duration cacheTtl = Duration.ofMinutes(15);
    private long cacheMaximumSize = 500;
    private Duration minimumRequestInterval = Duration.ZERO;
    private Map<String, String> competitionCodes = Map.of(
            "PREMIER_LEAGUE", "PL",
            "BUNDESLIGA", "BL1",
            "LA_LIGA", "PD",
            "SERIE_A", "SA",
            "LIGUE_1", "FL1");

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKeyHeader() { return apiKeyHeader; }
    public void setApiKeyHeader(String apiKeyHeader) { this.apiKeyHeader = apiKeyHeader; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
    public Duration getResponseTimeout() { return responseTimeout; }
    public void setResponseTimeout(Duration responseTimeout) { this.responseTimeout = responseTimeout; }
    public Duration getCacheTtl() { return cacheTtl; }
    public void setCacheTtl(Duration cacheTtl) { this.cacheTtl = cacheTtl; }
    public long getCacheMaximumSize() { return cacheMaximumSize; }
    public void setCacheMaximumSize(long cacheMaximumSize) { this.cacheMaximumSize = cacheMaximumSize; }
    public Duration getMinimumRequestInterval() { return minimumRequestInterval; }
    public void setMinimumRequestInterval(Duration minimumRequestInterval) { this.minimumRequestInterval = minimumRequestInterval; }
    public Map<String, String> getCompetitionCodes() { return competitionCodes; }
    public void setCompetitionCodes(Map<String, String> competitionCodes) { this.competitionCodes = competitionCodes; }
}
