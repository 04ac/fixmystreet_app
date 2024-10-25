package com.areen.fixmystreet.models

data class Case(
    val _id: String,
    val image: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val submittedBy: String,
    val resolved: Boolean,
    val threat: Int
)