package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.api.UserRepository
import com.example.myapplication.data.LoginResponse
import com.example.myapplication.data.PendingApp
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.viewmodel.AssignedAppsViewModel
import com.example.myapplication.viewmodel.NavigationEvent

private lateinit var userData: LoginResponse

class ListAssignedAppsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val assignedAppsViewModel = AssignedAppsViewModel(UserRepository())
        userData = intent.getParcelableExtra("USER_DATA")!!

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ListAssignedAppsScreen(assignedAppsViewModel, Modifier.fillMaxSize(), assignedAppsViewModel.isLoading)
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
                    assignedAppsViewModel.isLoading = false
                }

                is AssignedAppsViewModel.FetchAssignedAppsApiState.Error -> {
                    assignedAppsViewModel.isLoading = false
                }

                is AssignedAppsViewModel.FetchAssignedAppsApiState.Loading -> {

                }
            }
        }

        assignedAppsViewModel.isLoading = true
        assignedAppsViewModel.fetchAssignedApps(userData.employeeId.toInt())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListAssignedAppsScreen(assignedAppsViewModel: AssignedAppsViewModel, modifier: Modifier, isLoading: Boolean) {
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(topBar = {
        TopAppBar(
            title = { },
            colors = topAppBarColors(
                containerColor = Color.Blue, // Set your desired background color here
                titleContentColor = Color.White,
            ),
            navigationIcon = {
                IconButton(onClick = { assignedAppsViewModel.navigateBack() }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                SearchView()
            }
        )
    }, content = {
            innerPadding ->
        AssignedAppsList(
            modifier = Modifier.padding(innerPadding),
            assignedAppsViewModel,
            assignedAppsViewModel.isLoading
        )
    })
}

@Composable
fun SearchView() {
    var isExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    if (isExpanded) {
        TextField(
            value = searchQuery,
            onValueChange = { newQuery -> searchQuery = newQuery },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        IconButton(onClick = { isExpanded = true }) {
            Icon(Icons.Filled.Search, contentDescription = "Search")
        }
    }
}

@Composable
fun AssignedAppsList(
    modifier: Modifier,
    assignedAppsViewModel: AssignedAppsViewModel,
    loading: Boolean
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn {
            assignedAppsViewModel.pendingApps.value?.let {
                items(it) { item ->
                    ItemRow(item)
                    HorizontalDivider(
                        color = Color.Gray,
                        thickness = 1.dp
                    )
                }
            }
        }
        if (loading) {
            ApiProgressBar(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun ItemRow(item: PendingApp) {
    Column(modifier = Modifier.padding(16.dp)) {
        item.lanNo?.let { Text(text = it, modifier = Modifier.padding(start = 16.dp)) }
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            item.borrowerName?.let { Text(text = it, modifier = Modifier.align(Alignment.CenterVertically)) }
            Spacer(modifier = Modifier.weight(1f))
            item.loanAmount?.let { Text(text = it, modifier = Modifier.align(Alignment.CenterVertically)) }
        }
    }
}

@Composable
fun Greeting2(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview3() =
    ItemRow(item = PendingApp(
        lanNo = "5454545454",
        loanAmount = "500000",
        borrowerName = "djfdifjdf nfdijfd",
        loanDetailId = 0,
        caseType = null,
        caseNo = null,
        cbsLoanNo = null,
        customerId = null,
        stateName = null,
        branch = null,
        hubName= null,
        fatherName = null,
        borrowerAddress = null,
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
        contactNo = null))