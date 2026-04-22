package app.umaia.android.di

import app.umaia.android.data.sensor.CompositeStepTracker
import app.umaia.android.domain.repository.StepTracker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SensorModule {

    @Binds @Singleton
    abstract fun bindStepTracker(impl: CompositeStepTracker): StepTracker
}
