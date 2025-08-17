package com.aubank.loanapp.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aubank.loanapp.data.LoginResponse
import com.aubank.loanapp.data.MasterData
import com.aubank.loanapp.viewmodel.AssignedAppsViewModel

@Composable
fun PendingAppsList(
    modifier: Modifier = Modifier,
    viewModel: AssignedAppsViewModel,
    userData: LoginResponse,
    masterData: MasterData,
    loading: Boolean
) {
    val items by viewModel.filteredResults.observeAsState(emptyList())

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = items,
                key = { it.caseNo },
                contentType = { "loan_item" }
            ) { item ->
                LoanItemRow(item, userData, masterData, viewModel)
            }
        }

        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
