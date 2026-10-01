package ru.sferadevelop.weighly

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

class WeighlyApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** The application behind a ViewModel factory, the one place dependencies are handed out. */
fun CreationExtras.weighlyApplication(): WeighlyApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as WeighlyApplication
