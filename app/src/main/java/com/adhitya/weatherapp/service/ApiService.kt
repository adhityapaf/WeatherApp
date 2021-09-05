package com.adhitya.weatherapp.service

import com.adhitya.weatherapp.model.CurrentDailyForecast
import com.adhitya.weatherapp.model.CurrentWeather
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("weather")
    fun getWeather(
        @Query("q") location: String,
        @Query("units") metric: String,
        @Query("appid") appId: String
    ) : Call<CurrentWeather>

    @GET("weather")
    fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") metric: String,
        @Query("appid") appId: String
    ) : Call<CurrentWeather>

    @GET("onecall")
    fun getCurrentDailyForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("exclude") exclude:String,
        @Query("units") metric: String,
        @Query("appid") appId: String
    ) : Call<CurrentDailyForecast>
}