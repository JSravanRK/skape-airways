/* ═══════════════════════════════════════════════
   Skape Airways — Global JS (with all enhancements)
   ═══════════════════════════════════════════════ */

const API = '/api';

/* ── Auth helpers ─────────────────────────────── */
const Auth = {
  save(data) {
    localStorage.setItem('token',  data.token);
    localStorage.setItem('name',   data.name);
    localStorage.setItem('email',  data.email);
    localStorage.setItem('role',   data.role);
    localStorage.setItem('userId', data.userId);
    // Seed navbar miles immediately from login response
    if (typeof data.milesBalance === 'number') {
      localStorage.setItem('skymiles_balance', data.milesBalance);
    }
  },
  token()    { return localStorage.getItem('token'); },
  name()     { return localStorage.getItem('name'); },
  role()     { return localStorage.getItem('role'); },
  userId()   { return localStorage.getItem('userId'); },
  isLoggedIn() { return !!this.token(); },
  isAdmin()    { return this.role() === 'ADMIN'; },
  logout() { localStorage.clear(); window.location.href = '/login.html'; }
};

/* ── SkyMiles helper (synced with backend; localStorage = cache) ──────── */
const SkyMiles = {
  STORAGE_KEY: 'skymiles_balance',
  get()    { return parseInt(localStorage.getItem(this.STORAGE_KEY) || '0'); },
  set(n)   { localStorage.setItem(this.STORAGE_KEY, Math.max(0, n)); },
  add(n)   { this.set(this.get() + n); },
  reset()  { localStorage.removeItem(this.STORAGE_KEY); },
  fromAmount(amount) { return Math.round(amount / 10); },
  // Refresh from backend and update cache — never redirects on failure
  async sync() {
    try {
      const data = await silentGet('/miles/balance');
      if (data && typeof data.balance === 'number') this.set(data.balance);
    } catch(_) { /* keep cached value */ }
  }
};

/* ── HTTP wrapper ─────────────────────────────── */
async function http(method, url, body = null) {
  const opts = { method, headers: { 'Content-Type': 'application/json' } };
  if (Auth.token()) opts.headers['Authorization'] = 'Bearer ' + Auth.token();
  if (body) opts.body = JSON.stringify(body);
  let res;
  try {
    res = await fetch(API + url, opts);
  } catch (networkErr) {
    throw new Error('Network error — please check your connection and try again.');
  }
  // Parse JSON response — gracefully handle empty or non-JSON bodies
  const text = await res.text().catch(() => '');
  let data = {};
  if (text) {
    try { data = JSON.parse(text); } catch (_) { data = { error: text }; }
  }
  if (!res.ok) {
    // FIX: 401 = session expired — clear auth and redirect to login
    if (res.status === 401) {
      const msg = data.error || 'Session expired. Please sign in again.';
      // Only redirect if not already on login page
      if (!location.pathname.includes('/login')) {
        Auth.logout(); // clears localStorage and redirects
        throw new Error(msg); // won't actually show but kept for safety
      }
      throw new Error(msg);
    }
    if (res.status === 403) throw new Error(data.error || 'Access denied.');
    if (res.status === 404) throw new Error(data.error || 'Resource not found.');
    if (res.status === 500) throw new Error(data.error || 'Server error. Please try again.');
    throw new Error(data.error || data.message || `Request failed (${res.status})`);
  }
  return data;
}

const get   = (url)       => http('GET',    url);
const post  = (url, body) => http('POST',   url, body);
const put   = (url, body) => http('PUT',    url, body);
const del   = (url)       => http('DELETE', url);
const patch = (url, body) => http('PATCH',  url, body);

/**
 * Silent fetch — for background/optional requests (vouchers, miles sync).
 * Returns null on ANY error instead of throwing or redirecting.
 * Never calls Auth.logout() so it cannot wipe the session.
 */
async function silentGet(url) {
  try {
    const opts = { method: 'GET', headers: { 'Content-Type': 'application/json' } };
    if (Auth.token()) opts.headers['Authorization'] = 'Bearer ' + Auth.token();
    const res  = await fetch(API + url, opts);
    if (!res.ok) return null;
    const text = await res.text().catch(() => '');
    if (!text) return null;
    try { return JSON.parse(text); } catch(_) { return null; }
  } catch(_) { return null; }
}

/* ── Format helpers ───────────────────────────── */
function formatTime(dt) {
  return new Date(dt).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' });
}
function formatDate(dt) {
  return new Date(dt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
}
function formatDateTime(dt)   { return formatDate(dt) + ' ' + formatTime(dt); }
function formatDuration(mins) {
  const h = Math.floor(mins / 60), m = mins % 60;
  return h + 'h ' + (m > 0 ? m + 'm' : '');
}
function formatMoney(n) {
  return '₹' + Number(n).toLocaleString('en-IN', { minimumFractionDigits: 0 });
}

/* ── Status badge ─────────────────────────────── */
function statusBadge(status) {
  const map = {
    CONFIRMED: 'badge-success', PENDING: 'badge-warning',
    CANCELLED: 'badge-danger',  COMPLETED: 'badge-info',
    SCHEDULED: 'badge-info',    BOARDING: 'badge-success',
    DEPARTED: 'badge-info',     ARRIVED: 'badge-info',
    CANCELLED_FLIGHT: 'badge-danger', DELAYED: 'badge-warning',
    SUCCESS: 'badge-success',   FAILED: 'badge-danger', REFUNDED: 'badge-warning',
  };
  return `<span class="badge ${map[status] || 'badge-info'}">${status}</span>`;
}

/* ── Alert helper ─────────────────────────────── */
function showAlert(containerId, message, type = 'error') {
  const el = document.getElementById(containerId);
  if (!el) return;
  const icon = type === 'error' ? '⚠️' : type === 'success' ? '✅' : 'ℹ️';
  el.innerHTML = `<div class="alert alert-${type}">${icon} ${message}</div>`;
  setTimeout(() => { if (el) el.innerHTML = ''; }, 6000);
}

/* ── Loading state ────────────────────────────── */
function setLoading(btn, loading) {
  if (!btn) return;
  if (loading) {
    btn.dataset.origText = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner" style="width:18px;height:18px;border-width:2px;margin:0"></span>';
  } else {
    btn.disabled = false;
    btn.innerHTML = btn.dataset.origText || 'Submit';
  }
}

/* ── Seat count urgency helper ────────────────── */
function seatUrgencyBadge(availableSeats) {
  if (availableSeats <= 3)
    return `<span style="background:rgba(220,38,38,.12);color:#b91c1c;font-size:.7rem;font-weight:700;padding:.2rem .55rem;border-radius:10px;border:1px solid rgba(220,38,38,.2)">🔴 Only ${availableSeats} seat${availableSeats===1?'':'s'} left!</span>`;
  if (availableSeats <= 9)
    return `<span style="background:rgba(217,119,6,.1);color:#b45309;font-size:.7rem;font-weight:700;padding:.2rem .55rem;border-radius:10px;border:1px solid rgba(217,119,6,.2)">⚠️ ${availableSeats} seats left</span>`;
  return `<span style="font-size:.72rem;color:var(--muted)">${availableSeats} seats left</span>`;
}

/* ── Premium Navbar (with SkyMiles) ───────────── */
function renderNav(activePage = '') {
  const isLogged = Auth.isLoggedIn();
  const isAdmin  = Auth.isAdmin();
  const name     = Auth.name() || '';
  const initials = name.split(' ').map(w => w[0]).join('').toUpperCase().substring(0, 2);
  const miles    = SkyMiles.get();

  const links = isLogged
    ? `<a href="/index.html"           class="${activePage==='home'?'active':''}">Home</a>
       <a href="/pages/search.html"    class="${(activePage==='search'||activePage==='book')?'active':''}">Search</a>
       <a href="/pages/status.html"    class="${activePage==='status'?'active':''}">Flight Status</a>
       <a href="/pages/bookings.html"  class="${activePage==='bookings'?'active':''}">My Bookings</a>
       ${isAdmin ? '<a href="/pages/admin/dashboard.html" class="badge badge-gold" style="font-size:.73rem">Admin</a>' : ''}
       <span style="width:1px;height:16px;background:rgba(255,255,255,.1);display:inline-block"></span>
       <a href="/pages/miles.html" style="display:flex;align-items:center;gap:.35rem;color:var(--gold-l);font-size:.82rem;font-weight:600;text-decoration:none;padding:.35rem .7rem;background:rgba(201,168,76,.1);border-radius:6px;border:1px solid rgba(201,168,76,.2)">
         🌟 <span>${miles.toLocaleString('en-IN')} mi</span>
       </a>
       <div style="display:flex;align-items:center;gap:.45rem;color:rgba(255,255,255,.6);font-size:.83rem">
         <div style="width:28px;height:28px;border-radius:50%;background:linear-gradient(135deg,var(--gold-d),var(--gold-l));display:flex;align-items:center;justify-content:center;font-size:.68rem;font-weight:700;color:var(--deep)">${initials||'✦'}</div>
         ${name.split(' ')[0]}
       </div>
       <button class="btn btn-sm btn-ghost" style="border-color:rgba(255,255,255,.12);color:rgba(255,255,255,.5)" onclick="Auth.logout()">Sign Out</button>`
    : `<a href="/index.html"        class="${activePage==='home'?'active':''}">Home</a>
       <a href="/pages/search.html" class="${activePage==='search'?'active':''}">Search Flights</a>
       <a href="/pages/status.html" class="${activePage==='status'?'active':''}">Flight Status</a>
       <a href="/login.html"        class="${activePage==='login'?'active':''}">Sign In</a>
       <a href="/register.html" class="nav-btn">Join Free →</a>`;

  const nav = document.getElementById('main-nav');
  if (nav) nav.innerHTML = links;

  // Background sync — update navbar miles counter from backend after render
  if (isLogged) {
    SkyMiles.sync().then(() => {
      const milesEl = nav && nav.querySelector('a[href="/pages/miles.html"] span');
      if (milesEl) milesEl.textContent = SkyMiles.get().toLocaleString('en-IN') + ' mi';
    });
  }
}

/* ── Auth guards ──────────────────────────────── */
function requireAuth() {
  if (!Auth.isLoggedIn())
    window.location.href = '/login.html?redirect=' + encodeURIComponent(location.pathname + location.search);
}
function requireAdmin() { requireAuth(); if (!Auth.isAdmin()) window.location.href = '/index.html'; }

/* ── Class label ──────────────────────────────── */
function classLabel(cls) {
  return { ECONOMY: 'Economy', BUSINESS: 'Business Class', FIRST_CLASS: 'First Class' }[cls] || cls;
}

/* ── Barcode SVG generator (for e-ticket) ─────── */
function generateBarcodeSVG(text) {
  // Simple visual barcode from text characters
  const seed = text.split('').reduce((a, c) => a + c.charCodeAt(0), 0);
  const bars = [];
  let x = 0;
  for (let i = 0; i < 60; i++) {
    const w = ((seed * (i + 7) * 13) % 3) + 1;
    const gap = (i % 5 === 0) ? 2 : 1;
    bars.push(`<rect x="${x}" y="0" width="${w}" height="50" fill="#1a1f2e"/>`);
    x += w + gap;
  }
  const totalWidth = x;
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${totalWidth}" height="60" viewBox="0 0 ${totalWidth} 60">
    ${bars.join('')}
    <text x="${totalWidth/2}" y="58" text-anchor="middle" font-family="monospace" font-size="8" fill="#7b8294" letter-spacing="2">${text}</text>
  </svg>`;
}
