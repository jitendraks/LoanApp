package com.aubank.loanapp.data

data class Relation(
    val relationId: Int,
    val relationName: String
) {
    override fun toString(): String {
        return relationName
    }

    companion object {
        fun getRelationById(relations: List<Relation>, relationId: Int): Relation? {
            return relations.find { it.relationId == relationId } }
        }
    }
