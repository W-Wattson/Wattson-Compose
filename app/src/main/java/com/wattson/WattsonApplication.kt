package com.wattson

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Wattson
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection
 */
@HiltAndroidApp
class WattsonApplication : Application()
