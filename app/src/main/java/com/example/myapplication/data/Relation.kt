package com.example.myapplication.data

data class Relation(
    val relationId: Int,
    val relationName: String
) {
    override fun toString(): String {
        return relationName
    }
}
