import zipfile, struct, sys
src, dex, out = sys.argv[1], sys.argv[2], sys.argv[3]
newcode = int(sys.argv[4]) if len(sys.argv) > 4 else None
rename = len(sys.argv) > 5 and sys.argv[5] == 'rename'
import os; sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import axml_rename
RENAME = {
    'vn.personal.vietkey': 'vn.personal.vietkey.fast',
    'vn.personal.vietkey.mlkitinitprovider': 'vn.personal.vietkey.fast.mlkitinitprovider',
    'vn.personal.vietkey.androidx-startup': 'vn.personal.vietkey.fast.androidx-startup',
    'vn.personal.vietkey.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION': 'vn.personal.vietkey.fast.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION',
    'VietSmartKey': 'VietSmartKey Fast',
    'VietSmartKey \u00b7 Ti\u1ebfng Vi\u1ec7t': 'VietSmartKey Fast \u00b7 Ti\u1ebfng Vi\u1ec7t',
}

def patch_manifest(b, code):
    # AXML: string pool -> find "versionCode"; then find attribute record with that name index in <manifest>
    assert struct.unpack_from('<HH', b, 0)[0] == 0x0003
    sp = 8
    t, hs, size, count, scount, flags, sstart, stylestart = struct.unpack_from('<HHIIIIII', b, sp)
    assert t == 0x0001
    utf8 = flags & 0x100
    offs = struct.unpack_from('<%dI' % count, b, sp + hs)
    def s(i):
        o = sp + sstart + offs[i]
        if utf8:
            n = b[o]; o += 1
            if n & 0x80: o += 1
            m = b[o]; o += 1
            if m & 0x80: m = ((m & 0x7f) << 8) | b[o]; o += 1
            return b[o:o + m].decode('utf-8')
        n = struct.unpack_from('<H', b, o)[0]
        return b[o + 2:o + 2 + n * 2].decode('utf-16le')
    names = [s(i) for i in range(count)]
    vc = [i for i, n in enumerate(names) if n == 'versionCode']
    # walk chunks for first START_ELEMENT (manifest)
    p = sp + size
    while p < len(b):
        ct, chs, csz = struct.unpack_from('<HHI', b, p)
        if ct == 0x0102:
            ns, name, astart, asize, acount = struct.unpack_from('<IIHHH', b, p + 16)
            assert names[name] == 'manifest'
            for k in range(acount):
                a = p + 16 + astart + k * asize
                ans, aname, araw, vsize, res0, vtype, vdata = struct.unpack_from('<IIIHBBI', b, a)
                if aname in vc:
                    assert vtype == 0x10, vtype
                    print('versionCode', vdata, '->', code)
                    b = bytearray(b); struct.pack_into('<I', b, a + 16, code); return bytes(b)
            raise SystemExit('versionCode attr not found')
        p += csz
    raise SystemExit('manifest element not found')

zin = zipfile.ZipFile(src)
zout = zipfile.ZipFile(out, 'w')
for info in zin.infolist():
    name = info.filename
    if name.startswith('assets/dexopt/'):
        continue  # baseline profile cua dex cu (checksum khong con khop)
    data = zin.read(name)
    if name == 'classes.dex':
        data = open(dex, 'rb').read()
    if name == 'AndroidManifest.xml' and rename:
        data, ch = axml_rename.rewrite(data, RENAME)
        for a, b2 in ch: print('rename', repr(a), '->', repr(b2))
    if name == 'AndroidManifest.xml' and newcode:
        data = patch_manifest(data, newcode)
    zi = zipfile.ZipInfo(name, date_time=info.date_time)
    zi.compress_type = info.compress_type
    zi.external_attr = info.external_attr
    zi.create_system = info.create_system
    zi.extra = b''
    if zi.compress_type == zipfile.ZIP_STORED:
        align = 16384 if name.endswith('.so') else 4
        base = zout.fp.tell() + 30 + len(name.encode('utf-8'))
        L = 6 + ((-(base + 6)) % align)
        zi.extra = struct.pack('<HHH', 0xD935, L - 4, align) + b'\0' * (L - 6)
    zout.writestr(zi, data, compress_type=zi.compress_type, compresslevel=9 if zi.compress_type else None)
zout.close()
# check alignment
z = zipfile.ZipFile(out)
bad = 0
with open(out, 'rb') as f:
    for i in z.infolist():
        if i.compress_type == 0:
            f.seek(i.header_offset); h = f.read(30)
            n, e = struct.unpack_from('<HH', h, 26)
            if (i.header_offset + 30 + n + e) % 4: bad += 1
print('entries', len(z.infolist()), 'misaligned', bad)
