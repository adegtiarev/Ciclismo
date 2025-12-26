package arg.adegtiarev.ciclismo.di

import arg.adegtiarev.ciclismo.data.local.repository.RideRepositoryImpl
import arg.adegtiarev.ciclismo.domain.RideRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindRideRepository(
        rideRepositoryImpl: RideRepositoryImpl
    ): RideRepository
}