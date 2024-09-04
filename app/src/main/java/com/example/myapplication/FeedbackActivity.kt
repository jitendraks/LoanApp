package com.example.myapplication

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.FileProvider
import com.example.myapplication.api.UserRepository
import com.example.myapplication.components.CheckboxGroup
import com.example.myapplication.components.DropdownSpinner
import com.example.myapplication.data.Constants
import com.example.myapplication.data.Constants.CAPTURE_PHOTO_REQUEST_CODE
import com.example.myapplication.data.PendingApp
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.utils.DateTimeFormatter
import com.example.myapplication.viewmodel.FeedbackViewModel
import com.example.myapplication.viewmodel.NavigationEvent
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Date

class FeedbackActivity : ComponentActivity() {
    private val viewModel: FeedbackViewModel = FeedbackViewModel(UserRepository())

    private val getContentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // Handle the captured image URI

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app: PendingApp = intent.getParcelableExtra<PendingApp>(Constants.LOAN_APP)!!

        setContent {
            MyApplicationTheme {
                FeedbackScreen(modifier = Modifier.fillMaxSize(), app = app, viewModel = viewModel)
            }
        }

        viewModel.navigationEvent.observe(this)
        { event ->
            when (event) {
                NavigationEvent.NavigateBack -> {
                    finish()
                }
                NavigationEvent.NavigateToCaptureActivity -> {
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    val photoUri = FileProvider.getUriForFile(
                        this,
                        "com.example.myapplication.fileprovider",
                        createImageFile()
                    )
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    getContentLauncher.launch(intent)
                }
                else -> {}
            }
        }

    }

    @SuppressLint("SimpleDateFormat")
    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_${timestamp}_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(imageFileName, ".jpg", storageDir)

        return image
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CAPTURE_PHOTO_REQUEST_CODE && resultCode == RESULT_OK)
        {
            val imageFile = createImageFile()
            val imagePath = imageFile.absolutePath

            // Do something with the image path, e.g., display it in an Image composable
            // Image(bitmap = BitmapFactory.decodeFile(imagePath), contentDescription = "Captured Image")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(app: PendingApp, modifier: Modifier, viewModel: FeedbackViewModel) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(app.caseNo) },
            colors = topAppBarColors(
                containerColor = Color.Blue, // Set your desired background color here
                titleContentColor = Color.White,
            ),
            navigationIcon = {
                IconButton(onClick = {  }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )
    }, content = {
            innerPadding ->
        FeedbackForm(
            app,
            modifier = Modifier
                .padding(innerPadding)
                .systemBarsPadding(),
            viewModel
        )
    })
}

@Composable
fun FeedbackForm(app: PendingApp, modifier: Modifier, viewModel: FeedbackViewModel) {
    val scrollState = rememberScrollState()
    Column(modifier = modifier
        .fillMaxSize()
        .verticalScroll(scrollState))
    {
        OutlinedTextField(
            value = DateTimeFormatter.formatDate(LocalDateTime.now()),
            onValueChange = { },
            label = { Text("Visit Date") },
            singleLine = true,
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        DropdownSpinner(
            "Visit Type",
            listOf("New Visit", "Follow up")
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = app.borrowerName,
            onValueChange = { },
            label = { Text("Name of Borrower") },
            singleLine = true,
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        CheckboxGroup(
            "Visit Done",
            listOf("Borrower", "Co-Borrower", "Guarantor")
        )

        Spacer(modifier = Modifier.height(16.dp))
        DropdownSpinner(
            "Relation between Borrower and Co-Borrower",
            listOf("Blood Relation", "Relative", "Outsider")
        )

        Spacer(modifier = Modifier.height(16.dp))
        DropdownSpinner(
            "Type of Loan",
            listOf("Wheels-Loss", "Wheels-Abandon", "SBL/Home-Abandon", "Other")
        )

        Spacer(modifier = Modifier.height(16.dp))
        CheckboxGroup(
            "Vehicle Status",
            listOf("Repo Sold",
                "Repo Stock",
                "Third Party Sale",
                "Seezed by Govt. Body",
                "Theft (Stolen)",
                "With Customer",
                "Not Traceable",
                "Other")
        )

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = "",
            onValueChange = { },
            label = { Text("Name & Contact No. of Person Met During Visit") },
            singleLine = true,
            readOnly = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        DropdownSpinner(
            "Borrower/Family Currently Living at Given Address",
            listOf("Yes", "No")
        )

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = "",
            onValueChange = { },
            label = { Text("Landmark of New / Existing Address") },
            singleLine = true,
            readOnly = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = "",
            onValueChange = { },
            label = { Text("If Not Then New Address of Borrower") },
            singleLine = true,
            readOnly = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = "",
            onValueChange = { },
            label = { Text("Current Contact No. of Borrower") },
            singleLine = true,
            readOnly = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = "",
            onValueChange = { },
            label = { Text("Borrower current Job Address if any") },
            singleLine = true,
            readOnly = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        DropdownSpinner(
            "Borrower Current Job Profile",
            listOf(
                "Govt. Servant",
                "PSU Employee",
                "Driver",
                "Farmer",
                "Self Employed",
                "Private Job",
                "Information Not Received")
        )

        Spacer(modifier = Modifier.height(16.dp))
        DropdownSpinner(
            "Borrower Financial Condition",
            listOf(
                "Good",
                "Average",
                "Poor",
                "Very Poor",
                "Information Not Received")
        )

        Spacer(modifier = Modifier.height(16.dp))
        DropdownSpinner(
            "Is This Case Workable or Non-Workable",
            listOf(
                "Workable",
                "Non-Workable"
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = "",
            onValueChange = { },
            label = { Text("Reasons for Workable or Non-Workable (Strength or Weakness)") },
            singleLine = true,
            readOnly = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        CheckboxGroup(
            "Current litigation",
            listOf("Arbitration",
                "Award passed",
                "EP",
                "Sarfesi",
                "DRT/DRAT/HC/CIVIL in Sarfesi Cases",
                "Police Complaint/FIR",
                "138 NIA",
                "No litigation")
        )

        Spacer(modifier = Modifier.height(16.dp))
        CheckboxGroup(
            "New litigation Required other Then Running",
            listOf("Arbitration",
                "EP",
                "138 NIA",
                "Sarfesi",
                "Police Complaint/FIR",
                "Nor Required")
        )

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = "",
            onValueChange = { },
            label = { Text("Why New Litigation Required") },
            singleLine = true,
            readOnly = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = "",
            onValueChange = { },
            label = { Text("Any Settlement Proposal or PTP with Date") },
            singleLine = true,
            readOnly = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row {

        }
        Button(onClick = {
            viewModel.capturePicture()
        }) {
            Text("Capture Image")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview4() {
    MyApplicationTheme {
        FeedbackScreen(
            PendingApp(
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
                contactNo = null), Modifier.fillMaxSize(), FeedbackViewModel(UserRepository())
        )
    }
}