package com.example.smartdocsconvert.di

import android.app.Application
import android.content.Context
import com.example.smartdocsconvert.data.repository.FileRepositoryImpl
import com.example.smartdocsconvert.data.repository.PermissionRepositoryImpl
import com.example.smartdocsconvert.domain.repository.FileRepository
import com.example.smartdocsconvert.domain.repository.PermissionRepository
import com.example.smartdocsconvert.util.PermissionHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }
    
    @Provides
    @Singleton
    fun provideApplication(application: Application): Application {
        return application
    }
    
    @Provides
    @Singleton
    fun providePermissionHelper(@ApplicationContext context: Context): PermissionHelper {
        return PermissionHelper(context)
    }
    
    @Provides
    @Singleton
    fun providePermissionRepository(permissionHelper: PermissionHelper): PermissionRepository {
        return PermissionRepositoryImpl(permissionHelper)
    }
    
    @Provides
    @Singleton
    fun provideFileRepository(): FileRepository {
        return FileRepositoryImpl()
    }
} 