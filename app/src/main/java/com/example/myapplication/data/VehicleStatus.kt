package com.example.myapplication.data

data class VehicleStatus(
    val vehicleStatusId: Int,
    val name: String
) {
    override fun toString(): String {
        return name
    }

    companion object {
        fun getVehicleStatusById(vehicleStatuses: List<VehicleStatus>, vehicleStatusId: Int): VehicleStatus? {
            return vehicleStatuses.find { it.vehicleStatusId == vehicleStatusId }
        }
    }
}
