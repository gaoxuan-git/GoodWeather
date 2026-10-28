package com.llw.goodweather.bean;

import java.util.List;

/**
 * QWeather air-quality V1 current response.
 */
public class AirQualityCurrentResponse {
    public Metadata metadata;
    public List<Index> indexes;
    public List<Pollutant> pollutants;
    public List<StationRef> stations;

    public static class Metadata {
        public String tag;
        public List<String> attributions;
    }

    public static class Index {
        public String code;
        public double aqi;
        public String aqiDisplay;
        public String level;
        public String category;
        public PrimaryPollutant primaryPollutant;
    }

    public static class PrimaryPollutant {
        public String code;
        public String name;
        public String fullName;
    }

    public static class Pollutant {
        public String code;
        public String name;
        public String fullName;
        public Concentration concentration;
    }

    public static class Concentration {
        public double value;
        public String unit;
    }

    public static class StationRef {
        public String id;
        public String name;
    }
}
