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

enc=subprocess.Popen(['ffmpeg','-v','error','-y','-f','rawvideo','-pix_fmt','rgb24','-s',f'{W}x{H}','-r',str(OUT_FPS),'-i','-',
     '-c:v','libx264','-preset','medium','-crf','14','-pix_fmt','yuv420p','sb6_raw.mp4'],stdin=subprocess.PIPE)
cur=-1; prev=None
def load(i):
    raw=subprocess.run(['ffmpeg','-v','error','-i',f'clip_{i}.mp4','-f','rawvideo','-pix_fmt','rgb24','-'],capture_output=True).stdout
    return np.frombuffer(raw,np.uint8).reshape(-1,H,W,3)
for o in range(int(T_END*OUT_FPS)):
    t=o/OUT_FPS; i=min(max(np.searchsorted(SA,t,side='right')-1,0),len(SEGS)-1)
    if i!=cur: fr=load(i); cur=i; prev=None
    a,b,w0,w1,c0,c1=SEGS[i]; s=ramp(t,a,b,w0,w1); f1=(s-starts[i])*CF; f1=float(np.clip(f1,0,len(fr)-1.001))
    f0=f1 if prev is None else f1-0.7*(f1-prev); prev=f1
    lo,hi=int(np.floor(min(f0,f1))),int(np.floor(max(f0,f1)))
    if hi<=lo:
        w=f1-np.floor(f1); k=int(np.floor(f1)); img=cv2.addWeighted(fr[k],1-w,fr[min(k+1,len(fr)-1)],w,0)
    else:
        img=np.clip(fr[lo:hi+1].astype(np.float32).mean(0)+0.5,0,255).astype(np.uint8)
    # only effect: a slight push-in that follows the playback speed (no shake/flash/RGB)
    sp=float(vnat(t)[0])/(BASE+AMP); z=1+0.045*min(sp,1.0)
    if z>1.002:
        M=np.float32([[z,0,(1-z)*W/2],[0,z,(1-z)*H/2]]); img=cv2.warpAffine(img,M,(W,H),flags=cv2.INTER_CUBIC)
    enc.stdin.write(np.ascontiguousarray(img).tobytes())
enc.stdin.close(); enc.wait()
