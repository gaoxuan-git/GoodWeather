package com.llw.goodweather.bean;

import java.util.List;

/**
 * QWeather air-quality V1 daily response.
 */
public class AirQualityDailyResponse {
    public AirQualityCurrentResponse.Metadata metadata;
    public List<Day> days;

    public static class Day {
        public String forecastStartTime;
        public String forecastEndTime;
        public List<AirQualityCurrentResponse.Index> indexes;
        public List<AirQualityCurrentResponse.Pollutant> pollutants;
    }
}
