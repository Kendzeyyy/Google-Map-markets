package com.example.kendy.google_mapsv2.Common

import com.example.kendy.google_mapsv2.Remote.IGoogleAPIService
import com.example.kendy.google_mapsv2.Remote.RetrofitClient

object Common {

    private val GOOGLE_API_URL="https://maps.googleapis.com/"

    val googleApiService: IGoogleAPIService
    get() = RetrofitClient.getClient(GOOGLE_API_URL).create(IGoogleAPIService::class.java)
}