package com.greenshark.quickattend.data

data class Event(
    val id: String,
    val date: String,
    val schedule: String,
    val title: String,
    val description: String,
    val location: String,
    val mapLocation: String,
    val organizerId: String,
    val organizerName: String
)
