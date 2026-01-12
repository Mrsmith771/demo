// SAFE & PRECISE AD SELECTORS
const AD_SELECTORS = [
    'iframe[src*="doubleclick.net"]',
    'iframe[src*="googlesyndication"]',
    'iframe[src*="adservice"]',
    '[class~="banner"]',
    '[class~="ad-banner"]',
    '[class~="adsbygoogle"]',
    '[class*="sponsor"]:not(article)',
    '[class*="promo"]:not(.promoted-content)'
];

const processed = new WeakSet();

// REMOVE ELEMENT
function remove(el) {
    if (processed.has(el)) return;
    if (el === document.body || el === document.documentElement) return;

    processed.add(el);
    el.remove();

    chrome.runtime.sendMessage({ type: 'adBlocked' });
}

// INITIAL CLEAN
function cleanInitial() {
    for (const sel of AD_SELECTORS) {
        document.querySelectorAll(sel).forEach(remove);
    }
}

// MUTATION OBSERVER
const observer = new MutationObserver(mutations => {
    for (const { addedNodes } of mutations) {
        for (const node of addedNodes) {
            if (node.nodeType !== 1) continue;

            for (const sel of AD_SELECTORS) {
                if (node.matches?.(sel)) {
                    remove(node);
                    break;
                }

                node.querySelectorAll?.(sel).forEach(remove);
            }
        }
    }
});

// POPUP BLOCK (SOFT)
function blockPopups() {
    const originalOpen = window.open;

    window.open = function (url, ...args) {
        if (url && /popup|popunder|ads?|click/i.test(url)) {
            return null;
        }
        return originalOpen.call(this, url, ...args);
    };

    document.addEventListener(
        'click',
        e => {
            const a = e.target.closest('a');
            if (!a || !a.href) return;

            if (/redirect|adclick|pop/i.test(a.href)) {
                e.preventDefault();
                e.stopPropagation();
            }
        },
        true
    );
}

// INIT
function init() {
    cleanInitial();
    observer.observe(document.documentElement, {
        childList: true,
        subtree: true
    });
    blockPopups();
}

init();
