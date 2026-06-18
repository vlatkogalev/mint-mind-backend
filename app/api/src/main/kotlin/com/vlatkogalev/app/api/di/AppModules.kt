package com.vlatkogalev.app.api.di

import com.vlatkogalev.app.api.controllers.CoinController
import com.vlatkogalev.app.api.controllers.CoinPricingController
import com.vlatkogalev.app.api.controllers.CoinSetController
import com.vlatkogalev.app.api.controllers.DebugController
import com.vlatkogalev.app.api.controllers.MarketplaceController
import com.vlatkogalev.app.api.controllers.NewsController
import com.vlatkogalev.app.api.controllers.RevenueCatWebhookController
import com.vlatkogalev.app.api.controllers.StorageController
import com.vlatkogalev.app.api.controllers.UserAuthController
import com.vlatkogalev.app.api.service.SessionMergeService
import com.vlatkogalev.app.jobs.CoinEnrichmentQueue
import com.vlatkogalev.app.jobs.EbayListingsJob
import com.vlatkogalev.app.jobs.MarketplaceJobScheduler
import com.vlatkogalev.app.jobs.NewsJobScheduler
import com.vlatkogalev.app.jobs.RssFeedFetcher
import com.vlatkogalev.data.ebay.EbayCoinPricingService
import com.vlatkogalev.data.ebay.EbayMarketplaceFetcher
import com.vlatkogalev.data.ebay.EbayTokenProvider
import com.vlatkogalev.data.email.ResendEmailVerificationSender
import com.vlatkogalev.data.email.ResendPasswordResetEmailSender
import com.vlatkogalev.data.numista.NumistaMatcher
import com.vlatkogalev.data.numista.NumistaProvider
import com.vlatkogalev.data.postgres.repository.CatalogCoinRepositoryImpl
import com.vlatkogalev.data.postgres.repository.CoinRepositoryImpl
import com.vlatkogalev.data.postgres.repository.CoinSetRepositoryImpl
import com.vlatkogalev.data.postgres.repository.EnrichmentAttemptsRepositoryImpl
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
import com.vlatkogalev.domain.coin.repository.EnrichmentAttemptsRepository
import com.vlatkogalev.domain.coin.service.CoinCatalogProvider
import com.vlatkogalev.domain.coin.service.CoinEnrichmentService
import com.vlatkogalev.domain.coin.service.CoinEnrichmentServiceImpl
import com.vlatkogalev.domain.coin.service.CoinMatcher
import com.vlatkogalev.domain.coin.service.CoinService
import com.vlatkogalev.domain.coin.service.CoinServiceImpl
import com.vlatkogalev.domain.coin.service.CoinSetService
import com.vlatkogalev.domain.coin.service.CoinSetServiceImpl
import com.vlatkogalev.domain.coin.service.MatchSignal
import com.vlatkogalev.domain.coin.service.signals.CompositionSignal
import com.vlatkogalev.domain.coin.service.signals.CountrySignal
import com.vlatkogalev.domain.coin.service.signals.DenominationSignal
import com.vlatkogalev.domain.coin.service.signals.DiameterSignal
import com.vlatkogalev.domain.coin.service.signals.WeightSignal
import com.vlatkogalev.domain.coin.service.signals.YearSignal
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
import com.vlatkogalev.platform.core.config.EmailConfig
import com.vlatkogalev.platform.core.config.EbayConfig
import com.vlatkogalev.platform.core.config.NumistaConfig
import com.vlatkogalev.platform.core.config.loadEbayConfig
import com.vlatkogalev.platform.core.config.loadEmailConfig
import com.vlatkogalev.platform.core.config.loadNumistaConfig
import com.vlatkogalev.platform.core.storage.FileStorageService
import com.vlatkogalev.platform.core.time.TimeProvider
import com.vlatkogalev.platform.database.connectDatabase
import com.vlatkogalev.platform.database.createDataSource
import com.vlatkogalev.platform.database.runMigrations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.koin.dsl.module

val appModule = module {
    single<R2dbcDatabase>(createdAtStart = true) {
        runBlocking<Unit>(Dispatchers.IO) {
            val jdbcDataSource: javax.sql.DataSource = createDataSource()
            runMigrations(jdbcDataSource)
            (jdbcDataSource as java.io.Closeable).close()
        }
        connectDatabase()
    }

    single<UserPasswordHasher> { PasswordHasher() }
    single<UserTokenProvider> { JwtTokenProvider() }
    single<EmailConfig> { loadEmailConfig() }
    single<NumistaConfig> { loadNumistaConfig() }
    single<EbayConfig> { loadEbayConfig() }
    single<EmailVerificationSender> {
        val config = get<EmailConfig>()
        ResendEmailVerificationSender(
            apiKey = config.resendApiKey,
            fromAddress = config.fromAddress,
            appBaseUrl = config.appBaseUrl,
        )
    }
    single<TimeProvider> { TimeProvider.System }

    single<UserRepository> { UserRepositoryImpl(get()) }
    single<SubscriptionRepository> { SubscriptionRepositoryImpl(get()) }
    single { SubscriptionService(get()) }

    single<CoinRepository> { CoinRepositoryImpl(get()) }
    single<CoinSetRepository> { CoinSetRepositoryImpl(get()) }
    single<CatalogCoinRepository> { CatalogCoinRepositoryImpl(get()) }

    single<CoinCatalogProvider> {
        NumistaProvider(get<NumistaConfig>())
    }

    single<EnrichmentAttemptsRepository> { EnrichmentAttemptsRepositoryImpl(get()) }

    single<List<MatchSignal>> {
        listOf(
            CountrySignal(),
            DenominationSignal(),
            YearSignal(),
            WeightSignal(),
            DiameterSignal(),
            CompositionSignal(),
        )
    }

    single<CoinMatcher> { NumistaMatcher(get()) }

    single<CoinEnrichmentService> {
        CoinEnrichmentServiceImpl(
            catalogCoinRepository = get<CatalogCoinRepository>(),
            enrichmentAttemptsRepository = get<EnrichmentAttemptsRepository>(),
            coinRepository = get<CoinRepository>(),
            providers = listOf(get<CoinCatalogProvider>()),
            matcher = get<CoinMatcher>(),
        )
    }

    single<CoinEnrichmentQueue> {
        CoinEnrichmentQueue(
            enrichmentService = get<CoinEnrichmentService>(),
            coinRepository = get<CoinRepository>(),
        )
    }

    single<CoinService> {
        CoinServiceImpl(
            coinRepository = get<CoinRepository>(),
        )
    }

    single<CoinSetService> {
        CoinSetServiceImpl(
            coinSetRepository = get<CoinSetRepository>(),
            coinRepository = get<CoinRepository>(),
        )
    }

    single<NewsRepository> { NewsRepositoryImpl(get()) }
    single<MarketplaceRepository> { MarketplaceRepositoryImpl(get()) }

    single<RssFeedFetcher> { RssFeedFetcher(get()) }
    single<NewsJobScheduler> { NewsJobScheduler(get()) }

    single<EbayTokenProvider> { EbayTokenProvider(get<EbayConfig>()) }
    single<EbayMarketplaceFetcher> { EbayMarketplaceFetcher(get(), get<EbayConfig>()) }
    single<EbayListingsJob> { EbayListingsJob(get(), get(), get<EbayConfig>().feedPages) }
    single<MarketplaceJobScheduler> {
        MarketplaceJobScheduler(get(), intervalSeconds = get<EbayConfig>().feedRefreshIntervalSeconds.toLong())
    }
    single<CoinPricingService> { EbayCoinPricingService(get(), get<EbayConfig>()) }

    single<PasswordResetEmailSender> {
        val config = get<EmailConfig>()
        ResendPasswordResetEmailSender(
            apiKey = config.resendApiKey,
            fromAddress = config.fromAddress,
            appBaseUrl = config.appBaseUrl,
        )
    }

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

    single { UserAuthController(get(), get(), get()) }
    single { CoinController(get(), get(), get(), get(), get()) }
    single { CoinSetController(get(), get(), get()) }
    single { CoinPricingController(get(), get(), get()) }
    single { NewsController(get(), get()) }
    single { MarketplaceController(get(), get()) }
    single { RevenueCatWebhookController(get(), get()) }
    single { StorageController(get(), get()) }
    single { DebugController(get(), get()) }
}
