package com.example.spendwise.domain

import com.example.spendwise.data.model.Expense
import com.example.spendwise.data.model.Settlement
import com.example.spendwise.data.model.SplitType
import kotlin.math.abs
import kotlin.math.round

data class Debt(
    val fromUserId: String,
    val toUserId: String,
    val amount: Double,
    val currency: String
)

object CalculationUtils {

    // Exchange rates relative to USD
    private val exchangeRates = mapOf(
        "USD" to 1.0,
        "EUR" to 0.92,
        "GBP" to 0.79,
        "INR" to 83.0,
        "CAD" to 1.35,
        "AUD" to 1.52,
        "JPY" to 150.0
    )

    fun convertCurrency(amount: Double, fromCurrency: String, toCurrency: String): Double {
        if (fromCurrency == toCurrency) return amount
        val fromRate = exchangeRates[fromCurrency] ?: 1.0
        val toRate = exchangeRates[toCurrency] ?: 1.0
        val amountInUsd = amount / fromRate
        return round((amountInUsd * toRate) * 100.0) / 100.0
    }

    fun calculateSplits(
        totalAmount: Double,
        splitType: SplitType,
        memberIds: List<String>,
        rawValues: Map<String, Double>
    ): Map<String, Double> {
        if (memberIds.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, Double>()
        when (splitType) {
            SplitType.EQUAL -> {
                val share = round((totalAmount / memberIds.size) * 100.0) / 100.0
                var remainder = totalAmount - (share * memberIds.size)
                memberIds.forEachIndexed { index, id ->
                    var amt = share
                    if (index == 0) {
                        amt += round(remainder * 100.0) / 100.0
                    }
                    result[id] = round(amt * 100.0) / 100.0
                }
            }
            SplitType.EXACT -> {
                memberIds.forEach { id ->
                    result[id] = round((rawValues[id] ?: 0.0) * 100.0) / 100.0
                }
            }
            SplitType.PERCENTAGE -> {
                memberIds.forEach { id ->
                    val pct = rawValues[id] ?: 0.0
                    val amt = (totalAmount * pct) / 100.0
                    result[id] = round(amt * 100.0) / 100.0
                }
            }
            SplitType.SHARES -> {
                val totalShares = memberIds.sumOf { rawValues[it] ?: 0.0 }
                if (totalShares > 0) {
                    memberIds.forEach { id ->
                        val shares = rawValues[id] ?: 0.0
                        val amt = (totalAmount * shares) / totalShares
                        result[id] = round(amt * 100.0) / 100.0
                    }
                } else {
                    val share = totalAmount / memberIds.size
                    memberIds.forEach { result[it] = share }
                }
            }
        }
        return result
    }

    // Calculates net balances for each user: positive = is owed money, negative = owes money
    fun calculateNetBalances(
        memberIds: List<String>,
        expenses: List<Expense>,
        settlements: List<Settlement>,
        targetCurrency: String = "USD"
    ): Map<String, Double> {
        val balances = memberIds.associateWith { 0.0 }.toMutableMap()

        for (exp in expenses) {
            val convertedAmount = convertCurrency(exp.amount, exp.currency, targetCurrency)
            val paidBy = exp.paidById
            if (balances.containsKey(paidBy)) {
                balances[paidBy] = balances[paidBy]!! + convertedAmount
            } else {
                balances[paidBy] = convertedAmount
            }

            val splits = calculateSplits(exp.amount, exp.splitType, memberIds.filter { exp.splits.containsKey(it) || exp.splitType == SplitType.EQUAL }, exp.splits)
            // If explicit splits map is empty or partial, compute default equal for memberIds
            val effectiveSplits = if (splits.isNotEmpty()) splits else {
                val eq = exp.amount / memberIds.size
                memberIds.associateWith { eq }
            }

            for ((userId, splitAmount) in effectiveSplits) {
                val convertedSplit = convertCurrency(splitAmount, exp.currency, targetCurrency)
                if (balances.containsKey(userId)) {
                    balances[userId] = balances[userId]!! - convertedSplit
                } else {
                    balances[userId] = -convertedSplit
                }
            }
        }

        for (set in settlements) {
            val convertedAmount = convertCurrency(set.amount, set.currency, targetCurrency)
            // paidById paid receivedById, so paidById balance increases (debt paid off), receivedById decreases
            if (balances.containsKey(set.paidById)) {
                balances[set.paidById] = balances[set.paidById]!! + convertedAmount
            } else {
                balances[set.paidById] = convertedAmount
            }
            if (balances.containsKey(set.receivedById)) {
                balances[set.receivedById] = balances[set.receivedById]!! - convertedAmount
            } else {
                balances[set.receivedById] = -convertedAmount
            }
        }

        return balances.mapValues { round(it.value * 100.0) / 100.0 }
    }

    // Debt simplification algorithm (minimizing transactions)
    fun simplifyDebts(netBalances: Map<String, Double>, currency: String = "USD"): List<Debt> {
        val debtors = netBalances.filter { it.value < -0.01 }.toMutableMap()
        val creditors = netBalances.filter { it.value > 0.01 }.toMutableMap()

        val debts = mutableListOf<Debt>()

        val debtorList = debtors.entries.map { mutableMapOf(it.key to it.value) }.toMutableList()
        val creditorList = creditors.entries.map { mutableMapOf(it.key to it.value) }.toMutableList()

        // Simple greedy matching
        var dIdx = 0
        var cIdx = 0

        while (dIdx < debtorList.size && cIdx < creditorList.size) {
            val debtorEntry = debtorList[dIdx].entries.first()
            val creditorEntry = creditorList[cIdx].entries.first()

            val debtorId = debtorEntry.key
            val debtorAmount = debtorEntry.value // negative

            val creditorId = creditorEntry.key
            val creditorAmount = creditorEntry.value // positive

            val settleAmount = minOf(abs(debtorAmount), creditorAmount)
            val roundedSettle = round(settleAmount * 100.0) / 100.0

            if (roundedSettle > 0.01) {
                debts.add(Debt(fromUserId = debtorId, toUserId = creditorId, amount = roundedSettle, currency = currency))
            }

            val newDebtorAmount = debtorAmount + settleAmount
            val newCreditorAmount = creditorAmount - settleAmount

            debtorList[dIdx][debtorId] = newDebtorAmount
            creditorList[cIdx][creditorId] = newCreditorAmount

            if (abs(newDebtorAmount) < 0.01) dIdx++
            if (abs(newCreditorAmount) < 0.01) cIdx++
        }

        return debts
    }
}
