package com.example.myapplication.utils

import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

object DateTimeFormatter {

    private const val DATE_FORMAT = "MM/dd/yyyy"
    private const val TIME_FORMAT = "HH:mm"
    private const val DATE_TIME_FORMAT = "MM/dd/yyyy HH:mm"

    fun formatDate(date: LocalDateTime): String {
        return format(date, DATE_FORMAT)
    }

    fun formatTime(date: LocalDateTime): String {
        return format(date, TIME_FORMAT)
    }

    fun parseTime(date: String): LocalDateTime {
        return parse(date, TIME_FORMAT)
    }

    fun formatDateTime(date: LocalDateTime): String {
        return format(date, DATE_TIME_FORMAT)
    }

    private fun format(date: LocalDateTime, pattern: String): String {
        val formatter = DateTimeFormatter.ofPattern(pattern)
        return date.format(formatter)
    }

    private fun parse(date: String, pattern: String): LocalDateTime {
        val formatter = DateTimeFormatter.ofPattern(pattern)
        return formatter.parse(date) as LocalDateTime
    }
}