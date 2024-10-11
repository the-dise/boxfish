package com.v2ray.ang.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputFilter
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityRoutingEditBinding
import com.v2ray.ang.dto.RulesetItem
import com.v2ray.ang.extension.toast
import com.v2ray.ang.util.SettingsManager
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RoutingEditActivity : BaseActivity() {
    private val binding by lazy { ActivityRoutingEditBinding.inflate(layoutInflater) }
    private val position by lazy { intent.getIntExtra("position", -1) }

    private val outboundTag: Array<out String> by lazy {
        resources.getStringArray(R.array.outbound_tag)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.routing_settings_rule_title) // Title

        // Handling a click on navigationIcon (back button)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed() // Go back to previous screen
        }

        val rulesetItem = SettingsManager.getRoutingRuleset(position)
        if (rulesetItem != null) {
            bindingServer(rulesetItem)
        } else {
            clearServer()
        }

        applyLowerCaseFilter()
    }

    private fun applyLowerCaseFilter() {
        val lowerCaseFilter = InputFilter { source, _, _, _, _, _ ->
            source.toString().lowercase()
        }

        binding.etRemarks.filters = arrayOf(lowerCaseFilter)
        binding.etDomain.filters = arrayOf(lowerCaseFilter)
        binding.etIp.filters = arrayOf(lowerCaseFilter)
        binding.etPort.filters = arrayOf(lowerCaseFilter)
        binding.etProtocol.filters = arrayOf(lowerCaseFilter)
        binding.etNetwork.filters = arrayOf(lowerCaseFilter)
    }

    private fun bindingServer(rulesetItem: RulesetItem): Boolean {
        binding.etRemarks.text = Utils.getEditable(rulesetItem.remarks)
        binding.chkLocked.isChecked = rulesetItem.looked ?: false
        binding.etDomain.text = Utils.getEditable(rulesetItem.domain?.joinToString(","))
        binding.etIp.text = Utils.getEditable(rulesetItem.ip?.joinToString(","))
        binding.etPort.text = Utils.getEditable(rulesetItem.port)
        binding.etProtocol.text = Utils.getEditable(rulesetItem.protocol?.joinToString(","))
        binding.etNetwork.text = Utils.getEditable(rulesetItem.network)
        val outbound = Utils.arrayFind(outboundTag, rulesetItem.outboundTag)
        if (outbound >= 0) {
            binding.spOutboundTag.setText(outboundTag[outbound], false)
        }
        return true
    }

    private fun clearServer(): Boolean {
        binding.etRemarks.text = null
        binding.spOutboundTag.text = null
        return true
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun saveServer(): Boolean {
        val rulesetItem = SettingsManager.getRoutingRuleset(position) ?: RulesetItem()

        rulesetItem.remarks = binding.etRemarks.text.toString()
        rulesetItem.looked = binding.chkLocked.isChecked
        binding.etDomain.text.toString().let {
            rulesetItem.domain = if (it.isEmpty()) null else it.split(",").map { itt -> itt.trim() }
                .filter { itt -> itt.isNotEmpty() }
        }
        binding.etIp.text.toString().let {
            rulesetItem.ip = if (it.isEmpty()) null else it.split(",").map { itt -> itt.trim() }
                .filter { itt -> itt.isNotEmpty() }
        }
        binding.etProtocol.text.toString().let {
            rulesetItem.protocol =
                if (it.isEmpty()) null else it.split(",").map { itt -> itt.trim() }
                    .filter { itt -> itt.isNotEmpty() }
        }
        binding.etPort.text.toString().let { rulesetItem.port = it.ifEmpty { null } }
        binding.etNetwork.text.toString().let { rulesetItem.network = it.ifEmpty { null } }
        rulesetItem.outboundTag = binding.spOutboundTag.text.toString()

        if (TextUtils.isEmpty(rulesetItem.remarks)) {
            toast(R.string.sub_setting_remarks)
            return false
        }

        SettingsManager.saveRoutingRuleset(position, rulesetItem)
        toast(R.string.toast_success)
        finish()
        return true
    }

    private fun deleteServer(): Boolean {
        if (position >= 0) {
            MaterialAlertDialogBuilder(this).setMessage(R.string.del_config_confirm)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        SettingsManager.removeRoutingRuleset(position)
                        launch(Dispatchers.Main) {
                            finish()
                        }
                    }
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    // do nothing
                }
                .show()
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.action_server, menu)
        val delConfig = menu.findItem(R.id.del_config)

        if (position < 0) {
            delConfig?.isVisible = false
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
