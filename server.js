const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3000;
const PUBLIC_DIR = path.join(__dirname, 'public');
const BACKUP_FILE = path.join(__dirname, 'eco_tobacco_cloud_backup.json');

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.apk': 'application/vnd.android.package-archive'
};

const server = http.createServer((req, res) => {
  const urlPath = req.url.split('?')[0];

  // CORS headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  // API: Status
  if (urlPath === '/api/status') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'online', app: 'ECO TOBACCO Web Dashboard', port: PORT }));
    return;
  }

  // API: Get latest backup JSON
  if (urlPath === '/api/backup' && req.method === 'GET') {
    if (fs.existsSync(BACKUP_FILE)) {
      const data = fs.readFileSync(BACKUP_FILE, 'utf-8');
      res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(data);
    } else {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'No backup file found', hasData: false }));
    }
    return;
  }

  // API: Get backup metadata info
  if (urlPath === '/api/backup-info' && req.method === 'GET') {
    if (fs.existsSync(BACKUP_FILE)) {
      try {
        const data = fs.readFileSync(BACKUP_FILE, 'utf-8');
        const parsed = JSON.parse(data);
        const stat = fs.statSync(BACKUP_FILE);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          hasData: true,
          timestamp: parsed.lastUpdated || stat.mtimeMs,
          totalProducts: parsed.products ? parsed.products.length : 0,
          totalTransactions: parsed.transactions ? parsed.transactions.length : 0,
          totalPlans: parsed.procurementPlans ? parsed.procurementPlans.length : 0,
          sizeBytes: stat.size
        }));
        return;
      } catch (e) {
        // Fallthrough
      }
    }
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ hasData: false, message: 'Belum ada data cadangan di server' }));
    return;
  }

  // API: Save backup JSON
  if (urlPath === '/api/save-backup' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        if (!body || body.trim().length === 0) {
          throw new Error('Payload cadangan kosong');
        }
        // Verify JSON parse
        const parsed = JSON.parse(body);
        fs.writeFileSync(BACKUP_FILE, body, 'utf-8');
        const publicBackup = path.join(PUBLIC_DIR, 'eco_tobacco_cloud_backup.json');
        fs.writeFileSync(publicBackup, body, 'utf-8');

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          success: true,
          message: 'Data berhasil disinkronkan ke Server Cloud',
          totalProducts: parsed.products ? parsed.products.length : 0,
          totalTransactions: parsed.transactions ? parsed.transactions.length : 0,
          timestamp: Date.now()
        }));
      } catch (err) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: false, error: err.message }));
      }
    });
    return;
  }

  // Download APK
  if (urlPath === '/download/apk' || urlPath === '/download-apk') {
    const apkCandidates = [
      path.join(__dirname, 'eco-tobacco-signed.apk'),
      path.join(__dirname, 'eco-tobacco-release.apk'),
      path.join(__dirname, 'eco-tobacco-debug.apk'),
      path.join(__dirname, 'app-debug.apk'),
      path.join(__dirname, 'app/build/outputs/apk/debug/app-debug.apk')
    ];
    let foundApk = apkCandidates.find(p => fs.existsSync(p));
    if (foundApk) {
      const stat = fs.statSync(foundApk);
      res.writeHead(200, {
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Length': stat.size,
        'Content-Disposition': 'attachment; filename="eco-tobacco.apk"'
      });
      const stream = fs.createReadStream(foundApk);
      stream.pipe(res);
      return;
    } else {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('APK file not built yet.');
      return;
    }
  }

  // Serve static files from /public
  let filePath = path.join(PUBLIC_DIR, urlPath === '/' ? 'index.html' : urlPath);

  // Security check to prevent directory traversal
  if (!filePath.startsWith(PUBLIC_DIR)) {
    res.writeHead(403);
    res.end('Forbidden');
    return;
  }

  if (fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
    const ext = path.extname(filePath).toLowerCase();
    const contentType = MIME_TYPES[ext] || 'application/octet-stream';
    res.writeHead(200, { 'Content-Type': contentType });
    fs.createReadStream(filePath).pipe(res);
  } else {
    // Fallback to index.html for SPA routing
    const indexPath = path.join(PUBLIC_DIR, 'index.html');
    if (fs.existsSync(indexPath)) {
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      fs.createReadStream(indexPath).pipe(res);
    } else {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('Not Found');
    }
  }
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`ECO TOBACCO Web Dashboard server running on http://0.0.0.0:${PORT}`);
});
