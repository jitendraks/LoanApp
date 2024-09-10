package com.example.myapplication.data

data class VisitDone(
    val visitDoneId: Int,
    val name: String
) {
    override fun toString(): String {
        return name
    }
}
