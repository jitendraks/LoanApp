package com.aubank.loanapp.data

data class ApprovalRequest(
    var newLitigationId: Int,
    var workableNonWorkable: Int,
    var reasonforWorkable: String,
    var newLitigationReuired: String,
    var anySettlementProposal: String,
    var approvedStatus: Int,
    var approverId: Int,
    var approverRemark: String,
    var feedbackId: Int,
    var followupDetailId: Int) {
    constructor() : this(0, 0, "", "", "", 0, 0, "", 0, 0)
}