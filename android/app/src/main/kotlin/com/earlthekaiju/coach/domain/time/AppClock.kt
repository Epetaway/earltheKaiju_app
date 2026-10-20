package com.earlthekaiju.coach.domain.time

import android.os.SystemClock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

/**
 * Two kinds of time, kept apart on purpose:
 *  - [now]: wall-clock epoch millis, for records ("class started at 18:31"). Can jump (NTP, travel).
 *  - [elapsedRealtime]: monotonic millis since boot, for anything counted (the training timer).
 *    It never jumps and keeps counting in deep sleep, but resets on reboot.
 */
interface AppClock {
    fun now(): Long
    fun elapsedRealtime(): Long
    fun zone(): ZoneId

    fun today(): LocalDate = Instant.ofEpochMilli(now()).atZone(zone()).toLocalDate()
    fun localDateTime(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(now()), zone())
}

object SystemAppClock : AppClock {
    override fun now(): Long = System.currentTimeMillis()
    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}

/** Stable random ids for every record (see docs/05-data-model.md). */
fun newId(): String = UUID.randomUUID().toString()
