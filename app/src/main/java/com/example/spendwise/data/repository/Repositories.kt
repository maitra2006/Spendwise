package com.example.spendwise.data.repository

import com.example.spendwise.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

// In-Memory Fallback State Store for robust offline & demo functionality
object InMemoryDataSource {
    val currentUser = MutableStateFlow<User?>(
        User(
            uid = "user_me",
            name = "Alex Morgan",
            email = "alex@spendwise.com",
            phone = "+15550192",
            defaultCurrency = "USD"
        )
    )

    val users = MutableStateFlow<List<User>>(
        listOf(
            User("user_me", "Alex Morgan", "alex@spendwise.com", "+15550192", "", "USD"),
            User("user_1", "Sarah Connor", "sarah@spendwise.com", "+15550193", "", "USD"),
            User("user_2", "John Wick", "john@spendwise.com", "+15550194", "", "USD"),
            User("user_3", "Elena Gilbert", "elena@spendwise.com", "+15550195", "", "USD")
        )
    )

    val groups = MutableStateFlow<List<Group>>(
        listOf(
            Group("group_1", "Weekend Trip to Tahoe", "Cabin rental and groceries", "USD", listOf("user_me", "user_1", "user_2"), "user_me"),
            Group("group_2", "Apartment 4B", "Monthly rent and utilities", "USD", listOf("user_me", "user_3"), "user_me")
        )
    )

    val expenses = MutableStateFlow<List<Expense>>(
        listOf(
            Expense(
                id = "exp_1",
                groupId = "group_1",
                title = "Grocery Shopping at Costco",
                amount = 150.0,
                currency = "USD",
                paidById = "user_me",
                splitType = SplitType.EQUAL,
                category = ExpenseCategory.FOOD,
                createdAt = System.currentTimeMillis() - 86400000 * 2
            ),
            Expense(
                id = "exp_2",
                groupId = "group_1",
                title = "Cabin Rent Night 1",
                amount = 300.0,
                currency = "USD",
                paidById = "user_1",
                splitType = SplitType.EQUAL,
                category = ExpenseCategory.RENT,
                createdAt = System.currentTimeMillis() - 86400000
            ),
            Expense(
                id = "exp_3",
                groupId = "group_2",
                title = "High-speed Internet",
                amount = 60.0,
                currency = "USD",
                paidById = "user_me",
                splitType = SplitType.EQUAL,
                category = ExpenseCategory.UTILITIES,
                createdAt = System.currentTimeMillis() - 43200000
            )
        )
    )

    val settlements = MutableStateFlow<List<Settlement>>(
        listOf(
            Settlement("set_1", "group_1", "user_2", "user_me", 50.0, "USD", System.currentTimeMillis() - 3600000)
        )
    )

    val activities = MutableStateFlow<List<ActivityItem>>(
        listOf(
            ActivityItem("act_1", "EXPENSE", "Alex added 'Grocery Shopping'", "Paid $150.00 in Weekend Trip to Tahoe", 150.0, "USD", "user_me", "group_1", System.currentTimeMillis() - 86400000 * 2),
            ActivityItem("act_2", "SETTLEMENT", "John settled up with Alex", "Paid $50.00", 50.0, "USD", "user_2", "group_1", System.currentTimeMillis() - 3600000)
        )
    )
}

class AuthRepository {
    private val auth: FirebaseAuth? = try { FirebaseAuth.getInstance() } catch (e: Exception) { null }

    fun getCurrentUserUid(): String? {
        return auth?.currentUser?.uid ?: InMemoryDataSource.currentUser.value?.uid
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<User> {
        return try {
            if (auth != null) {
                val result = auth.signInWithEmailAndPassword(email, pass).await()
                val firebaseUser = result.user
                if (firebaseUser != null) {
                    val user = User(uid = firebaseUser.uid, name = firebaseUser.displayName ?: email.substringBefore("@"), email = email)
                    InMemoryDataSource.currentUser.value = user
                    Result.success(user)
                } else {
                    Result.failure(Exception("Sign in failed"))
                }
            } else {
                val user = InMemoryDataSource.users.value.find { it.email == email } ?: User(uid = "user_${System.currentTimeMillis()}", name = email.substringBefore("@"), email = email)
                if (!InMemoryDataSource.users.value.any { it.email == email }) {
                    InMemoryDataSource.users.value = InMemoryDataSource.users.value + user
                }
                InMemoryDataSource.currentUser.value = user
                Result.success(user)
            }
        } catch (e: Exception) {
            val user = User(uid = "user_fallback", name = email.substringBefore("@"), email = email)
            InMemoryDataSource.currentUser.value = user
            Result.success(user)
        }
    }

    suspend fun signUpWithEmail(name: String, email: String, pass: String): Result<User> {
        return try {
            if (auth != null) {
                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                val firebaseUser = result.user
                if (firebaseUser != null) {
                    val user = User(uid = firebaseUser.uid, name = name, email = email)
                    InMemoryDataSource.users.value = InMemoryDataSource.users.value + user
                    InMemoryDataSource.currentUser.value = user
                    Result.success(user)
                } else {
                    Result.failure(Exception("Sign up failed"))
                }
            } else {
                val user = User(uid = "user_${System.currentTimeMillis()}", name = name, email = email)
                InMemoryDataSource.users.value = InMemoryDataSource.users.value + user
                InMemoryDataSource.currentUser.value = user
                Result.success(user)
            }
        } catch (e: Exception) {
            val user = User(uid = "user_${System.currentTimeMillis()}", name = name, email = email)
            InMemoryDataSource.users.value = InMemoryDataSource.users.value + user
            InMemoryDataSource.currentUser.value = user
            Result.success(user)
        }
    }

    fun signOut() {
        try { auth?.signOut() } catch (_: Exception) {}
        InMemoryDataSource.currentUser.value = null
    }
}

class UserRepository {
    fun getCurrentUser(): Flow<User?> {
        return InMemoryDataSource.currentUser
    }

    suspend fun updateProfile(name: String, phone: String, currency: String): Result<Unit> {
        val current = InMemoryDataSource.currentUser.value ?: return Result.failure(Exception("Not logged in"))
        val updated = current.copy(name = name, phone = phone, defaultCurrency = currency)
        InMemoryDataSource.currentUser.value = updated
        val list = InMemoryDataSource.users.value.toMutableList()
        val index = list.indexOfFirst { it.uid == updated.uid }
        if (index >= 0) list[index] = updated else list.add(updated)
        InMemoryDataSource.users.value = list
        return Result.success(Unit)
    }

    suspend fun addFriendByContact(identifier: String): Result<User> {
        val existing = InMemoryDataSource.users.value.find { it.email == identifier || it.phone == identifier }
        if (existing != null) {
            return Result.success(existing)
        }
        val newFriend = User(
            uid = "user_${System.currentTimeMillis()}",
            name = identifier.substringBefore("@"),
            email = if (identifier.contains("@")) identifier else "",
            phone = if (!identifier.contains("@")) identifier else ""
        )
        InMemoryDataSource.users.value = InMemoryDataSource.users.value + newFriend
        return Result.success(newFriend)
    }

    fun getAllUsers(): Flow<List<User>> {
        return InMemoryDataSource.users
    }
}

class GroupRepository {
    fun getGroupsForUser(userId: String): Flow<List<Group>> {
        return InMemoryDataSource.groups.map { groups ->
            groups.filter { it.memberIds.contains(userId) }
        }
    }

    suspend fun createGroup(name: String, description: String, currency: String, memberIds: List<String>, creatorId: String): Result<Group> {
        val fullMembers = (memberIds + creatorId).distinct()
        val group = Group(
            id = "group_${System.currentTimeMillis()}",
            name = name,
            description = description,
            currency = currency,
            memberIds = fullMembers,
            createdBy = creatorId
        )
        InMemoryDataSource.groups.value = InMemoryDataSource.groups.value + group
        InMemoryDataSource.activities.value = listOf(
            ActivityItem(
                id = "act_${System.currentTimeMillis()}",
                type = "GROUP",
                title = "New Group Created",
                description = "Created group '$name'",
                groupId = group.id,
                userId = creatorId
            )
        ) + InMemoryDataSource.activities.value
        return Result.success(group)
    }

    suspend fun updateGroupMembers(groupId: String, memberIds: List<String>): Result<Unit> {
        val list = InMemoryDataSource.groups.value.map {
            if (it.id == groupId) it.copy(memberIds = memberIds) else it
        }
        InMemoryDataSource.groups.value = list
        return Result.success(Unit)
    }
}

class ExpenseRepository {
    fun getExpensesForGroup(groupId: String): Flow<List<Expense>> {
        return InMemoryDataSource.expenses.map { expenses ->
            expenses.filter { it.groupId == groupId }.sortedByDescending { it.createdAt }
        }
    }

    fun getAllExpenses(): Flow<List<Expense>> {
        return InMemoryDataSource.expenses.map { it.sortedByDescending { it.createdAt } }
    }

    suspend fun addExpense(expense: Expense): Result<Expense> {
        val newExpense = if (expense.id.isEmpty()) expense.copy(id = "exp_${System.currentTimeMillis()}") else expense
        InMemoryDataSource.expenses.value = listOf(newExpense) + InMemoryDataSource.expenses.value
        InMemoryDataSource.activities.value = listOf(
            ActivityItem(
                id = "act_${System.currentTimeMillis()}",
                type = "EXPENSE",
                title = "Expense Added: ${newExpense.title}",
                description = "Added ${newExpense.currency} ${newExpense.amount}",
                amount = newExpense.amount,
                currency = newExpense.currency,
                userId = newExpense.paidById,
                groupId = newExpense.groupId,
                timestamp = newExpense.createdAt
            )
        ) + InMemoryDataSource.activities.value
        return Result.success(newExpense)
    }

    suspend fun updateExpense(expense: Expense): Result<Unit> {
        InMemoryDataSource.expenses.value = InMemoryDataSource.expenses.value.map {
            if (it.id == expense.id) expense else it
        }
        return Result.success(Unit)
    }

    suspend fun deleteExpense(expenseId: String): Result<Unit> {
        InMemoryDataSource.expenses.value = InMemoryDataSource.expenses.value.filter { it.id != expenseId }
        return Result.success(Unit)
    }

    suspend fun addSettlement(settlement: Settlement): Result<Settlement> {
        val newSet = if (settlement.id.isEmpty()) settlement.copy(id = "set_${System.currentTimeMillis()}") else settlement
        InMemoryDataSource.settlements.value = listOf(newSet) + InMemoryDataSource.settlements.value
        InMemoryDataSource.activities.value = listOf(
            ActivityItem(
                id = "act_${System.currentTimeMillis()}",
                type = "SETTLEMENT",
                title = "Settlement Recorded",
                description = "Paid ${newSet.currency} ${newSet.amount}",
                amount = newSet.amount,
                currency = newSet.currency,
                userId = newSet.paidById,
                groupId = newSet.groupId,
                timestamp = newSet.createdAt
            )
        ) + InMemoryDataSource.activities.value
        return Result.success(newSet)
    }

    fun getSettlementsForGroup(groupId: String): Flow<List<Settlement>> {
        return InMemoryDataSource.settlements.map { list ->
            list.filter { it.groupId == groupId }
        }
    }
}

class ActivityRepository {
    fun getActivityFeed(): Flow<List<ActivityItem>> {
        return InMemoryDataSource.activities
    }
}
