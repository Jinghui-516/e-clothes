package tw.edu.pu.csim.s1120336.e_fit.clothes

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import tw.edu.pu.csim.s1120336.e_fit.R
import tw.edu.pu.csim.s1120336.e_fit.weather.RetrofitClient
import tw.edu.pu.csim.s1120336.e_fit.weather.WeatherResponse
import tw.edu.pu.csim.s1120336.e_fit.weather.WeatherService
import java.util.Locale

class Chat_AI : AppCompatActivity() {

    // 1. 宣告與 XML 對應的 UI 元件
    private lateinit var btnBack: ImageButton
    private lateinit var btnWork: Button
    private lateinit var btnSchool: Button
    private lateinit var btnCasual: Button
    private lateinit var btnSport: Button
    private lateinit var etCustomScenario: EditText
    private lateinit var spinnerLocation: Spinner
    private lateinit var tvWeatherInfo: TextView
    private lateinit var ivWeatherIcon: ImageView
    private lateinit var btnGenerate: Button
    private lateinit var rvRecommendedClothes: RecyclerView
    private lateinit var tvRecommendationTitle: TextView

    // 2. 宣告 API 與定位變數
    private lateinit var weatherService: WeatherService
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var currentSelectedScenario: String = ""
    private var currentTemperature: Int = 25

    private val taiwanCities = arrayOf(
        "基隆市", "臺北市", "新北市", "桃園市", "新竹縣", "新竹市", "苗栗縣",
        "臺中市", "彰化縣", "南投縣", "雲林縣", "嘉義縣", "嘉義市", "臺南市",
        "高雄市", "屏東縣", "宜蘭縣", "花蓮縣", "臺東縣", "澎湖縣", "金門縣", "連江縣"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 確保這裡綁定的是妳剛剛貼的那份 XML 檔案
        setContentView(R.layout.activity_chat_ai)

        initViews()
        setupScenarioButtons()
        setupLocationAndWeather()

        btnBack.setOnClickListener { finish() }
        btnGenerate.setOnClickListener { generateOutfitRecommendation() }
    }

    private fun initViews() {
        // 這裡的 ID 完全照著妳貼給我的 XML 打的，保證不會再報錯！
        btnBack = findViewById(R.id.btn_chat_back)
        btnWork = findViewById(R.id.btn_scenario_work)
        btnSchool = findViewById(R.id.btn_scenario_school)
        btnCasual = findViewById(R.id.btn_scenario_casual)
        btnSport = findViewById(R.id.btn_scenario_sport)
        etCustomScenario = findViewById(R.id.et_custom_scenario)
        spinnerLocation = findViewById(R.id.spinner_location)
        tvWeatherInfo = findViewById(R.id.tv_weather_info)
        ivWeatherIcon = findViewById(R.id.iv_weather_icon)
        btnGenerate = findViewById(R.id.btn_generate_recommendation)
        rvRecommendedClothes = findViewById(R.id.rv_recommended_clothes)
        tvRecommendationTitle = findViewById(R.id.tv_recommendation_title)
    }

    private fun setupScenarioButtons() {
        val buttons = listOf(btnWork, btnSchool, btnCasual, btnSport)
        val scenarios = listOf("職場商務", "校園日常", "休閒娛樂", "戶外運動")

        for (i in buttons.indices) {
            buttons[i].setOnClickListener {
                etCustomScenario.text.clear()
                currentSelectedScenario = scenarios[i]

                buttons.forEach { btn ->
                    btn.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
                    btn.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                }

                buttons[i].setBackgroundColor(ContextCompat.getColor(this, R.color.brand_primary))
                buttons[i].setTextColor(ContextCompat.getColor(this, R.color.white))
            }
        }
    }

    private fun setupLocationAndWeather() {
        weatherService = RetrofitClient.service
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, taiwanCities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLocation.adapter = adapter

        spinnerLocation.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCity = taiwanCities[position]
                tvWeatherInfo.text = "資料讀取中..."
                getWeatherForAi(selectedCity)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        getLocationAndSetSpinner()
    }

    private fun getLocationAndSetSpinner() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (addresses?.isNotEmpty() == true) {
                        var city = addresses[0].adminArea ?: ""
                        city = city.replace("台", "臺")
                        val cityIndex = taiwanCities.indexOf(city)
                        if (cityIndex >= 0) spinnerLocation.setSelection(cityIndex)
                    }
                }
            }
        } else {
            spinnerLocation.setSelection(taiwanCities.indexOf("臺中市"))
        }
    }

    private fun getWeatherForAi(locationCity: String) {
        val authorization = "CWA-A017743C-C744-4C39-9D6B-4CA2D7E6E086"
        weatherService.getWeatherApi(authorization, locationCity).enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (!response.isSuccessful) return
                val weatherResponse = response.body()
                weatherResponse?.records?.location?.firstOrNull { it.locationName == locationCity }?.let { location ->
                    val firstTimeBlock = location.weatherElement.firstOrNull()?.time?.firstOrNull()
                    if (firstTimeBlock != null) {
                        val weatherCondition = firstTimeBlock.parameter.parameterName
                        val temperatureStr = firstTimeBlock.parameter.parameterName
                        currentTemperature = temperatureStr.filter { it.isDigit() }.toIntOrNull() ?: 25
                        runOnUiThread {
                            tvWeatherInfo.text = "$weatherCondition | $temperatureStr°C"
                            setWeatherImage(ivWeatherIcon, weatherCondition)
                        }
                    }
                }
            }
            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                runOnUiThread { tvWeatherInfo.text = "天氣讀取失敗" }
            }
        })
    }

    private fun generateOutfitRecommendation() {
        val customText = etCustomScenario.text.toString().trim()
        val finalScenario = if (customText.isNotEmpty()) customText else currentSelectedScenario

        if (finalScenario.isEmpty()) {
            Toast.makeText(this, "請先選擇一個情境或輸入特殊場合喔！", Toast.LENGTH_SHORT).show()
            return
        }

        val seasonTag = when {
            currentTemperature >= 28 -> "炎熱(薄)"
            currentTemperature in 20..27 -> "涼爽(適中)"
            else -> "寒冷(厚)"
        }

        Toast.makeText(this, "正在為您尋找適合【$finalScenario】的穿搭...", Toast.LENGTH_SHORT).show()
        tvRecommendationTitle.visibility = View.VISIBLE
        rvRecommendedClothes.visibility = View.VISIBLE
    }

    private fun setWeatherImage(imageView: ImageView, weatherCondition: String) {
        when (weatherCondition) {
            "多雲","陰天" -> imageView.setImageResource(R.drawable.cloudy)
            "晴時多雲","多雲時晴" -> imageView.setImageResource(R.drawable.cloudy_and_sunny)
            "雨天","陣雨" -> imageView.setImageResource(R.drawable.raining)
            "晴天" -> imageView.setImageResource(R.drawable.sunny)
            else -> imageView.setImageResource(R.drawable.cloudy_and_sunny) // 預設圖示，避免落落長的判斷式卡住
        }
    }
}