package com.example.spendwise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.spendwise.data.repository.*
import com.example.spendwise.ui.navigation.SpendWiseNavGraph
import com.example.spendwise.ui.theme.SpendWiseTheme

class MainActivity : ComponentActivity() {
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()
    private val groupRepository = GroupRepository()
    private val expenseRepository = ExpenseRepository()
    private val activityRepository = ActivityRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpendWiseTheme {
                SpendWiseNavGraph(
                    authRepository = authRepository,
                    userRepository = userRepository,
                    groupRepository = groupRepository,
                    expenseRepository = expenseRepository,
                    activityRepository = activityRepository
                )
            }
        }
    }
}
