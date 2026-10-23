import { readFileSync, writeFileSync, readdirSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createHash } from 'node:crypto';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const dist=path.join(root,'dist/client');
const index=readFileSync(path.join(dist,'index.html'),'utf8');
const jsPath=index.match(/<script[^>]+src="([^"]+)"/)?.[1];
const cssPath=index.match(/<link[^>]+href="([^"]+\.css)"/)?.[1];
if(!jsPath||!cssPath)throw new Error('Build manquant : exécuter npm run build avant cet export.');
let js=readFileSync(path.join(dist,jsPath),'utf8');
let css=readFileSync(path.join(dist,cssPath),'utf8');
const mime={'.svg':'image/svg+xml','.png':'image/png','.webp':'image/webp','.jpg':'image/jpeg','.woff':'font/woff','.woff2':'font/woff2'};
for(const rel of readdirSync(path.join(dist,'assets'),{recursive:true}).sort((a,b)=>b.length-a.length)){
 const ext=path.extname(rel);if(!mime[ext])continue;
 const data='data:'+mime[ext]+';base64,'+readFileSync(path.join(dist,'assets',rel)).toString('base64');
 const url='/assets/'+rel.split(path.sep).join('/');js=js.split(url).join(data);css=css.split(url).join(data);
}
const html='<!doctype html><html lang="fr"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>Tinda SMS · Prototype</title><style>'+css+'</style></head><body><div id="root"></div><script type="module">'+js.replaceAll('</script','<\\/script').replace(/\n[ \t]+(?=\n)/g,'\n')+'</script></body></html>';
if(html.includes('/assets/'))throw new Error('Des assets restent externes : export interrompu.');
writeFileSync(path.join(root,'../preview/Tinda-SMS.html'),html);
const manifestPath=path.join(root,'../manifest.json');const manifest=JSON.parse(readFileSync(manifestPath,'utf8'));
manifest.preview_sha256=createHash('sha256').update(html).digest('hex');writeFileSync(manifestPath,JSON.stringify(manifest,null,2)+'\n');
console.log('Maquette autonome et empreinte mises à jour.');
