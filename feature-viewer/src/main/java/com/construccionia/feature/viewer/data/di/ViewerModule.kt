package com.construccionia.feature.viewer.data.di

import com.construccionia.feature.viewer.data.repository.ImageViewerRepositoryImpl
import com.construccionia.feature.viewer.domain.repository.ImageViewerRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ViewerModule {

    @Binds
    @Singleton
    abstract fun bindImageViewerRepository(
        impl: ImageViewerRepositoryImpl
    ): ImageViewerRepository
}
