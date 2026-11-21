/* Zuno background play: sites like YouTube / YouTube Music pause when the page reports it is
 * hidden. Keep document.visibilityState === "visible" so audio continues while Zuno is in the
 * background. Runs at document_start in every frame; disabled via the app's "Background play". */
"use strict";
(function () {
  const doc = document.wrappedJSObject;
  const visible = exportFunction(function () { return "visible"; }, window);
  const notHidden = exportFunction(function () { return false; }, window);
  try {
    Object.defineProperty(doc, "visibilityState", { get: visible, configurable: true });
    Object.defineProperty(doc, "hidden", { get: notHidden, configurable: true });
    Object.defineProperty(doc, "webkitVisibilityState", { get: visible, configurable: true });
    Object.defineProperty(doc, "webkitHidden", { get: notHidden, configurable: true });
  } catch (e) { /* frame without a document yet */ }
  const swallow = (e) => e.stopImmediatePropagation();
  document.addEventListener("visibilitychange", swallow, true);
  document.addEventListener("webkitvisibilitychange", swallow, true);

  browser.runtime.sendMessage({ type: "bgplay?" }).then((on) => {
    if (on !== false) return;
    // Background play is off: restore the real behaviour.
    for (const k of ["visibilityState", "hidden", "webkitVisibilityState", "webkitHidden"]) {
      try { delete doc[k]; } catch (e) {}
    }
    document.removeEventListener("visibilitychange", swallow, true);
    document.removeEventListener("webkitvisibilitychange", swallow, true);
  }).catch(() => {});
})();
