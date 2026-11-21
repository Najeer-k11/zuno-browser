package com.zuno.browser.browser

import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.PromptDelegate.AlertPrompt
import org.mozilla.geckoview.GeckoSession.PromptDelegate.BasePrompt
import org.mozilla.geckoview.GeckoSession.PromptDelegate.BeforeUnloadPrompt
import org.mozilla.geckoview.GeckoSession.PromptDelegate.ButtonPrompt
import org.mozilla.geckoview.GeckoSession.PromptDelegate.ChoicePrompt
import org.mozilla.geckoview.GeckoSession.PromptDelegate.FilePrompt
import org.mozilla.geckoview.GeckoSession.PromptDelegate.PopupPrompt
import org.mozilla.geckoview.GeckoSession.PromptDelegate.PromptResponse
import org.mozilla.geckoview.GeckoSession.PromptDelegate.RepostConfirmPrompt
import org.mozilla.geckoview.GeckoSession.PromptDelegate.TextPrompt

/** A web-content prompt waiting for the Compose layer to answer it. */
class PromptRequest(val prompt: BasePrompt, private val result: GeckoResult<PromptResponse>) {
    fun complete(response: PromptResponse) { if (!prompt.isComplete) result.complete(response) }
    fun dismiss() { if (!prompt.isComplete) result.complete(prompt.dismiss()) }
}

/** Routes Gecko prompts into [BrowserStore.prompt]; unsupported kinds fall back to Gecko defaults. */
class Prompts(private val store: BrowserStore) : GeckoSession.PromptDelegate {
    private fun show(prompt: BasePrompt): GeckoResult<PromptResponse> {
        val result = GeckoResult<PromptResponse>()
        store.prompt?.dismiss()
        store.prompt = PromptRequest(prompt, result)
        return result
    }

    override fun onAlertPrompt(session: GeckoSession, prompt: AlertPrompt) = show(prompt)
    override fun onButtonPrompt(session: GeckoSession, prompt: ButtonPrompt) = show(prompt)
    override fun onTextPrompt(session: GeckoSession, prompt: TextPrompt) = show(prompt)
    override fun onChoicePrompt(session: GeckoSession, prompt: ChoicePrompt) = show(prompt)
    override fun onFilePrompt(session: GeckoSession, prompt: FilePrompt) = show(prompt)
    override fun onRepostConfirmPrompt(session: GeckoSession, prompt: RepostConfirmPrompt) = show(prompt)

    override fun onBeforeUnloadPrompt(session: GeckoSession, prompt: BeforeUnloadPrompt): GeckoResult<PromptResponse> =
        GeckoResult.fromValue(prompt.confirm(AllowOrDeny.ALLOW))

    /** Called only for popups without a user gesture; those stay blocked. */
    override fun onPopupPrompt(session: GeckoSession, prompt: PopupPrompt): GeckoResult<PromptResponse> =
        GeckoResult.fromValue(prompt.confirm(AllowOrDeny.DENY))
}
