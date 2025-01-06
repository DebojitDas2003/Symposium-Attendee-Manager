package com.ddas.sam

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
        viewModel = ViewModelProvider(this, GuestViewModelFactory(guestRepository)).get(GuestViewModel::class.java)

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
        val downloadsDir = getExternalFilesDir(null)?.absolutePath
        val file = File(downloadsDir, fileName)

        lifecycleScope.launch {
            val guestList = viewModel.guests.value

            try {
                WorkbookFactory.create(true).use { workbook ->
                    val sheet = workbook.createSheet("Guests")
                    val headerRow = sheet.createRow(0)
                    val headers = listOf("Name", "Email", "Phone Number", "Company Name","Category","Amount", "Remarks", "Attendance", "Lanyard", "Gift", "Food Coupon")

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

                    Toast.makeText(this@MainActivity, "Exported to ${file.absolutePath}", Toast.LENGTH_LONG).show()
                    shareExcelFile(file)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Failed to export data", Toast.LENGTH_SHORT).show()
            }
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
