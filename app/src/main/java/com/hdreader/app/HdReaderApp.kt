package com.hdreader.app

import android.app.Application
import com.hdreader.app.readium.ReadiumService

class HdReaderApp : Application() {
    lateinit var readium: ReadiumService
        private set

    override fun onCreate() {
        super.onCreate()
        readium = ReadiumService(this)
    }
}
