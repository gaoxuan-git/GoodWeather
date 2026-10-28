package com.llw.goodweather.utils;

import com.llw.goodweather.bean.AirQualityCurrentResponse;
import com.llw.goodweather.bean.AirQualityDailyResponse;
import com.llw.goodweather.bean.AirQualityStationResponse;
import com.llw.goodweather.model.AirQualityData;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AirQualityMapperTest {
    private TimeZone originalTimeZone;

    @Before
    public void setUp() {
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
    }

    @After
    public void tearDown() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    public void mapCurrentPrefersChineseIndexAndMapsByPollutantCode() {
        AirQualityCurrentResponse response = new AirQualityCurrentResponse();
        AirQualityCurrentResponse.Index qaqi = index("qaqi", 1, "1.0", "优", null);
        AirQualityCurrentResponse.Index cn = index("cn-mee", 42, "42", "优", "pm2p5");
        response.indexes = Arrays.asList(qaqi, cn);
        response.pollutants = Arrays.asList(pollutant("o3", 81), pollutant("pm10", 26));

        AirQualityData.Current result = AirQualityMapper.mapCurrent(response);

        assertNotNull(result);
        assertEquals(42, result.aqi);
        assertEquals("42", result.aqiDisplay);
        assertEquals("PM 2.5", result.primary);
        assertEquals("26", result.pollutant("pm10").display);
        assertEquals("81", result.pollutant("o3").display);
        assertEquals("--", result.pollutant("co").display);
    }

    @Test
    public void mapCurrentFallsBackToFirstIndex() {
        AirQualityCurrentResponse response = new AirQualityCurrentResponse();
        response.indexes = Collections.singletonList(index("qaqi", 2, "2.0", "良", null));

        AirQualityData.Current result = AirQualityMapper.mapCurrent(response);

        assertNotNull(result);
        assertEquals("2.0", result.aqiDisplay);
        assertEquals("无污染", result.primary);
    }

    @Test
    public void mapDailyConvertsUtcBoundaryToLocalDate() {
        AirQualityDailyResponse response = new AirQualityDailyResponse();
        AirQualityDailyResponse.Day day = new AirQualityDailyResponse.Day();
        day.forecastStartTime = "2026-10-17T16:00Z";
        day.indexes = Collections.singletonList(index("cn-mee", 34, "34", "优", null));
        response.days = Collections.singletonList(day);

        assertEquals("2026-10-18", AirQualityMapper.mapDaily(response).get(0).date);
    }

    @Test
    public void formatCoordinateUsesTwoDecimalsAndDotSeparator() {
        assertEquals("39.92", AirQualityMapper.formatCoordinate("39.91755"));
        assertEquals("116.42", AirQualityMapper.formatCoordinate("116.41876"));
    }

    @Test
    public void emptyPayloadsReturnSafeEmptyResults() {
        assertNull(AirQualityMapper.mapCurrent(new AirQualityCurrentResponse()));
        assertTrue(AirQualityMapper.mapDaily(new AirQualityDailyResponse()).isEmpty());
        assertNull(AirQualityMapper.formatCoordinate("invalid"));
    }

    @Test
    public void mapStationKeepsReturnedPollutantsWithoutInventingAqi() {
        AirQualityStationResponse response = new AirQualityStationResponse();
        response.pollutants = Collections.singletonList(pollutant("pm2p5", 8));

        AirQualityData.Station result = AirQualityMapper.mapStation("东四", response);

        assertNotNull(result);
        assertEquals("东四", result.name);
        assertEquals("--", result.aqiDisplay);
        assertEquals("8", result.pollutant("pm2p5").display);
    }

    private static AirQualityCurrentResponse.Index index(
            String code, double aqi, String display, String category, String primaryCode) {
        AirQualityCurrentResponse.Index index = new AirQualityCurrentResponse.Index();
        index.code = code;
        index.aqi = aqi;
        index.aqiDisplay = display;
        index.category = category;
        if (primaryCode != null) {
            index.primaryPollutant = new AirQualityCurrentResponse.PrimaryPollutant();
            index.primaryPollutant.code = primaryCode;
            index.primaryPollutant.name = "PM 2.5";
        }
        return index;
    }

    private static AirQualityCurrentResponse.Pollutant pollutant(String code, double value) {
        AirQualityCurrentResponse.Pollutant pollutant = new AirQualityCurrentResponse.Pollutant();
        pollutant.code = code;
        pollutant.concentration = new AirQualityCurrentResponse.Concentration();
        pollutant.concentration.value = value;
        pollutant.concentration.unit = "μg/m³";
        return pollutant;
    }
}
