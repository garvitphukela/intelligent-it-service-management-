// Builds docs/report.pdf from docs/report.md.
// Usage: node docs/build-pdf.mjs   (needs `marked` and `playwright` resolvable, plus a Chromium; set CHROME_PATH if not bundled)
import { readFileSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { createRequire } from 'node:module'
const require = createRequire(process.env.NODE_DEPS ? join(process.env.NODE_DEPS, 'x.js') : import.meta.url)
const { marked } = require('marked')
const { chromium } = require('playwright')

const here = dirname(fileURLToPath(import.meta.url))
const md = readFileSync(join(here, 'report.md'), 'utf8').replace(/^---[\s\S]*?---\n/, '')
const body = marked.parse(md, { gfm: true })
const html = `<!doctype html><html><head><meta charset="utf-8"><title>I-ITSM Project Report</title>
<base href="${pathToFileURL(here + '/').href}">
<style>
  @page { size: A4; margin: 18mm 16mm 18mm 16mm; }
  html { font-family: "Helvetica Neue", Helvetica, Arial, sans-serif; font-size: 10.2pt; color: #111; line-height: 1.5; }
  body { margin: 0; }
  h1, h2, h3 { letter-spacing: -0.01em; line-height: 1.25; }
  h2 { font-size: 17pt; margin: 0 0 10px; padding-top: 4px; border-bottom: 1px solid #e5e5e5; padding-bottom: 6px; break-before: page; }
  h3 { font-size: 12pt; margin: 18px 0 6px; break-after: avoid; }
  p, li { orphans: 3; widows: 3; }
  code { font-family: "SFMono-Regular", Menlo, Consolas, monospace; font-size: 8.8pt; background: #f4f4f5; padding: 1px 4px; border-radius: 3px; }
  pre { background: #fafafa; border: 1px solid #e5e5e5; border-radius: 6px; padding: 10px 12px; overflow: hidden; white-space: pre-wrap; font-size: 8.4pt; break-inside: avoid; }
  pre code { background: none; padding: 0; font-size: inherit; }
  table { border-collapse: collapse; width: 100%; margin: 8px 0 14px; font-size: 8.8pt; break-inside: auto; }
  tr { break-inside: avoid; }
  th { text-align: left; background: #fafafa; font-weight: 600; }
  th, td { border: 1px solid #e5e5e5; padding: 5px 7px; vertical-align: top; }
  img { max-width: 100%; display: block; margin: 8px auto 16px; border: 1px solid #eee; border-radius: 6px; break-inside: avoid; }
  img[src*="diagrams/"] { border: none; max-height: 235mm; object-fit: contain; }
  img[src*="screenshots/"] { max-height: 200mm; object-fit: contain; object-position: top; }
  hr { display: none; }
  .cover { height: 250mm; display: flex; flex-direction: column; justify-content: center; }
  .cover h1 { font-size: 46pt; margin: 0; letter-spacing: -0.03em; }
  .cover h2 { font-size: 18pt; border: none; break-before: avoid; font-weight: 500; color: #333; margin: 6px 0 30px; }
  .cover table { width: auto; min-width: 60%; font-size: 10pt; margin-top: 30px; }
  .cover table th, .cover table td { border: none; border-bottom: 1px solid #eee; padding: 6px 12px 6px 0; background: none; }
  .toc { break-before: page; font-size: 11pt; }
  .toc ol { line-height: 1.9; }
  a { color: #0060df; text-decoration: none; }
</style></head><body>${body}</body></html>`
writeFileSync(join(here, 'report.html'), html)
const browser = await chromium.launch({ executablePath: process.env.CHROME_PATH || undefined })
const page = await browser.newPage()
await page.goto(pathToFileURL(join(here, 'report.html')).href, { waitUntil: 'networkidle' })
await page.pdf({ path: join(here, 'report.pdf'), format: 'A4', printBackground: true, displayHeaderFooter: true,
  headerTemplate: '<div></div>',
  footerTemplate: '<div style="width:100%;font-size:7.5pt;color:#888;padding:0 16mm;display:flex;justify-content:space-between;font-family:Helvetica"><span>I-ITSM · CS F213 OOP Project Report</span><span><span class="pageNumber"></span> / <span class="totalPages"></span></span></div>',
  margin: { top: '16mm', bottom: '18mm', left: '16mm', right: '16mm' } })
await browser.close()
console.log('wrote', join(here, 'report.pdf'))
