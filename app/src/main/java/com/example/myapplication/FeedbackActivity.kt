package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberImagePainter
import com.example.myapplication.api.UserRepository
import com.example.myapplication.components.DropdownSpinner
import com.example.myapplication.data.Constants
import com.example.myapplication.data.Constants.REQUEST_LOCATION_PERMISSION
import com.example.myapplication.data.FeedbackData
import com.example.myapplication.data.FinancialCondition
import com.example.myapplication.data.Job
import com.example.myapplication.data.Litigation
import com.example.myapplication.data.LoanType
import com.example.myapplication.data.PendingApp
import com.example.myapplication.data.Relation
import com.example.myapplication.data.VehicleStatus
import com.example.myapplication.data.VisitDone
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.utils.DateTimeFormatter
import com.example.myapplication.viewmodel.FeedbackViewModel
import com.example.myapplication.viewmodel.NavigationEvent
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


class FeedbackActivity : ComponentActivity() {
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
        val app: PendingApp = intent.getParcelableExtra<PendingApp>(Constants.LOAN_APP)!!
        viewModel.userData = intent.getParcelableExtra(Constants.USER_DATA)!!

        setContent {
            MyApplicationTheme {
                FeedbackScreen(
                    modifier = Modifier.fillMaxSize(),
                    app = app,
                    viewModel = viewModel,
                    loading = viewModel.isLoading
                )
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

        viewModel.fetchMasterDataApiState.observe(this)
        { event ->
            when (event) {
                is FeedbackViewModel.FetchMasterDataApiState.Success -> {
                    viewModel.masterData.value = event.masterData
                    viewModel.isLoading = false
                }

                is FeedbackViewModel.FetchMasterDataApiState.Error -> {
                    viewModel.isLoading = false
                }

                is FeedbackViewModel.FetchMasterDataApiState.Loading -> {

                }
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

        viewModel.isLoading = true
        viewModel.fetchMasterData()
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
        }.build()

        fusedLocationClient.requestLocationUpdates(locationRequest, LocationListener {
            viewModel.feedbackData.value!!.Latitude = it.latitude.toString()
            viewModel.feedbackData.value!!.Longitude = it.longitude.toString()

        }, Looper.getMainLooper())
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(app: PendingApp,
                   modifier: Modifier,
                   viewModel: FeedbackViewModel,
                   loading: Boolean) {
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
            loading
        )
    })
}

@Composable
fun FeedbackFormWithLoading(
    app: PendingApp,
    modifier: Modifier,
    viewModel: FeedbackViewModel,
    loading: Boolean) {
    Box(modifier = modifier.fillMaxSize()) {
        FeedbackForm(app = app, modifier = Modifier.fillMaxSize(), viewModel = viewModel)
        if (loading) {
            ApiProgressBar(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun FeedbackForm(
    app: PendingApp,
    modifier: Modifier,
    viewModel: FeedbackViewModel) {

    val scrollState = rememberScrollState()
    val photos by viewModel.photos.observeAsState(initial = arrayListOf())
    val feedbackData by viewModel.feedbackData.observeAsState(initial = FeedbackData())
    feedbackData.ContactNoWithMeet = "gdgfdfgdd"
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
        DropdownSpinner(
            "Visit Type",
            listOf("New Visit", "Follow up"),
        ) {
            result ->
            val fd = feedbackData.copy()
            fd.VisitType = result.toString()
            viewModel.feedbackData.value = fd
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = app.borrowerName,
            onValueChange = { },
            label = { Text("Name of Borrower") },
            singleLine = true,
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (viewModel.masterData.value?.visitDones?.isEmpty() == false) {
            Spacer(modifier = Modifier.height(16.dp))
            DropdownSpinner(
                "Visit Done",
                viewModel.masterData.value?.visitDones ?: emptyList()
            ) {
                result ->
                val fd = feedbackData.copy()
                fd.VisitDoneId = (result as VisitDone).visitDoneId
                viewModel.feedbackData.value = fd
            }

        }

        Spacer(modifier = Modifier.height(16.dp))
        DropdownSpinner(
            "Relation between Borrower and Co-Borrower",
            viewModel.masterData.value?.relations ?: emptyList()
        ) { result ->
            val fd = feedbackData.copy()
            fd.RelationId = (result as Relation).relationId
            viewModel.feedbackData.value = fd
        }

        Spacer(modifier = Modifier.height(16.dp))
        DropdownSpinner(
            "Type of Loan",
            viewModel.masterData.value?.loanTypes ?: emptyList()
        ) { result ->
            val fd = feedbackData.copy()
            fd.TypeOfLoanId = (result as LoanType).typeOfLoanId
            viewModel.feedbackData.value = fd
        }

        if (viewModel.masterData.value?.vehicleStatuses?.isEmpty() == false) {

            Spacer(modifier = Modifier.height(16.dp))
            DropdownSpinner(
                "Vehicle Status",
                viewModel.masterData.value?.vehicleStatuses ?: emptyList()
            ) { result ->
                val fd = feedbackData.copy()
                fd.VehicleStatusId = (result as VehicleStatus).vehicleStatusId
                viewModel.feedbackData.value = fd
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
            viewModel.masterData.value?.jobs ?: emptyList()
        ) { result ->
            val fd = feedbackData.copy()
            fd.JobId = (result as Job).jobId
            viewModel.feedbackData.value = fd
        }

        Spacer(modifier = Modifier.height(16.dp))
        DropdownSpinner(
            "Borrower Financial Condition",
            viewModel.masterData.value?.finConditions ?: emptyList()
        ) { result ->
            val fd = feedbackData.copy()
            fd.FinConditionId = (result as FinancialCondition).finConditionId
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

        if (viewModel.masterData.value?.litigations?.isNotEmpty() == true) {
            Spacer(modifier = Modifier.height(16.dp))
            DropdownSpinner(
                "Current litigation",
                viewModel.masterData.value?.litigations ?: emptyList()
            ) { result ->
                val fd = feedbackData.copy()
                fd.LitigationId = (result as Litigation).litigationId
                viewModel.feedbackData.value = fd
            }
        }


        if (viewModel.masterData.value?.litigations?.isNotEmpty() == true) {
            Spacer(modifier = Modifier.height(16.dp))
            DropdownSpinner(
                "New litigation Required other Then Running",
                viewModel.masterData.value?.litigations ?: emptyList()
            ) { result ->
                val fd = feedbackData.copy()
                fd.NewLitigationId = (result as Litigation).litigationId
                viewModel.feedbackData.value = fd
            }
        }

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
            viewModel.submitFeedback(app)
        }) {
            Text("Submit")
        }
    }
}

@Composable
fun ThumbnailView(imageUri: Uri?) {
    if (imageUri != null) {
        // Load image from the URI using Coil
        Image(
            painter = rememberImagePainter(imageUri),
            contentDescription = "Captured Image",
            modifier = Modifier
                .size(150.dp) // Adjust size for thumbnail
                .padding(8.dp),
            contentScale = ContentScale.Crop
        )
    } else {
        // Placeholder if no image is available
        Box(
            modifier = Modifier
                .size(150.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "No Image")
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
                contactNo = null),
            Modifier.fillMaxSize(),
            FeedbackViewModel(UserRepository()),
            loading = false
        )
    }
}