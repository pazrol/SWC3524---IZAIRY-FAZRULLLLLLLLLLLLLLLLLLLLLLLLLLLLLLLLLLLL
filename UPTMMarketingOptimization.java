import java.util.*;

// =============================================================
//  UPTM Marketing Campaign Optimization Problem (MCOP)
//  Course: SWC3524 / SWC4423
// =============================================================

public class UPTMMarketingOptimization {

    // Cost Matrix (Adjacency Matrix)
    static int[][] costMatrix = {
        {0, 15, 25, 35},
        {15, 0, 30, 28},
        {25, 30, 0, 20},
        {35, 28, 20, 0}
    };

    // Location names
    static String[] locations = {"UPTM", "City B", "City C", "City D"};

    // Static fields used by Backtracking (since helper signature is fixed)
    static int    btBestCost;
    static String btBestPath;

    // Static fields used by Divide & Conquer (since helper signature is fixed)
    static int    dcBestCost;
    static String dcBestPath;

    // =========================================================
    //  GREEDY ALGORITHM – always move to the nearest unvisited city
    // =========================================================
    public static String greedyMCOP(int[][] dist) {
        int n = dist.length;
        boolean[] visited = new boolean[n];
        int current = 0;
        visited[0] = true;
        int totalCost = 0;
        StringBuilder route = new StringBuilder(locations[0]);

        for (int step = 0; step < n - 1; step++) {
            int nearest = -1;
            int minCost = Integer.MAX_VALUE;

            for (int j = 0; j < n; j++) {
                if (!visited[j] && dist[current][j] > 0 && dist[current][j] < minCost) {
                    minCost = dist[current][j];
                    nearest = j;
                }
            }

            if (nearest == -1) break;           // no reachable city found
            visited[nearest] = true;
            totalCost += minCost;
            route.append(" -> ").append(locations[nearest]);
            current = nearest;
        }

        // Return to starting city
        totalCost += dist[current][0];
        route.append(" -> ").append(locations[0]);

        return "Greedy Route: " + route + " | Total Cost: " + totalCost;
    } // end of greedyMCOP

    // =========================================================
    //  DYNAMIC PROGRAMMING – Held-Karp bitmask DP
    // =========================================================
    public static String dynamicProgrammingMCOP(int[][] dist) {
        int n = dist.length;
        int VISITED_ALL = (1 << n) - 1;
        int[][] memo  = new int[n][1 << n];
        String[][] paths = new String[n][1 << n];   // kept for signature compatibility

        for (int[] row : memo) Arrays.fill(row, -1);

        // Compute minimum cost starting from city 0, with city 0 already visited (mask = 1)
        int minCost = dynamicProgrammingMCOPHelper(0, 1, dist, memo, VISITED_ALL, paths);

        // Reconstruct the optimal path by following memo values forward
        StringBuilder route = new StringBuilder(locations[0]);
        int pos  = 0;
        int mask = 1;

        while (mask != VISITED_ALL) {
            int best     = -1;
            int bestCost = Integer.MAX_VALUE;

            for (int next = 0; next < n; next++) {
                if ((mask & (1 << next)) == 0) {          // city 'next' not yet visited
                    int c = dist[pos][next]
                            + dynamicProgrammingMCOPHelper(next, mask | (1 << next),
                                                           dist, memo, VISITED_ALL, paths);
                    if (c < bestCost) { bestCost = c; best = next; }
                }
            }

            route.append(" -> ").append(locations[best]);
            mask |= (1 << best);
            pos   = best;
        }

        route.append(" -> ").append(locations[0]);
        return "Dynamic Programming Route: " + route + " | Total Cost: " + minCost;
    } // end of dynamicProgrammingMCOP

    private static int dynamicProgrammingMCOPHelper(int pos, int mask, int[][] dist,
                                                     int[][] memo, int VISITED_ALL,
                                                     String[][] paths) {
        // Base case: all cities visited – return cost to go back to start
        if (mask == VISITED_ALL) return dist[pos][0];

        // Return cached result
        if (memo[pos][mask] != -1) return memo[pos][mask];

        int n   = dist.length;
        int min = Integer.MAX_VALUE / 2;   // avoid overflow when adding edge costs

        for (int next = 0; next < n; next++) {
            if ((mask & (1 << next)) == 0) {          // city 'next' not yet visited
                int cost = dist[pos][next]
                           + dynamicProgrammingMCOPHelper(next, mask | (1 << next),
                                                          dist, memo, VISITED_ALL, paths);
                if (cost < min) min = cost;
            }
        }

        memo[pos][mask] = min;
        return min;
    } // end of dynamicProgrammingMCOPHelper

    // =========================================================
    //  BACKTRACKING – exhaustive search with pruning
    // =========================================================
    public static String backtrackingMCOP(int[][] dist) {
        int n = dist.length;
        boolean[] visited = new boolean[n];
        visited[0] = true;

        // Initialise best-tracking static fields
        btBestCost = Integer.MAX_VALUE;
        btBestPath = "";

        StringBuilder path = new StringBuilder(locations[0]);
        mcopBacktracking(0, dist, visited, n, 1, 0, path);

        return "Backtracking Route: " + btBestPath + " | Total Cost: " + btBestCost;
    } // end of backtrackingMCOP

    private static int mcopBacktracking(int pos, int[][] dist, boolean[] visited,
                                        int n, int count, int cost, StringBuilder path) {
        // Base case: all cities visited – close the tour
        if (count == n) {
            int totalCost = cost + dist[pos][0];
            if (totalCost < btBestCost) {
                btBestCost = totalCost;
                btBestPath = path.toString() + " -> " + locations[0];
            }
            return totalCost;
        }

        int minFound = Integer.MAX_VALUE;

        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                visited[i] = true;
                int prevLen = path.length();
                path.append(" -> ").append(locations[i]);

                int result = mcopBacktracking(i, dist, visited, n,
                                             count + 1, cost + dist[pos][i], path);
                if (result < minFound) minFound = result;

                // Undo choices (backtrack)
                path.setLength(prevLen);
                visited[i] = false;
            }
        }

        return minFound;
    } // end of mcopBacktracking

    // =========================================================
    //  DIVIDE AND CONQUER – divide unvisited cities, conquer each half
    // =========================================================
    public static String divideAndConquerMCOP(int[][] dist) {
        int n = dist.length;
        boolean[] visited = new boolean[n];
        visited[0] = true;

        // Initialise best-tracking static fields
        dcBestCost = Integer.MAX_VALUE;
        dcBestPath = "";

        StringBuilder path = new StringBuilder(locations[0]);
        divideAndConquerHelper(0, visited, 0, dist, n, path);

        return "Divide & Conquer Route: " + dcBestPath + " | Total Cost: " + dcBestCost;
    } // end of divideAndConquerMCOP

    private static int divideAndConquerHelper(int pos, boolean[] visited,
                                              int currentCost, int[][] dist,
                                              int n, StringBuilder path) {
        // Base case: all cities have been visited
        if (allVisited(visited)) {
            int totalCost = currentCost + dist[pos][0];
            if (totalCost < dcBestCost) {
                dcBestCost = totalCost;
                dcBestPath = path.toString() + " -> " + locations[0];
            }
            return totalCost;
        }

        // DIVIDE – collect unvisited cities and split into two halves
        List<Integer> unvisited = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (!visited[i]) unvisited.add(i);
        }

        int mid   = unvisited.size() / 2;
        List<Integer> leftHalf  = new ArrayList<>(unvisited.subList(0, mid));
        List<Integer> rightHalf = new ArrayList<>(unvisited.subList(mid, unvisited.size()));

        // CONQUER – explore each half (merge step is implicit through recursion)
        int minCost = Integer.MAX_VALUE;

        for (List<Integer> half : Arrays.asList(leftHalf, rightHalf)) {
            for (int next : half) {
                visited[next] = true;
                int prevLen = path.length();
                path.append(" -> ").append(locations[next]);

                int result = divideAndConquerHelper(next, visited,
                                                   currentCost + dist[pos][next],
                                                   dist, n, path);
                if (result < minCost) minCost = result;

                path.setLength(prevLen);
                visited[next] = false;
            }
        }

        return minCost;
    } // end of divideAndConquerHelper

    private static boolean allVisited(boolean[] visited) {
        for (boolean v : visited) {
            if (!v) return false;
        }
        return true;
    } // end of allVisited

    // =========================================================
    //  INSERTION SORT – sorts array in-place, returns sorted string
    // =========================================================
    public static String insertionSort(int[] arr) {
        int n = arr.length;

        for (int i = 1; i < n; i++) {
            int key = arr[i];
            int j   = i - 1;

            // Shift elements greater than key one position to the right
            while (j >= 0 && arr[j] > key) {
                arr[j + 1] = arr[j];
                j--;
            }
            arr[j + 1] = key;
        }

        return Arrays.toString(arr);
    } // end of insertionSort

    // =========================================================
    //  BINARY SEARCH – assumes the array is already sorted
    // =========================================================
    public static String binarySearch(int[] arr, int target) {
        int left  = 0;
        int right = arr.length - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (arr[mid] == target) {
                return String.valueOf(mid);        // return index as String
            } else if (arr[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return "Not found";
    } // end of binarySearch

    // =========================================================
    //  SPLAY TREE (static nested class)
    // =========================================================
    static class SplayTree {

        // Inner node class
        class Node {
            int  key;
            Node left, right;

            Node(int key) { this.key = key; }
        }

        Node root;

        // Right rotation
        private Node rightRotate(Node x) {
            Node y = x.left;
            x.left = y.right;
            y.right = x;
            return y;
        }

        // Left rotation
        private Node leftRotate(Node x) {
            Node y = x.right;
            x.right = y.left;
            y.left = x;
            return y;
        }

        // Splay: bring the node with 'key' to the root (or nearest)
        private Node splay(Node root, int key) {
            if (root == null || root.key == key) return root;

            if (key < root.key) {
                if (root.left == null) return root;

                if (key < root.left.key) {
                    // Zig-Zig (left-left)
                    root.left.left = splay(root.left.left, key);
                    root = rightRotate(root);
                } else if (key > root.left.key) {
                    // Zig-Zag (left-right)
                    root.left.right = splay(root.left.right, key);
                    if (root.left.right != null)
                        root.left = leftRotate(root.left);
                }
                return (root.left == null) ? root : rightRotate(root);

            } else {
                if (root.right == null) return root;

                if (key > root.right.key) {
                    // Zag-Zag (right-right)
                    root.right.right = splay(root.right.right, key);
                    root = leftRotate(root);
                } else if (key < root.right.key) {
                    // Zag-Zig (right-left)
                    root.right.left = splay(root.right.left, key);
                    if (root.right.left != null)
                        root.right = rightRotate(root.right);
                }
                return (root.right == null) ? root : leftRotate(root);
            }
        } // end of splay

        // Insert a key into the Splay Tree
        public void insert(int key) {
            if (root == null) { root = new Node(key); return; }

            root = splay(root, key);
            if (root.key == key) return;   // duplicate – ignore

            Node newNode = new Node(key);

            if (key < root.key) {
                newNode.right = root;
                newNode.left  = root.left;
                root.left     = null;
            } else {
                newNode.left  = root;
                newNode.right = root.right;
                root.right    = null;
            }
            root = newNode;
        } // end of insert

        // Search for a key (splays it to root if found)
        public boolean search(int key) {
            root = splay(root, key);
            return root != null && root.key == key;
        } // end of search

    } // end of SplayTree

    // =========================================================
    //  MAIN METHOD
    // =========================================================
    public static void main(String[] args) {

        // ---------- TSP Algorithms ----------
        System.out.println(greedyMCOP(costMatrix));
        System.out.println(dynamicProgrammingMCOP(costMatrix));
        System.out.println(backtrackingMCOP(costMatrix));
        System.out.println(divideAndConquerMCOP(costMatrix));

        System.out.println();

        // ---------- Sorting & Searching ----------
        int[] arr = {8, 3, 5, 1, 9, 2};
        insertionSort(arr);                                    // sorts in-place
        System.out.println("Sorted Array: " + Arrays.toString(arr));
        System.out.println("Binary Search (5 found at index): " + binarySearch(arr, 5));

        System.out.println();

        // ---------- Min-Heap ----------
        MinHeap heap = new MinHeap();
        heap.insert(10);
        heap.insert(3);
        heap.insert(15);
        System.out.println("Min-Heap Extract Min: " + heap.extractMin());

        System.out.println();

        // ---------- Splay Tree ----------
        SplayTree tree = new SplayTree();
        tree.insert(20);
        tree.insert(10);
        tree.insert(30);
        System.out.println("Splay Tree Search (10 found): " + tree.search(10));
    }

} // end of UPTMMarketingOptimization


// =============================================================
//  MIN-HEAP  (separate top-level class in the same file)
// =============================================================
class MinHeap {

    private int[]            heap;
    private int              size;
    private static final int CAPACITY = 256;

    public MinHeap() {
        heap = new int[CAPACITY];
        size = 0;
    }

    // Insert a value and restore the heap property (bubble up)
    public void insert(int val) {
        if (size >= CAPACITY) {
            System.out.println("MinHeap is full – cannot insert " + val);
            return;
        }
        heap[size] = val;
        bubbleUp(size);
        size++;
    } // end of insert

    // Remove and return the minimum element (root), then restore heap property
    public int extractMin() {
        if (size == 0) throw new NoSuchElementException("MinHeap is empty!");

        int min = heap[0];
        heap[0] = heap[size - 1];
        size--;
        heapifyDown(0);
        return min;
    } // end of extractMin

    // Peek at the minimum without removing it
    public int peekMin() {
        if (size == 0) throw new NoSuchElementException("MinHeap is empty!");
        return heap[0];
    }

    public int  getSize()  { return size; }
    public boolean isEmpty() { return size == 0; }

    // ---- private helpers ----

    private void bubbleUp(int i) {
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (heap[parent] > heap[i]) {
                swap(parent, i);
                i = parent;
            } else break;
        }
    }

    private void heapifyDown(int i) {
        int left  = 2 * i + 1;
        int right = 2 * i + 2;
        int smallest = i;

        if (left  < size && heap[left]  < heap[smallest]) smallest = left;
        if (right < size && heap[right] < heap[smallest]) smallest = right;

        if (smallest != i) {
            swap(i, smallest);
            heapifyDown(smallest);
        }
    }

    private void swap(int a, int b) {
        int temp = heap[a];
        heap[a]  = heap[b];
        heap[b]  = temp;
    }
} // end of MinHeap