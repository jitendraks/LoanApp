package com.aubank.loanapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.aubank.loanapp.api.UserRepository
import com.aubank.loanapp.components.ApiProgressBar
import com.aubank.loanapp.components.DropdownSpinner
import com.aubank.loanapp.components.EnhancedTopAppBar
import com.aubank.loanapp.components.FieldRow
import com.aubank.loanapp.components.LabelLineValue
import com.aubank.loanapp.data.ApprovalRequest
import com.aubank.loanapp.data.Constants
import com.aubank.loanapp.data.Litigation
import com.aubank.loanapp.data.MasterData
import com.aubank.loanapp.data.PendingApprovalFeedbackData
import com.aubank.loanapp.ui.theme.LoanAppTheme
import com.aubank.loanapp.viewmodel.FeedbackApprovalFormViewModel
import com.aubank.loanapp.viewmodel.NavigationEvent
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
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
        val masterData = (application as LoanApplication).getMasterData()!!
        viewModel.setValuesFromMasterData(masterData)
        setContent {
            LoanAppTheme {
                FeedbackApprovalForm(
                    modifier = Modifier.fillMaxSize(),
                    app = viewModel.feedbackData.value?.pendingApprovalFeedbackData!!,
                    viewModel = viewModel,
                    masterData = masterData,
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

        viewModel.postFeedbackApprovalDataApiState.observe(this)
        { event ->
            when (event) {
                is FeedbackApprovalFormViewModel.PostApprovalFeedbackDataApiState.Success -> {
                    viewModel.isLoading = false
                    Toast.makeText(this, "Feedback Approved.", Toast.LENGTH_LONG).show()
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
                                 masterData: MasterData,
                                 loading: Boolean) {
    Scaffold(topBar = {
        EnhancedTopAppBar (
            titleText = app.loanNo ?: "",
            onNavigateBack = {
                viewModel.navigateBack()
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
            masterData,
            loading
        )
    })
}

@Composable
private fun ApprovalFeedbackFormWithLoading(
    app: PendingApprovalFeedbackData,
    modifier: Modifier,
    viewModel: FeedbackApprovalFormViewModel,
    masterData: MasterData,
    loading: Boolean) {
    Box(modifier = modifier.fillMaxSize()) {
        ApprovalFeedbackForm(modifier = Modifier.fillMaxSize(), viewModel = viewModel, masterData)
        if (loading) {
            ApiProgressBar(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun ApprovalFeedbackForm(
    modifier: Modifier,
    viewModel: FeedbackApprovalFormViewModel,
    masterData: MasterData
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
                LabelLineValue(label = "Visit Type", value = pendingApprovalFeedbackData.visitType ?: "New Visit")
                Spacer(modifier = Modifier.height(16.dp))
                feedbackData.visitDoneValue?.let { LabelLineValue(label = "Visit Done", value = it) }
                Spacer(modifier = Modifier.height(16.dp))
                feedbackData.relationValue?.let { LabelLineValue(label = "Relation Between Borrower and Coborrower", value = it) }
                Spacer(modifier = Modifier.height(16.dp))
                feedbackData.loanTypeValue?.let { LabelLineValue(label = "Type Of Loan", value = it) }
                Spacer(modifier = Modifier.height(16.dp))
                feedbackData.vehicleStatusValue?.let { LabelLineValue(label = "Vehicle Status", value = it) }
                Spacer(modifier = Modifier.height(16.dp))
                pendingApprovalFeedbackData.nameWithMeet?.let { LabelLineValue(label = "Name & Contact No. of Person Met During Visit", value = it) }
                Spacer(modifier = Modifier.height(16.dp))
                LabelLineValue(label = "Borrower/Family Currently Living at Given Address", value = if (pendingApprovalFeedbackData.borrowerLivingCurrentAddress) "Yes" else "No")
                Spacer(modifier = Modifier.height(16.dp))
                pendingApprovalFeedbackData.landMark?.let { LabelLineValue(label = "Landmark of New / Existing Address", value = it) }
                Spacer(modifier = Modifier.height(16.dp))
                pendingApprovalFeedbackData.newAddressOfBorrower?.let { LabelLineValue(label = "New Address of Borrower", value = it) }
                Spacer(modifier = Modifier.height(16.dp))
                pendingApprovalFeedbackData.currentContactNoOfBorrower?.let { LabelLineValue(label = "Current Contact No. of Borrower", value = it) }
                Spacer(modifier = Modifier.height(16.dp))
                pendingApprovalFeedbackData.borrowerJobAddress?.let { LabelLineValue(label = "Borrower current Job Address if any", value = it) }
                Spacer(modifier = Modifier.height(16.dp))
                feedbackData.jobValue?.let { LabelLineValue(label = "Borrower Current Job Profile", value = it) }
                Spacer(modifier = Modifier.height(16.dp))
                feedbackData.financialConditionValue?.let { LabelLineValue(label = "Borrower Financial Condition", value = it) }
                Spacer(modifier = Modifier.height(16.dp))
                LabelLineValue(label = "Is This Case Workable or Non-Workable", value = if (pendingApprovalFeedbackData.workableNonWorkable) "Workable" else "Non-Workable")
                Spacer(modifier = Modifier.height(16.dp))
                pendingApprovalFeedbackData.reasonforWorkable?.let { LabelLineValue(label = "Reasons for Workable or Non-Workable (Strength or Weakness)", value = it) }
                Spacer(modifier = Modifier.height(16.dp))
                feedbackData.litigationValue?.let { LabelLineValue(label = "Current litigation", value = it) }
                Spacer(modifier = Modifier.height(16.dp))
                feedbackData.newLitigationValue?.let { LabelLineValue(label = "New litigation Required other Then Running", value = it) }
                Spacer(modifier = Modifier.height(16.dp))
                pendingApprovalFeedbackData.newLitigationReuired?.let { LabelLineValue(label = "Why New Litigation Required", value = it) }
                Spacer(modifier = Modifier.height(16.dp))
                pendingApprovalFeedbackData.anySettlementProposal?.let { LabelLineValue(label = "Any Settlement Proposal or PTP with Date", value = it) }

                Spacer(modifier = Modifier.height(16.dp))
                if (masterData.litigations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    DropdownSpinner(
                        "New litigation Required other Then Running",
                        masterData.litigations
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