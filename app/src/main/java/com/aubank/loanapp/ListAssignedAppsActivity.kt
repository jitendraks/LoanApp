package com.aubank.loanapp

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aubank.loanapp.api.UserRepository
import com.aubank.loanapp.components.ApiProgressBar
import com.aubank.loanapp.components.RadioButtonDialog
import com.aubank.loanapp.data.Constants
import com.aubank.loanapp.data.LoginResponse
import com.aubank.loanapp.data.MasterData
import com.aubank.loanapp.data.PendingApp
import com.aubank.loanapp.data.VisitDone
import com.aubank.loanapp.ui.theme.MyApplicationTheme
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
            MyApplicationTheme {
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

    Scaffold(topBar = {
        TopAppBar(
            title = {
                BasicTextField(
                    value = assignedAppsViewModel.searchQuery,
                    onValueChange = { newQuery ->
                        assignedAppsViewModel.searchQuery = newQuery
                        assignedAppsViewModel.filterData()
                    },
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Row {
                            if (assignedAppsViewModel.searchQuery.isEmpty()) {
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
                IconButton(onClick = { assignedAppsViewModel.navigateBack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    assignedAppsViewModel.isLoading = true
                    assignedAppsViewModel.fetchAssignedApps(userData.employeeId.toInt())
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
        PendingAppsList(
            modifier = Modifier.padding(innerPadding),
            assignedAppsViewModel,
            userData = userData,
            masterData = masterData,
            assignedAppsViewModel.isLoading,
        )
    })
}

@Composable
fun PendingAppsList(
    modifier: Modifier,
    assignedAppsViewModel: AssignedAppsViewModel,
    userData: LoginResponse,
    masterData: MasterData,
    loading: Boolean
) {
    val itemList by assignedAppsViewModel.filteredResults.observeAsState(initial = emptyList())
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn {
            items(itemList) { item ->
                ItemRow(item, userData, masterData, assignedAppsViewModel)
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
private fun ItemRow(item: PendingApp,
                    userData: LoginResponse,
                    masterData: MasterData,
                    viewModel: AssignedAppsViewModel) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf(masterData.visitDones[0]) }

    if (showDialog) {
        RadioButtonDialog(
            title = "Select an Option",
            options = masterData.visitDones,
            selectedOption = selectedOption,
            onOptionSelected = {
                selectedOption = it as VisitDone
                viewModel.fetchLastFeedbackData(item, selectedOption.visitDoneId)
                               },
            onDismiss = { showDialog = false }
        )
    }

    Column(modifier = Modifier.padding(16.dp).clickable {
        val intent = Intent(context, LoanDetailsActivity::class.java)
        intent.putExtra(Constants.LOAN_APP, item)
        intent.putExtra(Constants.USER_DATA, userData)
        context.startActivity(intent)
    }) {

        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Text(text = "Case No:", modifier = Modifier.align(Alignment.CenterVertically))
            Spacer(modifier = Modifier.weight(1f))
            Text(text = item.caseNo, modifier = Modifier.align(Alignment.CenterVertically))
        }

        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Text(text = "Borrower Name", modifier = Modifier.align(Alignment.CenterVertically))
            Spacer(modifier = Modifier.weight(1f))
            Text(text = item.borrowerName, modifier = Modifier.align(Alignment.CenterVertically))
        }

        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Text(text = "Loan Amount", modifier = Modifier.align(Alignment.CenterVertically))
            Spacer(modifier = Modifier.weight(1f))
            Text(text = item.loanAmount, modifier = Modifier.align(Alignment.CenterVertically))
        }

        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Text(text = "Pos After Sale", modifier = Modifier.align(Alignment.CenterVertically))
            Spacer(modifier = Modifier.weight(1f))
            item.posAfterSale?.let { Text(text = it, modifier = Modifier.align(Alignment.CenterVertically)) }
        }
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            TextButton(onClick = {
                showDialog = true


            }) {
                Text("Submit Feedback")
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GreetingPreview() =
    ItemRow(item = PendingApp(
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