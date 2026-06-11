package com.gitforandroid

import android.app.Application

/**
 * Minimal Application class for Robolectric unit tests.
 * Avoids Hilt initialization so tests run without DI.
 */
class TestApplication : Application()
