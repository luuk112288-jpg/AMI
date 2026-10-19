import test from 'node:test';
import assert from 'node:assert/strict';
import { renderTrack } from './generate-audio.mjs';

test('original audio is a valid mono PCM WAV with non-silent samples', () => {
  const wav = renderTrack(0, { seconds: 2, sampleRate: 8000 });
  assert.equal(wav.toString('ascii', 0, 4), 'RIFF');
  assert.equal(wav.toString('ascii', 8, 12), 'WAVE');
  assert.equal(wav.readUInt32LE(4), wav.length - 8);
  assert.equal(wav.readUInt16LE(20), 1);
  assert.equal(wav.readUInt16LE(22), 1);
  assert.equal(wav.readUInt32LE(24), 8000);
  assert.equal(wav.readUInt16LE(34), 16);
  assert.equal(wav.readUInt32LE(40), 2 * 8000 * 2);
  let peak = 0;
  for (let i = 44; i < wav.length; i += 2) peak = Math.max(peak, Math.abs(wav.readInt16LE(i)));
  assert.ok(peak > 2000 && peak < 32767, `expected audible, unclipped samples, peak=${peak}`);
  assert.equal(wav.readInt16LE(wav.length - 2), 0);
});
test('composition is repeatable and each demo track differs', () => {
  const options = { seconds: 1, sampleRate: 8000 };
  assert.deepEqual(renderTrack(0, options), renderTrack(0, options));
  assert.notDeepEqual(renderTrack(0, options), renderTrack(1, options));
});
