package com.example.myapplication.data

data class VehicleStatus(
    val vehicleStatusId: Int,
    val name: String
) {
    override fun toString(): String {
        return name
    }
}
