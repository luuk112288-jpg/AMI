import assert from 'node:assert/strict';
import { readFileSync, readdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { createHash } from 'node:crypto';

const root = fileURLToPath(new URL('../', import.meta.url));
const audio = path.join(root, 'app/src/main/assets/audio');
const files = readdirSync(audio).filter(name => name.endsWith('.wav')).sort();
assert.equal(files.length, 8, 'Eight bundled demo tracks are required.');
const hashes = new Set();
for (const name of files) {
  const wav = readFileSync(path.join(audio, name));
  assert.equal(wav.toString('ascii', 0, 4), 'RIFF', name);
  assert.equal(wav.toString('ascii', 8, 12), 'WAVE', name);
  assert.equal(wav.readUInt32LE(4), wav.length - 8, name);
  assert.equal(wav.readUInt32LE(24), 22050, name);
  assert.equal(wav.readUInt32LE(40) / wav.readUInt32LE(28), 40, name);
  assert.equal(wav.readUInt16LE(22), 1, name);
  assert.equal(wav.readUInt16LE(34), 16, name);
  let peak = 0;
  let energy = 0;
  for (let index = 44; index < wav.length; index += 2) {
    const value = wav.readInt16LE(index);
    peak = Math.max(peak, Math.abs(value));
    energy += value * value;
  }
  assert.ok(peak > 2000 && peak < 32767, `${name} must be audible and unclipped`);
  assert.ok(energy > 0, `${name} must not be silent`);
  const hash = createHash('sha256').update(wav).digest('hex');
  assert.ok(!hashes.has(hash), `${name} must have its own composition`);
  hashes.add(hash);
}
console.log('Verified 8 distinct, non-silent, unclipped 40-second mono PCM WAV assets.');
console.log('This validates assets only; Android compilation and device playback are not verified.');
