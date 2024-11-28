package com.aubank.loanapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aubank.loanapp.api.UserRepository
import com.aubank.loanapp.components.ApiProgressBar
import com.aubank.loanapp.data.Constants
import com.aubank.loanapp.data.LoginResponse
import com.aubank.loanapp.data.PendingApprovalFeedbackData
import com.aubank.loanapp.ui.theme.MyApplicationTheme
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
            MyApplicationTheme {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListPendingApprovalAppsScreen(
    pendingApprovalAppsViewModel: PendingApprovalAppsViewModel,
    userData: LoginResponse) {

    Scaffold(topBar = {
        TopAppBar(
            title = {
                BasicTextField(
                    value = pendingApprovalAppsViewModel.searchQuery,
                    onValueChange = { newQuery ->
                        pendingApprovalAppsViewModel.searchQuery = newQuery
                        pendingApprovalAppsViewModel.filterData()
                    },
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Row {
                            if (pendingApprovalAppsViewModel.searchQuery.isEmpty()) {
                                Text("Search...", color = Color.Gray)
                            }
                            innerTextField()
                        }
                    }
                )
            },
            colors = topAppBarColors(
                containerColor = Color.Blue, // Set your desired background color here
                titleContentColor = Color.White,
            ),
            navigationIcon = {
                IconButton(onClick = { pendingApprovalAppsViewModel.navigateBack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    pendingApprovalAppsViewModel.isLoading = true
                    pendingApprovalAppsViewModel.fetchPendingApprovalApps(userData.employeeId.toInt())
                }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh"
                    )
                }
            }
        )
    }, content = {
            innerPadding ->
        PendingApprovalAppsList(
            modifier = Modifier.padding(innerPadding),
            pendingApprovalAppsViewModel,
            userData = userData,
            pendingApprovalAppsViewModel.isLoading,
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
    val itemList by pendingApprovalAppsViewModel.filteredResults.observeAsState(initial = emptyList())
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn {
            items(itemList) { item ->
                ItemRow(item, userData = userData)
                HorizontalDivider(
                    color = Color.Gray,
                    thickness = 1.dp
                )
            }
        }
        if (loading) {
            ApiProgressBar(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun ItemRow(item: PendingApprovalFeedbackData, userData: LoginResponse) {
    val context = LocalContext.current

    Column(modifier = Modifier.padding(16.dp)
        .clickable {
            val intent = Intent(context, FeedbackApprovalFormActivity::class.java)
            intent.putExtra(Constants.USER_DATA, userData)
            intent.putExtra(Constants.PENDING_APPROVAL_FEEDBACK_DATA, item)
            context.startActivity(intent)
        }) {

        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Text(text = "Loan No:", modifier = Modifier.align(Alignment.CenterVertically))
            Spacer(modifier = Modifier.weight(1f))
            Text(text = item.loanNo ?: " ", modifier = Modifier.align(Alignment.CenterVertically))
        }

        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Text(text = "Employee Name", modifier = Modifier.align(Alignment.CenterVertically))
            Spacer(modifier = Modifier.weight(1f))
            item.employeeName?.let { Text(text = it, modifier = Modifier.align(Alignment.CenterVertically)) }
        }

        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Text(text = "Visit Date", modifier = Modifier.align(Alignment.CenterVertically))
            Spacer(modifier = Modifier.weight(1f))
            Text(text = item.visitDate ?: "", modifier = Modifier.align(Alignment.CenterVertically))
        }

        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Text(text = "Borrower Name", modifier = Modifier.align(Alignment.CenterVertically))
            Spacer(modifier = Modifier.weight(1f))
            item.borrowerName?.let { Text(text = it, modifier = Modifier.align(Alignment.CenterVertically)) }
        }

        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Text(text = "Loan Amount", modifier = Modifier.align(Alignment.CenterVertically))
            Spacer(modifier = Modifier.weight(1f))
            Text(text = item.loanAmount.toString(), modifier = Modifier.align(Alignment.CenterVertically))
        }

    }
}

@Preview(showBackground = true)
@Composable
private fun GreetingPreview5() =
    ItemRow(item = PendingApprovalFeedbackData(
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
        loanAmount = 225000,
        loanDate = "2023-04-28T00:00:00",
        bookLossPOS = 139005,
        costafterRepo = 199005,
        posBeforeSale = 0,
        posAfterSale = 122360,
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
                trackingStatus = true
            )
    )
