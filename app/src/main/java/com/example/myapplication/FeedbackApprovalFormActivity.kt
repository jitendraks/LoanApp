package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.myapplication.api.UserRepository
import com.example.myapplication.components.ApiProgressBar
import com.example.myapplication.components.DropdownSpinner
import com.example.myapplication.data.ApprovalRequest
import com.example.myapplication.data.Constants
import com.example.myapplication.data.Constants.REQUEST_LOCATION_PERMISSION
import com.example.myapplication.data.FinancialCondition
import com.example.myapplication.data.Litigation
import com.example.myapplication.data.PendingApprovalFeedbackData
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.utils.DateTimeFormatter
import com.example.myapplication.viewmodel.FeedbackApprovalFormViewModel
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

class FeedbackApprovalFormActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentPhotoPath: String? = null
    private val viewModel: FeedbackApprovalFormViewModel =
        FeedbackApprovalFormViewModel(UserRepository())

    private val getContentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
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
            Toast.makeText(
                this,
                "Camera permission is required to capture images",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel.userData = intent.getParcelableExtra(Constants.USER_DATA)!!
        viewModel.feedbackData.value?.pendingApprovalFeedbackData  =
            intent.getParcelableExtra(Constants.PENDING_APPROVAL_FEEDBACK_DATA)!!

        setContent {
            MyApplicationTheme {
                FeedbackApprovalForm(
                    modifier = Modifier.fillMaxSize(),
                    app = viewModel.feedbackData.value?.pendingApprovalFeedbackData!!,
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
                    if (ContextCompat.checkSelfPermission(
                            this,
                            android.Manifest.permission.CAMERA
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
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
                        grantUriPermission(
                            packageName,
                            photoUri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        getContentLauncher.launch(intent)
                    }
                }

                else -> {}
            }
        }

        viewModel.fetchMasterDataApiState.observe(this)
        { event ->
            when (event) {
                is FeedbackApprovalFormViewModel.FetchMasterDataApiState.Success -> {
                    viewModel.masterData.value = event.masterData
                    viewModel.isLoading = false
                }

                is FeedbackApprovalFormViewModel.FetchMasterDataApiState.Error -> {
                    viewModel.isLoading = false
                }

                is FeedbackApprovalFormViewModel.FetchMasterDataApiState.Loading -> {

                }
            }
        }

        viewModel.postFeedbackApprovalDataApiState.observe(this)
        { event ->
            when (event) {
                is FeedbackApprovalFormViewModel.PostApprovalFeedbackDataApiState.Success -> {
                    viewModel.isLoading = false
                    Toast.makeText(this, "Feedback submitted", Toast.LENGTH_LONG).show()
                    finish()
                }

                is FeedbackApprovalFormViewModel.PostApprovalFeedbackDataApiState.Error -> {
                    viewModel.isLoading = false
                    Toast.makeText(this, event.exception.toString(), Toast.LENGTH_LONG).show()
                }

                is FeedbackApprovalFormViewModel.PostApprovalFeedbackDataApiState.Loading -> {

                }
            }
        }
        viewModel.isLoading = true
        viewModel.fetchMasterData()
        // getLocation(this)
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

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackApprovalForm(app: PendingApprovalFeedbackData,
                   modifier: Modifier,
                   viewModel: FeedbackApprovalFormViewModel,
                   loading: Boolean) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(app.loanNo) },
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
        ApprovalFeedbackFormWithLoading(
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
private fun ApprovalFeedbackFormWithLoading(
    app: PendingApprovalFeedbackData,
    modifier: Modifier,
    viewModel: FeedbackApprovalFormViewModel,
    loading: Boolean) {
    Box(modifier = modifier.fillMaxSize()) {
        ApprovalFeedbackForm(modifier = Modifier.fillMaxSize(), viewModel = viewModel)
        if (loading) {
            ApiProgressBar(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun ApprovalFeedbackForm(
    modifier: Modifier,
    viewModel: FeedbackApprovalFormViewModel
) {

    val scrollState = rememberScrollState()
    val photos by viewModel.photos.observeAsState(initial = arrayListOf())
    val feedbackData by viewModel.feedbackData.observeAsState(initial = null)
    val approvalRequest by viewModel.approvalRequest.observeAsState(initial = ApprovalRequest())

    Column(modifier = modifier
        .fillMaxSize()
        .verticalScroll(scrollState))
    {
        feedbackData?.let {
            feedbackData ->
            feedbackData.pendingApprovalFeedbackData?.let {
                pendingApprovalFeedbackData ->
                pendingApprovalFeedbackData.visitDate?.let { FieldRow(label = "Visit Date", value = it) }
                Spacer(modifier = Modifier.height(16.dp))
                FieldRow(label = "Visit Type", value = pendingApprovalFeedbackData.visitType)
                Spacer(modifier = Modifier.height(16.dp))
                feedbackData.visitDoneValue?.let { FieldRow(label = "Visit Done", value = it) }
                Spacer(modifier = Modifier.height(16.dp))
                feedbackData.visitDoneValue?.let { FieldRow(label = "Visit Done", value = it) }
                Spacer(modifier = Modifier.height(16.dp))
                feedbackData.relationValue?.let { FieldRow(label = "Relation Between Borrower and Coborrower", value = it) }
                Spacer(modifier = Modifier.height(16.dp))
                feedbackData.loanTypeValue?.let { FieldRow(label = "Type Of Loan", value = it) }
                Spacer(modifier = Modifier.height(16.dp))
                feedbackData.vehicleStatusValue?.let { FieldRow(label = "Vehicle Status", value = it) }
                Spacer(modifier = Modifier.height(16.dp))
                FieldRow(label = "Name & Contact No. of Person Met During Visit", value = pendingApprovalFeedbackData.nameWithMeet)
                Spacer(modifier = Modifier.height(16.dp))
                FieldRow(label = "Borrower/Family Currently Living at Given Address", value = if (pendingApprovalFeedbackData.borrowerLivingCurrentAddress) "Yes" else "No")
                Spacer(modifier = Modifier.height(16.dp))
                pendingApprovalFeedbackData.landMark.let { FieldRow(label = "Landmark of New / Existing Address", value = it) }
                Spacer(modifier = Modifier.height(16.dp))
                pendingApprovalFeedbackData.newAddressOfBorrower.let { FieldRow(label = "New Address of Borrower", value = it) }
                Spacer(modifier = Modifier.height(16.dp))
                pendingApprovalFeedbackData.currentContactNoOfBorrower.let { FieldRow(label = "Current Contact No. of Borrower", value = it) }
                Spacer(modifier = Modifier.height(16.dp))
                pendingApprovalFeedbackData.borrowerJobAddress.let { FieldRow(label = "Borrower current Job Address if any", value = it) }
                Spacer(modifier = Modifier.height(16.dp))
                feedbackData.jobValue?.let { FieldRow(label = "Borrower Current Job Profile", value = it) }
                Spacer(modifier = Modifier.height(16.dp))
                feedbackData.financialConditionValue?.let { FieldRow(label = "Borrower Financial Condition", value = it) }
                Spacer(modifier = Modifier.height(16.dp))
                FieldRow(label = "Is This Case Workable or Non-Workable", value = if (pendingApprovalFeedbackData.workableNonWorkable) "Workable" else "Non-Workable")
                Spacer(modifier = Modifier.height(16.dp))
                FieldRow(label = "Reasons for Workable or Non-Workable (Strength or Weakness)", value = pendingApprovalFeedbackData.reasonforWorkable)
                Spacer(modifier = Modifier.height(16.dp))
                feedbackData.litigationValue?.let { FieldRow(label = "Current litigation", value = it) }
                Spacer(modifier = Modifier.height(16.dp))
                feedbackData.newLitigationValue?.let { FieldRow(label = "New litigation Required other Then Running", value = it) }
                Spacer(modifier = Modifier.height(16.dp))
                FieldRow(label = "Why New Litigation Required", value = pendingApprovalFeedbackData.newLitigationReuired)
                Spacer(modifier = Modifier.height(16.dp))
                FieldRow(label = "Any Settlement Proposal or PTP with Date", value = pendingApprovalFeedbackData.anySettlementProposal)

                Spacer(modifier = Modifier.height(16.dp))
                if (viewModel.masterData.value?.litigations?.isNotEmpty() == true) {
                    Spacer(modifier = Modifier.height(16.dp))
                    DropdownSpinner(
                        "New litigation Required other Then Running",
                        viewModel.masterData.value?.litigations ?: emptyList()
                    ) { result ->
                        approvalRequest?.let {
                            val ar = it.copy()
                            ar.newLitigationId = (result as Litigation).litigationId
                            viewModel.approvalRequest.value = ar
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                DropdownSpinner(
                    "Is This Case Workable or Non-Workable",
                    listOf(
                        "Workable",
                        "Non-Workable"
                    )
                ) { result ->
                    approvalRequest?.let {
                        val ar = it.copy()
                        ar.workableNonWorkable = if (result == "Workable") 1 else 0
                        viewModel.approvalRequest.value = ar
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = approvalRequest!!.reasonforWorkable,
                    onValueChange = { newValue ->
                        val ar = approvalRequest!!.copy()
                        ar.reasonforWorkable = newValue
                        viewModel.approvalRequest.value = ar
                    },
                    label = { Text("Reasons for Workable or Non-Workable (Strength or Weakness)") },
                    singleLine = true,
                    readOnly = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = approvalRequest!!.newLitigationReuired,
                    onValueChange = { newValue ->
                        val ar = approvalRequest!!.copy()
                        ar.newLitigationReuired = newValue
                        viewModel.approvalRequest.value = ar
                    },
                    label = { Text("Why New Litigation Required") },
                    singleLine = true,
                    readOnly = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = approvalRequest!!.anySettlementProposal,
                    onValueChange = { newValue ->
                        val ar = approvalRequest!!.copy()
                        ar.anySettlementProposal = newValue
                        viewModel.approvalRequest.value = ar
                    },
                    label = { Text("Any Settlement Proposal or PTP with Date") },
                    singleLine = true,
                    readOnly = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = approvalRequest!!.approverRemark,
                    onValueChange = { newValue ->
                        val ar = approvalRequest!!.copy()
                        ar.approverRemark = newValue
                        viewModel.approvalRequest.value = ar
                    },
                    label = { Text("Remark") },
                    singleLine = true,
                    readOnly = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }


        /*Button(onClick = {
            // viewModel.capturePicture()
        }) {
            Text("Show Images")
        }*/

        Row {
            Button(onClick = {
                viewModel.approveFeedback(true)
            }) {
                Text("Approve")
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(onClick = {
                viewModel.approveFeedback(false)
            }) {
                Text("Reject")
            }
        }
    }
}

@Composable
private fun FieldRow(label: String, value: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { },
        label = { Text(label) },
        singleLine = true,
        readOnly = true,
        modifier = Modifier.fillMaxWidth()
    )
}
