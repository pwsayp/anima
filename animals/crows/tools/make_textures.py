#!/usr/bin/env python3
"""Текстуры мода Crows без внешних зависимостей (только stdlib).

Запускать из корня репозитория:

    python3 animals/crows/tools/make_textures.py

Рисуются две развёртки вороны — чёрная и серая — и яйцо призыва.

Про черноту. Чёрное в игре — ловушка: залитый одним цветом моб превращается в силуэт без
формы, и с двух шагов не понять, где спина, а где крыло. Поэтому чернота набрана из
нескольких очень близких тонов, а по спине и крылу пущен слабый синеватый отлив — тот
самый блеск пера, что виден на фото. Отлив слабый: он должен читаться как блеск, а не как
раскраска.

Про серую ворону. Это не другой мод и не другая модель — только раскраска: серые спина,
брюхо и шея при чёрных голове, крыльях и хвосте. Та самая птица, которую видно из окна в
средней полосе.

**Развёртка обязана совпадать с texOffs в CrowModel.java.** Числа продублированы здесь
намеренно: скрипт и модель — разные языки, общего источника у них нет, и рассинхрон
проявится сразу же кашей на текстуре.
"""
import os
import random
import struct
import zlib

random.seed(20260807)

OUT = 'animals/crows/src/main/resources/assets/crows/textures'

# --- Раскраски -------------------------------------------------------------
# Роли: body — корпус, wing — крыло, head — голова, beak — клюв, tail — хвост,
# leg — лапа. Отлив (sheen) кладётся поверх верхних граней корпуса и крыла.
BLACK = {
    'body': (26, 26, 30, 255),
    'body_top': (20, 20, 24, 255),
    'wing': (22, 22, 26, 255),
    'head': (24, 24, 28, 255),
    'throat': (20, 20, 23, 255),
    'tail': (18, 18, 22, 255),
    'beak': (44, 44, 48, 255),
    'beak_dark': (28, 28, 32, 255),
    'leg': (38, 38, 42, 255),
    'sheen': (52, 56, 72, 255),
}

# Серая ворона: чёрные голова, крылья и хвост при светлом корпусе.
HOODED = dict(BLACK)
HOODED.update({
    'body': (124, 124, 128, 255),
    'body_top': (108, 108, 114, 255),
    'sheen': (156, 156, 162, 255),
})

EYE = (12, 12, 14, 255)
EYE_GLINT = (168, 168, 178, 255)

# --- Развёртки силуэтов ----------------------------------------------------
# Каждая часть: имя роли и (u, v, ширина, высота, глубина) — ровно как в CrowModel.
SHAPES = {
    'layered': {
        'body': (0, 0, 5, 5, 4),
        'rump': (18, 0, 4, 4, 4),
        'head': (34, 0, 3.5, 3.5, 3.5),
        'wing': (48, 0, 1, 4, 4),
        'primaries': (0, 9, 1, 3, 5),
        'rectrices': (12, 9, 4, 1, 6),
        'tail': (32, 9, 3, 1, 3),
        'neck': (44, 9, 3, 3, 2),
        'beak': (0, 17, 1.5, 1.5, 4),
        'beak_lower': (28, 17, 1.5, 0.85, 3.5),
        'shank': (12, 17, 1, 3, 1),
        'toes': (17, 17, 2, 1, 3),
    },
}

# Какой краской красить какую часть.
ROLE = {
    'body': 'body', 'rump': 'body', 'neck': 'body',
    'wing': 'wing', 'primaries': 'wing',
    'head': 'head', 'throat': 'throat',
    'tail': 'tail', 'tail_tip': 'tail', 'rectrices': 'tail',
    'beak': 'beak', 'beak_lower': 'beak', 'beak_tip': 'beak',
    'shank': 'leg', 'toes': 'leg',
}


class Img:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.px = [[(0, 0, 0, 0)] * w for _ in range(h)]

    def set(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[y][x] = c

    def rect(self, x, y, w, h, c, noise=0):
        for j in range(int(y), int(y + h + 0.999)):
            for i in range(int(x), int(x + w + 0.999)):
                self.set(i, j, shade(c, noise))

    def save(self, path):
        raw = b''
        for row in self.px:
            raw += b'\x00' + b''.join(bytes(p) for p in row)
        png = b'\x89PNG\r\n\x1a\n'
        png += chunk(b'IHDR', struct.pack('>IIBBBBB', self.w, self.h, 8, 6, 0, 0, 0))
        png += chunk(b'IDAT', zlib.compress(raw, 9))
        png += chunk(b'IEND', b'')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, 'wb') as f:
            f.write(png)
        print('written', path)


def chunk(tag, body):
    return struct.pack('>I', len(body)) + tag + body + struct.pack('>I', zlib.crc32(tag + body))


def shade(c, noise):
    if not noise:
        return c
    d = random.randint(-noise, noise)
    return (clamp(c[0] + d), clamp(c[1] + d), clamp(c[2] + d), c[3])


def clamp(v):
    return max(0, min(255, v))


def faces(u, v, w, h, d):
    """Развёртка коробки — та же раскладка, что у самой игры."""
    return {
        'top': (u + d, v, w, d),
        'bottom': (u + d + w, v, w, d),
        'right': (u, v + d, d, h),
        'front': (u + d, v + d, w, h),
        'left': (u + d + w, v + d, d, h),
        'back': (u + 2 * d + w, v + d, w, h),
        'all': (u, v, 2 * d + 2 * w, d + h),
    }


def sheen(img, face, colour):
    """Редкие светлые пиксели — блик на пере, гуще к верхнему краю."""
    x, y, w, h = face
    for j in range(int(h)):
        for i in range(int(w)):
            if (i * 7 + j * 5) % 3 == 0 and random.random() < 0.4 - j * 0.06:
                img.set(int(x) + i, int(y) + j, shade(colour, 12))


def crow(shape, palette, name):
    img = Img(64, 32)

    for part, (u, v, w, h, d) in SHAPES[shape].items():
        colour = palette[ROLE[part]]
        f = faces(u, v, w, h, d)
        img.rect(*f['all'], colour, 5)

        if part in ('body', 'rump', 'wing', 'primaries', 'head', 'neck'):
            # Спина темнее боков, и по ней отлив: сверху птицу и видно.
            img.rect(*f['top'], palette['body_top'] if part != 'head' else palette['head'], 4)
            sheen(img, f['top'], palette['sheen'])

        if part in ('tail', 'tail_tip', 'rectrices'):
            # Поперечная штриховка по перьям.
            x, y, tw, th = f['top']
            for i in range(0, int(tw), 2):
                img.rect(x + i, y, 1, th, palette['body_top'], 5)

        if part in ('beak', 'beak_tip', 'beak_lower'):
            img.rect(*f['bottom'], palette['beak_dark'], 3)

        if part == 'head':
            # Глаз почти не виден на чёрной голове — весь он держится на одной искре.
            for face_name, glint in (('right', 0), ('left', 1)):
                fx, fy, fw, fh = f[face_name]
                eye_x = int(fx) + (1 if glint == 0 else int(fw) - 3)
                img.rect(eye_x, int(fy) + 1, 2, 2, EYE)
                img.set(eye_x + glint, int(fy) + 1, EYE_GLINT)

    img.save(os.path.join(OUT, f'entity/{name}.png'))


# --- Яйцо ------------------------------------------------------------------
EGG_BASE = (32, 32, 38, 255)
EGG_LIGHT = (66, 70, 88, 255)
EGG_RIM = (22, 22, 26, 255)
EGG_RIM_DARK = (14, 14, 18, 255)
EGG_SPOT = (74, 78, 96, 255)

# Силуэт ванильного яйца призыва: для каждой строки — от какого до какого пикселя.
EGG_SHAPE = {
    1: (6, 9), 2: (5, 10), 3: (4, 11), 4: (3, 12), 5: (3, 12), 6: (2, 13), 7: (2, 13),
    8: (2, 13), 9: (2, 13), 10: (2, 13), 11: (2, 13), 12: (3, 12), 13: (4, 11), 14: (5, 10),
}

EGG_SPOTS = ((4, 3, 2, 1), (8, 4, 2, 2), (3, 7, 2, 1), (10, 7, 2, 1),
             (6, 9, 2, 2), (4, 11, 2, 1), (9, 11, 2, 1))


def egg():
    img = Img(16, 16)

    for y, (x0, x1) in EGG_SHAPE.items():
        for x in range(x0, x1 + 1):
            img.set(x, y, shade(EGG_BASE, 6))
        img.set(x0, y, EGG_RIM)
        img.set(x1, y, EGG_RIM_DARK)

    for x in range(*EGG_SHAPE[1]):
        img.set(x, 1, EGG_RIM)
    for x in range(*EGG_SHAPE[14]):
        img.set(x, 14, EGG_RIM_DARK)

    img.rect(5, 3, 2, 2, EGG_LIGHT, 6)
    img.set(4, 5, EGG_LIGHT)

    for x, y, w, h in EGG_SPOTS:
        for j in range(y, y + h):
            if j not in EGG_SHAPE:
                continue
            x0, x1 = EGG_SHAPE[j]
            for i in range(x, x + w):
                if x0 < i < x1:
                    img.set(i, j, shade(EGG_SPOT, 8))

    img.save(os.path.join(OUT, 'item/crow_spawn_egg.png'))


if __name__ == '__main__':
    crow('layered', BLACK, 'crow')
    crow('layered', HOODED, 'crow_hooded')
    egg()
