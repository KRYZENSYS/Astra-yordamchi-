package com.example.service

import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log

private const val TAG = "AstrafolioCallScreening"

class AstrafolioCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val callerNumber = callDetails.handle?.schemeSpecificPart ?: "Noma'lum raqam"
        val isIncoming = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            callDetails.callDirection == Call.Details.DIRECTION_INCOMING
        } else {
            true
        }

        // Always allow the call to ring normally while Astrafolio AI Secretary announces and responds
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()

        respondToCall(callDetails, response)

        if (isIncoming && AstrafolioBackgroundService.autoCallResponderEnabled.value) {
            try {
                val serviceIntent = Intent(applicationContext, AstrafolioBackgroundService::class.java).apply {
                    action = AstrafolioBackgroundService.ACTION_HANDLE_INCOMING_CALL
                    putExtra(AstrafolioBackgroundService.EXTRA_CALLER_NUMBER, callerNumber)
                    putExtra(AstrafolioBackgroundService.EXTRA_CALLER_LABEL, "Kiruvchi Qo'ng'iroq")
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    applicationContext.startForegroundService(serviceIntent)
                } else {
                    applicationContext.startService(serviceIntent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to dispatch incoming call to background service", e)
            }
        }
    }
}
