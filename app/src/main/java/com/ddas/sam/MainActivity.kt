package com.ddas.sam

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.launch
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import java.io.IOException

class MainActivity : ComponentActivity() {
    private lateinit var guestDao: GuestDao
    private lateinit var guestRepository: GuestRepository
    private lateinit var viewModel: GuestViewModel
    private lateinit var importExcelLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        // Initialize the database and repository
        val guestDatabase = GuestDatabase.getDatabase(applicationContext)
        guestDao = guestDatabase.guestDao()
        guestRepository = GuestRepository(guestDao)

        // Initialize ViewModel
        viewModel = ViewModelProvider(
            this,
            GuestViewModelFactory(guestRepository)
        ).get(GuestViewModel::class.java)

        // File picker launcher
        importExcelLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                uri?.let { importExcelData(it) }
            }
        }

        setContent {
            GuestListScreen(
                viewModel = viewModel,
                onImport = { launchFilePicker() },
                onExport = { exportExcelData() }
            )
        }

        // Trigger initial sync
        lifecycleScope.launch {
            viewModel.syncGuests()
        }
    }

    private fun importExcelData(uri: Uri) {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            try {
                val workbook = WorkbookFactory.create(inputStream)
                val sheet = workbook.getSheetAt(0)

                val guestList = mutableListOf<Guest>()

                // Skip the header row
                for (row in sheet.drop(1)) {
                    val name = row.getCell(0)?.let {
                        when (it.cellType) {
                            CellType.STRING -> it.stringCellValue
                            CellType.NUMERIC -> it.numericCellValue.toString()
                            else -> ""
                        }
                    } ?: ""

                    val phoneNumber = row.getCell(1)?.let {
                        when (it.cellType) {
                            CellType.STRING -> it.stringCellValue
                            CellType.NUMERIC -> it.numericCellValue.toLong().toString()
                            else -> ""
                        }
                    } ?: ""

                    val email = row.getCell(2)?.let {
                        when (it.cellType) {
                            CellType.STRING -> it.stringCellValue
                            CellType.NUMERIC -> it.numericCellValue.toString()
                            else -> ""
                        }
                    } ?: ""

                    val companyName = row.getCell(3)?.let {
                        when (it.cellType) {
                            CellType.STRING -> it.stringCellValue
                            CellType.NUMERIC -> it.numericCellValue.toString()
                            else -> ""
                        }
                    } ?: ""

                    val guest = Guest(
                        name = name,
                        email = email,
                        phoneNumber = phoneNumber,
                        companyName = companyName,
                    )
                    guestList.add(guest)
                }

                lifecycleScope.launch {
                    guestList.forEach { viewModel.addGuest(it) }
                    viewModel.syncGuests() // Ensure ViewModel is updated
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to import data", Toast.LENGTH_SHORT).show()
            }
        } ?: Toast.makeText(this, "Invalid file selected", Toast.LENGTH_SHORT).show()
    }

    private fun exportExcelData() {
        val fileName = "GuestList_${System.currentTimeMillis()}.xlsx"

        // Use the app-specific external files directory for downloads
        val downloadsDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (downloadsDir == null) {
            Toast.makeText(
                this@MainActivity,
                "Unable to access external storage",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        val file = File(downloadsDir, fileName)

        lifecycleScope.launch {
            val guestList = viewModel.guests.value

            try {
                WorkbookFactory.create(true).use { workbook ->
                    val sheet = workbook.createSheet("Guests")
                    val headerRow = sheet.createRow(0)
                    val headers = listOf(
                        "Name",
                        "Email",
                        "Phone Number",
                        "Company Name",
                        "Category",
                        "Amount",
                        "Remarks",
                        "Attendance",
                        "Lanyard",
                        "Gift",
                        "Food Coupon"
                    )

                    headers.forEachIndexed { index, header ->
                        headerRow.createCell(index).setCellValue(header)
                    }

                    guestList.forEachIndexed { index, guest ->
                        val row = sheet.createRow(index + 1)
                        row.createCell(0).setCellValue(guest.name)
                        row.createCell(1).setCellValue(guest.email)
                        row.createCell(2).setCellValue(guest.phoneNumber)
                        row.createCell(3).setCellValue(guest.companyName)
                        row.createCell(4).setCellValue(guest.category)
                        row.createCell(5).setCellValue(guest.amount)
                        row.createCell(6).setCellValue(guest.remarks)
                        row.createCell(7).setCellValue(if (guest.attending) "Yes" else "No")
                        row.createCell(8).setCellValue(if (guest.hasLanyard) "Yes" else "No")
                        row.createCell(9).setCellValue(if (guest.hasGift) "Yes" else "No")
                        row.createCell(10).setCellValue(if (guest.hasFoodCoupon) "Yes" else "No")
                    }

                    file.outputStream().use { outputStream ->
                        workbook.write(outputStream)
                    }

                    Log.d("ExcelExport", "File saved at: ${file.absolutePath}")

                    Toast.makeText(
                        this@MainActivity,
                        "Exported to ${file.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()

                    // Open the saved file
                    openExcelFile(file)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Failed to export data", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun openExcelFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)


            Log.d("ExcelOpen", "File exists: ${file.exists()}")
            Log.d("ExcelOpen", "File path: ${file.absolutePath}")
            Log.d("ExcelOpen", "File size: ${file.length()}")
            Log.d("ExcelOpen", "URI: $uri")


            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_ACTIVITY_NO_HISTORY

                // Add multiple MIME types that Excel files might use
                val mimeTypes = arrayOf(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx
                    "application/vnd.ms-excel", // .xls
                    "application/excel",
                    "application/x-excel",
                    "application/x-msexcel"
                )

                setDataAndType(uri, mimeTypes[0])

                // Add alternatives MIME types
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            }

            try {
                startActivity(openIntent)
            } catch (e: Exception) {
                Log.e("ExcelOpen", "Error opening file: ${e.message}")
                Toast.makeText(this, "Error opening file: ${e.message}", Toast.LENGTH_LONG).show()

                // Fallback: try to open with a more generic intent
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    startActivity(Intent.createChooser(fallbackIntent, "Open with..."))
                } catch (e: Exception) {
                    Toast.makeText(this, "No app found to open the file", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("ExcelOpen", "Error getting URI: ${e.message}")
            e.printStackTrace()
        }

    }

    private fun shareExcelFile(file: File) {
        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        if (shareIntent.resolveActivity(packageManager) != null) {
            startActivity(Intent.createChooser(shareIntent, "Share Excel File"))
        } else {
            Toast.makeText(this, "No app found to share the file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        }
        importExcelLauncher.launch(intent)
    }
}
