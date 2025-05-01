package com.aubank.loanapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.aubank.loanapp.api.UserRepository
import com.aubank.loanapp.components.LabelLineValue
import com.aubank.loanapp.data.Constants
import com.aubank.loanapp.data.PendingApp
import com.aubank.loanapp.ui.theme.MyApplicationTheme
import com.aubank.loanapp.viewmodel.LoanDetailsViewModel
import com.aubank.loanapp.viewmodel.NavigationEvent


class LoanDetailsActivity : ComponentActivity() {
    private val loanDetailsViewModel: LoanDetailsViewModel = LoanDetailsViewModel(UserRepository())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val loanAppDetails: PendingApp = intent.getParcelableExtra(Constants.LOAN_APP)!!
        loanDetailsViewModel.userData = intent.getParcelableExtra(Constants.USER_DATA)!!
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                LoanDetailsScreenActivity(
                    loanDetailsViewModel,
                    loanAppDetails
                )
            }
        }

        loanDetailsViewModel.navigationEvent.observe(this)
        { event ->
            when (event) {
                NavigationEvent.NavigateBack -> {
                    finish()
                }
                else -> {}
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoanDetailsScreenActivity(
    loanDetailsViewModel: LoanDetailsViewModel,
    loanAppDetails: PendingApp
) {
    val context = LocalContext.current
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(loanAppDetails.caseNo) },
            colors = topAppBarColors(
                containerColor = Color.Blue, // Set your desired background color here
                titleContentColor = Color.White,
            ),
            navigationIcon = {
                IconButton(onClick = { loanDetailsViewModel.navigateBack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                TextButton(
                    onClick = {
                        val intent = Intent(context, FeedbackActivity::class.java)
                        intent.putExtra(Constants.LOAN_APP, loanAppDetails)
                        intent.putExtra(Constants.USER_DATA, loanDetailsViewModel.userData)
                        context.startActivity(intent)
                        loanDetailsViewModel.navigateBack()
                    }
                ) {
                    Text(text = "Feedback")
                }
            }
        )
    }, content = { innerPadding ->
        LoanDetailsScreen(
            modifier = Modifier.padding(innerPadding),
            loanAppDetails
        )
    })
}

@Composable
private fun LoanDetailsScreen(modifier: Modifier, pendingApp: PendingApp) {
    val scrollState = rememberScrollState()
    Column(modifier = modifier
        .fillMaxSize()
        .verticalScroll(scrollState)) {
        pendingApp.caseType?.let { LabelLineValue(label = "Case Type", value = it) }
        pendingApp.loanAmount?.let { LabelLineValue(label = "Loan Amount", value = it) }
        pendingApp.borrowerName?.let { LabelLineValue(label = "Borrower Name", value = it) }
        pendingApp.borrowerAddress?.let { LabelLineValue(label = "Borrower Address", value = it) }
        pendingApp.borrowerContactNo?.let {
            LabelLineValue(
                label = "Borrower ContactNo",
                value = it
            )
        }
        pendingApp.coBorrowerName?.let { LabelLineValue(label = "Co-borrower Name", value = it) }
        pendingApp.coBorrowerAddress?.let {
            LabelLineValue(
                label = "Co-borrower Address",
                value = it
            )
        }
        pendingApp.coBorrowerContactNo?.let {
            LabelLineValue(
                label = "Co-borrower ContactNo",
                value = it
            )
        }
        pendingApp.guarantorName?.let { LabelLineValue(label = "Guarantor Name", value = it) }
        pendingApp.guarantorBorrowerAddress?.let {
            LabelLineValue(
                label = "Guarantor Address",
                value = it
            )
        }
        pendingApp.guarantorBorrowerContact?.let {
            LabelLineValue(
                label = "Guarantor Contact",
                value = it
            )
        }
        pendingApp.bookLossPOS?.let { LabelLineValue(label = "Book LossPOS", value = it) }
        pendingApp.daysPassedaftersaleDPD?.let {
            LabelLineValue(
                label = "Days Passed After Sale DPD",
                value = it
            )
        }
        pendingApp.vehicleNo?.let { LabelLineValue(label = "Vehicle No", value = it) }
        pendingApp.engineno?.let { LabelLineValue(label = "Engine no", value = it) }
        pendingApp.chassisno?.let { LabelLineValue(label = "Chassis no", value = it) }
        pendingApp.product?.let { LabelLineValue(label = "Product", value = it) }
        pendingApp.productCode?.let { LabelLineValue(label = "Product Code", value = it) }
        pendingApp.productName?.let { LabelLineValue(label = "Product Name", value = it) }
        pendingApp.classVehicleType?.let { LabelLineValue(label = "Class VehicleType", value = it) }
        pendingApp.loanDate?.let { LabelLineValue(label = "Loan Date", value = it) }
        pendingApp.costafterRepo?.let { LabelLineValue(label = "Cost after Repo", value = it) }
        pendingApp.sale?.let { LabelLineValue(label = "Sale", value = it) }
        pendingApp.saleDate?.let { LabelLineValue(label = "Sale Date", value = it) }
        pendingApp.installmentsNo?.let { LabelLineValue(label = "Installments No", value = it) }
        pendingApp.installmentsAdv?.let { LabelLineValue(label = "Installments Adv", value = it) }
        pendingApp.installmentsMonths?.let {
            LabelLineValue(
                label = "Installments Months",
                value = it
            )
        }
        pendingApp.scheme?.let { LabelLineValue(label = "Scheme", value = it) }
        pendingApp.dateOfFirstInstallment?.let {
            LabelLineValue(
                label = "Date Of First Installment",
                value = it
            )
        }
        pendingApp.dateOfLastInstallment?.let {
            LabelLineValue(
                label = "Date Of Last Installment",
                value = it
            )
        }
        pendingApp.amtRecthroughEMI?.let {
            LabelLineValue(
                label = "Amt Recthrough EMI",
                value = it
            )
        }
        pendingApp.posAfterSale?.let { LabelLineValue(label = "Pos After Sale", value = it) }
        pendingApp.posBeforeSale?.let { LabelLineValue(label = "Pos Before Sale", value = it) }
        pendingApp.acCloseDate?.let { LabelLineValue(label = "Ac Close Date", value = it) }
        pendingApp.loanRate?.let { LabelLineValue(label = "Loan Rate", value = it) }
        pendingApp.repaymentMode?.let { LabelLineValue(label = "Repayment Mode", value = it) }
        pendingApp.referenceName?.let { LabelLineValue(label = "Reference Name", value = it) }
        pendingApp.contactNo?.let { LabelLineValue(label = "Contact No", value = it) }
        pendingApp.lanNo?.let { LabelLineValue(label = "LanNo", value = it) }
        pendingApp.cbsLoanNo?.let { LabelLineValue(label = "Cbs LoanNo", value = it) }
        pendingApp.customerId?.let { LabelLineValue(label = "CustomerId", value = it) }
        pendingApp.stateName?.let { LabelLineValue(label = "StateName", value = it) }
        pendingApp.branch?.let { LabelLineValue(label = "Branch", value = it) }
        pendingApp.hubName?.let { LabelLineValue(label = "HubName", value = it) }
        pendingApp.fatherName?.let { LabelLineValue(label = "Father Name", value = it) }
        LabelLineValue(label = "caseNo", value = pendingApp.caseNo)
        pendingApp.loanDate?.let { LabelLineValue(label = "loanDate", value = it) }
        LabelLineValue(label = "loanAmount", value = pendingApp.loanAmount)

    }
}

@Preview(showBackground = true)
@Composable
private fun GreetingPreview() {
    MyApplicationTheme {
        LoanDetailsScreen(
            Modifier.fillMaxSize(), PendingApp(
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
                hubName = null,
                fatherName = null,
                borrowerAddress = "Data Infosys Ltd, Nr. FlyovervDurgapura, Jaipur",
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
                currentLitigation = "Current Litigation"
            )
        )
    }
}