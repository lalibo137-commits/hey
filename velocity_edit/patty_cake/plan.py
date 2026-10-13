# Patty Cake x Patrick's car v3: fewer, deliberate cuts; the other 808s are speed whips inside the shot
import numpy as np
T_END=22.3; OUT_FPS=60
HITS=[(1.63,.5),(2.42,1),(3.60,1),(4.20,.7),(5.58,1),(6.77,1),(7.36,.7),(8.74,.6),(9.92,1.2),(10.51,.7),(11.89,1),
      (13.08,1),(13.67,.7),(14.46,.7),(15.05,1),(15.64,.7),(16.24,.7),(16.83,.8),(17.62,.7),(18.21,1),(18.80,.7),
      (19.40,.7),(19.99,.8),(20.78,.7),(21.37,1)]
hits=np.array([h for h,_ in HITS]); STR=np.array([s for _,s in HITS])
# cut points: drop, bar downbeats, beat break, the huge 808, then half-bars in the busy section
CUTS=[0,2.42,5.58,8.74,9.92,11.89,15.05,16.83,18.21,19.99,21.37,T_END]
CLIPS=[(0.35,3.12,2.0),       # SpongeBob walks out the door (intro, slow-mo + zoom)
       (62.6,65.94,64.6),     # car drives into the Krusty Krab
       (17.45,20.2,18.8),     # Patrick waving from the car
       (69.0,72.14,70.6),     # Patrick hands up (beat break)
       (83.34,84.58,83.96),   # car whips past the pipe (huge 808)
       (91.6,100.2,95.0),     # driving through pipe city
       (136.4,141.58,138.0),  # flame car driving
       (101.34,105.82,103.0), # SpongeBob freaking out
       (107.42,126.7,113.6),  # driving, Patrick's mitt
       (126.9,129.18,127.9),  # Patrick's license
       (142.94,144.66,143.9)] # SpongeBob celebrates
BASE=0.3; AMP=3.6; RISE=0.14; FALL=0.22
def vnat(t):
    d=np.atleast_1d(t)[:,None]-hits[None,:]
    return BASE+AMP*(STR[None,:]*np.where(d<0,np.exp(-(d/RISE)**2),np.exp(-(d/FALL)**2))).sum(1)
tt=np.arange(int(T_END*OUT_FPS)+2)/OUT_FPS
N=np.concatenate([[0],np.cumsum(vnat(tt[:-1]+0.5/OUT_FPS))/OUT_FPS])
def Nat(t): return np.interp(t,tt,N)
SEGS=[]
for i,(c0,c1,pc) in enumerate(CLIPS):
    a,b=CUTS[i],CUTS[i+1]; n=Nat(b)-Nat(a); L=min(n,c1-c0)
    w0=min(max(pc-L/2,c0),c1-L); SEGS.append((a,b,w0,w0+L,c0,c1))
# per-shot zoom: (focus x, focus y as fraction of frame, start zoom, end zoom)
ZOOM=[(0.50,0.30,1.0,1.9),(0.40,0.60,1.0,1.40),(0.42,0.45,1.05,1.55),(0.45,0.35,1.0,1.65),(0.45,0.62,1.1,1.45),
      (0.45,0.55,1.0,1.35),(0.45,0.50,1.0,1.40),(0.45,0.45,1.05,1.55),(0.50,0.50,1.0,1.35),(0.40,0.45,1.0,1.55),(0.35,0.45,1.0,1.45)]
if __name__=='__main__':
    for a,b,w0,w1,c0,c1 in SEGS: print(f"out {a:5.2f}-{b:5.2f} src {w0:7.2f}-{w1:7.2f} mult {(w1-w0)/(Nat(b)-Nat(a)):.2f}")
    for i,(a,b,w0,w1,c0,c1) in enumerate(SEGS):
        p0=max(c0,w0-0.15); p1=min(c1,w1+0.15); open('jobs.txt','a' if i else 'w').write(f"{i} {p0:.3f} {p1-p0:.3f}\n")
