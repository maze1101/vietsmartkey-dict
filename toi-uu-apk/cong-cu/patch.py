"""Vá smali của VietSmartKey 0.56: chuyển các điểm nóng sang lớp Fast. Chạy: python3 -I patch.py <smali_dir>"""
import os, re, sys

root = sys.argv[1]
PKG = os.path.join(root, 'vn', 'personal', 'vietkey')
F = 'Lvn/personal/vietkey/Fast;'
log = []


def path(name):
    return os.path.join(PKG, name + '.smali')


def read(name):
    return open(path(name), encoding='utf-8').read()


def write(name, s):
    open(path(name), 'w', encoding='utf-8').write(s)


def must_replace(s, old, new, count=1, what=''):
    n = s.count(old)
    if n != count:
        raise SystemExit('pattern count %d != %d for %s: %r' % (n, count, what, old[:120]))
    log.append(what)
    return s.replace(old, new)


def regs(params):
    n = 0
    for p in params:
        n += 2 if p in ('J', 'D') else 1
    return n


def split_sig(sig):
    out, i = [], 0
    while i < len(sig):
        c = sig[i]
        if c == 'L':
            j = sig.index(';', i)
            out.append(sig[i:j + 1]); i = j + 1
        elif c == '[':
            j = i
            while sig[j] == '[':
                j += 1
            if sig[j] == 'L':
                j = sig.index(';', j)
            out.append(sig[i:j + 1]); i = j + 1
        else:
            out.append(c); i += 1
    return out


def delegate(cls, name, sig, ret, static, fast_name):
    """Đổi tên method gốc thành <name>Orig và thêm method mới gọi Fast.<fast_name>."""
    owner = 'Lvn/personal/vietkey/%s;' % cls
    s = read(cls)
    pat = re.compile(r'^(\.method [^\n]*?)\b%s\(%s\)%s$' % (re.escape(name), re.escape(sig), re.escape(ret)), re.M)
    ms = list(pat.finditer(s))
    if len(ms) != 1:
        raise SystemExit('method %s.%s%s found %d' % (cls, name, sig, len(ms)))
    header = ms[0].group(1)
    s = s[:ms[0].start()] + header + name + 'Orig(' + sig + ')' + ret + s[ms[0].end():]
    params = split_sig(sig)
    fparams = ([] if static else [owner]) + params
    np = regs(params) + (0 if static else 1)
    locals_ = 2 if ret in ('J', 'D') else (0 if ret == 'V' else 1)
    mods = '.method public static ' if static else '.method public '
    body = [mods + name + '(' + sig + ')' + ret, '    .registers %d' % (np + locals_)]
    if np == 0:
        body.append('    invoke-static {}, %s->%s(%s)%s' % (F, fast_name, ''.join(fparams), ret))
    else:
        body.append('    invoke-static/range {p0 .. p%d}, %s->%s(%s)%s' % (np - 1, F, fast_name, ''.join(fparams), ret))
    if ret == 'V':
        body.append('    return-void')
    elif ret in ('J', 'D'):
        body += ['    move-result-wide v0', '    return-wide v0']
    elif ret.startswith('L') or ret.startswith('['):
        body += ['    move-result-object v0', '    return-object v0']
    else:
        body += ['    move-result v0', '    return v0']
    body.append('.end method')
    s = s.rstrip('\n') + '\n\n' + '\n'.join(body) + '\n'
    write(cls, s)
    log.append('delegate %s.%s -> Fast.%s' % (cls, name, fast_name))


def inject(cls, method_header_re, call, at_start=True, at_returns=False):
    s = read(cls)
    m = re.search(r'^\.method [^\n]*%s[^\n]*$' % method_header_re, s, re.M)
    if not m:
        raise SystemExit('inject: method not found ' + method_header_re)
    end = s.index('\n.end method', m.end())
    body = s[m.end():end]
    lines = body.split('\n')
    if at_returns:
        lines = [('    ' + call + '\n' + l) if re.match(r'\s+return-void\s*$', l) else l for l in lines]
    if at_start:
        # sau .registers và mọi khối .annotation/.param ngay sau nó
        i = 0
        while not lines[i].strip().startswith('.registers'):
            i += 1
        i += 1
        while i < len(lines):
            t = lines[i].strip()
            if t.startswith('.annotation') or t.startswith('.param'):
                endtag = '.end annotation' if t.startswith('.annotation') else '.end param'
                if t.startswith('.param') and not any(x.strip() == '.end param' for x in lines[i + 1:i + 3]):
                    i += 1; continue
                while lines[i].strip() != endtag:
                    i += 1
                i += 1
            elif t == '':
                i += 1
            else:
                break
        lines.insert(i, '    ' + call)
    s = s[:m.end()] + '\n'.join(lines) + s[end:]
    write(cls, s)
    log.append('inject %s %s' % (cls, call))


# 1) regex: String.matches / replaceAll / split -> Fast (Pattern biên dịch sẵn)
REGEX = [
    (r'invoke-virtual \{(\w+), (\w+)\}, Ljava/lang/String;->matches\(Ljava/lang/String;\)Z',
     r'invoke-static {\1, \2}, %s->matches(Ljava/lang/String;Ljava/lang/String;)Z' % F),
    (r'invoke-virtual \{(\w+), (\w+), (\w+)\}, Ljava/lang/String;->replaceAll\(Ljava/lang/String;Ljava/lang/String;\)Ljava/lang/String;',
     r'invoke-static {\1, \2, \3}, %s->replaceAll(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;' % F),
    (r'invoke-virtual \{(\w+), (\w+)\}, Ljava/lang/String;->split\(Ljava/lang/String;\)\[Ljava/lang/String;',
     r'invoke-static {\1, \2}, %s->split(Ljava/lang/String;Ljava/lang/String;)[Ljava/lang/String;' % F),
    (r'invoke-virtual \{(\w+), (\w+), (\w+)\}, Ljava/lang/String;->split\(Ljava/lang/String;I\)\[Ljava/lang/String;',
     r'invoke-static {\1, \2, \3}, %s->split(Ljava/lang/String;Ljava/lang/String;I)[Ljava/lang/String;' % F),
]
total = 0
for fn in sorted(os.listdir(PKG)):
    if not fn.endswith('.smali') or fn.startswith('Fast'):
        continue
    p = os.path.join(PKG, fn)
    s = open(p, encoding='utf-8').read()
    o = s
    for a, b in REGEX:
        s, k = re.subn(a, b, s)
        total += k
    left = re.findall(r'Ljava/lang/String;->(matches|replaceAll|split)\(', s)
    if left:
        raise SystemExit('unpatched regex call in %s: %s' % (fn, left))
    if s != o:
        open(p, 'w', encoding='utf-8').write(s)
log.append('regex calls rewritten: %d' % total)

# 2) chuẩn hoá chuỗi có bộ nhớ đệm
delegate('PersonalData', 'fold', 'Ljava/lang/String;', 'Ljava/lang/String;', True, 'fold')
delegate('LearnedModel', 'clean', 'Ljava/lang/String;', 'Ljava/lang/String;', True, 'clean')
# 3) khoảng cách gõ phím không dùng phản xạ
delegate('TypoModel', 'cost', 'Ljava/lang/String;Ljava/lang/String;D', 'D', True, 'cost')
# 4) gợi ý: ghi nhớ + bỏ việc thừa
delegate('PersonalData', 'suggest', 'Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II', 'Ljava/util/List;', False, 'suggest')
delegate('Telex', 'tone', 'Ljava/lang/String;', 'I', True, 'tone')
delegate('Telex', 'bare', 'C', 'C', True, 'bare')
delegate('PersonalData', 'correct', 'Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I', 'Ljava/lang/String;', False, 'correct')
delegate('PersonalData', 'nextKeyOdds', 'Ljava/lang/String;', '[F', False, 'nextKeyOdds')
inject('PersonalData', r'loadMacros\(Ljava/io/Reader;\)V', 'invoke-static {}, %s->macrosChanged()V' % F, True, True)
# 5) dịch vụ bàn phím
s = read('VietKeyboardService')
s = must_replace(s, '.field private final raw:Lvn/personal/vietkey/CompositionBuffer;',
                 '.field final raw:Lvn/personal/vietkey/CompositionBuffer;', 1, 'raw field package-private')
write('VietKeyboardService', s)
s = read('CompositionBuffer')
s = must_replace(s, '.field private seed:Ljava/lang/String;', '.field seed:Ljava/lang/String;', 1, 'seed field package-private')
s = must_replace(s, '.field private final keys:Ljava/lang/StringBuilder;', '.field final keys:Ljava/lang/StringBuilder;', 1, 'keys field package-private')
write('CompositionBuffer', s)
delegate('VietKeyboardService', 'compose', '', 'Ljava/lang/String;', False, 'compose')
delegate('VietKeyboardService', 'previousWord', '', 'Ljava/lang/String;', False, 'previousWord')
delegate('VietKeyboardService', 'refreshSuggestions', '', 'V', False, 'refresh')
delegate('VietKeyboardService', 'reloadActiveMacros', '', 'V', False, 'reloadMacros')

s = read('VietKeyboardService')
m = re.search(r'^\.method public onUpdateSelection\(IIIIII\)V$', s, re.M)
end = s.index('\n.end method', m.end())
body = s[m.end():end]
old_tail = re.search(r'(\n\s+invoke-virtual \{p0\}, Lvn/personal/vietkey/VietKeyboardService;->refreshSuggestions\(\)V'
                     r'(?:\n[^\n]*)*?\n\s+invoke-virtual \{p0\}, Landroid/view/View;->invalidate\(\)V\n)', body)
if not old_tail:
    raise SystemExit('onUpdateSelection tail not found')
seg = old_tail.group(1)
labels = re.findall(r'^\s+(:\w+)\s*$', seg, re.M)
if labels:
    raise SystemExit('unexpected labels inside onUpdateSelection tail: %s' % labels)
body = body.replace(seg, '\n    invoke-static {p0}, %s->postRefresh(Lvn/personal/vietkey/VietKeyboardService;)V\n' % F)
s = s[:m.end()] + body + s[end:]
log.append('onUpdateSelection: coalesced refresh')

# lưu mô hình tự học: 1s -> 3s sau lần gõ cuối (vẫn lưu ngay khi rời ô nhập)
n = 0
lines = s.split('\n')
for i, l in enumerate(lines):
    mm = re.match(r'(\s+const-wide/16 (\w+), )0x3e8$', l)
    if mm:
        nxt = '\n'.join(lines[i:i + 8])
        if 'persistLearning' not in '\n'.join(lines[i - 12:i + 8]) or 'postDelayed' not in nxt:
            raise SystemExit('unexpected 0x3e8 at line %d' % i)
        lines[i] = mm.group(1) + '0xbb8'
        n += 1
if n != 5:
    raise SystemExit('persist delay count %d' % n)
s = '\n'.join(lines)
write('VietKeyboardService', s)
log.append('persist delay 1000->3000 (%d)' % n)

# 6) KeyboardView: bộ nhớ đệm màu chủ đề
delegate('KeyboardView', 'themeColor', 'Ljava/lang/String;I', 'I', False, 'themeColor')
inject('KeyboardView', r' refresh\(\)V', 'invoke-static {}, %s->themeChanged()V' % F, True, True)

s = read('KeyboardView')
s, k = re.subn(r'invoke-static \{(\w+), (\w+), (\w+), (\w+)\}, Landroid/text/TextUtils;->ellipsize\(Ljava/lang/CharSequence;Landroid/text/TextPaint;FLandroid/text/TextUtils\$TruncateAt;\)Ljava/lang/CharSequence;',
               r'invoke-static {\1, \2, \3, \4}, %s->ellipsize(Ljava/lang/CharSequence;Landroid/text/TextPaint;FLandroid/text/TextUtils$TruncateAt;)Ljava/lang/CharSequence;' % F, s)
if k != 2:
    raise SystemExit('ellipsize count %d' % k)
write('KeyboardView', s)
log.append('ellipsize cached (%d)' % k)

print('\n'.join(log))
