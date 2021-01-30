package com.monumeena.twomaddresspicker

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.location.*
import android.location.LocationListener
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
import com.google.android.gms.common.api.PendingResult
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.monumeena.twomaddresspicker.databinding.ActivityQuickAddressPickerBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.Serializable
import java.util.*


class TwoMAddressPicker : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnCameraMoveListener,
    GoogleMap.OnCameraIdleListener, View.OnClickListener {
    private lateinit var binding: ActivityQuickAddressPickerBinding
    private var mapView: View? = null
    private var locationButton: View? = null
    private lateinit var mMap: GoogleMap
    private val REQUEST_LOCATION_PERMISSION = 1
    private val AUTOCOMPLETE_REQUEST_CODE = 12
    private var mZoomLevel = 18.0f
    private var cameraIdleJob: Job? = null

    private var mAddress: Address? = null
    private var addressLinezero: String? = null
    private var city: String? = null
    private var state: String? = null
    private var country: String? = null
    private var postalCode: String? = null
    private var knownName: String? = null
    private var locationManager: LocationManager? = null

    private var userCurrentLat: Double? = null
    private var userCurrentLng: Double? = null
    private var mPinList: ArrayList<Pin>? = null
    private var pinMarker: Marker? = null

    companion object {
        val ARG_ZOOM_LEVEL = "level_zoom"
        val setDefaultLatLng = "arg_lat_lng"
        val ARG_LAT_LNG = "arg_lat_lng"
        val RESULT_ADDRESS = "address"
        val ARG_LIST_PIN = "list_pins"
        private var mDefaultLocation: LatLng? = null
    }

    protected val TAG = "LocationOnOff"

    private var googleApiClient: GoogleApiClient? = null
    val REQUEST_LOCATION = 199

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initPlacesApi()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_quick_address_picker)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?
        if (intent.hasExtra(ARG_LAT_LNG)) {
            val latLng = intent.getSerializableExtra(setDefaultLatLng) as MyLatLng
            mDefaultLocation = LatLng(latLng.latitude, latLng.longitude)
            Log.e("dsdjsk", "onCreate: " + latLng.latitude)
        }
        if (intent.hasExtra(ARG_LIST_PIN)) {
            mPinList = intent.getSerializableExtra(ARG_LIST_PIN) as ArrayList<Pin>
        }


        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapView = mapFragment?.view;
        mapFragment!!.getMapAsync(this)





    }

    private fun displayLocationSettingsRequest(context: Context) {
        val googleApiClient = GoogleApiClient.Builder(context)
            .addApi(LocationServices.API).build()
        googleApiClient.connect()
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 10000
        locationRequest.fastestInterval = (10000 / 2).toLong()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)
        val result: PendingResult<LocationSettingsResult> =
            LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build())
        result.setResultCallback { result ->
            val status: Status = result.status
            when (status.getStatusCode()) {
                LocationSettingsStatusCodes.SUCCESS -> {
                    Log.i(
                        "Dsdsfsf",
                        "All location settings are satisfied."
                    )

                    getCurrentLocation()
                }

                LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                    Log.i(
                        "Dsdsfsf",
                        "Location settings are not satisfied. Show the user a dialog to upgrade location settings "
                    )

                    try {
                        // Show the dialog by calling startResolutionForResult(), and check the result
                        // in onActivityResult().
                        status.startResolutionForResult(
                            this@TwoMAddressPicker,
                            112
                        )
                    } catch (e: SendIntentException) {
                        Log.i("Dsdsfsf", "PendingIntent unable to execute request.")
                    }
                }
                LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> Log.i(
                    "Dsdsfsf",
                    "Location settings are inadequate, and cannot be fixed here. Dialog not created."
                )
            }
        }
    }




    private fun getCurrentLocation() {

        val locationListener: LocationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (userCurrentLat == null && userCurrentLng == null) {
                    userCurrentLat = location.latitude
                    userCurrentLng = location.longitude
                    setMarkerOnTheMap(userCurrentLat!!.toString(), userCurrentLng!!.toString())

                }

            }

            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }


        try {
            // Request location updates
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                5000,
                0f,
                locationListener
            )
        } catch (ex: SecurityException) {
            Log.d("myTag", "Security Exception, no location available")
        }
    }

    override fun onMapReady(p0: GoogleMap?) {

        p0?.let { mapInit(it) }

        displayLocationSettingsRequest(this)
    }

    override fun onCameraMove() {
        mMap.clear()
        binding.imgLocationPinUp?.visibility = View.VISIBLE
        cameraIdleJob?.cancel()
    }

    override fun onCameraIdle() {

        cameraIdleJob?.cancel()
        cameraIdleJob = lifecycleScope.launch {
            delay(750)
            binding.imgLocationPinUp?.visibility = View.GONE
            /*val markerOptions = MarkerOptions().position(mMap.cameraPosition.target)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_marker))*/



     mMap.addMarker(MarkerOptions().apply {
         position(mMap.cameraPosition.target)
         icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_marker))
         draggable(false)
         getAddress(position.latitude, position.longitude)

     })


/*
            if (!mPinList.isNullOrEmpty()){
                for (pin in mPinList!!) {
                    val options =
                        MarkerOptions().position(LatLng(pin.latLng.latitude, pin.latLng.longitude))
                    if (pin.title?.isNotEmpty()!!) {
                        options.title(pin.title)
                        mMap?.addMarker(options)
                    }
                }
            }*/


        }
    }

    fun mapInit(googleMap: GoogleMap) {
        mMap = googleMap
        if (mDefaultLocation == null) {
           displayLocationSettingsRequest(this)
        } else {
           /* setMarkerOnTheMap(
                mDefaultLocation!!.latitude.toString(),
                mDefaultLocation!!.longitude.toString()
            )*/
        }


        googleMap.apply {
            // just a random location our map will point to when its launched
            enableMyLocation()
            if (ActivityCompat.checkSelfPermission(
                    this@TwoMAddressPicker,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this@TwoMAddressPicker,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                return
            }


            // maps events we need to respond to
            setOnCameraMoveListener(this@TwoMAddressPicker)
            setOnCameraIdleListener(this@TwoMAddressPicker)
            binding.etSearch.setOnClickListener(this@TwoMAddressPicker)
            binding.btnUseThisLocation.setOnClickListener(this@TwoMAddressPicker)
            if (mapView != null &&
                mapView!!.findViewById<View?>("1".toInt()) != null
            ) {
                // Get the button view
                locationButton =
                    (mapView!!.findViewById<View>("1".toInt()).parent as View).findViewById<View>("2".toInt())
                // and next place it, on bottom right (as Google Maps app)
                val layoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT
                )
                binding.mapLocationButton.setOnClickListener(this@TwoMAddressPicker)
                if (locationButton != null) {
                    locationButton?.visibility = View.GONE;
                }

            }


        }
    }

    private fun initPlacesApi() {
        try {
            val applicationInfo = packageManager.getApplicationInfo(
                packageName,
                PackageManager.GET_META_DATA
            );
            val bundle = applicationInfo.metaData;
            val apiKey = "AIzaSyD5LHs3Hy6hP9odkqd-JXftB2dkOCDiIAM";
            //  val apiKey = bundle.getString("com.google.android.geo.API_KEY");
            if (!apiKey.isNullOrEmpty()) {
                // Initialize the SDK
                Places.initialize(applicationContext, apiKey);
            }

        } catch (e: java.lang.Exception) {
            //Resolve error for not existing meta-tag, inform the developer about adding his api key
        }
    }

    private fun getAddress(latitude: Double, longitude: Double): String? {
        val result = StringBuilder()

        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses.size > 0) {
                mAddress = addresses[0]
                addressLinezero =
                    addresses[0].getAddressLine(0) // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                city = addresses[0].locality
                state = addresses[0].adminArea
                country = addresses[0].countryName
                postalCode = addresses[0].postalCode
                knownName = addresses[0].featureName
                result.append(addresses[0].adminArea + ", " + addresses[0].countryName + ", " + addresses[0].postalCode + " " + addresses[0].featureName)

                binding.textViewFullAdd?.text =
                    """${addressLinezero}, ${city}, ${knownName}, ${state}, ${country}"""
                binding.textViewCityName?.text = """$city"""
                binding.textViewPostalCode?.text = """$postalCode"""

            }
        } catch (e: IOException) {

        }
        return result.toString()
    }

    private fun setMarkerOnTheMap(latitude: String, logitude: String) {
        mMap.clear()
        val klcc = LatLng(latitude.toDouble(), logitude.toDouble())
        mMap.addMarker(MarkerOptions().apply {
            position(klcc)
            draggable(false)
            mZoomLevel
        })

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(klcc, 8f))
    }

    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun enableMyLocation() {
        if (isPermissionGranted()) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                return
            }
            mMap.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.mapLocationButton -> locationButton?.performClick()
            binding.etSearch -> {
                var fields: List<Place.Field> = listOf(
                    Place.Field.ID,
                    Place.Field.NAME,
                    Place.Field.LAT_LNG,
                    Place.Field.ADDRESS
                )

                var intent: Intent = Autocomplete.IntentBuilder(
                    AutocompleteActivityMode.FULLSCREEN, fields
                )
                    .build(this);
                startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
            }
            binding.btnUseThisLocation -> {
                intent.putExtra(RESULT_ADDRESS, mAddress)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }

        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            AUTOCOMPLETE_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    var place = Autocomplete.getPlaceFromIntent(data!!);
                    //moveMapToLocation(place.latLng!!)
                    setMarkerOnTheMap(
                        place.latLng!!.latitude.toString(),
                        place.latLng!!.longitude.toString()
                    )
                    getAddress(place.latLng!!.latitude, place.latLng!!.longitude)

                } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                    // TODO: Handle the error.
                    var status = Autocomplete.getStatusFromIntent(data!!);
                    status.statusMessage?.let { Log.i("mmmm", it) };
                } else if (resultCode == RESULT_CANCELED) {
                    // The user canceled the operation.
                }
            }

            112 -> {
                Log.e("dsdfdfdfdsds", "onActivityResult: " + data?.data)

                Toast.makeText(this, "dsd"+data, Toast.LENGTH_SHORT).show()
                displayLocationSettingsRequest(this)
            }
        }
    }


}

class MyLatLng(var latitude: Double, var longitude: Double) : Serializable
class Pin(var latLng: MyLatLng, var title: String?) : Serializable