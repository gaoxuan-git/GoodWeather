package com.llw.goodweather.utils;

import com.llw.goodweather.bean.AirQualityCurrentResponse;
import com.llw.goodweather.bean.AirQualityDailyResponse;
import com.llw.goodweather.bean.AirQualityStationResponse;
import com.llw.goodweather.model.AirQualityData;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Maps QWeather V1 transport models to stable UI models.
 */
public final class AirQualityMapper {
    private static final String CHINESE_INDEX = "cn-mee";

    private AirQualityMapper() {
    }

    public static AirQualityData.Current mapCurrent(AirQualityCurrentResponse response) {
        if (response == null) {
            return null;
        }
        AirQualityCurrentResponse.Index index = selectIndex(response.indexes);
        if (index == null) {
            return null;
        }
        return new AirQualityData.Current(
                (int) Math.round(index.aqi),
                displayAqi(index),
                safeText(index.category, "--"),
                primaryName(index),
                mapPollutants(response.pollutants),
                response.stations);
    }

    public static List<AirQualityData.Daily> mapDaily(AirQualityDailyResponse response) {
        if (response == null || response.days == null) {
            return Collections.emptyList();
        }
        List<AirQualityData.Daily> result = new ArrayList<>();
        for (AirQualityDailyResponse.Day day : response.days) {
            if (day == null) {
                continue;
            }
            AirQualityCurrentResponse.Index index = selectIndex(day.indexes);
            String date = toLocalDate(day.forecastStartTime);
            if (index == null || date == null) {
                continue;
            }
            result.add(new AirQualityData.Daily(
                    date,
                    displayAqi(index),
                    safeText(index.category, "--"),
                    primaryName(index)));
        }
        return result;
    }

    public static AirQualityData.Station mapStation(
            String stationName, AirQualityStationResponse response) {
        if (response == null) {
            return null;
        }
        Map<String, AirQualityData.PollutantValue> pollutants =
                mapPollutants(response.pollutants);
        if (pollutants.isEmpty()) {
            return null;
        }
        return new AirQualityData.Station(safeText(stationName, "监测站"), pollutants);
    }

    public static String formatCoordinate(String coordinate) {
        if (coordinate == null) {
            return null;
        }
        try {
            return String.format(Locale.US, "%.2f", Double.parseDouble(coordinate));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static AirQualityCurrentResponse.Index selectIndex(
            List<AirQualityCurrentResponse.Index> indexes) {
        if (indexes == null || indexes.isEmpty()) {
            return null;
        }
        for (AirQualityCurrentResponse.Index index : indexes) {
            if (index != null && CHINESE_INDEX.equals(index.code)) {
                return index;
            }
        }
        for (AirQualityCurrentResponse.Index index : indexes) {
            if (index != null) {
                return index;
            }
        }
        return null;
    }

    private static Map<String, AirQualityData.PollutantValue> mapPollutants(
            List<AirQualityCurrentResponse.Pollutant> pollutants) {
        Map<String, AirQualityData.PollutantValue> values = new HashMap<>();
        if (pollutants == null) {
            return values;
        }
        for (AirQualityCurrentResponse.Pollutant pollutant : pollutants) {
            if (pollutant == null || pollutant.code == null || pollutant.concentration == null) {
                continue;
            }
            double raw = pollutant.concentration.value;
            String display = BigDecimal.valueOf(raw).stripTrailingZeros().toPlainString();
            int progress = Math.max(0, Math.min(100, (int) Math.round(raw)));
            values.put(pollutant.code, new AirQualityData.PollutantValue(display, progress));
        }
        return values;
    }

    private static String primaryName(AirQualityCurrentResponse.Index index) {
        if (index.primaryPollutant == null) {
            return "无污染";
        }
        if (index.primaryPollutant.name != null
                && !index.primaryPollutant.name.trim().isEmpty()) {
            return index.primaryPollutant.name;
        }
        return index.primaryPollutant.code == null ? "无污染" : index.primaryPollutant.code;
    }

    private static String displayAqi(AirQualityCurrentResponse.Index index) {
        if (index.aqiDisplay != null && !index.aqiDisplay.trim().isEmpty()) {
            return index.aqiDisplay;
        }
        return BigDecimal.valueOf(index.aqi).stripTrailingZeros().toPlainString();
    }

    private static String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private static String toLocalDate(String dateTime) {
        if (dateTime == null) {
            return null;
        }
        SimpleDateFormat input =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US);
        input.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat output =
                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date parsed = input.parse(dateTime);
            return parsed == null ? null : output.format(parsed);
        } catch (ParseException exception) {
            return null;
        }
    }
}
