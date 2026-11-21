/* Zuno Shield - network-level blocking.
 * Config (enabled flag + per-site allowlist) arrives from the app over a native port.
 * Every block is counted per tab and pushed to that tab's content script, which relays it to
 * the app through the session-scoped native messaging channel. */
"use strict";

let enabled = true;
let bgPlay = true;
let allow = new Set();
const counts = new Map(); // tabId -> blocked count for the current page

const blockedHosts = new Set(ZUNO_BLOCKED_HOSTS);

function hostOf(url) {
  try { return new URL(url).hostname.toLowerCase(); } catch (e) { return ""; }
}

/** True if `host` or any of its parent domains is in `set`. */
function matches(set, host) {
  if (!host) return false;
  let h = host;
  while (true) {
    if (set.has(h)) return true;
    const dot = h.indexOf(".");
    if (dot < 0) return false;
    h = h.slice(dot + 1);
  }
}

function topHost(details) {
  if (details.frameAncestors && details.frameAncestors.length) {
    return hostOf(details.frameAncestors[details.frameAncestors.length - 1].url);
  }
  return hostOf(details.documentUrl || details.originUrl || "");
}

function strip(host) { return host.startsWith("www.") ? host.slice(4) : host; }

function isAllowed(host) { return matches(allow, strip(host)); }

browser.webRequest.onBeforeRequest.addListener(
  (details) => {
    if (!enabled || details.type === "main_frame") return {};
    const top = topHost(details);
    if (top && isAllowed(top)) return {};
    const host = hostOf(details.url);
    // Never block first-party requests.
    if (top && (host === top || host.endsWith("." + strip(top)))) return {};
    let blocked = matches(blockedHosts, host);
    if (!blocked) {
      const u = details.url;
      for (const p of ZUNO_BLOCKED_PATHS) { if (u.indexOf(p) !== -1) { blocked = true; break; } }
    }
    if (!blocked) return {};
    if (details.tabId >= 0) {
      const n = (counts.get(details.tabId) || 0) + 1;
      counts.set(details.tabId, n);
      browser.tabs.sendMessage(details.tabId, { type: "count", count: n }).catch(() => {});
    }
    return { cancel: true };
  },
  { urls: ["<all_urls>"] },
  ["blocking"]
);

// Reset the counter when a tab starts a new top-level navigation.
browser.webRequest.onBeforeRequest.addListener(
  (details) => { if (details.tabId >= 0) counts.set(details.tabId, 0); },
  { urls: ["<all_urls>"], types: ["main_frame"] }
);

browser.tabs.onRemoved.addListener((tabId) => counts.delete(tabId));

// Content scripts ask for their state when they start.
browser.runtime.onMessage.addListener((msg, sender) => {
  if (msg && msg.type === "bgplay?") return Promise.resolve(bgPlay);
  if (msg && msg.type === "hello") {
    const tabId = sender.tab ? sender.tab.id : -1;
    const host = hostOf(msg.url || "");
    return Promise.resolve({
      cosmetic: enabled && !isAllowed(host),
      count: counts.get(tabId) || 0,
    });
  }
  return undefined;
});

// App -> extension configuration channel.
const port = browser.runtime.connectNative("zunoshield");
port.onMessage.addListener((msg) => {
  if (msg && msg.type === "config") {
    enabled = !!msg.enabled;
    bgPlay = msg.bgPlay !== false;
    allow = new Set((msg.allow || []).map((h) => strip(String(h).toLowerCase())));
  }
});
port.postMessage({ type: "ready" });
