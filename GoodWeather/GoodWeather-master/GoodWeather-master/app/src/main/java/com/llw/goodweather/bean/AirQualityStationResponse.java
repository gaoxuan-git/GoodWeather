package com.llw.goodweather.bean;

import java.util.List;

/**
 * QWeather air-quality V1 station response.
 */
public class AirQualityStationResponse {
    public AirQualityCurrentResponse.Metadata metadata;
    public List<AirQualityCurrentResponse.Pollutant> pollutants;
}
