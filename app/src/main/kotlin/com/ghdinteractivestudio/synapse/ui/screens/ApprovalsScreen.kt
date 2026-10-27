package com.ghdinteractivestudio.synapse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.synapse.domain.model.ApprovalRequest
import com.ghdinteractivestudio.synapse.domain.model.ApprovalStatus
import com.ghdinteractivestudio.synapse.ui.components.ApprovalCardItem
import com.ghdinteractivestudio.synapse.ui.components.EmptyStateView
import com.ghdinteractivestudio.synapse.ui.components.PageHeader
import com.ghdinteractivestudio.synapse.ui.theme.Palette
import com.ghdinteractivestudio.synapse.ui.theme.Type
import com.ghdinteractivestudio.synapse.ui.theme.bold

@Composable
fun ApprovalsScreen(
    approvals: List<ApprovalRequest>,
    onRespond: (String, ApprovalStatus, Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.background)
            .statusBarsPadding()
    ) {
        PageHeader(
            title = "Approbations (${approvals.size})",
            pop = onNavigateBack
        )

        if (approvals.isEmpty()) {
            EmptyStateView(
                title = "Aucune approbation en attente",
                description = "Toutes les actions sensibles demandées par vos agents ont été traitées.",
                symbolName = "checkmark.circle.fill"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(approvals, key = { it.id }) { req ->
                    ApprovalCardItem(
                        request = req,
                        onApprove = { always -> onRespond(req.id, ApprovalStatus.APPROVED, always) },
                        onReject = { onRespond(req.id, ApprovalStatus.REJECTED, false) }
                    )
                }
            }
        }
    }
}
