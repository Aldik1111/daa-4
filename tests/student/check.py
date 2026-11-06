"""Run independent oracles against one Java process, then run maximum-size cases."""
from functools import cache
from pathlib import Path
import random
import sys

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from run import build, execute
from public_validate import check

rng = random.Random(250542)
cases = []

def add(task, expected, **fields):
    cases.append((dict(task=task, **fields), expected))

def matches(t, p):
    return [i for i in range(len(t) - len(p) + 1) if t.startswith(p, i)]

def subsequences(s):
    return {''.join(c for i, c in enumerate(s) if mask >> i & 1)
            for mask in range(1 << len(s))}

@cache
def merge_cost(blocks):
    if len(blocks) < 2:
        return 0
    return min(blocks[i] + blocks[i+1] +
               merge_cost(blocks[:i] + (blocks[i]+blocks[i+1],) + blocks[i+2:])
               for i in range(len(blocks)-1))

def suffix_result(t):
    sa = sorted(range(len(t)), key=lambda i: t[i:])
    lcp = [0]*len(t)
    for k, (i, j) in enumerate(zip(sa, sa[1:]), 1):
        while i+lcp[k] < len(t) and j+lcp[k] < len(t) and t[i+lcp[k]] == t[j+lcp[k]]:
            lcp[k] += 1
    k = max(range(len(t)), key=lcp.__getitem__, default=None)
    return dict(sa=sa, lcp=lcp, repeated='' if k is None else t[sa[k]:sa[k]+lcp[k]])

def string(n):
    return ''.join(rng.choice('abAB !~"\\') for _ in range(n))

add('rk', {'positions':[0, 4]}, text='abdjab', pattern='ab')
for a, b in [('', ''), ('a', ''), ('', 'b'), ('ABC', 'BAC'), ('aBaBa', 'BaBaa')]:
    add('lcs', {'length':max(map(len, subsequences(a) & subsequences(b)))}, a=a, b=b)
for sizes in [[], [9], [0,0,0], [1,9,1], [10**9]*5]:
    add('merge', {'cost':merge_cost(tuple(sizes))}, sizes=sizes)
for weights, edges in [([], []), ([7], []), ([-1,-2], [[0,1]]), ([0,0], [[1,0]])]:
    add('tree', {'weight':max([0]+weights)}, n=len(weights), weights=weights, edges=edges)
for text, pattern in [('', ''), ('', 'a'), ('A','Aa'), ('aAa! aAa!','Aa!'), ('aaaa','aa')]:
    for task in ('naive','kmp','rk'):
        add(task, {'positions':matches(text,pattern)}, text=text, pattern=pattern)
for t in ['', 'x', 'abcde', 'aaaaa', 'abcXabcYxyzXxyz']:
    add('suffix', suffix_result(t), text=t)
for p, t in [([],['','xx']), (['a'],[]), (['a','aa','a','baa'],['baaaa','baaaa',''])]:
    add('aho', {'counts':[[len(matches(s,x)) for x in p] for s in t]}, patterns=p, texts=t)

for _ in range(55):
    a,b=string(rng.randrange(9)),string(rng.randrange(9))
    add('lcs', {'length':max(map(len, subsequences(a) & subsequences(b)))}, a=a,b=b)
    sizes=[rng.randrange(8) for _ in range(rng.randrange(8))]
    add('merge', {'cost':merge_cost(tuple(sizes))}, sizes=sizes)
    n=rng.randrange(9)
    edges=[[i,rng.randrange(i)] for i in range(1,n)]
    rng.shuffle(edges)
    weights=[rng.randrange(-5,8) for _ in range(n)]
    best=max(sum(w for i,w in enumerate(weights) if mask>>i&1)
             for mask in range(1<<n) if all(not (mask>>u&1 and mask>>v&1) for u,v in edges))
    add('tree', {'weight':best}, n=n,weights=weights,edges=edges)
    t=string(rng.randrange(35))
    p=t[:rng.randrange(len(t)+1)] if rng.randrange(2) else string(rng.randrange(10))
    for task in ('naive','kmp','rk'):
        add(task, {'positions':matches(t,p)}, text=t,pattern=p)
    add('suffix', suffix_result(t), text=t)
    patterns=[string(rng.randrange(1,6)) for _ in range(rng.randrange(9))]
    texts=[t,t,'',string(20)]
    add('aho', {'counts':[[len(matches(s,x)) for x in patterns] for s in texts]}, patterns=patterns,texts=texts)

command=build('Java')

def verify(label):
    answers=execute(command,[q for q,_ in cases])
    for i, ((query, expected), answer) in enumerate(zip(cases,answers),1):
        if not check(query,answer,expected):
            raise AssertionError(f'{label}, case {i} ({query["task"]})')
    print(f'PASS {label}: {len(cases)}')
    cases.clear()

verify('mixed boundary and seeded independent oracle cases')
add('lcs', {'length':999}, a='a'*999+'b', b='b'+'a'*999)
add('merge', {'cost':1544*10**9}, sizes=[10**9]*200)
n=100_000
add('tree', {'weight':(n//2)*10**9}, n=n,weights=[10**9]*n,edges=[[i-1,i] for i in range(1,n)])
add('naive', {'positions':[0]}, text='a'*3000,pattern='a'*3000)
add('kmp', {'positions':list(range(100_001))}, text='a'*200_000,pattern='a'*100_000)
add('rk', {'positions':list(range(8001))}, text='a'*10_000,pattern='a'*2000)
n=50_000
add('suffix', dict(sa=list(range(n-1,-1,-1)),lcp=list(range(n)),repeated='a'*(n-1)), text='a'*n)
p=['a'*i for i in range(1,100)]+['a'*9]*9900+['a'*5950]
t=['a'*2000]*100
add('aho', {'counts':[[max(0,len(s)-len(x)+1) for x in p] for s in t]}, patterns=p,texts=t)
verify('published scale, with witness validation')
