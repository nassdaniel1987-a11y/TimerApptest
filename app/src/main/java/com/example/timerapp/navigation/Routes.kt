package com.example.timerapp.navigation

import kotlinx.serialization.Serializable

@Serializable
data object Home

@Serializable
data object CreateTimer

@Serializable
data object Settings

@Serializable
data object QRScanner

@Serializable
data object Categories

@Serializable
data object ManageQRCodes

@Serializable
data object CreateQRCode

@Serializable
data class QRCodeDetail(val qrCodeId: String)
