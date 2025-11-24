    //Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userAgreementDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_agreement")

class UserAgreementDataStore(context: Context) {

    private val agreementDataStore = context.userAgreementDataStore

    private object PreferencesKeys {
        val userAgreementKey = booleanPreferencesKey("user_agreement")
        val xiaoquUserAgreementKey = booleanPreferencesKey("xiaoqu_user_agreement")
        val sineUserAgreementKey = booleanPreferencesKey("sine_user_agreement")
        val sinePrivacyPolicyKey = booleanPreferencesKey("sine_privacy_policy")
    }

    val userAgreementFlow: Flow<Boolean> = agreementDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.userAgreementKey] ?: false
        }

    val xiaoquUserAgreementFlow: Flow<Boolean> = agreementDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.xiaoquUserAgreementKey] ?: false
        }

    val sineUserAgreementFlow: Flow<Boolean> = agreementDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.sineUserAgreementKey] ?: false
        }

    val sinePrivacyPolicyFlow: Flow<Boolean> = agreementDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.sinePrivacyPolicyKey] ?: false
        }

    suspend fun setUserAgreementAccepted(accepted: Boolean) {
        agreementDataStore.edit { preferences ->
            preferences[PreferencesKeys.userAgreementKey] = accepted
        }
    }

    suspend fun setXiaoquUserAgreementAccepted(accepted: Boolean) {
        agreementDataStore.edit { preferences ->
            preferences[PreferencesKeys.xiaoquUserAgreementKey] = accepted
        }
    }

    suspend fun setSineUserAgreementAccepted(accepted: Boolean) {
        agreementDataStore.edit { preferences ->
            preferences[PreferencesKeys.sineUserAgreementKey] = accepted
        }
    }

    suspend fun setSinePrivacyPolicyAccepted(accepted: Boolean) {
        agreementDataStore.edit { preferences ->
            preferences[PreferencesKeys.sinePrivacyPolicyKey] = accepted
        }
    }
}