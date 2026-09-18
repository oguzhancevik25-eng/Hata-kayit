const fs = require('fs');
const vm = require('vm');

for (const file of ['v15-extra.js','v16-sync.js']) {
  const source = fs.readFileSync(file, 'utf8');
  new vm.Script(source, { filename: file });
}

const html = fs.readFileSync('index.html', 'utf8');
const scripts = [...html.matchAll(/<script(?![^>]*\bsrc=)[^>]*>([\s\S]*?)<\/script>/gi)];
if (!scripts.length) throw new Error('index.html inline script bulunamadı');
scripts.forEach((m, i) => new vm.Script(m[1], { filename: 'index-inline-' + (i + 1) + '.js' }));
console.log('JavaScript syntax OK');
