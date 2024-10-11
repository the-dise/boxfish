package com.v2ray.ang.ui

import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityUserAssetUrlBinding
import com.v2ray.ang.dto.AssetUrlItem
import com.v2ray.ang.extension.toast
import com.v2ray.ang.util.MmkvManager
import com.v2ray.ang.util.Utils
import java.io.File

class UserAssetUrlActivity : BaseActivity() {
    private val binding by lazy { ActivityUserAssetUrlBinding.inflate(layoutInflater) }

    private var delConfig: MenuItem? = null
    private var saveConfig: MenuItem? = null

    private val extDir by lazy { File(Utils.userAssetPath(this)) }
    private val editAssetId by lazy { intent.getStringExtra("assetId").orEmpty() }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // Setup Toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_user_asset_add_url) // Title

        // Handling a click on navigationIcon (back button)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed() // Go back to previous screen
        }

        val assetItem = MmkvManager.decodeAsset(editAssetId)
        if (assetItem != null) {
            bindingAsset(assetItem)
        } else {
            clearAsset()
        }
    }

    /**
     * binding selected asset config
     */
    private fun bindingAsset(assetItem: AssetUrlItem): Boolean {
        binding.etRemarks.text = Utils.getEditable(assetItem.remarks)
        binding.etUrl.text = Utils.getEditable(assetItem.url)
        return true
    }

    /**
     * clear or init asset config
     */
    private fun clearAsset(): Boolean {
        binding.etRemarks.text = null
        binding.etUrl.text = null
        return true
    }

    /**
     * save asset config
     */
    private fun saveServer(): Boolean {
        var assetItem = MmkvManager.decodeAsset(editAssetId)
        var assetId = editAssetId
        if (assetItem != null) {
            // remove file associated with the asset
            val file = extDir.resolve(assetItem.remarks)
            if (file.exists()) {
                file.delete()
            }
        } else {
            assetId = Utils.getUuid()
            assetItem = AssetUrlItem()
        }

        assetItem.remarks = binding.etRemarks.text.toString()
        assetItem.url = binding.etUrl.text.toString()

        // check remarks unique
        val assetList = MmkvManager.decodeAssetUrls()
        if (assetList.any { it.second.remarks == assetItem.remarks && it.first != assetId }) {
            toast(R.string.msg_remark_is_duplicate)
            return false
        }


        if (TextUtils.isEmpty(assetItem.remarks)) {
            toast(R.string.sub_setting_remarks)
            return false
        }
        if (TextUtils.isEmpty(assetItem.url)) {
            toast(R.string.title_url)
            return false
        }

        MmkvManager.encodeAsset(assetId, assetItem)
        toast(R.string.toast_success)
        finish()
        return true
    }

    /**
     * save server config
     */
    private fun deleteServer(): Boolean {
        if (editAssetId.isNotEmpty()) {
            MaterialAlertDialogBuilder(this).setMessage(R.string.del_config_confirm)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    MmkvManager.removeAssetUrl(editAssetId)
                    finish()
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
        delConfig = menu.findItem(R.id.del_config)
        saveConfig = menu.findItem(R.id.save_config)

        if (editAssetId.isEmpty()) {
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