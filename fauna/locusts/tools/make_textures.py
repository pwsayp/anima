#!/usr/bin/env python3
"""Текстуры мода Locusts без внешних зависимостей (только stdlib).

Запускать из корня репозитория:

    python3 fauna/locusts/tools/make_textures.py

Рисуются две вещи: развёртка саранчи 64×32 под модель из LocustModel и яйцо призыва.

Про саранчу. Особь размером с треть блока и почти никогда не видна одна, поэтому мелкие
детали смысла не имеют — работает силуэт и три пятна цвета: песочное тело, тёмная пестрядь
на брюшке и крыльях, чёрный глаз. Пестрядь взята с натуры: у пустынной саранчи бок и надкрылья
в тёмную клетку, и именно она отличает её от зелёного кузнечика.

Крылья полупрозрачные, как стекло с прожилками, — поэтому модель рисуется полупрозрачным
слоем (см. LocustModel). Всё остальное непрозрачно: сквозь тело просвечивать нечему.

Развёртка повторяет texOffs из модели: тело (0,0), крыло (17,0), переднеспинка (36,0),
голова (48,0), бедро (0,9), голень (9,9), лапка (18,9), усик (23,9).

Про яйцо. Ванильные яйца призыва — это одна и та же форма в 16×16, у каждого моба своя
раскраска: основной цвет и крап поверх него. Здесь ровно то же самое, силуэт снят с
ванильного яйца пиксель в пиксель, цвета — саранчиные.
"""
import os
import random
import struct
import zlib

random.seed(20260807)

OUT = 'fauna/locusts/src/main/resources/assets/locusts/textures'

# --- Саранча ---------------------------------------------------------------
BODY = (196, 178, 126, 255)
BODY_TOP = (156, 138, 92, 255)
MARK = (88, 70, 44, 255)
THORAX = (188, 168, 116, 255)
# Крыло — мутноватое стекло: сквозь него видно спину, но плёнка читается. Прожилки и
# крап заметно плотнее, иначе на просвет крыло исчезает совсем.
WING = (206, 202, 186, 130)
WING_VEIN = (150, 140, 112, 205)
WING_MARK = (104, 88, 60, 225)
HEAD = (200, 184, 136, 255)
EYE = (54, 44, 32, 255)
EYE_GLINT = (222, 214, 190, 255)
LEG = (206, 192, 148, 255)
LEG_MARK = (104, 84, 50, 255)

# --- Яйцо ------------------------------------------------------------------
EGG_BASE = (198, 178, 120, 255)
EGG_LIGHT = (226, 210, 160, 255)
EGG_RIM = (150, 132, 84, 255)
EGG_RIM_DARK = (118, 100, 60, 255)
EGG_SPOT = (108, 86, 50, 255)


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


def speckle(img, face, colour, step=2, offset=0):
    """Тёмная клетка по боку: через пиксель, со сдвигом ряда — так читается пестрядь."""
    x, y, w, h = face
    for j in range(h):
        for i in range(w):
            if (i + j * offset) % step == 0:
                img.set(x + i, y + j, shade(colour, 10))


def locust():
    img = Img(64, 32)

    # Брюшко: спина темнее, бока в тёмную клетку.
    body = Box(0, 0, 2, 2, 6)
    body.fill(img, BODY, 8)
    img.rect(*body.top(), BODY_TOP, 6)
    speckle(img, body.right(), MARK, 2, 1)
    speckle(img, body.left(), MARK, 2, 1)

    # Крылья: полупрозрачная плёнка с продольной прожилкой и редким тёмным крапом.
    # Толщина у крыла 0.5, поэтому под развёртку берём целую строку — торцы её и займут.
    wing = Box(17, 0, 3, 1, 6)
    wing.fill(img, WING, 5)
    for face in (wing.top(), wing.bottom()):
        fx, fy, fw, fh = face
        # Прожилка вдоль переднего края — то, за что крыло цепляется взглядом.
        img.rect(fx, fy, 1, fh, WING_VEIN, 8)
        for j in range(fy, fy + fh, 2):
            img.set(fx + fw - 1, j, shade(WING_MARK, 10))
        img.set(fx + 1, fy + 1, shade(WING_MARK, 10))
        img.set(fx + 1, fy + fh - 2, shade(WING_MARK, 10))

    # Переднеспинка: горб с тёмным килем по хребту.
    thorax = Box(36, 0, 3, 3, 3)
    thorax.fill(img, THORAX, 7)
    x, y, w, h = thorax.top()
    img.rect(x + w // 2, y, 1, h, BODY_TOP, 5)
    for face in (thorax.right(), thorax.left()):
        fx, fy, fw, fh = face
        img.rect(fx, fy + fh - 1, fw, 1, MARK, 8)

    # Голова: глаз во всю щёку с искрой — у саранчи он именно такой.
    head = Box(48, 0, 2, 2, 2)
    head.fill(img, HEAD, 6)
    for face in (head.right(), head.left()):
        fx, fy, fw, fh = face
        img.rect(fx, fy, fw, fh, EYE, 6)
        img.set(fx, fy, EYE_GLINT)

    # Задняя нога: бедро в тёмную ёлочку, голень светлая с тёмным концом.
    femur = Box(0, 9, 1, 1, 3)
    femur.fill(img, LEG, 6)
    speckle(img, femur.right(), LEG_MARK, 2, 0)
    speckle(img, femur.left(), LEG_MARK, 2, 0)
    speckle(img, femur.top(), LEG_MARK, 2, 0)

    shin = Box(9, 9, 1, 1, 3)
    shin.fill(img, LEG, 5)
    for face in (shin.right(), shin.left(), shin.top()):
        fx, fy, fw, fh = face
        img.rect(fx + fw - 1, fy, 1, fh, LEG_MARK, 6)

    # Мелкие лапки и усики. Усик 0.5×0.5×2 занимает по развёртке 5×3.
    Box(18, 9, 1, 2, 1).fill(img, LEG, 6)
    img.rect(23, 9, 5, 3, LEG_MARK, 8)

    img.save(os.path.join(OUT, 'entity/locust.png'))


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
            img.set(x, y, shade(EGG_BASE, 8))
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

    img.save(os.path.join(OUT, 'item/locust_spawn_egg.png'))


# Сбитая особь боком: голова слева, брюшко вправо, задняя нога «домиком» вниз.
# Читаться должно с одного взгляда в слоте инвентаря, поэтому силуэт грубый и крупный.
#   a усик, h голова, E глаз, t переднеспинка, b брюшко, w крыло, f бедро, g голень, l лапка
ITEM_SHAPE = (
    '................',
    '................',
    '...a............',
    '....a...........',
    '....hh..wwwwww..',
    '...hEhtttbbbbb..',
    '...hhhtttbbbbbb.',
    '....llttfbbbbb..',
    '.....l..ff......',
    '........ff......',
    '.........gg.....',
    '..........gg....',
    '...........g....',
    '................',
    '................',
    '................',
)

RAW_PALETTE = {
    'a': (104, 84, 50, 255),
    'h': (200, 184, 136, 255),
    'E': (54, 44, 32, 255),
    't': (188, 168, 116, 255),
    'b': (196, 178, 126, 255),
    'w': (206, 202, 186, 255),
    'f': (206, 192, 148, 255),
    'g': (176, 160, 116, 255),
    'l': (186, 172, 130, 255),
}

# Жареная: то же самое, но пропечённое до тёмно-медового, а крылья ссохлись и потемнели.
COOKED_PALETTE = {
    'a': (72, 52, 30, 255),
    'h': (156, 116, 62, 255),
    'E': (44, 32, 22, 255),
    't': (142, 102, 54, 255),
    'b': (150, 108, 58, 255),
    'w': (128, 96, 56, 255),
    'f': (162, 122, 68, 255),
    'g': (134, 98, 52, 255),
    'l': (146, 108, 60, 255),
}


def item(palette, name, noise=8):
    img = Img(16, 16)
    for y, row in enumerate(ITEM_SHAPE):
        for x, code in enumerate(row):
            if code != '.':
                img.set(x, y, shade(palette[code], 0 if code == 'E' else noise))
    img.save(os.path.join(OUT, f'item/{name}.png'))


if __name__ == '__main__':
    locust()
    egg()
    item(RAW_PALETTE, 'locust')
    item(COOKED_PALETTE, 'cooked_locust')
