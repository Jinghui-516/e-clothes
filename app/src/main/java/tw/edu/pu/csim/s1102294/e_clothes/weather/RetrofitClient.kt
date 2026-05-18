package tw.edu.pu.csim.s1120336.e_fit.weather

import retrofit2.Call
import retrofit2.Retrofit
// 1. 刪除這行錯誤的 import
// import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // 2. 移除 ": WeatherService" (除非你想手動實作它)

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://opendata.cwa.gov.tw/api/v1/rest/datastore/")
            .addConverterFactory(GsonConverterFactory.create())
            // 3. 刪除這行 addCallAdapterFactory
            .build()
    }

    val service: WeatherService by lazy {
        retrofit.create(WeatherService::class.java)
    }
}