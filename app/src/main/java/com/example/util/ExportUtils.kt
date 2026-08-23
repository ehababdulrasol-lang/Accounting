package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.Account
import com.example.data.TrialBalanceReportRow
import com.example.ui.viewmodel.AccountStatementRow
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {

    // BOM (Byte Order Mark) for UTF-8. Crucial for MS Excel to identify Arabic characters!
    private const val UTF8_BOM = "\uFEFF"

    /**
     * Escapes a cell value for standard CSV compatibility.
     */
    private fun escapeCsv(cell: String): String {
        val clean = cell.replace("\n", " ").replace("\r", " ").trim()
        if (clean.contains(",") || clean.contains("\"") || clean.contains(";")) {
            return "\"" + clean.replace("\"", "\"\"") + "\""
        }
        return clean
    }

    /**
     * Generates a beautifully structured Arabic/English CSV string for Trial Balance rows.
     */
    fun generateTrialBalanceCsv(
        rows: List<TrialBalanceReportRow>,
        lang: String
    ): String {
        val isAr = lang == "ar"
        val sb = java.lang.StringBuilder()
        sb.append(UTF8_BOM)

        // Title and Meta Information
        val title = if (isAr) "تقرير ميزان المراجعة" else "Trial Balance Report"
        sb.append("${escapeCsv(title)}\n")
        sb.append("${if (isAr) "تاريخ التصدير" else "Export Date"},${escapeCsv(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))}\n\n")

        // Headers
        val headers = if (isAr) {
            listOf("رمز الحساب", "اسم الحساب", "نوع الحساب", "المدين الافتتاحي", "الدائن الافتتاحي", "مدين الفترة", "دائن الفترة", "المدين الختامي", "الدائن الختامي")
        } else {
            listOf("Account Code", "Account Name", "Account Type", "Opening Debit", "Opening Credit", "Period Debit", "Period Credit", "Closing Debit", "Closing Credit")
        }
        sb.append(headers.joinToString(",") { escapeCsv(it) }).append("\n")

        var totalOpDr = 0L
        var totalOpCr = 0L
        var totalPerDr = 0L
        var totalPerCr = 0L
        var totalClDr = 0L
        var totalClCr = 0L

        // Rows
        rows.forEach { row ->
            totalOpDr += row.openingDebit
            totalOpCr += row.openingCredit
            totalPerDr += row.periodDebit
            totalPerCr += row.periodCredit
            totalClDr += row.closingDebit
            totalClCr += row.closingCredit

            val typeText = if (isAr) {
                when (row.accountType.name) {
                    "ASSET" -> "أصول"
                    "LIABILITY" -> "التزامات"
                    "EQUITY" -> "حقوق ملكية"
                    "REVENUE" -> "إيرادات"
                    "EXPENSE" -> "مصروفات"
                    else -> row.accountType.name
                }
            } else {
                row.accountType.name
            }

            val cells = listOf(
                row.accountCode,
                row.accountName,
                typeText,
                FinancialUtils.formatBase(row.openingDebit),
                FinancialUtils.formatBase(row.openingCredit),
                FinancialUtils.formatBase(row.periodDebit),
                FinancialUtils.formatBase(row.periodCredit),
                FinancialUtils.formatBase(row.closingDebit),
                FinancialUtils.formatBase(row.closingCredit)
            )
            sb.append(cells.joinToString(",") { escapeCsv(it) }).append("\n")
        }

        // Totals Row
        val totalLabel = if (isAr) "الإجمالي الكلي" else "Total General Ledger Sum"
        val totalCells = listOf(
            "",
            totalLabel,
            "",
            FinancialUtils.formatBase(totalOpDr),
            FinancialUtils.formatBase(totalOpCr),
            FinancialUtils.formatBase(totalPerDr),
            FinancialUtils.formatBase(totalPerCr),
            FinancialUtils.formatBase(totalClDr),
            FinancialUtils.formatBase(totalClCr)
        )
        sb.append(totalCells.joinToString(",") { escapeCsv(it) }).append("\n")

        return sb.toString()
    }

    /**
     * Generates a beautifully structured Arabic/English CSV string for Account Ledger Statement rows.
     */
    fun generateAccountStatementCsv(
        account: Account,
        rows: List<AccountStatementRow>,
        lang: String
    ): String {
        val isAr = lang == "ar"
        val sb = java.lang.StringBuilder()
        sb.append(UTF8_BOM)

        // Header Metadata
        val reportTitle = if (isAr) "كشف حساب تفصيلي" else "Detailed Accounting Sub-Ledger Statement"
        sb.append("${escapeCsv(reportTitle)}\n")
        sb.append("${if (isAr) "الحساب" else "Ledger Account"},${escapeCsv("${account.accountCode} - ${account.name}")}\n")
        sb.append("${if (isAr) "تاريخ التصدير" else "Export Date"},${escapeCsv(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))}\n\n")

        // Headers
        val headers = if (isAr) {
            listOf("التاريخ", "رقم القيد", "البيان والملخص", "المدين (Dr)", "الدائن (Cr)", "الرصيد الجاري")
        } else {
            listOf("Date & Time", "Vchr No", "Narration Memo", "Debit (Dr)", "Credit (Cr)", "Running Balance")
        }
        sb.append(headers.joinToString(",") { escapeCsv(it) }).append("\n")

        var totalDebit = 0L
        var totalCredit = 0L

        // Rows
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        rows.forEach { row ->
            totalDebit += row.debit
            totalCredit += row.credit

            val cells = listOf(
                sdf.format(Date(row.date)),
                row.voucherNo,
                row.memo,
                if (row.debit > 0) FinancialUtils.formatBase(row.debit) else "0.00",
                if (row.credit > 0) FinancialUtils.formatBase(row.credit) else "0.00",
                FinancialUtils.formatBase(row.runningBalance)
            )
            sb.append(cells.joinToString(",") { escapeCsv(it) }).append("\n")
        }

        // Totals Row
        val totalLabel = if (isAr) "المجموع" else "Summarized Totals"
        val closingBal = rows.lastOrNull()?.runningBalance ?: 0L
        val totalCells = listOf(
            "",
            "",
            totalLabel,
            FinancialUtils.formatBase(totalDebit),
            FinancialUtils.formatBase(totalCredit),
            FinancialUtils.formatBase(closingBal)
        )
        sb.append(totalCells.joinToString(",") { escapeCsv(it) }).append("\n")

        return sb.toString()
    }

    /**
     * Utility method to write the generated CSV data directly to the user's selected URI/File descriptor.
     */
    fun writeCsvToUri(context: Context, uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(content)
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
