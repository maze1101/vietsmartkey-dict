"""Đổi chuỗi trong AndroidManifest.xml nhị phân (AXML): viết lại string pool."""
import struct

def _dec_len8(b, o):
    n = b[o]; o += 1
    if n & 0x80: n = ((n & 0x7f) << 8) | b[o]; o += 1
    return n, o

def _enc_len8(n):
    return bytes([n]) if n < 0x80 else bytes([0x80 | (n >> 8), n & 0xff])

def rewrite(b, mapping):
    t, hs, total = struct.unpack_from('<HHI', b, 0)
    assert t == 0x0003
    sp = hs
    ct, chs, csize, count, scount, flags, sstart, stystart = struct.unpack_from('<HHIIIIII', b, sp)
    assert ct == 0x0001
    utf8 = bool(flags & 0x100)
    offs = struct.unpack_from('<%dI' % count, b, sp + chs)
    strs = []
    for o in offs:
        p = sp + sstart + o
        if utf8:
            _, p = _dec_len8(b, p); m, p = _dec_len8(b, p)
            strs.append(b[p:p + m].decode('utf-8'))
        else:
            n = struct.unpack_from('<H', b, p)[0]; p += 2
            if n & 0x8000: n = ((n & 0x7fff) << 16) | struct.unpack_from('<H', b, p)[0]; p += 2
            strs.append(b[p:p + 2 * n].decode('utf-16le'))
    changed = []
    for i, s in enumerate(strs):
        if s in mapping:
            changed.append((s, mapping[s])); strs[i] = mapping[s]
    data = bytearray(); new_offs = []
    for s in strs:
        new_offs.append(len(data))
        if utf8:
            e = s.encode('utf-8')
            data += _enc_len8(len(s)) + _enc_len8(len(e)) + e + b'\0'
        else:
            e = s.encode('utf-16le'); n = len(e) // 2
            data += (struct.pack('<H', n) if n < 0x8000 else struct.pack('<HH', 0x8000 | (n >> 16), n & 0xffff)) + e + b'\0\0'
    while len(data) % 4: data += b'\0'
    styles = b''
    if scount:
        styoffs = b[sp + chs + 4 * count: sp + chs + 4 * count + 4 * scount]
        styles = b[sp + stystart: sp + csize]
    else:
        styoffs = b''
    new_sstart = chs + 4 * count + 4 * scount
    new_stystart = (new_sstart + len(data)) if scount else 0
    pool = struct.pack('<HHIIIIII', ct, chs, 0, count, scount, flags, new_sstart, new_stystart)
    pool += b[sp + 28: sp + chs]  # phan header con lai (neu co)
    pool += struct.pack('<%dI' % count, *new_offs) + styoffs + bytes(data) + styles
    pool = pool[:4] + struct.pack('<I', len(pool)) + pool[8:]
    rest = b[sp + csize:]
    out = b[:sp] + pool + rest
    out = out[:4] + struct.pack('<I', len(out)) + out[8:]
    return bytes(out), changed
