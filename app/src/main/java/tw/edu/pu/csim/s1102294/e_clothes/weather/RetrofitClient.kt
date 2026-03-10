package tw.edu.pu.csim.s1102294.e_clothes.weather

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient : WeatherService {
    fun myWeatherApi(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://opendata.cwa.gov.tw/api/v1/rest/datastore/")
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .build()
    }

    override fun getWeatherApi(
        authorization: String,
        lacation: String
    ): Call<WeatherResponse> {
        TODO("Not yet implemented")
    }
}