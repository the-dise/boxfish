package com.v2ray.ang.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.v2ray.ang.AngApplication.Companion.application
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ItemQrcodeBinding
import com.v2ray.ang.databinding.ItemRecyclerFooterBinding
import com.v2ray.ang.databinding.ItemRecyclerMainBinding
import com.v2ray.ang.dto.EConfigType
import com.v2ray.ang.extension.toast
import com.v2ray.ang.helper.ItemTouchHelperAdapter
import com.v2ray.ang.helper.ItemTouchHelperViewHolder
import com.v2ray.ang.service.V2RayServiceManager
import com.v2ray.ang.util.AngConfigManager
import com.v2ray.ang.util.MmkvManager
import com.v2ray.ang.util.MmkvManager.settingsStorage
import com.v2ray.ang.util.Utils
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import java.util.concurrent.TimeUnit

class MainRecyclerAdapter(val activity: MainActivity) :
    RecyclerView.Adapter<MainRecyclerAdapter.BaseViewHolder>(), ItemTouchHelperAdapter {
    companion object {
        private const val VIEW_TYPE_ITEM = 1
        private const val VIEW_TYPE_FOOTER = 2
    }

    private var mActivity: MainActivity = activity
    private val shareMethod: Array<out String> by lazy {
        mActivity.resources.getStringArray(R.array.share_method)
    }
    var isRunning = false

    override fun getItemCount() = mActivity.mainViewModel.serversCache.size + 1

    @SuppressLint("CheckResult")
    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        if (holder is MainViewHolder) {
            val guid = mActivity.mainViewModel.serversCache[position].guid
            val profile = mActivity.mainViewModel.serversCache[position].profile
//            //filter
//            if (mActivity.mainViewModel.subscriptionId.isNotEmpty()
//                && mActivity.mainViewModel.subscriptionId != config.subscriptionId
//            ) {
//                holder.itemMainBinding.cardView.visibility = View.GONE
//            } else {
//                holder.itemMainBinding.cardView.visibility = View.VISIBLE
//            }

            val aff = MmkvManager.decodeServerAffiliationInfo(guid)

            val colorPrimary = MaterialColors.getColor(
                holder.itemMainBinding.tvTestResult,
                com.google.android.material.R.attr.colorPrimary
            )
            val colorSecondary = MaterialColors.getColor(
                holder.itemMainBinding.tvTestResult,
                com.google.android.material.R.attr.colorSecondary
            )
            val colorError = MaterialColors.getColor(
                holder.itemMainBinding.tvTestResult,
                com.google.android.material.R.attr.colorError
            )

            holder.itemMainBinding.tvName.text = profile.remarks
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
            holder.itemMainBinding.tvTestResult.text = aff?.getTestDelayString().orEmpty()
            if ((aff?.testDelayMillis ?: 0L) < 0L) {
                holder.itemMainBinding.tvTestResult.setTextColor(colorError)
            } else {
                holder.itemMainBinding.tvTestResult.setTextColor(colorSecondary)
            }
            if (guid == MmkvManager.getSelectServer()) {
                holder.itemMainBinding.layoutIndicator.setBackgroundColor(colorPrimary)
            } else {
                holder.itemMainBinding.layoutIndicator.setBackgroundResource(0)
            }
            holder.itemMainBinding.tvSubscription.text =
                MmkvManager.decodeSubscription(profile.subscriptionId)?.remarks ?: ""

            var shareOptions = shareMethod.asList()

            when (profile.configType) {
                EConfigType.CUSTOM -> {
                    holder.itemMainBinding.tvType.text =
                        mActivity.getString(R.string.server_customize_config)
                    shareOptions = shareOptions.takeLast(1)
                }

                EConfigType.VLESS -> {
                    holder.itemMainBinding.tvType.text =
                        mActivity.getString(R.string.protocol_vless)
                }

                EConfigType.WIREGUARD -> {
                    holder.itemMainBinding.tvType.text =
                        mActivity.getString(R.string.protocol_wireguard)
                }

                EConfigType.VMESS -> {
                    holder.itemMainBinding.tvType.text =
                        mActivity.getString(R.string.protocol_vmess)
                }

                EConfigType.SOCKS -> {
                    holder.itemMainBinding.tvType.text =
                        mActivity.getString(R.string.protocol_socks)
                }

                EConfigType.SHADOWSOCKS -> {
                    holder.itemMainBinding.tvType.text =
                        mActivity.getString(R.string.protocol_shadowsocks)
                }

                EConfigType.TROJAN -> {
                    holder.itemMainBinding.tvType.text =
                        mActivity.getString(R.string.protocol_trojan)
                }

                EConfigType.HTTP -> {
                    holder.itemMainBinding.tvType.text =
                        mActivity.getString(R.string.protocol_http)
                }

                EConfigType.HYSTERIA2 -> {
                    holder.itemMainBinding.tvType.text =
                        mActivity.getString(R.string.protocol_hysteria2)
                }
            }

            // Hide homepage server address is xxx:xxx:***/xxx.xxx.xxx.****
            val strState = "${
                profile.server?.let {
                    if (it.contains(":"))
                        it.split(":").take(2).joinToString(":", postfix = ":***")
                    else
                        it.split('.').dropLast(1).joinToString(".", postfix = ".***")
                }
            }:${profile.serverPort}"

            holder.itemMainBinding.tvStatistics.text = strState

            holder.itemMainBinding.layoutShare.setOnClickListener {
                MaterialAlertDialogBuilder(mActivity).setItems(shareOptions.toTypedArray()) { _, i ->
                    try {
                        when (i) {
                            0 -> {
                                if (profile.configType == EConfigType.CUSTOM) {
                                    shareFullContent(guid)
                                } else {
                                    val ivBinding =
                                        ItemQrcodeBinding.inflate(LayoutInflater.from(mActivity))
                                    ivBinding.ivQRCode.setImageBitmap(
                                        AngConfigManager.share2QRCode(
                                            guid
                                        )
                                    )
                                    MaterialAlertDialogBuilder(mActivity).setView(ivBinding.root)
                                        .setTitle(R.string.scanner_qr_code)
                                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                                            // Respond to negative button press
                                        }
                                        .show()
                                }
                            }

                            1 -> {
                                if (AngConfigManager.share2Clipboard(mActivity, guid) == 0) {
                                    mActivity.toast(R.string.toast_success)
                                } else {
                                    mActivity.toast(R.string.toast_failure)
                                }
                            }

                            2 -> shareFullContent(guid)
                            else -> mActivity.toast("else")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }.setTitle(R.string.title_share)
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        // Respond to negative button press
                    }
                    .show()
            }

            holder.itemMainBinding.layoutEdit.setOnClickListener {
                val intent = Intent().putExtra("guid", guid)
                    .putExtra("isRunning", isRunning)
                if (profile.configType == EConfigType.CUSTOM) {
                    mActivity.startActivity(
                        intent.setClass(
                            mActivity,
                            ServerCustomConfigActivity::class.java
                        )
                    )
                } else {
                    mActivity.startActivity(intent.setClass(mActivity, ServerActivity::class.java))
                }
            }
            holder.itemMainBinding.layoutRemove.setOnClickListener {
                if (guid != MmkvManager.getSelectServer()) {
                    val message =
                        mActivity.getString(R.string.del_config_confirm_this, profile.remarks)

                    if (settingsStorage.decodeBool(AppConfig.PREF_CONFIRM_REMOVE)) {
                        MaterialAlertDialogBuilder(mActivity)
                            .setTitle(R.string.del_config_confirm_title)
                            .setMessage(message)

                            .setNegativeButton(android.R.string.cancel) { _, _ ->
                                // Respond to negative button press
                            }
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                removeServer(guid, position)
                            }
                            .show()
                    } else {
                        removeServer(guid, position)
                    }
                } else {
                    application.toast(R.string.toast_action_not_allowed)
                }
            }

            holder.itemMainBinding.infoContainer.setOnClickListener {
                val selected = MmkvManager.getSelectServer()
                if (guid != selected) {
                    MmkvManager.setSelectServer(guid)
                    if (!TextUtils.isEmpty(selected)) {
                        notifyItemChanged(mActivity.mainViewModel.getPosition(selected.orEmpty()))
                    }
                    notifyItemChanged(mActivity.mainViewModel.getPosition(guid))
                    if (isRunning) {
                        Utils.stopVService(mActivity)
                        Observable.timer(500, TimeUnit.MILLISECONDS)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                V2RayServiceManager.startV2Ray(mActivity)
                            }
                    }
                }
            }
        }
        if (holder is FooterViewHolder) {
            holder.itemFooterBinding.layoutEdit.visibility = View.INVISIBLE
        }
    }

    private fun shareFullContent(guid: String) {
        if (AngConfigManager.shareFullContent2Clipboard(mActivity, guid) == 0) {
            mActivity.toast(R.string.toast_success)
        } else {
            mActivity.toast(R.string.toast_failure)
        }
    }

    private fun removeServer(guid: String, position: Int) {
        mActivity.mainViewModel.removeServer(guid)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, mActivity.mainViewModel.serversCache.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return when (viewType) {
            VIEW_TYPE_ITEM ->
                MainViewHolder(
                    ItemRecyclerMainBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )

            else ->
                FooterViewHolder(
                    ItemRecyclerFooterBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == mActivity.mainViewModel.serversCache.size) {
            VIEW_TYPE_FOOTER
        } else {
            VIEW_TYPE_ITEM
        }
    }

    open class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun onItemSelected() {
            itemView.isFocusable = true
        }

        fun onItemClear() {
            itemView.isFocusable = false
        }
    }

    class MainViewHolder(val itemMainBinding: ItemRecyclerMainBinding) :
        BaseViewHolder(itemMainBinding.root), ItemTouchHelperViewHolder

    class FooterViewHolder(val itemFooterBinding: ItemRecyclerFooterBinding) :
        BaseViewHolder(itemFooterBinding.root)

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        mActivity.mainViewModel.swapServer(fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    override fun onItemMoveCompleted() {
        // do nothing
    }

    override fun onItemDismiss(position: Int) {
    }
}
