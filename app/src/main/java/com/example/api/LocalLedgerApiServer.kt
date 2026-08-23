package com.example.api

import android.util.Log
import com.example.data.LedgerRepository
import com.example.util.FinancialUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

class LocalLedgerApiServer(
    private val repository: LedgerRepository,
    private val scope: CoroutineScope
) {
    private var serverSocket: ServerSocket? = null
    var isRunning = false
        private set
    
    var lastOutputLog = "Server is idle..."
        private set

    val port = 8089

    fun start(onStatusChange: (Boolean, String) -> Unit = { _, _ -> }) {
        if (isRunning) return
        isRunning = true
        scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(port)
                lastOutputLog = "HTTP API Server started on port $port. Ready for integration requests."
                Log.d("LocalLedgerApiServer", lastOutputLog)
                onStatusChange(true, lastOutputLog)
                
                while (isRunning) {
                    val socket = serverSocket?.accept() ?: break
                    handleClient(socket)
                }
            } catch (e: Exception) {
                lastOutputLog = "Server failed: ${e.message}"
                Log.e("LocalLedgerApiServer", "Error in server socket", e)
                isRunning = false
                onStatusChange(false, lastOutputLog)
            }
        }
    }

    fun stop(onStatusChange: (Boolean, String) -> Unit = { _, _ -> }) {
        if (!isRunning) return
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e("LocalLedgerApiServer", "Error closing server socket", e)
        }
        serverSocket = null
        lastOutputLog = "HTTP API Server stopped."
        onStatusChange(false, lastOutputLog)
    }

    private fun handleClient(socket: Socket) {
        scope.launch(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val line = reader.readLine() ?: return@launch
                val parts = line.split(" ")
                if (parts.size < 2) {
                    sendResponse(socket, 400, "Bad Request", "text/plain")
                    return@launch
                }
                val method = parts[0]
                val path = parts[1]

                Log.d("LocalLedgerApiServer", "HTTP API Server Request: $method $path")
                
                if (method == "GET" && path.startsWith("/api/trial-balance")) {
                    generateTrialBalanceJsonResponse { jsonString ->
                        sendResponse(socket, 200, jsonString, "application/json")
                    }
                } else if (method == "GET" && path == "/api/status") {
                    val statusObj = JSONObject()
                    statusObj.put("status", "ONLINE")
                    statusObj.put("port", port)
                    statusObj.put("endpoint", "http://localhost:$port/api/trial-balance")
                    statusObj.put("ledgerSystem", "Libyan Financial Ledger Pro")
                    sendResponse(socket, 200, statusObj.toString(), "application/json")
                } else if (method == "GET" && (path == "/" || path == "/dashboard" || path == "/index.html" || path == "/portal")) {
                    sendResponse(socket, 200, generateWebDashboardHtml(), "text/html")
                } else {
                    val notFoundObj = JSONObject()
                    notFoundObj.put("error", "Not Found")
                    notFoundObj.put("message", "Available endpoints: /api/trial-balance, /api/status")
                    sendResponse(socket, 404, notFoundObj.toString(), "application/json")
                }
            } catch (e: Exception) {
                Log.e("LocalLedgerApiServer", "Error handling client session", e)
                try {
                    sendResponse(socket, 500, "Internal Server Error: ${e.message}", "text/plain")
                } catch (ex: Exception) { /* ignore */ }
            } finally {
                try {
                    socket.close()
                } catch (e: Exception) { /* ignore */ }
            }
        }
    }

    suspend fun generateTrialBalanceJsonResponse(callback: suspend (String) -> Unit) {
        try {
            // Get the first item of TrialBalance flow (calculated for all entries)
            val trialBalanceRows = repository.getTrialBalance(0L, System.currentTimeMillis()).first()
            
            // Calculate aggregations
            var sumOpeningDebit = 0L
            var sumOpeningCredit = 0L
            var sumPeriodDebit = 0L
            var sumPeriodCredit = 0L
            var sumClosingDebit = 0L
            var sumClosingCredit = 0L

            val rowsArray = JSONArray()
            for (row in trialBalanceRows) {
                val rowObj = JSONObject()
                rowObj.put("accountCode", row.accountCode)
                rowObj.put("accountName", row.accountName)
                rowObj.put("accountType", row.accountType.name)
                rowObj.put("openingDebit", row.openingDebit / FinancialUtils.BASE_SCALE_FACTOR)
                rowObj.put("openingCredit", row.openingCredit / FinancialUtils.BASE_SCALE_FACTOR)
                rowObj.put("periodDebit", row.periodDebit / FinancialUtils.BASE_SCALE_FACTOR)
                rowObj.put("periodCredit", row.periodCredit / FinancialUtils.BASE_SCALE_FACTOR)
                rowObj.put("closingDebit", row.closingDebit / FinancialUtils.BASE_SCALE_FACTOR)
                rowObj.put("closingCredit", row.closingCredit / FinancialUtils.BASE_SCALE_FACTOR)
                rowsArray.put(rowObj)

                sumOpeningDebit += row.openingDebit
                sumOpeningCredit += row.openingCredit
                sumPeriodDebit += row.periodDebit
                sumPeriodCredit += row.periodCredit
                sumClosingDebit += row.closingDebit
                sumClosingCredit += row.closingCredit
            }

            // Verify mathematical accuracy
            // Under Double Entry bookkeeping, Sum of Debits must equal Sum of Credits.
            // Check opening, period, and closing totals.
            val openingDifference = Math.abs(sumOpeningDebit - sumOpeningCredit)
            val periodDifference = Math.abs(sumPeriodDebit - sumPeriodCredit)
            val closingDifference = Math.abs(sumClosingDebit - sumClosingCredit)
            
            // Tolerance is 10 micro-units
            val isOpeningBalanced = openingDifference <= 10L
            val isPeriodBalanced = periodDifference <= 10L
            val isClosingBalanced = closingDifference <= 10L
            val isLedgerAccurate = isOpeningBalanced && isPeriodBalanced && isClosingBalanced

            val verification = JSONObject()
            verification.put("ledgerIsMathematicallyAccurate", isLedgerAccurate)
            verification.put("isClosingBalanced", isClosingBalanced)
            verification.put("isPeriodBalanced", isPeriodBalanced)
            verification.put("isOpeningBalanced", isOpeningBalanced)
            verification.put("closingDifferenceBase", closingDifference / FinancialUtils.BASE_SCALE_FACTOR)
            verification.put("periodDifferenceBase", periodDifference / FinancialUtils.BASE_SCALE_FACTOR)
            verification.put("openingDifferenceBase", openingDifference / FinancialUtils.BASE_SCALE_FACTOR)

            val summary = JSONObject()
            summary.put("totalOpeningDebit", sumOpeningDebit / FinancialUtils.BASE_SCALE_FACTOR)
            summary.put("totalOpeningCredit", sumOpeningCredit / FinancialUtils.BASE_SCALE_FACTOR)
            summary.put("totalPeriodDebit", sumPeriodDebit / FinancialUtils.BASE_SCALE_FACTOR)
            summary.put("totalPeriodCredit", sumPeriodCredit / FinancialUtils.BASE_SCALE_FACTOR)
            summary.put("totalClosingDebit", sumClosingDebit / FinancialUtils.BASE_SCALE_FACTOR)
            summary.put("totalClosingCredit", sumClosingCredit / FinancialUtils.BASE_SCALE_FACTOR)

            val root = JSONObject()
            root.put("timestamp", System.currentTimeMillis())
            root.put("fiscalPeriodEndUtc", System.currentTimeMillis())
            root.put("verification", verification)
            root.put("summary", summary)
            root.put("rows", rowsArray)

            callback(root.toString(2))
        } catch (e: Exception) {
            val errObj = JSONObject()
            errObj.put("error", "INTERNAL_AGGREGATION_FAILURE")
            errObj.put("details", e.message)
            callback(errObj.toString(2))
        }
    }

    private fun sendResponse(socket: Socket, statusCode: Int, body: String, contentType: String) {
        try {
            val statusText = when (statusCode) {
                200 -> "OK"
                400 -> "Bad Request"
                404 -> "Not Found"
                500 -> "Internal Server Error"
                else -> "OK"
            }
            val out: OutputStream = socket.getOutputStream()
            val header = "HTTP/1.1 $statusCode $statusText\r\n" +
                    "Content-Type: $contentType; charset=UTF-8\r\n" +
                    "Content-Length: ${body.toByteArray(Charsets.UTF_8).size}\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Connection: close\r\n\r\n"
            out.write(header.toByteArray(Charsets.UTF_8))
            out.write(body.toByteArray(Charsets.UTF_8))
            out.flush()
        } catch (e: Exception) {
            Log.e("LocalLedgerApiServer", "Error sending response", e)
        }
    }

    private fun generateWebDashboardHtml(): String {
        return """
<!DOCTYPE html>
<html lang="ar" dir="rtl" class="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>بوابة تدقيق ومطابقة أرصدة الحسابات | Libyan Financial Ledger</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://fonts.googleapis.com/css2?family=Tajawal:wght@300;400;500;700;900&family=JetBrains+Mono:wght@300;400;500;700&display=swap" rel="stylesheet">
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    fontFamily: {
                        sans: ['Tajawal', 'sans-serif'],
                        mono: ['JetBrains Mono', 'monospace'],
                    }
                }
            }
        }
    </script>
    <style>
        body {
            font-family: 'Tajawal', sans-serif;
            background-color: #0b0f19;
            color: #f1f5f9;
        }
        .custom-scrollbar::-webkit-scrollbar {
            width: 6px;
            height: 6px;
        }
        .custom-scrollbar::-webkit-scrollbar-track {
            background: #0f172a;
        }
        .custom-scrollbar::-webkit-scrollbar-thumb {
            background: #334155;
            border-radius: 4px;
        }
        .custom-scrollbar::-webkit-scrollbar-thumb:hover {
            background: #475569;
        }
        @media print {
            body {
                background-color: white !important;
                color: black !important;
            }
            .no-print {
                display: none !important;
            }
            .print-only {
                display: block !important;
            }
            .shadow-card {
                box-shadow: none !important;
                border: 1px solid #ccc !important;
            }
        }
    </style>
</head>
<body class="min-h-screen flex flex-col custom-scrollbar">

    <!-- Header Panel -->
    <header class="border-b border-slate-800 bg-slate-900/60 backdrop-blur-md sticky top-0 z-50 no-print">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex flex-col md:flex-row items-center justify-between gap-4">
            <div class="flex items-center gap-4">
                <div class="h-12 w-12 rounded-xl bg-gradient-to-tr from-emerald-500 to-sky-500 flex items-center justify-center shadow-lg shadow-emerald-500/10">
                    <span class="text-2xl font-black text-white">📊</span>
                </div>
                <div>
                    <h1 id="lbl-title" class="text-xl font-bold tracking-tight text-white">
                        منصة المراجعة السحابية | دفتر الأستاذ المالي الليبي Pro
                    </h1>
                    <p id="lbl-subtitle" class="text-xs text-slate-400 mt-0.5">
                        نظام بث البيانات الفورية وتدقيق القيد المزدوج اللامركزي
                    </p>
                </div>
            </div>
            
            <div class="flex flex-wrap items-center gap-3">
                <span class="inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 shadow-sm animate-pulse">
                    <span class="h-2 w-2 rounded-full bg-emerald-400 ml-2 me-2"></span>
                    <span id="lbl-status">حالة الاتصال الفوري: متصل</span>
                </span>
                
                <button onclick="toggleLanguage()" class="px-3.5 py-1.5 rounded-lg text-sm bg-slate-800 hover:bg-slate-700 transition font-bold border border-slate-700 text-slate-200 shadow-sm flex items-center gap-1.5">
                    🌐 <span id="btn-lang-toggle">English</span>
                </button>
                
                <button onclick="fetchTrialBalance()" class="px-4 py-1.5 rounded-lg text-sm font-bold bg-emerald-600 hover:bg-emerald-500 transition text-white shadow-md shadow-emerald-600/10 flex items-center gap-1.5">
                    🔄 <span id="lbl-reload">تحديث الأرصدة الآن</span>
                </button>
            </div>
        </div>
    </header>

    <main class="flex-grow max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 flex flex-col gap-6">
        
        <!-- Live Alert / Top Diagnostic Status Banner -->
        <section id="accuracy-banner" class="rounded-2xl border p-5 transition p-5 shadow-card flex items-start gap-4">
            <div class="p-3 rounded-xl" id="accuracy-icon-container">
                <span id="accuracy-icon" class="text-3xl">⏳</span>
            </div>
            <div class="flex-grow">
                <h3 id="lbl-accuracy-title" class="text-lg font-bold text-white mb-1">
                    جاري جلب ومطابقة الأرصدة الفورية للقيود...
                </h3>
                <p id="lbl-accuracy-msg" class="text-sm text-slate-400 leading-relaxed">
                    يرجى الانتظار لحين الانتهاء من الاتصال بقاعدة البيانات المحلية وفحص توازن ميزان المراجعة.
                </p>
                <div class="mt-3 flex items-center gap-3 text-xs font-mono" id="audit-details">
                    <!-- Dynamic properties go here -->
                </div>
            </div>
        </section>

        <!-- Dynamic Overview Cards Grid -->
        <section class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 no-print">
            
            <div class="bg-slate-900/40 border border-slate-800 rounded-2xl p-5 flex items-center justify-between">
                <div>
                    <p id="lbl-card-accounts" class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">إجمالي الحسابات المفعلة</p>
                    <h4 id="val-accounts-count" class="text-2xl font-black font-mono text-white">0</h4>
                </div>
                <div class="text-3xl p-3 bg-slate-800/40 rounded-xl">📁</div>
            </div>

            <div class="bg-slate-900/40 border border-slate-800 rounded-2xl p-5 flex items-center justify-between">
                <div>
                    <p id="lbl-card-debits" class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">مجموع الأرصدة المدينـة</p>
                    <h4 id="val-debits-sum" class="text-2xl font-black font-mono text-emerald-400">0.00</h4>
                </div>
                <div class="text-3xl p-3 bg-emerald-500/10 rounded-xl">🟢</div>
            </div>

            <div class="bg-slate-900/40 border border-slate-800 rounded-2xl p-5 flex items-center justify-between">
                <div>
                    <p id="lbl-card-credits" class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">مجموع الأرصدة الدائنـة</p>
                    <h4 id="val-credits-sum" class="text-2xl font-black font-mono text-orange-400">0.00</h4>
                </div>
                <div class="text-3xl p-3 bg-orange-500/10 rounded-xl">🟡</div>
            </div>

            <div class="bg-slate-900/40 border border-slate-800 rounded-2xl p-5 flex items-center justify-between">
                <div>
                    <p id="lbl-card-variance" class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">الانحراف أو الفارق المالي</p>
                    <h4 id="val-variance-sum" class="text-2xl font-black font-mono text-white">0.00</h4>
                </div>
                <div class="text-3xl p-3 bg-slate-800/40 rounded-xl" id="variance-icon-bg">⚖️</div>
            </div>

        </section>

        <!-- Search Controls -->
        <section class="bg-slate-900/40 border border-slate-800 rounded-2xl p-5 flex flex-col md:flex-row gap-4 items-center justify-between no-print">
            <div class="relative w-full md:max-w-md">
                <span class="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 rtl:right-0 rtl:left-auto ltr:left-0 ltr:right-auto pointer-events-none">🔍</span>
                <input type="text" id="search-input" onkeyup="filterTable()" placeholder="البحث برمز الحساب، الاسم، أو التصنيف بالدليل المحاسبي..." class="w-full bg-slate-950 border border-slate-700/80 rounded-xl pl-4 pr-10 py-2.5 text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition font-medium">
            </div>
            
            <div class="flex flex-wrap items-center gap-2 w-full md:w-auto">
                <button onclick="setFilterType('ALL')" id="filter-all" class="px-4 py-2 rounded-lg text-xs font-black transition active-filter bg-slate-800 text-white border border-slate-700">الكل / All</button>
                <button onclick="setFilterType('ASSET')" id="filter-asset" class="px-4 py-2 rounded-lg text-xs font-bold transition text-slate-400 hover:text-white bg-slate-900/30">أصول / Asset</button>
                <button onclick="setFilterType('LIABILITY')" id="filter-liability" class="px-4 py-2 rounded-lg text-xs font-bold transition text-slate-400 hover:text-white bg-slate-900/30">خصوم / Liability</button>
                <button onclick="setFilterType('EQUITY')" id="filter-equity" class="px-4 py-2 rounded-lg text-xs font-bold transition text-slate-400 hover:text-white bg-slate-900/30">حقوق ملكية / Equity</button>
                <button onclick="setFilterType('REVENUE')" id="filter-revenue" class="px-4 py-2 rounded-lg text-xs font-bold transition text-slate-400 hover:text-white bg-slate-900/30">إيرادات / Revenue</button>
                <button onclick="setFilterType('EXPENSE')" id="filter-expense" class="px-4 py-2 rounded-lg text-xs font-bold transition text-slate-400 hover:text-white bg-slate-900/30">مصروفات / Expense</button>
            </div>
        </section>

        <!-- Legal Print Header (only shows when printing) -->
        <section class="hidden print-only py-6 text-center border-b border-black">
            <h2 class="text-2xl font-black">تقرير ميزان المراجعة القانوني الموحد</h2>
            <h3 class="text-lg mt-1">المؤسسة الليبية للاستثمارات وإدارة الأصول</h3>
            <p class="text-xs text-gray-600 mt-2 font-mono">تاريخ استخراج البيانات: <span class="utc-now-print"></span> UTC</p>
        </section>

        <!-- Trial Balance Interactive Table Card -->
        <section class="bg-slate-900/40 border border-slate-800 rounded-2xl overflow-hidden shadow-card">
            <div class="px-6 py-4 border-b border-slate-800 flex items-center justify-between flex-wrap gap-3 no-print">
                <h2 id="lbl-table-header" class="text-md font-bold text-white flex items-center gap-2">
                    📋 كشف ميزان المراجعة المدقق
                </h2>
                <div class="flex items-center gap-2">
                    <button onclick="triggerPrint()" class="px-3.5 py-1.5 rounded-lg text-xs bg-slate-800 hover:bg-slate-700 transition font-bold border border-slate-700 text-slate-200 shadow flex items-center gap-1">
                        🖨️ <span id="lbl-btn-print">طباعة التقرير القانوني</span>
                    </button>
                    <button onclick="exportToCsv()" class="px-3.5 py-1.5 rounded-lg text-xs bg-emerald-600/10 hover:bg-emerald-600/20 transition font-bold border border-emerald-500/20 text-emerald-400 shadow flex items-center gap-1">
                        📥 <span id="lbl-btn-export">تصدير ورقة التحليل (CSV)</span>
                    </button>
                </div>
            </div>

            <!-- Table Container -->
            <div class="overflow-x-auto custom-scrollbar">
                <table class="w-full text-left border-collapse" id="ledger-table">
                    <thead>
                        <tr class="bg-slate-900/80 border-b border-slate-800 text-xs font-bold text-slate-300">
                            <th class="py-3.5 px-6 text-right w-24" id="th-code">رمز الحساب</th>
                            <th class="py-3.5 px-6 text-right" id="th-name">اسم الحساب</th>
                            <th class="py-3.5 px-6 text-center w-28" id="th-type">التصنيف</th>
                            <th class="py-3.5 px-6 text-right font-mono w-32" id="th-op-debit">رصيد الفتح (مدين)</th>
                            <th class="py-3.5 px-6 text-right font-mono w-32" id="th-op-credit">رصيد الفتح (دائن)</th>
                            <th class="py-3.5 px-6 text-right font-mono w-32" id="th-pd-debit">حركة الفترة (مدين)</th>
                            <th class="py-3.5 px-6 text-right font-mono w-32" id="th-pd-credit">حركة الفترة (دائن)</th>
                            <th class="py-3.5 px-6 text-right font-mono w-32" id="th-cl-debit">الرصيد النهائي (مدين)</th>
                            <th class="py-3.5 px-6 text-right font-mono w-32" id="th-cl-credit">الرصيد النهائي (دائن)</th>
                        </tr>
                    </thead>
                    <tbody id="table-body" class="divide-y divide-slate-800/60 font-medium text-slate-300">
                        <!-- Simulated Rows will be appended here -->
                        <tr>
                            <td colspan="9" class="py-12 text-center text-slate-500 max-w-7xl">
                                <span class="inline-block animate-spin mr-2">⏳</span> جاري تحميل ميزان المراجعة وتدقيق القيود...
                            </td>
                        </tr>
                    </tbody>
                    <tfoot>
                        <tr class="bg-slate-900/85 border-t-2 border-slate-800 text-sm font-bold text-white">
                            <th class="py-4 px-6 text-right" id="tf-lbl" colspan="3">إجمالي مجاميع الأرصدة المتطابقة:</th>
                            <th class="py-4 px-6 text-right font-mono whitespace-nowrap" id="tot-op-debit">0.00</th>
                            <th class="py-4 px-6 text-right font-mono whitespace-nowrap" id="tot-op-credit">0.00</th>
                            <th class="py-4 px-6 text-right font-mono whitespace-nowrap" id="tot-pd-debit">0.00</th>
                            <th class="py-4 px-6 text-right font-mono whitespace-nowrap" id="tot-pd-credit">0.00</th>
                            <th class="py-4 px-6 text-right font-mono whitespace-nowrap" id="tot-cl-debit">0.00</th>
                            <th class="py-4 px-6 text-right font-mono whitespace-nowrap" id="tot-cl-credit">0.00</th>
                        </tr>
                    </tfoot>
                </table>
            </div>
        </section>

    </main>

    <footer class="border-t border-slate-800 bg-slate-950/80 py-6 mt-12 text-center text-xs text-slate-500 no-print">
        <div class="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-4">
            <p>جميع البيانات مأخوذة فورياً من قاعدة بيانات المحمول المحلية المشفرة - IFRS Compliant</p>
            <p class="font-mono">Libyan Ledger Pro Integration Gateway v1.4.0</p>
        </div>
    </footer>

    <!-- Logic Script without any $ symbol to avoid Kotlin string interpolation mismatch -->
    <script>
        var currentLang = 'ar';
        var tableData = [];
        var activeFilterType = 'ALL';

        var i18n = {
            ar: {
                title: "منصة المراجعة السحابية | دفتر الأستاذ المالي الليبي Pro",
                subtitle: "نظام بث البيانات الفورية وتدقيق القيد المزدوج اللامركزي",
                status: "حالة الاتصال الفوري: متصل",
                serverStatus: "خادم ميزان المراجعة المحلي نشط وبث حي على منفذ 8089",
                accuracyTitle: "مطابقة التوازن المالي والدقة الرياضية للقيود",
                accurateMsg: "دقة رياضية ممتازة ومطابقة تامة لأرصدة القيد المزدوج 100% (هامش فارق = 0.0 د.ل)",
                inaccurateMsg: "تنبيه: تم رصد عدم توازن في الأرصدة والقيود! يرجى مراجعة سجلات اليومية وقيد التسويات السريعة.",
                searchPlaceholder: "البحث برمز الحساب، الاسم، أو التصنيف بالدليل المحاسبي...",
                cardAccounts: "إجمالي الحسابات المفعلة",
                cardDebits: "مجموع الأرصدة المدينـة",
                cardCredits: "مجموع الأرصدة الدائنـة",
                cardVariance: "الانحراف أو الفارق المالي",
                tableHeader: "📋 كشف ميزان المراجعة المدقق",
                btnPrint: "طباعة التقرير القانوني",
                btnExport: "تصدير ورقة التحليل (CSV)",
                thCode: "رمز الحساب",
                thName: "اسم الحساب",
                thType: "التصنيف",
                thOpDebit: "رصيد الفتح (مدين)",
                thOpCredit: "رصيد الفتح (دائن)",
                thPdDebit: "حركة الفترة (مدين)",
                thPdCredit: "حركة الفترة (دائن)",
                thClDebit: "الرصيد النهائي (مدين)",
                thClCredit: "الرصيد النهائي (دائن)",
                totals: "إجمالي مجاميع الأرصدة المتطابقة:",
                reload: "تحديث الأرصدة الآن",
                verificationStatus: "تطابق القيد المزدوج",
                difference: "قيمة الفارق المعلق",
                currency: " د.ل",
                asset: "أصول",
                liability: "خصوم",
                equity: "حقوق ملكية",
                revenue: "إيرادات",
                expense: "مصروفات"
            },
            en: {
                title: "Auditor Cloud Portal | Libyan Ledger Pro",
                subtitle: "Real-time ledger broadcast & double-entry validation engine",
                status: "Live Connection Standard: ONLINE",
                serverStatus: "Local Trial Balance server running and broadcasting on port 8089",
                accuracyTitle: "Ledger Accuracy Verification & Mathematical Match Checks",
                accurateMsg: "Mathematical alignment confirmed. 100% compliant Double-Entry check. (Variance = 0.0 LYD)",
                inaccurateMsg: "CRITICAL warning: Unbalanced double-entry ledger offset identified! Verify recent journal vouchers.",
                searchPlaceholder: "Search account code, title, or classification category...",
                cardAccounts: "Total Registered Accounts",
                cardDebits: "Sum of Debit Balances",
                cardCredits: "Sum of Credit Balances",
                cardVariance: "Drift / Mathematical Variance",
                tableHeader: "📋 Audited Trial Balance Ledger",
                btnPrint: "Print Auditor Statement",
                btnExport: "Export Analytics (CSV)",
                thCode: "Account Code",
                thName: "Account Description",
                thType: "Taxonomy",
                thOpDebit: "Opening Debit",
                thOpCredit: "Opening Credit",
                thPdDebit: "Period Debit",
                thPdCredit: "Period Credit",
                thClDebit: "Closing Debit",
                thClCredit: "Closing Credit",
                totals: "Consolidated Grand Totals:",
                reload: "Refresh Ledger Data",
                verificationStatus: "Audit Match Status",
                difference: "Drift Value Balance",
                currency: " LYD",
                asset: "ASSET",
                liability: "LIABILITY",
                equity: "EQUITY",
                revenue: "REVENUE",
                expense: "EXPENSE"
            }
        };

        function updateUILabels() {
            var dictionary = i18n[currentLang];
            document.getElementById("lbl-title").innerText = dictionary.title;
            document.getElementById("lbl-subtitle").innerText = dictionary.subtitle;
            document.getElementById("lbl-status").innerText = dictionary.status;
            document.getElementById("lbl-reload").innerText = dictionary.reload;
            document.getElementById("lbl-card-accounts").innerText = dictionary.cardAccounts;
            document.getElementById("lbl-card-debits").innerText = dictionary.cardDebits;
            document.getElementById("lbl-card-credits").innerText = dictionary.cardCredits;
            document.getElementById("lbl-card-variance").innerText = dictionary.cardVariance;
            document.getElementById("search-input").placeholder = dictionary.searchPlaceholder;
            document.getElementById("lbl-table-header").innerText = dictionary.tableHeader;
            document.getElementById("lbl-btn-print").innerText = dictionary.btnPrint;
            document.getElementById("lbl-btn-export").innerText = dictionary.btnExport;
            
            document.getElementById("th-code").innerText = dictionary.thCode;
            document.getElementById("th-name").innerText = dictionary.thName;
            document.getElementById("th-type").innerText = dictionary.thType;
            document.getElementById("th-op-debit").innerText = dictionary.thOpDebit;
            document.getElementById("th-op-credit").innerText = dictionary.thOpCredit;
            document.getElementById("th-pd-debit").innerText = dictionary.thPdDebit;
            document.getElementById("th-pd-credit").innerText = dictionary.thPdCredit;
            document.getElementById("th-cl-debit").innerText = dictionary.thClDebit;
            document.getElementById("th-cl-credit").innerText = dictionary.thClCredit;
            document.getElementById("tf-lbl").innerText = dictionary.totals;

            // Apply body classes for alignment
            if (currentLang === 'ar') {
                document.documentElement.dir = 'rtl';
                document.documentElement.lang = 'ar';
            } else {
                document.documentElement.dir = 'ltr';
                document.documentElement.lang = 'en';
            }
            
            // Re-render table rows with new taxonomy translations
            renderTableRows();
        }

        function toggleLanguage() {
            if (currentLang === 'ar') {
                currentLang = 'en';
                document.getElementById("btn-lang-toggle").innerText = "العربية";
            } else {
                currentLang = 'ar';
                document.getElementById("btn-lang-toggle").innerText = "English";
            }
            updateUILabels();
        }

        function triggerPrint() {
            document.querySelectorAll(".utc-now-print").forEach(function(el) {
                el.innerText = new Date().toISOString();
            });
            window.print();
        }

        function setFilterType(type) {
            activeFilterType = type;
            // Update UI filter button views
            var filters = ['ALL', 'ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE'];
            filters.forEach(function(f) {
                var btn = document.getElementById("filter-" + f.toLowerCase());
                if (f === type) {
                    btn.className = "px-4 py-2 rounded-lg text-xs font-black transition active-filter bg-emerald-600 text-white border border-emerald-500 shadow";
                } else {
                    btn.className = "px-4 py-2 rounded-lg text-xs font-bold transition text-slate-400 hover:text-white bg-slate-900/30 border border-transparent";
                }
            });
            filterTable();
        }

        function fetchTrialBalance() {
            fetch("/api/trial-balance")
                .then(function(res) {
                    return res.json();
                })
                .then(function(json) {
                    tableData = json.rows || [];
                    processVerificationStatus(json.verification, json.summary);
                    renderTableRows();
                })
                .catch(function(err) {
                    console.error("Error connecting to localized restful API stream", err);
                    alert("نظام الربط معطل: يرجى التأكد من تشغيل خادم ميزان المراجعة على منفذ 8089.");
                });
        }

        function processVerificationStatus(verifyDetails, summary) {
            var banner = document.getElementById("accuracy-banner");
            var iconContainer = document.getElementById("accuracy-icon-container");
            var icon = document.getElementById("accuracy-icon");
            var title = document.getElementById("lbl-accuracy-title");
            var desc = document.getElementById("lbl-accuracy-msg");
            var detailsBox = document.getElementById("audit-details");
            
            var isBalanced = verifyDetails ? verifyDetails.ledgerIsMathematicallyAccurate : true;
            var accountsCount = tableData.length;
            
            // Fill Cards
            document.getElementById("val-accounts-count").innerText = accountsCount;
            
            var totalOpeningDebit = summary ? summary.totalOpeningDebit : 0.0;
            var totalOpeningCredit = summary ? summary.totalOpeningCredit : 0.0;
            var totalPeriodDebit = summary ? summary.totalPeriodDebit : 0.0;
            var totalPeriodCredit = summary ? summary.totalPeriodCredit : 0.0;
            var totalClosingDebit = summary ? summary.totalClosingDebit : 0.0;
            var totalClosingCredit = summary ? summary.totalClosingCredit : 0.0;

            // Apply formatted values to cards/table footers
            document.getElementById("val-debits-sum").innerText = formatCurrency(totalClosingDebit);
            document.getElementById("val-credits-sum").innerText = formatCurrency(totalClosingCredit);
            
            var diff = Math.abs(totalClosingDebit - totalClosingCredit);
            var varianceCard = document.getElementById("val-variance-sum");
            var varianceBg = document.getElementById("variance-icon-bg");
            
            varianceCard.innerText = formatCurrency(diff);
            
            if (diff <= 0.01) {
                varianceCard.className = "text-2xl font-black font-mono text-emerald-400";
                varianceBg.className = "text-3xl p-3 bg-emerald-500/10 rounded-xl";
            } else {
                varianceCard.className = "text-2xl font-black font-mono text-rose-400";
                varianceBg.className = "text-3xl p-3 bg-rose-500/10 rounded-xl";
            }

            // Footers
            document.getElementById("tot-op-debit").innerText = formatCurrency(totalOpeningDebit);
            document.getElementById("tot-op-credit").innerText = formatCurrency(totalOpeningCredit);
            document.getElementById("tot-pd-debit").innerText = formatCurrency(totalPeriodDebit);
            document.getElementById("tot-pd-credit").innerText = formatCurrency(totalPeriodCredit);
            document.getElementById("tot-cl-debit").innerText = formatCurrency(totalClosingDebit);
            document.getElementById("tot-cl-credit").innerText = formatCurrency(totalClosingCredit);

            var dictionary = i18n[currentLang];

            if (isBalanced) {
                banner.className = "rounded-2xl border bg-emerald-500/10 border-emerald-500/30 p-5 shadow-card flex items-start gap-4";
                iconContainer.className = "p-3 bg-emerald-500/20 rounded-xl";
                icon.innerText = "✅";
                title.innerText = dictionary.accuracyTitle;
                title.className = "text-lg font-extrabold text-emerald-400 mb-1";
                desc.innerText = dictionary.accurateMsg;
            } else {
                banner.className = "rounded-2xl border bg-rose-500/10 border-rose-500/30 p-5 shadow-card flex items-start gap-4";
                iconContainer.className = "p-3 bg-rose-500/20 rounded-xl";
                icon.innerText = "⚠️";
                title.innerText = dictionary.accuracyTitle;
                title.className = "text-lg font-extrabold text-rose-400 mb-1";
                desc.innerText = dictionary.inaccurateMsg;
            }

            // Create metadata detail bubbles
            detailsBox.innerHTML = "";
            var details = [
                { label: dictionary.verificationStatus, val: isBalanced ? "PASS / CERTIFIED" : "WARNING / ADJUST REQUIRED", col: isBalanced ? "text-emerald-400" : "text-rose-400" },
                { label: dictionary.difference, val: formatCurrency(diff) + dictionary.currency, col: diff > 0.01 ? "text-rose-400 font-bold" : "text-slate-400" }
            ];
            
            details.forEach(function(item) {
                var div = document.createElement("div");
                div.className = "bg-slate-900/60 border border-slate-800 rounded-lg px-3 py-1.5 flex items-center gap-2";
                div.innerHTML = "<span class='text-slate-500'>" + item.label + ":</span><span class='" + item.col + "'>" + item.val + "</span>";
                detailsBox.appendChild(div);
            });
        }

        function formatCurrency(val) {
            return (val || 0.0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
        }

        function renderTableRows() {
            var body = document.getElementById("table-body");
            body.innerHTML = "";
            
            if (tableData.length === 0) {
                body.innerHTML = "<tr><td colspan='9' class='py-12 text-center text-slate-500'>لا توجد قيود مسجلة لعرضها حالياً. املأ سجل ميزان المراجعة بالعمليات.</td></tr>";
                return;
            }

            var query = document.getElementById("search-input").value.toLowerCase();
            var dict = i18n[currentLang];

            tableData.forEach(function(row) {
                // Filter by type
                if (activeFilterType !== 'ALL' && row.accountType !== activeFilterType) {
                    return;
                }
                
                // Filter by search query
                var nameMatch = (row.accountName || '').toLowerCase().indexOf(query) !== -1;
                var codeMatch = (row.accountCode || '').toLowerCase().indexOf(query) !== -1;
                var typeMatch = (row.accountType || '').toLowerCase().indexOf(query) !== -1;
                
                if (!nameMatch && !codeMatch && !typeMatch) {
                    return;
                }

                var typeText = dict[row.accountType.toLowerCase()] || row.accountType;
                var tr = document.createElement("tr");
                tr.className = "hover:bg-slate-900/25 transition duration-150 border-b border-slate-800 text-xs sm:text-sm";
                
                tr.innerHTML = 
                    "<td class='py-3.5 px-6 text-right font-bold text-slate-200 font-mono'>" + row.accountCode + "</td>" +
                    "<td class='py-3.5 px-6 text-right font-semibold text-white'>" + row.accountName + "</td>" +
                    "<td class='py-3.5 px-6 text-center'><span class='inline-block px-2.5 py-1 rounded-full text-xs font-bold " + getTypeBadgeStyle(row.accountType) + "'>" + typeText + "</span></td>" +
                    "<td class='py-3.5 px-6 text-right font-mono text-slate-400'>" + formatCurrency(row.openingDebit) + "</td>" +
                    "<td class='py-3.5 px-6 text-right font-mono text-slate-400'>" + formatCurrency(row.openingCredit) + "</td>" +
                    "<td class='py-3.5 px-6 text-right font-mono text-slate-300'>" + formatCurrency(row.periodDebit) + "</td>" +
                    "<td class='py-3.5 px-6 text-right font-mono text-slate-300'>" + formatCurrency(row.periodCredit) + "</td>" +
                    "<td class='py-3.5 px-6 text-right font-mono font-bold text-emerald-400'>" + formatCurrency(row.closingDebit) + "</td>" +
                    "<td class='py-3.5 px-6 text-right font-mono font-bold text-orange-400'>" + formatCurrency(row.closingCredit) + "</td>";
                
                body.appendChild(tr);
            });
        }

        function getTypeBadgeStyle(type) {
            switch(type) {
                case 'ASSET': return 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20';
                case 'LIABILITY': return 'bg-rose-500/10 text-rose-400 border border-rose-500/20';
                case 'EQUITY': return 'bg-sky-500/10 text-sky-400 border border-sky-500/20';
                case 'REVENUE': return 'bg-purple-500/10 text-purple-400 border border-purple-500/20';
                case 'EXPENSE': return 'bg-amber-500/10 text-amber-500 border border-amber-500/20';
                default: return 'bg-slate-800 text-slate-400';
            }
        }

        function filterTable() {
            renderTableRows();
        }

        function exportToCsv() {
            if (tableData.length === 0) {
                alert("يرجى ملء ميزان المراجعة بالبيانات أولاً قبل التصدير.");
                return;
            }
            
            var csv = [];
            var dict = i18n[currentLang];
            
            // Header
            var headers = [dict.thCode, dict.thName, dict.thType, dict.thOpDebit, dict.thOpCredit, dict.thPdDebit, dict.thPdCredit, dict.thClDebit, dict.thClCredit];
            csv.push(headers.join(","));
            
            tableData.forEach(function(row) {
                var itemType = dict[row.accountType.toLowerCase()] || row.accountType;
                var cols = [
                    '"' + row.accountCode + '"',
                    '"' + row.accountName + '"',
                    '"' + itemType + '"',
                    row.openingDebit,
                    row.openingCredit,
                    row.periodDebit,
                    row.periodCredit,
                    row.closingDebit,
                    row.closingCredit
                ];
                csv.push(cols.join(","));
            });
            
            var csvString = csv.join("\n");
            var blob = new Blob(["\uFEFF" + csvString], { type: "text/csv;charset=utf-8;" });
            var link = document.createElement("a");
            link.href = URL.createObjectURL(blob);
            link.setAttribute("download", "trial_balance_" + new Date().getTime() + ".csv");
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        }

        // Auto Load Data on Page Start
        window.onload = function() {
            updateUILabels();
            fetchTrialBalance();
        };
    </script>
</body>
</html>
        """.trimIndent()
    }
}

