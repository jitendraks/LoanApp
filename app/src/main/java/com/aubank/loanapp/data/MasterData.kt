package com.aubank.loanapp.data

data class MasterData(
    val visitDones: List<VisitDone>,
    val finConditions: List<FinancialCondition>,
    val incomeSlabs: List<IncomeSlab>,
    val jobs: List<Job>,
    val litigations: List<Litigation>,
    val relations: List<Relation>,
    val vehicleStatuses: List<VehicleStatus>,
    val loanTypes: List<LoanType>
)
