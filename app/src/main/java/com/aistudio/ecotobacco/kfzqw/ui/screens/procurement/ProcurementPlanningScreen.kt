package com.aistudio.ecotobacco.kfzqw.ui.screens.procurement

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProcurementPlanWithItems
import com.aistudio.ecotobacco.kfzqw.data.utils.LocalScanner
import com.aistudio.ecotobacco.kfzqw.viewmodel.TobaccoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcurementPlanningScreen(
    viewModel: TobaccoViewModel,
    scannedResult: LocalScanner.ScannedResult? = null,
    onNavigateBack: () -> Unit
) {
    val plans by viewModel.procurementPlans.collectAsStateWithLifecycle()
    var showAddEditDialog by remember { mutableStateOf(false) }
    var planToEdit by remember { mutableStateOf<ProcurementPlanWithItems?>(null) }
    
    // Gunakan ID untuk melacak plan yang sedang dibuka agar datanya selalu sinkron (real-time)
    var selectedPlanId by remember { mutableStateOf<Int?>(null) }
    val selectedPlanForDetail = remember(selectedPlanId, plans) {
        plans.find { it.plan.id == selectedPlanId }
    }
    
    val context = LocalContext.current

    // Handle scanned result to open dialog automatically
    LaunchedEffect(scannedResult) {
        if (scannedResult != null) {
            planToEdit = null
            showAddEditDialog = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Rencana Kulakan", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.navigationBarsPadding(),
                onClick = { planToEdit = null; showAddEditDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, "Tambah") },
                text = { Text("Buat Rencana", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        if (plans.isEmpty()) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inventory, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    Spacer(Modifier.height(16.dp))
                    Text("Belum ada rencana kulakan", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(plans, key = { it.plan.id }) { planWithItems ->
                    PlanItemCard(
                        planWithItems = planWithItems,
                        onClick = { selectedPlanId = planWithItems.plan.id },
                        onEdit = { planToEdit = planWithItems; showAddEditDialog = true },
                        onDelete = { viewModel.deleteProcurementPlan(planWithItems.plan) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (showAddEditDialog) {
        AddEditPlanDialog(
            viewModel = viewModel,
            planToEdit = planToEdit,
            initialScannedResult = scannedResult,
            onDismiss = { showAddEditDialog = false; planToEdit = null },
            onConfirm = { date, supplier, items ->
                if (planToEdit != null) viewModel.updateProcurementPlan(planToEdit!!.plan.copy(date = date, supplierName = supplier), items)
                else viewModel.addProcurementPlan(date, supplier, items)
                showAddEditDialog = false; planToEdit = null
            }
        )
    }

    selectedPlanForDetail?.let { plan ->
        PlanDetailDialog(
            plan = plan,
            viewModel = viewModel,
            onDismiss = { selectedPlanId = null },
            onToggle = { viewModel.toggleProcurementItemStatus(it) },
            onRemoveItem = { viewModel.removeItemFromPlan(it) },
            onShare = { planToShare, exportType, note ->
                val shareText = generateShareText(planToShare, exportType, note)
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Bagikan Rencana Kulakan")
                context.startActivity(shareIntent)
            },
            onShareImage = { planToShare ->
                sharePlanAsImage(context, planToShare)
            }
        )
    }
}
