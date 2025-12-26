package arg.adegtiarev.ciclismo.di

import android.content.Context
import androidx.room.Room
import arg.adegtiarev.ciclismo.data.local.CiclismoDatabase
import arg.adegtiarev.ciclismo.data.local.dao.RideDao
import arg.adegtiarev.ciclismo.data.local.dao.TrackingPointDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): CiclismoDatabase {
        return Room.databaseBuilder(
            context,
            CiclismoDatabase::class.java,
            "ciclismo_db"
        ).build()
    }


    @Provides
    fun provideRideDao(database: CiclismoDatabase): RideDao {
        return database.rideDao()
    }

    @Provides
    fun provideTrackingPointDao(database: CiclismoDatabase): TrackingPointDao {
        return database.trackingPointDao()
    }
}
