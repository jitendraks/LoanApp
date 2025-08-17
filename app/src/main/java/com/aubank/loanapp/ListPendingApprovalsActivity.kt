package com.aubank.loanapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aubank.loanapp.api.UserRepository
import com.aubank.loanapp.components.ApiProgressBar
import com.aubank.loanapp.components.EnhancedTopAppBar
import com.aubank.loanapp.components.PendingAppsList
import com.aubank.loanapp.data.Constants
import com.aubank.loanapp.data.LoginResponse
import com.aubank.loanapp.data.PendingApprovalFeedbackData
import com.aubank.loanapp.ui.theme.LoanAppTheme
import com.aubank.loanapp.viewmodel.NavigationEvent
import com.aubank.loanapp.viewmodel.PendingApprovalAppsViewModel

class ListPendingApprovalsActivity : ComponentActivity() {
    private lateinit var pendingApprovalAppsViewModel: PendingApprovalAppsViewModel
    private lateinit var userData: LoginResponse

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingApprovalAppsViewModel = PendingApprovalAppsViewModel(UserRepository())
        userData = intent.getParcelableExtra("USER_DATA")!!

        enableEdgeToEdge()
        setContent {
            LoanAppTheme {
                ListPendingApprovalAppsScreen(pendingApprovalAppsViewModel,
                    userData = userData)
            }
        }

        pendingApprovalAppsViewModel.navigationEvent.observe(this)
        { event ->
            when (event) {
                NavigationEvent.NavigateBack -> {
                    finish()
                }

                else -> {}
            }
        }

        pendingApprovalAppsViewModel.fetchPendingApprovalAppsApiState.observe(this)
        { event ->
            when (event) {
                is PendingApprovalAppsViewModel.FetchPendingApprovalAppsApiState.Success -> {
                    pendingApprovalAppsViewModel.pendingApprpvals.value = event.pendingApprovals
                    pendingApprovalAppsViewModel.filterData()
                    pendingApprovalAppsViewModel.isLoading = false
                }

                is PendingApprovalAppsViewModel.FetchPendingApprovalAppsApiState.Error -> {
                    pendingApprovalAppsViewModel.isLoading = false
                }

                is PendingApprovalAppsViewModel.FetchPendingApprovalAppsApiState.Loading -> {

                }
            }
        }

        pendingApprovalAppsViewModel.isLoading = true
        pendingApprovalAppsViewModel.fetchPendingApprovalApps(userData.employeeId.toInt())
    }

    override fun onResume() {
        super.onResume()
        pendingApprovalAppsViewModel.isLoading = true
        pendingApprovalAppsViewModel.fetchPendingApprovalApps(userData.employeeId.toInt())
    }

}

@Composable
fun ListPendingApprovalAppsScreen(
    pendingApprovalAppsViewModel: PendingApprovalAppsViewModel,
    userData: LoginResponse
) {
    Scaffold(
        topBar = {
            EnhancedTopAppBar(
                searchQuery = pendingApprovalAppsViewModel.searchQuery,
                onSearchQueryChange = {
                    pendingApprovalAppsViewModel.searchQuery = it
                    pendingApprovalAppsViewModel.filterData()
                                      },
                onNavigateBack = { pendingApprovalAppsViewModel.navigateBack() },
                onRefresh = {
                    pendingApprovalAppsViewModel.isLoading = true
                    pendingApprovalAppsViewModel.fetchPendingApprovalApps(userData.employeeId.toInt())
                }
            )
        }, content = {
                innerPadding ->
            PendingApprovalAppsList(
                modifier = Modifier.padding(innerPadding),
                pendingApprovalAppsViewModel,
                userData,
                pendingApprovalAppsViewModel.isLoading
            )
        })
}

@Composable
private fun PendingApprovalAppsList(
    modifier: Modifier,
    pendingApprovalAppsViewModel: PendingApprovalAppsViewModel,
    userData: LoginResponse,
    loading: Boolean
) {
    val items by pendingApprovalAppsViewModel.filteredResults.observeAsState(initial = emptyList())

    Box(modifier = modifier.fillMaxSize()) {
        when {
            loading && items.isEmpty() -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
                Text(
                    text = "Loading pending approvals...",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            !loading && items.isEmpty() -> {
                EmptyState(
                    title = "No Pending Approvals",
                    description = "All approvals processed or no loans pending."
                )
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = items,
                        key = { item ->
                            "${item.loanNo}-${item.feedBackId}-${item.employeeId}-${item.visitDate}"
                        }
                    ) { item ->
                        LoanApprovalCard(item = item, userData = userData)
                    }
                }
            }
        }
    }
}


@Composable
fun LoanApprovalCard(item: PendingApprovalFeedbackData, userData: LoginResponse) {
    val context = LocalContext.current
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 8.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(context, FeedbackApprovalFormActivity::class.java).apply {
                    putExtra(Constants.USER_DATA, userData)
                    putExtra(Constants.PENDING_APPROVAL_FEEDBACK_DATA, item)
                }
                context.startActivity(intent)
            }
            .semantics { contentDescription = "Loan Approval Card for ${item.loanNo}" }
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Loan No: ${item.loanNo ?: "N/A"}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Employee:", style = MaterialTheme.typography.labelMedium)
                Text(text = item.employeeName ?: "N/A", style = MaterialTheme.typography.bodyMedium)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Borrower:", style = MaterialTheme.typography.labelMedium)
                Text(text = item.borrowerName ?: "N/A", style = MaterialTheme.typography.bodyMedium)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Visit Date:", style = MaterialTheme.typography.labelMedium)
                Text(text = item.visitDate ?: "N/A", style = MaterialTheme.typography.bodyMedium)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Loan Amount:", style = MaterialTheme.typography.labelMedium)
                Text(text = item.loanAmount?.toString() ?: "N/A", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}


@Composable
fun EmptyState(title: String, description: String, icon: ImageVector = Icons.Default.CheckCircle) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


@Preview(showBackground = true)
@Composable
private fun GreetingPreview5() =
    LoanApprovalCard(item = PendingApprovalFeedbackData(
        feedBackId = 1,
        employeeId = 1,
        visitDate = "19/09/2024",
        loanNo = "L9001010534772398",
        visitType = "New Visit",
        loanDetailId= 1,
        visitDoneId = 1,
        relationId = 1,
        typeOfLoanId = 1,
        vehicleStatusId = 1,
        nameWithMeet = "",
        contactNoWithMeet = "",
        borrowerLivingCurrentAddress = true,
        newAddressOfBorrower = "",
        landMark = "",
        currentContactNoOfBorrower = "",
        borrowerJobAddress = "",
        jobId = 1,
        finConditionId = 1,
        incomeId = 1,
        litigationId = 1,
        newLitigationId = 1,
        workableNonWorkable = true,
        reasonforWorkable = "",
        newLitigationReuired = "",
        anySettlementProposal = "",
        approvedStatus = false,
        approverId = 1,
        approverRemark = "",
        createdDate = "",
        employeeName = "ABHISHEK RAGHUWANSHI",
        loanAmount = 225000.0,
        loanDate = "2023-04-28T00:00:00",
        bookLossPOS = 139005.0,
        costafterRepo = 199005.0,
        posBeforeSale = 0.0,
        posAfterSale = 122360.0,
        borrowerName = "RAM SINGH",
        borrowerAddress = "HOUSE NO 01 WARD NO 04 TEHSIL BHUNTAR NH 21 VILLAGE KALHELI SHARABAI PO BHUNTAR KHOKHAN, KULLU, HIMACHAL PRADESH, Pin-175125",
        borrowerContactNo = "7876639912",
        coBorrowerName = "KHUSHVOO DEVI",
        coBorrowerAddress = "HOUSE NO 1 WARD NO 4 NH 21 TEHSIL, BHUNTAR VILLAGE KALHELI SHARABAI, KHOKHAN KULLU, KULLU, HIMACHAL PRADESH, Pin-175125",
        coBorrowerContactNo = "7807933752",
        guarantorName = "RAVINDER DASS",
        guarantorBorrowerAddress = "VILLAGE HURLA, ROTE II 42 107, HURLA KULLU, KULLU, HIMACHAL PRADESH, Pin-175125",
        guarantorBorrowerContactNo = "9736893434"),
            LoginResponse(userId = "1",
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
            )
    )
