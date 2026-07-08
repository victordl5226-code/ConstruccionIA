package com.construccionia.feature.animation.data.di

import com.construccionia.feature.animation.data.lottie.LottieAnimationProvider
import com.construccionia.feature.animation.data.repository.AnimationRepositoryImpl
import com.construccionia.feature.animation.domain.repository.AnimationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnimationModule {

    @Provides
    @Singleton
    fun provideAnimationRepository(
        animationProvider: LottieAnimationProvider
    ): AnimationRepository {
        return AnimationRepositoryImpl(animationProvider)
    }
}
