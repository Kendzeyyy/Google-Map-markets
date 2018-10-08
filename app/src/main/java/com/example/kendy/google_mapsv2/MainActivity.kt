package com.example.kendy.google_mapsv2

import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.telecom.Call
import android.util.Log
import android.widget.Toast
import com.example.kendy.google_mapsv2.Model.MyPlaces
import com.example.kendy.google_mapsv2.Remote.IGoogleAPIService
import com.google.android.gms.common.api.Response
import com.google.android.gms.common.internal.service.Common
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.StringBuilder
import javax.security.auth.callback.Callback

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    private var latitude:Double=0.toDouble()
    private var longtitude:Double=0.toDouble()

    private lateinit var mLastLocation: Location
    private var mMarker: Marker?=null

    // Location
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var locationRequest: LocationRequest
    lateinit var locationCallback: LocationCallback

    companion object {
        private val MY_PERMISSION_CODE: Int = 1000
    }

    lateinit var mService: IGoogleAPIService

    internal lateinit var currentPlace: MyPlaces

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //Init service
        mService = com.example.kendy.google_mapsv2.Common.Common.googleApiService

        // Request runtime permission
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission()
            buildLocationRequest()
            buildLocationCallBack()

            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
        }

        bottom_navigation_view.setOnNavigationItemReselectedListener {item ->
            when(item.itemId){
                R.id.action_market -> nearByPlace("market")
            }
        }
    }

    private fun nearByPlace(typePlace: String){

        // Clear all marker on map
        mMap.clear()

        //build URL request base on location
        val url = getUrl(latitude, longtitude, typePlace)

        mService.getNearbyPlaces(url)
                .enqueue(object : retrofit2.Callback<MyPlaces> {
                    override fun onResponse(call: retrofit2.Call<MyPlaces>, response: retrofit2.Response<MyPlaces>) {
                        currentPlace = response.body()!!

                        if (response.isSuccessful){
                            for (i in 0 until response!!.body()!!.results!!.size){
                                val markerOptions = MarkerOptions()
                                val googlePlace = response.body()!!.results!![i]
                                val lat = googlePlace.geometry!!.location!!.lat
                                val lng = googlePlace.geometry!!.location!!.lng
                                val placeName = googlePlace.name
                                val latLng = LatLng(lat, lng)

                                markerOptions.position(latLng)
                                markerOptions.title(placeName)
                                if (typePlace.equals("market"))
                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_market))
                                else
                                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                                markerOptions.snippet(i.toString()) // Assign index for market

                                //Add marker to map
                                mMap!!.addMarker(markerOptions)
                                mMap!!.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                                mMap!!.animateCamera(CameraUpdateFactory.zoomTo(11f))

                            }
                        }
                    }

                    override fun onFailure(call: retrofit2.Call<MyPlaces>, t: Throwable) {
                        Toast.makeText(baseContext, ""+t!!.message,Toast.LENGTH_SHORT).show()
                    }
                })
    }

    private fun getUrl(latitude: Double, longtitude: Double, typePlace: String): String {
        val googlePlaceUrl = StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json")
        googlePlaceUrl.append("?location=$latitude, $longtitude")
        googlePlaceUrl.append("&radius=1500")
        googlePlaceUrl.append("&type=$typePlace")
        googlePlaceUrl.append("&key=AIzaSyC2l04Lix-h5OslFzQpG79R6F06fYQX3uo")

        Log.d("URL_DEBUG", googlePlaceUrl.toString())
        return googlePlaceUrl.toString()
    }

    private fun buildLocationCallBack(){
        locationCallback = object : LocationCallback(){
                override fun onLocationResult(p0: LocationResult?){
                    mLastLocation = p0!!.locations.get(p0!!.locations.size-1) // get last location

                    if(mMarker != null){
                        mMarker!!.remove()
                    }
                    latitude = mLastLocation.latitude
                    longtitude = mLastLocation.longitude

                    val latLng = LatLng(latitude,longtitude)
                    val markerOptions = MarkerOptions()
                            .position(latLng)
                            .title("Your position")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    mMarker = mMap!!.addMarker(markerOptions)

                    // move camera
                    mMap!!.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                    mMap!!.animateCamera(CameraUpdateFactory.zoomTo(11f))
                }

        }
    }

    private fun buildLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000
        locationRequest.fastestInterval = 3000
        locationRequest.smallestDisplacement = 10f
    }

    private fun checkLocationPermission(): Boolean {

        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)){
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), MY_PERMISSION_CODE)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), MY_PERMISSION_CODE)
            }
            return false
        }
        else return true
    }

    // Override OnRequestPermissionResult
    @RequiresApi (Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode){
            MY_PERMISSION_CODE ->{
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                        if (checkLocationPermission()) {
                            mMap!!.isMyLocationEnabled = true
                        }
                    }
                    else{
                        Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onStop() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onStop()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Init Google Play Services
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap!!.isMyLocationEnabled = true
            }
        }
        else{
            mMap!!.isMyLocationEnabled = true
        }
    }
}
