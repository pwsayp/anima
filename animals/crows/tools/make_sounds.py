#!/usr/bin/env python3
"""Звуки вороны из полевых записей — в ванильном виде.

Запускать из корня репозитория:

    python3 animals/crows/tools/make_sounds.py <файл.mp3> [ещё файлы...]

Зачем скрипт вообще нужен. Живая запись и звук мода — разные вещи. В записи карканье идёт
серией, между криками ветер и птичья мелочь на заднем плане, длится всё это секундами. Если
положить такое в игру как есть, ворона будет включать радиопередачу: длинную, с чужими
звуками и не туда попадающую по времени.

Ванильный звук устроен ровно наоборот, и правила у него простые:

* **короткий** — один крик, а не серия: от четверти секунды до полутора;
* **сухой** — без хвоста, паузы и фона: вырезается только сам крик;
* **моно** — стерео игра не разместит в пространстве, и звук будет идти «из головы»;
* **тихий и ровный** — приведён к одной громкости, чтобы не выделяться среди ванильных;
* **22 кГц** — та самая ламповая грубоватость, с которой звучит вся игра.

Что делает скрипт: разбирает mp3 через системный afconvert, ищет по огибающей отдельные
крики, вырезает их с запасом, нормирует, гасит края (щелчок на обрыве волны слышен даже
там, где сам обрыв не виден), понижает частоту и кодирует в ogg — единственный формат,
который Minecraft читает.
"""
import array
import audioop
import os
import subprocess
import sys
import tempfile
import wave

OUT = 'animals/crows/src/main/resources/assets/crows/sounds'

RATE = 22050
"""Частота готового звука: ванильная грубоватость."""

WINDOW_MS = 5
"""Шаг огибающей. Мельче — шумно, крупнее — смазываются края крика."""

THRESHOLD = 0.16
"""Доля от самого громкого места, ниже которой считаем, что крика нет."""

GAP_MS = 140
"""Разрыв короче этого — всё ещё один крик, а не два."""

PAD_MS = 25
PEAK = 0.88
MAX_MS = 1400


def decode(path):
    """mp3 → моно 44.1 кГц PCM. Декодировать умеет сама macOS, ставить нечего."""
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as tmp:
        wav = tmp.name
    subprocess.run(
        ['afconvert', '-f', 'WAVE', '-d', 'LEI16@44100', '-c', '1', path, wav],
        check=True, capture_output=True)
    with wave.open(wav, 'rb') as f:
        rate = f.getframerate()
        data = array.array('h')
        data.frombytes(f.readframes(f.getnframes()))
    os.unlink(wav)
    return rate, data


def envelope(data, rate):
    step = max(1, rate * WINDOW_MS // 1000)
    return [max(abs(v) for v in data[i:i + step]) for i in range(0, len(data), step)], step


def find_calls(data, rate):
    """Найти отдельные крики: куски, где громко, склеенные через короткие паузы."""
    env, step = envelope(data, rate)
    if not env:
        return []

    limit = max(env) * THRESHOLD
    gap = max(1, GAP_MS // WINDOW_MS)
    pad = rate * PAD_MS // 1000

    calls, start, quiet = [], None, 0
    for i, level in enumerate(env):
        if level >= limit:
            if start is None:
                start = i
            quiet = 0
        elif start is not None:
            quiet += 1
            if quiet >= gap:
                calls.append((start * step, (i - quiet) * step))
                start = None
    if start is not None:
        calls.append((start * step, len(data)))

    out = []
    for a, b in calls:
        a = max(0, a - pad)
        b = min(len(data), b + pad)
        if b - a < rate // 10:          # короче десятой доли секунды — это щелчок, не крик
            continue
        b = min(b, a + rate * MAX_MS // 1000)
        out.append((max(abs(v) for v in data[a:b]), a, b))
    out.sort(reverse=True)
    return out


def polish(data, rate, fade_out_ms=70, limit_ms=None):
    """Нормировать, погасить края и понизить частоту."""
    chunk = array.array('h', data)
    if limit_ms:
        chunk = chunk[:rate * limit_ms // 1000]

    peak = max((abs(v) for v in chunk), default=1) or 1
    gain = PEAK * 32767 / peak
    chunk = array.array('h', (int(max(-32768, min(32767, v * gain))) for v in chunk))

    # Края гасим всегда: обрыв волны на полуслове слышен как щелчок.
    fade_in = rate * 8 // 1000
    fade_out = rate * fade_out_ms // 1000
    for i in range(min(fade_in, len(chunk))):
        chunk[i] = int(chunk[i] * i / fade_in)
    for i in range(min(fade_out, len(chunk))):
        chunk[len(chunk) - 1 - i] = int(chunk[len(chunk) - 1 - i] * i / fade_out)

    converted, _ = audioop.ratecv(chunk.tobytes(), 2, 1, rate, RATE, None)
    return converted


def write_ogg(pcm, name):
    os.makedirs(OUT, exist_ok=True)
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as tmp:
        wav = tmp.name
    with wave.open(wav, 'wb') as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(RATE)
        f.writeframes(pcm)

    path = os.path.join(OUT, f'{name}.ogg')
    subprocess.run(['oggenc', '-Q', '-q', '3', '-o', path, wav], check=True)
    os.unlink(wav)
    print(f'written {path}  ({os.path.getsize(path)} байт, '
          f'{len(pcm) / 2 / RATE:.2f} с)')


def main(paths):
    calls = []
    for path in paths:
        rate, data = decode(path)
        found = find_calls(data, rate)
        print(f'{os.path.basename(path)}: криков найдено {len(found)}')
        # По одному лучшему крику с записи: три голоса из трёх файлов звучат
        # разнообразнее, чем три подряд из одного.
        for level, a, b in found[:2]:
            calls.append((level, rate, data[a:b]))

    if not calls:
        sys.exit('в этих файлах не нашлось ни одного крика')

    calls.sort(key=lambda c: -c[0])
    best = calls[:3]

    for i, (_, rate, chunk) in enumerate(best, start=1):
        write_ogg(polish(chunk, rate), f'crow_ambient{i}')

    # Крик боли — тот же голос, но обрубленный: птица не докаркивает.
    _, rate, chunk = best[0]
    write_ogg(polish(chunk, rate, fade_out_ms=40, limit_ms=380), 'crow_hurt')

    # Смерть — крик с долгим угасанием.
    _, rate, chunk = best[-1]
    write_ogg(polish(chunk, rate, fade_out_ms=260), 'crow_death')


if __name__ == '__main__':
    sources = sys.argv[1:]
    if not sources:
        sys.exit(__doc__)
    main(sources)
