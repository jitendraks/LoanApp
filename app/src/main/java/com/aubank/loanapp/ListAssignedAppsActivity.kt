package com.aubank.loanapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.aubank.loanapp.api.UserRepository
import com.aubank.loanapp.components.EnhancedTopAppBar
import com.aubank.loanapp.components.LoanItemRow
import com.aubank.loanapp.components.PendingAppsList
import com.aubank.loanapp.data.Constants
import com.aubank.loanapp.data.LoginResponse
import com.aubank.loanapp.data.MasterData
import com.aubank.loanapp.data.PendingApp
import com.aubank.loanapp.ui.theme.LoanAppTheme
import com.aubank.loanapp.viewmodel.AssignedAppsViewModel
import com.aubank.loanapp.viewmodel.NavigationEvent

class ListAssignedAppsActivity : ComponentActivity() {
    private lateinit var assignedAppsViewModel: AssignedAppsViewModel
    private lateinit var  userData: LoginResponse
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        assignedAppsViewModel = AssignedAppsViewModel(UserRepository())
        userData = intent.getParcelableExtra("USER_DATA")!!
        val masterData = (application as LoanApplication).getMasterData()!!
        enableEdgeToEdge()
        setContent {
            LoanAppTheme {
                ListPendingApprovalsScreen(
                    assignedAppsViewModel,
                    userData = userData,
                    masterData
                )
            }
        }

        assignedAppsViewModel.navigationEvent.observe(this)
        { event ->
            when (event) {
                NavigationEvent.NavigateBack -> {
                    finish()
                }

                else -> {}
            }
        }

        assignedAppsViewModel.fetchAssignedAppsApiState.observe(this)
        { event ->
            when (event) {
                is AssignedAppsViewModel.FetchAssignedAppsApiState.Success -> {
                    assignedAppsViewModel.pendingApps.value = event.pendingApp
                    assignedAppsViewModel.filterData()
                    assignedAppsViewModel.isLoading = false
                }

                is AssignedAppsViewModel.FetchAssignedAppsApiState.Error -> {
                    assignedAppsViewModel.isLoading = false
                }

                is AssignedAppsViewModel.FetchAssignedAppsApiState.Loading -> {

                }
            }
        }

        assignedAppsViewModel.fetchLastFeedbackDataApiState.observe(this)
        { event ->
            when (event) {
                is AssignedAppsViewModel.FetchLastFeedbackDataApiState.Success -> {

                    val intent = Intent(this, FeedbackFollowupActivity::class.java)
                    intent.putExtra(Constants.LOAN_APP, event.pendingApp)
                    intent.putExtra(Constants.USER_DATA, userData)
                    intent.putExtra(Constants.VISIT_DONE_ID, event.visitDoneId)
                    intent.putExtra(Constants.PENDING_APPROVAL_FEEDBACK_DATA, event.feedbackData)
                    startActivity(intent)
                    assignedAppsViewModel.isLoading = false
                }

                is AssignedAppsViewModel.FetchLastFeedbackDataApiState.Error -> {
                    val intent = Intent(this, FeedbackActivity::class.java)
                    intent.putExtra(Constants.LOAN_APP, event.pendingApp)
                    intent.putExtra(Constants.USER_DATA, userData)
                    intent.putExtra(Constants.VISIT_DONE_ID, event.visitDoneId)
                    startActivity(intent)
                    assignedAppsViewModel.isLoading = false
                }

                is AssignedAppsViewModel.FetchLastFeedbackDataApiState.Loading -> {

                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        assignedAppsViewModel.isLoading = true
        assignedAppsViewModel.fetchAssignedApps(userData.employeeId.toInt())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListPendingApprovalsScreen(
    assignedAppsViewModel: AssignedAppsViewModel,
    userData: LoginResponse,
    masterData: MasterData
) {

    val searchQuery = assignedAppsViewModel.searchQuery

    Scaffold(topBar = {
        EnhancedTopAppBar(
            searchQuery = searchQuery,
            onSearchQueryChange = {
                assignedAppsViewModel.searchQuery = it
                assignedAppsViewModel.filterData()
            },
            onNavigateBack = { assignedAppsViewModel.navigateBack() },
            onRefresh = {
                assignedAppsViewModel.isLoading = true
                assignedAppsViewModel.fetchAssignedApps(userData.employeeId.toInt())
            }
        )
    }, content = {
            innerPadding ->
        PendingAppsList(
            modifier = Modifier.padding(innerPadding),
            assignedAppsViewModel,
            userData = userData,
            masterData = masterData,
            assignedAppsViewModel.isLoading,
        )
    })
}

@Preview(showBackground = true)
@Composable
private fun GreetingPreview() =
    LoanItemRow(item = PendingApp(
        lanNo = "5454545454",
        loanAmount = "500000",
        borrowerName = "djfdifjdf nfdijfd",
        loanDetailId = 0,
        caseType = null,
        caseNo = "775656565",
        cbsLoanNo = null,
        customerId = null,
        stateName = null,
        branch = null,
        hubName= null,
        fatherName = null,
        borrowerAddress = "Durgapura, Jaipur",
        borrowerContactNo = null,
        coBorrowerName = null,
        coBorrowerAddress = null,
        coBorrowerContactNo = null,
        guarantorName = null,
        guarantorBorrowerAddress = null,
        guarantorBorrowerContact = null,
        bookLossPOS = null,
        daysPassedaftersaleDPD = null,
        vehicleNo = null,
        engineno = null,
        chassisno = null,
        product = null,
        productCode = null,
        productName = null,
        classVehicleType = null,
        loanDate = null,
        costafterRepo = null,
        sale = null,
        saleDate = null,
        installmentsNo = null,
        installmentsAdv = null,
        installmentsMonths = null,
        scheme = null,
        dateOfFirstInstallment = null,
        dateOfLastInstallment = null,
        amtRecthroughEMI = null,
        posAfterSale = null,
        posBeforeSale = null,
        acCloseDate = null,
        loanRate = null,
        repaymentMode = null,
        referenceName = null,
        contactNo = null,
        currentLitigation = "Current Litigation"),
        userData = LoginResponse(userId = "1",
            employeeId = "1",
            name = "Employee Name",
            emailAddress = "employee@emailadderess.com",
            roleName = "Role Name",
            monthlyTarget = 10000,
            yearlyTarget = 50000,
            monthlyAchievedTarget = 2000,
            hodStatus = true,
            trackingTime = "15",
            achievedTarget = 34343,
            trackingStatus = true,
            applicationAlloted = 0
        ),
        MasterData(jobs = emptyList(),
            loanTypes = emptyList(),
            relations = emptyList(),
            finConditions = emptyList(),
            visitDones = emptyList(),
            incomeSlabs = emptyList(),
            litigations = emptyList(),
            vehicleStatuses = emptyList()
        ), AssignedAppsViewModel(UserRepository()))