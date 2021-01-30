package com.monumeena.twomaddresspicker

import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.location.Address
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.PendingResult
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.monumeena.twomaddresspicker.databinding.ActivityMapsBinding


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    lateinit var bindingActivity :ActivityMapsBinding
    private val REQUEST_ADDRESS = 132
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingActivity =DataBindingUtil.setContentView(this, R.layout.activity_maps)



        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        bindingActivity.btn.setOnClickListener {
            val intent = Intent(this, TwoMAddressPicker::class.java)
            intent.putExtra(TwoMAddressPicker.ARG_LAT_LNG, MyLatLng(23.77000, 77.3100))
            startActivityForResult(intent, REQUEST_ADDRESS)
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val address: Address? = data?.getParcelableExtra(TwoMAddressPicker.RESULT_ADDRESS) as Address?
        Log.e("Dsdsdsds", "onActivityResult: " + address)
       /* Latitude = address?.latitude.toString()
        Longitude = address?.longitude.toString()
        googleAddress = address?.getAddressLine(0)*/
    }


}