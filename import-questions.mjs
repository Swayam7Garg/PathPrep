#!/usr/bin/env node
// Bulk question importer for PathPrep (Node 18+, no dependencies).
//
// Put this file in <backend-repo>/scripts/ and your JSON batches in <backend-repo>/content/questions/**.json
//
// Usage (run from the backend repo root):
//   ADMIN_API_KEY=... node scripts/import-questions.mjs                      # dry run of every file (saves nothing)
//   ADMIN_API_KEY=... node scripts/import-questions.mjs content/questions/java-backend   # one folder or file
//   ADMIN_API_KEY=... node scripts/import-questions.mjs --apply              # really import as DRAFT
//   ADMIN_API_KEY=... node scripts/import-questions.mjs --apply --publish    # import AND publish what was created in this run
//
// Env:
//   API_BASE_URL   default http://localhost:8080
//   ADMIN_API_KEY  sent as X-Admin-Key
//   ADMIN_TOKEN    (after Phase 7) an ADMIN user's access token, sent as Authorization: Bearer
//
// Only publish AFTER a human has verified every answer key. This script never publishes unless --publish is given.

import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join, resolve, extname } from 'node:path';

const args = process.argv.slice(2);
const apply = args.includes('--apply');
const publish = args.includes('--publish');
const target = resolve(args.find((a) => !a.startsWith('--')) ?? 'content/questions');
const base = (process.env.API_BASE_URL ?? 'http://localhost:8080').replace(/\/$/, '');

if (publish && !apply) {
  console.error('--publish requires --apply (nothing is created in a dry run, so there is nothing to publish).');
  process.exit(2);
}

const headers = { 'Content-Type': 'application/json' };
if (process.env.ADMIN_TOKEN) headers['Authorization'] = `Bearer ${process.env.ADMIN_TOKEN}`;
else if (process.env.ADMIN_API_KEY) headers['X-Admin-Key'] = process.env.ADMIN_API_KEY;
else {
  console.error('Set ADMIN_API_KEY (or ADMIN_TOKEN after Phase 7).');
  process.exit(2);
}

function listJson(path) {
  const st = statSync(path);
  if (st.isFile()) return extname(path) === '.json' ? [path] : [];
  return readdirSync(path)
    .sort()
    .flatMap((name) => listJson(join(path, name)));
}

const errorsOf = (item) => item.errors ?? item.reasons ?? item.messages ?? [];

async function post(url, body) {
  const res = await fetch(url, { method: 'POST', headers, body: JSON.stringify(body) });
  const text = await res.text();
  let json = null;
  try { json = JSON.parse(text); } catch { /* keep raw text */ }
  return { status: res.status, json, text };
}

const files = listJson(target);
if (files.length === 0) {
  console.error(`No .json files found under ${target}`);
  process.exit(2);
}

console.log(`${apply ? 'APPLY' : 'DRY RUN'} against ${base}  (${files.length} file(s))\n`);

let anyProblem = false;
const totals = { ok: 0, duplicate: 0, invalid: 0 };
const createdIds = [];

for (const file of files) {
  const short = file.replace(process.cwd() + '/', '');
  let payload;
  try {
    payload = JSON.parse(readFileSync(file, 'utf8'));
  } catch (e) {
    console.log(`${short}\n  PARSE ERROR: ${e.message}\n`);
    anyProblem = true;
    continue;
  }

  const { status, json, text } = await post(`${base}/api/v1/admin/questions/import?dryRun=${!apply}`, payload);
  if (status !== 200 || !json) {
    console.log(`${short}\n  HTTP ${status}: ${(json && (json.code + ' ' + json.message)) || text.slice(0, 200)}\n`);
    anyProblem = true;
    continue;
  }

  const items = json.items ?? [];
  const ok = items.filter((i) => i.status === 'CREATED' || i.status === 'WOULD_CREATE');
  const dup = items.filter((i) => i.status === 'DUPLICATE');
  const bad = items.filter((i) => i.status === 'INVALID');
  totals.ok += ok.length; totals.duplicate += dup.length; totals.invalid += bad.length;
  if (bad.length) anyProblem = true;
  for (const i of ok) if (i.status === 'CREATED' && i.questionId != null) createdIds.push(i.questionId);

  console.log(`${short}\n  ${apply ? 'created' : 'would create'}: ${ok.length}   duplicates: ${dup.length}   invalid: ${bad.length}`);
  for (const i of bad) console.log(`  - item #${i.index}: ${errorsOf(i).join('; ') || 'invalid'}`);
  console.log('');
}

console.log(`TOTAL  ${apply ? 'created' : 'would create'}: ${totals.ok}   duplicates: ${totals.duplicate}   invalid: ${totals.invalid}`);

if (publish && createdIds.length > 0) {
  console.log(`\nPublishing ${createdIds.length} question(s) created in this run...`);
  for (let i = 0; i < createdIds.length; i += 200) {
    const chunk = createdIds.slice(i, i + 200);
    const { status, json, text } = await post(`${base}/api/v1/admin/questions/publish`, { questionIds: chunk });
    if (status !== 200 || !json) {
      console.log(`  HTTP ${status}: ${(json && (json.code + ' ' + json.message)) || text.slice(0, 200)}`);
      anyProblem = true;
      continue;
    }
    const items = json.items ?? [];
    const failed = items.filter((r) => r.published === false);
    console.log(`  chunk ${i / 200 + 1}: published ${items.length - failed.length}, failed ${failed.length}`);
    for (const r of failed) console.log(`  - question ${r.questionId}: ${(r.reasons ?? r.errors ?? []).join('; ')}`);
    if (failed.length) anyProblem = true;
  }
}

process.exit(anyProblem ? 1 : 0);
