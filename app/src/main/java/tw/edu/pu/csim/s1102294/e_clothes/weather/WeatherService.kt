package tw.edu.pu.csim.s1102294.e_clothes.weather

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {
    @GET("F-C0032-001")
    fun getWeatherApi(
        @Query("Authorization") authorization: String,
        @Query("location") location: String
    ): Call<WeatherResponse>
}