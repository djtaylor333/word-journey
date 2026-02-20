package com.djtaylor.wordjourney.di

import com.djtaylor.wordjourney.billing.IBillingManager
import com.djtaylor.wordjourney.billing.StubBillingManager
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
    abstract fun bindBillingManager(stub: StubBillingManager): IBillingManager
}
