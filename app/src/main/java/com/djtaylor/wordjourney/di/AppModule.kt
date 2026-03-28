package com.djtaylor.wordjourney.di

import com.djtaylor.wordjourney.billing.IAdManager
import com.djtaylor.wordjourney.billing.IBillingManager
import com.djtaylor.wordjourney.billing.RealBillingManager
import com.djtaylor.wordjourney.billing.StubAdManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindBillingManager(impl: RealBillingManager): IBillingManager

    // AdMob is temporarily disabled while a new ad partner is evaluated.
    // Swap StubAdManager → RealAdManager (or a new partner implementation) to re-enable ads.
    @Binds
    @Singleton
    abstract fun bindAdManager(impl: StubAdManager): IAdManager
}
