# Assignment 4 — Analysis

## Environment and commands

Language: Java. Local environment: Windows 11 (build 22631), Intel Core i5-12450H, 16 GiB RAM, Eclipse Temurin OpenJDK 17.0.20.1, Python 3.12.1.

Run from the repository root:

```text
python tools/run.py --setup
python tools/run.py --check
python tools/run.py --input examples/lcs.jsonl
python tests/student/check.py
javac --release 17 -encoding UTF-8 -d Java/build/benchmark Java/source/Algorithms.java tests/student/SearchBenchmark.java
java -Xms128m -Xmx384m -cp Java/build/benchmark SearchBenchmark
```

The launcher builds Java with `javac --release 17 -encoding UTF-8` and runs `java -Xmx384m -cp Java/build Main`. The supplied transport reads one UTF-8 JSON request per line; the algorithms keep no state between requests.

## Algorithms and correctness

**LCS.** `dp[i][j]` is the best common subsequence length for the first `i` and `j` characters. An empty prefix gives zero. Equal final characters extend a solution for both shortened prefixes. Otherwise, at least one final character must be dropped, so take the maximum of the cell above and the cell to the left. Row-major order supplies all dependencies. Backtracking follows a diagonal on equality, storing the matching character from right to left; otherwise it follows a neighbor of equal value, preferring the upper one on ties. These moves preserve the remaining optimum. Time and auxiliary memory are O((n+1)(m+1)); the result uses O(length+1) space.

**Ordered merging.** `cost[l][r]` is the least cost to merge the closed original interval. A single block costs zero, as does an empty input. In the last merge, its children must be adjacent intervals [l,k] and [k+1,r]. Minimize their two optimum costs plus the size of [l,r], calculated from 64-bit prefix sums. Increasing interval length makes child costs available. Store the first minimizing split; reconstruct both child plans before appending their parent triple so both operands are present when each merge occurs. Time is O(n³) and auxiliary memory O(n²) for nonempty inputs, including an O(n) reconstruction stack; output is O(n). The empty case is constant.

**Tree independent set.** Root at vertex 0 and record a parent-before-child order using an array and packed adjacency lists. Process its reverse so every child is complete before its parent. Taking u contributes its own weight and forces each child to be skipped; skipping u permits the larger of each child's take and skip states. Distinct child subtrees have no connecting edges, so these choices can be combined. Leaves start with take = weight and skip = 0. In forward order, a vertex is chosen only if its parent is unchosen and take > skip, with ties skipped. This yields an optimal independent set, including the empty solution for negative weights. Totals use long. The traversal and reconstruction are iterative, so a 100,000-node chain does not consume recursive stack space. Time and auxiliary memory are O(n+1); output is O(selected count+1).

**Naive search.** Try every legal start from 0 through n−m and compare characters until mismatch or the full pattern matches. Every reported start passes all comparisons. Advancing by one preserves overlaps. The shared empty-pattern rule emits all n+1 boundaries, and m>n produces no starts. Time is O(nm+n+1), auxiliary space O(1), and the result list costs O(z+1).

**KMP.** `pi[i]` is the length of the longest proper prefix of the pattern ending as a suffix at i, with pi[0]=0. In the build and the search, j denotes the length currently matched; on mismatch the next possible border is pi[j−1], so a viable start is never skipped. On equality extend j. At j=m emit i−m+1 and fall back to pi[m−1] so overlapping matches remain possible. Fallback decreases j and extension increases it by at most one per input character, giving O(n+m+z+1) time, O(m+1) auxiliary space and O(z+1) output.

**Exact Rabin–Karp.** Build polynomial fingerprints with ASCII codes, base 31 and modulus 101, with the highest power on the left. To slide, subtract the outgoing code times 31^(m−1) modulo 101, multiply by 31, add the incoming code, and normalize with floorMod. This maintains the fingerprint of each current window. A true match always has an equal fingerprint; every equal-hash candidate is verified character by character, so collisions cannot produce false positives. Time for 1≤m≤n is O(n+m+cm), where c is the number of candidate windows; it is not worst-case linear. The bound across all cases is O(nm+n+m+1), with O(1) auxiliary storage and O(z+1) output.

**Suffix array and Kasai.** Start with ranks from individual ASCII characters. In each doubling round, two adjacent ranked chunks represent the next doubled prefix. A missing second half has rank zero, below all real ranks. Stable counting sort on the second rank and then the first sorts rank pairs, and equal pairs get the same new rank. At unique ranks or full prefix coverage, the order is the suffix order. The algorithm sorts integer indices and ranks; it never constructs all suffix strings. Then invert the order. Kasai compares each suffix with its previous suffix in sorted order. Dropping the first character from a pair with common prefix of length k preserves at least k−1 common characters for the next comparison when a predecessor exists; the search then extends that lower bound. The first LCP entry is zero. A pair sharing a prefix lies inside a consecutive block of suffixes with that prefix, so the maximum adjacent LCP is the longest repeat. Keep the first strictly longer candidate in the text-order scan and allow overlaps. Counting sorts cost O(n+95) per round, hence O((n+1)log(n+1)+1) time with fixed alphabet. Kasai is linear. Auxiliary memory and result space are each O(n+1); empty text returns empty arrays and string.

**Aho–Corasick.** Insert the patterns into a trie with 95 transitions per state and record the terminal state of each dictionary entry, sharing terminals for duplicates. Breadth-first order computes each failure link to the longest proper suffix represented in the trie and fills absent transitions from the failure state, which was already processed. During each independent text scan, the state is the longest current suffix in the trie. Increment only that state's visit count; propagating visits to failure ancestors in reverse breadth-first order credits all suffix patterns ending there, including overlapping occurrences. Start at the root with fresh counts for each text. Read terminal counts in dictionary order and rows in text order. Empty dictionary gives one empty row per text, no texts gives no rows, and each empty text gives a row of zeros (one zero per pattern). For total pattern length L, p patterns, S states and q texts of total length T, construction costs O(L+95S+p), scans and propagation cost at most O(T+q(S+p+1)), auxiliary memory is O(95S+p), and output is O(q(p+1)+1).

The bounds above exclude parsing and serialization. Input storage follows the request size: string lengths for string tasks, O(n) blocks or tree data, and O(L+T+p+q) for dictionary matching. Exact result sizes are listed separately from working memory.

## Own tests

The public checker passed all 44 cases and two repeated mixed batches in one process. `tests/student/check.py` passed 478 mixed boundary and seeded oracle cases plus eight scale cases. It verifies result schemas and witnesses with the template validator against independent expected values. Small random cases use seed 250542.

| Command | Fixture in tests/student/check.py | Expected and actual | Defect targeted |
|---|---|---|---|
| lcs | Empty sides; ABC/BAC; repeated characters; 1000 × 1000 | Brute-force small subsequence optimum; large length 999, valid witness; pass | Substring instead of subsequence; rejecting a tied optimum |
| merge | Empty/single; zeros; 1,9,1; 200 blocks of 10⁹ | Enumerated adjacent plans; large cost 1,544,000,000,000; valid plans; pass | Nonadjacent merges, reversed plan order or integer overflow |
| tree | Empty/single, negative and zero; 100,000-node chain | Exhaustive small independent sets; large weight 50,000,000,000,000; pass | Negative optimum, duplicate/adjacent selections or deep recursion |
| naive / kmp / rk | Empty text/pattern, overlap, case, punctuation and shared random cases | Independent exact starts; pass | Missing last boundary, overlaps or case sensitivity |
| rk | abdjab searched for ab; all-a maximum product case | [0,4] despite the dj collision; 8,001 large starts; pass | Reporting a collision or using a negative rolling residue |
| kmp | 200,000 a's searched for 100,000 a's | 100,001 starts; pass | Quadratic scan or resetting overlap state |
| suffix | Empty/single/all distinct/all equal/tied repeats; 50,000 a's | Direct small order and LCP; descending large SA, LCP 0..49,999; pass | Empty suffix, invalid order, off-by-one LCP or repeat length |
| aho | Empty inputs, suffixes, duplicates, repeated texts; 10,000 patterns and 100 texts | Independent exact counts, including dense maximum-size fixture; pass | Missing failure-link matches, leaked visits, per-match enumeration |

## Search comparison

`SearchBenchmark.java` measures the three algorithms on the same inputs. Random text uses Java Random with seed 250542+n and uniform lowercase letters; the pattern is the slice beginning at floor(n/3). Repetitive inputs use all a's in both text and pattern. A startsWith oracle checks every position for all algorithms before timing. In a single JVM with its default tiered JIT and the heap flags above, each algorithm/case gets 150 warm-up calls, then three measured batches of 30 calls in rotating algorithm order. The timer includes algorithm work and position-list allocation, excluding input generation, oracle, compilation, JVM startup and JSON. Each number is the measured batch time divided by 30, in µs per call.

| Case | n | m | Algorithm | Run 1 | Run 2 | Run 3 | Median | Verified starts |
| --- | ---: | ---: | --- | ---: | ---: | ---: | ---: | ---: |
| Random | 300 | 30 | naive | 14.13 | 11.39 | 6.38 | 11.39 | 1 |
| Random | 300 | 30 | KMP | 12.78 | 6.78 | 5.53 | 6.78 | 1 |
| Random | 300 | 30 | exact RK | 16.75 | 14.71 | 7.23 | 14.71 | 1 |
| Random | 1200 | 120 | naive | 25.04 | 23.82 | 24.26 | 24.26 | 1 |
| Random | 1200 | 120 | KMP | 20.02 | 19.92 | 19.49 | 19.92 | 1 |
| Random | 1200 | 120 | exact RK | 20.42 | 20.07 | 20.26 | 20.26 | 1 |
| Random | 3000 | 300 | naive | 6.96 | 6.36 | 6.27 | 6.36 | 1 |
| Random | 3000 | 300 | KMP | 47.09 | 46.52 | 46.56 | 46.56 | 1 |
| Random | 3000 | 300 | exact RK | 56.09 | 55.91 | 56.22 | 56.09 | 1 |
| Repetitive | 300 | 30 | naive | 21.32 | 20.32 | 21.75 | 21.32 | 271 |
| Repetitive | 300 | 30 | KMP | 16.08 | 14.50 | 15.74 | 15.74 | 271 |
| Repetitive | 300 | 30 | exact RK | 26.64 | 24.38 | 24.64 | 24.64 | 271 |
| Repetitive | 1200 | 120 | naive | 235.17 | 368.76 | 394.86 | 368.76 | 1081 |
| Repetitive | 1200 | 120 | KMP | 16.60 | 13.01 | 10.46 | 13.01 | 1081 |
| Repetitive | 1200 | 120 | exact RK | 397.98 | 430.60 | 458.15 | 430.60 | 1081 |
| Repetitive | 3000 | 300 | naive | 1298.72 | 1517.65 | 1356.34 | 1356.34 | 2701 |
| Repetitive | 3000 | 300 | KMP | 19.26 | 38.18 | 23.31 | 23.31 | 2701 |
| Repetitive | 3000 | 300 | exact RK | 2159.53 | 1698.30 | 1827.64 | 1827.64 | 2701 |

For random text, most naive comparisons fail quickly; the largest random case even had the lowest naive median in this session. That is not a scaling law: JVM optimization and scheduling affect such short measurements. On the largest repetitive input KMP's median was 23.31 µs versus 1356.34 µs for naive and 1827.64 µs for exact RK. Every all-a window is a real match, so exact RK must verify every candidate, while KMP carries prefix information forward. On other inputs the small modulus also admits false candidates (as with ab/dj); the results should not be generalized to all rolling hashes. The measurements illustrate behavior on these workloads; the invariants above justify the complexity bounds.

## Sources and AI assistance

The [official template](https://github.com/RZhaniya/daa-assignment-4-archive) supplies the specification, JSON adapters, public checker and workflow. The official lesson and practice materials cover the recurrences, prefix fallback, rolling hash and automaton used here.

OpenAI Codex was used for testing, correctness checks, and assistance with implementation and documentation. The saved outputs and benchmark measurements were obtained by executing the included code in the local environment. No additional external algorithm code was copied.

For Assignment 4, Codex implemented the Java algorithms and assisted with the report and test harnesses. Local verification used the official launcher, the independent oracle tests and the benchmark above.
