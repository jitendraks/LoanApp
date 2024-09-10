package com.example.myapplication.data

data class FeedbackDataWithIdValue(
    var pendingApprovalFeedbackData: PendingApprovalFeedbackData?,
    var visitDoneValue: String?,
    var vehicleStatusValue: String?,
    var loanTypeValue: String?,
    var relationValue: String?,
    var financialConditionValue: String?,
    var jobValue: String?,
    var litigationValue: String?,
    var newLitigationValue: String?,
    var incomeValue: String?) {
    constructor() : this(null, null, null, null, null, null, null, null, null, null)
}
