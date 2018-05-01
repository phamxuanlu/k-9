package com.fsck.k9.ui.endtoend


import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.os.Build
import android.os.Bundle
import android.transition.TransitionInflater
import android.transition.TransitionManager
import android.view.View
import com.fsck.k9.R
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.finishWithErrorToast
import com.fsck.k9.mail.TransportProvider
import com.fsck.k9.view.StatusIndicator
import kotlinx.android.synthetic.main.crypto_key_transfer.*
import org.koin.android.architecture.ext.viewModel
import org.koin.android.ext.android.inject
import org.openintents.openpgp.OpenPgpApiManager
import timber.log.Timber


class AutocryptKeyTransferActivity : K9Activity(), AutocryptKeyTransferView {
    private val viewModel: AutocryptKeyTransferViewModel by viewModel()
    private val transportProvider: TransportProvider by inject()
    private val openPgpApiManager: OpenPgpApiManager by inject { mapOf("lifecycle" to lifecycle) }
    private lateinit var presenter: AutocryptKeyTransferPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.crypto_key_transfer)

        val account = intent.getStringExtra(EXTRA_ACCOUNT)

        transfer_send_button.setOnClickListener { presenter.onClickTransferSend() }
        transfer_show_code_button.setOnClickListener { presenter.onClickShowTransferCode() }

        presenter = AutocryptKeyTransferPresenter(applicationContext, this, this, viewModel, openPgpApiManager, transportProvider)
        presenter.initFromIntent(account)
    }

    override fun setAddress(address: String) {
        transfer_address_1.text = address
        transfer_address_2.text = address
    }

    override fun sceneBegin() {
        transfer_send_button.visibility = View.VISIBLE
        transfer_msg_info.visibility = View.VISIBLE
        transfer_layout_generating.visibility = View.GONE
        transfer_layout_sending.visibility = View.GONE
        transfer_layout_finish.visibility = View.GONE
        transfer_error_send.visibility = View.GONE
        transfer_show_code_button.visibility = View.GONE
    }

    override fun sceneGeneratingAndSending() {
        setupSceneTransition()

        transfer_send_button.visibility = View.GONE
        transfer_msg_info.visibility = View.GONE
        transfer_layout_generating.visibility = View.VISIBLE
        transfer_layout_sending.visibility = View.VISIBLE
        transfer_layout_finish.visibility = View.GONE
        transfer_error_send.visibility = View.GONE
        transfer_show_code_button.visibility = View.GONE
    }

    override fun sceneSendError() {
        setupSceneTransition()

        transfer_send_button.visibility = View.GONE
        transfer_msg_info.visibility = View.GONE
        transfer_layout_generating.visibility = View.VISIBLE
        transfer_layout_sending.visibility = View.VISIBLE
        transfer_layout_finish.visibility = View.GONE
        transfer_error_send.visibility = View.VISIBLE
        transfer_show_code_button.visibility = View.GONE
    }

    override fun sceneFinished(transition: Boolean) {
        if (transition) {
            setupSceneTransition()
        }

        transfer_send_button.visibility = View.GONE
        transfer_msg_info.visibility = View.GONE
        transfer_layout_generating.visibility = View.VISIBLE
        transfer_layout_sending.visibility = View.VISIBLE
        transfer_layout_finish.visibility = View.VISIBLE
        transfer_error_send.visibility = View.GONE
        transfer_show_code_button.visibility = View.VISIBLE
    }

    override fun setLoadingStateGenerating() {
        transfer_progress_generating.setDisplayedChild(StatusIndicator.Status.PROGRESS)
        transfer_progress_sending.setDisplayedChild(StatusIndicator.Status.IDLE)
    }

    override fun setLoadingStateSending() {
        transfer_progress_generating.setDisplayedChild(StatusIndicator.Status.OK)
        transfer_progress_sending.setDisplayedChild(StatusIndicator.Status.PROGRESS)
    }

    override fun setLoadingStateSendingFailed() {
        transfer_progress_generating.setDisplayedChild(StatusIndicator.Status.OK)
        transfer_progress_sending.setDisplayedChild(StatusIndicator.Status.ERROR)
    }

    override fun setLoadingStateFinished() {
        transfer_progress_generating.setDisplayedChild(StatusIndicator.Status.OK)
        transfer_progress_sending.setDisplayedChild(StatusIndicator.Status.OK)
    }

    override fun finishWithInvalidAccountError() {
        finishWithErrorToast(R.string.toast_account_not_found)
    }

    override fun finishWithProviderConnectError(providerName: String) {
        finishWithErrorToast(R.string.toast_openpgp_provider_error, providerName)
    }

    override fun launchUserInteractionPendingIntent(pendingIntent: PendingIntent) {
        try {
            startIntentSender(pendingIntent.intentSender, null, 0, 0, 0)
        } catch (e: SendIntentException) {
            Timber.e(e)
        }
    }

    private fun setupSceneTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val transition = TransitionInflater.from(this).inflateTransition(R.transition.transfer_transitions)
            TransitionManager.beginDelayedTransition(findViewById(android.R.id.content), transition)
        }
    }

    companion object {
        private const val EXTRA_ACCOUNT = "account"

        fun createIntent(context: Context, accountUuid: String): Intent {
            val intent = Intent(context, AutocryptKeyTransferActivity::class.java)
            intent.putExtra(EXTRA_ACCOUNT, accountUuid)
            return intent
        }
    }
}

interface AutocryptKeyTransferView {
    fun setAddress(address: String)
    fun sceneBegin()
    fun sceneGeneratingAndSending()
    fun sceneSendError()
    fun sceneFinished(transition: Boolean = true)
    fun setLoadingStateGenerating()
    fun setLoadingStateSending()
    fun setLoadingStateSendingFailed()
    fun setLoadingStateFinished()
    fun finishWithInvalidAccountError()
    fun finishWithProviderConnectError(providerName: String)
    fun launchUserInteractionPendingIntent(pendingIntent: PendingIntent)
}