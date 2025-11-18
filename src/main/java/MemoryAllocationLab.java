import java.io.*;
import java.util.*;

public class MemoryAllocationLab {

    private static ArrayList<MemoryBlock> memory = new ArrayList<>();
    private static int totalMemory = 0;
    private static int successfulAllocations = 0;
    private static int failedAllocations = 0;

    public static class MemoryBlock {
        int start;
        int size;
        String processName;

        public MemoryBlock(int start, int size, String processName) {
            this.start = start;
            this.size = size;
            this.processName = processName;
        }

        public boolean isFree() {
            return processName == null;
        }
    }

    private static void processRequests(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line = br.readLine();
            totalMemory = Integer.parseInt(line.trim());

            System.out.println("Reading from: " + filename);
            System.out.println("Total Memory: " + totalMemory + " KB");
            System.out.println("----------------------------------------");
            System.out.println("\nProcessing requests...\n");

            memory.clear();
            memory.add(new MemoryBlock(0, totalMemory, null));

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(" ");

                if (parts[0].equals("REQUEST")) {
                    String process = parts[1];
                    int size = Integer.parseInt(parts[2]);
                    allocate(process, size);
                } else if (parts[0].equals("RELEASE")) {
                    String process = parts[1];
                    deallocate(process);
                }
            }

        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }

    private static void allocate(String processName, int size) {
        for (int i = 0; i < memory.size(); i++) {
            MemoryBlock block = memory.get(i);

            if (block.isFree() && block.size >= size) {
                int remaining = block.size - size;

                block.processName = processName;
                block.size = size;

                if (remaining > 0) {
                    MemoryBlock leftover = new MemoryBlock(block.start + size, remaining, null);
                    memory.add(i + 1, leftover);
                }

                successfulAllocations++;
                System.out.println("REQUEST " + processName + " " + size + " KB → SUCCESS");
                return;
            }
        }

        failedAllocations++;
        System.out.println("REQUEST " + processName + " " + size + " KB → FAILED (Insufficient Memory)");
    }

    private static void deallocate(String processName) {
        for (int i = 0; i < memory.size(); i++) {
            MemoryBlock block = memory.get(i);

            if (!block.isFree() && block.processName.equals(processName)) {
                block.processName = null;
                System.out.println("RELEASE " + processName + " → SUCCESS");
                mergeAdjacentBlocks();
                return;
            }
        }
        System.out.println("RELEASE " + processName + " → FAILED (Process not found)");
    }

    private static void mergeAdjacentBlocks() {
        for (int i = 0; i < memory.size() - 1; i++) {
            MemoryBlock current = memory.get(i);
            MemoryBlock next = memory.get(i + 1);

            if (current.isFree() && next.isFree()) {
                current.size += next.size;
                memory.remove(i + 1);
                i--;
            }
        }
    }

    private static void displayStatistics() {
        int allocated = 0;
        int free = 0;
        int largestFree = 0;
        int numProcesses = 0;
        int numFreeBlocks = 0;

        for (MemoryBlock block : memory) {
            if (block.isFree()) {
                free += block.size;
                numFreeBlocks++;
                if (block.size > largestFree) largestFree = block.size;
            } else {
                allocated += block.size;
                numProcesses++;
            }
        }

        double allocatedPct = (allocated * 100.0) / totalMemory;
        double freePct = (free * 100.0) / totalMemory;
        double fragmentation = (free == 0 ? 0 : ((free - largestFree) * 100.0 / free));

        System.out.println("\n========================================");
        System.out.println("Final Memory State");
        System.out.println("========================================");

        int index = 1;
        for (MemoryBlock block : memory) {
            int end = block.start + block.size - 1;
            if (block.isFree()) {
                System.out.printf("Block %d: [%d-%d]  FREE (%d KB)\n", index, block.start, end, block.size);
            } else {
                System.out.printf("Block %d: [%d-%d]  %s (%d KB) - ALLOCATED\n",
                        index, block.start, end, block.processName, block.size);
            }
            index++;
        }

        System.out.println("\n========================================");
        System.out.println("Memory Statistics");
        System.out.println("========================================");
        System.out.println("Total Memory:           " + totalMemory + " KB");
        System.out.printf("Allocated Memory:       %d KB (%.2f%%)\n", allocated, allocatedPct);
        System.out.printf("Free Memory:            %d KB (%.2f%%)\n", free, freePct);
        System.out.println("Number of Processes:    " + numProcesses);
        System.out.println("Number of Free Blocks:  " + numFreeBlocks);
        System.out.println("Largest Free Block:     " + largestFree + " KB");
        System.out.printf("External Fragmentation: %.2f%%\n", fragmentation);
        System.out.println();
        System.out.println("Successful Allocations: " + successfulAllocations);
        System.out.println("Failed Allocations:     " + failedAllocations);
        System.out.println("========================================");
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java MemoryAllocationLab <input_file>");
            return;
        }

        System.out.println("========================================");
        System.out.println("Memory Allocation Simulator (First-Fit)");
        System.out.println("========================================\n");

        processRequests(args[0]);
        displayStatistics();
    }
}
