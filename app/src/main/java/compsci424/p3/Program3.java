/* COMPSCI 424 Program 3
 * Name: Jack Cerni
 * 
 * This is a template. Program3.java *must* contain the main class
 * for this program. 
 * 
 * You will need to add other classes to complete the program, but
 * there's more than one way to do this. Create a class structure
 * that works for you. Add any classes, methods, and data structures
 * that you need to solve the problem and display your solution in the
 * correct format.
 */

package compsci424.p3;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import compsci424.p3.Proc;

/**
 * Main class for this program. To help you get started, the major
 * steps for the main program are shown as comments in the main
 * method. Feel free to add more comments to help you understand
 * your code, or for any reason. Also feel free to edit this
 * comment to be more helpful.
 */
public class Program3 {
    // Declare any class/instance variables that you need here.
    static int numResources, numProcesses;
    static int[] available, total;
    static int[][] max, allocation, request;

    static Semaphore lock = new Semaphore(1);

    /**
     * @param args Command-line arguments. 
     * 
     * args[0] should be a string, either "manual" or "auto". 
     * 
     * args[1] should be another string: the path to the setup file
     * that will be used to initialize your program's data structures. 
     * To avoid having to use full paths, put your setup files in the
     * top-level directory of this repository.
     * - For Test Case 1, use "424-p3-test1.txt".
     * - For Test Case 2, use "424-p3-test2.txt".
     */
    public static void main(String[] args) {
        // Code to test command-line argument processing.
        // You can keep, modify, or remove this. It's not required.
        /*if (args.length < 2) {
            System.err.println("Not enough command-line arguments provided, exiting.");
            return;
        }
        System.out.println("Selected mode: " + args[0]);
        System.out.println("Setup file location: " + args[1]);*/

        // 1. Open the setup file using the path in args[1]
        String currentLine;
        BufferedReader setupFileReader;
        try {
            setupFileReader = new BufferedReader(new FileReader(args[1]));
        } catch (FileNotFoundException e) {
            System.err.println("Cannot find setup file at " + args[1] + ", exiting.");
            return;
       }

        // 2. Get the number of resources and processes from the setup
        // file, and use this info to create the Banker's Algorithm
        // data structures

        // For simplicity's sake, we'll use one try block to handle
        // possible exceptions for all code that reads the setup file.
        try {
            // Get number of resources
            currentLine = setupFileReader.readLine();
            if (currentLine == null) {
                System.err.println("Cannot find number of resources, exiting.");
                setupFileReader.close();
                return;
            }
            else {
                numResources = Integer.parseInt(currentLine.split(" ")[0]);
                System.out.println(numResources + " resources");
            }
 
            // Get number of processes
            currentLine = setupFileReader.readLine();
            if (currentLine == null) {
                System.err.println("Cannot find number of processes, exiting.");
                setupFileReader.close();
                return;
            }
            else {
                numProcesses = Integer.parseInt(currentLine.split(" ")[0]);
                System.out.println(numProcesses + " processes");
            }

            // Create the Banker's Algorithm data structures, in any
            // way you like as long as they have the correct size

            available = new int[numResources];
            total = new int[numResources];
            max = new int[numProcesses][numResources];
            allocation = new int[numProcesses][numResources];
            request = new int[numProcesses][numResources];

            // 3. Use the rest of the setup file to initialize the
            // data structures
            String line;
            String[] nums;

            setupFileReader.readLine();
            line = setupFileReader.readLine();
            nums = line.trim().split("\\s+");
            for(int r = 0; r < numResources; r++) {
                available[r] = Integer.parseInt(nums[r]);
            }

            setupFileReader.readLine();
            for(int p = 0; p < numProcesses; p++) {
                line = setupFileReader.readLine();
                nums = line.trim().split("\\s+");

                for(int r = 0; r < numResources; r++) {
                    max[p][r] = Integer.parseInt(nums[r]);
                }
            }
            
            setupFileReader.readLine();
            for(int p = 0; p < numProcesses; p++) {
                line = setupFileReader.readLine();
                nums = line.trim().split("\\s+");

                for(int r = 0; r < numResources; r++) {
                    allocation[p][r] = Integer.parseInt(nums[r]);
                }
            }

            for(int r = 0; r < numResources; r++) {
                int runningTotal = 0;
                for(int p = 0; p < numProcesses; p++) {
                    runningTotal += allocation[p][r];
                }
                runningTotal += available[r];
                total[r] = runningTotal;
            }

            setupFileReader.close(); // done reading the file, so close it
        }
        catch (IOException e) {
            System.err.println("Something went wrong while reading setup file "
            + args[1] + ". Stack trace follows. Exiting.");
            e.printStackTrace(System.err);
            System.err.println("Exiting.");
            return;
        }

        // 4. Check initial conditions to ensure that the system is 
        // beginning in a safe state: see "Check initial conditions"
        // in the Program 3 instructions
        for(int p = 0; p < numProcesses; p++) {
            for(int r = 0; r < numResources; r++) {
                if(allocation[p][r] > max[p][r]) {
                    System.out.println("ERROR: Max claim for resource " + r + " by process " + p + " exceeds " + allocation[p][r] + ".");
                    return;
                }
            }
        }

        if(!graphReduces(available, max, allocation, request, numProcesses, numResources)) {
            System.out.println("ERROR: System is not in a safe state.");
            return;
        }

        // 5. Go into either manual or automatic mode, depending on
        // the value of args[0]; you could implement these two modes
        // as separate methods within this class, as separate classes
        // with their own main methods, or as additional code within
        // this main method.
        
        if(args[0].equals("manual")) {
            bankersManual(available, max, allocation, request, numProcesses, numResources);
        } else if(args[0].equals("auto")) {
            bankersAuto(available, max, allocation, request, numProcesses, numResources);
        } else {
            System.out.println("ERROR: Invalid mode input.");
            return;
        }
    }

    private static void bankersManual(int[] available, int[][] max, int[][] allocation, int[][] request, int p, int r) {
        System.out.println("\nManual Mode: Enter commands in the form '[request/release] [I] of [J] for [K]', followed by 'end' to run.");
        System.out.println("Example: 'request 3 of 1 for 0' requests 3 units of resource 1 for process 0.\n");

        Scanner in = new Scanner(System.in);
        ArrayList<String> args = new ArrayList<String>();

        String lastArg; // Get user instructions
        while(true) {
            lastArg = in.nextLine();
            if(lastArg.equals("end")) {
                System.out.println();
                break;
            }
            args.add(lastArg);
        }

        in.close();

        String[] instructions = new String[args.size()]; // Break down raw input strings into more usable, separated arrays
        int[] units = new int[args.size()];
        int[] resources = new int[args.size()];
        int[] processes = new int[args.size()];

        String[] chunks = new String[6];
        for(int i = 0; i < args.size(); i++) {
            chunks = args.get(i).split(" ");

            instructions[i] = chunks[0];
            units[i] = Integer.parseInt(chunks[1]);
            resources[i] = Integer.parseInt(chunks[3]);
            processes[i] = Integer.parseInt(chunks[5]);

            request[processes[i]][resources[i]] += units[i];
        }

        int[] tempAvailable;
        int[][] tempAllocation, tempRequest;

        String instruction;
        int unit, resource, process;

        for(int i = 0; i < args.size(); i++) {
            instruction = instructions[i];
            unit = units[i];
            resource = resources[i];
            process = processes[i];

            tempAvailable = available;
            tempAllocation = allocation;
            tempRequest = request;

            if(instruction.equals("request")) {
                tempAvailable[resource] -= unit;
                tempAllocation[process][resource] += unit;
                tempRequest[process][resource] -= unit;

                System.out.print("Process " + process + " " + instruction + "s " + unit + " unit(s) of resource " + resource + ": ");
                if(graphReduces(tempAvailable, max, tempAllocation, tempRequest, p, r)) {
                    System.out.print("granted.\n");

                    available = tempAvailable;
                    allocation = tempAllocation;
                    request = tempRequest;
                } else {
                    System.out.print("denied.\n");
                }
            } else if(instruction.equals("release")) {
                tempAvailable[resource] += unit;
                tempAllocation[process][resource] -= unit;

                System.out.println("Process " + process + " " + instruction + "s " + unit + " unit(s) of resource " + resource + ".");

                available = tempAvailable;
                allocation = tempAllocation;
            } else {
                System.out.println("ERROR: Instruction '" + instruction + "' of request #" + i + " is invalid.");
                return;
            }
        }
    }

    private static void bankersAuto(int[] available, int[][] max, int[][] allocation, int[][] request, int p, int r) {
        for(int i = 0; i < p; i++) { // Create and start a thread for each process 
             new Proc(i).start();
        }
        //System.out.println("TOTAL:" + total[0] + " " + total[1] + " " + total[2] + " " + total[3]);
    }

    public static boolean graphReduces(int[] available, int[][] max, int[][] allocation, int[][] request, int p, int r) {
        int[][] potential = new int[p][r]; // Potential requests for each process. I decided to name this "potential" instead of "need"
        int[] work = new int[r]; // A temporary copy of available, to try a graph reduction before committing to it
        boolean[] finish = new boolean[p]; // If a process has been "removed" from the graph, its index is set to true

        for(int i = 0; i < r; i++) {
            work[i] = available[i];
        }

        for(int i = 0; i < p; i++) { // Initialize finish to false (all processes present)
            finish[i] = false;
        }

        for(int x = 0; x < p; x++) { // Calculate potential requests for each process
            for(int y = 0; y < r; y++) {
                potential[x][y] = max[x][y] - allocation[x][y];
            }
        }

        int removed = 0; // Track number of processes removed from the graph
        while(removed < p) {
            boolean state = false;
            for(int x = 0; x < p; x++) { // Check each process
                if(finish[x] == false) { // If this process isn't finished, aka still in graph
                    int index;
                    for(index = 0;  index < r; index++) {
                        if(potential[x][index] > work[index]) { // Check to see if this process's potential requests can be fulfilled by the currently available resources (work)
                            break;
                        }
                    }

                    if(index == r) { // If the resources in work were sufficient for the process's potential requests
                        for(int y = 0; y < r; y++) {
                            work[y] += allocation[x][y]; // Put the resources that were allocated to the request back in work
                        }

                        finish[x] = true; // Remove this process from the graph
                        state = true;
                        removed++;
                    }
                }
            }

            if(state == false) { // No safe way to proceed
                break;
            }
        }

        if(removed < p) { // No safe way to proceed
            return false;
        } else { // Safe to proceed
            return true;
        }
    }
}

class Proc extends Thread { // I tried to put this class in a separate file, but I couldn't figure out how to compile & run both files at the same time from cmd. This was the only way I could get it to work
    int p, r, processID;

    public Proc(int processID) {
        this.p = Program3.numProcesses;
        this.r = Program3.numResources;
        this.processID = processID;
    }

    public void run() {
        String instruction;
        int unit, resource;
        int[] tempAvailable;
        int[][] tempAllocation, tempRequest;

        int val = getRand(0, 1); // Generate either a 0 or a 1. 0 represents a request, and 1 a release

        
        for(int i = 0; i < 6; i++) {
            tempAvailable = Program3.available;
            tempAllocation = Program3.allocation;
            tempRequest = Program3.request;
            //System.out.println("Available: " + Program3.available[2]);

            if(val == 0) { // A request was chosen
                instruction = "request";
                val++; // Make the next instruction a release
                resource = getRand(0, r - 1); // Pick a resource
                unit = getRand(1, Program3.max[processID][resource]); // Pick a quantity of that resource to request
                //System.out.println("Unit: " + unit);

                tempAvailable[resource] -= unit;
                tempAllocation[processID][resource] += unit;
                tempRequest[processID][resource] -= unit;
                
                if(Program3.graphReduces(tempAvailable, Program3.max, tempAllocation, tempRequest, p, r)) {
                    System.out.println("Process " + processID + " " + instruction + "s " + unit + " unit(s) of resource " + resource + ": granted");

                    try { // Attempt to acquire the mutex
                        Program3.lock.acquire();
                    } catch (InterruptedException e) {
                        return;
                    }

                    Program3.available = tempAvailable; // Write to the shared arrays
                    Program3.allocation = tempAllocation;
                    Program3.request = tempRequest;

                    Program3.lock.release(); // Release the mutex
                } else {
                    System.out.println("Process " + processID + " " + instruction + "s " + unit + " unit(s) of resource " + resource + ": denied");
                }
            } else if(val == 1) { // A release was chosen
                instruction = "release";
                val--; // Make the next instruction a request
                resource = getRand(0, r - 1); // Pick a resource
                unit = getRand(1, Program3.allocation[processID][resource]); // Pick a quantity of that resource to release
                tempAvailable[resource] += unit;

                System.out.println("Process " + processID + " " + instruction + "s " + unit + " unit(s) of resource " + resource + ".");

                try { // Attempt to acquire the mutex
                    Program3.lock.acquire();
                } catch (InterruptedException e) {
                    return;
                }

                Program3.available = tempAvailable; // Write to the shared available array

                Program3.lock.release(); // Release the mutex
            }
        }

        interrupt(); // Once all six actions have been completed, kill this thread
    }

    private int getRand(int min, int max) {
        return (int) ((((max - min) + 1) * Math.random()) + min);
    }
}