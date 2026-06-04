package com.example.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.*
import com.example.ui.Localization
import com.example.ui.viewmodel.AccountStatementRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PrintUtils {

    private fun getHeaderBrand(context: Context, isAr: Boolean): Triple<String, String, String> {
        val prefs = context.getSharedPreferences("ledger_settings", Context.MODE_PRIVATE)
        val orgAr = prefs.getString("org_name_ar", "المؤسسة الليبية للتدقيق المالي") ?: "المؤسسة الليبية للتدقيق المالي"
        val orgEn = prefs.getString("org_name_en", "Libyan Financial Ledger Pro") ?: "Libyan Financial Ledger Pro"
        val prAr = prefs.getString("print_details_ar", "") ?: ""
        val prEn = prefs.getString("print_details_en", "") ?: ""
        val logoText = prefs.getString("logo_config", "🕌") ?: "🕌"
        val logoImageUri = prefs.getString("logo_image_uri", "") ?: ""

        val currentOrgName = if (isAr) orgAr else orgEn
        val defaultPrintDetails = if (isAr) "إدارة الشؤون والتدقيق المالي العام" else "General Ledger Finance & Auditing Dept."
        val currentPrintDetails = if (isAr) {
            if (prAr.isNotEmpty()) prAr else defaultPrintDetails
        } else {
            if (prEn.isNotEmpty()) prEn else defaultPrintDetails
        }

        val brandLogo = if (logoImageUri.isNotEmpty()) {
            try {
                val file = java.io.File(logoImageUri)
                if (file.exists()) {
                    val bytes = file.readBytes()
                    val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    "<img src=\"data:image/png;base64,$base64\" style=\"max-height:46px; max-width:120px; vertical-align:middle; object-fit:contain;\" />"
                } else {
                    logoText
                }
            } catch (e: Exception) {
                logoText
            }
        } else {
            logoText
        }

        return Triple(currentOrgName, currentPrintDetails, brandLogo)
    }

    private fun getHtmlTemplate(body: String, isRtl: Boolean): String {
        val dir = if (isRtl) "rtl" else "ltr"
        val textAlign = if (isRtl) "right" else "left"
        return """
            <!DOCTYPE html>
            <html dir="$dir" lang="${if (isRtl) "ar" else "en"}">
            <head>
                <meta charset="UTF-8">
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        color: #333;
                        margin: 20px;
                        direction: $dir;
                        line-height: 1.4;
                    }
                    .header-container {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        border-bottom: 2px solid #5a1ec0;
                        padding-bottom: 15px;
                        margin-bottom: 20px;
                    }
                    .header-title {
                        font-size: 24px;
                        font-weight: bold;
                        color: #5a1ec0;
                    }
                    .header-meta {
                        font-size: 11px;
                        text-align: right;
                        color: #666;
                    }
                    .info-grid {
                        display: table;
                        width: 100%;
                        margin-bottom: 20px;
                        background: #fcfcfc;
                        border: 1px solid #e0e0e0;
                        border-collapse: collapse;
                    }
                    .info-row {
                        display: table-row;
                    }
                    .info-cell-label {
                        display: table-cell;
                        padding: 8px 12px;
                        font-weight: bold;
                        background-color: #f1f0f5;
                        width: 20%;
                        border: 1px solid #dcdcdc;
                        font-size: 13px;
                    }
                    .info-cell-value {
                        display: table-cell;
                        padding: 8px 12px;
                        border: 1px solid #dcdcdc;
                        font-size: 13px;
                    }
                    table.report-table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-top: 15px;
                        font-size: 12px;
                    }
                    table.report-table th {
                        background-color: #5a1ec0;
                        color: white;
                        text-align: $textAlign;
                        padding: 10px;
                        font-weight: bold;
                        border: 1px solid #4815a1;
                    }
                    table.report-table td {
                        padding: 10px;
                        border-bottom: 1px solid #e0e0e0;
                        border-left: 1px solid #f1f0f5;
                        border-right: 1px solid #f1f0f5;
                    }
                    table.report-table tr:nth-child(even) {
                        background-color: #f9f9fc;
                    }
                    .text-right {
                        text-align: right !important;
                    }
                    .text-center {
                        text-align: center !important;
                    }
                    .total-badge {
                        background-color: #e2f7eb;
                        color: #0d8343;
                        font-size: 12px;
                        font-weight: bold;
                        padding: 3px 8px;
                        border-radius: 4px;
                        display: inline-block;
                    }
                    .total-badge.red {
                        background-color: #fcebeb;
                        color: #c5221f;
                    }
                    .footer-signatures {
                        margin-top: 50px;
                        width: 100%;
                        display: table;
                    }
                    .sig-block {
                        display: table-cell;
                        width: 33.3%;
                        text-align: center;
                        font-size: 13px;
                    }
                    .sig-line {
                        margin-top: 40px;
                        border-top: 1px dotted #888;
                        width: 70%;
                        display: inline-block;
                    }
                    .badge-posted {
                        background-color: #0d8343;
                        color: white;
                        padding: 2px 6px;
                        border-radius: 3px;
                        font-size: 11px;
                        font-weight: bold;
                    }
                    .badge-draft {
                        background-color: #e09b11;
                        color: white;
                        padding: 2px 6px;
                        border-radius: 3px;
                        font-size: 11px;
                        font-weight: bold;
                    }
                </style>
            </head>
            <body>
                $body
            </body>
            </html>
        """.trimIndent()
    }

    private fun printHtml(context: Context, htmlContent: String, jobName: String) {
        try {
            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    try {
                        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                        val printAdapter = webView.createPrintDocumentAdapter(jobName)
                        val printAttributes = PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .build()
                        printManager.print(jobName, printAdapter, printAttributes)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        android.widget.Toast.makeText(context, "Printing error: " + e.message, android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        } catch (e: Throwable) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "WebView/Printing not supported on this device: " + e.message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    // Prints Voucher Details (Journal/Receipt/Payment Voucher)
    fun printVoucher(
        context: Context,
        voucher: VoucherHeader,
        lines: List<VoucherLine>,
        allAccounts: List<Account>,
        currencies: List<Currency>,
        lang: String
    ) {
        val isAr = lang == "ar"
        val (brandOrg, brandDetails, brandLogo) = getHeaderBrand(context, isAr)
        
        val title = when (voucher.type) {
            VoucherType.JOURNAL -> if (isAr) "قيد تسوية يومية دفتري" else "Journal Entry Voucher"
            VoucherType.RECEIPT -> if (isAr) "سند قبض مالي نقدية" else "Receipt Cash Voucher"
            VoucherType.PAYMENT -> if (isAr) "سند صرف مالي نقدية" else "Payment Cash Voucher"
        }

        val typeText = when (voucher.type) {
            VoucherType.JOURNAL -> if (isAr) "تسوية يومية" else "Journal Entry"
            VoucherType.RECEIPT -> if (isAr) "سند قبض" else "Receipt Voucher"
            VoucherType.PAYMENT -> if (isAr) "سند صرف" else "Payment Voucher"
        }

        val signLabelReceiver = if (isAr) "المستلم / المسلّم" else "Payee / Receiver"
        val signLabelAccountant = if (isAr) "منشئ القيد / المحاسب" else "Prepared By (Accountant)"
        val signLabelAuditor = if (isAr) "التوقيع والاعتماد والختم" else "Audited & Approved"

        val statusText = if (voucher.isPosted) {
            if (isAr) "<span class='badge-posted'>مرحل ومرحّل</span>" else "<span class='badge-posted'>POSTED</span>"
        } else {
            if (isAr) "<span class='badge-draft'>مسودة قيد</span>" else "<span class='badge-draft'>DRAFT</span>"
        }

        val rowsBuilder = StringBuilder()
        var totalDrBase = 0L
        var totalCrBase = 0L

        lines.forEach { line ->
            val acc = allAccounts.find { it.id == line.accountId }
            val accName = acc?.let { Localization.getAccountName(it.accountCode, it.name, lang) } ?: "Unassigned Account"
            val currency = currencies.find { it.id == line.currencyId } ?: Currency(code = "LYD", name = "LYD", decimalPlaces = 3)

            val drOrig = if (line.debit > 0) FinancialUtils.formatOriginal(line.debit, currency.decimalPlaces) else ""
            val crOrig = if (line.credit > 0) FinancialUtils.formatOriginal(line.credit, currency.decimalPlaces) else ""
            
            val drBase = if (line.debit > 0) {
                totalDrBase += line.amountBase
                FinancialUtils.formatBase(line.amountBase)
            } else ""

            val crBase = if (line.credit > 0) {
                totalCrBase += line.amountBase
                FinancialUtils.formatBase(line.amountBase)
            } else ""

            val detailsText = line.memo ?: ""
            val rateStr = if (line.currencyId == 1L) "-" else line.exchangeRate.toString()

            rowsBuilder.append("""
                <tr>
                    <td>${acc?.accountCode ?: "-"}</td>
                    <td><strong>$accName</strong></td>
                    <td>$detailsText</td>
                    <td class="text-right">$drOrig ${currency.code}</td>
                    <td class="text-right">$crOrig ${currency.code}</td>
                    <td class="text-right">$rateStr</td>
                    <td class="text-right"><strong>$drBase</strong></td>
                    <td class="text-right"><strong>$crBase</strong></td>
                </tr>
            """.trimIndent())
        }

        val body = """
            <div class="header-container" style="display:flex; justify-content:space-between; align-items:center;">
                <div style="display:flex; align-items:center; gap:12px;">
                    <span style="font-size:36px; line-height:1;">$brandLogo</span>
                    <div>
                        <div class="header-title">$title</div>
                        <div style="font-size:13px; color:#555; margin-top:5px;">
                            $brandDetails
                        </div>
                    </div>
                </div>
                <div class="header-meta">
                    <strong>$brandOrg</strong><br>
                    ${if (isAr) "تاريخ الطباعة:" else "Printed:"} ${formatDate(System.currentTimeMillis())}
                </div>
            </div>

            <div class="info-grid">
                <div class="info-row">
                    <div class="info-cell-label">${if (isAr) "رقم السند" else "Voucher No"}</div>
                    <div class="info-cell-value"><strong>${voucher.voucherNo}</strong></div>
                    <div class="info-cell-label">${if (isAr) "تاريخ التسجيل" else "Voucher Date"}</div>
                    <div class="info-cell-value">${formatDate(voucher.date)}</div>
                </div>
                <div class="info-row">
                    <div class="info-cell-label">${if (isAr) "تصنيف السند" else "Classification"}</div>
                    <div class="info-cell-value">$typeText</div>
                    <div class="info-cell-label">${if (isAr) "الحالة المالية" else "Status"}</div>
                    <div class="info-cell-value">$statusText</div>
                </div>
                <div class="info-row">
                    <div class="info-cell-label">${if (isAr) "بيان السند العام" else "Narration Memo"}</div>
                    <div class="info-cell-value" colspan="3">${voucher.description}</div>
                </div>
            </div>

            <table class="report-table">
                <thead>
                    <tr>
                        <th style="width: 12%;">${if (isAr) "رمز الحساب" else "Code"}</th>
                        <th style="width: 23%;">${if (isAr) "الحساب المالي" else "Account"}</th>
                        <th style="width: 18%;">${if (isAr) "البيان / الملاحظة" else "Row Memo"}</th>
                        <th style="width: 11%;" class="text-right">${if (isAr) "مدين (أصلي)" else "Debit (Orig)"}</th>
                        <th style="width: 11%;" class="text-right">${if (isAr) "دائن (أصلي)" else "Credit (Orig)"}</th>
                        <th style="width: 7%;" class="text-right">${if (isAr) "الصرف" else "Rate"}</th>
                        <th style="width: 14%;" class="text-right">${if (isAr) "مدين (معادل د.ل)" else "Debit (Base)"}</th>
                        <th style="width: 14%;" class="text-right">${if (isAr) "دائن (معادل د.ل)" else "Credit (Base)"}</th>
                    </tr>
                </thead>
                <tbody>
                    ${rowsBuilder.toString()}
                    <tr style="background-color: #f1f0f5; font-weight: bold;">
                        <td colspan="6">${if (isAr) "إجمالي السند المحاسبي المطابق" else "TOTAL BALANCED SUM"}</td>
                        <td class="text-right text-success" style="color:#0d8343;">${FinancialUtils.formatBase(totalDrBase)}</td>
                        <td class="text-right text-success" style="color:#0d8343;">${FinancialUtils.formatBase(totalCrBase)}</td>
                    </tr>
                </tbody>
            </table>

            <div class="footer-signatures">
                <div class="sig-block">
                    <strong>$signLabelAccountant</strong>
                    <div class="sig-line"></div>
                </div>
                <div class="sig-block">
                    <strong>$signLabelReceiver</strong>
                    <div class="sig-line"></div>
                </div>
                <div class="sig-block">
                    <strong>$signLabelAuditor</strong>
                    <div class="sig-line"></div>
                </div>
            </div>
            
            <div style="margin-top:40px; text-align: center; font-size: 10px; color: #999; border-top: 1px solid #eee; padding-top: 10px;">
                ${if (isAr) "إن هذا السند معتمد وتلقائي وصادر بموجب لوائح ومعايير ميزان التدقيق المالي الموحد." else "This voucher is certified, automated, and compiled under standard compliant dual ledger criteria."}
            </div>
        """.trimIndent()

        printHtml(context, getHtmlTemplate(body, isAr), "${voucher.voucherNo}_print")
    }

    // Prints Account Statement ( كشف الحساب )
    fun printAccountStatement(
        context: Context,
        account: Account,
        rows: List<AccountStatementRow>,
        lang: String
    ) {
        val isAr = lang == "ar"
        val (brandOrg, brandDetails, brandLogo) = getHeaderBrand(context, isAr)
        
        val title = if (isAr) "دفتر الأستاذ المساعد - كشف حساب مالي تفصيلي" else "Sub-Ledger Account Statement Report"
        
        val totalDebit = rows.sumOf { it.debit }
        val totalCredit = rows.sumOf { it.credit }
        val finalBalance = rows.lastOrNull()?.runningBalance ?: 0L

        val rowsBuilder = StringBuilder()
        rows.forEach { row ->
            rowsBuilder.append("""
                <tr>
                    <td>${formatDate(row.date)}</td>
                    <td><strong>${row.voucherNo}</strong></td>
                    <td>${row.memo}</td>
                    <td class="text-right">${if (row.debit > 0) FinancialUtils.formatBase(row.debit) else "-"}</td>
                    <td class="text-right">${if (row.credit > 0) FinancialUtils.formatBase(row.credit) else "-"}</td>
                    <td class="text-right" style="font-weight: bold; color: ${if (row.runningBalance >= 0) "#1b5e20" else "#b71c1c"};">
                        ${FinancialUtils.formatBase(row.runningBalance)}
                    </td>
                </tr>
            """.trimIndent())
        }

        val body = """
            <div class="header-container" style="display:flex; justify-content:space-between; align-items:center;">
                <div style="display:flex; align-items:center; gap:12px;">
                    <span style="font-size:36px; line-height:1;">$brandLogo</span>
                    <div>
                        <div class="header-title">$title</div>
                        <div style="font-size:13px; color:#555; margin-top:5px;">
                            $brandDetails
                        </div>
                    </div>
                </div>
                <div class="header-meta">
                    <strong>$brandOrg</strong><br>
                    ${if (isAr) "تاريخ الاستخراج:" else "Generated:"} ${formatDate(System.currentTimeMillis())}
                </div>
            </div>

            <div class="info-grid">
                <div class="info-row">
                    <div class="info-cell-label">${if (isAr) "رمز الحساب" else "Account Code"}</div>
                    <div class="info-cell-value"><strong>${account.accountCode}</strong></div>
                    <div class="info-cell-label">${if (isAr) "اسم الحساب المالي" else "Account Name"}</div>
                    <div class="info-cell-value"><strong>${Localization.getAccountName(account.accountCode, account.name, lang)}</strong></div>
                </div>
                <div class="info-row">
                    <div class="info-cell-label">${if (isAr) "نوع حساب الدليل" else "Account Type"}</div>
                    <div class="info-cell-value">${account.accountType.name}</div>
                    <div class="info-cell-label">${if (isAr) "المعيار الحسابي" else "Verification Standards"}</div>
                    <div class="info-cell-value">${if (isAr) "المعايير الدولية لإعداد التقارير المالية (IFRS)" else "IFRS Standard Sub-Ledger"}</div>
                </div>
            </div>

            <table class="report-table">
                <thead>
                    <tr>
                        <th style="width: 18%;">${if (isAr) "التاريخ والوقت" else "Date & Time"}</th>
                        <th style="width: 15%;">${if (isAr) "رقم السند/القيد" else "Voucher No"}</th>
                        <th style="width: 33%;">${if (isAr) "شرح وبيان القيد" else "Line Memo Narrative"}</th>
                        <th style="width: 11%;" class="text-right">${if (isAr) "مدين (د.ل)" else "Debit (Dr)"}</th>
                        <th style="width: 11%;" class="text-right">${if (isAr) "دائن (د.ل)" else "Credit (Cr)"}</th>
                        <th style="width: 12%;" class="text-right">${if (isAr) "الرصيد التراكمي د.ل" else "Running Balance"}</th>
                    </tr>
                </thead>
                <tbody>
                    ${if (rows.isEmpty()) "<tr><td colspan='6' class='text-center' style='padding:20px; color:#666;'>" + (if (isAr) "لا توجد أي قيود أو معاملات مرحلة مسجلة بدفتر الأستاذ لهذا الحساب حالياً." else "No posted ledger transactions found for this account.") + "</td></tr>" else rowsBuilder.toString()}
                    
                    <tr style="background-color: #f1f0f5; font-weight: bold;">
                        <td colspan="3">${if (isAr) "إجمالي حركات وحصيلة كشف الحساب" else "STATEMENT PERIOD TOTAL SUMMARY"}</td>
                        <td class="text-right" style="color:#0d8343;">${FinancialUtils.formatBase(totalDebit)}</td>
                        <td class="text-right" style="color:#b71c1c;">${FinancialUtils.formatBase(totalCredit)}</td>
                        <td class="text-right" style="background-color:#e2f7eb; color: ${if (finalBalance >= 0) "#0d8343" else "#c5221f"};">
                            ${FinancialUtils.formatBase(finalBalance)}
                        </td>
                    </tr>
                </tbody>
            </table>

            <div class="footer-signatures" style="margin-top:60px;">
                <div class="sig-block">
                    <strong>${if (isAr) "المطابقة والتدقيق بواسطة" else "Audited & Verified By"}</strong>
                    <div class="sig-line"></div>
                </div>
                <div class="sig-block">
                    <strong>&nbsp;</strong>
                </div>
                <div class="sig-block">
                    <strong>${if (isAr) "توقيع رئيس الشؤون المالية" else "Chief Accountant Seal / Signature"}</strong>
                    <div class="sig-line"></div>
                </div>
            </div>
        """.trimIndent()

        printHtml(context, getHtmlTemplate(body, isAr), "${account.accountCode}_statement_print")
    }

    // Prints Trial Balance ( ميزان المراجعة )
    fun printTrialBalance(
        context: Context,
        rows: List<TrialBalanceReportRow>,
        totalOpDr: Long,
        totalOpCr: Long,
        totalPerDr: Long,
        totalPerCr: Long,
        totalClDr: Long,
        totalClCr: Long,
        lang: String
    ) {
        val isAr = lang == "ar"
        val (brandOrg, brandDetails, brandLogo) = getHeaderBrand(context, isAr)
        
        val title = if (isAr) "ميزان المراجعة الموحد بالأرصدة والحركات" else "Consolidated Audit Trial Balance Report"

        val rowsBuilder = StringBuilder()
        rows.forEach { row ->
            rowsBuilder.append("""
                <tr>
                    <td><code>${row.accountCode}</code></td>
                    <td><strong>${Localization.getAccountName(row.accountCode, row.accountName, lang)}</strong></td>
                    <td class="text-right">${if (row.openingDebit > 0) FinancialUtils.formatBase(row.openingDebit) else "-"}</td>
                    <td class="text-right">${if (row.openingCredit > 0) FinancialUtils.formatBase(row.openingCredit) else "-"}</td>
                    <td class="text-right">${if (row.periodDebit > 0) FinancialUtils.formatBase(row.periodDebit) else "-"}</td>
                    <td class="text-right">${if (row.periodCredit > 0) FinancialUtils.formatBase(row.periodCredit) else "-"}</td>
                    <td class="text-right" style="font-weight:bold;">${if (row.closingDebit > 0) FinancialUtils.formatBase(row.closingDebit) else "-"}</td>
                    <td class="text-right" style="font-weight:bold;">${if (row.closingCredit > 0) FinancialUtils.formatBase(row.closingCredit) else "-"}</td>
                </tr>
            """.trimIndent())
        }

        val body = """
            <div class="header-container" style="display:flex; justify-content:space-between; align-items:center;">
                <div style="display:flex; align-items:center; gap:12px;">
                    <span style="font-size:36px; line-height:1;">$brandLogo</span>
                    <div>
                        <div class="header-title">$title</div>
                        <div style="font-size:13px; color:#555; margin-top:5px;">
                            $brandDetails
                        </div>
                    </div>
                </div>
                <div class="header-meta">
                    <strong>$brandOrg</strong><br>
                    ${if (isAr) "تاريخ التصدير:" else "Exported Date:"} ${formatDate(System.currentTimeMillis())}
                </div>
            </div>

            <table class="report-table">
                <thead>
                    <tr>
                        <th style="width: 10%;">${if (isAr) "الرمز" else "Code"}</th>
                        <th style="width: 30%;">${if (isAr) "اسم الحساب المحاسبي" else "Account Component Name"}</th>
                        <th style="width: 10%;" class="text-right">${if (isAr) "افتتاحي مدين" else "Open Dr"}</th>
                        <th style="width: 10%;" class="text-right">${if (isAr) "افتتاحي دائن" else "Open Cr"}</th>
                        <th style="width: 10%;" class="text-right">${if (isAr) "حركة مدين" else "Period Dr"}</th>
                        <th style="width: 10%;" class="text-right">${if (isAr) "حركة دائن" else "Period Cr"}</th>
                        <th style="width: 10%;" class="text-right">${if (isAr) "مدين نهائي" else "Close Dr"}</th>
                        <th style="width: 10%;" class="text-right">${if (isAr) "دائن نهائي" else "Close Cr"}</th>
                    </tr>
                </thead>
                <tbody>
                    $rowsBuilder
                    <tr style="background-color: #f1f0f5; font-weight: bold; font-size: 13px;">
                        <td colspan="2">${if (isAr) "المجموع المالي المتطابق وميزان المطابقة" else "GRAND TOTAL VERIFIED BALANCES"}</td>
                        <td class="text-right">${FinancialUtils.formatBase(totalOpDr)}</td>
                        <td class="text-right">${FinancialUtils.formatBase(totalOpCr)}</td>
                        <td class="text-right">${FinancialUtils.formatBase(totalPerDr)}</td>
                        <td class="text-right">${FinancialUtils.formatBase(totalPerCr)}</td>
                        <td class="text-right" style="color: #0d8343;">${FinancialUtils.formatBase(totalClDr)}</td>
                        <td class="text-right" style="color: #0d8343;">${FinancialUtils.formatBase(totalClCr)}</td>
                    </tr>
                </tbody>
            </table>

            <div style="margin-top: 30px; background: #e2f7eb; color: #0d8343; padding: 12px; border-radius: 4px; font-weight: bold; text-align: center; font-size: 12px;">
                ${if (isAr) "✓ مطابقة الحصيلة: تم مراجعة القوانين والمعايير المزدوجة المتكافئة، وميزان المراجعة متوازن ولا توجد فروقات." else "✓ Compliance OK: Double-entry totals are mathematically identical. The unadjusted trial balance has zero gap."}
            </div>
            
            <div class="footer-signatures" style="margin-top:50px;">
                <div class="sig-block">
                    <strong>${if (isAr) "أعد بواسطة" else "Prepared By"}</strong>
                    <div class="sig-line"></div>
                </div>
                <div class="sig-block">
                    <strong>${if (isAr) "المراجع القانوني" else "Financial Auditor Sign"}</strong>
                    <div class="sig-line"></div>
                </div>
                <div class="sig-block">
                    <strong>${if (isAr) "الاعتماد النهائي للتصدير" else "Management Signoff"}</strong>
                    <div class="sig-line"></div>
                </div>
            </div>
        """.trimIndent()

        printHtml(context, getHtmlTemplate(body, isAr), "trial_balance_print")
    }

    // Prints Balance Sheet ( الميزانية العمومية )
    fun printBalanceSheet(
        context: Context,
        totalAssets: Long,
        totalLiabilities: Long,
        totalEquity: Long,
        assetAccounts: List<Account>,
        liabilityAccounts: List<Account>,
        equityAccounts: List<Account>,
        snapshots: List<AccountBalanceSnapshot>,
        lang: String
    ) {
        val isAr = lang == "ar"
        val (brandOrg, brandDetails, brandLogo) = getHeaderBrand(context, isAr)
        
        val title = if (isAr) "الميزانية العمومية والمركز المالي للمؤسسة" else "Consolidated Statement of Financial Position (Balance Sheet)"

        val assetRows = StringBuilder()
        assetAccounts.forEach { acc ->
            val bal = snapshots.find { it.accountId == acc.id }?.balance ?: 0L
            assetRows.append("""
                <tr>
                    <td><code>${acc.accountCode}</code></td>
                    <td>${Localization.getAccountName(acc.accountCode, acc.name, lang)}</td>
                    <td class="text-right">${FinancialUtils.formatBase(bal)}</td>
                </tr>
            """.trimIndent())
        }

        val liabilityRows = StringBuilder()
        liabilityAccounts.forEach { acc ->
            val bal = snapshots.find { it.accountId == acc.id }?.balance ?: 0L
            liabilityRows.append("""
                <tr>
                    <td><code>${acc.accountCode}</code></td>
                    <td>${Localization.getAccountName(acc.accountCode, acc.name, lang)}</td>
                    <td class="text-right">${FinancialUtils.formatBase(-bal)}</td>
                </tr>
            """.trimIndent())
        }

        val equityRows = StringBuilder()
        equityAccounts.forEach { acc ->
            val bal = snapshots.find { it.accountId == acc.id }?.balance ?: 0L
            equityRows.append("""
                <tr>
                    <td><code>${acc.accountCode}</code></td>
                    <td>${Localization.getAccountName(acc.accountCode, acc.name, lang)}</td>
                    <td class="text-right">${FinancialUtils.formatBase(-bal)}</td>
                </tr>
            """.trimIndent())
        }

        val totalLE = totalLiabilities + totalEquity
        val isEqBalanced = Math.abs(totalAssets - totalLE) <= 10L

        val body = """
            <div class="header-container" style="display:flex; justify-content:space-between; align-items:center;">
                <div style="display:flex; align-items:center; gap:12px;">
                    <span style="font-size:36px; line-height:1;">$brandLogo</span>
                    <div>
                        <div class="header-title">$title</div>
                        <div style="font-size:13px; color:#555; margin-top:5px;">
                            $brandDetails
                        </div>
                    </div>
                </div>
                <div class="header-meta">
                    <strong>$brandOrg</strong><br>
                    ${if (isAr) "تاريخ التقرير:" else "As Of Date:"} ${formatDate(System.currentTimeMillis())}
                </div>
            </div>

            <h3 style="color:#5a1ec0; border-bottom:1px solid #ddd; padding-bottom:5px; font-size:15px;">
                ${if (isAr) "1. الأصول والمدخرات المالية" else "1. ASSET ACCOUNTS"}
            </h3>
            <table class="report-table">
                <thead>
                    <tr>
                        <th style="width: 20%;">${if (isAr) "الرمز" else "Code"}</th>
                        <th style="width: 55%;">${if (isAr) "الحساب" else "Item Detail"}</th>
                        <th style="width: 25%;" class="text-right">${if (isAr) "الرصيد المالي د.ل" else "Amount Base"}</th>
                    </tr>
                </thead>
                <tbody>
                    $assetRows
                    <tr style="background-color: #f1f0f5; font-weight: bold;">
                        <td colspan="2">${if (isAr) "إجمالي الأصول (أ)" else "Total Assets (A)"}</td>
                        <td class="text-right" style="color: #0d8343; font-size:13px;">${FinancialUtils.formatBase(totalAssets)}</td>
                    </tr>
                </tbody>
            </table>

            <h3 style="color:#5a1ec0; border-bottom:1px solid #ddd; padding-bottom:5px; margin-top:25px; font-size:15px;">
                ${if (isAr) "2. الالتزامات والمطلوبات" else "2. LIABILITY ACCOUNTS"}
            </h3>
            <table class="report-table">
                <thead>
                    <tr>
                        <th style="width: 20%;">${if (isAr) "الرمز" else "Code"}</th>
                        <th style="width: 55%;">${if (isAr) "الحساب" else "Item Detail"}</th>
                        <th style="width: 25%;" class="text-right">${if (isAr) "الرصيد المالي د.ل" else "Amount Base"}</th>
                    </tr>
                </thead>
                <tbody>
                    $liabilityRows
                    <tr style="background-color: #f1f0f5; font-weight: bold;">
                        <td colspan="2">${if (isAr) "إجمالي الالتزامات (ل)" else "Total Liabilities (L)"}</td>
                        <td class="text-right" style="color:#c5221f; font-size:13px;">${FinancialUtils.formatBase(totalLiabilities)}</td>
                    </tr>
                </tbody>
            </table>

            <h3 style="color:#5a1ec0; border-bottom:1px solid #ddd; padding-bottom:5px; margin-top:25px; font-size:15px;">
                ${if (isAr) "3. حقوق الملكية ورأس المال" else "3. EQUITY / CAPITALS"}
            </h3>
            <table class="report-table">
                <thead>
                    <tr>
                        <th style="width: 20%;">${if (isAr) "الرمز" else "Code"}</th>
                        <th style="width: 55%;">${if (isAr) "الحساب" else "Item Detail"}</th>
                        <th style="width: 25%;" class="text-right">${if (isAr) "الرصيد المالي د.ل" else "Amount Base"}</th>
                    </tr>
                </thead>
                <tbody>
                    $equityRows
                    <tr style="background-color: #f1f0f5; font-weight: bold;">
                        <td colspan="2">${if (isAr) "إجمالي حقوق الملكية (ح)" else "Total Owner Equity (E)"}</td>
                        <td class="text-right" style="color: #5a1ec0; font-size:13px;">${FinancialUtils.formatBase(totalEquity)}</td>
                    </tr>
                </tbody>
            </table>

            <div style="margin-top: 30px; display: table; width: 100%; border: 2px solid #5a1ec0; background: #fafafc; padding: 12px; border-radius: 4px;">
                <div style="display: table-row;">
                    <div style="display: table-cell; width: 50%; font-weight: bold; padding:6px;">
                        ${if (isAr) "مجموع الأصول (أ)" else "Total Assets (A)"}: <span style="color:#0d8343;">${FinancialUtils.formatBase(totalAssets)} د.ل</span>
                    </div>
                    <div style="display: table-cell; width: 50%; font-weight: bold; padding:6px; border-left:1px solid #ddd;">
                        ${if (isAr) "مجموع الالتزامات وحقوق الملكية (ل + ح)" else "Total Liabilities & Equity (L + E)"}: <span style="color:#5a1ec0;">${FinancialUtils.formatBase(totalLE)} د.ل</span>
                    </div>
                </div>
            </div>

            <div style="margin-top: 20px; text-align: center; font-weight: bold;">
                ${if (isEqBalanced) {
                    if (isAr) "<div class='total-badge'>✓ معادلة الميزانية متوازنة تماماً وصحيحة محاسبياً</div>" 
                    else "<div class='total-badge'>✓ Balance Sheet is in perfect equilibrium. General Ledger equations match perfectly.</div>"
                } else {
                    if (isAr) "<div class='total-badge red'>⚠️ خلل طفيف/فروق في إغلاق الحسابات الزمني! يرجى الاستعلام وتدقيق قيود اليومية.</div>" 
                    else "<div class='total-badge red'>⚠️ Mismatch found in equations of period closure. Please ensure all journal postings are balanced.</div>"
                }}
            </div>
            
            <div class="footer-signatures" style="margin-top:40px;">
                <div class="sig-block">
                    <strong>${if (isAr) "أعد بواسطة المحاسب" else "Financial Officer Sign"}</strong>
                    <div class="sig-line"></div>
                </div>
                <div class="sig-block">
                    <strong>&nbsp;</strong>
                </div>
                <div class="sig-block">
                    <strong>${if (isAr) "المدير العام للمنشأة" else "Company General Seal & Dec"}</strong>
                    <div class="sig-line"></div>
                </div>
            </div>
        """.trimIndent()

        printHtml(context, getHtmlTemplate(body, isAr), "balance_sheet_print")
    }

    // Prints Income Statement ( قائمة الدخل )
    fun printIncomeStatement(
        context: Context,
        totalRevenue: Long,
        totalExpense: Long,
        netIncome: Long,
        profitMargin: Double,
        revenueAccounts: List<Account>,
        expenseAccounts: List<Account>,
        snapshots: List<AccountBalanceSnapshot>,
        isLibyan: Boolean,
        lang: String
    ) {
        val isAr = lang == "ar"
        val (brandOrg, brandDetails, brandLogo) = getHeaderBrand(context, isAr)
        
        val title = if (isAr) "قائمة الدخل والأرباح والخسائر الرسمية" else "Official Consolidated Revenue & Income Statement"

        val revRows = StringBuilder()
        revenueAccounts.forEach { acc ->
            val bal = snapshots.find { it.accountId == acc.id }?.balance ?: 0L
            revRows.append("""
                <tr>
                    <td><code>${acc.accountCode}</code></td>
                    <td>${Localization.getAccountName(acc.accountCode, acc.name, lang)}</td>
                    <td class="text-right">${FinancialUtils.formatBase(-bal)}</td>
                </tr>
            """.trimIndent())
        }

        val expRows = StringBuilder()
        expenseAccounts.forEach { acc ->
            val bal = snapshots.find { it.accountId == acc.id }?.balance ?: 0L
            expRows.append("""
                <tr>
                    <td><code>${acc.accountCode}</code></td>
                    <td>${Localization.getAccountName(acc.accountCode, acc.name, lang)}</td>
                    <td class="text-right">${FinancialUtils.formatBase(bal)}</td>
                </tr>
            """.trimIndent())
        }

        val badgeColor = if (netIncome >= 0) "total-badge" else "total-badge red"
        val resultText = if (netIncome >= 0) {
            if (isAr) "صافي ربح الفترة التشغيلية المعتمد" else "CONSOLIDATED NET REVENUE SAVINGS (PROFIT)"
        } else {
            if (isAr) "صافي عجز خسارة الفترة التشغيلية" else "NET CONSOLIDATED OPERATIONAL DEFICIT (LOSS)"
        }

        val body = """
            <div class="header-container" style="display:flex; justify-content:space-between; align-items:center;">
                <div style="display:flex; align-items:center; gap:12px;">
                    <span style="font-size:36px; line-height:1;">$brandLogo</span>
                    <div>
                        <div class="header-title">$title</div>
                        <div style="font-size:13px; color:#555; margin-top:5px;">
                            $brandDetails
                        </div>
                    </div>
                </div>
                <div class="header-meta">
                    <strong>$brandOrg</strong><br>
                    ${if (isAr) "تاريخ التصفية:" else "Issued Date:"} ${formatDate(System.currentTimeMillis())}
                </div>
            </div>

            <h3 style="color:#0d8343; border-bottom:1px solid #ddd; padding-bottom:5px; font-size:15px;">
                ${if (isAr) "1. الإيرادات والتحصيلات" else "1. REVENUES"}
            </h3>
            <table class="report-table">
                <thead>
                    <tr>
                        <th style="width: 20%;">${if (isAr) "الرمز" else "Code"}</th>
                        <th style="width: 55%;">${if (isAr) "الحساب" else "Category Item Detail"}</th>
                        <th style="width: 25%;" class="text-right">${if (isAr) "المبلغ د.ل" else "Amount Base"}</th>
                    </tr>
                </thead>
                <tbody>
                    $revRows
                    <tr style="background-color: #f1f0f5; font-weight: bold;">
                        <td colspan="2">${if (isAr) "إجمالي المقبوضات والإيرادات" else "Total Revenues"}</td>
                        <td class="text-right" style="color:#0d8343; font-size:13px;">${FinancialUtils.formatBase(totalRevenue)}</td>
                    </tr>
                </tbody>
            </table>

            <h3 style="color:#b71c1c; border-bottom:1px solid #ddd; padding-bottom:5px; margin-top:25px; font-size:15px;">
                ${if (isAr) "2. المصروفات العامة والتشغيلية" else "2. OPERATIONAL EXPENSES"}
            </h3>
            <table class="report-table">
                <thead>
                    <tr>
                        <th style="width: 20%;">${if (isAr) "الرمز" else "Code"}</th>
                        <th style="width: 55%;">${if (isAr) "الحساب" else "Operating Expense Description"}</th>
                        <th style="width: 25%;" class="text-right">${if (isAr) "المبلغ د.ل" else "Amount Base"}</th>
                    </tr>
                </thead>
                <tbody>
                    $expRows
                    <tr style="background-color: #f1f0f5; font-weight: bold;">
                        <td colspan="2">${if (isAr) "إجمالي المصاريف والمدفوعات" else "Total Operating Expenses"}</td>
                        <td class="text-right" style="color:#c5221f; font-size:13px;">${FinancialUtils.formatBase(totalExpense)}</td>
                    </tr>
                </tbody>
            </table>

            <div style="margin-top:35px; text-align:center; padding: 20px; border: 2px dashed ${if (netIncome >= 0) "#0d8343" else "#c5221f"}; background:#fbfbfb;">
                <div style="font-size:14px; font-weight:bold; color:#777; margin-bottom:8px;">$resultText</div>
                <div style="font-size:28px; font-weight:900; color: ${if (netIncome >= 0) "#0d8343" else "#c5221f"};">
                    ${if (netIncome >= 0) "+" else ""}${FinancialUtils.formatBase(netIncome)} ${if (isLibyan) "د.ل" else "LYD"}
                </div>
                <div style="font-size: 13px; color:#555; margin-top:8px;">
                    ${if (isAr) "هامش العائد والاسترداد المئوي:" else "Revenue Yield Margin percentage:"} ${String.format("%.2f", profitMargin)}%
                </div>
            </div>

            <div class="footer-signatures" style="margin-top:50px;">
                <div class="sig-block">
                    <strong>${if (isAr) "إعداد وتدقيق محاسب الشؤون" else "Audit Accountant Verification"}</strong>
                    <div class="sig-line"></div>
                </div>
                <div class="sig-block">
                    <strong>&nbsp;</strong>
                </div>
                <div class="sig-block">
                    <strong>${if (isAr) "التوقيع والاعتماد والختم الرسمي" else "Official Executive Directors Seal"}</strong>
                    <div class="sig-line"></div>
                </div>
            </div>
        """.trimIndent()

        printHtml(context, getHtmlTemplate(body, isAr), "income_statement_print")
    }

    fun printCashFlow(
        context: Context,
        statement: com.example.ui.viewmodel.CashFlowStatement,
        isLibyan: Boolean,
        lang: String
    ) {
        val isAr = lang == "ar"
        val (brandOrg, brandDetails, brandLogo) = getHeaderBrand(context, isAr)
        
        val title = if (isAr) "قائمة التدفقات النقدية الرسمية (IAS 7)" else "Official Statement of Cash Flows (IAS 7)"

        val netOperating = statement.operatingInflow - statement.operatingOutflow
        val netInvesting = statement.investingInflow - statement.investingOutflow
        val netFinancing = statement.financingInflow - statement.financingOutflow
        val netChange = netOperating + netInvesting + netFinancing

        val body = """
            <div class="header-container" style="display:flex; justify-content:space-between; align-items:center;">
                <div style="display:flex; align-items:center; gap:12px;">
                    <span style="font-size:36px; line-height:1;">$brandLogo</span>
                    <div>
                        <div class="header-title">$title</div>
                        <div style="font-size:13px; color:#555; margin-top:5px;">
                            $brandDetails
                        </div>
                    </div>
                </div>
                <div class="header-meta">
                    <strong>$brandOrg</strong><br>
                    ${if (isAr) "تاريخ الإصدار:" else "Issued Date:"} ${formatDate(System.currentTimeMillis())}
                </div>
            </div>

            <table class="report-table">
                <thead>
                    <tr>
                        <th style="width: 75%;">${if (isAr) "البيــــــــان" else "Classification / Flow Indicator"}</th>
                        <th style="width: 25%;" class="text-right">${if (isAr) "المبلغ د.ل" else "Amount (LYD)"}</th>
                    </tr>
                </thead>
                <tbody>
                    <!-- Operating Activities -->
                    <tr style="background-color: #f1f0f5; font-weight: bold;">
                        <td>${if (isAr) "1. التدفقات النقدية من الأنشطة التشغيلية" else "1. Cash Flows from Operating Activities"}</td>
                        <td></td>
                    </tr>
                    <tr>
                        <td style="padding-left: 20px;">${if (isAr) "المقبوضات النقدية من العملاء والإيرادات" else "Cash Inflows from customers & revenues"}</td>
                        <td class="text-right" style="color:#0d8343;">+${FinancialUtils.formatBase(statement.operatingInflow)}</td>
                    </tr>
                    <tr>
                        <td style="padding-left: 20px;">${if (isAr) "المدفوعات النقدية للموردين والمصاريف" else "Cash Outflows for suppliers & expenses"}</td>
                        <td class="text-right" style="color:#c5221f;">-${FinancialUtils.formatBase(statement.operatingOutflow)}</td>
                    </tr>
                    <tr style="font-weight: bold; font-style: italic;">
                        <td style="padding-left: 15px;">${if (isAr) "صافي النقد المتوفر من الأنشطة التشغيلية" else "Net Cash from Operating Activities"}</td>
                        <td class="text-right" style="color:${if (netOperating >= 0) "#0d8343" else "#c5221f"};">
                            ${if (netOperating >= 0) "+" else ""}${FinancialUtils.formatBase(netOperating)}
                        </td>
                    </tr>

                    <!-- Investing Activities -->
                    <tr style="background-color: #f1f0f5; font-weight: bold; margin-top: 15px;">
                        <td>${if (isAr) "2. التدفقات النقدية من الأنشطة الاستثمارية" else "2. Cash Flows from Investing Activities"}</td>
                        <td></td>
                    </tr>
                    <tr>
                        <td style="padding-left: 20px;">${if (isAr) "المتحصلات من بيع أصول غير متداولة" else "Inflows from sale of non-current assets"}</td>
                        <td class="text-right" style="color:#0d8343;">+${FinancialUtils.formatBase(statement.investingInflow)}</td>
                    </tr>
                    <tr>
                        <td style="padding-left: 20px;">${if (isAr) "المدفوعات لشراء أصول غير متداولة" else "Outflows for purchase of non-current assets"}</td>
                        <td class="text-right" style="color:#c5221f;">-${FinancialUtils.formatBase(statement.investingOutflow)}</td>
                    </tr>
                    <tr style="font-weight: bold; font-style: italic;">
                        <td style="padding-left: 15px;">${if (isAr) "صافي النقد المستخدم في الأنشطة الاستثمارية" else "Net Cash from Investing Activities"}</td>
                        <td class="text-right" style="color:${if (netInvesting >= 0) "#0d8343" else "#c5221f"};">
                            ${if (netInvesting >= 0) "+" else ""}${FinancialUtils.formatBase(netInvesting)}
                        </td>
                    </tr>

                    <!-- Financing Activities -->
                    <tr style="background-color: #f1f0f5; font-weight: bold; margin-top: 15px;">
                        <td>${if (isAr) "3. التدفقات النقدية من الأنشطة التمويلية" else "3. Cash Flows from Financing Activities"}</td>
                        <td></td>
                    </tr>
                    <tr>
                        <td style="padding-left: 20px;">${if (isAr) "المقبوضات من زيادة رأس المال والقروض" else "Inflows from equity increases & financing issues"}</td>
                        <td class="text-right" style="color:#0d8343;">+${FinancialUtils.formatBase(statement.financingInflow)}</td>
                    </tr>
                    <tr>
                        <td style="padding-left: 20px;">${if (isAr) "المدفوعات لتسديد القروض أو الأرباح" else "Outflows for loan settlements & dividends"}</td>
                        <td class="text-right" style="color:#c5221f;">-${FinancialUtils.formatBase(statement.financingOutflow)}</td>
                    </tr>
                    <tr style="font-weight: bold; font-style: italic;">
                        <td style="padding-left: 15px;">${if (isAr) "صافي النقد من الأنشطة التمويلية" else "Net Cash from Financing Activities"}</td>
                        <td class="text-right" style="color:${if (netFinancing >= 0) "#0d8343" else "#c5221f"};">
                            ${if (netFinancing >= 0) "+" else ""}${FinancialUtils.formatBase(netFinancing)}
                        </td>
                    </tr>

                    <!-- Summary Reconciliation -->
                    <tr style="border-top: 2px solid #5a1ec0; background-color: #f5f4fa; font-weight: bold;">
                        <td>${if (isAr) "صافي الحركة النقدية للفترة" else "Net increase/decrease in cash during period"}</td>
                        <td class="text-right" style="color:${if (netChange >= 0) "#0d8343" else "#c5221f"};">
                            ${if (netChange >= 0) "+" else ""}${FinancialUtils.formatBase(netChange)}
                        </td>
                    </tr>
                    <tr style="font-weight: bold;">
                        <td>${if (isAr) "الرصيد النقدي أول الفترة" else "Cash and cash equivalents, beginning of period"}</td>
                        <td class="text-right" style="color:#5a1ec0;">${FinancialUtils.formatBase(statement.openingBalance)}</td>
                    </tr>
                    <tr style="border-top: 2px double #5a1ec0; background-color: #e2f7eb; font-weight: bold; font-size: 13px;">
                        <td style="color: #0d8343;">${if (isAr) "الرصيد النقدي نهاية الفترة (المطابق للخزينة والبنك)" else "Cash and cash equivalents, end of period"}</td>
                        <td class="text-right" style="color: #0d8343;">${FinancialUtils.formatBase(statement.closingBalance)}</td>
                    </tr>
                </tbody>
            </table>

            <div class="footer-signatures" style="margin-top:50px;">
                <div class="sig-block">
                    <strong>${if (isAr) "إعداد وتدقيق محاسب الشؤون" else "Audit Accountant Verification"}</strong>
                    <div class="sig-line"></div>
                </div>
                <div class="sig-block">
                    <strong>&nbsp;</strong>
                </div>
                <div class="sig-block">
                    <strong>${if (isAr) "التوقيع والاعتماد والختم الرسمي" else "Official Executive Directors Seal"}</strong>
                    <div class="sig-line"></div>
                </div>
            </div>
        """.trimIndent()

        printHtml(context, getHtmlTemplate(body, isAr), "cash_flow_print")
    }

    fun printCustomersReport(
        context: Context,
        customers: List<Customer>,
        allAccounts: List<Account>,
        snapshots: List<AccountBalanceSnapshot>,
        lang: String
    ) {
        val isAr = lang == "ar"
        val (brandOrg, brandDetails, brandLogo) = getHeaderBrand(context, isAr)
        val title = if (isAr) "تقرير كشف أرصدة العملاء التفصيلي" else "Detailed Customers Balance Report"

        val rowsHtml = StringBuilder()
        var totalOutstanding = 0L
        customers.forEach { customer ->
            val linkedAcc = allAccounts.find { it.id == customer.accountId }
            val balance = snapshots.find { it.accountId == customer.accountId }?.balance ?: 0L
            totalOutstanding += balance
            
            rowsHtml.append("""
                <tr>
                    <td>${customer.name}</td>
                    <td>${customer.phone.ifEmpty { "-" }}</td>
                    <td>${customer.groupName.ifEmpty { if (isAr) "عام" else "General" }}</td>
                    <td>${linkedAcc?.accountCode ?: "-"} / ${Localization.getAccountName(linkedAcc?.accountCode ?: "", linkedAcc?.name ?: "", lang)}</td>
                    <td class="text-right" style="font-weight:bold;">${FinancialUtils.formatBase(balance)}</td>
                </tr>
            """.trimIndent())
        }

        val body = """
            <div class="header-container">
                <div style="display:flex; align-items:center; gap:12px;">
                    <span style="font-size:36px; line-height:1;">$brandLogo</span>
                    <div>
                        <div class="header-title">$title</div>
                        <div style="font-size:13px; color:#555; margin-top:5px;">
                            $brandDetails
                        </div>
                    </div>
                </div>
                <div class="header-meta">
                    <strong>$brandOrg</strong><br>
                    ${if (isAr) "تاريخ التصفية:" else "Issued Date:"} ${formatDate(System.currentTimeMillis())}
                </div>
            </div>

            <table class="report-table">
                <thead>
                    <tr>
                        <th>${if (isAr) "الاسم" else "Name"}</th>
                        <th>${if (isAr) "الهاتف" else "Phone"}</th>
                        <th>${if (isAr) "التصنيف" else "Category Group"}</th>
                        <th>${if (isAr) "الحساب الدفتري" else "General Ledger Account"}</th>
                        <th class="text-right">${if (isAr) "الرصيد د.ل" else "Oustanding LYD"}</th>
                    </tr>
                </thead>
                <tbody>
                    $rowsHtml
                    <tr style="background-color: #f1f0f5; font-weight: bold; font-size:13px;">
                        <td colspan="4">${if (isAr) "إجمالي الذمم المدينة المستحقة" else "Total Outstanding Receivables"}</td>
                        <td class="text-right" style="color:#0d8343;">${FinancialUtils.formatBase(totalOutstanding)}</td>
                    </tr>
                </tbody>
            </table>

            <div class="footer-signatures" style="margin-top:50px;">
                <div class="sig-block">
                    <strong>${if (isAr) "إعداد وتدقيق الشؤون المالية" else "Finance Auditor Verification"}</strong>
                    <div class="sig-line"></div>
                </div>
                <div class="sig-block">
                    <strong>&nbsp;</strong>
                </div>
                <div class="sig-block">
                    <strong>${if (isAr) "الاعتماد والختم الرسمي" else "Official Seal of Approval"}</strong>
                    <div class="sig-line"></div>
                </div>
            </div>
        """.trimIndent()

        printHtml(context, getHtmlTemplate(body, isAr), "customers_report_print")
    }

    fun printSuppliersReport(
        context: Context,
        suppliers: List<Supplier>,
        allAccounts: List<Account>,
        snapshots: List<AccountBalanceSnapshot>,
        lang: String
    ) {
        val isAr = lang == "ar"
        val (brandOrg, brandDetails, brandLogo) = getHeaderBrand(context, isAr)
        val title = if (isAr) "تقرير كشف أرصدة الموردين وحسابات الدائنين" else "Detailed Suppliers Accounts Payable Report"

        val rowsHtml = StringBuilder()
        var totalPayable = 0L
        suppliers.forEach { supplier ->
            val linkedAcc = allAccounts.find { it.id == supplier.accountId }
            val balance = snapshots.find { it.accountId == supplier.accountId }?.balance ?: 0L
            val revBalance = if (balance != 0L) -balance else 0L
            totalPayable += revBalance
            
            rowsHtml.append("""
                <tr>
                    <td>${supplier.name}</td>
                    <td>${supplier.phone.ifEmpty { "-" }}</td>
                    <td>${supplier.groupName.ifEmpty { if (isAr) "مورد عام" else "General Supplier" }}</td>
                    <td>${linkedAcc?.accountCode ?: "-"} / ${Localization.getAccountName(linkedAcc?.accountCode ?: "", linkedAcc?.name ?: "", lang)}</td>
                    <td class="text-right" style="font-weight:bold; color: #b71c1c;">${FinancialUtils.formatBase(revBalance)}</td>
                </tr>
            """.trimIndent())
        }

        val body = """
            <div class="header-container">
                <div style="display:flex; align-items:center; gap:12px;">
                    <span style="font-size:36px; line-height:1;">$brandLogo</span>
                    <div>
                        <div class="header-title">$title</div>
                        <div style="font-size:13px; color:#555; margin-top:5px;">
                            $brandDetails
                        </div>
                    </div>
                </div>
                <div class="header-meta">
                    <strong>$brandOrg</strong><br>
                    ${if (isAr) "تاريخ الإصدار:" else "Issued Date:"} ${formatDate(System.currentTimeMillis())}
                </div>
            </div>

            <table class="report-table">
                <thead>
                    <tr>
                        <th>${if (isAr) "الاسم" else "Supplier Name"}</th>
                        <th>${if (isAr) "الهاتف" else "Phone"}</th>
                        <th>${if (isAr) "التصنيف" else "Category Group"}</th>
                        <th>${if (isAr) "الحساب الدفتري" else "General Ledger Account"}</th>
                        <th class="text-right">${if (isAr) "الرصيد المستحق د.ل" else "Payable LYD"}</th>
                    </tr>
                </thead>
                <tbody>
                    $rowsHtml
                    <tr style="background-color: #f1f0f5; font-weight: bold; font-size:13px;">
                        <td colspan="4">${if (isAr) "إجمالي مستحقات الدفع للموردين" else "Total Accounts Payable"}</td>
                        <td class="text-right" style="color:#c5221f;">${FinancialUtils.formatBase(totalPayable)}</td>
                    </tr>
                </tbody>
            </table>

            <div class="footer-signatures" style="margin-top:50px;">
                <div class="sig-block">
                    <strong>${if (isAr) "إعداد وتدقيق الشؤون المالية" else "Finance Auditor Verification"}</strong>
                    <div class="sig-line"></div>
                </div>
                <div class="sig-block">
                    <strong>&nbsp;</strong>
                </div>
                <div class="sig-block">
                    <strong>${if (isAr) "الاعتماد والختم الرسمي" else "Official Seal of Approval"}</strong>
                    <div class="sig-line"></div>
                </div>
            </div>
        """.trimIndent()

        printHtml(context, getHtmlTemplate(body, isAr), "suppliers_report_print")
    }

    fun printBanksReport(
        context: Context,
        banks: List<Bank>,
        allBranches: List<BankBranch>,
        allBankAccounts: List<BankAccount>,
        allAccounts: List<Account>,
        snapshots: List<AccountBalanceSnapshot>,
        lang: String
    ) {
        val isAr = lang == "ar"
        val (brandOrg, brandDetails, brandLogo) = getHeaderBrand(context, isAr)
        val title = if (isAr) "كشف الحسابات المصرفية والأرصدة للبنوك" else "Detailed Bank Accounts Balances Report"

        val rowsHtml = StringBuilder()
        var totalAssets = 0L

        banks.forEach { bank ->
            val branches = allBranches.filter { it.bankId == bank.id }
            branches.forEach { branch ->
                val bankAccounts = allBankAccounts.filter { it.branchId == branch.id }
                bankAccounts.forEach { bAcc ->
                    val linkedAcc = allAccounts.find { it.id == bAcc.accountId }
                    val balance = snapshots.find { it.accountId == bAcc.accountId }?.balance ?: 0L
                    totalAssets += balance

                    rowsHtml.append("""
                        <tr>
                            <td><strong>${bank.name}</strong></td>
                            <td>${branch.name}</td>
                            <td>${bAcc.accountName} <br><small style="color:#666;">${bAcc.accountNumber}</small></td>
                            <td>${linkedAcc?.accountCode ?: "-"} / ${Localization.getAccountName(linkedAcc?.accountCode ?: "", linkedAcc?.name ?: "", lang)}</td>
                            <td class="text-right" style="font-weight:bold; color: #0d8343;">${FinancialUtils.formatBase(balance)}</td>
                        </tr>
                    """.trimIndent())
                }
            }
        }

        val body = """
            <div class="header-container">
                <div style="display:flex; align-items:center; gap:12px;">
                    <span style="font-size:36px; line-height:1;">$brandLogo</span>
                    <div>
                        <div class="header-title">$title</div>
                        <div style="font-size:13px; color:#555; margin-top:5px;">
                            $brandDetails
                        </div>
                    </div>
                </div>
                <div class="header-meta">
                    <strong>$brandOrg</strong><br>
                    ${if (isAr) "تاريخ الجرد:" else "Prepared On:"} ${formatDate(System.currentTimeMillis())}
                </div>
            </div>

            <table class="report-table">
                <thead>
                    <tr>
                        <th>${if (isAr) "البنك" else "Bank"}</th>
                        <th>${if (isAr) "الفرع" else "Branch"}</th>
                        <th>${if (isAr) "تفاصيل الحساب" else "Bank Account details"}</th>
                        <th>${if (isAr) "الحساب المالي الدفتري" else "General Ledger Link"}</th>
                        <th class="text-right">${if (isAr) "الرصيد الفعلي د.ل" else "Balance LYD"}</th>
                    </tr>
                </thead>
                <tbody>
                    ${if (rowsHtml.isNotEmpty()) rowsHtml else "<tr><td colspan='5' class='text-center'>${if (isAr) "لا توجد حسابات مصرفية مسجلة" else "No bank accounts registered"}</td></tr>"}
                    <tr style="background-color: #f1f0f5; font-weight: bold; font-size:13px;">
                        <td colspan="4">${if (isAr) "إجمالي الأرصدة والسيولة بالبنوك" else "Total Cash at Bank Balance"}</td>
                        <td class="text-right" style="color:#0d8343;">${FinancialUtils.formatBase(totalAssets)}</td>
                    </tr>
                </tbody>
            </table>

            <div class="footer-signatures" style="margin-top:50px;">
                <div class="sig-block">
                    <strong>${if (isAr) "إعداد وتطابق إدارة الخزينة" else "Treasury Department Representative"}</strong>
                    <div class="sig-line"></div>
                </div>
                <div class="sig-block">
                    <strong>&nbsp;</strong>
                </div>
                <div class="sig-block">
                    <strong>${if (isAr) "التوقيع والاعتماد المفوض" else "Official Seal of Approval"}</strong>
                    <div class="sig-line"></div>
                </div>
            </div>
        """.trimIndent()

        printHtml(context, getHtmlTemplate(body, isAr), "banks_report_print")
    }
}
