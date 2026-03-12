package app.dauphin.models

import kotlinx.serialization.Serializable

@Serializable
data class CourseResponse(
    val stuelelist: List<CourseItem>
)

@Serializable
data class CourseItem(
    val ch_cos_name: String,
    val en_cos_name: String,
    val time_plase: String,
    val seat_no: String,
    val teach_name: String,
    val teach_name_en: String,
    val note: String,
    val week: String,
    val sess1: String,
    val sess2: String,
    val sess3: String,
    val cos_no: String,
    val cos_ele_seq: String,
    val remark: String,
    val room: String,
    val timePlase: TimePlaseInfo
)

@Serializable
data class TimePlaseInfo(
    val week: String,
    val sesses: List<String>,
    val room: String
)

@Serializable
data class RawCourseItem(
    val weekno: String,
    val sessno: String,
    val week: String,
    val sesstime: String,
    val seatno: String,
    val ch_cos_name: String,
    val en_cos_name: String,
    val teach_name: String,
    val teach_name_en: String,
    val note: String,
    val room: String
)
