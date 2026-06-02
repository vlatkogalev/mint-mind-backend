package com.vlatkogalev.app.api.di

import com.vlatkogalev.app.api.controllers.CoinController
import com.vlatkogalev.app.api.controllers.CoinPricingController
import com.vlatkogalev.app.api.controllers.CoinSetController
import com.vlatkogalev.app.api.controllers.NewsController
import com.vlatkogalev.app.api.controllers.MarketplaceController
import com.vlatkogalev.app.api.controllers.RevenueCatWebhookController
import com.vlatkogalev.app.api.controllers.StorageController
import com.vlatkogalev.app.api.controllers.UserAuthController
import com.vlatkogalev.app.jobs.EbayListingsJob
import com.vlatkogalev.app.jobs.MarketplaceJobScheduler
import com.vlatkogalev.app.api.service.SessionMergeService
import com.vlatkogalev.app.jobs.NewsJobScheduler
import com.vlatkogalev.app.jobs.RssFeedFetcher
import com.vlatkogalev.data.ebay.EbayCoinPricingService
import com.vlatkogalev.data.ebay.EbayMarketplaceFetcher
import com.vlatkogalev.data.ebay.EbayTokenProvider
import com.vlatkogalev.data.email.ResendEmailVerificationSender
import com.vlatkogalev.data.numista.NumistaProvider
import com.vlatkogalev.data.postgres.daos.CatalogCoinQueries
import com.vlatkogalev.data.postgres.daos.CoinQueries
import com.vlatkogalev.data.postgres.daos.CoinSetQueries
import com.vlatkogalev.data.postgres.daos.MarketplaceQueries
import com.vlatkogalev.data.postgres.daos.NewsQueries
import com.vlatkogalev.data.email.ResendPasswordResetEmailSender
import com.vlatkogalev.data.postgres.daos.SubscriptionQueries
import com.vlatkogalev.data.postgres.daos.UserQueries
import com.vlatkogalev.data.postgres.repository.CatalogCoinRepositoryImpl
import com.vlatkogalev.data.postgres.repository.CoinRepositoryImpl
import com.vlatkogalev.data.postgres.repository.CoinSetRepositoryImpl
import com.vlatkogalev.data.postgres.repository.MarketplaceRepositoryImpl
import com.vlatkogalev.data.postgres.repository.NewsRepositoryImpl
import com.vlatkogalev.data.postgres.repository.SubscriptionRepositoryImpl
import com.vlatkogalev.data.postgres.repository.UserRepositoryImpl
import com.vlatkogalev.data.s3.S3FileStorageService
import com.vlatkogalev.domain.billing.repository.SubscriptionRepository
import com.vlatkogalev.domain.billing.service.SubscriptionService
import com.vlatkogalev.domain.coin.repository.CatalogCoinRepository
import com.vlatkogalev.domain.coin.repository.CoinRepository
import com.vlatkogalev.domain.coin.repository.CoinSetRepository
import com.vlatkogalev.domain.coin.service.CoinCatalogProvider
import com.vlatkogalev.domain.coin.service.CoinEnrichmentService
import com.vlatkogalev.domain.coin.service.CoinEnrichmentServiceImpl
import com.vlatkogalev.domain.coin.service.CoinService
import com.vlatkogalev.domain.coin.service.CoinServiceImpl
import com.vlatkogalev.domain.coin.service.CoinSetService
import com.vlatkogalev.domain.coin.service.CoinSetServiceImpl
import com.vlatkogalev.domain.marketplace.repository.MarketplaceRepository
import com.vlatkogalev.domain.news.repository.NewsRepository
import com.vlatkogalev.domain.pricing.service.CoinPricingService
import com.vlatkogalev.domain.user.repository.UserRepository
import com.vlatkogalev.domain.user.service.EmailVerificationSender
import com.vlatkogalev.domain.user.service.PasswordResetEmailSender
import com.vlatkogalev.domain.user.service.UserAuthService
import com.vlatkogalev.domain.user.service.UserAuthServiceImpl
import com.vlatkogalev.domain.user.service.UserPasswordHasher
import com.vlatkogalev.domain.user.service.UserTokenProvider
import com.vlatkogalev.platform.auth.JwtTokenProvider
import com.vlatkogalev.platform.auth.PasswordHasher
import com.vlatkogalev.platform.core.config.EbayConfig
import com.vlatkogalev.platform.core.config.EmailConfig
import com.vlatkogalev.platform.core.config.NumistaConfig
import com.vlatkogalev.platform.core.config.loadEbayConfig
import com.vlatkogalev.platform.core.config.loadEmailConfig
import com.vlatkogalev.platform.core.config.loadNumistaConfig
import com.vlatkogalev.platform.core.storage.FileStorageService
import com.vlatkogalev.platform.core.time.TimeProvider
import com.vlatkogalev.platform.database.createDataSource
import com.vlatkogalev.platform.database.runMigrations
import org.koin.dsl.module
import javax.sql.DataSource

val appModule = module {
    single<DataSource> {
        val dataSource = createDataSource()
        runMigrations(dataSource)
        dataSource
    }

    single<UserPasswordHasher> { PasswordHasher() }
    single<UserTokenProvider> { JwtTokenProvider() }
    single<EmailConfig> { loadEmailConfig() }
    single<EmailVerificationSender> {
        val config = get<EmailConfig>()
        ResendEmailVerificationSender(
            apiKey = config.resendApiKey,
            fromAddress = config.fromAddress,
            appBaseUrl = config.appBaseUrl,
        )
    }
    single<PasswordResetEmailSender> {
        val config = get<EmailConfig>()
        ResendPasswordResetEmailSender(
            apiKey = config.resendApiKey,
            fromAddress = config.fromAddress,
            appBaseUrl = config.appBaseUrl,
        )
    }
    single<EbayConfig> { loadEbayConfig() }
    single<NumistaConfig> { loadNumistaConfig() }
    single<TimeProvider> { TimeProvider.System }
    single { RssFeedFetcher(get()) }
    single { NewsJobScheduler(get()) }

    single { UserQueries(get()) }
    single { SubscriptionQueries(get()) }
    single { CoinQueries(get()) }
    single { CoinSetQueries(get()) }
    single { CatalogCoinQueries(get()) }
    single { NewsQueries(get()) }
    single { MarketplaceQueries(get()) }

    single<UserRepository> { UserRepositoryImpl(get(), get()) }
    single<SubscriptionRepository> { SubscriptionRepositoryImpl(get()) }
    single<CoinRepository> { CoinRepositoryImpl(get()) }
    single<CatalogCoinRepository> { CatalogCoinRepositoryImpl(get(), get()) }
    single<CoinSetRepository> { CoinSetRepositoryImpl(get()) }
    single<NewsRepository> { NewsRepositoryImpl(get()) }
    single<MarketplaceRepository> { MarketplaceRepositoryImpl(get()) }

    single { SubscriptionService(get()) }
    single<CoinCatalogProvider> { NumistaProvider(get()) }
    single<List<CoinCatalogProvider>> { listOf(get<CoinCatalogProvider>()) }
    single<CoinEnrichmentService> { CoinEnrichmentServiceImpl(get(), get()) }
    single<CoinService> { CoinServiceImpl(get(), get()) }
    single<CoinSetService> { CoinSetServiceImpl(get(), get()) }
    single<UserAuthService> {
        UserAuthServiceImpl(
            userRepository = get<UserRepository>(),
            passwordHasher = get<UserPasswordHasher>(),
            jwtTokenProvider = get<UserTokenProvider>(),
            skipEmailVerification = get<EmailConfig>().skipVerification,
            emailVerificationSender = get<EmailVerificationSender>(),
            passwordResetEmailSender = get<PasswordResetEmailSender>(),
        )
    }
    single { SessionMergeService(get(), get()) }
    single<FileStorageService> { S3FileStorageService() }
    single { EbayTokenProvider(get()) }
    single<CoinPricingService> { EbayCoinPricingService(get(), get()) }
    single { EbayMarketplaceFetcher(get(), get()) }
    single {
        EbayListingsJob(
            fetcher = get(),
            repository = get(),
            pagesToFetch = get<EbayConfig>().feedPagesToFetch,
        )
    }
    single {
        MarketplaceJobScheduler(
            job = get(),
            intervalSeconds = get<EbayConfig>().feedRefreshIntervalSeconds,
        )
    }

    single { UserAuthController(get(), get(), get()) }
    single { RevenueCatWebhookController(get(), get()) }
    single { StorageController(get(), get()) }
    single { CoinController(get(), get(), get(), get()) }
    single { CoinSetController(get(), get()) }
    single { CoinPricingController(get(), get(), get()) }
    single { NewsController(get(), get()) }
    single { MarketplaceController(get(), get()) }
}
