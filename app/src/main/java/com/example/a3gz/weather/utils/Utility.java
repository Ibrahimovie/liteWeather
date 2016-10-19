package com.example.a3gz.weather.utils;

import android.content.SharedPreferences;
import android.text.TextUtils;

import com.example.a3gz.weather.City;
import com.example.a3gz.weather.db.WeatherDB;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by 3gz on 2016/10/18.
 * 解析服务器返回的数据
 */

public class Utility {

    //处理从服务器获取的数据
    public synchronized static boolean handleCityResponse(WeatherDB weatherDB, String response) {

        if (!TextUtils.isEmpty(response)) {
            try {
                //Json解析
                JSONArray jsonArray = new JSONObject(response).getJSONArray("city_info");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject city_info = jsonArray.getJSONObject(i);
                    City city = new City();
                    String city_name_ch = city_info.getString("city");
                    String city_name_en = "";
                    String city_code = city_info.getString("id");
                    city.setCity_code(city_code);
                    city.setCity_name_en(city_name_en);
                    city.setCity_name_ch(city_name_ch);
                    weatherDB.saveCity(city);//将解析出来的数据存储到City表
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    //处理从服务器返回的天气信息
    //数据是JSON,存储到sharedpreferences文件中
    public synchronized static boolean handleWeatherResponse(SharedPreferences.Editor editor, String response) {
        if (!TextUtils.isEmpty(response)) {
            try {
                //先把JSON数据加载成数组，因为根部HeWeather data service 3.0后面是[符号，说明是以数组形式存放，只是这个数组里面只有一个元素
                JSONArray jsonArray = new JSONObject(response).getJSONArray("HeWeather data service 3.0");
                //这个数组里面只有一个元素，直接取出第一个元素为JSONObject
                JSONObject weather_info_all = jsonArray.getJSONObject(0);
                //城市名称和数据更新的时间是在basic下面，直接获取
                JSONObject weather_info_basic = weather_info_all.getJSONObject("basic");
                /*"basic": {
                    "city": "北京",
                    "cnty": "中国",
                    "id": "CN101010100",
                    "lat": "39.904000",
                    "lon": "116.391000",
                    "update":
                    {
                       "loc": "2016-06-30 08:51",
                       "utc": "2016-06-30 00:51"
                    }
                },*/

                //有city和update，city可以直接通过名称获取到信息
                editor.putString("city_name_ch", weather_info_basic.getString("city"));
                editor.putString("city_code", weather_info_basic.getString("id"));
                //更新的时间是不能获取的，因为update后面是｛｝，表明这是一个对象
                //先根据名称获取这个对象
                JSONObject weather_info_basic_update = weather_info_basic.getJSONObject("update");
                //然后再根据这个对象获取名称是loc的数据信息
                editor.putString("update_time", weather_info_basic_update.getString("loc"));

                //关于天气的所有信息都是在daily_forecast名称下面，daily_forecast后面是[符号，说明这也是一个JSON数组
                //所以先根据名称获取JSONArray对象
                JSONArray weather_info_daily_forecast = weather_info_all.getJSONArray("daily_forecast");
                //[]里面是由很多个像下面这样的元素组成的
                /*
                {
                    "astro": {
                        "sr": "04:49",
                        "ss": "19:47"
                    },
                    "cond": {
                        "code_d": "302",
                        "code_n": "302",
                        "txt_d": "雷阵雨",
                        "txt_n": "雷阵雨"
                    },
                    "date": "2016-06-30",
                    "hum": "30",
                    "pcpn": "0.2",
                    "pop": "39",
                    "pres": "1002",
                    "tmp": {
                        "max": "31",
                        "min": "22"
                    },
                    "vis": "10",
                    "wind": {
                          "deg": "204",
                          "dir": "无持续风向",
                          "sc": "微风",
                          "spd": "4"
                    }
                },
                */

                //第一个元素是当前的日期相关的天气数据，获取出来的是一个JSONObject
                JSONObject weather_info_now_forecast = weather_info_daily_forecast.getJSONObject(0);
                //date是可以直接获取的，因为date后面是没有｛｝的
                editor.putString("data_now", weather_info_now_forecast.getString("date"));//当前日期
                //tmp节点是当前的温度，包含最低和最高,这是一个JSONObject
                JSONObject weather_info_now_forecast_tmp = weather_info_now_forecast.getJSONObject("tmp");
                editor.putString("tmp_min", weather_info_now_forecast_tmp.getString("min"));
                editor.putString("tmp_max", weather_info_now_forecast_tmp.getString("max"));

                //cond是当前的实际天气描述，获取方法和tmp是一样的
                JSONObject weather_info_now_forecast_cond = weather_info_now_forecast.getJSONObject("cond");
                editor.putString("txt_d", weather_info_now_forecast_cond.getString("txt_d"));//天气情况前
                editor.putString("txt_n", weather_info_now_forecast_cond.getString("txt_n"));//天气情况后

                //最后提交
                editor.commit();
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
