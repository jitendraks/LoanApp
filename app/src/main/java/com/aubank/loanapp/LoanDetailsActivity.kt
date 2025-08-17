package com.aubank.loanapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.aubank.loanapp.api.UserRepository
import com.aubank.loanapp.components.LabelLineValue
import com.aubank.loanapp.components.RadioButtonDialog
import com.aubank.loanapp.data.Constants
import com.aubank.loanapp.data.MasterData
import com.aubank.loanapp.data.PendingApp
import com.aubank.loanapp.data.VisitDone
import com.aubank.loanapp.ui.theme.LoanAppTheme
import com.aubank.loanapp.viewmodel.AssignedAppsViewModel
import com.aubank.loanapp.viewmodel.LoanDetailsViewModel
import com.aubank.loanapp.viewmodel.NavigationEvent

class LoanDetailsActivity : ComponentActivity() {
    private val viewModel = LoanDetailsViewModel(
        userRepository = UserRepository()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val masterData = (application as LoanApplication).getMasterData()!!
        val app: PendingApp = intent.getParcelableExtra(Constants.LOAN_APP)!!
        viewModel.userData = intent.getParcelableExtra(Constants.USER_DATA)!!

        setContent {
            LoanAppTheme {
                LoanDetailsScreenActivity(masterData, viewModel, app)
            }
        }

        viewModel.navigationEvent.observe(this) { event ->
            if (event == NavigationEvent.NavigateBack) finish()
        }

        viewModel.fetchLastFeedbackDataApiState.observe(this)
        { event ->
            when (event) {
                is AssignedAppsViewModel.FetchLastFeedbackDataApiState.Success -> {

                    val intent = Intent(this, FeedbackFollowupActivity::class.java)
                    intent.putExtra(Constants.LOAN_APP, event.pendingApp)
                    intent.putExtra(Constants.USER_DATA, viewModel.userData)
                    intent.putExtra(Constants.VISIT_DONE_ID, event.visitDoneId)
                    intent.putExtra(Constants.PENDING_APPROVAL_FEEDBACK_DATA, event.feedbackData)
                    startActivity(intent)
                    viewModel.isLoading = false
                }

                is AssignedAppsViewModel.FetchLastFeedbackDataApiState.Error -> {
                    val intent = Intent(this, FeedbackActivity::class.java)
                    intent.putExtra(Constants.LOAN_APP, event.pendingApp)
                    intent.putExtra(Constants.USER_DATA, viewModel.userData)
                    intent.putExtra(Constants.VISIT_DONE_ID, event.visitDoneId)
                    startActivity(intent)
                    viewModel.isLoading = false
                }

                is AssignedAppsViewModel.FetchLastFeedbackDataApiState.Loading -> {

                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoanDetailsScreenActivity(
    masterData: MasterData,
    viewModel: LoanDetailsViewModel,
    loanApp: PendingApp
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf(masterData.visitDones.first()) }

    if (showDialog) {
        RadioButtonDialog(
            title = "Select Feedback Option",
            options = masterData.visitDones,
            selectedOption = selectedOption,
            onOptionSelected = {
                selectedOption = it as VisitDone
                viewModel.fetchLastFeedbackData(loanApp, it.visitDoneId)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(loanApp.caseNo) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    TextButton(onClick = {
                        showDialog = true
                    }) {
                        Text("Feedback")
                    }
                }
            )
        }
    ) { padding ->
        LoanDetailsScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            loanApp = loanApp
        )
    }
}

@Composable
private fun LoanDetailsScreen(
    modifier: Modifier,
    loanApp: PendingApp
) {
    // Prepare all key/value pairs
    val generalDetails = listOf(
        "Case Type" to loanApp.caseType,
        "Loan Amount" to loanApp.loanAmount,
        "Borrower Name" to loanApp.borrowerName,
        "Borrower Address" to loanApp.borrowerAddress,
        "Borrower Contact" to loanApp.borrowerContactNo,
        "Co-borrower Name" to loanApp.coBorrowerName,
        "Co-borrower Address" to loanApp.coBorrowerAddress,
        "Co-borrower Contact" to loanApp.coBorrowerContactNo,
        "Guarantor Name" to loanApp.guarantorName,
        "Guarantor Address" to loanApp.guarantorBorrowerAddress,
        "Guarantor Contact" to loanApp.guarantorBorrowerContact
    ).filter { it.second != null }

    // Prepare all key/value pairs
    val loanDetails = listOf(
        "Bank Loss POS" to loanApp.bookLossPOS,
        "Days passed after sale DPD" to loanApp.daysPassedaftersaleDPD,
        "Loan Date" to loanApp.loanDate,
        "Cost after Repo" to loanApp.costafterRepo,
        "Sale" to loanApp.sale,
        "Sale Date" to loanApp.saleDate,
        "Installments No" to loanApp.installmentsNo,
        "Installments Adv" to loanApp.installmentsAdv,
        "Installments Months" to loanApp.installmentsMonths,
        "Scheme" to loanApp.scheme,
        "Date of first installment" to loanApp.dateOfFirstInstallment,
        "Date of last installment" to loanApp.dateOfLastInstallment,
        "Amt Recthrough EMI" to loanApp.amtRecthroughEMI,
        "POS After Sale" to loanApp.posAfterSale,
        "POS Before Sale" to loanApp.posBeforeSale,
        "Ac Close Date" to loanApp.acCloseDate,
        "Loan Rate" to loanApp.loanRate,
        "Repayment Mode" to loanApp.repaymentMode,
        "Reference Name" to loanApp.referenceName,
        "Contact No" to loanApp.contactNo,
        "LAN No" to loanApp.lanNo,
        "CBS Loan No" to loanApp.cbsLoanNo,
        "Customer Id" to loanApp.customerId,
        "Branch" to loanApp.branch,
        "Father Name" to loanApp.fatherName,
    ).filter { it.second != null }

    val vehicleDetails = listOf(
        "Vehicle No" to loanApp.vehicleNo,
        "Engine No" to loanApp.engineno,
        "Chassis No" to loanApp.chassisno,
        "Product" to loanApp.product,
        "Product Code" to loanApp.productCode,
        "Product Name" to loanApp.productName
    ).filter { it.second != null }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (generalDetails.isNotEmpty()) {
            item {
                SectionCard("General Details") {
                    generalDetails.forEach { (label, value) ->
                        LabelLineValue(label = label, value = value!!)
                    }
                }
            }
        }

        if (loanDetails.isNotEmpty()) {
            item {
                SectionCard("Loan Details") {
                    loanDetails.forEach { (label, value) ->
                        LabelLineValue(label = label, value = value!!)
                    }
                }
            }
        }

        if (vehicleDetails.isNotEmpty()) {
            item {
                SectionCard("Vehicle Details") {
                    vehicleDetails.forEach { (label, value) ->
                        LabelLineValue(label = label, value = value!!)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewLoanDetails() {
    LoanAppTheme {
        LoanDetailsScreen(
            modifier = Modifier.fillMaxSize(),
            loanApp = PendingApp(
                lanNo = "12345",
                loanAmount = "500000",
                borrowerName = "John Doe",
                loanDetailId = 1,
                caseType = "WHEELS-LOSS",
                caseNo = "LVWAR01314-150291591",
                borrowerAddress = "Sample Address",
                borrowerContactNo = "9999999999",
                coBorrowerName = "Jane Doe",
                coBorrowerAddress = "Sample Co-Address",
                coBorrowerContactNo = "8888888888",
                guarantorName = "Guarantor",
                guarantorBorrowerAddress = "Sample G Addr",
                guarantorBorrowerContact = "7777777777",
                bookLossPOS = null,
                daysPassedaftersaleDPD = null,
                vehicleNo = "MH32Q4738",
                engineno = "GHE1K64440",
                chassisno = "MA1ZN2GHKE1K79927",
                product = "MAHINDRA - BOLERO PICK UP",
                productCode = "MBPU123",
                productName = "Bolero",
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
                currentLitigation = null,
                cbsLoanNo = TODO(),
                customerId = TODO(),
                stateName = TODO(),
                branch = TODO(),
                hubName = TODO(),
                fatherName = TODO()
            )
        )
    }
}
