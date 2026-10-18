package com.v2ray.ang.ui.xfinder

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.ui.base.BaseViewModel
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScannerUiState(
    val data: ScannerData? = null,
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    /** The user's own configs the mix dialog offers, by id; loaded when the dialog opens. */
    val ownConfigs: List<OwnConfig> = emptyList(),
    val mixBusy: Boolean = false,
)

/** A config of the user that can be picked as the base of a mix. */
data class OwnConfig(val guid: String, val name: String)

class ScannerViewModel(application: Application) : BaseViewModel(application) {
    private val repository = XfinderRepository()

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, hasError = false) }
        loadJob = viewModelScope.launch {
            try {
                XfinderLogStore.info("fetch scanner: ${XfinderSources.SCANNER_URL}")
                val json = repository.fetchText(XfinderSources.SCANNER_URL)
                val data = withContext(Dispatchers.Default) { ScannerParser.parse(json) }
                if (data == null) {
                    XfinderLogStore.error("fetch scanner: failed or malformed")
                    _uiState.update { it.copy(isLoading = false, hasError = true) }
                } else {
                    XfinderLogStore.info("fetch scanner: ${data.ips.size} ips")
                    _uiState.update { it.copy(data = data, isLoading = false, hasError = false) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                XfinderLogStore.error("fetch scanner: ${e.javaClass.simpleName}")
                _uiState.update { it.copy(isLoading = false, hasError = true) }
            }
        }
    }

    fun copyIp(ip: String) {
        Utils.setClipboard(app, ip)
        toast(R.string.xf_ip_copied)
    }

    /** Loads the user's own configs (shareable kinds only) for the picker of the mix dialog. */
    fun loadOwnConfigs() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                MmkvManager.decodeAllServerList().mapNotNull { guid ->
                    val profile = MmkvManager.decodeServerConfig(guid) ?: return@mapNotNull null
                    val kind = profile.configType
                    if (kind != EConfigType.VLESS && kind != EConfigType.VMESS && kind != EConfigType.TROJAN) return@mapNotNull null
                    OwnConfig(guid, profile.remarks)
                }.take(MAX_OWN_CONFIGS)
            }
            _uiState.update { it.copy(ownConfigs = list) }
        }
    }

    /**
     * Mixes the best [count] scanned IPs into the config: [ownGuid] (one of the user's configs) or, when it is null,
     * the pasted [pasted] link. The copies go into a new group of their own. [done] gets how many were added.
     */
    fun mix(count: Int, ownGuid: String?, pasted: String, done: (Int) -> Unit) {
        val ips = _uiState.value.data?.ips?.map { it.ip }.orEmpty()
        if (ips.isEmpty() || _uiState.value.mixBusy) {
            done(0)
            return
        }
        _uiState.update { it.copy(mixBusy = true) }
        viewModelScope.launch {
            val added = withContext(Dispatchers.IO) {
                try {
                    val base = if (ownGuid != null) AngConfigManager.shareConfig(ownGuid) else pasted
                    val links = ScannerMixer.mix(base, ips, count)
                    if (links.isEmpty()) return@withContext 0
                    val groupId = Utils.getUuid()
                    MmkvManager.encodeSubscription(
                        groupId,
                        SubscriptionItem(remarks = app.getString(R.string.xf_mix_group_name), autoUpdate = false),
                    )
                    val (configs, _) = AngConfigManager.importBatchConfig(links.joinToString("\n"), groupId, true)
                    if (configs <= 0) MmkvManager.removeSubscription(groupId)
                    configs
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    XfinderLogStore.error("mix: ${e.javaClass.simpleName}")
                    0
                }
            }
            _uiState.update { it.copy(mixBusy = false) }
            if (added > 0) toast(app.getString(R.string.xf_mix_done, added)) else toast(R.string.xf_mix_failed)
            done(added)
        }
    }

    private companion object {
        const val MAX_OWN_CONFIGS = 60
    }
}
