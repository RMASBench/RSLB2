package RSLBench.Helpers;

import java.util.ArrayList;
import java.util.Locale;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardWorldModel;
import RSLBench.Params;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ArrayStuff
{
    private static final Logger Logger = LogManager.getLogger(ArrayStuff.class);

    /*
     * Transpose Matrix
     */
    final static public double[][] transpose(double[][] matrix)
    {
        double oldM[][] = new double[matrix.length][matrix[0].length];
        double newM[][] = new double[matrix[0].length][matrix.length];
        // Copy matrix
        for (int i = 0; i < matrix.length; i++)
        {
            for (int j = 0; j < matrix[0].length; j++)
            {
                oldM[i][j] = matrix[i][j];
            }
        }
        for (int i = 0; i < oldM.length; i++)
        {
            for (int j = 0; j < oldM[0].length; j++)
            {
                newM[j][i] = oldM[i][j];
            }
        }
        return newM;
    }

    /*
     * Transform a utility matrix to a cost matrix.
     */
    final static public double[][] transformUtilityToCost(double[][] matrix)
    {
        double oldM[][] = new double[matrix.length][matrix[0].length];
        double newM[][] = new double[matrix.length][matrix[0].length];
        // Copy matrix
        double minVal = Double.MAX_VALUE;
        double maxVal = Double.MIN_VALUE;
        for (int i = 0; i < matrix.length; i++)
        {
            for (int j = 0; j < matrix[0].length; j++)
            {
                if (matrix[i][j] < minVal)
                    minVal = matrix[i][j];
                if (matrix[i][j] > maxVal)
                    maxVal = matrix[i][j];
                oldM[i][j] = matrix[i][j];
            }
        }
        for (int i = 0; i < oldM.length; i++)
        {
            for (int j = 0; j < oldM[0].length; j++)
            {
                newM[i][j] = maxVal - oldM[i][j];
            }
        }
        return newM;
    }

    /*
     * Make a square matrix and fill new elements with value.
     */
    public final static double[][] makeSquareMatrix(double[][] matrix, double value)
    {
        int rows = matrix.length;
        int cols = matrix[0].length;
        if (rows == cols)
            return matrix;
        int N = cols;
        if (rows > cols)
        {
            N = rows;
        }
        double newM[][] = new double[N][N];
        for (int i = 0; i < N; i++)
        {
            for (int j = 0; j < N; j++)
            {
                newM[i][j] = value;
            }
        }

        // Copy matrix
        for (int i = 0; i < matrix.length; i++)
        {
            for (int j = 0; j < matrix[0].length; j++)
            {
                newM[i][j] = matrix[i][j];
            }
        }
        return newM;
    }

    /*
     * Print out a matrix of Doubles.
     */
    public final static void printMatrixDouble(double[][] A)
    {
        Locale.setDefault(Locale.US);
        System.out.format("Matrix of size rows=%d cols=%d: \n", A.length, A[0].length);
        for (int i = 0; i < A.length; i++)
        {
            for (int j = 0; j < A[0].length; j++)
            {
                if (A[i][j] == Double.MAX_VALUE)
                    System.out.format("inf ");
                else if (A[i][j] == -Double.MAX_VALUE)
                    System.out.format("-inf ");
                else
                    System.out.format("%1.2f ", A[i][j]);
            }
            System.out.println();
        }
        System.out.println();
    }

    /*
     * Print out a matrix of Integers.
     */
    public final static void printMatrixInt(int[][] A)
    {
        System.out.format("Matrix of size rows=%d cols=%d: \n", A.length, A[0].length);
        for (int i = 0; i < A.length; i++)
        {
            for (int j = 0; j < A[0].length; j++)
            {
                if (A[i][j] == Integer.MAX_VALUE)
                    System.out.format("inf ");
                else if (A[i][j] == -Integer.MAX_VALUE)
                    System.out.format("-inf ");
                else
                    System.out.format("%d ", A[i][j]);
            }
            System.out.println();
        }
        System.out.println();
    }

 

    public static int[] assignmentMatrixToAssignment(double[][] A, ArrayList<EntityID> agents, ArrayList<Building> targets, StandardWorldModel world)
    {
        // Assign agents to their targets
        int ass[] = new int[agents.size()];
        for (int i = 0; i < agents.size(); i++)
            ass[i] = -1;

        // Assign each agent to the best F1 building then to the best F2
        // building, etc.
        for (int a = 0; a < agents.size(); a++) {
            double max = -Double.MAX_VALUE;
            int target = -1;

            // First, only consider newly ignited targets
            if (Params.OPTIMIZE_ASSIGNMENT) {
                for (int t = 0; t < targets.size(); t++) {
                    Building b = targets.get(t);
                    double f = b.getFieryness();
                    if (f != 1)
                    	continue;
                    else if (A[a][t] > max) {
                    	max = A[a][t];
                    	target = t;
                    }
                }
            }

            // Second, consider all the others
            if (target == -1) {
                for (int t = 0; t < targets.size(); t++) {
                    if (A[a][t] > max) {
                        max = A[a][t];
                        target = t;
                    }
                }
            }
            if (target >= 0) {
                ass[a] = target;
            } else {
                Logger.warn("HAVE NO TARGET FOR AGENT " + a);
            }
        }
        return ass;
    }
}