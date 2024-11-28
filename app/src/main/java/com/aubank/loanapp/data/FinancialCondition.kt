package com.aubank.loanapp.data

data class FinancialCondition(
    val finConditionId: Int,
    val name: String,
    val incomeId: Int
) {
    override fun toString(): String {
        return name
    }

    companion object {
        fun getFinancialConditionById(finConditions: List<FinancialCondition>, finConditionId: Int): FinancialCondition? {
            return finConditions.find { it.finConditionId == finConditionId }
        }
    }
}