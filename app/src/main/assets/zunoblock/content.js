/* Zuno Shield - cosmetic filtering + relays the tab's blocked count to the app. */
"use strict";

const SELECTORS = [
  "ins.adsbygoogle", "[id^='google_ads_']", "[id^='div-gpt-ad']", "[class*='adsbygoogle']",
  "iframe[src*='doubleclick.net']", "iframe[src*='googlesyndication']", "iframe[id^='google_ads']",
  "[data-ad-slot]", "[data-ad-client]", "[data-google-query-id]", "[data-ad]", "[data-adunit]",
  ".ad-banner", ".ad-container", ".ad-slot", ".ad-wrapper", ".adsbox", ".adbox", ".ad-unit",
  ".advertisement", ".advert", ".sponsored-ad", ".banner-ad", ".top-ad", ".sidebar-ad",
  "#ad-banner", "#ad-container", "#ad_wrapper", "#adbox", "#advertisement", "#top-ad",
  ".taboola", "[id^='taboola-']", ".trc_related_container", ".OUTBRAIN", "[data-widget-id^='AR_']",
  "amp-ad", "amp-embed[type='taboola']", "amp-sticky-ad", ".mgid-widget", "[id^='mgid_']",
  ".revcontent", "[id^='rc-widget']", ".sticky-ad", ".adhesion-ad", ".ad-sticky"
];

let lastCount = -1;
function report(count) {
  if (count === lastCount) return;
  lastCount = count;
  browser.runtime.sendNativeMessage("zunoshield", { type: "blocked", count, url: location.href })
    .catch(() => {});
}

function injectCss() {
  const style = document.createElement("style");
  style.id = "zuno-shield";
  style.textContent = SELECTORS.join(",\n") + " { display: none !important; }";
  (document.head || document.documentElement).appendChild(style);
}

browser.runtime.sendMessage({ type: "hello", url: location.href }).then((state) => {
  if (!state) return;
  if (state.cosmetic) {
    if (document.documentElement) injectCss();
    else document.addEventListener("DOMContentLoaded", injectCss, { once: true });
  }
  report(state.count);
}).catch(() => {});

browser.runtime.onMessage.addListener((msg) => {
  if (msg && msg.type === "count") report(msg.count);
});
