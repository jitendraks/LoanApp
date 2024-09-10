package com.example.myapplication.data

data class IncomeSlab(
    val incomeId: Int,
    val slabName: String
) {
    override fun toString(): String {
        return slabName
    }

    companion object {
        fun getIncomeSlabById(incomeSlabs: List<IncomeSlab>, incomeId: Int): IncomeSlab? {
            return incomeSlabs.find { it.incomeId == incomeId }
        }
    }
}
