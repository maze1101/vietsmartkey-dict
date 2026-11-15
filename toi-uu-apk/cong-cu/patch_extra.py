# Phần vá thêm cho bản 0.57-fast; được patch.py exec() cuối cùng (dùng chung read/write/log/F/re).

# 7) Nhấn giữ phím chữ để gõ số / ký hiệu (kiểu Laban Key)
s = read('KeyboardView')
m = re.search(r'^\.method public onTouchEvent\(Landroid/view/MotionEvent;\)Z$', s, re.M)
end = s.index('\n.end method', m.end())
body = s[m.end():end]
tail = re.search(r'(\n\s+:cond_\w+\n\s+invoke-virtual \{p0\}, Landroid/view/View;->invalidate\(\)V\n\s+return v4\s*)$', body)
if not tail or 'hold:Ljava/lang/Runnable;' not in body[tail.start() - 400:tail.start()]:
    raise SystemExit('onTouchEvent DOWN tail not found')
seg = tail.group(1)
lbl = re.search(r':cond_\w+', seg).group(0)
body = body[:tail.start()] + seg.replace(lbl, lbl + '\n    invoke-static {p0}, %s->onKeyDown(Lvn/personal/vietkey/KeyboardView;)V' % F, 1)
s = s[:m.end()] + body + s[end:]

m = re.search(r'^\.method public onDraw\(Landroid/graphics/Canvas;\)V$', s, re.M)
end = s.index('\n.end method', m.end())
body = s[m.end():end]
mm = re.search(r'\n(\s+:cond_\w+)\n(\s+iget-boolean v2, v0, Lvn/personal/vietkey/KeyboardView;->popup:Z)', body)
if not mm or body.count('->popup:Z') != 1:
    raise SystemExit('onDraw popup point not found')
body = (body[:mm.start()] + '\n' + mm.group(1) + '\n    move-object/from16 v2, p1\n'
        + '    invoke-static {v0, v2}, %s->drawHints(Lvn/personal/vietkey/KeyboardView;Landroid/graphics/Canvas;)V\n' % F
        + mm.group(2) + body[mm.end():])
s = s[:m.end()] + body + s[end:]
write('KeyboardView', s)
log.append('long-press symbols + hints')

# Công tắc trong Cài đặt › Vuốt & chạm (ngay sau "Vuốt để gõ")
s = read('SettingsActivity')
call = ('    invoke-virtual {v0, v1, v2, v3, v11}, Lvn/personal/vietkey/SettingsActivity;->toggle'
        '(Landroid/widget/LinearLayout;Ljava/lang/String;Ljava/lang/String;Z)V\n')
k = s.index('const-string v3, "swipeTyping"')
j = s.index(call, k)
if j - k > 200:
    raise SystemExit('swipeTyping toggle call not adjacent')
label = 'Nh\\u1ea5n gi\\u1eef ph\\u00edm ch\\u1eef \\u0111\\u1ec3 g\\u00f5 s\\u1ed1 & k\\u00fd hi\\u1ec7u (nh\\u01b0 Laban Key)'
ins = call + '\n    const-string v2, "%s"\n\n    const-string v3, "longPressSymbols"\n\n' % label + call
s = s[:j] + ins + s[j + len(call):]
write('SettingsActivity', s)
log.append('settings toggle longPressSymbols')

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
