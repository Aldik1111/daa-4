import java.util.*;

/** Algorithms for the eight Assignment 4 JSONL commands. */
public final class Algorithms {
    static Map<String,Object> lcs(Map<String,Object> input) {
        String a = (String) input.get("a"), b = (String) input.get("b");
        int n = a.length(), m = b.length();
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1)
                        ? dp[i - 1][j - 1] + 1 : Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
        char[] sequence = new char[dp[n][m]];
        int end = sequence.length, i = n, j = m;
        while (i > 0 && j > 0) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) {
                sequence[--end] = a.charAt(--i);
                j--;
            } else if (dp[i - 1][j] >= dp[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }
        return Map.of("length", sequence.length, "sequence", new String(sequence));
    }

    static Map<String,Object> merge(Map<String,Object> input) {
        List<?> sizes = (List<?>) input.get("sizes");
        int n = sizes.size();
        List<int[]> plan = new ArrayList<>();
        if (n < 2) return Map.of("cost", 0L, "plan", plan);
        long[] prefix = new long[n + 1];
        for (int i = 0; i < n; i++) prefix[i + 1] = prefix[i] + ((Number) sizes.get(i)).longValue();
        long[][] cost = new long[n][n];
        int[][] split = new int[n][n];
        for (int len = 2; len <= n; len++) {
            for (int l = 0; l + len <= n; l++) {
                int r = l + len - 1;
                long best = Long.MAX_VALUE;
                long sum = prefix[r + 1] - prefix[l];
                for (int k = l; k < r; k++) {
                    long candidate = cost[l][k] + cost[k + 1][r] + sum;
                    if (candidate < best) {
                        best = candidate;
                        split[l][r] = k;
                    }
                }
                cost[l][r] = best;
            }
        }
        mergePlan(0, n - 1, split, plan);
        return Map.of("cost", cost[0][n - 1], "plan", plan);
    }

    private static void mergePlan(int l, int r, int[][] split, List<int[]> plan) {
        if (l == r) return;
        int k = split[l][r];
        mergePlan(l, k, split, plan);
        mergePlan(k + 1, r, split, plan);
        plan.add(new int[]{l, k, r});
    }

    static Map<String,Object> tree(Map<String,Object> input) {
        int n = ((Number) input.get("n")).intValue();
        if (n == 0) return Map.of("weight", 0L, "selected", List.of());
        List<?> weights = (List<?>) input.get("weights");
        List<?> edges = (List<?>) input.get("edges");
        int[] head = new int[n], to = new int[2 * edges.size()], next = new int[to.length];
        Arrays.fill(head, -1);
        int e = 0;
        for (Object edge : edges) {
            List<?> pair = (List<?>) edge;
            int u = ((Number) pair.get(0)).intValue(), v = ((Number) pair.get(1)).intValue();
            to[e] = v; next[e] = head[u]; head[u] = e++;
            to[e] = u; next[e] = head[v]; head[v] = e++;
        }
        int[] parent = new int[n], order = new int[n];
        parent[0] = -1;
        for (int i = 0, end = 1; i < end; i++) {
            int u = order[i];
            for (int arc = head[u]; arc != -1; arc = next[arc]) {
                int v = to[arc];
                if (v == parent[u]) continue;
                parent[v] = u;
                order[end++] = v;
            }
        }
        long[] take = new long[n], skip = new long[n];
        for (int i = 0; i < n; i++) take[i] = ((Number) weights.get(i)).longValue();
        for (int i = n - 1; i > 0; i--) {
            int u = order[i], p = parent[u];
            take[p] += skip[u];
            skip[p] += Math.max(take[u], skip[u]);
        }
        boolean[] chosen = new boolean[n];
        List<Integer> selected = new ArrayList<>();
        for (int u : order) {
            if ((parent[u] == -1 || !chosen[parent[u]]) && take[u] > skip[u]) {
                chosen[u] = true;
                selected.add(u);
            }
        }
        return Map.of("weight", Math.max(take[0], skip[0]), "selected", selected);
    }

    static Map<String,Object> naive(Map<String,Object> input) {
        return Map.of("positions", naivePositions((String) input.get("text"), (String) input.get("pattern")));
    }

    static Map<String,Object> kmp(Map<String,Object> input) {
        return Map.of("positions", kmpPositions((String) input.get("text"), (String) input.get("pattern")));
    }

    static Map<String,Object> rk(Map<String,Object> input) {
        return Map.of("positions", rkPositions((String) input.get("text"), (String) input.get("pattern")));
    }

    private static List<Integer> boundaries(int n) {
        List<Integer> positions = new ArrayList<>(n + 1);
        for (int i = 0; i <= n; i++) positions.add(i);
        return positions;
    }

    private static boolean matches(String text, String pattern, int start) {
        for (int j = 0; j < pattern.length(); j++) {
            if (text.charAt(start + j) != pattern.charAt(j)) return false;
        }
        return true;
    }

    static List<Integer> naivePositions(String text, String pattern) {
        if (pattern.isEmpty()) return boundaries(text.length());
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i <= text.length() - pattern.length(); i++) {
            if (matches(text, pattern, i)) positions.add(i);
        }
        return positions;
    }

    static List<Integer> kmpPositions(String text, String pattern) {
        int n = text.length(), m = pattern.length();
        if (m == 0) return boundaries(n);
        List<Integer> positions = new ArrayList<>();
        if (m > n) return positions;
        int[] pi = new int[m];
        for (int i = 1, j = 0; i < m; i++) {
            while (j > 0 && pattern.charAt(i) != pattern.charAt(j)) j = pi[j - 1];
            if (pattern.charAt(i) == pattern.charAt(j)) j++;
            pi[i] = j;
        }
        for (int i = 0, j = 0; i < n; i++) {
            while (j > 0 && text.charAt(i) != pattern.charAt(j)) j = pi[j - 1];
            if (text.charAt(i) == pattern.charAt(j)) j++;
            if (j == m) {
                positions.add(i - m + 1);
                j = pi[j - 1];
            }
        }
        return positions;
    }

    static List<Integer> rkPositions(String text, String pattern) {
        int n = text.length(), m = pattern.length();
        if (m == 0) return boundaries(n);
        List<Integer> positions = new ArrayList<>();
        if (m > n) return positions;
        int base = 31, mod = 101, power = 1, target = 0, window = 0;
        for (int i = 0; i < m; i++) {
            if (i > 0) power = power * base % mod;
            target = (target * base + pattern.charAt(i)) % mod;
            window = (window * base + text.charAt(i)) % mod;
        }
        for (int i = 0; i <= n - m; i++) {
            if (window == target && matches(text, pattern, i)) positions.add(i);
            if (i < n - m) {
                window = Math.floorMod((window - text.charAt(i) * power) * base
                        + text.charAt(i + m), mod);
            }
        }
        return positions;
    }

    static Map<String,Object> suffix(Map<String,Object> input) {
        String text = (String) input.get("text");
        int n = text.length();
        int[] sa = new int[n], lcp = new int[n];
        if (n == 0) return Map.of("sa", sa, "lcp", lcp, "repeated", "");
        int[] rank = new int[n], updated = new int[n], temp = new int[n];
        int[] count = new int[Math.max(n, 95) + 1];
        int classes = 95;
        for (int i = 0; i < n; i++) {
            sa[i] = i;
            rank[i] = text.charAt(i) - 31; // rank zero is reserved for beyond the end
        }
        for (int width = 1; width < n; width *= 2) {
            countRanks(sa, temp, rank, width, classes, count);
            countRanks(temp, sa, rank, 0, classes, count);
            classes = 1;
            updated[sa[0]] = classes;
            for (int i = 1; i < n; i++) {
                int a = sa[i - 1], b = sa[i];
                if (rank[a] != rank[b] || rankAt(rank, a + width) != rankAt(rank, b + width)) classes++;
                updated[b] = classes;
            }
            int[] old = rank;
            rank = updated;
            updated = old;
            if (classes == n) break;
        }
        for (int i = 0; i < n; i++) rank[sa[i]] = i;
        int best = 0, start = 0;
        for (int i = 0, len = 0; i < n; i++) {
            int position = rank[i];
            if (position == 0) {
                len = 0;
                continue;
            }
            int j = sa[position - 1];
            while (i + len < n && j + len < n && text.charAt(i + len) == text.charAt(j + len)) len++;
            lcp[position] = len;
            if (len > best) {
                best = len;
                start = i;
            }
            if (len > 0) len--;
        }
        return Map.of("sa", sa, "lcp", lcp, "repeated", text.substring(start, start + best));
    }

    private static int rankAt(int[] rank, int i) {
        return i < rank.length ? rank[i] : 0;
    }

    private static void countRanks(int[] from, int[] to, int[] rank, int offset, int classes, int[] count) {
        Arrays.fill(count, 0, classes + 1, 0);
        for (int i : from) count[rankAt(rank, i + offset)]++;
        int start = 0;
        for (int key = 0; key <= classes; key++) {
            int size = count[key];
            count[key] = start;
            start += size;
        }
        for (int i : from) to[count[rankAt(rank, i + offset)]++] = i;
    }

    static Map<String,Object> aho(Map<String,Object> input) {
        List<?> patterns = (List<?>) input.get("patterns"), texts = (List<?>) input.get("texts");
        List<long[]> counts = new ArrayList<>();
        if (texts.isEmpty()) return Map.of("counts", counts);
        if (patterns.isEmpty()) {
            for (Object ignored : texts) counts.add(new long[0]);
            return Map.of("counts", counts);
        }
        int alphabet = 95;
        List<int[]> transitions = new ArrayList<>();
        transitions.add(new int[alphabet]);
        int[] terminal = new int[patterns.size()];
        for (int i = 0; i < terminal.length; i++) {
            String pattern = (String) patterns.get(i);
            int state = 0;
            for (int j = 0; j < pattern.length(); j++) {
                int symbol = pattern.charAt(j) - 32;
                int[] row = transitions.get(state);
                if (row[symbol] == 0) {
                    row[symbol] = transitions.size();
                    transitions.add(new int[alphabet]);
                }
                state = row[symbol];
            }
            terminal[i] = state;
        }
        int states = transitions.size();
        int[] failure = new int[states], order = new int[states];
        int tail = 1;
        for (int child : transitions.get(0)) if (child != 0) order[tail++] = child;
        for (int i = 1; i < tail; i++) {
            int state = order[i];
            int[] row = transitions.get(state), fallback = transitions.get(failure[state]);
            for (int c = 0; c < alphabet; c++) {
                int child = row[c];
                if (child == 0) {
                    row[c] = fallback[c];
                } else {
                    failure[child] = fallback[c];
                    order[tail++] = child;
                }
            }
        }
        long[] visits = new long[states];
        for (Object value : texts) {
            String text = (String) value;
            long[] row = new long[terminal.length];
            if (!text.isEmpty()) {
                Arrays.fill(visits, 0L);
                int state = 0;
                for (int i = 0; i < text.length(); i++) {
                    state = transitions.get(state)[text.charAt(i) - 32];
                    visits[state]++;
                }
                for (int i = states - 1; i > 0; i--) {
                    int u = order[i];
                    visits[failure[u]] += visits[u];
                }
                for (int i = 0; i < row.length; i++) row[i] = visits[terminal[i]];
            }
            counts.add(row);
        }
        return Map.of("counts", counts);
    }
}
