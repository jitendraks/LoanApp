package com.aubank.loanapp.components

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aubank.loanapp.LoanDetailsActivity
import com.aubank.loanapp.data.Constants
import com.aubank.loanapp.data.MasterData
import com.aubank.loanapp.data.PendingApp
import com.aubank.loanapp.data.LoginResponse
import com.aubank.loanapp.data.VisitDone
import com.aubank.loanapp.viewmodel.AssignedAppsViewModel

@Composable
fun LoanItemRow(
    item: PendingApp,
    userData: LoginResponse,
    masterData: MasterData,
    viewModel: AssignedAppsViewModel
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
                viewModel.fetchLastFeedbackData(item, it.visitDoneId)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }

    // Improved card layout
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp), // More vertical spacing
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable {
                    val intent = Intent(context, LoanDetailsActivity::class.java)
                    intent.putExtra(Constants.LOAN_APP, item)
                    intent.putExtra(Constants.USER_DATA, userData)
                    context.startActivity(intent)
                }
        ) {
            // Top: Borrower's Name + Case No
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.borrowerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Case #${item.caseNo}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Middle: Loan Amount & POS (stacked for clarity)
            Text(
                text = "Loan Amount: ₹${item.loanAmount}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            item.posAfterSale?.let {
                Text(
                    text = "POS After Sale: ₹$it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(
                    onClick = { showDialog = true }
                ) {
                    Text("Submit Feedback")
                }
            }
        }
    }
}
