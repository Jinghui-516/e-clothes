package tw.edu.pu.csim.s1102294.e_clothes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import tw.edu.pu.csim.s1102294.e_clothes.Community.Friends
import tw.edu.pu.csim.s1102294.e_clothes.Community.Personal_Page
import tw.edu.pu.csim.s1102294.e_clothes.Match.Match_home
import tw.edu.pu.csim.s1102294.e_clothes.clothes.New_clothes
import tw.edu.pu.csim.s1102294.e_clothes.weather.RetrofitClient
import tw.edu.pu.csim.s1102294.e_clothes.weather.WeatherResponse
import tw.edu.pu.csim.s1102294.e_clothes.weather.WeatherService
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import tw.edu.pu.csim.s1102294.e_clothes.Match.Match
import tw.edu.pu.csim.s1102294.e_clothes.Match.Matching_details
import tw.edu.pu.csim.s1102294.e_clothes.Match.edit_Profile
import tw.edu.pu.csim.s1102294.e_clothes.clothes.choose_add
import tw.edu.pu.csim.s1102294.e_clothes.weather.Time
import java.text.SimpleDateFormat
import java.util.*


class home : AppCompatActivity() {

    // Inner adapter class for match list
    class MatchAdapter(private var matchList: List<Match>) : RecyclerView.Adapter<MatchAdapter.MatchViewHolder>() {

        inner class MatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val matchNameTextView: TextView = itemView.findViewById(R.id.match_name)
            private val weatherCategoryTextView: TextView = itemView.findViewById(R.id.weather_category)
            private val hatImageView: ImageView = itemView.findViewById(R.id.hat_image)
            private val clothesImageView: ImageView = itemView.findViewById(R.id.clothes_image)
            private val pantsImageView: ImageView = itemView.findViewById(R.id.pants_image)
            private val shoesImageView: ImageView = itemView.findViewById(R.id.shoes_image)

            fun bind(match: Match) {
                matchNameTextView.text = match.搭配名稱
                weatherCategoryTextView.text = match.天氣種類

                // Load images
                Glide.with(itemView.context).load(match.帽子圖片網址).into(hatImageView)
                Glide.with(itemView.context).load(match.上衣圖片網址).into(clothesImageView)
                Glide.with(itemView.context).load(match.褲子圖片網址).into(pantsImageView)
                Glide.with(itemView.context).load(match.鞋子圖片網址).into(shoesImageView)

                itemView.setOnClickListener {
                    val intent = Intent(itemView.context, Matching_details::class.java)
                    intent.putExtra("matchData", match)  // Serializable or Parcelable
                    itemView.context.startActivity(intent)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.match_item, parent, false)
            return MatchViewHolder(view)
        }

        override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
            holder.bind(matchList[position])
        }

        override fun getItemCount(): Int = matchList.size

        fun updateMatches(newMatches: List<Match>) {
            matchList = newMatches
            notifyDataSetChanged()
        }
    }

    lateinit var Match: ImageView
    lateinit var Home: ImageView
    lateinit var Friend: ImageView
    lateinit var Clothes: ImageView
    lateinit var set: ImageView
    lateinit var profile: ImageView

//    private val LOCATION_PERMISSION_REQUEST_CODE = 1
//    private var locationManager: LocationManager? = null
    private lateinit var weatherService: WeatherService
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationTextView: TextView

    lateinit var today_morning_time: TextView
    lateinit var today_morning_weather: ImageView
    lateinit var today_morning_temperature: TextView

    lateinit var today_night_time: TextView
    lateinit var today_night_weather: ImageView
    lateinit var today_night_temperature: TextView

    lateinit var tomorrow_morning_time: TextView
    lateinit var tomorrow_morning_weather: ImageView
    lateinit var tomorrow_morning_temperature: TextView

    lateinit var tomorrow_night_time: TextView
    lateinit var tomorrow_night_weather: ImageView
    lateinit var tomorrow_night_temperature: TextView

    private val handler = Handler(Looper.getMainLooper())
    lateinit var locationCity: String
//    private val updateWeatherRunnable = object : Runnable {
//        override fun run() {
//            getWeather(locationCity)
//            handler.postDelayed(this, 3600000) // 1 hour
//        }
//    }

    lateinit var textView8: TextView
    private val matchList = mutableListOf<Match>()
    private lateinit var matchRecyclerView: RecyclerView
    private lateinit var matchAdapter: Match_home.MatchAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        textView8 = findViewById(R.id.textView8)
        profile = findViewById(R.id.profile)
        profile.setOnClickListener {
            val intent1 = Intent(this, Personal_Page::class.java)
            startActivity(intent1)
            finish()
        }

        Home = findViewById(R.id.Home)
        Home.setOnClickListener {
//            Home.text = ""
            val intent1 = Intent(this, home::class.java)
            startActivity(intent1)
            finish()
        }

        Match = findViewById(R.id.Match)
        Match.setOnClickListener {
//            Match.text = ""
            val intent2 = Intent(this, Match_home::class.java)
            startActivity(intent2)
            finish()
        }

        Clothes = findViewById(R.id.Clothes)
        Clothes.setOnClickListener {
//            textView9.text = "123"
//            checkPermission()
            val intent = Intent(this, choose_add::class.java)
            startActivity(intent)
            finish()
        }

        Friend = findViewById(R.id.Friend)
        Friend.setOnClickListener {
            val intent1 = Intent(this, Friends::class.java)
            startActivity(intent1)
            finish()
        }

        set = findViewById(R.id.set)
        set.setOnClickListener {
            val intent1 = Intent(this, Setting::class.java)
            startActivity(intent1)
            finish()
        }
        locationCity = "臺北市"
        locationTextView = findViewById(R.id.locationTextView)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
            return
        }

        val taiwanCities = mapOf(
            "台北市" to "臺北市",
            "新北市" to "新北市",
            "桃園市" to "桃園市",
            "台中市" to "臺中市",
            "台南市" to "臺南市",
            "高雄市" to "高雄市",
            "基隆市" to "基隆市",
            "新竹市" to "新竹市",
            "嘉義市" to "嘉義市",
            // 加入其他縣市
        )

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    if (addresses?.isNotEmpty() == true) {
                        val city = addresses[0].adminArea
                        val cityChinese = taiwanCities[city] ?: city  // 使用映射表轉換
                        locationTextView.text = cityChinese ?: "Unknown City"
                        getWeather(cityChinese)
                    }
                }
            }

        today_morning_time = findViewById(R.id.today_morning_time)
        today_morning_weather = findViewById(R.id.today_morning_weather)
        today_morning_temperature = findViewById(R.id.today_morning_temperature)

        today_night_time = findViewById(R.id.today_night_time)
        today_night_weather = findViewById(R.id.today_night_weather)
        today_night_temperature = findViewById(R.id.today_night_temperature)

        tomorrow_morning_time = findViewById(R.id.tomorrow_morning_time)
        tomorrow_morning_weather = findViewById(R.id.tomorrow_morning_weather)
        tomorrow_morning_temperature = findViewById(R.id.tomorrow_morning_temperature)

        tomorrow_night_time = findViewById(R.id.tomorrow_night_time)
        tomorrow_night_weather = findViewById(R.id.tomorrow_night_weather)
        tomorrow_night_temperature = findViewById(R.id.tomorrow_night_temperature)

        weatherService = RetrofitClient.myWeatherApi().create(WeatherService::class.java)
        locationCity = locationTextView.text.toString().trim()
//        getWeather(locationCity)

        matchRecyclerView = findViewById(R.id.match_recycler_view)
        matchRecyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize the adapter with an empty list
        matchAdapter = Match_home.MatchAdapter(emptyList())
        matchRecyclerView.adapter = matchAdapter

        // Fetch the "搭配" data from Firestore
        fetchMatchDataFromFirestore()

    }

    private fun fetchMatchDataFromFirestore() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val email = currentUser.email
            val db = FirebaseFirestore.getInstance()

            if (email != null) {
                db.collection(email)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (documents.isEmpty) {
                            Log.e("Firestore", "No documents found!")
                        } else {
                            // Process and filter only valid match data
                            for (document in documents) {
                                val match = document.toObject(tw.edu.pu.csim.s1102294.e_clothes.Match.Match::class.java)

                                // Check if the match object has the required fields
                                if (match.isValid()) {
                                    Log.d("MatchData", "Match: $match")  // 输出数据检查
                                    matchList.add(match)
                                } else {
                                    Log.d("MatchData", "Invalid match: $match")  // 输出无效数据检查
                                }
                            }

                            // Update UI with the fetched matches
                            runOnUiThread {
                                matchAdapter.updateMatches(matchList)
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("MatchActivity", "Error fetching data: ${e.message}")
                    }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // 權限已授予，重新嘗試取得位置
        } else {
            // 權限被拒絕
        }
    }

    private fun getWeather(locationCity: String) {
        val authorization = "CWA-A017743C-C744-4C39-9D6B-4CA2D7E6E086"
        weatherService.getWeatherApi(authorization, locationCity)
            .enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                    if (!response.isSuccessful) {
                        Log.e("WeatherError", "Response not successful: ${response.errorBody()?.string()}")
                        return
                    }

                    val weatherResponse = response.body()
                    weatherResponse?.records?.location?.firstOrNull { it.locationName == locationCity }?.let { location ->
                        location.weatherElement.forEach { weatherElement ->
                            handleWeatherTimes(weatherElement.time)
                        }
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Log.e("WeatherError", "API call failed: ${t.message}")
                }
            })
    }

    private fun handleWeatherTimes(times: List<Time>) {
        times.forEach { time ->
            val startTime = time.startTime
            val weatherCondition = time.parameter.parameterName
            val temperature = time.parameter.parameterName

            when {
                isTargetTime(startTime, getCurrentDate(), "06:00") -> updateUI(today_morning_time, today_morning_temperature, today_morning_weather, startTime, temperature, weatherCondition)
                isTargetTime(startTime, getCurrentDate(), "18:00") -> updateUI(today_night_time, today_night_temperature, today_night_weather, startTime, temperature, weatherCondition)
                isTargetTime(startTime, getNextDate(), "06:00") -> updateUI(tomorrow_morning_time, tomorrow_morning_temperature, tomorrow_morning_weather, startTime, temperature, weatherCondition)
                isTargetTime(startTime, getNextDate(), "18:00") -> updateUI(tomorrow_night_time, tomorrow_night_temperature, tomorrow_night_weather, startTime, temperature, weatherCondition)
            }
        }
    }

    private fun isTargetTime(startTime: String, date: String, hour: String): Boolean {
        return startTime.startsWith(date) && startTime.substring(11, 16) == hour
    }

    private fun updateUI(timeView: TextView, tempView: TextView, weatherView: ImageView, startTime: String, temperature: String, weatherCondition: String) {
        timeView.text = "${startTime.substring(0, 16)}  "
        tempView.text = "$temperature ˚C"
        setWeatherImage(weatherView, weatherCondition)
    }



    private fun setWeatherImage(imageView: ImageView, weatherCondition: String) {
        when (weatherCondition) {
            "多雲","陰天","多雲時陰有霧","多雲時陰晨霧","陰時多雲有霧","陰時多雲晨霧" -> imageView.setImageResource(R.drawable.cloudy)
            "晴時多雲","多雲時晴","多雲時陰","陰時多雲","晴時多雲有霧","晴時多雲晨霧","多雲時晴有霧","多雲時晴晨霧" -> imageView.setImageResource(R.drawable.cloudy_and_sunny)
            "多雲有霧","多雲晨霧","有霧","晨霧","陰有霧","陰晨霧" -> imageView.setImageResource(R.drawable.fog)
            "雨天","晴午後陰短暫雨","晴午後陰短暫陣雨","陰短暫雨","陰短暫陣雨","陰午後短暫陣雨","多雲時陰有雨","多雲時陰陣雨","晴時多雲陣雨","多雲時晴陣雨","陰時多雲有雨","陰時多雲有陣雨","陰時多雲陣雨","陰有雨","陰有陣雨",
            "陰雨","陰陣雨","陣雨","午後陣雨","有雨","多雲局部雨或雪有霧","多雲時陰局部雨或雪有霧","陰時多雲局部雨或雪有霧","陰局部雨或雪有霧","短暫雨或雪有霧","有雨或雪有霧","短暫陣雨有霧","短暫陣雨晨霧","短暫雨有霧",
            "短暫雨晨霧","有雨有霧","陣雨有霧","短暫陣雨或雷雨有霧","陣雨或雷雨有霧"-> imageView.setImageResource(R.drawable.raining)
            "多雲陣雨","多雲短暫雨","多雲短暫陣雨","午後短暫陣雨","短暫陣雨","多雲時晴短暫陣雨","多雲時晴短暫雨","晴時多雲短暫陣雨","晴短暫陣雨","短暫雨","多雲時陰短暫雨","多雲時陰短暫陣雨","陰時多雲短暫雨","陰時多雲短暫陣雨",
            "晴午後多雲局部雨","晴午後多雲局部陣雨","晴午後多雲局部短暫雨","晴午後多雲局部短暫陣雨","晴午後多雲短暫雨","晴午後多雲短暫陣雨","晴午後局部雨","晴午後局部陣雨","晴午後局部短暫雨","晴午後局部短暫陣雨","晴午後陣雨",
            "晴午後短暫雨","晴午後短暫陣雨","晴時多雲午後短暫陣雨","多雲午後局部雨","多雲午後局部陣雨","多雲午後局部短暫雨","多雲午後局部短暫陣雨","多雲午後陣雨","多雲午後短暫雨","多雲午後短暫陣雨","多雲時陰午後短暫陣雨",
            "陰時多雲午後短暫陣雨","多雲時晴午後短暫陣雨","多雲局部雨","多雲局部陣雨","多雲局部短暫雨","多雲局部短暫陣雨","多雲時陰局部雨","多雲時陰局部陣雨","多雲時陰局部短暫雨","多雲時陰局部短暫陣雨","晴午後陰局部雨",
            "晴午後陰局部陣雨","晴午後陰局部短暫雨","晴午後陰局部短暫陣雨","陰局部雨","陰局部陣雨","陰局部短暫雨","陰局部短暫陣雨","陰時多雲局部雨","陰時多雲局部陣雨","陰時多雲局部短暫雨","陰時多雲局部短暫陣雨","多雲有霧有局部雨",
            "多雲有霧有局部陣雨","多雲有霧有局部短暫雨","多雲有霧有局部短暫陣雨","多雲有霧有陣雨","多雲有霧有短暫雨","多雲有霧有短暫陣雨","多雲局部雨有霧","多雲局部雨晨霧","多雲局部陣雨有霧","多雲局部陣雨晨霧","多雲局部短暫雨有霧",
            "多雲局部短暫雨晨霧","多雲局部短暫陣雨有霧","多雲局部短暫陣雨晨霧","多雲陣雨有霧","多雲短暫雨有霧","多雲短暫雨晨霧","多雲短暫陣雨有霧","多雲短暫陣雨晨霧","有霧有短暫雨","有霧有短暫陣雨","多雲時陰有霧有局部雨","多雲時陰有霧有局部陣雨",
            "多雲時陰有霧有局部短暫雨","多雲時陰有霧有局部短暫陣雨","多雲時陰有霧有陣雨","多雲時陰有霧有短暫雨","多雲時陰有霧有短暫陣雨","多雲時陰局部雨有霧","多雲時陰局部陣雨有霧","多雲時陰局部短暫雨有霧","多雲時陰局部短暫陣雨有霧","多雲時陰陣雨有霧",
            "多雲時陰短暫雨有霧","多雲時陰短暫雨晨霧","多雲時陰短暫陣雨有霧","多雲時陰短暫陣雨晨霧","陰有霧有陣雨","陰局部雨有霧","陰局部陣雨有霧","陰局部短暫陣雨有霧","陰時多雲有霧有局部雨","陰時多雲有霧有局部陣雨","陰時多雲有霧有局部短暫雨",
            "陰時多雲有霧有局部短暫陣雨","陰時多雲有霧有陣雨","陰時多雲有霧有短暫雨","陰時多雲有霧有短暫陣雨","陰時多雲局部雨有霧","陰時多雲局部陣雨有霧","陰時多雲局部短暫雨有霧","陰時多雲局部短暫陣雨有霧","陰時多雲陣雨有霧","陰時多雲短暫雨有霧",
            "陰時多雲短暫雨晨霧","陰時多雲短暫陣雨有霧","陰時多雲短暫陣雨晨霧","陰陣雨有霧","陰短暫雨有霧","陰短暫雨晨霧","陰短暫陣雨有霧","陰短暫陣雨晨霧"-> imageView.setImageResource(R.drawable.shower)
            "多雲局部陣雨或雪","多雲時陰有雨或雪","多雲時陰短暫雨或雪","多雲短暫雨或雪","陰有雨或雪","陰時多雲有雨或雪","陰時多雲短暫雨或雪","陰短暫雨或雪","多雲時陰有雪","多雲時陰短暫雪","多雲短暫雪","陰有雪","陰時多雲有雪","陰時多雲短暫雪","陰短暫雪",
            "有雨或雪","有雨或短暫雪","陰有雨或短暫雪","陰時多雲有雨或短暫雪","多雲時陰有雨或短暫雪","多雲有雨或短暫雪","多雲有雨或雪","多雲時晴有雨或雪","晴時多雲有雨或雪","晴有雨或雪","短暫雨或雪","多雲時晴短暫雨或雪","晴短暫雨或雪","晴時多雲短暫雨或雪",
            "有雪","多雲有雪","多雲時晴有雪","晴時多雲有雪","晴有雪","短暫雪","多雲時晴短暫雪","晴時多雲短暫雪","晴短暫雪","下雪","積冰","暴風雪"-> imageView.setImageResource(R.drawable.snow)
            "晴天","晴有霧","晴晨霧" -> imageView.setImageResource(R.drawable.sunny)
            "多雲陣雨或雷雨","多雲短暫陣雨或雷雨","多雲短暫雷陣雨","多雲雷陣雨","短暫陣雨或雷雨後多雲","短暫雷陣雨後多雲","短暫陣雨或雷雨","晴時多雲短暫陣雨或雷雨","晴短暫陣雨或雷雨","多雲時晴短暫陣雨或雷雨","午後短暫雷陣雨","多雲時陰陣雨或雷雨","多雲時陰短暫陣雨或雷雨",
            "多雲時陰短暫雷陣雨","多雲時陰雷陣雨", "晴陣雨或雷雨","晴時多雲陣雨或雷雨","多雲時晴陣雨或雷雨","陰時多雲有雷陣雨","陰時多雲陣雨或雷雨","陰時多雲短暫陣雨或雷雨","陰時多雲短暫雷陣雨","陰時多雲雷陣雨","陰有陣雨或雷雨","陰有雷陣雨","陰陣雨或雷雨",
            "陰雷陣雨","晴午後陰短暫陣雨或雷雨","晴午後陰短暫雷陣雨", "陰短暫陣雨或雷雨","陰短暫雷陣雨","雷雨","陣雨或雷雨後多雲","陰陣雨或雷雨後多雲","陰短暫陣雨或雷雨後多雲","陰短暫雷陣雨後多雲","陰雷陣雨後多雲","雷陣雨後多雲","陣雨或雷雨","雷陣雨","午後雷陣雨",
            "晴午後多雲陣雨或雷雨","晴午後多雲雷陣雨","晴午後陣雨或雷雨","晴午後雷陣雨","晴午後多雲局部陣雨或雷雨","晴午後多雲局部短暫陣雨或雷雨","晴午後多雲局部短暫雷陣雨","晴午後多雲局部雷陣雨","晴午後多雲短暫陣雨或雷雨","晴午後多雲短暫雷陣雨","晴午後局部短暫雷陣雨",
            "晴午後局部雷陣雨","晴午後短暫雷陣雨","晴雷陣雨","晴時多雲雷陣雨","晴時多雲午後短暫雷陣雨","多雲午後局部陣雨或雷雨","多雲午後局部短暫陣雨或雷雨","多雲午後局部短暫雷陣雨","多雲午後局部雷陣雨","多雲午後陣雨或雷雨","多雲午後短暫陣雨或雷雨","多雲午後短暫雷陣雨",
            "多雲午後雷陣雨","多雲時晴雷陣雨","多雲時晴午後短暫雷陣雨","多雲時陰午後短暫雷陣雨","陰時多雲午後短暫雷陣雨","陰午後短暫雷陣雨","多雲局部陣雨或雷雨","多雲局部短暫陣雨或雷雨","多雲局部短暫雷陣雨","多雲局部雷陣雨","多雲時陰局部陣雨或雷雨","多雲時陰局部短暫陣雨或雷雨",
            "多雲時陰局部短暫雷陣雨","多雲時陰局部雷陣雨","晴午後陰局部陣雨或雷雨","晴午後陰局部短暫陣雨或雷雨","晴午後陰局部短暫雷陣雨","晴午後陰局部雷陣雨","陰局部陣雨或雷雨","陰局部短暫陣雨或雷雨","陰局部短暫雷陣雨","陰局部雷陣雨","陰時多雲局部陣雨或雷雨",
            "陰時多雲局部短暫陣雨或雷雨","陰時多雲局部短暫雷陣雨","陰時多雲局部雷陣雨","多雲有陣雨或雷雨有霧","多雲有雷陣雨有霧","多雲有霧有陣雨或雷雨","多雲有霧有雷陣雨","多雲局部陣雨或雷雨有霧","多雲局部短暫陣雨或雷雨有霧","多雲局部短暫雷陣雨有霧",
            "多雲局部雷陣雨有霧","多雲陣雨或雷雨有霧","多雲短暫陣雨或雷雨有霧","多雲短暫雷陣雨有霧","多雲雷陣雨有霧","多雲時晴短暫陣雨或雷雨有霧","多雲時陰有陣雨或雷雨有霧","多雲時陰有雷陣雨有霧","多雲時陰有霧有陣雨或雷雨","多雲時陰有霧有雷陣雨","多雲時陰局部陣雨或雷雨有霧",
            "多雲時陰局部短暫陣雨或雷雨有霧","多雲時陰局部短暫雷陣雨有霧","多雲時陰局部雷陣雨有霧","多雲時陰陣雨或雷雨有霧","多雲時陰短暫陣雨或雷雨有霧","多雲時陰短暫雷陣雨有霧","多雲時陰雷陣雨有霧","陰局部陣雨或雷雨有霧","陰局部短暫陣雨或雷雨有霧","陰局部短暫雷陣雨有霧",
            "陰局部雷陣雨有霧","陰時多雲有陣雨或雷雨有霧","陰時多雲有雷陣雨有霧","陰時多雲有霧有陣雨或雷雨","陰時多雲有霧有雷陣雨","陰時多雲局部陣雨或雷雨有霧","陰時多雲局部短暫陣雨或雷雨有霧","陰時多雲局部短暫雷陣雨有霧","陰時多雲局部雷陣雨有霧","陰時多雲陣雨或雷雨有霧",
            "陰時多雲短暫陣雨或雷雨有霧","陰時多雲短暫雷陣雨有霧","陰時多雲雷陣雨有霧","陰短暫陣雨或雷雨有霧","陰短暫雷陣雨有霧","雷陣雨有霧"-> imageView.setImageResource(R.drawable.thunderstorm)

        }
    }
    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun getNextDate(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, 1)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

}

