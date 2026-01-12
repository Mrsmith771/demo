// Ad blocking rules - common ad domains
const AD_DOMAINS = [
    'doubleclick.net',
    'googlesyndication.com',
    'googleadservices.com',
    'adservice.google.com',
    'ads.google.com',
    'facebook.com/plugins',
    'connect.facebook.net',
    'taboola.com',
    'outbrain.com',
    'advertising.com',
    'adnxs.com',
    'adsystem.com',
    'amazon-adsystem.com',
    'pubmatic.com',
    'criteo.com',
    'scorecardresearch.com',
    'quantserve.com',
    'google-analytics.com',
    'googletagmanager.com',
    'hotjar.com',
    'mouseflow.com',
    'crazyegg.com'
];

// Tracker domains
const TRACKER_DOMAINS = [
    'google-analytics.com',
    'googletagmanager.com',
    'facebook.com/tr',
    'connect.facebook.net',
    'hotjar.com',
    'mouseflow.com',
    'crazyegg.com',
    'mixpanel.com',
    'segment.com',
    'heap.io'
];

let stats = {
    adsBlocked: 0,
    trackersBlocked: 0
};

// Load stats on startup
chrome.storage.local.get(['adsBlocked', 'trackersBlocked']).then((result) => {
    stats.adsBlocked = result.adsBlocked || 0;
    stats.trackersBlocked = result.trackersBlocked || 0;
});

// Reset stats daily
chrome.alarms.create('resetStats', { periodInMinutes: 1440 }); // 24 hours

chrome.alarms.onAlarm.addListener((alarm) => {
    if (alarm.name === 'resetStats') {
        stats.adsBlocked = 0;
        stats.trackersBlocked = 0;
        chrome.storage.local.set({ adsBlocked: 0, trackersBlocked: 0 });
    }
});

// Listen for web requests
chrome.webRequest.onBeforeRequest.addListener(
    async (details) => {
        const settings = await chrome.storage.sync.get(['blockAds', 'blockTrackers']);
        const url = new URL(details.url);
        const domain = url.hostname;

        // Check if blocking is disabled for this site
        const tabInfo = await chrome.tabs.get(details.tabId);
        if (tabInfo && tabInfo.url) {
            const tabUrl = new URL(tabInfo.url);
            const disabled = await chrome.storage.local.get(['disabledSites']);
            const disabledSites = disabled.disabledSites || [];

            if (disabledSites.includes(tabUrl.hostname)) {
                return { cancel: false };
            }
        }

        // Check if it's an ad domain
        if (settings.blockAds !== false) {
            for (const adDomain of AD_DOMAINS) {
                if (domain.includes(adDomain)) {
                    stats.adsBlocked++;
                    await updateStats();
                    console.log('Blocked ad:', details.url);
                    return { cancel: true };
                }
            }
        }

        // Check if it's a tracker
        if (settings.blockTrackers !== false) {
            for (const trackerDomain of TRACKER_DOMAINS) {
                if (domain.includes(trackerDomain)) {
                    stats.trackersBlocked++;
                    await updateStats();
                    console.log('Blocked tracker:', details.url);
                    return { cancel: true };
                }
            }
        }

        return { cancel: false };
    },
    { urls: ['<all_urls>'] },
    ['blocking']
);

async function updateStats() {
    await chrome.storage.local.set({
        adsBlocked: stats.adsBlocked,
        trackersBlocked: stats.trackersBlocked
    });

    // Notify popup if it's open
    try {
        chrome.runtime.sendMessage({
            type: 'statsUpdate',
            adsBlocked: stats.adsBlocked,
            trackersBlocked: stats.trackersBlocked
        });
    } catch (e) {
        // Popup not open, ignore
    }
}

// Handle messages from content script
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    if (message.type === 'adBlocked') {
        stats.adsBlocked++;
        updateStats();
    } else if (message.type === 'getStats') {
        sendResponse(stats);
    } else if (message.type === 'saveAuth') {
        // Save auth data from web page
        chrome.storage.local.set({
            userEmail: message.email,
            userName: message.username,
            authToken: message.token
        }).then(() => {
            console.log('Auth saved in extension:', message.email);
        });
    } else if (message.type === 'clearAuth') {
        // Clear auth data
        chrome.storage.local.remove(['userEmail', 'userName', 'authToken']).then(() => {
            console.log('Auth cleared from extension');
        });
    }
    return true;
});

// Installation handler
chrome.runtime.onInstalled.addListener((details) => {
    if (details.reason === 'install') {
        console.log('AdBlocker installed!');
        chrome.storage.sync.set({
            blockAds: true,
            blockTrackers: true,
            blockPopups: true
        });
    }
});