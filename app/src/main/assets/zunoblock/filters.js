/* Zuno Shield - bundled filter list.
 * Hostnames of ad networks, ad servers and trackers. A request is blocked when its host or any
 * parent domain appears here (third-party requests only). Gecko's own Enhanced Tracking
 * Protection (Disconnect lists) runs underneath this as a second layer. Keep it conservative:
 * nothing here should be needed for a site to function. */
"use strict";

const ZUNO_BLOCKED_HOSTS = [
  // Google ads
  "doubleclick.net", "googlesyndication.com", "googleadservices.com", "googletagservices.com",
  "adservice.google.com", "pagead2.googlesyndication.com", "google-analytics.com",
  "ssl.google-analytics.com", "googletagmanager.com", "imasdk.googleapis.com", "2mdn.net",
  "admob.com", "adsense.com", "app-measurement.com",
  // Amazon / Microsoft / Meta / X ad + pixel endpoints
  "amazon-adsystem.com", "aax.amazon-adsystem.com", "assoc-amazon.com",
  "ads.microsoft.com", "bat.bing.com", "clarity.ms", "ads.linkedin.com", "px.ads.linkedin.com",
  "connect.facebook.net", "an.facebook.com", "pixel.facebook.com",
  "ads-twitter.com", "static.ads-twitter.com", "ads-api.twitter.com", "analytics.twitter.com",
  "ads.pinterest.com", "ct.pinterest.com", "analytics.tiktok.com", "ads.tiktok.com",
  "business-api.tiktok.com", "sc-static.net", "tr.snapchat.com", "ads.reddit.com",
  "events.reddit.com", "alb.reddit.com", "ads.yahoo.com", "analytics.yahoo.com",
  "gemini.yahoo.com", "adtech.yahooinc.com",
  // Programmatic exchanges / SSPs / DSPs
  "adnxs.com", "adnxs-simple.com", "rubiconproject.com", "pubmatic.com", "openx.net",
  "casalemedia.com", "indexww.com", "criteo.com", "criteo.net", "contextweb.com",
  "advertising.com", "adform.net", "adformdsp.net", "smartadserver.com", "sascdn.com",
  "yieldmo.com", "sharethrough.com", "triplelift.com", "3lift.com", "teads.tv", "33across.com",
  "gumgum.com", "media.net", "adroll.com", "taboola.com", "taboolasyndication.com",
  "outbrain.com", "outbrainimg.com", "revcontent.com", "mgid.com", "zemanta.com",
  "spotxchange.com", "spotx.tv", "springserve.com", "lijit.com", "sovrn.com", "bidswitch.net",
  "adsrvr.org", "mathtag.com", "turn.com", "rlcdn.com", "demdex.net", "everesttech.net",
  "bluekai.com", "krxd.net", "exelator.com", "eyeota.net", "tapad.com", "agkn.com",
  "crwdcntrl.net", "adsymptotic.com", "serving-sys.com", "flashtalking.com", "innovid.com",
  "moatads.com", "moatpixel.com", "doubleverify.com", "adsafeprotected.com", "iasds01.com",
  "quantserve.com", "quantcount.com", "scorecardresearch.com", "imrworldwide.com",
  "yieldlab.net", "adition.com", "stroeerdigitalgroup.de", "adtelligent.com", "e-planning.net",
  "undertone.com", "kargo.com", "districtm.io", "emxdgt.com", "synacor.com", "onetag-sys.com",
  "rhythmone.com", "1rx.io", "unrulymedia.com", "inmobi.com", "applovin.com", "applvn.com",
  "unityads.unity3d.com", "vungle.com", "chartboost.com", "ironsrc.com", "supersonicads.com",
  "adcolony.com", "mopub.com", "startapp.com", "tremorhub.com", "yahoo-ads.com",
  "zedo.com", "propellerads.com", "popads.net", "popcash.net", "exoclick.com", "juicyads.com",
  "trafficjunky.net", "adsterra.com", "hilltopads.net", "clickadu.com", "admaven.com",
  "a-ads.com", "bidvertiser.com", "infolinks.com", "chitika.com", "vibrantmedia.com",
  "buysellads.com", "carbonads.com", "carbonads.net", "adzerk.net", "kevel.co",
  "adhese.com", "adpushup.com", "ezoic.net", "ezodn.com", "ezojs.com", "mediavine.com",
  "adthrive.com", "raptive.com", "freestar.com", "pubnation.com", "venatusmedia.com",
  "primis.tech", "connatix.com", "playwire.com", "nativo.com", "ntv.io", "dianomi.com",
  "plista.com", "ligatus.com", "adblade.com", "content.ad", "lockerdome.com",
  // Analytics / session replay / fingerprinting
  "hotjar.com", "hotjar.io", "mouseflow.com", "fullstory.com", "crazyegg.com", "luckyorange.com",
  "inspectlet.com", "smartlook.com", "logrocket.io", "mixpanel.com", "segment.io",
  "segment.com", "amplitude.com", "heap.io", "heapanalytics.com", "kissmetrics.com",
  "chartbeat.com", "chartbeat.net", "parsely.com", "newrelic.com", "nr-data.net",
  "omtrdc.net", "2o7.net", "hs-analytics.net", "hubspot.net", "branch.io", "app.link",
  "adjust.com", "appsflyer.com", "kochava.com", "singular.net", "comscore.com",
  "fingerprintjs.com", "fpjs.io", "iovation.com", "threatmetrix.com", "statcounter.com",
  "clicky.com", "getclicky.com", "woopra.com", "optimizely.com", "vwo.com",
  "visualwebsiteoptimizer.com", "mc.yandex.ru", "top-fwz1.mail.ru",
  "counter.yadro.ru", "tns-counter.ru", "addthis.com", "sharethis.com", "addtoany.com",
  "onesignal.com", "pushwoosh.com", "pushcrew.com", "subscribers.com", "sumo.com",
  "omnisend.com", "klaviyo.com", "bounceexchange.com", "bouncex.net", "wunderkind.co",
  "liadm.com", "liveintent.com", "id5-sync.com", "33across.com", "permutive.com",
  "permutive.app", "zeotap.com", "lotame.com", "intentiq.com", "pippio.com",
  // Malvertising / cryptominers
  "coinhive.com", "coin-hive.com", "jsecoin.com", "cryptoloot.pro", "webminepool.com",
  "minero.cc", "authedmine.com", "crypto-loot.com"
];

/* Path fragments that only ever appear in ad-serving URLs. */
const ZUNO_BLOCKED_PATHS = [
  "/pagead/", "/adsbygoogle", "/gampad/ads", "/securepubads.", "/prebid.js", "/prebid-",
  "/pubads_impl", "/ads.js?", "/adframe.", "/adserver/", "/doubleclick/"
];
