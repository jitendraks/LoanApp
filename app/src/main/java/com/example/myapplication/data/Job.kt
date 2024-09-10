package com.example.myapplication.data

data class Job(
    val jobId: Int,
    val name: String
) {
    override fun toString(): String {
        return name
    }
}
