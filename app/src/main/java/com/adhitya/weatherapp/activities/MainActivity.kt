package com.adhitya.weatherapp.activities

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.adhitya.weatherapp.BuildConfig
import com.adhitya.weatherapp.R
import com.adhitya.weatherapp.activities.MainActivity.Companion.API_KEY
import com.adhitya.weatherapp.databinding.ActivityMainBinding
import com.adhitya.weatherapp.model.CurrentDailyForecast
import com.adhitya.weatherapp.model.CurrentWeather
import com.adhitya.weatherapp.service.ApiConfig
import com.google.gson.Gson
import kotlinx.android.synthetic.main.add_city_dialog.*
import org.w3c.dom.Text
import retrofit2.Call
import retrofit2.Response
import java.util.*
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), LocationListener {


    companion object {
        const val TAG = "MainActivity"
        const val API_KEY = "1f12388d56b3f38bccb2e3c0d57c5adc"
    }
    private val metric: String = "metric"
    private lateinit var activityMainBinding: ActivityMainBinding
    private val spinnerArr = arrayListOf<String>()
    private lateinit var currentLocation: String
    private var long : Double? = null
    private var lati : Double? = null
    private val exclude: String = "minutely,hourly,alerts"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)
        getCurrentDate()
        loadSpinner()
        getLongLati()
        activityMainBinding.btnRefresh.setOnClickListener {
            loadSelectedLocationData(currentLocation)
        }
        activityMainBinding.btnAddOwnTown.setOnClickListener {
            showDialog()
        }
    }

    private fun getLongLati() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 115)
            return
        }
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val criteria = Criteria()
        val provider = locationManager.getBestProvider(criteria, true)
        val location = provider?.let { locationManager.getLastKnownLocation(it) }
        if (location != null) {
            onLocationChanged(location)
        } else {
            if (provider != null) {
                locationManager.requestLocationUpdates(provider, 20000, 0f, this)
            } else {
                Log.d(TAG, "getCurrentLocation: Not found")
            }
        }
    }

    private fun showDialog(){
        val builder = AlertDialog.Builder(this@MainActivity)
        val inflater : LayoutInflater = this.layoutInflater
        val view : View = inflater.inflate(R.layout.add_city_dialog, null)
        val edtCity : EditText = view.findViewById(R.id.edt_city)
        builder.setTitle(R.string.add_own_town)
        builder.setView(view).setPositiveButton(
            "Add", DialogInterface.OnClickListener{ dialog, id ->
                val city = edtCity.text.toString()
                Log.d(TAG, "showDialog: $city")
                spinnerArr.add(city)
                dialog.dismiss()
            }
        )
            .setNegativeButton(
                "Cancel", DialogInterface.OnClickListener { dialog, id ->
                    dialog.cancel()
                }
            )
        builder.create()
        builder.show()
    }


    private fun loadSpinner() {
        spinnerArr.add("Choose your location..")
        spinnerArr.add("Gdansk")
        spinnerArr.add("Warszawa")
        spinnerArr.add("Krakow")
        spinnerArr.add("Wroclaw")
        spinnerArr.add("Lodz")
        spinnerArr.add("Current Location")
        val spinner: Spinner = activityMainBinding.spLocation
        var arrrayAdapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, spinnerArr)
        arrrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = arrrayAdapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position != 0 && position != 6) {
                    currentLocation = spinnerArr[position].toString()
                    loadSelectedLocationData(spinnerArr[position])
                } else if (position == 6) {
                    getLongLati()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }

    }

    private fun getCurrentDate() {
        val date = DateFormat.format("EEEE, dd MMMM yyyy", Calendar.getInstance().time)
        activityMainBinding.tvCurrentDate.text = date.toString()
    }

    private fun loadCurrentLoc() {
        activityMainBinding.progressBar.visibility = View.VISIBLE
        val client =
            long?.let { lati?.let { it1 -> ApiConfig.getApiService().getCurrentWeather(it1, it, metric, API_KEY) } }
        client?.enqueue(object : retrofit2.Callback<CurrentWeather>{
            override fun onResponse(
                call: Call<CurrentWeather>,
                response: Response<CurrentWeather>
            ) {
                if (response.isSuccessful) {
                    Log.d(TAG, "onResponse: ${Gson().toJson(response.body())}")
                    with(activityMainBinding) {
                        tvWeatherCondition.text = response.body()?.weather?.get(0)?.description.toString().capitalize()
                        tvWeatherLocation.text = response.body()?.name
                        tvWeatherCurrentTemp.text = response.body()?.main?.temp?.roundToInt().toString()
                        tvWeatherCurrentHighTemp.text = response.body()?.main?.tempMax?.roundToInt().toString()
                        tvWeatherCurrentLowTemp.text = response.body()?.main?.tempMin?.roundToInt().toString()
                        tvHumidity.text = response.body()?.main?.humidity.toString()
                        tvPressure.text = response.body()?.main?.pressure.toString()
                        tvWind.text = response.body()?.wind?.speed?.roundToInt().toString()
                    }
                        currentLocation = response.body()?.name.toString()
                    long?.let { lati?.let { it1 -> loadDailyForecast(it, it1) } }
                    populateWeatherIcon(response.body()?.weather?.get(0)?.description.toString())


                } else {
                    activityMainBinding.progressBar.visibility = View.GONE
                    Log.d(TAG, "onResponseFailure: ${response.message()}")
                    Toast.makeText(this@MainActivity, "Terjadi kesalahan.\n${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CurrentWeather>, t: Throwable) {
                activityMainBinding.progressBar.visibility = View.GONE
                Log.d(TAG, "onFailure: ${t.message.toString()}")
                Toast.makeText(this@MainActivity, "Terjadi kesalahan.\n" +
                        t.message.toString(), Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadSelectedLocationData(location:String) {
        activityMainBinding.progressBar.visibility = View.VISIBLE
        val client = ApiConfig.getApiService().getWeather(location, metric, API_KEY)
        client.enqueue(object : retrofit2.Callback<CurrentWeather>{
            override fun onResponse(
                call: Call<CurrentWeather>,
                response: Response<CurrentWeather>
            ) {
                if (response.isSuccessful) {
                    Log.d(TAG, "onResponse: ${Gson().toJson(response.body())}")
                    with(activityMainBinding) {
                        tvWeatherCondition.text = response.body()?.weather?.get(0)?.description.toString().capitalize()
                        tvWeatherLocation.text = response.body()?.name
                        tvWeatherCurrentTemp.text = response.body()?.main?.temp?.roundToInt().toString()
                        tvWeatherCurrentHighTemp.text = response.body()?.main?.tempMax?.roundToInt().toString()
                        tvWeatherCurrentLowTemp.text = response.body()?.main?.tempMin?.roundToInt().toString()
                        tvHumidity.text = response.body()?.main?.humidity.toString()
                        tvPressure.text = response.body()?.main?.pressure.toString()
                        tvWind.text = response.body()?.wind?.speed?.roundToInt().toString()
                    }
                    currentLocation = response.body()?.name.toString()
                    long = response.body()?.coord?.lon
                    lati = response.body()?.coord?.lat
                    long?.let { lati?.let { it1 -> loadDailyForecast(it, it1) } }
                    populateWeatherIcon(response.body()?.weather?.get(0)?.description.toString())
                } else {
                    activityMainBinding.progressBar.visibility = View.GONE
                    Log.d(TAG, "onResponseFailure: ${response.message()}")
                    Toast.makeText(this@MainActivity, "Terjadi kesalahan.\n${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CurrentWeather>, t: Throwable) {
                activityMainBinding.progressBar.visibility = View.GONE
                Log.d(TAG, "onFailure: ${t.message.toString()}")
                Toast.makeText(this@MainActivity, "Terjadi kesalahan.\n" +
                        t.message.toString(), Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onLocationChanged(location: Location) {
        long = location.longitude
        lati = location.latitude

        loadCurrentLoc()
    }

    fun loadDailyForecast(long: Double, lat:Double){
        activityMainBinding.llDaily.visibility = View.INVISIBLE
        val client = ApiConfig.getApiService().getCurrentDailyForecast(lat, long, exclude, metric, API_KEY)
        client.enqueue(object : retrofit2.Callback<CurrentDailyForecast> {
            override fun onResponse(
                call: Call<CurrentDailyForecast>,
                response: Response<CurrentDailyForecast>
            ) {
                if (response.isSuccessful) {
                    Log.d(TAG, "onResponseDailyForecast: ${Gson().toJson(response.body())}")
                    with(activityMainBinding){
                        tvMonTemp.text = response.body()?.daily?.get(0)?.temp?.day?.roundToInt().toString()
                        tvTueTemp.text = response.body()?.daily?.get(1)?.temp?.day?.roundToInt().toString()
                        tvWedTemp.text = response.body()?.daily?.get(2)?.temp?.day?.roundToInt().toString()
                        tvThuTemp.text = response.body()?.daily?.get(3)?.temp?.day?.roundToInt().toString()
                        tvFriTemp.text = response.body()?.daily?.get(4)?.temp?.day?.roundToInt().toString()
                        tvSatTemp.text = response.body()?.daily?.get(5)?.temp?.day?.roundToInt().toString()
                        tvSunTemp.text = response.body()?.daily?.get(6)?.temp?.day?.roundToInt().toString()
                    }
                    activityMainBinding.progressBar.visibility = View.GONE
                    activityMainBinding.llDaily.visibility = View.VISIBLE
                } else {
                    activityMainBinding.progressBar.visibility = View.GONE
                    Log.d(TAG, "onResponseFailure: ${response.message()}")
                    Toast.makeText(this@MainActivity, "Terjadi kesalahan.\n${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CurrentDailyForecast>, t: Throwable) {
                activityMainBinding.progressBar.visibility = View.GONE
                Log.d(TAG, "onFailure: ${t.message.toString()}")
                Toast.makeText(this@MainActivity, "Terjadi kesalahan.\n" +
                        t.message.toString(), Toast.LENGTH_SHORT).show()
            }

        })
    }

    private fun populateWeatherIcon(condition: String) {
        with(activityMainBinding) {
            when(condition) {
                "broken clouds" -> ivWeather.setAnimation(R.raw.broken_clouds)
                "light rain" -> ivWeather.setAnimation(R.raw.light_rain)
                "haze" -> ivWeather.setAnimation(R.raw.broken_clouds)
                "overcast clouds" -> ivWeather.setAnimation(R.raw.overcast_clouds)
                "few clouds" -> ivWeather.setAnimation(R.raw.few_clouds)
                "moderate rain" -> ivWeather.setAnimation(R.raw.moderate_rain)
                "scattered clouds" -> ivWeather.setAnimation(R.raw.scattered_clouds)
                "heavy intensity rain" -> ivWeather.setAnimation(R.raw.heavy_intentsity)
                "clear sky" -> ivWeather.setAnimation(R.raw.clear_sky)
                else -> ivWeather.setAnimation(R.raw.unknown)
            }
            ivWeather.playAnimation()
        }
    }

}