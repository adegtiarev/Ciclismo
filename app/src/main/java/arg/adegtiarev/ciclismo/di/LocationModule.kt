package arg.adegtiarev.ciclismo.di

import android.content.Context
import arg.adegtiarev.ciclismo.data.DefaultLocationClient
import arg.adegtiarev.ciclismo.domain.LocationClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {
    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    @Provides
    @Singleton
    fun provideLocationClient(
        @ApplicationContext context: Context,
        client: FusedLocationProviderClient
    ): LocationClient = DefaultLocationClient(context, client)
}