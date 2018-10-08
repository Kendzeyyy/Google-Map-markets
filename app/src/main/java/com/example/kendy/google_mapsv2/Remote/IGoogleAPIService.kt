package com.example.kendy.google_mapsv2.Remote

import com.example.kendy.google_mapsv2.Model.MyPlaces
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Url

interface IGoogleAPIService {
    @GET
    fun getNearbyPlaces(@Url url:String):Call<MyPlaces>
}