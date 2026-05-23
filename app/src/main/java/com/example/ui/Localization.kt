package com.example.ui

import androidx.compose.ui.unit.LayoutDirection

object Localization {

    enum class Key {
        // App Core Labels
        APP_NAME,
        DASHBOARD,
        ACCOUNTS,
        CUSTOMERS,
        VOUCHERS,
        REPORTS,
        AUDITS,
        INITIALIZING,
        SEEDING_PROP,
        
        // Navigation Options
        NAV_DASHBOARD,
        NAV_ACCOUNTS,
        NAV_CUSTOMERS,
        NAV_SUPPLIERS,
        NAV_VOUCHERS,
        NAV_REPORTS,
        NAV_SETTINGS,

        // Dashboard Screen
        TOTAL_ASSETS,
        TOTAL_LIABILITIES,
        TOTAL_EQUITY,
        TOTAL_REVENUE,
        TOTAL_EXPENSES,
        NET_PROFIT,
        NET_LOSS,
        TRIAL_BALANCE_SUMMARY,
        QUICK_ACTIONS,
        CREATE_VOUCHER,
        VIEW_LEDGER,
        PREPARE_REPORTS,
        AUDIT_SYSTEM_LOGS,
        CURRENCY_OVERVIEW,
        BASE_CURRENCY,
        EXCHANGE_RATE,
        POSTED_TXS,
        DRAFT_TXS,

        // Accounts / CoA Screen
        CHART_OF_ACCOUNTS,
        SUB_ACCOUNTS_FOR,
        ADD_ACCOUNT,
        ADD_SUBACCOUNT,
        NAME,
        CODE,
        TYPE,
        CURRENCY,
        IS_FOLDER,
        CANCEL,
        CREATE,
        DELETE,
        CONFIRM_DELETE_ACC,
        PARENT,
        SURE_DELETE,
        SYSTEM_ACC_CANT_DELETE,

        // Customers Screen
        CUSTOMER_MANAGEMENT,
        ADD_NEW_CUSTOMER,
        CUSTOMER_NAME,
        PHONE,
        EMAIL,
        C_ACCOUNT_LINKING,
        AUTO_OPEN_ACCOUNT,
        LINK_EXISTING_MATCH,
        LINK_EXISTING,
        SAVE_CUSTOMER,
        SELECT_EXISTING_COA,
        NO_CUSTOMERS_FOUND,
        CUSTOMER_NAME_EMPTY,
        CUSTOMER_ADDED_SUCCESS,
        CUSTOMER_EXISTING_LINKED,
        CUSTOMER_NEW_OPENED,
        SEARCH_CUSTOMER,

        // Vouchers Screen
        VOUCHER_TX_JOURNAL,
        POSTED,
        DRAFT,
        UNPOSTED_TIP,
        POST,
        UNPOST,
        EDIT,
        NEW_VOUCHER,
        ST_JOURNAL,
        ST_RECEIPT,
        ST_PAYMENT,
        DATE,
        MEMO,
        TOTAL_BASE,
        POST_SUCCESS,
        POST_ERROR,
        CONFIRM_DELETE_VOUCHER,

        // Voucher Editor Screen
        VOUCHER_EDITOR,
        VOUCHER_NO,
        TOTAL_DEBIT,
        TOTAL_CREDIT,
        BALANCED,
        UNBALANCED,
        ADD_LINE,
        SAVE_DRAFT,
        DEBIT,
        CREDIT,
        EXCHANGE_RATE_LABEL,
        SELECT_ACC,
        ACTIONS,

        // Reports Screen
        FINANCIAL_STATEMENTS,
        TRIAL_BALANCE,
        BALANCE_SHEET,
        INCOME_STATEMENT,
        START_DATE,
        END_DATE,
        RECALCULATE,
        ACCOUNT,
        CLOSING_DEBIT,
        CLOSING_CREDIT,
        TRIAL_BALANCE_BALANCED,
        TRIAL_BALANCE_UNBALANCED,
        VAL_BS_EQUALS,
        VAL_BS_NOT_EQUALS,
        RETAINED_EARNINGS,
        REVENUE_MINUS_EXPENSES,

        // Settings / Compliance Screen
        COMPLIANCE_SETTINGS,
        LANGUAGE,
        ADD_CURRENCY,
        CURRENCY_CODE,
        DECIMAL_PLACES,
        ADD,
        FISCAL_PERIODS,
        LOCKED,
        ACTIVE,
        START_OF_YEAR,
        END_OF_YEAR,
        LOG_TRAIL_IFRS,
        PERFORMED_BY,
        TIMESTAMP,
        DETAILS,
        CURRENCY_ADDED,
        FISCAL_ADDED,
        LANGUAGE_CHANGED,
        ACCOUNTING_MODE,
        LIBYAN_MODE,
        NORMAL_MODE,
        LIBYAN_MODE_DESC,
        VIEW_STATEMENT,
        STATEMENT_TITLE,
        RUNNING_BALANCE,
        CLOSE_STATEMENT,
        NO_STATEMENT_TXS,
        THEME_MODE,
        DARK_MODE,
        LIGHT_MODE,
        THEME_DESC
    }

    private val enMap = mapOf(
        Key.APP_NAME to "Enterprise Ledger Workspace",
        Key.DASHBOARD to "Dashboard",
        Key.ACCOUNTS to "Accounts",
        Key.CUSTOMERS to "Customers",
        Key.VOUCHERS to "Vouchers",
        Key.REPORTS to "Reports",
        Key.AUDITS to "Audits & Settings",
        Key.INITIALIZING to "Initializing Ledger Database...",
        Key.SEEDING_PROP to "Seeding IFRS Chart of Accounts & Opening Fiscal Year 2026...",
        
        Key.NAV_DASHBOARD to "Dashboard",
        Key.NAV_ACCOUNTS to "Accounts",
        Key.NAV_CUSTOMERS to "Customers",
        Key.NAV_SUPPLIERS to "Suppliers",
        Key.NAV_VOUCHERS to "Vouchers",
        Key.NAV_REPORTS to "Reports",
        Key.NAV_SETTINGS to "Compliance",

        Key.TOTAL_ASSETS to "Total Assets",
        Key.TOTAL_LIABILITIES to "Total Liabilities",
        Key.TOTAL_EQUITY to "Total Equity",
        Key.TOTAL_REVENUE to "Total Revenue",
        Key.TOTAL_EXPENSES to "Total Expenses",
        Key.NET_PROFIT to "Net Profit",
        Key.NET_LOSS to "Net Loss",
        Key.TRIAL_BALANCE_SUMMARY to "Trial Balance Summary",
        Key.QUICK_ACTIONS to "Compliance Quick Actions",
        Key.CREATE_VOUCHER to "New Voucher Block",
        Key.VIEW_LEDGER to "Audit Chart of Accounts",
        Key.PREPARE_REPORTS to "Run Financial Trial",
        Key.AUDIT_SYSTEM_LOGS to "Examine Sys Audit",
        Key.CURRENCY_OVERVIEW to "Registered Currencies",
        Key.BASE_CURRENCY to "Base Currency",
        Key.EXCHANGE_RATE to "Exchange Rate",
        Key.POSTED_TXS to "Posted Vouchers",
        Key.DRAFT_TXS to "Draft Entries",

        Key.CHART_OF_ACCOUNTS to "Chart of Accounts (IFRS)",
        Key.SUB_ACCOUNTS_FOR to "Sub-accounts for",
        Key.ADD_ACCOUNT to "Add Account",
        Key.ADD_SUBACCOUNT to "Add Sub-account Under",
        Key.NAME to "Name",
        Key.CODE to "Account Code",
        Key.TYPE to "Account Type",
        Key.CURRENCY to "Currency",
        Key.IS_FOLDER to "Is Folder (Group)?",
        Key.CANCEL to "Cancel",
        Key.CREATE to "Create",
        Key.DELETE to "Delete",
        Key.CONFIRM_DELETE_ACC to "Are you sure you want to delete account:",
        Key.PARENT to "Parent Account",
        Key.SURE_DELETE to "Confirm Deletion",
        Key.SYSTEM_ACC_CANT_DELETE to "System Accounts are locked and cannot be deleted.",

        Key.CUSTOMER_MANAGEMENT to "Customer Accounts Ledger",
        Key.ADD_NEW_CUSTOMER to "Register Customer Profile",
        Key.CUSTOMER_NAME to "Customer Name",
        Key.PHONE to "Phone",
        Key.EMAIL to "Email Address",
        Key.C_ACCOUNT_LINKING to "Ledger Account Setup",
        Key.AUTO_OPEN_ACCOUNT to "Automatically create and link brand new account under accounts receivable (1103)",
        Key.LINK_EXISTING_MATCH to "Search existing Chart of Accounts with same name and link",
        Key.LINK_EXISTING to "Select existing Chart of Accounts manually...",
        Key.SAVE_CUSTOMER to "Register Customer",
        Key.SELECT_EXISTING_COA to "Select Account from Ledger",
        Key.NO_CUSTOMERS_FOUND to "No customers registered yet. Click plus below to add.",
        Key.CUSTOMER_NAME_EMPTY to "Customer name is mandatory.",
        Key.CUSTOMER_ADDED_SUCCESS to "Customer added successfully",
        Key.CUSTOMER_EXISTING_LINKED to "linked to existing general ledger account",
        Key.CUSTOMER_NEW_OPENED to "Newly opened account successfully registered under code",
        Key.SEARCH_CUSTOMER to "Search customers...",

        Key.VOUCHER_TX_JOURNAL to "Entries & Voucher Logs",
        Key.POSTED to "POSTED",
        Key.DRAFT to "DRAFT",
        Key.UNPOSTED_TIP to "Draft Vouchers do not impact Ledger balances until posted.",
        Key.POST to "Post",
        Key.UNPOST to "Unpost",
        Key.EDIT to "Edit",
        Key.NEW_VOUCHER to "New Voucher",
        Key.ST_JOURNAL to "Journal Entry",
        Key.ST_RECEIPT to "Receipt Voucher",
        Key.ST_PAYMENT to "Payment Voucher",
        Key.DATE to "Date",
        Key.MEMO to "Memo / Notes",
        Key.TOTAL_BASE to "Base Currency Total",
        Key.POST_SUCCESS to "Voucher Posted Successfully!",
        Key.POST_ERROR to "Posting Error",
        Key.CONFIRM_DELETE_VOUCHER to "Delete this draft voucher?",

        Key.VOUCHER_EDITOR to "Double-Entry Voucher Editor",
        Key.VOUCHER_NO to "Voucher ID",
        Key.TOTAL_DEBIT to "Total Debit",
        Key.TOTAL_CREDIT to "Total Credit",
        Key.BALANCED to "Balanced (No Trial Gap)",
        Key.UNBALANCED to "Unbalanced Entry Gap",
        Key.ADD_LINE to "Add Line Item",
        Key.SAVE_DRAFT to "Save Draft Voucher",
        Key.DEBIT to "Debit",
        Key.CREDIT to "Credit",
        Key.EXCHANGE_RATE_LABEL to "Exchange Rate",
        Key.SELECT_ACC to "Select Account",
        Key.ACTIONS to "Actions",

        Key.FINANCIAL_STATEMENTS to "Financial Reports Builder",
        Key.TRIAL_BALANCE to "Trial Balance",
        Key.BALANCE_SHEET to "Balance Sheet",
        Key.INCOME_STATEMENT to "Income Statement",
        Key.START_DATE to "Start Date",
        Key.END_DATE to "End Date",
        Key.RECALCULATE to "Query Periods",
        Key.ACCOUNT to "Account Component",
        Key.CLOSING_DEBIT to "DR Closing",
        Key.CLOSING_CREDIT to "CR Closing",
        Key.TRIAL_BALANCE_BALANCED to "Trial Balance Balanced successfully! No gap.",
        Key.TRIAL_BALANCE_UNBALANCED to "Trial Balance has accounting gap! Please check unposted drafts.",
        Key.VAL_BS_EQUALS to "Validation: Total Assets equal Liabilities + Owners Equity.",
        Key.VAL_BS_NOT_EQUALS to "Warning: Balance Sheet is out of balance. Verify all posted values.",
        Key.RETAINED_EARNINGS to "Retained Earnings (Beginning)",
        Key.REVENUE_MINUS_EXPENSES to "Net Current Period Income",

        Key.COMPLIANCE_SETTINGS to "Compliance Control & Currencies",
        Key.LANGUAGE to "System Language",
        Key.ADD_CURRENCY to "Add Foreign Currency",
        Key.CURRENCY_CODE to "ISO Code (e.g., EUR, SAR)",
        Key.DECIMAL_PLACES to "Decimal Scale Points",
        Key.ADD to "Add Component",
        Key.FISCAL_PERIODS to "Audited Fiscal Periods",
        Key.LOCKED to "Locked & Immutable",
        Key.ACTIVE to "Active Intake",
        Key.START_OF_YEAR to "Fiscal Start Date",
        Key.END_OF_YEAR to "Fiscal End Date",
        Key.LOG_TRAIL_IFRS to "IFRS 100% Audit Logs Trail",
        Key.PERFORMED_BY to "User Profile",
        Key.TIMESTAMP to "System Timestamp",
        Key.DETAILS to "Trace Details Logs",
        Key.CURRENCY_ADDED to "Currency added successfully.",
        Key.FISCAL_ADDED to "Fiscal period registered successfully.",
        Key.LANGUAGE_CHANGED to "System changed to English language mode.",
        Key.ACCOUNTING_MODE to "Accounting System Mode",
        Key.LIBYAN_MODE to "Libyan Accounting Mode (د.ل)",
        Key.NORMAL_MODE to "Standard International Mode (LYD)",
        Key.LIBYAN_MODE_DESC to "Enables formatted figures with the local Libyan Dinar symbol (د.ل) and Libyan practices.",
        Key.VIEW_STATEMENT to "View Account Statement",
        Key.STATEMENT_TITLE to "Account Transactions Ledger",
        Key.RUNNING_BALANCE to "Running Balance",
        Key.CLOSE_STATEMENT to "Close Statement",
        Key.NO_STATEMENT_TXS to "No posted ledger transactions found for this account.",
        Key.THEME_MODE to "Interface Appearance Theme",
        Key.DARK_MODE to "Dark Slate Mode",
        Key.LIGHT_MODE to "Crisp Light Mode",
        Key.THEME_DESC to "Toggle the interface workspace theme between deep modern dark or classic crisp light mode."
    )

    private val arMap = mapOf(
        Key.APP_NAME to "بيئة نظام قيود الحسابات والدفاتر",
        Key.DASHBOARD to "لوحة التحكم",
        Key.ACCOUNTS to "شجرة الحسابات",
        Key.CUSTOMERS to "شاشة العملاء",
        Key.VOUCHERS to "القيود واليومية",
        Key.REPORTS to "التقارير المالية",
        Key.AUDITS to "التدقيق والتهيئة",
        Key.INITIALIZING to "جاري تهيئة قاعدة بيانات الحسابات...",
        Key.SEEDING_PROP to "جاري تحميل شجرة الحسابات والدفتر المساعد للمعيار الدولي IFRS والسنة المالية 2026...",
        
        Key.NAV_DASHBOARD to "الرئيسية",
        Key.NAV_ACCOUNTS to "الحسابات",
        Key.NAV_CUSTOMERS to "العملاء",
        Key.NAV_SUPPLIERS to "الموردين",
        Key.NAV_VOUCHERS to "القيود",
        Key.NAV_REPORTS to "التقارير",
        Key.NAV_SETTINGS to "الإعدادات",

        Key.TOTAL_ASSETS to "إجمالي الأصول",
        Key.TOTAL_LIABILITIES to "إجمالي الالتزامات",
        Key.TOTAL_EQUITY to "إجمالي حقوق الملكية",
        Key.TOTAL_REVENUE to "إجمالي الإيرادات",
        Key.TOTAL_EXPENSES to "إجمالي المصروفات",
        Key.NET_PROFIT to "صافي الربح",
        Key.NET_LOSS to "صافي الخسائر",
        Key.TRIAL_BALANCE_SUMMARY to "ملخص ميزان المراجعة",
        Key.QUICK_ACTIONS to "إجراءات التدقيق السريعة",
        Key.CREATE_VOUCHER to "إنشاء قيد محاسبي جديد",
        Key.VIEW_LEDGER to "معاينة دليل الحسابات",
        Key.PREPARE_REPORTS to "استخراج التقارير المالية",
        Key.AUDIT_SYSTEM_LOGS to "معاينة سجل تدقيق المعايير",
        Key.CURRENCY_OVERVIEW to "العملات المسجلة بالنظام",
        Key.BASE_CURRENCY to "العملة الأساسية",
        Key.EXCHANGE_RATE to "سعر الصرف",
        Key.POSTED_TXS to "القيود المرحلة",
        Key.DRAFT_TXS to "القيود المسودة",

        Key.CHART_OF_ACCOUNTS to "شجرة الحسابات (إيفاد للمعايير الدولية)",
        Key.SUB_ACCOUNTS_FOR to "الحسابات الفرعية لـ",
        Key.ADD_ACCOUNT to "إضافة حساب جديد",
        Key.ADD_SUBACCOUNT to "إضافة حساب فرعي تحت",
        Key.NAME to "الاسم",
        Key.CODE to "رمز الحساب الرقمي",
        Key.TYPE to "نوع التوجيه المحاسبي",
        Key.CURRENCY to "عملة الحساب",
        Key.IS_FOLDER to "هل هو حساب رئيسي (تبويب مجلد)؟",
        Key.CANCEL to "إلغاء",
        Key.CREATE to "حفظ وإنشاء",
        Key.DELETE to "حذف الحساب",
        Key.CONFIRM_DELETE_ACC to "هل أنت متأكد من رغبتك في حذف الحساب المحاسبي التالي:",
        Key.PARENT to "الحساب الأب الرئيسي",
        Key.SURE_DELETE to "تأكيد مسح الحساب",
        Key.SYSTEM_ACC_CANT_DELETE to "لا يمكن حذف هاته الحسابات لأنها حسابات نظام محمية برمجياً.",

        Key.CUSTOMER_MANAGEMENT to "سجل المقبوضات وإدارة حسابات العملاء",
        Key.ADD_NEW_CUSTOMER to "تسجيل ملف عميل جديد",
        Key.CUSTOMER_NAME to "اسم العميل الكامل",
        Key.PHONE to "رقم الهاتف والاتصال",
        Key.EMAIL to "البريد الإلكتروني",
        Key.C_ACCOUNT_LINKING to "إعدادات الربط المالي واستحقاق اليومية",
        Key.AUTO_OPEN_ACCOUNT to "فتح حساب ذو رمز آلي فريد تتبعاً لحساب العملاء الرئيسي (1103000)",
        Key.LINK_EXISTING_MATCH to "ربط ملف العميل بحساب يحمل نفس مطابقة الاسم في شجرة الحسابات",
        Key.LINK_EXISTING to "تحديد حساب مالي موجود مسبقاً بدليل الحسابات...",
        Key.SAVE_CUSTOMER to "تسجيل وحفظ العميل",
        Key.SELECT_EXISTING_COA to "اختر حساب من دليل الحسابات المعتمد",
        Key.NO_CUSTOMERS_FOUND to "لا يوجد عملاء مسجلون حالياً. انقر على الإشارة أسفله لإضافة أول عميل.",
        Key.CUSTOMER_NAME_EMPTY to "يجب كتابة اسم العميل بشكل صحيح ولا يتم قبول الخانات الفارغة.",
        Key.CUSTOMER_ADDED_SUCCESS to "تم إضافة العميل بنجاح تام",
        Key.CUSTOMER_EXISTING_LINKED to "تم ربطه بالحساب المالي المحدد مسبقا بنجاح",
        Key.CUSTOMER_NEW_OPENED to "تم تسجيل ملف العميل برمجياً وفتح حساب فرعي برمز آلي:",
        Key.SEARCH_CUSTOMER to "البحث في قائمة العملاء بالنظام...",

        Key.VOUCHER_TX_JOURNAL to "تحرير الدفاتر وسجلات قيود اليومية للعملاء",
        Key.POSTED to "مُرَحلْ ومحمي",
        Key.DRAFT to "مُسَوّدة",
        Key.UNPOSTED_TIP to "القيود المسودة لا ينتج عنها تسوية حسابية بأرصدة الشجرة لحين اعتماد ترحيلها ماليًا.",
        Key.POST to "ترحيل القيد",
        Key.UNPOST to "إلغاء الترحيل",
        Key.EDIT to "تعديل القيد",
        Key.NEW_VOUCHER to "إضافة قيد قيد يومية",
        Key.ST_JOURNAL to "قيد تسوية يومية",
        Key.ST_RECEIPT to "سند قبض مالي",
        Key.ST_PAYMENT to "سند صرف نقدي",
        Key.DATE to "تاريخ تسجيل المعاملة",
        Key.MEMO to "شرح القيد والمذكرات",
        Key.TOTAL_BASE to "إجمالي المعاملة بالعملة الأساسية",
        Key.POST_SUCCESS to "تم ترحيل قيد اليومية المختار وتجميده بالدفاتر العامة!",
        Key.POST_ERROR to "خلل في قواعد ترحيل السند المالي المكتوب",
        Key.CONFIRM_DELETE_VOUCHER to "هل ترغب في تحديداً بحذف مسودة القيد هاته؟",

        Key.VOUCHER_EDITOR to "لوحة صياغة القيد المحاسبي ومزدوج القيد",
        Key.VOUCHER_NO to "رقم السند/القيد",
        Key.TOTAL_DEBIT to "إجمالي المدين (DR)",
        Key.TOTAL_CREDIT to "إجمالي الدائن (CR)",
        Key.BALANCED to "متوازن (لا غبار محاسبي عليه)",
        Key.UNBALANCED to "القيد المحاسبي غير متوازن! يوجد خلل بالفرق",
        Key.ADD_LINE to "إضافة سطر حسابي فرعي",
        Key.SAVE_DRAFT to "حفظ كمسودة بالدفتر",
        Key.DEBIT to "المدين (DR)",
        Key.CREDIT to "الدائن (CR)",
        Key.EXCHANGE_RATE_LABEL to "سعر صرف العملة الأجنبية",
        Key.SELECT_ACC to "حدد حساب مالي",
        Key.ACTIONS to "خيارات السطر",

        Key.FINANCIAL_STATEMENTS to "مجمع صياغة التقارير المالية والتحليل الإحصائي",
        Key.TRIAL_BALANCE to "ميزان المراجعة بالأرصدة",
        Key.BALANCE_SHEET to "الميزانية العمومية والمركز المحاسبي",
        Key.INCOME_STATEMENT to "قائمة الدخل والأرباح والخسائر",
        Key.START_DATE to "تاريخ البدء المحاسبي",
        Key.END_DATE to "تاريخ الإغلاق المحدد",
        Key.RECALCULATE to "تصفية واستعلام الأرصدة",
        Key.ACCOUNT to "مكون الحساب المالي",
        Key.CLOSING_DEBIT to "مدين نهاية المدة (DR)",
        Key.CLOSING_CREDIT to "دائن نهاية المدة (CR)",
        Key.TRIAL_BALANCE_BALANCED to "ميزان المراجعة متطابق ولا توجد أي فروقات ميزانية!",
        Key.TRIAL_BALANCE_UNBALANCED to "تنبيه: ميزان المراجعة به فروقات! يرجى مراجعة ترحيل مسودات اليومية المعلقة.",
        Key.VAL_BS_EQUALS to "فحص رياضي: الأصول متطابقة ومساوية تماماً للالتزامات + حقوق الملكية العادلة.",
        Key.VAL_BS_NOT_EQUALS to "تنبيه: الميزانية العمومية غير متوازنة! يرجى التحقق من صياغات الإغلاق.",
        Key.RETAINED_EARNINGS to "المرتب الخاص بالأرباح المحتجزة السابقة",
        Key.REVENUE_MINUS_EXPENSES to "صافي دخل الفترة المالية الحالية",

        Key.COMPLIANCE_SETTINGS to "إدارة تهيئة النظام والعملات والتدقيق",
        Key.LANGUAGE to "لغة واجهات النظام (Language)",
        Key.ADD_CURRENCY to "تسجيل عملة أجنبية جديدة",
        Key.CURRENCY_CODE to "رمز العملة العالمي (مثل: EUR, SAR)",
        Key.DECIMAL_PLACES to "رتب الخانات العشرية (الأجزاء)",
        Key.ADD to "إضافة العملة كخيار نشط",
        Key.FISCAL_PERIODS to "الفترات والسنوات المالية الماليّة",
        Key.LOCKED to "مغلق ومدقق (ممنوع التعديل عليه)",
        Key.ACTIVE to "مفتوح ويستقبل معاملات مالية",
        Key.START_OF_YEAR to "تاريخ فتح السنة المالية",
        Key.END_OF_YEAR to "تاريخ نهاية وإقفال الدورة",
        Key.LOG_TRAIL_IFRS to "مسار مدونات التدقيق الإداري IFRS للمراجعين",
        Key.PERFORMED_BY to "اسم منفذ الإجراء",
        Key.TIMESTAMP to "التوقيت الفعلي للعملية",
        Key.DETAILS to "تفاصيل ومسار التدقيق المعتمد",
        Key.CURRENCY_ADDED to "تم تسجيل وإدراج عملة محاسبية إضافية للنظام بنجاح.",
        Key.FISCAL_ADDED to "تم تفعيل وإدراج دورة مالية إضافية بنجاح بنظام الحسابات.",
        Key.LANGUAGE_CHANGED to "تم تغيير ضبط لغة النظام بنجاح إلى اللغة العربية الفصحى.",
        Key.ACCOUNTING_MODE to "نمط النظام المحاسبي للعملة",
        Key.LIBYAN_MODE to "الوضع المحاسبي الليبي المحلي (د.ل)",
        Key.NORMAL_MODE to "الوضع المحاسبي الافتراضي الدولي (LYD)",
        Key.LIBYAN_MODE_DESC to "تفعيل العرض المتوافق مع الدفاتر الليبية باستخدام الرمز المحاسبي د.ل وقواعد الحساب المحلية.",
        Key.VIEW_STATEMENT to "كشف حساب",
        Key.STATEMENT_TITLE to "دفتر أستاذ كشف الحساب والعمليات",
        Key.RUNNING_BALANCE to "الرصيد التراكمي المتبقي",
        Key.CLOSE_STATEMENT to "الرجوع وإغلاق الكشف",
        Key.NO_STATEMENT_TXS to "لا توجد أي قيود أو معاملات مرحلة مسجلة بدفتر الأستاذ لهذا الحساب حالياً.",
        Key.THEME_MODE to "مظهر النظام وهيكل الألوان",
        Key.DARK_MODE to "الوضع الداكن (المظلم)",
        Key.LIGHT_MODE to "الوضع العادي (المضيء)",
        Key.THEME_DESC to "التبديل الفوري بين المظهر الليلي الداكن المريح للأعين أو النمط النهاري المضيء الساطع."
    )

    fun translate(key: Key, lang: String): String {
        return if (lang == "ar") {
            arMap[key] ?: enMap[key] ?: key.name
        } else {
            enMap[key] ?: key.name
        }
    }

    fun getLayoutDirection(lang: String): LayoutDirection {
        return if (lang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
    }

    fun getAccountName(accountCode: String, defaultName: String, lang: String): String {
        if (lang == "ar") {
            return when (accountCode) {
                "1" -> "الأصول"
                "2" -> "الالتزامات"
                "3" -> "حقوق الملكية"
                "4" -> "الإيرادات"
                "5" -> "المصروفات"
                "11" -> "الأصول المتداولة"
                "1101" -> "النقدية بالخزينة (د.ل)"
                "1102" -> "النقدية بالصندوق (USD)"
                "1103" -> "حسابات الذمم والمدينين والعملاء"
                "1103001" -> "شركة المدار الجديد للاتصالات"
                "1103002" -> "شركة ليبيانا للهاتف المحمول"
                "21" -> "الالتزامات المتداولة"
                "2101" -> "حسابات الدائنين والموردين"
                "31" -> "رأس مال الشركاء"
                "32" -> "الأرباح المحتجزة / المدورة"
                "4101" -> "إيرادات المبيعات"
                "4102" -> "إيرادات الخدمات والاستشارات"
                "51" -> "المصروفات التشغيلية والمصاريف العمومية"
                "5101" -> "مصروفات الإيجار"
                "5102" -> "مصروفات الرواتب ومزايا الموظفين"
                "5103" -> "أرباح وخسائر فروقات أسعار الصرف"
                else -> defaultName
            }
        } else {
            return when (accountCode) {
                "1" -> "ASSETS"
                "2" -> "LIABILITIES"
                "3" -> "EQUITY"
                "4" -> "REVENUE"
                "5" -> "EXPENSES"
                "11" -> "Current Assets"
                "1101" -> "Cash on Hand (LYD)"
                "1102" -> "Petty Cash (USD)"
                "1103" -> "Accounts Receivable"
                "1103001" -> "Al-Madar Telecomm"
                "1103002" -> "Libyana Mobile Services"
                "21" -> "Current Liabilities"
                "2101" -> "Accounts Payable"
                "31" -> "Owner's Capital"
                "32" -> "Retained Earnings"
                "4101" -> "Sales Income"
                "4102" -> "Consulting Income"
                "51" -> "Operational Expenses"
                "5101" -> "Rent Expense"
                "5102" -> "Salaries Expense"
                "5103" -> "FX Gain or Loss"
                else -> defaultName
            }
        }
    }

    fun getLocalizedNotification(msg: String, lang: String): String {
        if (lang != "ar") return msg
        val lower = msg.lowercase()
        return when {
            lower.contains("init error") -> msg.replace("Init Error", "خطأ في التهيئة")
            lower.contains("account code and name cannot be blank") -> "رمز الحساب واسمه لا يمكن أن يكونا فارغين."
            lower.contains("successfully created") && lower.contains("account") -> {
                val match = Regex("Account '(.*)' successfully created").find(msg)
                val info = match?.groupValues?.get(1) ?: ""
                "تم إنشاء الحساب '$info' بنجاح."
            }
            lower.contains("creation failed") -> msg.replace("Creation failed", "فشل الإنشاء")
            lower.contains("deleted") && lower.contains("account") -> {
                val match = Regex("Account '(.*)' deleted").find(msg)
                val info = match?.groupValues?.get(1) ?: ""
                "تم حذف الحساب '$info' بنجاح."
            }
            lower.contains("deletion failed") -> msg.replace("Deletion failed", "فشل الحذف")
            lower.contains("posted transactions are immutable") -> "المعاملات المرحلة مجمدة برمجياً ولا يمكن تعديلها."
            lower.contains("must have at least 2 lines") -> "يجب أن يحتوي القيد المحاسبي على سطرين على الأقل."
            lower.contains("voucher code cannot be blank") -> "لا يمكن ترك رقم السند/القيد فارغاً."
            lower.contains("must have a valid account selected") -> "يجب اختيار حساب مالي صحيح لجميع أسطر القيد."
            lower.contains("saved successfully") && lower.contains("draft voucher") -> {
                val match = Regex("Draft voucher #(.*) saved successfully").find(msg)
                val info = match?.groupValues?.get(1) ?: ""
                "تم حفظ مسودة السند #$info بنجاح."
            }
            lower.contains("save failed") -> msg.replace("Save failed", "فشل الحفظ")
            lower.contains("successfully validated, posted") -> "تم التحقق من القيد وترحيله وتجميده في الدفاتر بنجاح!"
            lower.contains("rolled back to draft") -> "تم إلغاء ترحيل السند وإعادته كمسودة."
            lower.contains("unposting error") -> msg.replace("Unposting error", "خطأ أثناء إلغاء الترحيل")
            lower.contains("draft voucher deleted successfully") -> "تم حذف مسودة السند بنجاح."
            lower.contains("deletion error") -> msg.replace("Deletion error", "خطأ في الحذف")
            lower.contains("trial balance report generation failed") -> msg.replace("Trial Balance report generation failed", "فشل إنشاء ميزان المراجعة")
            lower.contains("fiscal period name cannot be blank") -> "اسم الدورة المالية لا يمكن أن يكون فارغاً."
            lower.contains("created successfully") && lower.contains("fiscal period") -> {
                val match = Regex("Fiscal Period '(.*)' created successfully").find(msg)
                val info = match?.groupValues?.get(1) ?: ""
                "تم إنشاء الدورة المالية '$info' بنجاح."
            }
            lower.contains("fiscal year status updated") -> "تم تحديث حالة السنة المالية بنجاح."
            lower.contains("status update failed") -> msg.replace("Status update failed", "فشل تحديث الحالة")
            lower.contains("currency code and name are required") -> "رمز العملة واسمها مطلوبان."
            lower.contains("added successfully") && lower.contains("currency") -> {
                val match = Regex("Currency '(.*)' added successfully").find(msg)
                val info = match?.groupValues?.get(1) ?: ""
                "تم إضافة العملة '$info' بنجاح."
            }
            lower.contains("insertion failed") -> msg.replace("Insertion failed", "فشل الإدراج")
            lower.contains("customer name are required") -> "اسم العميل ومواصفاته مطلوبة."
            lower.contains("fully registered in general ledger") -> {
                val match = Regex("Customer '(.*)' fully registered").find(msg)
                val info = match?.groupValues?.get(1) ?: ""
                "تم تسجيل ملف العميل '$info' وربطه بالدفتر المالي العام بنجاح."
            }
            lower.contains("save customer failed") -> msg.replace("Save customer failed", "فشل تسجيل العميل")
            lower.contains("customer profiling deleted") -> "تم حذف ملف العميل بنجاح."
            else -> msg
        }
    }
}
