import os,re,sys
root=sys.argv[1]
need=set()
pat=re.compile(r'new-instance (\w+), (L[^;]+;)')
for dp,_,fs in os.walk(root):
    for f in fs:
        p=os.path.join(dp,f); s=open(p,encoding='utf-8').read(); lines=s.split('\n'); ch=False
        last={}
        for i,l in enumerate(lines):
            m=pat.search(l)
            if m: last[m.group(1)]=(m.group(2),i); continue
            m2=re.search(r'invoke-direct \{(\w+)\}, Ljava/lang/Object;-><init>\(\)V',l)
            if m2 and m2.group(1) in last:
                cls,li=last.pop(m2.group(1))
                if cls!='Ljava/lang/Object;' and i-li<12:
                    lines[i]=l.replace('Ljava/lang/Object;-><init>',cls+'-><init>'); need.add(cls); ch=True
        if ch: open(p,'w',encoding='utf-8').write('\n'.join(lines))
for cls in need:
    p=os.path.join(root,cls[1:-1]+'.smali')
    if not os.path.exists(p): print('missing',cls); continue
    s=open(p,encoding='utf-8').read()
    if '.method public constructor <init>()V' in s or 'constructor <init>()V' in s: continue
    sup=re.search(r'\.super (L[^;]+;)',s).group(1)
    s+='\n.method public constructor <init>()V\n    .registers 1\n    invoke-direct {p0}, %s-><init>()V\n    return-void\n.end method\n'%sup
    open(p,'w',encoding='utf-8').write(s)
print('patched',len(need))
