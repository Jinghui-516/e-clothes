package tw.edu.pu.csim.s1120336.e_fit.clothes

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import tw.edu.pu.csim.s1120336.e_fit.R
import tw.edu.pu.csim.s1120336.e_fit.weather.RetrofitClient
import tw.edu.pu.csim.s1120336.e_fit.weather.WeatherResponse
import tw.edu.pu.csim.s1120336.e_fit.weather.WeatherService
import java.util.Locale

class Chat_AI : AppCompatActivity() {

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
        setContentView(R.layout.activity_chat_ai)

        initViews()
        setupScenarioButtons()
        setupLocationAndWeather()

        btnBack.setOnClickListener { finish() }
        btnGenerate.setOnClickListener { generateOutfitRecommendation() }
    }

    private fun initViews() {
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

        rvRecommendedClothes.layoutManager = GridLayoutManager(this, 3)
    }

    private fun setupScenarioButtons() {
        val buttons = listOf(btnWork, btnSchool, btnCasual, btnSport)
        val scenarios = listOf("職場商務", "校園日常", "休閒娛樂", "戶外運動")

        for (i in buttons.indices) {
            buttons[i].setOnClickListener {
                // 🌟 判斷：如果再次點擊已經選中的按鈕 -> 取消選取
                if (currentSelectedScenario == scenarios[i]) {
                    currentSelectedScenario = ""
                    buttons[i].setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
                    buttons[i].setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                } else {
                    // 🌟 否則 -> 正常選取，並清除其他按鈕顏色與自訂輸入框
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

                    val wxElement = location.weatherElement.getOrNull(0)?.time?.firstOrNull()?.parameter?.parameterName
                    val tempElement = location.weatherElement.getOrNull(2)?.time?.firstOrNull()?.parameter?.parameterName

                    if (wxElement != null && tempElement != null) {
                        currentTemperature = tempElement.filter { it.isDigit() }.toIntOrNull() ?: 25

                        val seasonHint = when {
                            currentTemperature >= 28 -> "(28°C以上 建議炎熱薄衣物)"
                            currentTemperature in 20..27 -> "(20~27°C 建議涼爽適中衣物)"
                            else -> "(20°C以下 建議寒冷厚衣物)"
                        }

                        runOnUiThread {
                            tvWeatherInfo.text = "$wxElement | $tempElement°C\n$seasonHint"
                            setWeatherImage(ivWeatherIcon, wxElement)
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

        val userEmail = FirebaseAuth.getInstance().currentUser?.email
        if (userEmail == null) {
            Toast.makeText(this, "無法取得使用者資訊，請重新登入", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "AI 正在尋找【$finalScenario】且適合【$seasonTag】的穿搭...", Toast.LENGTH_SHORT).show()

        val db = FirebaseFirestore.getInstance()
        db.collection(userEmail)
            .get()
            .addOnSuccessListener { documents ->
                val imageUrls = mutableListOf<String>()

                for (document in documents) {
                    if (document.contains("服裝種類")) {
                        val dbScenarios = document.get("適合情境") as? List<String> ?: emptyList()
                        val dbSeason = document.getString("適合氣溫") ?: ""
                        val dbTags = document.get("標籤") as? List<String> ?: emptyList()

                        val isScenarioMatch = dbScenarios.contains(finalScenario)
                        val isSeasonMatch = dbSeason == seasonTag

                        val isCustomMatch = customText.isNotEmpty() && (
                                dbScenarios.any { it.contains(customText) } ||
                                        dbTags.any { it.contains(customText) }
                                )

                        if ((isScenarioMatch || isCustomMatch) && isSeasonMatch) {
                            val imageUrl = document.getString("圖片完整網址") ?: document.getString("圖片網址")
                            if (!imageUrl.isNullOrEmpty()) {
                                imageUrls.add(imageUrl)
                            }
                        }
                    }
                }

                if (imageUrls.isEmpty()) {
                    Toast.makeText(this, "衣櫃裡好像沒有符合【$finalScenario + $seasonTag】的衣服喔！", Toast.LENGTH_LONG).show()
                    rvRecommendedClothes.visibility = View.GONE
                    tvRecommendationTitle.visibility = View.GONE
                } else {
                    tvRecommendationTitle.text = "✨ 推薦妳的【$finalScenario】單品"
                    tvRecommendationTitle.visibility = View.VISIBLE
                    rvRecommendedClothes.visibility = View.VISIBLE
                    Toast.makeText(this, "為您精選了 ${imageUrls.size} 件穿搭！", Toast.LENGTH_SHORT).show()

                    rvRecommendedClothes.adapter = AiClothesAdapter(imageUrls)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "讀取衣櫃失敗：${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("AI_Clothes", "Firebase Error: ", e)
            }
    }

    private fun setWeatherImage(imageView: ImageView, weatherCondition: String) {
        when (weatherCondition) {
            "多雲","陰天" -> imageView.setImageResource(R.drawable.cloudy)
            "晴時多雲","多雲時晴" -> imageView.setImageResource(R.drawable.cloudy_and_sunny)
            "雨天","陣雨" -> imageView.setImageResource(R.drawable.raining)
            "晴天" -> imageView.setImageResource(R.drawable.sunny)
            else -> imageView.setImageResource(R.drawable.cloudy_and_sunny)
        }
    }

    inner class AiClothesAdapter(private val imageUrls: List<String>) : RecyclerView.Adapter<AiClothesAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivCloth: ImageView = view.findViewById(R.id.iv_cloth_image)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ai_clothes, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            Glide.with(holder.itemView.context)
                .load(imageUrls[position])
                .centerCrop()
                .into(holder.ivCloth)
        }

        override fun getItemCount() = imageUrls.size
    }
}