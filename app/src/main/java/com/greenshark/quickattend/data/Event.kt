package com.greenshark.quickattend.data

data class Event(
    val id: String,
    val date: String,
    val time: String,
    val title: String,
    val description: String,
    val location: String,
    val organizerId: String,
)
