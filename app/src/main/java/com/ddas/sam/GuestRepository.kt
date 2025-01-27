package com.ddas.sam

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class GuestRepository(private val guestDao: GuestDao) {
    private val firestore = FirebaseFirestore.getInstance().apply {
        // Enable offline persistence
        firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
    }
    private val guestsCollection = firestore.collection("guests")

    fun getAllGuests(): Flow<List<Guest>> = guestDao.getAllGuests()

    suspend fun syncGuests() {
        val localGuests = guestDao.getAllGuestsOnce()
        val firestoreGuests = guestsCollection.get().await().documents.mapNotNull { it.toObject<Guest>() }

        val batch = firestore.batch()

        // Log fetched guests
        println("Local Guests: ${localGuests.size}, Firestore Guests: ${firestoreGuests.size}")

        // Upload local changes
        localGuests.forEach { guest ->
            if (guest.name.isNotBlank()) {
                val docRef = guestsCollection.document(validateGuestId(guest.name))
                batch.set(docRef, guest)
            }
        }

        // Commit batch update
        batch.commit().await()

        // Handle guests from Firestore
        firestoreGuests.forEach { guest ->
            if (guest.name.isNotBlank()) {
                when {
                    guest.deleted -> {
                        // Delete locally if marked as deleted in Firestore
                        guestDao.deleteGuest(guest)
                    }
                    guestDao.getGuestByName(guest.name) == null -> {
                        // Insert missing guests
                        guestDao.insertGuest(guest)
                    }
                }
            }
        }
    }

    suspend fun insertGuest(guest: Guest) {
        if (guest.name.isNotBlank()) {
            val guestWithId = guest.copy(name = validateGuestId(guest.name))
            guestDao.insertGuest(guestWithId)
            syncWithFirestore(guestWithId)
        } else {
            println("Error: Guest name is blank, skipping insert.")
        }
    }

    suspend fun updateGuest(guest: Guest) {
        if (guest.name.isNotBlank()) {
            val guestWithId = guest.copy(name = validateGuestId(guest.name))
            guestDao.updateGuest(guestWithId)
            syncWithFirestore(guestWithId)
        } else {
            println("Error: Guest name is blank, skipping update.")
        }
    }

    suspend fun getGuestByName(name: String): Guest? = guestDao.getGuestByName(name.trim())

    suspend fun deleteGuest(guest: Guest) {
        if (guest.name.isNotBlank()) {
            // Mark guest as deleted locally
            val markedGuest = guest.copy(deleted = true)
            guestDao.updateGuest(markedGuest)
            syncWithFirestore(markedGuest) // Update Firestore with the deleted flag
        } else {
            println("Error: Guest name is blank, skipping delete.")
        }
    }

    private fun observeFirestoreChanges(): Flow<List<Guest>> = callbackFlow {
        val listenerRegistration = guestsCollection.addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                close(exception)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val updatedGuests = snapshot.documents.mapNotNull { it.toObject<Guest>() }
                    .filter { it.name.isNotBlank() } // Filter out invalid guests
                trySend(updatedGuests)
            }
        }
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun startRealtimeSync() {
        observeFirestoreChanges().collect { firestoreGuests ->
            firestoreGuests.forEach { guest ->
                if (guest.name.isNotBlank()) {
                    val localGuest = guestDao.getGuestByName(guest.name)
                    when {
                        guest.deleted -> {
                            // Delete locally if marked as deleted in Firestore
                            if (localGuest != null) {
                                guestDao.deleteGuest(localGuest)
                            }
                        }
                        localGuest == null -> {
                            // Insert new guest if it doesn't exist locally
                            guestDao.insertGuest(guest)
                        }
                        else -> {
                            // Update existing guest
                            guestDao.updateGuest(guest)
                        }
                    }
                }
            }
        }
    }

    private suspend fun syncWithFirestore(guest: Guest) {
        if (guest.name.isNotBlank()) {
            guestsCollection.document(validateGuestId(guest.name)).set(guest).await()
        } else {
            println("Error: Guest name is blank, skipping Firestore sync.")
        }
    }

    suspend fun clearFirebaseData() {
        val batch = firestore.batch()
        val documents = guestsCollection.get().await().documents
        documents.forEach { doc ->
            batch.delete(doc.reference)
        }
        batch.commit().await()
    }

    suspend fun clearLocalDatabase() {
        guestDao.clearAllGuests()
    }

    private fun validateGuestId(name: String): String {
        // Trim whitespace and handle blank names
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return "guest-${System.currentTimeMillis()}"
        }
        return trimmedName.lowercase() // Use lowercase for consistent ID generation
    }
}