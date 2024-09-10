package com.example.myapplication.data

data class VisitDone(
    val visitDoneId: Int,
    val name: String
) {
    override fun toString(): String {
        return name
    }

    companion object {
        fun getVisitDoneById(visitDoneList: List<VisitDone>, visitDoneId: Int): VisitDone? {
            return visitDoneList.find { it.visitDoneId == visitDoneId }
        }
    }
}
