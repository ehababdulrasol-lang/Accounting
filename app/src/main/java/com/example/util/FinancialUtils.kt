package com.example.util

import com.example.data.VoucherHeader
import com.example.data.VoucherLine
import java.text.DecimalFormat
import kotlin.math.pow
import kotlin.math.roundToLong

object FinancialUtils {

    const val BASE_SCALE_FACTOR = 1_000_000.0
    const val BASE_SCALE = 1_000_000L

    fun getPowerOfTen(decimalPlaces: Int): Double {
        return 10.0.pow(decimalPlaces)
    }

    /**
     * Converts raw double amount entered by user (e.g. 10.500) into scaled Long
     * based on currency decimal places.
     */
    fun doubleToOriginalLong(amount: Double, decimalPlaces: Int): Long {
        return (amount * getPowerOfTen(decimalPlaces)).roundToLong()
    }

    /**
     * Converts scaled Long of original currency to its double representation.
     */
    fun originalLongToDouble(amount: Long, decimalPlaces: Int): Double {
        return amount.toDouble() / getPowerOfTen(decimalPlaces)
    }

    /**
     * Converts original amount in original currency to base currency scaled Long.
     */
    fun toBaseAmount(originalAmount: Long, decimalPlaces: Int, exchangeRate: Double): Long {
        val originalDouble = originalLongToDouble(originalAmount, decimalPlaces)
        val baseDouble = originalDouble * exchangeRate
        return (baseDouble * BASE_SCALE_FACTOR).roundToLong()
    }

    /**
     * Format scaled Long to custom string for original currency.
     */
    fun formatOriginal(amount: Long, decimalPlaces: Int): String {
        val doubleVal = originalLongToDouble(amount, decimalPlaces)
        val pattern = when (decimalPlaces) {
            0 -> "#,##0"
            1 -> "#,##0.0"
            2 -> "#,##0.00"
            3 -> "#,##0.000"
            else -> "#,##0.0000"
        }
        return DecimalFormat(pattern).format(doubleVal)
    }

    /**
     * Format base currency scaled Long.
     */
    fun formatBase(amountBase: Long): String {
        val doubleVal = amountBase.toDouble() / BASE_SCALE_FACTOR
        // Default base is LYD with 3 decimal places
        return DecimalFormat("#,##0.000").format(doubleVal)
    }
}

class VoucherValidationEngine {

    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }

    fun validateVoucher(header: VoucherHeader, lines: List<VoucherLine>): ValidationResult {
        if (lines.size < 2) {
            return ValidationResult.Error("Voucher must have at least two line entries.")
        }

        // Filter valid lines (either debit > 0 or credit > 0)
        val nonZeroLines = lines.filter { it.debit > 0 || it.credit > 0 }
        if (nonZeroLines.size < 2) {
            return ValidationResult.Error("Voucher must have at least two entries with non-zero amounts.")
        }

        val totalDebitBase = lines.sumOf { if (it.debit > 0) it.amountBase else 0L }
        val totalCreditBase = lines.sumOf { if (it.credit > 0) it.amountBase else 0L }

        // Precision check: Tolerance is 10 micro-units (0.00001)
        val difference = Math.abs(totalDebitBase - totalCreditBase)
        val tolerance = 10L // micro-units

        if (difference > tolerance) {
            val formattedDebit = FinancialUtils.formatBase(totalDebitBase)
            val formattedCredit = FinancialUtils.formatBase(totalCreditBase)
            return ValidationResult.Error("Double-entry balance mismatch! Debits ($formattedDebit) != Credits ($formattedCredit). Difference is ${FinancialUtils.formatBase(difference)} in base currency.")
        }

        // Validate currency conversion matches (originalAmount * exchangeRate)
        for (i in lines.indices) {
            val line = lines[i]
            val originalAmount = if (line.debit > 0L) line.debit else line.credit
            if (originalAmount <= 0) {
                return ValidationResult.Error("Line item #${i + 1} amount must be greater than zero.")
            }
            
            // Expected base calculation
            val expectedBase = FinancialUtils.toBaseAmount(originalAmount, if (line.currencyId == 1L) 3 else 2, line.exchangeRate)
            val calculatedDiff = Math.abs(line.amountBase - expectedBase)
            if (calculatedDiff > 100L) { // Allow slight rounding difference for exchange rate multiply
                return ValidationResult.Error("Currency conversion mismatch on Line item #${i + 1}.")
            }
        }

        return ValidationResult.Success
    }
}
