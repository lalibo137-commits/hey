import numpy as np, subprocess, sys
W,H=1280,720; SRC_FPS=120; OUT_FPS=60
P=0.6667; PH=0.33           # kick grid in output/audio time (audio starts at 1:03)
SRC0=0.0                    # source time = original video time
# anchor cuts (source time in original video) -> beats per segment; each cut lands on a kick
anchors=[0.33,0.81,1.58,3.125,4.625,5.083,5.79,7.29,8.25,9.25,12.67,13.25,14.29,14.71,15.375,16.71,18.54,20.83,22.42,24.0,25.54,26.875,28.46,29.95]
beats  =[1,   1,   2,   2,    1,    1,    2,   1,   1,   5,    1,    1,    1,    1,     2,     2,    3,    2,    2,    2,    2,    2,    2]
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
# 120fps interpolated source in two parts: 0-4.925s (head) and 5.0s-end (tail)
HEAD_N=591; TAIL_T0=5.0
dec=subprocess.Popen(['ffmpeg','-v','error','-i','interp_head.mp4','-i','interp120.mp4','-filter_complex','[0:v][1:v]concat=n=2:v=1[v]','-map','[v]','-f','rawvideo','-pix_fmt','rgb24','-'],stdout=subprocess.PIPE)
def fidx(st):
    return st*SRC_FPS if st*SRC_FPS<HEAD_N-1 else HEAD_N+(st-TAIL_T0)*SRC_FPS
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
    f0=fidx(s_open); f1=fidx(s)
    idx=np.arange(int(np.floor(f0)),int(np.floor(f1))+1)
    if len(idx)<=1:
        i0=int(np.floor(f1)); w=f1-i0   # sub-frame linear blend
        img=frame(i0).astype(np.float32)*(1-w)+frame(i0+1).astype(np.float32)*w
    else:
        img=np.mean([frame(i).astype(np.float32) for i in idx],axis=0)
    enc.stdin.write(np.clip(img+0.5,0,255).astype(np.uint8).tobytes())
enc.stdin.close(); enc.wait(); dec.kill()
print(T_END)
