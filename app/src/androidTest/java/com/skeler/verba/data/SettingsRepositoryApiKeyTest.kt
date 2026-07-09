package com.skeler.verba.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.skeler.verba.model.Provider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Confirms what actually lands on disk through the real repository path, not
 * just [KeyCipher] in isolation — a throwaway DataStore file so this never
 * touches the app's real verba_settings store.
 */
@RunWith(AndroidJUnit4::class)
class SettingsRepositoryApiKeyTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun newRepository(): SettingsRepository {
        val dataStore = PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("verba_settings_test_${System.nanoTime()}")
        }
        return SettingsRepository(dataStore, KeyCipher())
    }

    @Test
    fun apiKeyRoundTripsThroughTheRepository() = runBlocking {
        val repository = newRepository()
        val secret = "sk-or-v1-integration-test-value"

        repository.setApiKey(Provider.OPENROUTER, secret)

        assertEquals(secret, repository.apiKey(Provider.OPENROUTER).first())
        assertEquals(mapOf(Provider.OPENROUTER to secret), repository.apiKeys.first())
    }

    @Test
    fun rawPreferenceNeverHoldsThePlaintextKey() = runBlocking {
        // A key only SettingsRepository/KeyCipher should be able to read back —
        // if this literal string shows up raw in the backing preferences file,
        // encryption isn't actually happening.
        val dataStoreName = "verba_settings_test_${System.nanoTime()}"
        val dataStore = PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile(dataStoreName)
        }
        val repository = SettingsRepository(dataStore, KeyCipher())
        val secret = "sk-ant-plaintext-must-not-appear-anywhere"

        repository.setApiKey(Provider.ANTHROPIC, secret)

        val onDiskKey = context.preferencesDataStoreFile(dataStoreName)
            .readText()
        assertFalse("plaintext key leaked into the preferences file", onDiskKey.contains(secret))
    }

    @Test
    fun removingTheKeyClearsItCompletely() = runBlocking {
        val repository = newRepository()
        repository.setApiKey(Provider.MISTRAL, "some-mistral-key")
        repository.clearApiKey(Provider.MISTRAL)

        assertNull(repository.apiKey(Provider.MISTRAL).first())
    }

    /**
     * A key saved before encryption shipped is still plain text on disk on
     * upgrade — this must keep reading back exactly, not disappear just
     * because it doesn't decrypt as ciphertext.
     */
    @Test
    fun aPreExistingPlaintextKeyStillReadsBackCorrectly() = runBlocking {
        val dataStoreName = "verba_settings_test_${System.nanoTime()}"
        val dataStore = PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile(dataStoreName)
        }
        val legacyPlaintextKey = "sk-or-v1-legacy-key-stored-before-encryption"
        // Bypass the repository entirely to simulate data written by an older
        // app version, before KeyCipher existed.
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("api_key_openrouter")] = legacyPlaintextKey
        }

        val repository = SettingsRepository(dataStore, KeyCipher())

        assertEquals(legacyPlaintextKey, repository.apiKey(Provider.OPENROUTER).first())
        assertEquals(mapOf(Provider.OPENROUTER to legacyPlaintextKey), repository.apiKeys.first())
    }
}
