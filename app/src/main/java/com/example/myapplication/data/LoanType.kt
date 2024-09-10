package com.example.myapplication.data

data class LoanType (
    val typeOfLoanId: Int,
    val name: String
) {
    override fun toString(): String {
        return name
    }

    companion object {
        fun getLoanTypeById(loanTypes: List<LoanType>, loanTypeId: Int): LoanType? {
            return loanTypes.find { it.typeOfLoanId == loanTypeId }
        }
    }
}
