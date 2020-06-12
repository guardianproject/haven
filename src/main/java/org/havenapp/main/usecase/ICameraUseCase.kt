package org.havenapp.main.usecase

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

interface ICameraUseCase {
    suspend fun capturePhoto(): Boolean
}

class CameraUseCase(
        private val outputDir: File
) : ICameraUseCase {
    private val cameraDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    override suspend fun capturePhoto(): Boolean = withContext(cameraDispatcher) {

        true
    }
}
