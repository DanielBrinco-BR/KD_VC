package com.projects.android.kd_vc.activities

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.DatePicker
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.projects.android.kd_vc.PhoneApplication
import com.projects.android.kd_vc.R
import com.projects.android.kd_vc.databinding.ActivityMapsBinding
import com.projects.android.kd_vc.room.PhoneViewModel
import com.projects.android.kd_vc.room.PhoneViewModelFactory
import java.text.SimpleDateFormat
import java.util.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private val TAG = "KadeVc"

    private var cal = Calendar.getInstance()

    private lateinit var mMap: GoogleMap
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    private lateinit var binding: ActivityMapsBinding
    private lateinit var phoneNumber: String

    private val phoneViewModel: PhoneViewModel by viewModels {
        PhoneViewModelFactory((application as PhoneApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val actionBar: ActionBar = supportActionBar!!
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeButtonEnabled(true)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        phoneNumber = intent.getStringExtra("phone_number").toString()

        dateSetListener = object : DatePickerDialog.OnDateSetListener {
            override fun onDateSet(view: DatePicker, year: Int, monthOfYear: Int,
                                   dayOfMonth: Int) {
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val sdf = SimpleDateFormat("dd-MM-yyyy", Locale("pt", "BR"))
                val date = sdf.format(cal.getTime())

                val formattedDate = "%$date"
                val formattedPhoneNumber = "%$phoneNumber"
                var isLast = false

                phoneViewModel.findListPhoneDataByDate(formattedDate, formattedPhoneNumber).observe(this@MapsActivity, Observer { listPhoneData ->
                    Log.i(TAG, "MapsActivity.findListPhoneDataByDate() - Observer: listPhoneData.size: ${listPhoneData.size}")

                    try {
                        val lastPhoneData = listPhoneData.get(listPhoneData.size - 1)
                        mMap.clear()

                        listPhoneData.forEach {
                            var formattedPhoneNumber = it.phoneNumber.substring(2)
                            formattedPhoneNumber = "(" + formattedPhoneNumber.substring(0, 2) + ") " + formattedPhoneNumber.substring(2, 7) + "-" + formattedPhoneNumber.substring(7)


                            val title = formattedPhoneNumber + " - " + it.date + " - " + it.time

                            val batteryLevel = "Nível da bateria: " + it.batteryLevel
                            val wifiSSID = "Conectado na rede Wi-Fi " + it.wifiSSID
                            val hasInternet = it.hasInternet
                            val networkType = "Conectado no " + it.networkType
                            var snippet = ""

                            if(hasInternet.equals("false")) {
                                snippet = batteryLevel + "\n" + "Sem conexão com a internet"
                            } else if(wifiSSID.equals("")) {
                                snippet = batteryLevel + "\n" + networkType
                            } else {
                                snippet = batteryLevel + "\n" + wifiSSID
                            }

                            Log.i(TAG, "MapsActivity.findListPhoneDataByDate() - Observer: title: $title, snippet: $snippet")

                            if(lastPhoneData.date.equals(it.date) && lastPhoneData.time.equals(it.time)) {
                                isLast = true
                            }

                            updateMap(LatLng(it.latitude.toDouble(), it.longitude.toDouble()), title, snippet, isLast, true)
                        }
                    } catch(e: Exception) {
                        Log.e(TAG, "MapsActivity.findListPhoneDataByDate() - Observer: ${e.message}, \n ${e.stackTraceToString()}")
                    }
                })
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_map_options, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            android.R.id.home -> {
                Log.i(TAG, "MapsActivity.onOptionsItemSelected() - this.finish()")
                this.finish()
                true
            }
            R.id.search_by_data -> {
                DatePickerDialog(this@MapsActivity,
                    dateSetListener,
                    // set DatePickerDialog to point to today's date when it loads up
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show()
                true
            }
            R.id.normal_map -> {
                mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                true
            }
            R.id.hybrid_map -> {
                mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
                true
            }
            R.id.satellite_map -> {
                mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
                true
            }
            R.id.terrain_map -> {
                mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
                true
            }
            else -> super.onOptionsItemSelected(item)
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

        // Carrega o último local registrado:
        phoneViewModel.findLastPhoneDataByNumber(phoneNumber).observe(this, Observer { phoneData ->
                try {
                    var formattedPhoneNumber = phoneData.phoneNumber.substring(2)
                    formattedPhoneNumber = "(" + formattedPhoneNumber.substring(0, 2) + ") " + formattedPhoneNumber.substring(2, 7) + "-" + formattedPhoneNumber.substring(7)

                    val title = formattedPhoneNumber + " - " + phoneData.date + " - " + phoneData.time
                    Log.i(TAG, "MapsActivity.onCreate() - Observer: phoneNumber: $phoneNumber, title: $title")
                    Log.i(TAG, "MapsActivity.onCreate() - Observer: ${phoneData.latitude}, ${phoneData.longitude}")

                    val batteryLevel = "Nível da bateria: " + phoneData.batteryLevel
                    val wifiSSID = "Conectado na rede Wi-Fi " + phoneData.wifiSSID
                    val hasInternet = phoneData.hasInternet
                    val networkType = "Conectado no " + phoneData.networkType
                    var snippet = ""

                    if(hasInternet.equals("false")) {
                        snippet = batteryLevel + "\n" + "Sem conexão com a internet"
                    } else if(wifiSSID.equals("")) {
                        snippet = batteryLevel + "\n" + networkType
                    } else {
                        snippet = batteryLevel + "\n" + wifiSSID
                    }

                    updateMap(LatLng(phoneData.latitude.toDouble(), phoneData.longitude.toDouble()), title, snippet, false, false)
                } catch(e: Exception) {
                    Log.e(TAG, "MapsActivity.onCreate() - Observer: ${e.message}, \n ${e.stackTraceToString()}")
                }
        })
    }

    private fun updateMap(position: LatLng, title: String, snippet: String, isLast: Boolean, allLocations: Boolean) {
        Log.i(TAG, "MapsActivity.updateMap() - title: $title, snippet: $snippet")

        if(isLast) {
            val markerOptions = MarkerOptions()
            markerOptions.position(position).title(title).snippet(snippet).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            val marker: Marker? = mMap.addMarker(markerOptions)
            val bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.mipmap.user_location_foreground)
            marker?.setIcon(bitmapDescriptor)

            //test:
            marker?.zIndex = Float.MAX_VALUE
        } else {
            mMap.addMarker(MarkerOptions().position(position).title(title).snippet(snippet).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
            mMap.moveCamera(CameraUpdateFactory.newLatLng(position))
        }

        if(allLocations) {
            mMap.animateCamera(CameraUpdateFactory.zoomTo(10.0F))
        } else {
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15.0F))
        }

        // Adaptação para exibir snippet com múltiplas linhas:
        mMap.setInfoWindowAdapter(object : InfoWindowAdapter {
            override fun getInfoWindow(arg0: Marker): View? {
                return null
            }

            override fun getInfoContents(marker: Marker): View {
                val info = LinearLayout(this@MapsActivity)
                info.orientation = LinearLayout.VERTICAL
                val title = TextView(this@MapsActivity)
                title.setTextColor(Color.BLACK)
                title.gravity = Gravity.CENTER
                title.setTypeface(null, Typeface.BOLD)
                title.text = marker.title
                val snippet = TextView(this@MapsActivity)
                snippet.setTextColor(Color.GRAY)
                snippet.text = marker.snippet
                info.addView(title)
                info.addView(snippet)
                return info
            }
        })
    }
}