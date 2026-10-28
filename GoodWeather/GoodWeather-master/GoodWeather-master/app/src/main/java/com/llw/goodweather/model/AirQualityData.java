package com.llw.goodweather.model;

import com.llw.goodweather.bean.AirQualityCurrentResponse;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Null-safe display models for air-quality screens.
 */
public final class AirQualityData {
    private AirQualityData() {
    }

    public static final class PollutantValue {
        public final String display;
        public final int progress;

        public PollutantValue(String display, int progress) {
            this.display = display;
            this.progress = progress;
        }
    }

    public static final class Current {
        public final int aqi;
        public final String aqiDisplay;
        public final String category;
        public final String primary;
        public final Map<String, PollutantValue> pollutants;
        public final List<AirQualityCurrentResponse.StationRef> stations;

        public Current(int aqi, String aqiDisplay, String category, String primary,
                       Map<String, PollutantValue> pollutants,
                       List<AirQualityCurrentResponse.StationRef> stations) {
            this.aqi = aqi;
            this.aqiDisplay = aqiDisplay;
            this.category = category;
            this.primary = primary;
            this.pollutants = pollutants == null ? Collections.emptyMap() : pollutants;
            this.stations = stations == null ? Collections.emptyList() : stations;
        }

        public PollutantValue pollutant(String code) {
            PollutantValue value = pollutants.get(code);
            return value == null ? new PollutantValue("--", 0) : value;
        }
    }

    public static final class Daily {
        public final String date;
        public final String aqiDisplay;
        public final String category;
        public final String primary;

        public Daily(String date, String aqiDisplay, String category, String primary) {
            this.date = date;
            this.aqiDisplay = aqiDisplay;
            this.category = category;
            this.primary = primary;
        }
    }

    public static final class Station {
        public final String name;
        public final String aqiDisplay;
        public final String category;
        public final String primary;
        public final Map<String, PollutantValue> pollutants;

        public Station(String name, Map<String, PollutantValue> pollutants) {
            this.name = name;
            this.aqiDisplay = "--";
            this.category = "监测值";
            this.primary = "--";
            this.pollutants = pollutants == null ? Collections.emptyMap() : pollutants;
        }

        public PollutantValue pollutant(String code) {
            PollutantValue value = pollutants.get(code);
            return value == null ? new PollutantValue("--", 0) : value;
        }
    }
}
