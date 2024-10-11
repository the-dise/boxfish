package com.v2ray.ang.ui

import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import com.blacksquircle.ui.editorkit.model.ColorScheme
import com.blacksquircle.ui.editorkit.utils.EditorTheme
import com.blacksquircle.ui.language.json.JsonLanguage
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityServerCustomConfigBinding
import com.v2ray.ang.dto.EConfigType
import com.v2ray.ang.dto.ServerConfig
import com.v2ray.ang.dto.V2rayConfig
import com.v2ray.ang.extension.toast
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.MmkvManager
import com.v2ray.ang.util.Utils
import me.drakeet.support.toast.ToastCompat

class ServerCustomConfigActivity : BaseActivity() {
    private val binding by lazy { ActivityServerCustomConfigBinding.inflate(layoutInflater) }

    private val editGuid by lazy { intent.getStringExtra("guid").orEmpty() }
    private val isRunning by lazy {
        intent.getBooleanExtra("isRunning", false)
                && editGuid.isNotEmpty()
                && editGuid == MmkvManager.getSelectServer()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Setup Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_server) // Title

        // Handling a click on navigationIcon (back button)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed() // Go back to previous screen
        }


        if (!Utils.getDarkModeStatus(this)) {
            binding.editor.colorScheme = EditorTheme.INTELLIJ_LIGHT
        }
        binding.editor.language = JsonLanguage()
        val config = MmkvManager.decodeServerConfig(editGuid)
        if (config != null) {
            bindingServer(config)
        } else {
            clearServer()
        }
    }

    /**
     * binding selected server config
     */
    private fun bindingServer(config: ServerConfig): Boolean {
        binding.etRemarks.text = Utils.getEditable(config.remarks)
        val raw = MmkvManager.decodeServerRaw(editGuid)
        if (raw.isNullOrBlank()) {
            binding.editor.setTextContent(
                Utils.getEditable(
                    config.fullConfig?.toPrettyPrinting().orEmpty()
                )
            )
        } else {
            binding.editor.setTextContent(Utils.getEditable(raw))
        }
        binding.editor.typeface = ResourcesCompat.getFont(this, R.font.roboto_mono)
        binding.editor.textSize = 14F

        val m = ColorScheme(
            textColor = MaterialColors.getColor(binding.editor, com.google.android.material.R.attr.colorOnSurface),
            cursorColor = MaterialColors.getColor(binding.editor, com.google.android.material.R.attr.colorPrimary),
            backgroundColor = MaterialColors.getColor(binding.editor, com.google.android.material.R.attr.colorSurface),
            gutterColor = MaterialColors.getColor(binding.editor, com.google.android.material.R.attr.colorSurfaceVariant),
            gutterDividerColor = MaterialColors.getColor(binding.editor, com.google.android.material.R.attr.colorOutline),
            gutterCurrentLineNumberColor = MaterialColors.getColor(binding.editor, com.google.android.material.R.attr.colorOnSurfaceVariant),
            gutterTextColor = MaterialColors.getColor(binding.editor, com.google.android.material.R.attr.colorOnSurface),
            selectedLineColor = MaterialColors.getColor(binding.editor, com.google.android.material.R.attr.colorSurfaceVariant),
            selectionColor = MaterialColors.getColor(binding.editor, com.google.android.material.R.attr.colorTertiary),
            suggestionQueryColor = MaterialColors.getColor(binding.editor, com.google.android.material.R.attr.colorTertiary),
            findResultBackgroundColor = MaterialColors.getColor(binding.editor, com.google.android.material.R.attr.colorSecondary),
            delimiterBackgroundColor = MaterialColors.getColor(binding.editor, com.google.android.material.R.attr.colorSecondaryVariant),
            numberColor = MaterialColors.getColor(binding.editor, com.google.android.material.R.attr.colorPrimary),
            operatorColor = MaterialColors.getColor(binding.editor, com.google.android.material.R.attr.colorPrimaryVariant),
            keywordColor = MaterialColors.getColor(binding.editor, com.google.android.material.R.attr.colorError), // true/false
            typeColor = MaterialColors.getColor(binding.editor, com.google.android.material.R.attr.colorSecondary),
            langConstColor = MaterialColors.getColor(binding.editor, com.google.android.material.R.attr.colorTertiary),
            preprocessorColor = MaterialColors.getColor(binding.editor, com.google.android.material.R.attr.colorTertiary),
            variableColor = MaterialColors.getColor(binding.editor, com.google.android.material.R.attr.colorSecondary),
            methodColor = MaterialColors.getColor(binding.editor, com.google.android.material.R.attr.colorTertiary),
            stringColor = MaterialColors.getColor(binding.editor, com.google.android.material.R.attr.colorPrimary),
            commentColor = MaterialColors.getColor(binding.editor, com.google.android.material.R.attr.colorOnSurfaceVariant),
            tagColor = MaterialColors.getColor(binding.editor, com.google.android.material.R.attr.colorOnSurface),
            tagNameColor = MaterialColors.getColor(binding.editor, com.google.android.material.R.attr.colorPrimary),
            attrNameColor = MaterialColors.getColor(binding.editor, com.google.android.material.R.attr.colorSecondary),
            attrValueColor = MaterialColors.getColor(binding.editor, com.google.android.material.R.attr.colorPrimary),
            entityRefColor = MaterialColors.getColor(binding.editor, com.google.android.material.R.attr.colorPrimary)
        )

        binding.editor.colorScheme = m // EditorTheme.DARCULA // m
        return true
    }

    /**
     * clear or init server config
     */
    private fun clearServer(): Boolean {
        binding.etRemarks.text = null
        return true
    }

    /**
     * save server config
     */
    private fun saveServer(): Boolean {
        if (TextUtils.isEmpty(binding.etRemarks.text.toString())) {
            toast(R.string.server_lab_remarks)
            return false
        }

        val v2rayConfig = try {
            JsonUtil.fromJson(binding.editor.text.toString(), V2rayConfig::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            ToastCompat.makeText(
                this,
                "${getString(R.string.toast_malformed_json)} ${e.cause?.message}",
                Toast.LENGTH_LONG
            ).show()
            return false
        }

        val config =
            MmkvManager.decodeServerConfig(editGuid) ?: ServerConfig.create(EConfigType.CUSTOM)
        config.remarks =
            if (binding.etRemarks.text.isNullOrEmpty()) v2rayConfig.remarks.orEmpty() else binding.etRemarks.text.toString()
        config.fullConfig = v2rayConfig

        MmkvManager.encodeServerConfig(editGuid, config)
        MmkvManager.encodeServerRaw(editGuid, binding.editor.text.toString())
        toast(R.string.toast_success)
        finish()
        return true
    }

    /**
     * save server config
     */
    private fun deleteServer(): Boolean {
        if (editGuid.isNotEmpty()) {
            MaterialAlertDialogBuilder(this).setMessage(R.string.del_config_confirm)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    MmkvManager.removeServer(editGuid)
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
