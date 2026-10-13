import numpy as np, subprocess, sys
W,H=1280,720; SRC_FPS=120; OUT_FPS=60
P=0.6667; PH=0.33           # kick grid in output/audio time (audio starts at 1:03)
SRC0=0.083                  # skip 2-frame flash at start of source (rel. to 0:05)
# anchor cuts (source time rel. to 0:05) -> beats per segment; each cut lands on a kick
anchors=[0.41,0.79,2.29,3.25,4.25,7.67,8.25,9.29,9.71,10.375,11.71,13.54,15.83,17.42,19.0,20.54,21.875,23.46,24.95]
beats  =[1,   2,   1,   1,   5,   1,   1,   1,   1,    2,     2,    3,    2,    2,    2,    2,    2,    2]
SMIN=0.2; K=(1-SMIN)*3.2; EXP=2.2
def G(u): return SMIN*u + K*u**(EXP+1)/(EXP+1)   # integral of speed shape, G(1)=1
# build (out_t, src_t) mapping function
segs=[]  # (out_start, out_end, src_start, src_end, ramp)
t=PH
for i,n in enumerate(beats):
    c=(anchors[i+1]-anchors[i])/n
    last = i==len(beats)-1
    for b in range(n):
        segs.append((t,t+P,anchors[i]+b*c,anchors[i]+(b+1)*c, not last))
        t+=P
T_END=t
def src_at(ot):
    if ot<PH: return SRC0+(anchors[0]-SRC0)*ot/PH
    for a,b,s0,s1,r in segs:
        if ot<b:
            u=(ot-a)/P
            return s0+(s1-s0)*(G(u) if r else u)
    return anchors[-1]
nout=int(round(T_END*OUT_FPS))
print("output dur",T_END,"frames",nout,file=sys.stderr)
dec=subprocess.Popen(['ffmpeg','-v','error','-i','interp120.mp4','-f','rawvideo','-pix_fmt','rgb24','-'],stdout=subprocess.PIPE)
FS=W*H*3
enc=subprocess.Popen(['ffmpeg','-v','error','-y','-f','rawvideo','-pix_fmt','rgb24','-s',f'{W}x{H}','-r',str(OUT_FPS),'-i','-',
     '-c:v','libx264','-preset','medium','-crf','14','-pix_fmt','yuv420p','vel_raw.mp4'],stdin=subprocess.PIPE)
buf={}; nxt=0
def frame(i):
    global nxt
    while nxt<=i:
        d=dec.stdout.read(FS)
        if len(d)<FS: break
        buf[nxt]=np.frombuffer(d,np.uint8).reshape(H,W,3); nxt+=1
    for k in [k for k in buf if k<i-40]: del buf[k]
    return buf[min(i,nxt-1)]
prev=src_at(0)
for o in range(nout):
    ot=o/OUT_FPS; s=src_at(ot)
    # 180-degree shutter: blend source span covered in half a frame interval
    s_open=s-0.5*max(0.0,s-prev); prev=s
    f0=s_open*SRC_FPS; f1=s*SRC_FPS
    idx=np.arange(int(np.floor(f0)),int(np.floor(f1))+1)
    if len(idx)<=1:
        i0=int(np.floor(f1)); w=f1-i0   # sub-frame linear blend
        img=frame(i0).astype(np.float32)*(1-w)+frame(i0+1).astype(np.float32)*w
    else:
        img=np.mean([frame(i).astype(np.float32) for i in idx],axis=0)
    enc.stdin.write(np.clip(img+0.5,0,255).astype(np.uint8).tobytes())
enc.stdin.close(); enc.wait(); dec.kill()
print(T_END)
