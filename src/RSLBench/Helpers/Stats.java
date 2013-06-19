package RSLBench.Helpers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;

import RSLBench.Params;

import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardWorldModel;
/**
 * This class collects and writes the stats in the fileName file (default logs/basePackage_groupName_className.dat).
 */
public class Stats
{
    private static ArrayList<Integer> violatedConstraintsHistory = new ArrayList<Integer>();
    private static ArrayList<Long> computationTimeHistory = new ArrayList<Long>();
    private static ArrayList<Long> messagesInBytesHistory = new ArrayList<Long>();
    private static ArrayList<Integer> NCCCHistory = new ArrayList<Integer>();
    private static int windowSize = 20;
    
    /**
     * Writes the header of the file and the names of the metrics
     * @param fileName:name of the file 
     */
    public static void writeHeader(String fileName)
    {
        try
        {
            BufferedWriter out = new BufferedWriter(new FileWriter(fileName, true));
            out.write("#"
            		  + " StartTime: " + Params.START_EXPERIMENT_TIME 
            		  + " EndTime: " + Params.END_EXPERIMENT_TIME  
            		  + " Range: " + 	Params.SIMULATED_COMMUNICATION_RANGE
            		  + " CostTradeOff: " + Params.TRADE_OFF_FACTOR_TRAVEL_COST_AND_UTILITY
                      + " SelectTargets when Idle: " + Params.AGENT_SELECT_IDLE_TARGET);
            out.newLine();
            out.write("Time  NumBuildings  NumBurining  numOnceBurned  numDestroyed  totalAreaDestroyed violatedConstraints MAViolatedConstraints computationTime MAComputationTime numberOfMessages messagesInBytes MAMessageInBytes averageNCCC MAAverageNCCC messagesForFactorgraph ");
            out.newLine();
            out.close();
        } catch (IOException e)
        {
            System.out.println("IOException:");
            e.printStackTrace();
        }
    }
    
    
    /**
     * Writes the metrics of teh benchmark to file
     * @param fileName: the name of the file
     * @param time: the number of the step of the computation
     * @param world: the model of the world
     * @param violatedConstraints: the number of violated constraints during a step of computation
     * @param computationTime: the time of computation in millisecond for each timestep
     * @param messagesInBytes: the size of the messages in bytes per timestep
     * @param averageNccc: the average number of nccc per timestep (non concurrent contraint checks)
     * @param nMessages: the number of messages exchanged between the agents per timestep
     * @param nOtherMessages: the number of other messages (messages exchanged between the agents
     * before or after the decisional process)
     */
    public static void writeStatsToFile(String fileName, int time, StandardWorldModel world, int violatedConstraints, long computationTime, long messagesInBytes, int averageNccc, int nMessages, int nOtherMessages)
    {
        int numBuildings = 0;
        int numBurning = 0;
        int numDestroyed = 0;
        int numOnceBurned = 0;
        double totalAreaDestroyed = 0.0;
        violatedConstraintsHistory.add(violatedConstraints);
        computationTimeHistory.add(computationTime);
        messagesInBytesHistory.add(messagesInBytes);
        NCCCHistory.add(averageNccc);
        Collection<StandardEntity> allBuildings = world.getEntitiesOfType(StandardEntityURN.BUILDING);

        for (Iterator<StandardEntity> it = allBuildings.iterator(); it.hasNext();)
        {
            Building building = (Building) it.next();
            double area = building.getTotalArea();
            numBuildings++;

            if (building.isOnFire())
                numBurning++;
            if (building.getFieryness() > 3)
            {
                numDestroyed++;
                totalAreaDestroyed = totalAreaDestroyed + area;
            }
            if (building.getFieryness() > 0)
                numOnceBurned++;
        }
        totalAreaDestroyed = totalAreaDestroyed / (1000.0);

        try
        {
            BufferedWriter out = new BufferedWriter(new FileWriter(fileName, true));
            out.write(time + " " + numBuildings + " " + numBurning + " " + numOnceBurned + " " + numDestroyed + " " + totalAreaDestroyed + " " + violatedConstraints + " " + computeMovingAverage(violatedConstraintsHistory) + " " + computationTime + " " + computeMovingAverage(computationTimeHistory) + " " + nMessages + " " + messagesInBytes + " "+ + computeMovingAverage(messagesInBytesHistory) + " " + averageNccc + " " + computeMovingAverage(NCCCHistory) + " " + nOtherMessages);
            out.newLine();
            out.close();
        } catch (IOException e)
        {
            System.out.println("IOException:");
            e.printStackTrace();
        }
    }

    /**
     * Utility method that computes the moving average
     * @param data: the stat from which it is computed the moving average
     * @return the moving average of the given stat
     */
    public static float computeMovingAverage(ArrayList<? extends Number> data) {
        int windowActualSize = Math.min(data.size(), windowSize);
        if (data.get(0) instanceof Integer){
            int sum = 0;
            for (int i = 0; i < windowActualSize; i++) {
                sum += (Integer)data.get(data.size() - (i+1));

            }
        float avg = sum/windowActualSize;
        return avg;
        }  
        else {
            long sum = 0;
            for (int i = 0; i < windowActualSize; i++) {
                sum += (Long)data.get(data.size() - (i+1));

        }
        float avg = sum/windowActualSize;
        return avg;
        }
    }
}
