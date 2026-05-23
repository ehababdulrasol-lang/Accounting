package com.example.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DatabaseBackupHelper {
    private const val DB_NAME = "ledger_pro_database"

    /**
     * Creates a local named backup (with date & time) inside context.getExternalFilesDir("backups")
     */
    fun backupDatabaseLocal(context: Context): File? {
        try {
            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) return null

            val db = AppDatabase.getDatabase(context)
            // Run a checkpoint to merge WAL journal before backing up
            try {
                db.openHelper.writableDatabase.query("PRAGMA checkpoint(FULL)")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            db.close()
            AppDatabase.resetInstance()

            val backupDir = File(context.getExternalFilesDir(null), "backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(backupDir, "ledger_backup_$timeStamp.db")

            copyFile(dbFile, backupFile)

            // Copy auxiliary journal/wal files if they exist
            val shmFile = File(dbFile.path + "-shm")
            if (shmFile.exists()) {
                copyFile(shmFile, File(backupFile.path + "-shm"))
            }
            val walFile = File(dbFile.path + "-wal")
            if (walFile.exists()) {
                copyFile(walFile, File(backupFile.path + "-wal"))
            }

            return backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Restores a local backup from the backup folder
     */
    fun restoreDatabaseLocal(context: Context, backupFile: File): Boolean {
        try {
            val dbFile = context.getDatabasePath(DB_NAME)

            val db = AppDatabase.getDatabase(context)
            db.close()
            AppDatabase.resetInstance()

            deleteDatabaseFiles(context)

            copyFile(backupFile, dbFile)

            val backupShm = File(backupFile.path + "-shm")
            if (backupShm.exists()) {
                copyFile(backupShm, File(dbFile.path + "-shm"))
            }

            val backupWal = File(backupFile.path + "-wal")
            if (backupWal.exists()) {
                copyFile(backupWal, File(dbFile.path + "-wal"))
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Gets a list of all local backups
     */
    fun getLocalBackupFiles(context: Context): List<File> {
        val backupDir = File(context.getExternalFilesDir(null), "backups")
        if (!backupDir.exists()) return emptyList()
        return backupDir.listFiles { file -> file.isFile && file.name.endsWith(".db") }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * Deletes a local backup file
     */
    fun deleteLocalBackup(backupFile: File): Boolean {
        try {
            if (backupFile.exists()) {
                backupFile.delete()
            }
            val shm = File(backupFile.path + "-shm")
            if (shm.exists()) shm.delete()
            val wal = File(backupFile.path + "-wal")
            if (wal.exists()) wal.delete()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Exports database file to any Uri chosen by Storage Access Framework
     */
    fun exportDatabaseToUri(context: Context, uri: Uri): Boolean {
        try {
            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) return false

            val db = AppDatabase.getDatabase(context)
            try {
                db.openHelper.writableDatabase.query("PRAGMA checkpoint(FULL)")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            db.close()
            AppDatabase.resetInstance()

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                FileInputStream(dbFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return false
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Imports database file from any Uri chosen by Storage Access Framework
     */
    fun importDatabaseFromUri(context: Context, uri: Uri): Boolean {
        try {
            val dbFile = context.getDatabasePath(DB_NAME)

            val db = AppDatabase.getDatabase(context)
            db.close()
            AppDatabase.resetInstance()

            deleteDatabaseFiles(context)

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(dbFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return false

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun deleteDatabaseFiles(context: Context) {
        val dbFile = context.getDatabasePath(DB_NAME)
        if (dbFile.exists()) dbFile.delete()

        val shmFile = File(dbFile.path + "-shm")
        if (shmFile.exists()) shmFile.delete()

        val walFile = File(dbFile.path + "-wal")
        if (walFile.exists()) walFile.delete()
    }

    private fun copyFile(src: File, dst: File) {
        FileInputStream(src).use { inStream ->
            FileOutputStream(dst).use { outStream ->
                inStream.copyTo(outStream)
            }
        }
    }
}
