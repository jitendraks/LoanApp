package com.example.myapplication.data

data class LoanType (
    val typeOfLoanId: Int,
    val name: String
) {
    override fun toString(): String {
        return name
    }
}
