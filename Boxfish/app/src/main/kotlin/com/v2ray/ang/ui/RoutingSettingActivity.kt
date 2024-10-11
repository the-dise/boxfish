package com.v2ray.ang.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityRoutingSettingBinding
import com.v2ray.ang.dto.RulesetItem
import com.v2ray.ang.extension.toast
import com.v2ray.ang.helper.SimpleItemTouchHelperCallback
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.MmkvManager
import com.v2ray.ang.util.MmkvManager.settingsStorage
import com.v2ray.ang.util.SettingsManager
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RoutingSettingActivity : BaseActivity() {
    private val binding by lazy { ActivityRoutingSettingBinding.inflate(layoutInflater) }

    var rulesets: MutableList<RulesetItem> = mutableListOf()
    private val adapter by lazy { RoutingSettingRecyclerAdapter(this) }
    private var mItemTouchHelper: ItemTouchHelper? = null
    private val routingDomainStrategy: Array<out String> by lazy {
        resources.getStringArray(R.array.routing_domain_strategy)
    }
    private val presetRulesets: Array<out String> by lazy {
        resources.getStringArray(R.array.preset_rulesets)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // Setup Toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.routing_settings_title) // Title

        // Handling a click on navigationIcon (back button)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed() // Go back to previous screen
        }

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        mItemTouchHelper = ItemTouchHelper(SimpleItemTouchHelperCallback(adapter))
        mItemTouchHelper?.attachToRecyclerView(binding.recyclerView)

        // Set up AutoCompleteTextView with adapter
        val autoCompleteTextView = binding.spDomainStrategy
        val domainStrategyAdapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, routingDomainStrategy)
        autoCompleteTextView.setAdapter(domainStrategyAdapter)

        // Set selected item based on settings
        val currentStrategy =
            settingsStorage.decodeString(AppConfig.PREF_ROUTING_DOMAIN_STRATEGY) ?: ""
        val foundIndex = Utils.arrayFind(routingDomainStrategy, currentStrategy)
        if (foundIndex >= 0) {
            autoCompleteTextView.setText(routingDomainStrategy[foundIndex], false)
        } else {
            autoCompleteTextView.setText(routingDomainStrategy[0], false)
        }

        // Handle selection changes
        autoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
            settingsStorage.encode(
                AppConfig.PREF_ROUTING_DOMAIN_STRATEGY,
                routingDomainStrategy[position]
            )
        }
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_routing_setting, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.add_rule -> {
            startActivity(Intent(this, RoutingEditActivity::class.java))
            true
        }

        R.id.user_asset_setting -> {
            startActivity(Intent(this, UserAssetActivity::class.java))
            true
        }

        R.id.import_rulesets -> {
            MaterialAlertDialogBuilder(this).setMessage(R.string.routing_settings_import_rulesets_tip)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    MaterialAlertDialogBuilder(this)
                        .setItems(presetRulesets.asList().toTypedArray()) { _, i ->
                            try {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    SettingsManager.resetRoutingRulesets(
                                        this@RoutingSettingActivity,
                                        i
                                    )
                                    launch(Dispatchers.Main) {
                                        refreshData()
                                        toast(R.string.toast_success)
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }.show()


                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    //do nothing
                }
                .show()
            true
        }

        R.id.import_rulesets_from_clipboard -> {
            MaterialAlertDialogBuilder(this).setMessage(R.string.routing_settings_import_rulesets_tip)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    try {
                        val clipboard = Utils.getClipboard(this)
                        lifecycleScope.launch(Dispatchers.IO) {
                            val ret = SettingsManager.resetRoutingRulesetsFromClipboard(clipboard)
                            launch(Dispatchers.Main) {
                                if (ret) {
                                    refreshData()
                                    toast(R.string.toast_success)
                                } else {
                                    toast(R.string.toast_failure)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    //do nothing
                }
                .show()
            true
        }

        R.id.export_rulesets_to_clipboard -> {
            val rulesetList = MmkvManager.decodeRoutingRulesets()
            if (rulesetList.isNullOrEmpty()) {
                toast(R.string.toast_failure)
            } else {
                Utils.setClipboard(this, JsonUtil.toJson(rulesetList))
                toast(R.string.toast_success)
            }
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    fun refreshData() {
        rulesets = MmkvManager.decodeRoutingRulesets() ?: mutableListOf()
        adapter.notifyDataSetChanged()
    }
}