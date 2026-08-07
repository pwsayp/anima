#!/usr/bin/env python3
"""Звуки саранчи из полевых записей — в ванильном виде.

Запускать из корня репозитория:

    python3 fauna/locusts/tools/make_sounds.py <файл.mp3> [ещё файлы...]

Чем это отличается от такого же скрипта у ворон. Ворона кричит отдельными криками, и там
задача была найти в записи один крик. У саранчи звук сплошной: стрекочет она без пауз, и
искать в этом «отдельный звук» бессмысленно — записи ровные от начала до конца. Поэтому
здесь не поиск, а **нарезка**: из самых громких мест берутся куски нужной длины.

Кусков нужно три сорта, и длина у них разная не случайно:

* **голос особи** — меньше секунды: он играется у каждой саранчи в стае, и длинный звук
  превратил бы тучу в сплошной гул из полутора сотен наложений;
* **налёт** — несколько секунд: это тот самый гул с горизонта, ради которого у игрока
  есть минута форы, и он должен звучать как надвигающаяся стая, а не как один кузнечик;
* **боль и смерть** — совсем коротко, обрывком.

Остальное как у ворон: моно (стерео игра не разместит в пространстве), приведённая
громкость, погашенные края, 22 кГц и ogg — единственный формат, который Minecraft читает.
"""
import array
import audioop
import os
import subprocess
import sys
import tempfile
import wave

OUT = 'fauna/locusts/src/main/resources/assets/locusts/sounds'

RATE = 22050
PEAK = 0.85
EDGE_MS = 400
"""Сколько миллисекунд отрезать с начала и конца записи: там щелчки и подводка."""


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


def loudest(data, rate, seconds, count=1):
    """Найти самые громкие куски нужной длины, не давая им наползать друг на друга."""
    length = int(rate * seconds)
    edge = rate * EDGE_MS // 1000
    step = max(1, length // 4)

    scored = []
    for start in range(edge, max(edge + 1, len(data) - length - edge), step):
        chunk = data[start:start + length]
        # Считаем по огрублённой выборке: точность тут не нужна, а скорость нужна.
        level = sum(abs(v) for v in chunk[::37]) / max(1, len(chunk[::37]))
        scored.append((level, start))

    scored.sort(reverse=True)
    picked = []
    for _, start in scored:
        if all(abs(start - other) >= length for other in picked):
            picked.append(start)
            if len(picked) == count:
                break
    return [data[s:s + length] for s in picked]


def polish(chunk, rate, fade_in_ms=15, fade_out_ms=80):
    """Нормировать, погасить края и понизить частоту."""
    chunk = array.array('h', chunk)

    peak = max((abs(v) for v in chunk), default=1) or 1
    gain = PEAK * 32767 / peak
    chunk = array.array('h', (int(max(-32768, min(32767, v * gain))) for v in chunk))

    # Края гасим всегда: обрыв волны на полуслове слышен как щелчок. У сплошного стрёкота
    # это важнее, чем у крика, — резать приходится ровно посередине звука.
    fade_in = max(1, rate * fade_in_ms // 1000)
    fade_out = max(1, rate * fade_out_ms // 1000)
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
    print(f'written {path}  ({os.path.getsize(path)} байт, {len(pcm) / 2 / RATE:.2f} с)')


def main(paths):
    voices = []
    swarms = []
    for path in paths:
        rate, data = decode(path)
        print(f'{os.path.basename(path)}: {len(data) / rate:.0f} с')
        for chunk in loudest(data, rate, 0.8, count=1):
            voices.append((rate, chunk))
        for chunk in loudest(data, rate, 4.5, count=1):
            swarms.append((rate, chunk))

    if not voices:
        sys.exit('нечего резать')

    for i, (rate, chunk) in enumerate(voices[:3], start=1):
        write_ogg(polish(chunk, rate), f'locust_ambient{i}')

    # Голос стаи — самый длинный и самый плотный кусок из всех записей.
    rate, chunk = max(swarms, key=lambda s: max(abs(v) for v in s[1][::53]))
    write_ogg(polish(chunk, rate, fade_in_ms=400, fade_out_ms=900), 'swarm_approach')

    # Боль и смерть — обрывки голоса.
    rate, chunk = voices[0]
    write_ogg(polish(chunk[:int(rate * 0.18)], rate, fade_out_ms=60), 'locust_hurt')
    write_ogg(polish(chunk[:int(rate * 0.35)], rate, fade_out_ms=200), 'locust_death')


if __name__ == '__main__':
    sources = sys.argv[1:]
    if not sources:
        sys.exit(__doc__)
    main(sources)
