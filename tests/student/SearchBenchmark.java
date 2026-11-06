import java.util.*;
import java.util.function.BiFunction;

/** Compare in-process algorithm calls with identical input and timing scope. */
public final class SearchBenchmark {
    private static volatile int sink;
    private static final int CALLS = 30;
    private static final long SEED = 250542;
    private static final List<BiFunction<String,String,List<Integer>>> SEARCHES =
            List.of(Algorithms::naivePositions, Algorithms::kmpPositions, Algorithms::rkPositions);
    private static final String[] NAMES = {"naive", "KMP", "exact RK"};

    private static String randomString(Random rng, int n) {
        StringBuilder text = new StringBuilder(n);
        for (int i = 0; i < n; i++) text.append((char) ('a' + rng.nextInt(26)));
        return text.toString();
    }

    private static List<Integer> oracle(String text, String pattern) {
        List<Integer> answer = new ArrayList<>();
        for (int i = 0; i <= text.length() - pattern.length(); i++)
            if (text.startsWith(pattern, i)) answer.add(i);
        return answer;
    }

    private static double measure(BiFunction<String,String,List<Integer>> algorithm,
                                  String text, String pattern, int calls) {
        int total = 0;
        long start = System.nanoTime();
        for (int i = 0; i < calls; i++) total += algorithm.apply(text, pattern).size();
        long elapsed = System.nanoTime() - start;
        sink = total;
        return elapsed / (1000.0 * calls);
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.ROOT);
        for (boolean repetitive : new boolean[]{false,true}) {
            for (int n : new int[]{300, 1200, 3000}) {
                int m = n / 10;
                Random rng = new Random(SEED + n);
                String text = repetitive ? "a".repeat(n) : randomString(rng, n);
                String pattern = repetitive ? "a".repeat(m) : text.substring(n / 3, n / 3 + m);
                List<Integer> expected = oracle(text, pattern);
                for (var search : SEARCHES)
                    if (!search.apply(text, pattern).equals(expected)) throw new AssertionError("wrong starts");
                for (var search : SEARCHES) measure(search, text, pattern, 150);
                double[][] times = new double[3][3];
                for (int run = 0; run < 3; run++)
                    for (int offset = 0; offset < 3; offset++) {
                        int i = (run + offset) % 3;
                        times[i][run] = measure(SEARCHES.get(i), text, pattern, CALLS);
                    }
                for (int i = 0; i < 3; i++) {
                    double[] sorted = times[i].clone();
                    Arrays.sort(sorted);
                    System.out.printf("| %s | %d | %d | %s | %.2f | %.2f | %.2f | %.2f | %d |%n",
                            repetitive ? "Repetitive" : "Random", n, m, NAMES[i],
                            times[i][0], times[i][1], times[i][2], sorted[1], expected.size());
                }
            }
        }
    }
}
