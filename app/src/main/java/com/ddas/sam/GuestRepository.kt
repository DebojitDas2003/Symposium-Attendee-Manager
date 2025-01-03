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

        // Upload local changes (including deleted guests)
        localGuests.forEach { guest ->
            val docRef = guestsCollection.document(guest.name)
            batch.set(docRef, guest)
        }

        // Commit batch update
        batch.commit().await()

        // Handle guests from Firestore
        firestoreGuests.forEach { guest ->
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

    suspend fun insertGuest(guest: Guest) {
        guestDao.insertGuest(guest)
        syncWithFirestore(guest)
    }

    suspend fun updateGuest(guest: Guest) {
        guestDao.updateGuest(guest)
        syncWithFirestore(guest)
    }

    suspend fun getGuestByName(name: String): Guest? = guestDao.getGuestByName(name)

    suspend fun deleteGuest(guest: Guest) {
        // Mark guest as deleted locally
        val markedGuest = guest.copy(deleted = true)
        guestDao.updateGuest(markedGuest)
        syncWithFirestore(markedGuest) // Update Firestore with the deleted flag
    }

    private fun observeFirestoreChanges(): Flow<List<Guest>> = callbackFlow {
        val listenerRegistration = guestsCollection.addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                close(exception)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val updatedGuests = snapshot.documents.mapNotNull { it.toObject<Guest>() }
                trySend(updatedGuests)
            }
        }
        awaitClose { listenerRegistration.remove() }
    }


    suspend fun startRealtimeSync() {
        observeFirestoreChanges().collect { firestoreGuests ->
            firestoreGuests.forEach { guest ->
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


    private suspend fun syncWithFirestore(guest: Guest) {
        guestsCollection.document(guest.name).set(guest).await()
    }



}

