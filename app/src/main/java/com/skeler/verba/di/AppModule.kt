package com.skeler.verba.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.skeler.verba.data.remote.ChatApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Retrofit demands a base URL even though every call passes an absolute
    // @Url; this one is never hit on its own.
    private const val PLACEHOLDER_BASE_URL = "https://openrouter.ai/api/v1/"

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("verba_settings")
        }

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            // Free-tier models queue under load; give them room to answer.
            .readTimeout(90, TimeUnit.SECONDS)
            // Authorization is per-call: the key depends on which provider
            // the selected model belongs to.
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("X-Title", "Verba")
                        .build(),
                )
            }
            .build()

    @Provides
    @Singleton
    fun provideChatApi(client: OkHttpClient, json: Json): ChatApi =
        Retrofit.Builder()
            .baseUrl(PLACEHOLDER_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ChatApi::class.java)
}
