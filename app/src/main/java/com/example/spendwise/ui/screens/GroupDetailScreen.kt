package com.example.spendwise.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendwise.data.model.Expense
import com.example.spendwise.data.model.Group
import com.example.spendwise.data.repository.*
import com.example.spendwise.domain.CalculationUtils
import com.example.spendwise.domain.Debt
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: String,
    groupRepository: GroupRepository,
    expenseRepository: ExpenseRepository,
    userRepository: UserRepository,
    onBack: () -> Unit,
    onAddExpense: (String) -> Unit,
    onSettleUp: (String) -> Unit
) {
    val groups by groupRepository.getGroupsForUser("user_me").collectAsState(initial = emptyList())
    val group = groups.find { it.id == groupId } ?: Group(id = groupId, name = "Group Detail")
    val expenses by expenseRepository.getExpensesForGroup(groupId).collectAsState(initial = emptyList())
    val settlements by expenseRepository.getSettlementsForGroup(groupId).collectAsState(initial = emptyList())
    val users by userRepository.getAllUsers().collectAsState(initial = emptyList())

    var selectedTab by remember { mutableStateOf(0) } // 0: Expenses, 1: Balances, 2: Simplified Debts
    val netBalances = remember(group, expenses, settlements) {
        CalculationUtils.calculateNetBalances(group.memberIds, expenses, settlements, group.currency)
    }
    val simplifiedDebts = remember(netBalances, group) {
        CalculationUtils.simplifyDebts(netBalances, group.currency)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { onAddExpense(groupId) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Expense")
                    }
                    OutlinedButton(
                        onClick = { onSettleUp(groupId) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Settle Up")
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Expenses") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Balances") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Simplified") })
            }

            when (selectedTab) {
                0 -> {
                    if (expenses.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No expenses in this group yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(expenses) { expense ->
                                val payer = users.find { it.uid == expense.paidById }?.name ?: "Someone"
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = expense.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(text = "Paid by $payer • ${expense.category.displayName}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text(
                                            text = "${expense.currency} ${String.format("%.2f", expense.amount)}",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(group.memberIds) { memberId ->
                            val user = users.find { it.uid == memberId }
                            val balance = netBalances[memberId] ?: 0.0
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = user?.name ?: memberId, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                    val balanceText = when {
                                        balance > 0.01 -> "gets back ${group.currency} ${String.format("%.2f", balance)}"
                                        balance < -0.01 -> "owes ${group.currency} ${String.format("%.2f", abs(balance))}"
                                        else -> "settled up"
                                    }
                                    val color = when {
                                        balance > 0.01 -> MaterialTheme.colorScheme.secondary
                                        balance < -0.01 -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Text(text = balanceText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
                                }
                            }
                        }
                    }
                }
                2 -> {
                    if (simplifiedDebts.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("All debts are settled up!", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(simplifiedDebts) { debt ->
                                val fromUser = users.find { it.uid == debt.fromUserId }?.name ?: "Someone"
                                val toUser = users.find { it.uid == debt.toUserId }?.name ?: "Someone"
                                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "$fromUser pays $toUser", fontSize = 16.sp, modifier = Modifier.weight(1f))
                                        Text(
                                            text = "${debt.currency} ${String.format("%.2f", debt.amount)}",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
