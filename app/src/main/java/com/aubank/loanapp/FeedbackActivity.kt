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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.aubank.loanapp.api.UserRepository
import com.aubank.loanapp.components.ApiProgressBar
import com.aubank.loanapp.components.DropdownSpinner
import com.aubank.loanapp.components.ThumbnailView
import com.aubank.loanapp.data.*
import com.aubank.loanapp.ui.theme.LoanAppTheme
import com.aubank.loanapp.utils.DateTimeFormatter
import com.aubank.loanapp.viewmodel.FeedbackViewModel
import com.aubank.loanapp.viewmodel.NavigationEvent
import com.google.android.gms.location.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*

class FeedbackActivity : ComponentActivity(), LocationListener {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentPhotoPath: String? = null
    private val viewModel: FeedbackViewModel = FeedbackViewModel(UserRepository())

    private val getContentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.addPhotoPath(currentPhotoPath!!)
            val photoUri = FileProvider.getUriForFile(
                this,
                "${BuildConfig.APPLICATION_ID}.provider",
                File(currentPhotoPath!!)
            )
            viewModel.addPhoto(photoUri)
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.capturePicture()
        } else {
            Toast.makeText(this, "Camera permission is required to capture images", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app: PendingApp = intent.extras?.getParcelable(Constants.LOAN_APP)!!
        viewModel.userData = intent.getParcelableExtra(Constants.USER_DATA)!!
        viewModel.feedbackData.value!!.VisitDoneId = intent.getIntExtra(Constants.VISIT_DONE_ID, 0)
        viewModel.feedbackData.value!!.VisitType = "New Visit"

        setContent {
            LoanAppTheme {
                (application as LoanApplication).getMasterData()?.let {
                    FeedbackScreen(
                        app = app,
                        viewModel = viewModel,
                        masterData = it,
                        loading = viewModel.isLoading
                    )
                }
            }
        }

        setupObservers()
        getLocation(this)
    }

    private fun setupObservers() {
        viewModel.navigationEvent.observe(this) { event ->
            when (event) {
                NavigationEvent.NavigateBack -> finish()
                NavigationEvent.NavigateToCaptureActivity -> handleCameraPermission()
                else -> {}
            }
        }

        viewModel.postFeedbackDataApiState.observe(this) { event ->
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
                    viewModel.isLoading = true
                }
            }
        }
    }

    private fun handleCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            launchCamera()
        }
    }

    private fun launchCamera() {
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

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun getLocation(activity: Activity) {
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
        val locationRequest = LocationRequest.Builder(10000).apply {
            setMinUpdateIntervalMillis(5000)
            setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            setWaitForAccurateLocation(false)
        }.build()
        fusedLocationClient.requestLocationUpdates(locationRequest, this, Looper.getMainLooper())
    }

    private fun requestLocationPermission(activity: Activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), Constants.REQUEST_LOCATION_PERMISSION)
        } else {
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
    var isError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(app.caseNo) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomActionBar(
                onCaptureImage = { viewModel.capturePicture() },
                onSubmit = {
                    if (viewModel.validateFormFields()) {
                        viewModel.submitFeedback(app)
                    } else {
                        isError = true
                    }
                },
                loading = loading
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            FeedbackForm(
                app = app,
                viewModel = viewModel,
                masterData = masterData,
                isError = isError,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            )

            if (loading) {
                ApiProgressBar(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun BottomActionBar(
    onCaptureImage: () -> Unit,
    onSubmit: () -> Unit,
    loading: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onCaptureImage,
                modifier = Modifier.weight(1f),
                enabled = !loading
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add Photo")
            }

            Button(
                onClick = onSubmit,
                modifier = Modifier.weight(1f),
                enabled = !loading
            ) {
                Text("Submit Feedback")
            }
        }
    }
}

@Composable
private fun FeedbackForm(
    app: PendingApp,
    viewModel: FeedbackViewModel,
    masterData: MasterData,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val photos by viewModel.photos.observeAsState(initial = arrayListOf())
    val feedbackData by viewModel.feedbackData.observeAsState(initial = FeedbackData())

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Visit Details Section
        SectionCard("Visit Details") {
            LabeledTextField(
                label = "Visit Date",
                value = DateTimeFormatter.formatDate(LocalDateTime.now()),
                readOnly = true
            )
            LabeledTextField(
                label = "Visit Type",
                value = "New Visit",
                readOnly = true
            )
            LabeledTextField(
                label = "Name of Borrower",
                value = app.borrowerName,
                readOnly = true
            )
            LabeledTextField(
                label = "Visit Done",
                value = masterData.visitDones.find { it.visitDoneId == feedbackData.VisitDoneId }?.toString() ?: "",
                readOnly = true
            )
        }

        // Borrower Information Section
        SectionCard("Borrower Information") {
            LabeledDropdown(
                label = "Relation between Borrower and Co-Borrower",
                items = masterData.relations,
                isError = isError && viewModel.feedbackData.value!!.RelationId <= 0,
                onSelected = { result ->
                    val fd = feedbackData.copy()
                    fd.RelationId = (result as Relation).relationId
                    viewModel.feedbackData.value = fd
                }
            )

            LabeledTextField(
                label = "Name & Contact No. of Person Met During Visit",
                value = feedbackData.NameWithMeet,
                onValueChange = { newValue ->
                    val fd = feedbackData.copy()
                    fd.NameWithMeet = newValue
                    viewModel.feedbackData.value = fd
                }
            )

            LabeledDropdown(
                label = "Borrower/Family Currently Living at Given Address",
                items = listOf("Yes", "No"),
                onSelected = { result ->
                    val fd = feedbackData.copy()
                    fd.BorrowerLivingCurrentAddress = (result == "Yes")
                    viewModel.feedbackData.value = fd
                }
            )

            LabeledTextField(
                label = "Landmark of New / Existing Address",
                value = feedbackData.LandMark,
                onValueChange = { newValue ->
                    val fd = feedbackData.copy()
                    fd.LandMark = newValue
                    viewModel.feedbackData.value = fd
                }
            )

            if (!feedbackData.BorrowerLivingCurrentAddress) {
                LabeledTextField(
                    label = "New Address of Borrower",
                    value = feedbackData.NewAddressOfBorrower,
                    onValueChange = { newValue ->
                        val fd = feedbackData.copy()
                        fd.NewAddressOfBorrower = newValue
                        viewModel.feedbackData.value = fd
                    }
                )
            }

            LabeledTextField(
                label = "Current Contact No. of Borrower",
                value = feedbackData.CurrentContactNoOfBorrower,
                onValueChange = { newValue ->
                    val fd = feedbackData.copy()
                    fd.CurrentContactNoOfBorrower = newValue
                    viewModel.feedbackData.value = fd
                }
            )

            LabeledTextField(
                label = "Borrower current Job Address if any",
                value = feedbackData.BorrowerJobAddress,
                onValueChange = { newValue ->
                    val fd = feedbackData.copy()
                    fd.BorrowerJobAddress = newValue
                    viewModel.feedbackData.value = fd
                }
            )
        }

        // Loan Information Section
        SectionCard("Loan Information") {
            LabeledDropdown(
                label = "Type of Loan",
                items = masterData.loanTypes,
                isError = isError && viewModel.feedbackData.value!!.TypeOfLoanId <= 0,
                onSelected = { result ->
                    val fd = feedbackData.copy()
                    fd.TypeOfLoanId = (result as LoanType).typeOfLoanId
                    viewModel.feedbackData.value = fd
                }
            )

            if (masterData.vehicleStatuses.isNotEmpty()) {
                LabeledDropdown(
                    label = "Vehicle Status",
                    items = masterData.vehicleStatuses,
                    isError = isError && viewModel.feedbackData.value!!.VehicleStatusId <= 0,
                    onSelected = { result ->
                        val fd = feedbackData.copy()
                        fd.VehicleStatusId = (result as VehicleStatus).vehicleStatusId
                        viewModel.feedbackData.value = fd
                    }
                )
            }

            LabeledDropdown(
                label = "Borrower Current Job Profile",
                items = masterData.jobs,
                isError = isError && viewModel.feedbackData.value!!.JobId <= 0,
                onSelected = { result ->
                    val fd = feedbackData.copy()
                    fd.JobId = (result as Job).jobId
                    viewModel.feedbackData.value = fd
                }
            )

            LabeledDropdown(
                label = "Borrower Financial Condition",
                items = masterData.finConditions,
                isError = isError && viewModel.feedbackData.value!!.FinConditionId <= 0,
                onSelected = { result ->
                    val fd = feedbackData.copy()
                    fd.FinConditionId = (result as FinancialCondition).finConditionId
                    viewModel.feedbackData.value = fd
                }
            )

            LabeledDropdown(
                label = "Borrower Income",
                items = masterData.incomeSlabs,
                onSelected = { result ->
                    val fd = feedbackData.copy()
                    fd.IncomeId = (result as IncomeSlab).incomeId
                    viewModel.feedbackData.value = fd
                }
            )
        }

        // Assessment Section
        SectionCard("Case Assessment") {
            LabeledDropdown(
                label = "Is This Case Workable or Non-Workable",
                items = listOf("Workable", "Non-Workable"),
                onSelected = { result ->
                    val fd = feedbackData.copy()
                    fd.WorkableNonWorkable = result == "Workable"
                    viewModel.feedbackData.value = fd
                }
            )

            LabeledTextField(
                label = "Reasons for Workable or Non-Workable (Strength or Weakness)",
                value = feedbackData.ReasonforWorkable,
                onValueChange = { newValue ->
                    val fd = feedbackData.copy()
                    fd.ReasonforWorkable = newValue
                    viewModel.feedbackData.value = fd
                }
            )

            LabeledTextField(
                label = "Current Litigation",
                value = app.currentLitigation ?: "",
                readOnly = true
            )

            if (masterData.litigations.isNotEmpty()) {
                LabeledDropdown(
                    label = "New Litigation Required other Then Running",
                    items = masterData.litigations,
                    onSelected = { result ->
                        val fd = feedbackData.copy()
                        fd.NewLitigationId = (result as Litigation).litigationId
                        viewModel.feedbackData.value = fd
                    }
                )

                if (feedbackData.NewLitigationId > 0) {
                    LabeledTextField(
                        label = "Why New Litigation Required",
                        value = feedbackData.NewLitigationReuired,
                        onValueChange = { newValue ->
                            val fd = feedbackData.copy()
                            fd.NewLitigationReuired = newValue
                            viewModel.feedbackData.value = fd
                        }
                    )
                }
            }

            LabeledTextField(
                label = "Any Settlement Proposal or PTP with Date",
                value = feedbackData.AnySettlementProposal,
                onValueChange = { newValue ->
                    val fd = feedbackData.copy()
                    fd.AnySettlementProposal = newValue
                    viewModel.feedbackData.value = fd
                }
            )
        }

        // Photos Section
        if (photos.isNotEmpty()) {
            SectionCard("Photos") {
                LazyRow(
                    modifier = Modifier.height(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(photos) { photoUri ->
                        ThumbnailView(imageUri = photoUri)
                    }
                }
            }
        }

        // Bottom spacing for fixed button
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            content()
        }
    }
}

@Composable
private fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit = {},
    readOnly: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        readOnly = readOnly,
        modifier = Modifier.fillMaxWidth(),
        colors = if (readOnly) {
            OutlinedTextFieldDefaults.colors(
                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        } else {
            OutlinedTextFieldDefaults.colors()
        }
    )
}

@Composable
private fun <T : Any> LabeledDropdown(
    label: String,
    items: List<T>,
    isError: Boolean = false,
    onSelected: (T) -> Unit
) {
    Column {
        DropdownSpinner(
            label,
            items
        ) { selection: Any ->
            @Suppress("UNCHECKED_CAST")
            val item = selection as T
            onSelected(item)
        }

        if (isError) {
            Text(
                text = "This field is required",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FeedbackScreenPreview() {
    LoanAppTheme {
        FeedbackScreen(
            app = PendingApp(
                lanNo = "5454545454",
                loanAmount = "500000",
                borrowerName = "John Doe",
                loanDetailId = 0,
                caseType = null,
                caseNo = "LVWAR01314-150291591",
                cbsLoanNo = null,
                customerId = null,
                stateName = null,
                branch = null,
                hubName = null,
                fatherName = null,
                borrowerAddress = "Sample Address",
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
                currentLitigation = "AWARD PASSED"
            ),
            viewModel = FeedbackViewModel(UserRepository()),
            masterData = MasterData(
                jobs = emptyList(),
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