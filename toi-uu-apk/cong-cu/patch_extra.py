# Phần vá thêm cho bản 0.57-fast; được patch.py exec() cuối cùng (dùng chung read/write/log/F/re).

# 7) Học thói quen: không tự học chữ gõ nhầm
s = read('ForeignWords')
s = must_replace(s, '.field private final english:Lvn/personal/vietkey/WordFrequencies;',
                 '.field final english:Lvn/personal/vietkey/WordFrequencies;', 1, 'ForeignWords.english package-private')
write('ForeignWords', s)
delegate('VietKeyboardService', 'remember', 'Ljava/lang/String;Ljava/lang/String;', 'V', False, 'remember')

# 8) Bỏ các hàm gốc không còn ai gọi
for cls, sig in [('PersonalData', 'suggestOrig(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II)Ljava/util/List;'),
                 ('PersonalData', 'nextKeyOddsOrig(Ljava/lang/String;)[F'),
                 ('TypoModel', 'costOrig(Ljava/lang/String;Ljava/lang/String;D)D')]:
    s = read(cls)
    name = sig.split('(')[0]
    for fn in os.listdir(PKG):
        if fn.endswith('.smali') and ('->' + name + '(') in open(os.path.join(PKG, fn), encoding='utf-8').read():
            raise SystemExit('still referenced: %s in %s' % (name, fn))
    m = re.search(r'^\.method [^\n]*' + re.escape(sig) + '$', s, re.M)
    e = s.index('.end method', m.end()) + len('.end method')
    s = s[:m.start()] + s[e:]
    write(cls, s)
    log.append('removed unused %s.%s' % (cls, name))
