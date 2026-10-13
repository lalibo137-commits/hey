# TikTok velocity edit v4: a cut on (almost) every hard kick, slow-mo between, speed whip peaking on the kick,
# plus flash / RGB split / shake / zoom punch on each hit.
import numpy as np, subprocess, sys, cv2
W,H=1280,720; SRC_FPS=120; OUT_FPS=60
T_END=31.6
grid=np.load('hits_all.npy')
cand=sorted([(a,t) for t,a in grid if a>=2.5 and t<T_END-0.6],reverse=True)
hits=[]
for a,t in cand:
    if all(abs(t-h)>=0.6 for h in hits): hits.append(t)
hits=sorted(hits); HARD=set(hits)
# fill long gaps with snare hits (backbeat: 0.448 + 1.334k) so slow-mo never drags
snares=[0.448+1.334*k for k in range(30) if 0.448+1.334*k<T_END-0.6]
for _ in range(3):
    for sn in snares:
        hs=sorted(hits); i=np.searchsorted(hs,sn)
        prv=hs[i-1] if i>0 else 0.0; nx=hs[i] if i<len(hs) else T_END
        if nx-prv>1.3 and sn-prv>=0.6 and nx-sn>=0.6: hits.append(sn)
hits=np.array(sorted(hits)); STR=np.array([1.0 if h in HARD else 0.6 for h in hits])
# shots in original video time (transition frames trimmed); text card 22.42-24.0 dropped
SH=[(0.0,0.81),(0.84,1.56),(1.6,3.1),(3.14,4.6),(4.64,5.06),(5.1,5.77),(5.8,7.25),(7.34,7.81),(7.84,8.27),
    (8.3,9.27),(9.3,10.02),(10.06,11.56),(11.6,12.69),(12.72,13.27),(13.3,13.77),(13.8,14.27),(14.3,14.73),
    (14.76,15.35),(15.38,16.69),(16.72,18.56),(18.59,19.48),(19.52,20.86),(20.89,22.4),(24.02,25.56),
    (25.59,26.85),(26.88,28.44),(28.47,29.95)]
BASE=0.22; AMP=3.1; RISE=0.08; FALL=0.12
def vnat(t):
    d=np.atleast_1d(t)[:,None]-hits[None,:]
    return BASE+AMP*(STR[None,:]*np.where(d<0,np.exp(-(d/RISE)**2),np.exp(-(d/FALL)**2))).sum(1)
tt=np.arange(int(T_END*OUT_FPS)+2)/OUT_FPS
N=np.concatenate([[0],np.cumsum(vnat(tt[:-1]+0.5/OUT_FPS))/OUT_FPS])
def Nat(t): return np.interp(t,tt,N)
# slots: [0,h0], [h0,h1], ..., [h_last,T_END]; first shot -> first slot, last shot -> last slot
B=np.concatenate([[0],hits,[T_END]]); nslot=len(B)-1
mid=SH[1:-1]; ns=nslot-2
# DP: assign middle shots in order to consecutive groups of 1-3 slots; cost = mismatch of shot length vs natural consumption
INF=1e18; dp=np.full((len(mid)+1,ns+1),INF); bk={}; dp[0,0]=0
for i in range(len(mid)):
    L=mid[i][1]-mid[i][0]
    for j in range(ns+1):
        if dp[i,j]>=INF: continue
        for k in (1,2,3):
            if j+k>ns: break
            n=Nat(B[1+j+k])-Nat(B[1+j]); m=L/n
            c=(np.log(min(m,1.0))**2)*3 if m<1 else 0.15*np.log(m)   # slowing a shot down hurts more than trimming it
            if dp[i,j]+c<dp[i+1,j+k]: dp[i+1,j+k]=dp[i,j]+c; bk[(i+1,j+k)]=k
groups=[]; j=ns
for i in range(len(mid),0,-1): k=bk[(i,j)]; groups.append(k); j-=k
groups=groups[::-1]
plan=[(B[0],B[1],SH[0])]; s=1
for g,sh in zip(groups,mid): plan.append((B[s],B[s+g],sh)); s+=g
plan.append((B[-2],B[-1],SH[-1]))
# per shot: source window; trim long shots (keep the end, where the next whip lands), slow short ones
segs=[]
for a,b,(s0,s1) in plan:
    n=Nat(b)-Nat(a); L=s1-s0; m=L/n
    if m>1.25: s0=s1-1.25*n                     # cap speed multiplier; drop the shot's head
    segs.append((a,b,s0,s1))
    print(f"out {a:5.2f}-{b:5.2f}  src {s0:5.2f}-{s1:5.2f}  mult {(s1-s0)/n:4.2f}",file=sys.stderr)
SA=np.array([x[0] for x in segs])
def src_at(t):
    i=min(max(np.searchsorted(SA,t,side='right')-1,0),len(segs)-1); a,b,s0,s1=segs[i]
    return i, s0+(s1-s0)*(Nat(t)-Nat(a))/(Nat(b)-Nat(a))
HEAD_N=591; TAIL_T0=5.0
def fidx(st): return st*SRC_FPS if st*SRC_FPS<HEAD_N-1 else HEAD_N+(st-TAIL_T0)*SRC_FPS
# random-access frame cache: decode interpolated footage once into a memory-mapped file
TOT=HEAD_N+2996
mm=np.lib.format.open_memmap('frames.npy',mode='w+',dtype=np.uint8,shape=(TOT,H//2*2,W,3)) if '--decode' in sys.argv else np.load('frames.npy',mmap_mode='r')
if '--decode' in sys.argv:
    dec=subprocess.Popen(['ffmpeg','-v','error','-i','interp_head.mp4','-i','interp120.mp4','-filter_complex','[0:v][1:v]concat=n=2:v=1[v]','-map','[v]','-f','rawvideo','-pix_fmt','rgb24','-'],stdout=subprocess.PIPE)
    for k in range(TOT):
        d=dec.stdout.read(W*H*3)
        if len(d)<W*H*3: break
        mm[k]=np.frombuffer(d,np.uint8).reshape(H,W,3)
    mm.flush(); print("decoded",k,file=sys.stderr); sys.exit()
def frame(i): return mm[int(np.clip(i,0,TOT-1))]
enc=subprocess.Popen(['ffmpeg','-v','error','-y','-f','rawvideo','-pix_fmt','rgb24','-s',f'{W}x{H}','-r',str(OUT_FPS),'-i','-',
     '-c:v','libx264','-preset','medium','-crf','14','-pix_fmt','yuv420p','vel3_raw.mp4'],stdin=subprocess.PIPE)
rng=np.random.default_rng(7)
for o in range(int(T_END*OUT_FPS)):
    t=o/OUT_FPS; seg,s=src_at(t)
    _,sp=src_at(max(0,t-1/OUT_FPS))
    s_open=s-0.6*max(0.0,s-sp) if src_at(max(0,t-1/OUT_FPS))[0]==seg else s   # motion blur, never across a cut
    s_open=max(s_open,segs[seg][2])
    f0=fidx(s_open); f1=fidx(s)
    lo,hi=int(np.floor(f0)),int(np.floor(f1))
    if hi<=lo:
        w=f1-hi; img=cv2.addWeighted(np.asarray(frame(hi)),1-w,np.asarray(frame(min(hi+1,int(fidx(segs[seg][3]))))),w,0)
    else:
        img=np.clip(np.mean([frame(i).astype(np.float32) for i in range(lo,hi+1)],axis=0)+0.5,0,255).astype(np.uint8)
    d=t-hits; ok=d>=0; dt=d[ok].min() if ok.any() else 9.0; st=STR[ok][np.argmin(d[ok])] if ok.any() else 0
    pre=hits-t; pre=pre[pre>0]; dp_=pre.min() if len(pre) else 9.0
    # zoom: builds slightly into the hit, punches on it, settles
    z=1+0.13*st*np.exp(-dt/0.17)+0.05*np.exp(-(dp_/0.12)**2)
    sx=11*st*np.exp(-dt/0.09)*np.sin(dt*2*np.pi*22); sy=8*st*np.exp(-dt/0.09)*np.cos(dt*2*np.pi*17)
    M=np.float32([[z,0,(1-z)*W/2+sx],[0,z,(1-z)*H/2+sy]])
    img=cv2.warpAffine(img,M,(W,H),flags=cv2.INTER_CUBIC,borderMode=cv2.BORDER_REFLECT)
    k=int(round(12*st*np.exp(-dt/0.11)))
    if k>0:                                   # RGB split
        r=np.roll(img[:,:,0],k,axis=1); b=np.roll(img[:,:,2],-k,axis=1)
        img=np.dstack([r,img[:,:,1],b])
    fl=0.55*st*st*np.exp(-dt/0.05)                  # flash
    if fl>0.01: img=cv2.addWeighted(img,1+fl,np.full_like(img,255),fl*0.35,0)
    enc.stdin.write(np.ascontiguousarray(img).tobytes())
enc.stdin.close(); enc.wait()
