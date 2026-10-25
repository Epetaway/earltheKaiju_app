package com.earlthekaiju.coach.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf

/** The app shell's snackbar host, so any screen can confirm an action and offer Undo. */
val LocalSnackbarHost = staticCompositionLocalOf { SnackbarHostState() }
