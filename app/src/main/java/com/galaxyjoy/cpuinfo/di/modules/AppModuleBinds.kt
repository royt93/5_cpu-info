package com.galaxyjoy.cpuinfo.di.modules

import com.galaxyjoy.cpuinfo.appinitializers.AppInitializer
import com.galaxyjoy.cpuinfo.appinitializers.NativeToolsInitializer
import com.galaxyjoy.cpuinfo.appinitializers.InitializerRx
import com.galaxyjoy.cpuinfo.appinitializers.InitializerTheme
import com.galaxyjoy.cpuinfo.appinitializers.InitializerTimber
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModuleBinds {

    @Binds
    @IntoSet
    abstract fun provideNativeToolsInitializer(bind: NativeToolsInitializer): AppInitializer

    @Binds
    @IntoSet
    abstract fun provideRxInitializer(bind: InitializerRx): AppInitializer

    @Binds
    @IntoSet
    abstract fun provideTimberInitializer(bind: InitializerTimber): AppInitializer

    @Binds
    @IntoSet
    abstract fun provideThemeInitializer(bind: InitializerTheme): AppInitializer
}
