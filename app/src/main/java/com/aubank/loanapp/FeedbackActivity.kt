package com.aubank.loanapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.aubank.loanapp.api.UserRepository
import com.aubank.loanapp.components.ApiProgressBar
import com.aubank.loanapp.components.DropdownSpinner
import com.aubank.loanapp.components.ThumbnailView
import com.aubank.loanapp.data.Constants
import com.aubank.loanapp.data.Constants.REQUEST_LOCATION_PERMISSION
import com.aubank.loanapp.data.FeedbackData
import com.aubank.loanapp.data.FinancialCondition
import com.aubank.loanapp.data.IncomeSlab
import com.aubank.loanapp.data.Job
import com.aubank.loanapp.data.Litigation
import com.aubank.loanapp.data.LoanType
import com.aubank.loanapp.data.MasterData
import com.aubank.loanapp.data.PendingApp
import com.aubank.loanapp.data.Relation
import com.aubank.loanapp.data.VehicleStatus
import com.aubank.loanapp.data.VisitDone
import com.aubank.loanapp.ui.theme.MyApplicationTheme
import com.aubank.loanapp.utils.DateTimeFormatter
import com.aubank.loanapp.viewmodel.FeedbackViewModel
import com.aubank.loanapp.viewmodel.NavigationEvent
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Date
import java.util.Locale


class FeedbackActivity : ComponentActivity(), LocationListener {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentPhotoPath: String? = null
    private val viewModel: FeedbackViewModel = FeedbackViewModel(UserRepository())
    private val getContentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.addPhotoPath(currentPhotoPath!!)
            val photoUri = FileProvider.getUriForFile(
                this,
                "${BuildConfig.APPLICATION_ID}.provider",
                File(currentPhotoPath!!)
            )
            // Handle the captured image URI
            viewModel.addPhoto(photoUri)
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission is granted, capture the image
            viewModel.capturePicture()
        } else {
            // Permission denied, show a message to the user
            Toast.makeText(this, "Camera permission is required to capture images", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app: PendingApp = intent.extras?.getParcelable<PendingApp>(Constants.LOAN_APP)!!
        viewModel.userData = intent.getParcelableExtra(Constants.USER_DATA)!!
        viewModel.feedbackData.value!!.VisitDoneId = intent.getIntExtra(Constants.VISIT_DONE_ID, 0)
        viewModel.feedbackData.value!!.VisitType = "New Visit"
        setContent {
            MyApplicationTheme {
                (application as LoanApplication).getMasterData()?.let {
                    FeedbackScreen(
                        app = app,
                        viewModel = viewModel,
                        it,
                        loading = viewModel.isLoading
                    )
                }
            }
        }

        viewModel.navigationEvent.observe(this)
        { event ->
            when (event) {
                NavigationEvent.NavigateBack -> {
                    finish()
                }
                NavigationEvent.NavigateToCaptureActivity -> {
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        // Request the camera permission
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    } else {
                        // Permission is already granted, launch the camera
                        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        val photoUri = FileProvider.getUriForFile(
                            this,
                            "${BuildConfig.APPLICATION_ID}.provider",
                            createImageFile()
                        )
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                        grantUriPermission(packageName, photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        getContentLauncher.launch(intent)
                    }
                }
                else -> {}
            }
        }

        viewModel.postFeedbackDataApiState.observe(this)
        { event ->
            when (event) {
                is FeedbackViewModel.PostFeedbackDataApiState.Success -> {
                    viewModel.isLoading = false
                    Toast.makeText(this, "Feedback submitted", Toast.LENGTH_LONG).show()
                    finish()
                }

                is FeedbackViewModel.PostFeedbackDataApiState.Error -> {
                    viewModel.isLoading = false
                    Toast.makeText(this, event.exception.toString(), Toast.LENGTH_LONG).show()
                }

                is FeedbackViewModel.PostFeedbackDataApiState.Loading -> {

                }
            }
        }

        getLocation(this)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    private fun getLocation(activity: Activity) {
        presenceViewModel.isLoading = true
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission(activity)
        } else {
            requestLocationUpdates(activity)
        }
    }
    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates(context: Context) {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        // Use LocationRequest.Builder for better readability
        val locationRequest = LocationRequest.Builder(
            // Update interval set to 10 seconds to balance accuracy and battery usage
            10000
        ).apply {
            // Fastest interval set to 5 seconds to allow for quicker updates if available
            setMinUpdateIntervalMillis(5000)
            // High accuracy is required for this application (e.g., precise location tracking)
            setPriority(Priority.PRIORITY_HIGH_ACCURACY)

            // Consider using setWaitForAccurateLocation(true) for high accuracy
            setWaitForAccurateLocation(false)

        }.build()

        fusedLocationClient.requestLocationUpdates(locationRequest, this, Looper.getMainLooper())
    }

    private fun requestLocationPermission(activity: Activity) {
        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_LOCATION_PERMISSION)
        } else {
            // Permission already granted
            getLocation(activity)
        }
    }

    override fun onLocationChanged(location: Location) {
        viewModel.feedbackData.value!!.Latitude = location.latitude.toString()
        viewModel.feedbackData.value!!.Longitude = location.longitude.toString()
        fusedLocationClient.removeLocationUpdates(this)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackScreen(
    app: PendingApp,
    viewModel: FeedbackViewModel,
    masterData: MasterData,
    loading: Boolean
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(app.caseNo) },
            colors = topAppBarColors(
                containerColor = Color.Blue, // Set your desired background color here
                titleContentColor = Color.White,
            ),
            navigationIcon = {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )
    }, content = {
            innerPadding ->
        FeedbackFormWithLoading(
            app,
            modifier = Modifier
                .padding(innerPadding)
                .systemBarsPadding(),
            viewModel,
            masterData,
            loading
        )
    })
}

@Composable
private fun FeedbackFormWithLoading(
    app: PendingApp,
    modifier: Modifier,
    viewModel: FeedbackViewModel,
    masterData: MasterData,
    loading: Boolean) {
    Box(modifier = modifier.fillMaxSize()) {
        FeedbackForm(app = app, modifier = Modifier.fillMaxSize(), viewModel = viewModel, masterData)
        if (loading) {
            ApiProgressBar(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun FeedbackForm(
    app: PendingApp,
    modifier: Modifier,
    viewModel: FeedbackViewModel,
    masterData: MasterData
) {

    val scrollState = rememberScrollState()
    val photos by viewModel.photos.observeAsState(initial = arrayListOf())
    val feedbackData by viewModel.feedbackData.observeAsState(initial = FeedbackData())
    var isError by remember { mutableStateOf(false) }

    Column(modifier = modifier
        .fillMaxSize()
        .verticalScroll(scrollState))
    {
        viewModel.feedbackData.value!!.VisitDate = DateTimeFormatter.formatVisitDateTimeFormat(LocalDateTime.now())
        OutlinedTextField(
            value = DateTimeFormatter.formatDate(LocalDateTime.now()),
            onValueChange = { },
            label = { Text("Visit Date") },
            singleLine = true,
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = "New Visit",
            onValueChange = { },
            label = { Text("Visit Type") },
            singleLine = true,
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
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
        OutlinedTextField(
            value = masterData.visitDones.find { visitDone: VisitDone ->  visitDone.visitDoneId == feedbackData.VisitDoneId}.toString(),
            onValueChange = { },
            label = { Text("Visit Done") },
            singleLine = true,
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        DropdownSpinner(
            "Relation between Borrower and Co-Borrower",
            masterData.relations
        ) { result ->
            val fd = feedbackData.copy()
            fd.RelationId = (result as Relation).relationId
            viewModel.feedbackData.value = fd
        }
        if (isError) {
            if (viewModel.feedbackData.value!!.RelationId <= 0) {
                Text(
                    text = "This field is required",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        DropdownSpinner(
            "Type of Loan",
            masterData.loanTypes
        ) { result ->
            val fd = feedbackData.copy()
            fd.TypeOfLoanId = (result as LoanType).typeOfLoanId
            viewModel.feedbackData.value = fd
        }
        if (isError) {
            if (viewModel.feedbackData.value!!.TypeOfLoanId <= 0) {
                Text(
                    text = "This field is required",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        if (masterData.vehicleStatuses.isNotEmpty()) {

            Spacer(modifier = Modifier.height(16.dp))
            DropdownSpinner(
                "Vehicle Status",
                masterData.vehicleStatuses
            ) { result ->
                val fd = feedbackData.copy()
                fd.VehicleStatusId = (result as VehicleStatus).vehicleStatusId
                viewModel.feedbackData.value = fd
            }
        }
        if (isError) {
            if (viewModel.feedbackData.value!!.VehicleStatusId <= 0) {
                Text(
                    text = "This field is required",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = feedbackData.NameWithMeet,
            onValueChange = { newValue ->
                val fd = feedbackData.copy()
                fd.NameWithMeet = newValue
                viewModel.feedbackData.value = fd
                            },
            label = { Text("Name & Contact No. of Person Met During Visit") },
            singleLine = true,
            readOnly = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        DropdownSpinner(
            "Borrower/Family Currently Living at Given Address",
            listOf("Yes", "No")
        ) { result ->
            val fd = feedbackData.copy()
            fd.BorrowerLivingCurrentAddress = (result == "Yes")
            viewModel.feedbackData.value = fd
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = feedbackData.LandMark,
            onValueChange = {newValue ->
                val fd = feedbackData.copy()
                fd.LandMark = newValue
                viewModel.feedbackData.value = fd
                            },
            label = { Text("Landmark of New / Existing Address") },
            singleLine = true,
            readOnly = false,
            modifier = Modifier.fillMaxWidth()
        )

        if (!feedbackData.BorrowerLivingCurrentAddress) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = feedbackData.NewAddressOfBorrower,
                onValueChange = { newValue ->
                    val fd = feedbackData.copy()
                    fd.NewAddressOfBorrower = newValue
                    viewModel.feedbackData.value = fd
                },
                label = { Text("If Not Then New Address of Borrower") },
                singleLine = true,
                readOnly = false,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = feedbackData.CurrentContactNoOfBorrower,
            onValueChange = { newValue ->
                val fd = feedbackData.copy()
                fd.CurrentContactNoOfBorrower = newValue
                viewModel.feedbackData.value = fd
            },
            label = { Text("Current Contact No. of Borrower") },
            singleLine = true,
            readOnly = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = feedbackData.BorrowerJobAddress,
            onValueChange = { newValue ->
                val fd = feedbackData.copy()
                fd.BorrowerJobAddress = newValue
                viewModel.feedbackData.value = fd
            },
            label = { Text("Borrower current Job Address if any") },
            singleLine = true,
            readOnly = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        DropdownSpinner(
            "Borrower Current Job Profile",
            masterData.jobs
        ) { result ->
            val fd = feedbackData.copy()
            fd.JobId = (result as Job).jobId
            viewModel.feedbackData.value = fd
        }
        if (isError) {
            if (viewModel.feedbackData.value!!.JobId <= 0) {
                Text(
                    text = "This field is required",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        DropdownSpinner(
            "Borrower Financial Condition",
            masterData.finConditions
        ) { result ->
            val fd = feedbackData.copy()
            fd.FinConditionId = (result as FinancialCondition).finConditionId
            viewModel.feedbackData.value = fd
        }
        if (isError) {
            if (viewModel.feedbackData.value!!.FinConditionId <= 0) {
                Text(
                    text = "This field is required",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        DropdownSpinner(
            "Borrower Income",
            masterData.incomeSlabs
        ) { result ->
            val fd = feedbackData.copy()
            fd.IncomeId = (result as IncomeSlab).incomeId
            viewModel.feedbackData.value = fd
        }

        Spacer(modifier = Modifier.height(16.dp))
        DropdownSpinner(
            "Is This Case Workable or Non-Workable",
            listOf(
                "Workable",
                "Non-Workable"
            )
        ) { result ->
            val fd = feedbackData.copy()
            fd.WorkableNonWorkable = result == "Workable"
            viewModel.feedbackData.value = fd
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = feedbackData.ReasonforWorkable,
            onValueChange = { newValue ->
                val fd = feedbackData.copy()
                fd.ReasonforWorkable = newValue
                viewModel.feedbackData.value = fd
            },
            label = { Text("Reasons for Workable or Non-Workable (Strength or Weakness)") },
            singleLine = true,
            readOnly = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = app.currentLitigation ?: "",
            onValueChange = { newValue ->
                val fd = feedbackData.copy()
                fd.ReasonforWorkable = newValue
                viewModel.feedbackData.value = fd
            },
            label = { Text("Current litigation") },
            singleLine = true,
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )


        if (masterData.litigations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            DropdownSpinner(
                "New litigation Required other Then Running",
                masterData.litigations
            ) { result ->
                val fd = feedbackData.copy()
                fd.NewLitigationId = (result as Litigation).litigationId
                viewModel.feedbackData.value = fd
            }
        }

        if (feedbackData.NewLitigationId > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = feedbackData.NewLitigationReuired,
                onValueChange = { newValue ->
                    val fd = feedbackData.copy()
                    fd.NewLitigationReuired = newValue
                    viewModel.feedbackData.value = fd
                },
                label = { Text("Why New Litigation Required") },
                singleLine = true,
                readOnly = false,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = feedbackData.AnySettlementProposal,
            onValueChange = { newValue ->
                val fd = feedbackData.copy()
                fd.AnySettlementProposal = newValue
                viewModel.feedbackData.value = fd
            },
            label = { Text("Any Settlement Proposal or PTP with Date") },
            singleLine = true,
            readOnly = false,
            modifier = Modifier.fillMaxWidth()
        )


        Spacer(modifier = Modifier.height(16.dp))
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(bottom = 16.dp)
        ) {
            items(photos) { photoUri ->
                ThumbnailView(imageUri = photoUri)
            }
        }

        Button(onClick = {
            viewModel.capturePicture()
        }) {
            Text("Capture Image")
        }

        Button(onClick = {
            if (viewModel.validateFormFields()) {
                viewModel.submitFeedback(app)
            } else {
                isError = true
            }
        }) {
            Text("Submit")
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun GreetingPreview() {
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
                contactNo = null,
                currentLitigation = "Current Litigation"),
            FeedbackViewModel(UserRepository()),
            MasterData(jobs = emptyList(),
                loanTypes = emptyList(),
                relations = emptyList(),
                finConditions = emptyList(),
                visitDones = emptyList(),
                incomeSlabs = emptyList(),
                litigations = emptyList(),
                vehicleStatuses = emptyList()
            ),
            loading = false
        )
    }
}