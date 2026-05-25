package com.gourav.dronetelemetry.di

import com.gourav.dronetelemetry.data.repository.SocketTelemetryRepository
import com.gourav.dronetelemetry.domain.repository.TelemetryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TelemetryModule {
    @Binds
    @Singleton
    abstract fun bindTelemetryRepository(repository: SocketTelemetryRepository): TelemetryRepository
}
