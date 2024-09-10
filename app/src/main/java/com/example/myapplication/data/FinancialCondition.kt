package com.example.myapplication.data

data class FinancialCondition(
    val finConditionId: Int,
    val name: String,
    val incomeId: Int
) {
    override fun toString(): String {
        return name
    }
}