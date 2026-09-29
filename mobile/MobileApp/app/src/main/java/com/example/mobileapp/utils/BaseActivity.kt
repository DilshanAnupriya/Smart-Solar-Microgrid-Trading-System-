/*
 * File:        BaseActivity.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       UI
 * Author:      Cooray B.D.A (IT22189530)
 * Created:     2026-09-29
 * Description: Parent class for every screen that calls the API. Gives each
 *              screen a coroutine scope that is cancelled when the screen closes.
 */

package com.example.mobileapp.utils

import android.app.Activity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

/**
 * An Activity with [uiScope] for launching API calls.
 */
abstract class BaseActivity : Activity() {

    /**
     * Runs on the main thread, so results can update the views directly; the network work
     * itself moves to Dispatchers.IO inside ApiClient.
     */
    protected val uiScope: CoroutineScope = MainScope()

    override fun onDestroy() {
        // Stop any call still in flight, so its result never reaches a closed screen
        uiScope.cancel()
        super.onDestroy()
    }
}
