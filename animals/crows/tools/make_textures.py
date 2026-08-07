#!/usr/bin/env python3
"""Текстуры мода Crows без внешних зависимостей (только stdlib).

Запускать из корня репозитория:

    python3 animals/crows/tools/make_textures.py

Рисуются две вещи: развёртка вороны 64×32 под модель из CrowModel и яйцо призыва.

Про ворону. Она чёрная, но чёрное в игре — это ловушка: залитый одним цветом моб
превращается в силуэт без формы, и с двух шагов не понять, где спина, а где крыло. Поэтому
чернота здесь набрана из нескольких очень близких тонов, а по спине и сложенному крылу
пущен синеватый отлив — тот самый, что виден на фото у настоящей вороны на солнце. Отлив
слабый: он должен читаться как блеск пера, а не как раскраска.

Развёртка повторяет texOffs из модели: тело (0,0), голова (26,0), клюв (44,0),
крыло (0,13), хвост (18,13), цевка (44,13), пальцы (48,13).

Про яйцо. Ванильные яйца призыва — это одна и та же форма в 16×16, у каждого моба своя
раскраска: основной цвет и крап поверх него. Здесь ровно то же самое, силуэт снят с
ванильного яйца пиксель в пиксель, цвета — вороньи.
"""
import os
import random
import struct
import zlib

random.seed(20260807)

OUT = 'animals/crows/src/main/resources/assets/crows/textures'

# --- Ворона ----------------------------------------------------------------
BODY = (26, 26, 30, 255)
BACK = (20, 20, 24, 255)
SHEEN = (52, 56, 72, 255)
BREAST = (32, 32, 36, 255)
HEAD = (24, 24, 28, 255)
EYE = (12, 12, 14, 255)
EYE_GLINT = (168, 168, 178, 255)
BEAK = (44, 44, 48, 255)
BEAK_DARK = (28, 28, 32, 255)
WING = (22, 22, 26, 255)
TAIL = (18, 18, 22, 255)
LEG = (38, 38, 42, 255)

# --- Яйцо ------------------------------------------------------------------
EGG_BASE = (32, 32, 38, 255)
EGG_LIGHT = (66, 70, 88, 255)
EGG_RIM = (22, 22, 26, 255)
EGG_RIM_DARK = (14, 14, 18, 255)
EGG_SPOT = (74, 78, 96, 255)


class Img:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.px = [[(0, 0, 0, 0)] * w for _ in range(h)]

    def set(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[y][x] = c

    def rect(self, x, y, w, h, c, noise=0):
        for j in range(y, y + h):
            for i in range(x, x + w):
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


class Box:
    """Развёртка коробки W×H×D с texOffs (u,v) — та же раскладка, что у самой игры.

    Верхний ряд: сверху (u+d, v) и снизу (u+d+w, v), оба w×d.
    Нижний ряд: правый бок (u, v+d), перед (u+d, v+d), левый бок (u+d+w, v+d),
    зад (u+d+w+d, v+d) — боковины d×h, перед и зад w×h.
    """

    def __init__(self, u, v, w, h, d):
        self.u, self.v, self.w, self.h, self.d = u, v, w, h, d

    def fill(self, img, colour, noise=0):
        img.rect(self.u, self.v, 2 * self.d + 2 * self.w, self.d + self.h, colour, noise)

    def top(self):
        return self.u + self.d, self.v, self.w, self.d

    def bottom(self):
        return self.u + self.d + self.w, self.v, self.w, self.d

    def right(self):
        return self.u, self.v + self.d, self.d, self.h

    def front(self):
        return self.u + self.d, self.v + self.d, self.w, self.h

    def left(self):
        return self.u + self.d + self.w, self.v + self.d, self.d, self.h

    def back(self):
        return self.u + 2 * self.d + self.w, self.v + self.d, self.w, self.h


def sheen(img, face, colour, density=3):
    """Редкие светлые пиксели — блик на пере. Гуще к верхнему краю грани."""
    x, y, w, h = face
    for j in range(h):
        for i in range(w):
            if (i * 7 + j * 5) % density == 0 and random.random() < 0.45 - j * 0.05:
                img.set(x + i, y + j, shade(colour, 12))


def crow():
    img = Img(64, 32)

    # Тело: спина темнее груди, по спине отлив.
    body = Box(0, 0, 5, 5, 7)
    body.fill(img, BODY, 5)
    img.rect(*body.top(), BACK, 4)
    sheen(img, body.top(), SHEEN)
    img.rect(*body.front(), BREAST, 5)
    img.rect(*body.bottom(), BREAST, 4)

    # Голова: глаз сидит высоко и близко к клюву. На чёрной птице сам глаз почти не виден —
    # весь он держится на одной светлой искре, как на фото. Два светлых пикселя вместо
    # одного дают выпученный взгляд, поэтому искра ровно одна на щёку.
    head = Box(26, 0, 4, 4, 4)
    head.fill(img, HEAD, 5)
    img.rect(*head.top(), BACK, 4)
    for face, glint_at in ((head.right(), 0), (head.left(), 1)):
        fx, fy, fw, fh = face
        # Клюв смотрит в −Z: на правой грани перед — это левый край, на левой — правый.
        eye_x = fx + (1 if glint_at == 0 else fw - 3)
        img.rect(eye_x, fy + 1, 2, 2, EYE)
        img.set(eye_x + glint_at, fy + 1, EYE_GLINT)

    # Клюв: верх светлее, низ уходит в тень — так видно горбинку.
    beak = Box(44, 0, 2, 2, 3)
    beak.fill(img, BEAK, 4)
    img.rect(*beak.bottom(), BEAK_DARK, 3)

    # Сложенное крыло: тёмное, с продольным отливом по верхнему краю.
    wing = Box(0, 13, 1, 4, 8)
    wing.fill(img, WING, 4)
    for face in (wing.right(), wing.left()):
        fx, fy, fw, fh = face
        img.rect(fx, fy, fw, 1, BACK, 3)
        sheen(img, (fx, fy, fw, 2), SHEEN, 2)

    # Хвост: самый тёмный, с поперечной штриховкой по перьям.
    tail = Box(18, 13, 5, 1, 7)
    tail.fill(img, TAIL, 4)
    for face in (tail.top(), tail.bottom()):
        fx, fy, fw, fh = face
        for i in range(0, fw, 2):
            img.rect(fx + i, fy, 1, fh, BACK, 5)

    # Лапы.
    Box(44, 13, 1, 3, 1).fill(img, LEG, 5)
    Box(48, 13, 2, 1, 3).fill(img, LEG, 5)

    img.save(os.path.join(OUT, 'entity/crow.png'))


# Силуэт ванильного яйца призыва: для каждой строки — от какого до какого пикселя.
EGG_SHAPE = {
    1: (6, 9), 2: (5, 10), 3: (4, 11), 4: (3, 12), 5: (3, 12), 6: (2, 13), 7: (2, 13),
    8: (2, 13), 9: (2, 13), 10: (2, 13), 11: (2, 13), 12: (3, 12), 13: (4, 11), 14: (5, 10),
}

# Крап поверх основного цвета: (x, y, ширина, высота).
EGG_SPOTS = ((4, 3, 2, 1), (8, 4, 2, 2), (3, 7, 2, 1), (10, 7, 2, 1),
             (6, 9, 2, 2), (4, 11, 2, 1), (9, 11, 2, 1))


def egg():
    img = Img(16, 16)

    for y, (x0, x1) in EGG_SHAPE.items():
        for x in range(x0, x1 + 1):
            img.set(x, y, shade(EGG_BASE, 6))
        # Ободок: слева и сверху светлее, справа и снизу глубже — так яйцо кажется круглым.
        img.set(x0, y, EGG_RIM)
        img.set(x1, y, EGG_RIM_DARK)

    for x in range(*EGG_SHAPE[1]):
        img.set(x, 1, EGG_RIM)
    for x in range(*EGG_SHAPE[14]):
        img.set(x, 14, EGG_RIM_DARK)

    # Блик сверху слева.
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
    crow()
    egg()
