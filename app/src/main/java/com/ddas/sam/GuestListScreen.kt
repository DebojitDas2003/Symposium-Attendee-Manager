package com.ddas.sam

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun GuestListScreen(
    viewModel: GuestViewModel = viewModel(),
    onImport: () -> Unit,
    onExport: () -> Unit
) {
    // Observe state flows
    val searchResults by viewModel.searchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val attendeeCount by viewModel.attendeeCount.collectAsState()
    val giftsCount by viewModel.giftsCount.collectAsState()
    val foodCouponsCount by viewModel.foodCouponsCount.collectAsState()
    var showCategoryFilterDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) } // State for Add Guest Dialog
    val yetToAttendCount by viewModel.yetToAttendCount.collectAsState()
    val attendanceFilter by viewModel.attendanceFilter.collectAsState()
    var guestToDelete by remember { mutableStateOf<Guest?>(null) }
    var guestToViewItems by remember { mutableStateOf<Guest?>(null) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    val hardcodedPassword = "GCECTAA2025"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Guest List",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 16.dp)
            )

            Button(
                colors = ButtonDefaults.buttonColors(Color.Red),
                onClick = { showPasswordDialog = true },
                modifier = Modifier

                    .padding(start = 4.dp)
            ) {
                Text("Reset\nGuestList", textAlign = TextAlign.Center)
            }
            Button(
                onClick = { showCategoryFilterDialog = true },
                modifier = Modifier

                    .padding(start = 16.dp)
            ) {
                Text("Filter by Category")
            }
            IconButton (
                onClick = { viewModel.syncGuests() },
                modifier = Modifier
                    .padding(start = 16.dp)
            ) {
                Icon(Icons.Sharp.Refresh, contentDescription = "sync")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AttendanceFilterButton(
                label = "All",
                selected = attendanceFilter == GuestViewModel.AttendanceFilter.All,
                onClick = { viewModel.updateAttendanceFilter(GuestViewModel.AttendanceFilter.All) }
            )
            AttendanceFilterButton(
                label = "Present",
                selected = attendanceFilter == GuestViewModel.AttendanceFilter.Present,
                onClick = { viewModel.updateAttendanceFilter(GuestViewModel.AttendanceFilter.Present) }
            )
            AttendanceFilterButton(
                label = "Yet to Attend",
                selected = attendanceFilter == GuestViewModel.AttendanceFilter.YetToAttend,
                onClick = { viewModel.updateAttendanceFilter(GuestViewModel.AttendanceFilter.YetToAttend) }
            )
        }


        // Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            label = { Text("Search Guests") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        // LazyColumn for displaying guests based on search results
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .horizontalScroll(rememberScrollState()),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(searchResults) { guest ->
                GuestRow(
                    guest = guest,
                    onEdit = { viewModel.updateGuest(it) },
                    onDelete = { guestToDelete = it }, // Set guest to be deleted
                    onClick = { guestToViewItems = guest }
                )
            }
        }

        guestToViewItems?.let { guest ->
            GuestItemsDialog(
                guest = guest,
                onDismiss = { guestToViewItems = null }
            )
        }

        // Confirmation Dialog for Deleting a Guest
        guestToDelete?.let { guest ->
            AlertDialog(
                onDismissRequest = { guestToDelete = null },
                title = { Text("Delete Guest") },
                text = { Text("Are you sure you want to delete ${guest.name}? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteGuest(guest) // Call ViewModel to delete guest
                            guestToDelete = null // Clear the dialog
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    Button(onClick = { guestToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Category Filter Dialog
        if (showCategoryFilterDialog) {
            CategoryFilterDialog(
                currentCategory = viewModel.selectedCategory.collectAsState().value,
                onCategorySelected = {
                    viewModel.updateSelectedCategory(it)
                    showCategoryFilterDialog = false
                },
                onDismiss = { showCategoryFilterDialog = false }
            )
        }

        // Summary counts at the bottom
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Attendees: $attendeeCount")
            Text("Yet to Attend: $yetToAttendCount")
            Text("Gifts: $giftsCount")
            Text("Food Coupons: $foodCouponsCount")
        }

        val categoryCount by viewModel.categoryCounts.collectAsState()
        val groupedCategories = categoryCount.entries.chunked(3)
        groupedCategories.forEach { group ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                group.forEach { (category, count) ->
                    Text("$category: $count")
                }

                // Add empty space to balance the row if there are fewer than 3 items

            }
        }

        // Buttons for importing, exporting, and adding guests
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onImport,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            ) {
                Text("Upload\nDocument", textAlign = TextAlign.Center)
            }
            Button(
                onClick = onExport,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                Text("Download\nData", textAlign = TextAlign.Center)
            }
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            ) {
                Text("Add\nGuest", textAlign = TextAlign.Center)
            }
        }

        // Add Guest Dialog
        if (showAddDialog) {
            AddGuestDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { newGuest ->
                    viewModel.addGuest(newGuest) // Add the new guest to ViewModel
                    showAddDialog = false
                }
            )
        }
        if (showPasswordDialog) {
            AlertDialog(
                onDismissRequest = { showPasswordDialog = false },
                title = { Text("Enter Password") },
                text = {
                    TextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (passwordInput == hardcodedPassword) {
                                viewModel.resetFirebase()
                                showPasswordDialog = false
                                passwordInput = ""
                            }
                        }
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    Button(onClick = { showPasswordDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}



@Composable
fun CategoryFilterDialog(
    currentCategory: String,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val categories = listOf("All", "Delegate", "College Invitee", "VIP", "Student")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Category") },
        text = {
            Column(modifier = Modifier.padding(16.dp)) {
                categories.forEach { category ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = category == currentCategory,
                            onClick = { onCategorySelected(category) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(category)
                    }
                }
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
fun AttendanceFilterButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
        ),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(text = label)
    }
}

@Composable
fun DeleteGuestConfirmationDialog(
    guest: Guest,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Guest") },
        text = { Text("Are you sure you want to delete ${guest.name}? This action cannot be undone.") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Delete")
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
fun GuestItemsDialog(
    guest: Guest,
    onDismiss: () -> Unit
) {
    val itemsReceived = mutableListOf<String>()

    // Add items based on the guest's attributes
    if (guest.hasLanyard) itemsReceived.add("Lanyard")
    if (guest.hasGift) itemsReceived.add("Gift")
    if (guest.hasFoodCoupon) itemsReceived.add("Food Coupon")

    // If there are no items, display a message
    if (itemsReceived.isEmpty()) {
        itemsReceived.add("No items received")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Items Received by\n${guest.name}") },
        text = {
            Column {
                itemsReceived.forEach { item ->
                    Text(text = "- $item")
                }
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
fun PasswordDialog(
    onDismiss: () -> Unit,
    onPasswordCorrect: () -> Unit,
    hardcodedPassword: String = "guestreset123"
) {
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Password to Reset Guest List") },
        text = {
            Column {
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password == hardcodedPassword) {
                        onPasswordCorrect()
                        onDismiss()
                    } else {
                        errorMessage = "Incorrect password. Please try again."
                    }
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
