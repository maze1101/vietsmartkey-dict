# Phần vá thêm cho bản 0.57-fast; được patch.py exec() cuối cùng (dùng chung read/write/log/F/re).

# 7) Học thói quen: không tự học chữ gõ nhầm
s = read('ForeignWords')
s = must_replace(s, '.field private final english:Lvn/personal/vietkey/WordFrequencies;',
                 '.field final english:Lvn/personal/vietkey/WordFrequencies;', 1, 'ForeignWords.english package-private')
write('ForeignWords', s)
delegate('VietKeyboardService', 'remember', 'Ljava/lang/String;Ljava/lang/String;', 'V', False, 'remember')

# 9) Giọng nói: ghi chữ Google trả về vào "Nhật ký giọng nói" (công tắc, mặc định tắt)
delegate('VietKeyboardService', 'voiceCommit', 'Ljava/lang/String;', 'V', False, 'voiceCommit')
s = read('SettingsActivity')
call = ('    invoke-virtual {v0, v1, v2, v3, v11}, Lvn/personal/vietkey/SettingsActivity;->toggle'
        '(Landroid/widget/LinearLayout;Ljava/lang/String;Ljava/lang/String;Z)V\n')
k = s.index('const-string v3, "voiceEnabled"')
j = s.index(call, k)
if j - k > 200:
    raise SystemExit('voiceEnabled toggle call not adjacent')
after = s[j + len(call):j + len(call) + 3000]
m4 = re.search(r'\bv4\b', after)
if not m4 or not re.match(r'\s*move-result-object v4', after[after.rfind('\n', 0, m4.start()) + 1:]):
    raise SystemExit('v4 not free after voiceEnabled toggle')
label = 'Ghi ch\\u1eef Google nh\\u1eadn \\u0111\\u01b0\\u1ee3c v\\u00e0o nh\\u1eadt k\\u00fd (\\u0111\\u1ec3 g\\u1eedi g\\u00f3p \\u00fd d\\u1ea5u c\\u00e2u)'
ins = call + ('\n    const-string v2, "%s"\n\n    const-string v3, "voiceLogText"\n\n    const/4 v4, 0x0\n\n'
              '    invoke-virtual {v0, v1, v2, v3, v4}, Lvn/personal/vietkey/SettingsActivity;->toggle'
              '(Landroid/widget/LinearLayout;Ljava/lang/String;Ljava/lang/String;Z)V\n') % label
s = s[:j] + ins + s[j + len(call):]
write('SettingsActivity', s)
log.append('settings toggle voiceLogText')

# 10) Phím cách: chạm mép dưới z..m khi từ đã trọn -> phím cách, có tự học
delegate('KeyboardView', 'hit', 'FF', 'Lvn/personal/vietkey/KeyboardView$Key;', False, 'hit')
delegate('KeyboardView', 'learnLetterMiss', '', 'V', False, 'learnLetterMiss')
delegate('KeyboardView', 'learnSpaceMiss', '', 'V', False, 'learnSpaceMiss')

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
