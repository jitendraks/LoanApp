package com.example.myapplication.data

data class Litigation(
    val litigationId: Int,
    val name: String
) {
    override fun toString(): String {
        return name
    }

    companion object {
        fun getLitigationById(litigations: List<Litigation>, litigationId: Int): Litigation? {
            return litigations.find { it.litigationId == litigationId }
        }
    }
}