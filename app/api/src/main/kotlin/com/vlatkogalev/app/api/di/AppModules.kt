package com.vlatkogalev.app.api.di

import com.vlatkogalev.app.api.controllers.StorageController
import com.vlatkogalev.app.api.controllers.UserAuthController
import com.vlatkogalev.app.data.UserAuthServiceImpl
import com.vlatkogalev.app.data.daos.UserQueries
import com.vlatkogalev.app.data.repository.UserRepositoryImpl
import com.vlatkogalev.app.data.repository.UserRepository
import com.vlatkogalev.app.domain.service.UserAuthService
import com.vlatkogalev.platform.auth.JwtTokenProvider
import com.vlatkogalev.platform.auth.PasswordHasher
import com.vlatkogalev.platform.core.time.TimeProvider
import com.vlatkogalev.platform.database.createDataSource
import com.vlatkogalev.platform.database.runMigrations
import com.vlatkogalev.platform.storage.FileStorageService
import com.vlatkogalev.platform.storage.S3FileStorageService
import org.koin.dsl.module
import javax.sql.DataSource

val appModule = module {
    single<DataSource> {
        val dataSource = createDataSource()
        runMigrations(dataSource)
        dataSource
    }

    single { PasswordHasher() }
    single { JwtTokenProvider() }
    single<TimeProvider> { TimeProvider.System }

    single { UserQueries(get()) }

    single<UserRepository> { UserRepositoryImpl(get()) }

    single<UserAuthService> {
        UserAuthServiceImpl(
            userRepository = get<UserRepository>(),
            passwordHasher = get<PasswordHasher>(),
            jwtTokenProvider = get<JwtTokenProvider>(),
        )
    }

    single<FileStorageService> { S3FileStorageService() }

    single { UserAuthController(get(), get()) }
    single { StorageController(get(), get()) }
}
