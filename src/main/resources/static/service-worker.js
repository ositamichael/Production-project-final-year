const SHELL_CACHE = 'unicircle-shell-v7';
const SHELL_FILES = [
  '/offline.html',
  '/favicon.svg',
  '/css/app.css?v=37',
  '/css/legal.css',
  '/js/app.js?v=37',
  '/manifest.webmanifest'
];

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(SHELL_CACHE).then((cache) => cache.addAll(SHELL_FILES)));
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((names) => Promise.all(names.filter((name) => name !== SHELL_CACHE).map((name) => caches.delete(name))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const request = event.request;
  if (request.method !== 'GET') return;
  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;
  if (url.pathname.startsWith('/api/') || url.pathname.startsWith('/actuator/') ||
      url.pathname.startsWith('/login') || url.pathname.startsWith('/logout') ||
      url.pathname.startsWith('/sign-in') || url.pathname.startsWith('/ws/')) return;

  if (request.mode === 'navigate') {
    event.respondWith(fetch(request).catch(() => caches.match('/offline.html')));
    return;
  }

  if (url.pathname.startsWith('/css/') || url.pathname.startsWith('/js/') ||
      url.pathname.startsWith('/images/') || url.pathname === '/favicon.svg' ||
      url.pathname === '/manifest.webmanifest') {
    event.respondWith(caches.match(request).then((cached) => cached || fetch(request)));
  }
});
