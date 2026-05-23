package com.example.data

import android.util.Log

object DatabaseSeeder {

    suspend fun seedIfEmpty(db: AppDatabase) {
        val currencyDao = db.currencyDao()
        val fiscalYearDao = db.fiscalYearDao()
        val accountDao = db.accountDao()
        val auditLogDao = db.auditLogDao()

        // 1. Seed currencies if none exist
        val baseCurrency = currencyDao.getBaseCurrency()
        if (baseCurrency != null) {
            // Already seeded!
            return
        }

        Log.d("DatabaseSeeder", "Seeding database with professional financial defaults...")

        // Seed Currencies
        val lydId = currencyDao.insert(
            Currency(code = "LYD", name = "Libyan Dinar (Base)", isBase = true, decimalPlaces = 3)
        )
        val usdId = currencyDao.insert(
            Currency(code = "USD", name = "US Dollar", isBase = false, decimalPlaces = 2)
        )

        // Seed Fiscal Year 2026 (Current year)
        // 2026-01-01 00:00:00 UTC = 1767225600000
        // 2026-12-31 23:59:59 UTC = 1798761599000
        val fy2026Id = fiscalYearDao.insert(
            FiscalYear(
                name = "FY 2026",
                startDate = 1767225600000L,
                endDate = 1798761599000L,
                isLocked = false
            )
        )

        // Seed Chart of Accounts
        // Level 1 Parents (Groups)
        val assetsPid = accountDao.insert(
            Account(accountCode = "1", name = "ASSETS", parentId = null, accountType = AccountType.ASSET, currencyId = lydId, isGroup = true)
        )
        val liabilitiesPid = accountDao.insert(
            Account(accountCode = "2", name = "LIABILITIES", parentId = null, accountType = AccountType.LIABILITY, currencyId = lydId, isGroup = true)
        )
        val equityPid = accountDao.insert(
            Account(accountCode = "3", name = "EQUITY", parentId = null, accountType = AccountType.EQUITY, currencyId = lydId, isGroup = true)
        )
        val revenuePid = accountDao.insert(
            Account(accountCode = "4", name = "REVENUE", parentId = null, accountType = AccountType.REVENUE, currencyId = lydId, isGroup = true)
        )
        val expensesPid = accountDao.insert(
            Account(accountCode = "5", name = "EXPENSES", parentId = null, accountType = AccountType.EXPENSE, currencyId = lydId, isGroup = true)
        )

        // Level 2 Subgroups & Accounts
        // Under Assets
        val currentAssetsId = accountDao.insert(
            Account(accountCode = "11", name = "Current Assets", parentId = assetsPid, accountType = AccountType.ASSET, currencyId = lydId, isGroup = true)
        )
        val cashOnHandId = accountDao.insert(
            Account(accountCode = "1101", name = "Cash on Hand (LYD)", parentId = currentAssetsId, accountType = AccountType.ASSET, currencyId = lydId, isGroup = false)
        )
        val pettyCashUsdId = accountDao.insert(
            Account(accountCode = "1102", name = "Petty Cash (USD)", parentId = currentAssetsId, accountType = AccountType.ASSET, currencyId = usdId, isGroup = false)
        )
        val accountsReceivableId = accountDao.insert(
            Account(accountCode = "1103", name = "Accounts Receivable", parentId = currentAssetsId, accountType = AccountType.ASSET, currencyId = lydId, isGroup = true, isSystemAccount = true)
        )
        // Debtors under Accounts Receivable
        accountDao.insert(
            Account(accountCode = "1103001", name = "Al-Madar Telecomm", parentId = accountsReceivableId, accountType = AccountType.ASSET, currencyId = lydId, isGroup = false)
        )
        accountDao.insert(
            Account(accountCode = "1103002", name = "Libyana Mobile Services", parentId = accountsReceivableId, accountType = AccountType.ASSET, currencyId = lydId, isGroup = false)
        )

        // Under Liabilities
        val currentLiabilitiesId = accountDao.insert(
            Account(accountCode = "21", name = "Current Liabilities", parentId = liabilitiesPid, accountType = AccountType.LIABILITY, currencyId = lydId, isGroup = true)
        )
        accountDao.insert(
            Account(accountCode = "2101", name = "Accounts Payable", parentId = currentLiabilitiesId, accountType = AccountType.LIABILITY, currencyId = lydId, isGroup = true)
        )

        // Under Equity
        accountDao.insert(
            Account(accountCode = "31", name = "Owner's Capital", parentId = equityPid, accountType = AccountType.EQUITY, currencyId = lydId, isGroup = false)
        )
        accountDao.insert(
            Account(accountCode = "32", name = "Retained Earnings", parentId = equityPid, accountType = AccountType.EQUITY, currencyId = lydId, isGroup = false, isSystemAccount = true)
        )

        // Under Revenue
        accountDao.insert(
            Account(accountCode = "4101", name = "Sales Income", parentId = revenuePid, accountType = AccountType.REVENUE, currencyId = lydId, isGroup = false)
        )
        accountDao.insert(
            Account(accountCode = "4102", name = "Consulting Income", parentId = revenuePid, accountType = AccountType.REVENUE, currencyId = lydId, isGroup = false)
        )

        // Under Expenses
        val operationalExpensesId = accountDao.insert(
            Account(accountCode = "51", name = "Operational Expenses", parentId = expensesPid, accountType = AccountType.EXPENSE, currencyId = lydId, isGroup = true)
        )
        accountDao.insert(
            Account(accountCode = "5101", name = "Rent Expense", parentId = operationalExpensesId, accountType = AccountType.EXPENSE, currencyId = lydId, isGroup = false)
        )
        accountDao.insert(
            Account(accountCode = "5102", name = "Salaries Expense", parentId = operationalExpensesId, accountType = AccountType.EXPENSE, currencyId = lydId, isGroup = false)
        )
        accountDao.insert(
            Account(accountCode = "5103", name = "FX Gain or Loss", parentId = operationalExpensesId, accountType = AccountType.EXPENSE, currencyId = lydId, isGroup = false, isSystemAccount = true)
        )

        // Log initiation in Audit Logs
        auditLogDao.insert(
            AuditLog(
                voucherId = 0,
                voucherNo = "SYSTEM",
                action = "SYSTEM_INIT",
                details = "Enterprise Chart of Accounts successfully initialized under IFRS compliance. Default Base currency LYD (3 decimals) and secondary USD configured. Fiscal Year 2026 successfully opened."
            )
        )
        
        Log.d("DatabaseSeeder", "Seeding complete.")
    }
}
