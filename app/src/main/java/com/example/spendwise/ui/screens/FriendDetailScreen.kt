package com.example.spendwise.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendwise.data.model.Expense
import com.example.spendwise.data.model.User
import com.example.spendwise.data.repository.*
import com.example.spendwise.domain.CalculationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendDetailScreen(
    friendId: String,
    userRepository: UserRepository,
    expenseRepository: ExpenseRepository,
    onBack: () -> Unit,
    onSettleUp: (String) -> Unit
) {
    val users by userRepository.getAllUsers().collectAsState(initial = emptyList())
    val friend = users.find { it.uid == friendId } ?: User(uid = friendId, name = "Friend")
    val allExpenses by expenseRepository.getAllExpenses().collectAsState(initial = emptyList())

    val mutualExpenses = expensesWithFriend(allExpenses, "user_me", friendId)
    val netBalance = remember(mutualExpenses) {
        var bal = 0.0
        for (exp in mutualExpenses) {
            if (exp.paidById == "user_me") {
                bal += (exp.amount / 2.0)
            } else if (exp.paidById == friendId) {
                bal -= (exp.amount / 2.0)
            }
        }
        bal
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(friend.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Button(
                        onClick = { onSettleUp(friendId) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Settle Up with ${friend.name}")
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Overall Balance with ${friend.name}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    val balanceText = when {
                        netBalance > 0.01 -> "You are owed USD ${String.format("%.2f", netBalance)}"
                        netBalance < -0.01 -> "You owe USD ${String.format("%.2f", kotlin.math.abs(netBalance))}"
                        else -> "Everything is settled up!"
                    }
                    val color = when {
                        netBalance > 0.01 -> MaterialTheme.colorScheme.secondary
                        netBalance < -0.01 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Text(text = balanceText, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
                }
            }

            if (mutualExpenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No direct expenses with ${friend.name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(mutualExpenses) { expense ->
                        val payer = users.find { it.uid == expense.paidById }?.name ?: "Someone"
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = expense.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = "Paid by $payer", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    }
}

fun expensesWithFriend(expenses: List<Expense>, myUid: String, friendId: String): List<Expense> {
    return expenses.filter { exp ->
        exp.paidById == myUid || exp.paidById == friendId
    }
}
