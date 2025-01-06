package com.ddas.sam

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "guest")
data class Guest(
    @PrimaryKey val name: String,
    val email: String,
    val phoneNumber: String,
    val companyName: String,
    val attending: Boolean = false,
    val hasLanyard: Boolean = false,
    val hasGift: Boolean = false,
    val hasFoodCoupon: Boolean = false,
    val remarks: String? = null,
    val paymentMode: String? = null,
    val amount: String? = null,
    val category: String? = null,
    val deleted: Boolean = false
) {
    // No-argument constructor required for Firebase deserialization
    constructor() : this(
        name = "",
        email = "",
        phoneNumber = "",
        companyName = "",
        attending = false,
        hasLanyard = false,
        hasGift = false,
        hasFoodCoupon = false,
        remarks = null,
        paymentMode = null,
        amount = null,
        category = null,
        deleted = false
    )
}
