# Patty Cake x Patrick's car velocity edit: hits, slots, clip windows
import numpy as np
T_END=22.3
# (time after 0:23, strength) — 808 hits on the 16th grid (76 BPM, 0.7895s beat)
HITS=[(1.63,.5),(2.42,1),(3.60,1),(4.20,.7),(5.58,1),(6.77,1),(7.36,.7),(8.74,.7),(9.92,1.3),(10.51,.7),(11.89,1),
      (13.08,1),(13.67,.7),(14.46,.7),(15.05,1),(15.64,.7),(16.24,.7),(16.83,.7),(17.62,.7),(18.21,1),(18.80,.7),
      (19.40,.7),(19.99,.7),(20.78,.7),(21.37,1.1)]
hits=np.array([h for h,_ in HITS]); STR=np.array([s for _,s in HITS])
# one clip per slot [prev hit, hit]: (shot start, shot end, preferred centre) in episode seconds
CLIPS=[(3.17,4.15,3.6),     # car pulls up
       (10.22,11.66,11.0),  # car rolls up to the Krusty Krab
       (15.6,17.38,16.8),   # Patrick waving from the car
       (63.6,65.94,65.0),   # car drives into Krusty Krab window
       (65.98,67.42,66.7),  # Patrick in the car
       (101.34,105.82,102.6),# SpongeBob freaking out in the car
       (105.86,107.38,106.6),# Patrick shocked
       (86.54,87.82,87.2),  # pointing forward
       (69.0,72.14,70.7),   # Patrick hands up (beat break)
       (83.34,84.58,84.0),  # road + pipe pass (big 808 returns)
       (88.5,89.46,89.0),   # road wide
       (85.1,86.5,85.8),    # traffic light
       (84.62,85.06,84.84), # road w/ car
       (89.5,90.42,90.0),   # road wide
       (90.46,91.5,91.0),   # road wide
       (91.6,100.2,93.0),   # driving through pipe city
       (126.9,129.18,127.8),# Patrick's license
       (129.4,134.38,130.6),# SpongeBob squished
       (136.4,141.58,137.2),# flame car driving
       (112.0,114.5,113.2), # Patrick in car
       (142.22,142.9,142.56),# Patrick shocked close
       (136.4,141.58,140.3),# SpongeBob with license
       (107.42,126.7,118.0),# driving
       (67.46,69.0,68.2),   # Patrick helmet
       (18.05,20.2,19.3),   # yellow car
       (142.94,144.66,143.9)]# SpongeBob celebrates
BASE=0.22; AMP=4.2; RISE=0.08; FALL=0.12
def vnat(t):
    d=np.atleast_1d(t)[:,None]-hits[None,:]
    return BASE+AMP*(STR[None,:]*np.where(d<0,np.exp(-(d/RISE)**2),np.exp(-(d/FALL)**2))).sum(1)
OUT_FPS=60
tt=np.arange(int(T_END*OUT_FPS)+2)/OUT_FPS
N=np.concatenate([[0],np.cumsum(vnat(tt[:-1]+0.5/OUT_FPS))/OUT_FPS])
def Nat(t): return np.interp(t,tt,N)
B=np.concatenate([[0],hits,[T_END]])
assert len(B)-1==len(CLIPS),(len(B)-1,len(CLIPS))
SEGS=[]
for i,(c0,c1,pc) in enumerate(CLIPS):
    a,b=B[i],B[i+1]; n=Nat(b)-Nat(a); L=min(n*1.25,c1-c0)
    w0=min(max(pc-L/2,c0),c1-L); SEGS.append((a,b,w0,w0+L,c0,c1))
if __name__=='__main__':
    for a,b,w0,w1,c0,c1 in SEGS: print(f"out {a:5.2f}-{b:5.2f} src {w0:7.2f}-{w1:7.2f} mult {(w1-w0)/(Nat(b)-Nat(a)):.2f}")
