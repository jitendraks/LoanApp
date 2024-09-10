package com.example.myapplication.data

data class IncomeSlab(
    val incomeId: Int,
    val slabName: String
) {
    override fun toString(): String {
        return slabName
    }
}
