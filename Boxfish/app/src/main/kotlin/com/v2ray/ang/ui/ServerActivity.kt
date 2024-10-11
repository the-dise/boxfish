package com.v2ray.ang.ui

import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.v2ray.ang.AppConfig
import com.v2ray.ang.AppConfig.PREF_ALLOW_INSECURE
import com.v2ray.ang.AppConfig.WIREGUARD_LOCAL_ADDRESS_V4
import com.v2ray.ang.AppConfig.WIREGUARD_LOCAL_ADDRESS_V6
import com.v2ray.ang.AppConfig.WIREGUARD_LOCAL_MTU
import com.v2ray.ang.R
import com.v2ray.ang.dto.EConfigType
import com.v2ray.ang.dto.ServerConfig
import com.v2ray.ang.dto.V2rayConfig
import com.v2ray.ang.dto.V2rayConfig.Companion.DEFAULT_PORT
import com.v2ray.ang.dto.V2rayConfig.Companion.TLS
import com.v2ray.ang.extension.removeWhiteSpace
import com.v2ray.ang.extension.toast
import com.v2ray.ang.util.MmkvManager
import com.v2ray.ang.util.MmkvManager.settingsStorage
import com.v2ray.ang.util.Utils
import com.v2ray.ang.util.Utils.getIpv6Address

class ServerActivity : BaseActivity() {

    private val editGuid by lazy { intent.getStringExtra("guid").orEmpty() }
    private val isRunning by lazy {
        intent.getBooleanExtra("isRunning", false)
                && editGuid.isNotEmpty()
                && editGuid == MmkvManager.getSelectServer()
    }
    private val createConfigType by lazy {
        EConfigType.fromInt(intent.getIntExtra("createConfigType", EConfigType.VMESS.value))
            ?: EConfigType.VMESS
    }
    private val subscriptionId by lazy {
        intent.getStringExtra("subscriptionId")
    }

    private val securitys: Array<out String> by lazy {
        resources.getStringArray(R.array.securitys)
    }
    private val shadowsocksSecurity: Array<out String> by lazy {
        resources.getStringArray(R.array.ss_securitys)
    }
    private val flows: Array<out String> by lazy {
        resources.getStringArray(R.array.flows)
    }
    private val networks: Array<out String> by lazy {
        resources.getStringArray(R.array.networks)
    }
    private val tcpTypes: Array<out String> by lazy {
        resources.getStringArray(R.array.header_type_tcp)
    }
    private val kcpAndQuicTypes: Array<out String> by lazy {
        resources.getStringArray(R.array.header_type_kcp_and_quic)
    }
    private val grpcModes: Array<out String> by lazy {
        resources.getStringArray(R.array.mode_type_grpc)
    }
    private val streamSecuritys: Array<out String> by lazy {
        resources.getStringArray(R.array.streamsecurityxs)
    }
    private val allowinsecures: Array<out String> by lazy {
        resources.getStringArray(R.array.allowinsecures)
    }
    private val uTlsItems: Array<out String> by lazy {
        resources.getStringArray(R.array.streamsecurity_utls)
    }
    private val alpns: Array<out String> by lazy {
        resources.getStringArray(R.array.streamsecurity_alpn)
    }

    // Kotlin synthetics was used, but since it is removed in 1.8. We switch to old manual approach.
    // We don't use AndroidViewBinding because, it is better to share similar logics for different
    // protocols. Use findViewById manually ensures the xml are de-coupled with the activity logic.
    private val etRemarks: EditText by lazy { findViewById(R.id.et_remarks) }
    private val etAddress: EditText by lazy { findViewById(R.id.et_address) }
    private val etPort: EditText by lazy { findViewById(R.id.et_port) }
    private val etId: EditText by lazy { findViewById(R.id.et_id) }
    private val etAlterID: EditText? by lazy { findViewById(R.id.et_alterId) }
    private val etSecurity: EditText? by lazy { findViewById(R.id.et_security) }
    private val spFlow: AutoCompleteTextView? by lazy { findViewById(R.id.sp_flow) }
    private val spSecurity: Spinner? by lazy { findViewById(R.id.sp_security) }
    private val spStreamSecurity: Spinner? by lazy { findViewById(R.id.sp_stream_security) }
    private val spAllowInsecure: AutoCompleteTextView? by lazy { findViewById(R.id.sp_allow_insecure) }
    private val containerAllowInsecure: TextInputLayout? by lazy { findViewById(R.id.til_allow_insecure) }
    private val etSni: EditText? by lazy { findViewById(R.id.et_sni) }
    private val containerSni: TextInputLayout? by lazy { findViewById(R.id.til_sni) }
    private val spStreamFingerprint: AutoCompleteTextView? by lazy { findViewById(R.id.sp_stream_fingerprint) } //uTLS
    private val containerFingerprint: TextInputLayout? by lazy { findViewById(R.id.til_stream_fingerprint) }
    private val spNetwork: Spinner? by lazy { findViewById(R.id.sp_network) }
    private val spHeaderType: Spinner? by lazy { findViewById(R.id.sp_header_type) }
    private val spHeaderTypeTitle: TextView? by lazy { findViewById(R.id.sp_header_type_title) }
    private val tvRequestHost: TextInputLayout? by lazy { findViewById(R.id.tv_request_host) }
    private val etRequestHost: EditText? by lazy { findViewById(R.id.et_request_host) }
    private val tvPath: TextInputLayout? by lazy { findViewById(R.id.tv_path) }
    private val etPath: EditText? by lazy { findViewById(R.id.et_path) }
    private val spStreamAlpn: AutoCompleteTextView? by lazy { findViewById(R.id.sp_stream_alpn) } //uTLS
    private val containerAlpn: TextInputLayout? by lazy { findViewById(R.id.til_stream_alpn) }
    private val etPublicKey: EditText? by lazy { findViewById(R.id.et_public_key) }
    private val containerPublicKey: LinearLayout? by lazy { findViewById(R.id.lay_public_key) }
    private val etShortId: EditText? by lazy { findViewById(R.id.et_short_id) }
    private val containerShortId: LinearLayout? by lazy { findViewById(R.id.lay_short_id) }
    private val etSpiderX: EditText? by lazy { findViewById(R.id.et_spider_x) }
    private val containerSpiderX: LinearLayout? by lazy { findViewById(R.id.lay_spider_x) }
    private val etReserved1: EditText? by lazy { findViewById(R.id.et_reserved1) }
    private val etReserved2: EditText? by lazy { findViewById(R.id.et_reserved2) }
    private val etReserved3: EditText? by lazy { findViewById(R.id.et_reserved3) }
    private val etLocalAddress: EditText? by lazy { findViewById(R.id.et_local_address) }
    private val etLocalMtu: EditText? by lazy { findViewById(R.id.et_local_mtu) }
    private val etObfsPassword: EditText? by lazy { findViewById(R.id.et_obfs_password) }

    private var isConfigurationChanged = false // Track if the configuration has changed

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        isConfigurationChanged = true
        val config = MmkvManager.decodeServerConfig(editGuid)
        when (config?.configType ?: createConfigType) {
            EConfigType.VMESS -> setContentView(R.layout.activity_server_vmess)
            EConfigType.CUSTOM -> return
            EConfigType.SHADOWSOCKS -> setContentView(R.layout.activity_server_shadowsocks)
            EConfigType.SOCKS -> setContentView(R.layout.activity_server_socks)
            EConfigType.HTTP -> setContentView(R.layout.activity_server_socks)
            EConfigType.VLESS -> setContentView(R.layout.activity_server_vless)
            EConfigType.TROJAN -> setContentView(R.layout.activity_server_trojan)
            EConfigType.WIREGUARD -> setContentView(R.layout.activity_server_wireguard)
            EConfigType.HYSTERIA2 -> setContentView(R.layout.activity_server_hysteria2)
        }

        // Setup Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_server) // Title

        // Handling a click on navigationIcon (back button)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed() // Go back to previous screen
        }

        spNetwork?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val types = transportTypes(networks[position])
                spHeaderType?.isEnabled = types.size > 1
                val adapter =
                    ArrayAdapter(this@ServerActivity, android.R.layout.simple_spinner_item, types)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spHeaderType?.adapter = adapter
                spHeaderTypeTitle?.text = if (networks[position] == "grpc")
                    getString(R.string.server_lab_mode_type) else
                    getString(R.string.server_lab_head_type)
                config?.getProxyOutbound()?.getTransportSettingDetails()?.let { transportDetails ->
                    spHeaderType?.setSelection(Utils.arrayFind(types, transportDetails[0]))
                    etRequestHost?.text = Utils.getEditable(transportDetails[1])
                    etPath?.text = Utils.getEditable(transportDetails[2])
                }

                tvRequestHost?.hint = Utils.getEditable(
                    getString(
                        when (networks[position]) {
                            "tcp" -> R.string.server_lab_request_host_http
                            "ws" -> R.string.server_lab_request_host_ws
                            "httpupgrade" -> R.string.server_lab_request_host_httpupgrade
                            "splithttp" -> R.string.server_lab_request_host_splithttp
                            "h2" -> R.string.server_lab_request_host_h2
                            "quic" -> R.string.server_lab_request_host_quic
                            "grpc" -> R.string.server_lab_request_host_grpc
                            else -> R.string.server_lab_request_host
                        }
                    )
                )

                tvPath?.hint = Utils.getEditable(
                    getString(
                        when (networks[position]) {
                            "kcp" -> R.string.server_lab_path_kcp
                            "ws" -> R.string.server_lab_path_ws
                            "httpupgrade" -> R.string.server_lab_path_httpupgrade
                            "splithttp" -> R.string.server_lab_path_splithttp
                            "h2" -> R.string.server_lab_path_h2
                            "quic" -> R.string.server_lab_path_quic
                            "grpc" -> R.string.server_lab_path_grpc
                            else -> R.string.server_lab_path
                        }
                    )
                )
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // do nothing
            }
        }
        spStreamSecurity?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (streamSecuritys[position].isBlank()) {
                    containerSni?.visibility = View.GONE
                    containerFingerprint?.visibility = View.GONE
                    containerAlpn?.visibility = View.GONE
                    containerAllowInsecure?.visibility = View.GONE
                    containerPublicKey?.visibility = View.GONE
                    containerShortId?.visibility = View.GONE
                    containerSpiderX?.visibility = View.GONE
                } else {
                    containerSni?.visibility = View.VISIBLE
                    containerFingerprint?.visibility = View.VISIBLE
                    containerAlpn?.visibility = View.VISIBLE
                    if (streamSecuritys[position] == TLS) {
                        containerAllowInsecure?.visibility = View.VISIBLE
                        containerPublicKey?.visibility = View.GONE
                        containerShortId?.visibility = View.GONE
                        containerSpiderX?.visibility = View.GONE
                    } else {
                        containerAllowInsecure?.visibility = View.GONE
                        containerAlpn?.visibility = View.GONE
                        containerPublicKey?.visibility = View.VISIBLE
                        containerShortId?.visibility = View.VISIBLE
                        containerSpiderX?.visibility = View.VISIBLE
                    }
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                // do nothing
            }
        }
        if (config != null) {
            bindingServer(config)
        } else {
            clearServer()
        }

        // Registering the back press callback
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isConfigurationChanged) {
                    MaterialAlertDialogBuilder(this@ServerActivity)
                        .setTitle(R.string.confirm_save)
                        .setMessage(R.string.confirm_save_changes) // Your string resource for confirmation message
                        .setPositiveButton(R.string.save) { _, _ ->
                            saveServer() // Save changes
                            finish() // Exit the activity
                        }
                        .setNegativeButton(R.string.discard) { _, _ ->
                            finish() // Exit without saving
                        }
                        .setNeutralButton(android.R.string.cancel) { dialog, _ ->
                            dialog.dismiss() // Close the dialog
                        }
                        .show()
                } else {
                    isEnabled = false
                }
            }
        })
    }

    /**
     * binding selected server config
     */
    private fun bindingServer(config: ServerConfig): Boolean {
        val outbound = config.getProxyOutbound() ?: return false

        etRemarks.text = Utils.getEditable(config.remarks)
        etAddress.text = Utils.getEditable(outbound.getServerAddress().orEmpty())
        etPort.text =
            Utils.getEditable(outbound.getServerPort()?.toString() ?: DEFAULT_PORT.toString())
        etId.text = Utils.getEditable(outbound.getPassword().orEmpty())
        etAlterID?.text =
            Utils.getEditable(outbound.settings?.vnext?.get(0)?.users?.get(0)?.alterId.toString())
        if (config.configType == EConfigType.SOCKS
            || config.configType == EConfigType.HTTP
        ) {
            etSecurity?.text =
                Utils.getEditable(outbound.settings?.servers?.get(0)?.users?.get(0)?.user.orEmpty())
        } else if (config.configType == EConfigType.VLESS) {
            etSecurity?.text = Utils.getEditable(outbound.getSecurityEncryption().orEmpty())
            val flow = Utils.arrayFind(
                flows,
                outbound.settings?.vnext?.get(0)?.users?.get(0)?.flow.orEmpty()
            )
            if (flow >= 0) {
                spFlow?.setText(flows[flow], false)
            }
        } else if (config.configType == EConfigType.WIREGUARD) {
            etPublicKey?.text =
                Utils.getEditable(outbound.settings?.peers?.get(0)?.publicKey.orEmpty())
            if (outbound.settings?.reserved == null) {
                etReserved1?.text = Utils.getEditable("0")
                etReserved2?.text = Utils.getEditable("0")
                etReserved3?.text = Utils.getEditable("0")
            } else {
                etReserved1?.text =
                    Utils.getEditable(outbound.settings?.reserved?.get(0).toString())
                etReserved2?.text =
                    Utils.getEditable(outbound.settings?.reserved?.get(1).toString())
                etReserved3?.text =
                    Utils.getEditable(outbound.settings?.reserved?.get(2).toString())
            }
            if (outbound.settings?.address == null) {
                etLocalAddress?.text =
                    Utils.getEditable("${WIREGUARD_LOCAL_ADDRESS_V4},${WIREGUARD_LOCAL_ADDRESS_V6}")
            } else {
                val list = outbound.settings?.address as List<*>
                etLocalAddress?.text = Utils.getEditable(list.joinToString(","))
            }
            if (outbound.settings?.mtu == null) {
                etLocalMtu?.text = Utils.getEditable(WIREGUARD_LOCAL_MTU)
            } else {
                etLocalMtu?.text = Utils.getEditable(outbound.settings?.mtu.toString())
            }
        } else if (config.configType == EConfigType.HYSTERIA2) {
            etObfsPassword?.text = Utils.getEditable(outbound.settings?.obfuscatePassword)
        }

        val securityEncryptions =
            if (config.configType == EConfigType.SHADOWSOCKS) shadowsocksSecurity else securitys
        val security =
            Utils.arrayFind(securityEncryptions, outbound.getSecurityEncryption().orEmpty())
        if (security >= 0) {
            spSecurity?.setSelection(security)
        }

        val streamSetting = config.outboundBean?.streamSettings ?: return true
        val streamSecurity = Utils.arrayFind(streamSecuritys, streamSetting.security)
        if (streamSecurity >= 0) {
            spStreamSecurity?.setSelection(streamSecurity)
            (streamSetting.tlsSettings ?: streamSetting.realitySettings)?.let { tlsSetting ->
                containerSni?.visibility = View.VISIBLE
                containerFingerprint?.visibility = View.VISIBLE
                containerAlpn?.visibility = View.VISIBLE
                etSni?.text = Utils.getEditable(tlsSetting.serverName)
                tlsSetting.fingerprint?.let {
                    val utlsIndex = Utils.arrayFind(uTlsItems, tlsSetting.fingerprint)
                    spStreamFingerprint?.setSelection(utlsIndex)
                }
                tlsSetting.alpn?.let {
                    val alpnIndex = Utils.arrayFind(
                        alpns,
                        Utils.removeWhiteSpace(tlsSetting.alpn.joinToString(",")).orEmpty()
                    )
                    spStreamAlpn?.setSelection(alpnIndex)
                }
                if (streamSetting.tlsSettings != null) {
                    containerAllowInsecure?.visibility = View.VISIBLE
                    val allowinsecure =
                        Utils.arrayFind(allowinsecures, tlsSetting.allowInsecure.toString())
                    if (allowinsecure >= 0) {
                        spAllowInsecure?.setText(allowinsecures[allowinsecure], false)
                    }
                    containerPublicKey?.visibility = View.GONE
                    containerShortId?.visibility = View.GONE
                    containerSpiderX?.visibility = View.GONE
                } else { // reality settings
                    containerPublicKey?.visibility = View.VISIBLE
                    etPublicKey?.text = Utils.getEditable(tlsSetting.publicKey.orEmpty())
                    containerShortId?.visibility = View.VISIBLE
                    etShortId?.text = Utils.getEditable(tlsSetting.shortId.orEmpty())
                    containerSpiderX?.visibility = View.VISIBLE
                    etSpiderX?.text = Utils.getEditable(tlsSetting.spiderX.orEmpty())
                    containerAllowInsecure?.visibility = View.GONE
                }
            }
            if (streamSetting.tlsSettings == null && streamSetting.realitySettings == null) {
                containerSni?.visibility = View.GONE
                containerFingerprint?.visibility = View.GONE
                containerAlpn?.visibility = View.GONE
                containerAllowInsecure?.visibility = View.GONE
                containerPublicKey?.visibility = View.GONE
                containerShortId?.visibility = View.GONE
                containerSpiderX?.visibility = View.GONE
            }
        }
        val network = Utils.arrayFind(networks, streamSetting.network)
        if (network >= 0) {
            spNetwork?.setSelection(network)
        }
        return true
    }

    /**
     * clear or init server config
     */
    private fun clearServer(): Boolean {
        etRemarks.text = null
        etAddress.text = null
        etPort.text = Utils.getEditable(DEFAULT_PORT.toString())
        etId.text = null
        etAlterID?.text = Utils.getEditable("0")
        spSecurity?.setSelection(0)
        spNetwork?.setSelection(0)

        spHeaderType?.setSelection(0)
        etRequestHost?.text = null
        etPath?.text = null
        spStreamSecurity?.setSelection(0)
        spAllowInsecure?.setSelection(0)
        etSni?.text = null

        //et_security.text = null
        spFlow?.text = null
        etPublicKey?.text = null
        etReserved1?.text = Utils.getEditable("0")
        etReserved2?.text = Utils.getEditable("0")
        etReserved3?.text = Utils.getEditable("0")
        etLocalAddress?.text =
            Utils.getEditable("${WIREGUARD_LOCAL_ADDRESS_V4},${WIREGUARD_LOCAL_ADDRESS_V6}")
        etLocalMtu?.text = Utils.getEditable(WIREGUARD_LOCAL_MTU)
        return true
    }

    /**
     * save server config
     */
    private fun saveServer(): Boolean {
        if (TextUtils.isEmpty(etRemarks.text.toString())) {
            toast(R.string.server_lab_remarks)
            return false
        }
        if (TextUtils.isEmpty(etAddress.text.toString())) {
            toast(R.string.server_lab_address)
            return false
        }
        val port = Utils.parseInt(etPort.text.toString())
        if (port <= 0) {
            toast(R.string.server_lab_port)
            return false
        }
        val config =
            MmkvManager.decodeServerConfig(editGuid) ?: ServerConfig.create(createConfigType)
        if (config.configType != EConfigType.SOCKS
            && config.configType != EConfigType.HTTP
            && TextUtils.isEmpty(etId.text.toString())
        ) {
            if (config.configType == EConfigType.TROJAN
                || config.configType == EConfigType.SHADOWSOCKS
                || config.configType == EConfigType.HYSTERIA2
            ) {
                toast(R.string.server_lab_id3)
            } else {
                toast(R.string.server_lab_id)
            }
            return false
        }
        spStreamSecurity?.let {
            if (config.configType == EConfigType.TROJAN && TextUtils.isEmpty(streamSecuritys[it.selectedItemPosition])) {
                toast(R.string.server_lab_stream_security)
                return false
            }
        }
        etAlterID?.let {
            val alterId = Utils.parseInt(it.text.toString())
            if (alterId < 0) {
                toast(R.string.server_lab_alterid)
                return false
            }
        }

        config.remarks = etRemarks.text.toString().trim()
        config.outboundBean?.settings?.vnext?.get(0)?.let { vnext ->
            saveVnext(vnext, port, config)
        }
        config.outboundBean?.settings?.servers?.get(0)?.let { server ->
            saveServers(server, port, config)
        }
        val wireguard = config.outboundBean?.settings
        wireguard?.peers?.get(0)?.let { _ ->
            savePeer(wireguard, port)
        }

        config.outboundBean?.streamSettings?.let {
            val sni = saveStreamSettings(it)
            saveTls(it, sni)
        }
        if (config.subscriptionId.isEmpty() && !subscriptionId.isNullOrEmpty()) {
            config.subscriptionId = subscriptionId.orEmpty()
        }
        if (config.configType == EConfigType.HYSTERIA2) {
            config.outboundBean?.settings?.obfuscatePassword = etObfsPassword?.text?.toString()
        }

        MmkvManager.encodeServerConfig(editGuid, config)
        isConfigurationChanged = false
        toast(R.string.toast_success)
        finish()
        return true
    }

    private fun saveVnext(
        vnext: V2rayConfig.OutboundBean.OutSettingsBean.VnextBean,
        port: Int,
        config: ServerConfig
    ) {
        vnext.address = etAddress.text.toString().trim()
        vnext.port = port
        vnext.users[0].id = etId.text.toString().trim()
        if (config.configType == EConfigType.VMESS) {
            vnext.users[0].alterId = Utils.parseInt(etAlterID?.text.toString())
            vnext.users[0].security = securitys[spSecurity?.selectedItemPosition ?: 0]
        } else if (config.configType == EConfigType.VLESS) {
            vnext.users[0].encryption = etSecurity?.text.toString().trim()
            vnext.users[0].flow = spFlow?.text.toString()
        }
    }

    private fun saveServers(
        server: V2rayConfig.OutboundBean.OutSettingsBean.ServersBean,
        port: Int,
        config: ServerConfig
    ) {
        server.address = etAddress.text.toString().trim()
        server.port = port
        if (config.configType == EConfigType.SHADOWSOCKS) {
            server.password = etId.text.toString().trim()
            server.method = shadowsocksSecurity[spSecurity?.selectedItemPosition ?: 0]
        } else if (config.configType == EConfigType.SOCKS || config.configType == EConfigType.HTTP) {
            if (TextUtils.isEmpty(etSecurity?.text) && TextUtils.isEmpty(etId.text)) {
                server.users = null
            } else {
                val socksUsersBean =
                    V2rayConfig.OutboundBean.OutSettingsBean.ServersBean.SocksUsersBean()
                socksUsersBean.user = etSecurity?.text.toString().trim()
                socksUsersBean.pass = etId.text.toString().trim()
                server.users = listOf(socksUsersBean)
            }
        } else if (config.configType == EConfigType.TROJAN || config.configType == EConfigType.HYSTERIA2) {
            server.password = etId.text.toString().trim()
        }
    }

    private fun savePeer(wireguard: V2rayConfig.OutboundBean.OutSettingsBean, port: Int) {
        wireguard.secretKey = etId.text.toString().trim()
        wireguard.peers?.get(0)?.publicKey = etPublicKey?.text.toString().trim()
        wireguard.peers?.get(0)?.endpoint =
            getIpv6Address(etAddress.text.toString().trim()) + ":" + port
        val reserved1 = Utils.parseInt(etReserved1?.text.toString())
        val reserved2 = Utils.parseInt(etReserved2?.text.toString())
        val reserved3 = Utils.parseInt(etReserved3?.text.toString())
        if (reserved1 > 0 || reserved2 > 0 || reserved3 > 0) {
            wireguard.reserved = listOf(reserved1, reserved2, reserved3)
        } else {
            wireguard.reserved = null
        }
        wireguard.address = etLocalAddress?.text.toString().removeWhiteSpace().split(",")
        wireguard.mtu = Utils.parseInt(etLocalMtu?.text.toString())
    }

    private fun saveStreamSettings(streamSetting: V2rayConfig.OutboundBean.StreamSettingsBean): String? {
        val network = spNetwork?.selectedItemPosition ?: return null
        val type = spHeaderType?.selectedItemPosition ?: return null
        val requestHost = etRequestHost?.text?.toString()?.trim() ?: return null
        val path = etPath?.text?.toString()?.trim() ?: return null

        val sni = streamSetting.populateTransportSettings(
            transport = networks[network],
            headerType = transportTypes(networks[network])[type],
            host = requestHost,
            path = path,
            seed = path,
            quicSecurity = requestHost,
            key = path,
            mode = transportTypes(networks[network])[type],
            serviceName = path,
            authority = requestHost,
        )

        return sni
    }

    private fun saveTls(streamSetting: V2rayConfig.OutboundBean.StreamSettingsBean, sni: String?) {
        val streamSecurity = spStreamSecurity?.selectedItemPosition ?: return
        val sniField = etSni?.text?.toString()?.trim()
        val allowInsecureField = spAllowInsecure?.text.toString()

        val utlsSelectedItem = spStreamFingerprint?.text?.toString()
        val utlsIndex = if (!utlsSelectedItem.isNullOrEmpty()) {
            uTlsItems.indexOf(utlsSelectedItem)
        } else {
            0
        }
        val alpnSelectedItem = spStreamAlpn?.text?.toString()
        val alpnIndex = if (!alpnSelectedItem.isNullOrEmpty()) {
            alpns.indexOf(alpnSelectedItem)
        } else {
            0 // Default index if nothing is selected
        }

        val publicKey = etPublicKey?.text?.toString()
        val shortId = etShortId?.text?.toString()
        val spiderX = etSpiderX?.text?.toString()

        val allowInsecure =
            if (allowInsecureField.isBlank()) {
                settingsStorage.decodeBool(PREF_ALLOW_INSECURE)
            } else {
                allowinsecures.find { it.equals(allowInsecureField, ignoreCase = true) }
                    ?.toBoolean() ?: false
            }

        streamSetting.populateTlsSettings(
            streamSecurity = streamSecuritys[streamSecurity],
            allowInsecure = allowInsecure,
            sni = sniField ?: sni ?: "",
            fingerprint = uTlsItems.getOrNull(utlsIndex) ?: "",
            alpns = alpns.getOrNull(alpnIndex) ?: "",
            publicKey = publicKey,
            shortId = shortId,
            spiderX = spiderX
        )
    }

    private fun transportTypes(network: String?): Array<out String> {
        return when (network) {
            "tcp" -> {
                tcpTypes
            }

            "kcp", "quic" -> {
                kcpAndQuicTypes
            }

            "grpc" -> {
                grpcModes
            }

            else -> {
                arrayOf("---")
            }
        }
    }

    /**
     * save server config
     */
    private fun deleteServer() {
        if (settingsStorage.decodeBool(AppConfig.PREF_CONFIRM_REMOVE)) {
            showDeleteConfirmationDialog()
        } else {
            application.toast(R.string.toast_action_not_allowed)
        }
    }

    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.del_config_confirm_title)
            .setMessage(R.string.del_config_confirm_current)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                MmkvManager.removeServer(editGuid)
                finish()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .show()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.action_server, menu)
        val delButton = menu.findItem(R.id.del_config)
        val saveButton = menu.findItem(R.id.save_config)

        if (editGuid.isNotEmpty()) {
            if (isRunning) {
                delButton?.isVisible = false
                saveButton?.isVisible = false
            }
        } else {
            delButton?.isVisible = false
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.del_config -> {
            deleteServer()
            true
        }

        R.id.save_config -> {
            saveServer()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }
}
