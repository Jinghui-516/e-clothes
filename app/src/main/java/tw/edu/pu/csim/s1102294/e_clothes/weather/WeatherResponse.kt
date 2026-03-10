package tw.edu.pu.csim.s1102294.e_clothes.weather

data class WeatherResponse(
    val records: Records
)

data class Records(
    val location: List<Location>
)

data class Location(
    val locationName: String,
    val weatherElement: List<WeatherElement>
)

data class WeatherElement(
    val elementName: String,
    val time: List<Time>
)

data class Time(
    val startTime: String,
    val endTime: String,
    val parameter: Parameter
)

data class Parameter(
    val parameterName: String,
    val parameterUnit: String
)
