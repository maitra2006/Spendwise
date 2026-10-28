package com.example.spendwise.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val photoUrl: String = "",
    val defaultCurrency: String = "USD"
)

data class Group(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val currency: String = "USD",
    val memberIds: List<String> = emptyList(),
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

enum class SplitType {
    EQUAL, EXACT, PERCENTAGE, SHARES
}

enum class ExpenseCategory(val displayName: String) {
    FOOD("Food & Drink"),
    RENT("Rent"),
    UTILITIES("Utilities"),
    TRANSPORT("Transport"),
    ENTERTAINMENT("Entertainment"),
    SHOPPING("Shopping"),
    OTHER("Other")
}

data class Expense(
    val id: String = "",
    val groupId: String = "", // empty if non-group (friend to friend)
    val title: String = "",
    val amount: Double = 0.0,
    val currency: String = "USD",
    val paidById: String = "",
    val splitType: SplitType = SplitType.EQUAL,
    val splits: Map<String, Double> = emptyMap(), // userId -> exact amount, percentage, or shares (or empty for equal)
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class Settlement(
    val id: String = "",
    val groupId: String = "",
    val paidById: String = "",
    val receivedById: String = "",
    val amount: Double = 0.0,
    val currency: String = "USD",
    val createdAt: Long = System.currentTimeMillis()
)

data class ActivityItem(
    val id: String = "",
    val type: String = "EXPENSE", // EXPENSE, SETTLEMENT, GROUP
    val title: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val currency: String = "USD",
    val userId: String = "",
    val groupId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
