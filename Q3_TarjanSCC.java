// Q3 - CO3: Tarjan's SCC — Linux Kernel Module Dependency Analysis

import java.util.*;

public class Q3_TarjanSCC {

    // ── Tarjan's SCC ──────────────────────────────────────────────────────────
    static class TarjanSCC {
        Map<String, List<String>> adj;
        Map<String, Integer> disc = new HashMap<>();
        Map<String, Integer> low  = new HashMap<>();
        Set<String>          onStack = new HashSet<>();
        Deque<String>        stack   = new ArrayDeque<>();
        List<List<String>>   sccs    = new ArrayList<>();
        int timer = 0;

        TarjanSCC(Map<String, List<String>> adj) {
            this.adj = adj;
        }

        List<List<String>> findSCCs() {
            // Process vertices in alphabetical order (as required by exam)
            List<String> vertices = new ArrayList<>(adj.keySet());
            Collections.sort(vertices);

            for (String v : vertices) {
                if (!disc.containsKey(v)) dfs(v);
            }
            return sccs;
        }

        void dfs(String u) {
            disc.put(u, timer);
            low.put(u, timer);
            timer++;
            stack.push(u);
            onStack.add(u);

            System.out.printf("  VISIT  %-4s  disc=%-2d  low=%-2d  stack=%s%n",
                    u, disc.get(u), low.get(u), stackSnapshot());

            for (String v : adj.getOrDefault(u, Collections.emptyList())) {
                if (!disc.containsKey(v)) {
                    // Tree edge — recurse
                    dfs(v);
                    // TODO 1: propagate low-link from child
                    low.put(u, Math.min(low.get(u), low.get(v)));
                    System.out.printf("  RETURN %-4s  low updated to %d  (via tree edge to %s)%n",
                            u, low.get(u), v);
                } else if (onStack.contains(v)) {
                    // TODO 2: back/cross edge to ancestor still on stack
                    low.put(u, Math.min(low.get(u), disc.get(v)));
                    System.out.printf("  BACK   %-4s→%-4s  low updated to %d%n",
                            u, v, low.get(u));
                }
                // Edge to a completed SCC — ignore
            }

            // TODO 3: check if u is an SCC root; if so, pop the SCC
            if (low.get(u).equals(disc.get(u))) {
                List<String> scc = new ArrayList<>();
                while (true) {
                    String w = stack.pop();
                    onStack.remove(w);
                    scc.add(w);
                    if (w.equals(u)) break;
                }
                Collections.sort(scc);   // sort for deterministic output
                sccs.add(scc);
                System.out.printf("  *** SCC found: %s%n%n", scc);
            }
        }

        String stackSnapshot() {
            List<String> s = new ArrayList<>(stack);
            Collections.reverse(s);
            return s.toString();
        }
    }

    // ── Build the 9-node module-dependency graph from the exam ────────────────
    static Map<String, List<String>> buildGraph() {
        Map<String, List<String>> g = new LinkedHashMap<>();

        // Initialise all 9 nodes
        for (String m : new String[]{"m1","m2","m3","m4","m5","m6","m7","m8","m9"}) {
            g.put(m, new ArrayList<>());
        }

        // 12 directed edges inferred from the exam diagram
        // Yellow SCC cluster: m1, m2, m3 (mutual cycle)
        g.get("m1").add("m2");
        g.get("m2").add("m3");
        g.get("m3").add("m1");

        // Pink SCC cluster: m6, m7 (mutual cycle)
        g.get("m6").add("m7");
        g.get("m7").add("m6");

        // Cross-SCC edges (DAG edges in the condensation)
        g.get("m1").add("m4");
        g.get("m2").add("m5");
        g.get("m4").add("m6");
        g.get("m5").add("m6");
        g.get("m7").add("m8");
        g.get("m8").add("m9");
        g.get("m3").add("m9");

        return g;
    }

    // ── Print adjacency list ──────────────────────────────────────────────────
    static void printGraph(Map<String, List<String>> g) {
        System.out.println("Adjacency list:");
        for (Map.Entry<String, List<String>> e : g.entrySet()) {
            System.out.println("  " + e.getKey() + " → " + e.getValue());
        }
    }

    // ── Condensation DAG (topological order of SCCs) ──────────────────────────
    static void printCondensation(List<List<String>> sccs) {
        System.out.println("\n── Condensation DAG (each SCC = one super-node) ──");
        System.out.println("SCCs in reverse topological order (Tarjan gives reverse-topo naturally):");
        for (int i = 0; i < sccs.size(); i++) {
            List<String> scc = sccs.get(i);
            String tag = scc.size() > 1 ? "  *** REFACTORING CANDIDATE" : "";
            System.out.printf("  SCC-%d: %-30s size=%d%s%n",
                    i + 1, scc.toString(), scc.size(), tag);
        }
    }

    // ── Main ──────────────────────────────────────────────────────────────────
    public static void main(String[] args) {

        System.out.println("=== Q3: Tarjan's SCC — Linux Kernel Module Dependencies ===\n");

        Map<String, List<String>> graph = buildGraph();
        printGraph(graph);

        System.out.println("\n── DFS Trace (alphabetical vertex order) ──\n");

        TarjanSCC tarjan = new TarjanSCC(graph);
        List<List<String>> sccs = tarjan.findSCCs();

        System.out.println("── All SCCs found ──");
        for (int i = 0; i < sccs.size(); i++) {
            System.out.println("  SCC " + (i + 1) + ": " + sccs.get(i));
        }

        printCondensation(sccs);

        System.out.println("\n── Refactoring Recommendations ──");
        for (List<String> scc : sccs) {
            if (scc.size() > 1) {
                System.out.println("  Modules " + scc
                        + " have a cyclic dependency — merge into one module OR");
                System.out.println("  extract a common abstraction/interface to break the cycle.");
            }
        }

        // Find the most problematic SCC (largest)
        List<String> biggest = sccs.stream()
                .max(Comparator.comparingInt(List::size))
                .orElse(Collections.emptyList());
        System.out.println("\n  Most problematic SCC (largest): " + biggest
                + " (size=" + biggest.size() + ")");

        System.out.println("\n── Complexity Summary ──");
        System.out.println("Tarjan's SCC time : O(V + E)");
        System.out.println("This graph        : V=9, E=12 → O(21) operations");
        System.out.println("Space             : O(V) for disc[], low[], stack, onStack");
    }
}
