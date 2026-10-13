# Render Patty Cake x Patrick's car velocity edit from plan.py + interpolated clips (clip_i.mp4 @100fps)
import numpy as np, subprocess, cv2
exec(open('plan.py').read().split('if __name__')[0])
W,H=1280,960; CF=100
starts={}
for l in open('jobs.txt'):
    i,s,d=l.split(); starts[int(i)]=float(s)
enc=subprocess.Popen(['ffmpeg','-v','error','-y','-f','rawvideo','-pix_fmt','rgb24','-s',f'{W}x{H}','-r',str(OUT_FPS),'-i','-',
     '-c:v','libx264','-preset','medium','-crf','14','-pix_fmt','yuv420p','sb_raw.mp4'],stdin=subprocess.PIPE)
class Clip:
    def __init__(s,i):
        s.p=subprocess.Popen(['ffmpeg','-v','error','-i',f'clip_{i}.mp4','-f','rawvideo','-pix_fmt','rgb24','-'],stdout=subprocess.PIPE)
        s.buf={}; s.n=0; s.t0=starts[i]
    def get(s,k):
        k=max(0,int(k))
        while s.n<=k:
            d=s.p.stdout.read(W*H*3)
            if len(d)<W*H*3: break
            s.buf[s.n]=np.frombuffer(d,np.uint8).reshape(H,W,3); s.n+=1
        for j in [j for j in s.buf if j<k-40]: del s.buf[j]
        return s.buf[min(k,s.n-1)]
SA=np.array([x[0] for x in SEGS]); cur=-1; clip=None; prev=None
for o in range(int(T_END*OUT_FPS)):
    t=o/OUT_FPS; i=min(max(np.searchsorted(SA,t,side='right')-1,0),len(SEGS)-1)
    a,b,w0,w1,c0,c1=SEGS[i]
    if i!=cur:
        if clip: clip.p.kill()
        clip=Clip(i); cur=i; prev=None
    s=w0+(w1-w0)*(Nat(t)-Nat(a))/(Nat(b)-Nat(a))
    f1=(s-clip.t0)*CF; f0=f1 if prev is None else f1-0.6*max(0,f1-prev); prev=f1
    lo,hi=int(np.floor(f0)),int(np.floor(f1))
    if hi<=lo:
        w=f1-hi; img=cv2.addWeighted(clip.get(hi),1-w,clip.get(hi+1),w,0)
    else:
        img=np.clip(np.mean([clip.get(k).astype(np.float32) for k in range(lo,hi+1)],axis=0)+0.5,0,255).astype(np.uint8)
    d=t-hits; ok=d>=0
    if ok.any():
        j=np.argmin(np.where(ok,d,9)); dt=d[j]; st=STR[j]
    else: dt=9.0; st=0
    pre=hits-t; pre=pre[pre>0]; dp_=pre.min() if len(pre) else 9.0
    z=1+0.12*st*np.exp(-dt/0.16)+0.05*np.exp(-(dp_/0.12)**2)
    sx=14*st*np.exp(-dt/0.09)*np.sin(dt*2*np.pi*22); sy=10*st*np.exp(-dt/0.09)*np.cos(dt*2*np.pi*17)
    M=np.float32([[z,0,(1-z)*W/2+sx],[0,z,(1-z)*H/2+sy]])
    img=cv2.warpAffine(img,M,(W,H),flags=cv2.INTER_CUBIC,borderMode=cv2.BORDER_REFLECT)
    k=int(round(12*st*np.exp(-dt/0.11)))
    if k>0: img=np.dstack([np.roll(img[:,:,0],k,axis=1),img[:,:,1],np.roll(img[:,:,2],-k,axis=1)])
    fl=0.5*st*st*np.exp(-dt/0.05)
    if fl>0.01: img=cv2.addWeighted(img,1+fl,np.full_like(img,255),fl*0.3,0)
    enc.stdin.write(np.ascontiguousarray(img).tobytes())
enc.stdin.close(); enc.wait()
