package com.ddas.sam

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp



@Composable
fun GuestRow(
    guest: Guest,
    onEdit: (Guest) -> Unit,
    onDelete: (Guest) -> Unit,
    onClick: () -> Unit
) {
    var showItemsDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = { showItemsDialog = true }, modifier = Modifier.width(60.dp)) {
            Icon(Icons.Filled.Star, contentDescription = "Items")
        }

        Text(guest.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(100.dp))
        Text(guest.email, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(150.dp))
        Text(guest.phoneNumber, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(120.dp))
        Text(guest.companyName, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(120.dp))

        Button(onClick = { showEditDialog = true }, modifier = Modifier.width(60.dp)) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit")
        }
        Button(
            onClick = { onDelete(guest) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Delete")
        }
    }

    if (showItemsDialog) {
        ShowItemsDialog(guest, onEdit, onDismiss = { showItemsDialog = false })
    }

    if (showEditDialog) {
        ShowEditDialog(guest, onEdit, onDismiss = { showEditDialog = false })
    }
}


@Composable
fun ShowItemsDialog(
    guest: Guest,
    onEdit: (Guest) -> Unit,
    onDismiss: () -> Unit
) {
    // Local state to manage checkbox values
    var attendance by remember { mutableStateOf(guest.attending) }
    var lanyard by remember { mutableStateOf(guest.hasLanyard) }
    var gift by remember { mutableStateOf(guest.hasGift) }
    var foodCoupon by remember { mutableStateOf(guest.hasFoodCoupon) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Items Received") },
        text = {
            Column(modifier = Modifier.padding(16.dp)) {
                CheckboxRow(
                    label = "Attendance",
                    checked = attendance,
                    onCheckedChange = {
                        attendance = it
                        onEdit(guest.copy(attending = attendance))
                    }
                )
                CheckboxRow(
                    label = "Lanyard",
                    checked = lanyard,
                    onCheckedChange = {
                        lanyard = it
                        onEdit(guest.copy(hasLanyard = lanyard))
                    }
                )
                CheckboxRow(
                    label = "Gift",
                    checked = gift,
                    onCheckedChange = {
                        gift = it
                        onEdit(guest.copy(hasGift = gift))
                    }
                )
                CheckboxRow(
                    label = "Food Coupon",
                    checked = foodCoupon,
                    onCheckedChange = {
                        foodCoupon = it
                        onEdit(guest.copy(hasFoodCoupon = foodCoupon))
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}


@Composable
fun ShowEditDialog(guest: Guest, onEdit: (Guest) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(TextFieldValue(guest.name)) }
    var email by remember { mutableStateOf(TextFieldValue(guest.email)) }
    var phoneNumber by remember { mutableStateOf(TextFieldValue(guest.phoneNumber)) }
    var companyName by remember { mutableStateOf(TextFieldValue(guest.companyName)) }
    var remarks by remember { mutableStateOf(TextFieldValue(guest.remarks ?: "")) }
    var amount by remember { mutableStateOf(TextFieldValue(guest.amount ?: "")) }

    var category by remember { mutableStateOf(guest.category ?: "None") }
    var paymentMode by remember { mutableStateOf(guest.paymentMode ?: "None") }
    var showToast by remember { mutableStateOf(false) }

    if (showToast) {
        // Display Toast when showToast is true
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            Toast.makeText(context, "Name is required to edit the guest", Toast.LENGTH_SHORT).show()
            showToast = false // Reset the state to prevent repeated Toasts
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Guest") },
        text = {
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = phoneNumber, onValueChange = { phoneNumber = it }, label = { Text("Phone Number") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = companyName, onValueChange = { companyName = it }, label = { Text("Company Name") })
                Spacer(modifier = Modifier.height(8.dp))
                Text("Category", style = MaterialTheme.typography.bodyMedium)
                Column {
                    RadioButtonGroup(
                        options = listOf("None", "Delegate", "College Invitee", "VIP", "Student"),
                        selectedOption = category,
                        onOptionSelected = { category = it }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = remarks, onValueChange = { remarks = it }, label = { Text("Remarks") })
                Spacer(modifier = Modifier.height(8.dp))
                Text("Payment Mode", style = MaterialTheme.typography.bodyMedium)
                Column {
                    RadioButtonGroup(
                        options = listOf("None", "Online", "Offline", "Due"),
                        selectedOption = paymentMode,
                        onOptionSelected = { paymentMode = it }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    onValueChange = { amount = it },
                    label = { Text("Amount") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.text.isNotBlank()) { // Only the name is required
                    onEdit(
                        guest.copy(
                            name = name.text,
                            email = email.text,
                            phoneNumber = phoneNumber.text,
                            companyName = companyName.text,
                            remarks = remarks.text,
                            paymentMode = paymentMode,
                            category = category,
                            amount = amount.text.ifBlank { "0" } // Default to "0" if empty
                        )
                    )
                    onDismiss() // Close the dialog after editing
                } else {
                    showToast = true // Trigger Toast
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@Composable
fun CheckboxRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}



@Composable
fun AddGuestDialog(onDismiss: () -> Unit, onAdd: (Guest) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("None") }
    var remarks by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("None") }
    var amount by remember { mutableStateOf("") }
    var showToast by remember { mutableStateOf(false) }

    if (showToast) {
        // Display Toast when showToast is true
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            Toast.makeText(context, "Name is required to add a guest", Toast.LENGTH_SHORT).show()
            showToast = false // Reset the state to prevent repeated Toasts
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Guest") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Company Name") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Category", style = MaterialTheme.typography.bodyMedium)
                Column {
                    RadioButtonGroup(
                        options = listOf("None","Delegate", "College Invitee", "VIP", "Student"),
                        selectedOption = category,
                        onOptionSelected = { category = it }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Payment Mode", style = MaterialTheme.typography.bodyMedium)
                Column {
                    RadioButtonGroup(
                        options = listOf("None","Online", "Offline", "Due"),
                        selectedOption = paymentMode,
                        onOptionSelected = { paymentMode = it }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) { // Only the name is required
                        onAdd(
                            Guest(
                                name = name,
                                email = email.ifBlank { "" },
                                phoneNumber = phoneNumber.ifBlank { "" },
                                companyName = companyName.ifBlank { "" },
                                remarks = remarks.ifBlank { "" },
                                paymentMode = paymentMode,
                                amount = amount.ifBlank { "0" }, // Default to "0" if empty
                            )
                        )
                        onDismiss() // Close the dialog after adding
                    } else {
                        showToast = true // Trigger Toast
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@Composable
fun RadioButtonGroup(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    Column {
        options.forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(
                    selected = option == selectedOption,
                    onClick = { onOptionSelected(option) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(option, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

