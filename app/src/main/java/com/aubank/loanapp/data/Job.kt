package com.aubank.loanapp.data

data class Job(
    val jobId: Int,
    val name: String
) {
    override fun toString(): String {
        return name
    }

    companion object {
        fun getJobById(jobs: List<Job>, jobId: Int): Job? {
            return jobs.find { it.jobId == jobId }
        }
    }
}
