const API_URL = 'http://localhost:8080';

// Load stats and settings
document.addEventListener('DOMContentLoaded', async () => {
    await loadStats();
    await loadSettings();
    await checkAuth();
    await loadCurrentSite();
    setupEventListeners();
});

async function loadStats() {
    const stats = await chrome.storage.local.get(['adsBlocked', 'trackersBlocked']);
    document.getElementById('adsBlocked').textContent = stats.adsBlocked || 0;
    document.getElementById('trackersBlocked').textContent = stats.trackersBlocked || 0;
}

async function loadSettings() {
    const settings = await chrome.storage.sync.get(['blockAds', 'blockTrackers', 'blockPopups']);

    document.getElementById('blockAds').checked = settings.blockAds !== false;
    document.getElementById('blockTrackers').checked = settings.blockTrackers !== false;
    document.getElementById('blockPopups').checked = settings.blockPopups !== false;
}

async function checkAuth() {
    // Try to get auth from chrome.storage first
    let auth = await chrome.storage.local.get(['userEmail', 'userName', 'authToken']);

    // If not found, try to get from localhost page's localStorage
    if (!auth.userEmail) {
        try {
            const [tab] = await chrome.tabs.query({ url: 'http://localhost:*/*' });
            if (tab) {
                const result = await chrome.scripting.executeScript({
                    target: { tabId: tab.id },
                    func: () => {
                        return {
                            userEmail: localStorage.getItem('userEmail'),
                            userName: localStorage.getItem('userName'),
                            authToken: localStorage.getItem('authToken'),
                            isAuthenticated: localStorage.getItem('isAuthenticated')
                        };
                    }
                });

                if (result[0]?.result?.isAuthenticated === 'true') {
                    // Save to chrome.storage for future use
                    await chrome.storage.local.set({
                        userEmail: result[0].result.userEmail,
                        userName: result[0].result.userName,
                        authToken: result[0].result.authToken
                    });
                    auth = result[0].result;
                }
            }
        } catch (e) {
            console.log('Could not read localStorage from page:', e);
        }
    }

    if (auth.userEmail && auth.authToken) {
        document.getElementById('loggedInView').classList.remove('hidden');
        document.getElementById('loggedOutView').classList.add('hidden');
        const displayName = auth.userName || auth.userEmail;
        document.getElementById('userEmail').textContent = displayName;
    } else {
        document.getElementById('loggedInView').classList.add('hidden');
        document.getElementById('loggedOutView').classList.remove('hidden');
    }
}

async function loadCurrentSite() {
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
    if (tab && tab.url) {
        try {
            const url = new URL(tab.url);
            const domain = url.hostname;
            document.getElementById('currentSite').textContent = `Current site: ${domain}`;

            const disabled = await chrome.storage.local.get(['disabledSites']);
            const disabledSites = disabled.disabledSites || [];

            const btn = document.getElementById('toggleSiteBtn');
            if (disabledSites.includes(domain)) {
                btn.textContent = 'Enable on this site';
            } else {
                btn.textContent = 'Disable on this site';
            }
        } catch (e) {
            document.getElementById('currentSite').textContent = 'Invalid URL';
        }
    }
}

function setupEventListeners() {
    // Toggle settings
    document.getElementById('blockAds').addEventListener('change', async (e) => {
        await chrome.storage.sync.set({ blockAds: e.target.checked });
        showStatus('Settings updated');
    });

    document.getElementById('blockTrackers').addEventListener('change', async (e) => {
        await chrome.storage.sync.set({ blockTrackers: e.target.checked });
        showStatus('Settings updated');
    });

    document.getElementById('blockPopups').addEventListener('change', async (e) => {
        await chrome.storage.sync.set({ blockPopups: e.target.checked });
        showStatus('Settings updated');
    });

    // Toggle site blocking
    document.getElementById('toggleSiteBtn').addEventListener('click', async () => {
        const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
        if (tab && tab.url) {
            try {
                const url = new URL(tab.url);
                const domain = url.hostname;

                const disabled = await chrome.storage.local.get(['disabledSites']);
                let disabledSites = disabled.disabledSites || [];

                if (disabledSites.includes(domain)) {
                    disabledSites = disabledSites.filter(d => d !== domain);
                    showStatus(`Enabled on ${domain}`);
                } else {
                    disabledSites.push(domain);
                    showStatus(`Disabled on ${domain}`);
                }

                await chrome.storage.local.set({ disabledSites });
                await loadCurrentSite();

                // Reload the page
                chrome.tabs.reload(tab.id);
            } catch (e) {
                showStatus('Error: Invalid URL');
            }
        }
    });

    // Auth buttons
    document.getElementById('loginBtn').addEventListener('click', () => {
        chrome.tabs.create({ url: `${API_URL}/` });
    });

    document.getElementById('logoutBtn').addEventListener('click', async () => {
        await chrome.storage.local.remove(['userEmail', 'userName', 'authToken']);
        await checkAuth();
        showStatus('Logged out successfully');
    });

    document.getElementById('syncBtn').addEventListener('click', async () => {
        await syncSettings();
    });
}

async function syncSettings() {
    const auth = await chrome.storage.local.get(['userEmail', 'authToken']);

    if (!auth.userEmail || !auth.authToken) {
        showStatus('Please login first');
        return;
    }

    const stats = await chrome.storage.local.get(['adsBlocked', 'trackersBlocked']);
    const settings = await chrome.storage.sync.get(['blockAds', 'blockTrackers', 'blockPopups']);

    try {
        // Send stats to server (you'll need to create this endpoint)
        const response = await fetch(`${API_URL}/api/stats/sync`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${auth.authToken}`
            },
            body: JSON.stringify({
                email: auth.userEmail,
                adsBlocked: stats.adsBlocked || 0,
                trackersBlocked: stats.trackersBlocked || 0,
                settings: settings
            })
        });

        if (response.ok) {
            showStatus('Synced successfully!');
        } else {
            showStatus('Sync failed');
        }
    } catch (error) {
        console.error('Sync error:', error);
        showStatus('Sync error - check connection');
    }
}

function showStatus(message) {
    const statusEl = document.getElementById('statusMsg');
    statusEl.textContent = `> ${message}`;
    setTimeout(() => {
        statusEl.textContent = '';
    }, 3000);
}

// Listen for stats updates
chrome.runtime.onMessage.addListener((message) => {
    if (message.type === 'statsUpdate') {
        document.getElementById('adsBlocked').textContent = message.adsBlocked;
        document.getElementById('trackersBlocked').textContent = message.trackersBlocked;
    }
});