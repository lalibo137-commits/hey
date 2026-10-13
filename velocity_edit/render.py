# TikTok-style velocity edit: slow-mo base, speed whips up and peaks ON each hard kick, eases back to slow.
import numpy as np, subprocess, sys, cv2
W,H=1280,720; SRC_FPS=120; OUT_FPS=60
T_END=31.6                      # output length (audio 1:03 -> 1:34.6)
SRC_END=29.95                   # last usable source time
# hard 808 kicks (seconds after 1:03), from low-band attack strength on the 8th-note grid (90 BPM, phase 0.114)
grid=np.load('hits_all.npy')
cand=sorted([(a,t) for t,a in grid if a>=2.5 and t<T_END-0.6],reverse=True)
hits=[]
for a,t in cand:                # strongest first, keep >=0.6s apart
    if all(abs(t-h)>=0.6 for h in hits): hits.append(t)
hits=np.array(sorted(hits))
# source cuts (original video time) to snap onto kicks
cuts=[0.835,1.58,3.125,4.625,5.083,5.79,7.335,8.295,9.295,12.71,13.295,14.29,14.755,15.375,16.71,18.585,20.88,22.42,24.0,25.585,26.875,28.46]
BASE=0.3; AMP=3.2; RISE=0.08; FALL=0.17
def vnat(t):
    d=t[:,None]-hits[None,:]
    return BASE+AMP*np.where(d<0,np.exp(-(d/RISE)**2),np.exp(-(d/FALL)**2)).sum(1)
tt=np.arange(int(T_END*OUT_FPS)+1)/OUT_FPS
N=np.concatenate([[0],np.cumsum(vnat(tt[:-1]+0.5/OUT_FPS))/OUT_FPS])
S=SRC_END/N[-1]
Nh=np.interp(hits,tt,N)*S
anch=[(0.0,0.0)]; used=-1
for c in cuts:                  # snap each cut to the nearest unused later kick
    j=used+1+int(np.argmin(np.abs(Nh[used+1:]-c))) if used+1<len(hits) else None
    if j is None: break
    # accept only if the speed multiplier since the previous anchor stays near S (keeps slow-mo consistent)
    pa,ps=anch[-1]; m=(c-ps)/(Nh[j]/S-np.interp(pa,tt,N))/S
    if abs(Nh[j]-c)<0.9 and 0.7<m<1.45: anch.append((hits[j],c)); used=j
anch.append((T_END,SRC_END))
at=np.array([a for a,_ in anch]); asrc=np.array([s for _,s in anch])
aN=np.interp(at,tt,N)
def src_at(t):
    n=np.interp(t,tt,N); i=min(np.searchsorted(at,t,side='right')-1,len(at)-2)
    return asrc[i]+(asrc[i+1]-asrc[i])*(n-aN[i])/(aN[i+1]-aN[i])
print("S",round(S,2),"base x",round(BASE*S,2),"peak x",round((BASE+AMP)*S,2),file=sys.stderr)
print("hits",len(hits),np.round(hits,2).tolist(),file=sys.stderr)
print("anchors",[(round(a,2),s) for a,s in anch],file=sys.stderr)
print("seg speed mult",np.round(np.diff(asrc)/np.diff(aN),2).tolist(),file=sys.stderr)
HEAD_N=591; TAIL_T0=5.0
def fidx(st): return st*SRC_FPS if st*SRC_FPS<HEAD_N-1 else HEAD_N+(st-TAIL_T0)*SRC_FPS
dec=subprocess.Popen(['ffmpeg','-v','error','-i','interp_head.mp4','-i','interp120.mp4','-filter_complex','[0:v][1:v]concat=n=2:v=1[v]','-map','[v]','-f','rawvideo','-pix_fmt','rgb24','-'],stdout=subprocess.PIPE)
enc=subprocess.Popen(['ffmpeg','-v','error','-y','-f','rawvideo','-pix_fmt','rgb24','-s',f'{W}x{H}','-r',str(OUT_FPS),'-i','-',
     '-c:v','libx264','-preset','medium','-crf','14','-pix_fmt','yuv420p','vel2_raw.mp4'],stdin=subprocess.PIPE)
FS=W*H*3; buf={}; nxt=0
def frame(i):
    global nxt
    i=max(0,i)
    while nxt<=i:
        d=dec.stdout.read(FS)
        if len(d)<FS: break
        buf[nxt]=np.frombuffer(d,np.uint8).reshape(H,W,3); nxt+=1
    for k in [k for k in buf if k<i-60]: del buf[k]
    return buf[min(i,nxt-1)]
# all shot boundaries (incl. ones not snapped) in interpolated-frame index space; blur never crosses them
ALLCUTS=sorted(set(cuts+[2.835,7.835,13.795,18.17,19.0,19.5,24.5]))
CI=np.array([np.ceil(fidx(c)) for c in ALLCUTS])
prev=src_at(0.0)
for o in range(int(T_END*OUT_FPS)):
    t=o/OUT_FPS; s=src_at(t)
    s_open=s-0.5*max(0.0,s-prev); prev=s          # 180-degree shutter motion blur
    f0=fidx(s_open); f1=fidx(s)
    lo_i=int(np.floor(f0)); hi_i=int(np.floor(f1))
    before=CI[CI<=f1]
    if len(before): lo_i=max(lo_i,int(before[-1]))
    nxt_c=CI[CI>f1]
    idx=np.arange(lo_i,hi_i+1)
    if len(idx)<=1:
        i0=max(int(np.floor(f1)),lo_i); w=f1-i0
        if len(nxt_c) and i0+1>=nxt_c[0]: w=0.0
        img=frame(i0) if w==0 else cv2.addWeighted(frame(i0),1-w,frame(i0+1),w,0)
    else:
        img=np.clip(np.mean([frame(i).astype(np.float32) for i in idx],axis=0)+0.5,0,255).astype(np.uint8)
    # zoom punch on each kick
    d=t-hits; d=d[d>=0]
    z=1+0.09*np.exp(-d.min()/0.14) if len(d) else 1.0
    if z>1.001:
        cw,ch=W/z,H/z; x0,y0=(W-cw)/2,(H-ch)/2
        M=np.float32([[z,0,-x0*z],[0,z,-y0*z]])
        img=cv2.warpAffine(img,M,(W,H),flags=cv2.INTER_CUBIC)
    enc.stdin.write(img.tobytes())
enc.stdin.close(); enc.wait(); dec.kill()
