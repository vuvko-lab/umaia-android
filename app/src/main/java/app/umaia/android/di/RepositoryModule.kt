package app.umaia.android.di

import app.umaia.android.data.remote.SupabaseLoginRepository
import app.umaia.android.data.remote.SupabaseNurRepository
import app.umaia.android.data.remote.SupabaseProfileRepository
import app.umaia.android.data.remote.SupabaseRewardRepository
import app.umaia.android.data.remote.SupabaseStepRepository
import app.umaia.android.domain.repository.LoginRepository
import app.umaia.android.domain.repository.NurRepository
import app.umaia.android.domain.repository.ProfileRepository
import app.umaia.android.domain.repository.RewardRepository
import app.umaia.android.domain.repository.StepRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindLoginRepository(impl: SupabaseLoginRepository): LoginRepository

    @Binds @Singleton
    abstract fun bindProfileRepository(impl: SupabaseProfileRepository): ProfileRepository

    @Binds @Singleton
    abstract fun bindNurRepository(impl: SupabaseNurRepository): NurRepository

    @Binds @Singleton
    abstract fun bindStepRepository(impl: SupabaseStepRepository): StepRepository

    @Binds @Singleton
    abstract fun bindRewardRepository(impl: SupabaseRewardRepository): RewardRepository
}
