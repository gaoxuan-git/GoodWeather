package com.llw.goodweather.bean;

import java.util.List;

public class ServerCitiesResponse {

    private String code;
    private int count;
    private List<CityBean> cities;

    public String getCode() {
        return code;
    }

    public int getCount() {
        return count;
    }

    public List<CityBean> getCities() {
        return cities;
    }

    public static class CityBean {
        private String id;
        private String name;
        private String adm1;
        private String adm2;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getAdm1() {
            return adm1;
        }

        public String getAdm2() {
            return adm2;
        }
    }
}
