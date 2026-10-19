import { mkdirSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

// Original deterministic compositions. No recordings or external samples.
export function renderTrack(index, { seconds = 40, sampleRate = 22050 } = {}) {
  const samples = Math.floor(seconds * sampleRate);
  const wav = Buffer.alloc(44 + samples * 2);
  wav.write('RIFF', 0); wav.writeUInt32LE(wav.length - 8, 4); wav.write('WAVEfmt ', 8);
  wav.writeUInt32LE(16, 16); wav.writeUInt16LE(1, 20); wav.writeUInt16LE(1, 22);
  wav.writeUInt32LE(sampleRate, 24); wav.writeUInt32LE(sampleRate * 2, 28);
  wav.writeUInt16LE(2, 32); wav.writeUInt16LE(16, 34); wav.write('data', 36); wav.writeUInt32LE(samples * 2, 40);
  const roots = [45, 48, 50, 43, 52, 46, 55, 41];
  const progressions = [[0, 5, 3, 7], [0, 7, 5, 3], [0, 3, 8, 5], [0, 5, 10, 7]];
  const steps = [0, 7, 12, 10, 7, 3, 5, 12, 14, 12, 7, 5, 3, 7, 10, 5];
  const bpm = 76 + index * 3;
  const beat = 60 / bpm;
  const frequency = note => 440 * 2 ** ((note - 69) / 12);
  const tau = Math.PI * 2;
  for (let n = 0; n < samples; n++) {
    const t = n / sampleRate;
    const chord = Math.floor(t / (beat * 4)) % 4;
    const root = roots[index % 8] + progressions[index % 4][chord];
    const tick = t % beat;
    const eighth = t % (beat / 2);
    const step = Math.floor(t / (beat / 2));
    let pad = 0;
    for (const interval of [12, 15, 19, 22]) {
      const hz = frequency(root + interval);
      pad += Math.sin(tau * hz * t + .07 * Math.sin(tau * .2 * t)) * .035;
    }
    const bass = Math.sin(tau * frequency(root) * t) * .19 * Math.exp(-tick * 3);
    const melodyHz = frequency(root + 24 + steps[(step + index * 2) % steps.length]);
    const melody = (Math.sin(tau * melodyHz * t) + .2 * Math.sin(tau * melodyHz * 2 * t)) * Math.exp(-eighth * 10) * .12;
    const kickTime = t % (beat * 2);
    const kick = Math.sin(tau * (48 * kickTime + 11 * (1 - Math.exp(-kickTime * 30)))) * Math.exp(-kickTime * 22) * .23;
    const hat = (Math.sin(tau * 3119 * t) + Math.sin(tau * 4871 * t)) * Math.exp(-eighth * 100) * .021;
    const fade = Math.min(1, t / 1.5, (samples - 1 - n) / (sampleRate * 2));
    const value = Math.tanh((pad + bass + melody + kick + hat) * 1.3) * fade * .85;
    wav.writeInt16LE(Math.round(value * 32766), 44 + n * 2);
  }
  return wav;
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const target = fileURLToPath(new URL('../app/src/main/assets/audio/', import.meta.url));
  mkdirSync(target, { recursive: true });
  for (let i = 0; i < 8; i++) writeFileSync(path.join(target, `demo-${i}.wav`), renderTrack(i));
  process.stdout.write('Generated 8 original 40-second PCM WAV tracks.\n');
}
