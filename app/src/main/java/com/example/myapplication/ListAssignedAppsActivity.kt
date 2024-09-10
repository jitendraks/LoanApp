package com.example.myapplication

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
import androidx.compose.material3.TextButton
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
import com.example.myapplication.api.UserRepository
import com.example.myapplication.components.ApiProgressBar
import com.example.myapplication.data.Constants
import com.example.myapplication.data.LoginResponse
import com.example.myapplication.data.PendingApp
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.viewmodel.AssignedAppsViewModel
import com.example.myapplication.viewmodel.NavigationEvent

class ListAssignedAppsActivity : ComponentActivity() {
    private lateinit var assignedAppsViewModel: AssignedAppsViewModel
    private lateinit var  userData: LoginResponse
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        assignedAppsViewModel = AssignedAppsViewModel(UserRepository())
        userData = intent.getParcelableExtra("USER_DATA")!!

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ListPendingApprovalsScreen(
                    assignedAppsViewModel,
                    userData = userData
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
    userData: LoginResponse
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
            assignedAppsViewModel.isLoading,
        )
    })
}

@Composable
fun PendingAppsList(
    modifier: Modifier,
    assignedAppsViewModel: AssignedAppsViewModel,
    userData: LoginResponse,
    loading: Boolean
) {
    val itemList by assignedAppsViewModel.filteredResults.observeAsState(initial = emptyList())
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn {
            items(itemList) { item ->
                ItemRow(item, userData)
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
private fun ItemRow(item: PendingApp, userData: LoginResponse) {
    val context = LocalContext.current
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
                val intent = Intent(context, FeedbackActivity::class.java)
                intent.putExtra(Constants.LOAN_APP, item)
                intent.putExtra(Constants.USER_DATA, userData)
                context.startActivity(intent)
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
        contactNo = null),
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
            trackingStatus = true
        ))