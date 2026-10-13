# Patty Cake x Patrick's car — hard version: deep slow-mo, 7x whips, rewinds on inner 808s, stutters, big punches.
# No color grading / filters / text.
import numpy as np, subprocess, cv2, sys
exec(open('plan.py').read().split('if __name__')[0])
W,H=1280,960; CF=100
starts={int(l.split()[0]):float(l.split()[1]) for l in open('jobs.txt')}
def ss(x): x=np.clip(x,0,1); return x*x*(3-2*x)
def ramp(t,a,b,s0,s1):           # velocity ramp between two times using the natural speed curve
    return s0+(s1-s0)*(Nat(t)-Nat(a))/max(Nat(b)-Nat(a),1e-6)
def src_pos(i,t):
    a,b,w0,w1,c0,c1=SEGS[i]; L=w1-w0
    mode,m=MODES.get(i,('ramp',None))
    if mode=='ramp': return ramp(t,a,b,w0,w1)
    if mode=='rewind':           # forward out of the hit, snap rewind on the inner 808, whip forward into next hit
        r=Q; p=w0+0.6*L; q=w0+0.12*L
        if t<m: return ramp(t,a,m,w0,p)
        if t<m+r: return p+(q-p)*ss((t-m)/r)
        x=(t-m-r)/(b-m-r); return q+(w1-q)*x**2.6
    if mode=='break':            # beat drop-out: drift forward, rewind hard, then slam forward into the big 808
        p=w0+0.5*L; q=w0
        if t<m: return w0+(p-w0)*(1-(1-(t-a)/(m-a))**1.5)
        if t<m+0.16: return p+(q-p)*ss((t-m)/0.16)
        x=(t-m-0.16)/(b-m-0.16); return q+(w1-q)*x**2.2
    if mode=='stutter':          # back-and-forth on every 16th, net forward, whip into next hit
        base=ramp(t,a,b,w0,w1); k=(t-a)/Q; ph=k-np.floor(k)
        amp=0.22*L*(1-(t-a)/(b-a))
        return base+amp*(np.sin(np.pi*ph)*(1 if int(k)%2==0 else -1))
SA=np.array([x[0] for x in SEGS])
ALLH=np.concatenate([hits,INNER]); ALLS=np.concatenate([STR,np.full(len(INNER),0.55)])
enc=subprocess.Popen(['ffmpeg','-v','error','-y','-f','rawvideo','-pix_fmt','rgb24','-s',f'{W}x{H}','-r',str(OUT_FPS),'-i','-',
     '-c:v','libx264','-preset','medium','-crf','14','-pix_fmt','yuv420p','sb5_raw.mp4'],stdin=subprocess.PIPE)
cur=-1; prev=None
def load(i):
    raw=subprocess.run(['ffmpeg','-v','error','-i',f'clip_{i}.mp4','-f','rawvideo','-pix_fmt','rgb24','-'],capture_output=True).stdout
    return np.frombuffer(raw,np.uint8).reshape(-1,H,W,3)
for o in range(int(T_END*OUT_FPS)):
    t=o/OUT_FPS; i=min(max(np.searchsorted(SA,t,side='right')-1,0),len(SEGS)-1)
    if i!=cur: fr=load(i); cur=i; prev=None
    s=src_pos(i,t); f1=(s-starts[i])*CF; f1=float(np.clip(f1,0,len(fr)-1.001))
    f0=f1 if prev is None else f1-0.7*(f1-prev); prev=f1
    lo,hi=int(np.floor(min(f0,f1))),int(np.floor(max(f0,f1)))
    if hi<=lo:
        w=f1-np.floor(f1); k=int(np.floor(f1)); img=cv2.addWeighted(fr[k],1-w,fr[min(k+1,len(fr)-1)],w,0)
    else:
        img=np.clip(fr[lo:hi+1].astype(np.float32).mean(0)+0.5,0,255).astype(np.uint8)
    # hit dynamics from all hits (main + inner 808s)
    d=t-ALLH; ok=d>=0
    if ok.any(): j=np.argmin(np.where(ok,d,99)); dt=d[j]; st=ALLS[j]
    else: dt=9.0; st=0
    pre=hits-t; pre=pre[pre>0]; dp_=pre.min() if len(pre) else 9.0
    z=1+0.22*st*np.exp(-dt/0.14)+0.10*np.exp(-(dp_/0.09)**2)   # punch on hit + push-in during the whip
    if abs(t-9.92)<0.5 and t>=9.92: z+=0.35*np.exp(-(t-9.92)/0.12)  # extra slam on the biggest 808
    amp=26*st*np.exp(-dt/0.10)
    sx=amp*np.sin(dt*2*np.pi*19+1.3*j if ok.any() else 0); sy=0.8*amp*np.cos(dt*2*np.pi*14+0.7*j if ok.any() else 0)
    rot=2.2*st*np.exp(-dt/0.10)*np.sin(dt*2*np.pi*11)
    M=cv2.getRotationMatrix2D((W/2,H/2),rot,z); M[0,2]+=sx; M[1,2]+=sy
    img=cv2.warpAffine(img,M,(W,H),flags=cv2.INTER_CUBIC,borderMode=cv2.BORDER_REFLECT)
    fl=0.6*np.exp(-dt/0.04) if st>=1.0 else 0.0     # white flash only on the heaviest hits
    if fl>0.01: img=cv2.addWeighted(img,1-fl*0.5,np.full_like(img,255),fl*0.5,0)
    enc.stdin.write(np.ascontiguousarray(img).tobytes())
enc.stdin.close(); enc.wait()
