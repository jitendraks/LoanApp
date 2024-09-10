package com.example.myapplication.data

data class Litigation(
    val litigationId: Int,
    val name: String
) {
    override fun toString(): String {
        return name
    }
}