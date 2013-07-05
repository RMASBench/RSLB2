/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Helpers.Utility;

import RSLBench.Params;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rescuecore2.standard.entities.Building;
import rescuecore2.worldmodel.EntityID;

/**
 * Utility function that mimicks the pre-utility functions evaluation.
 * 
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
public class FirstUtilityFunction extends AbstractUtilityFunction {
    private static final Logger Logger = LogManager.getLogger(FirstUtilityFunction.class);
    
    @Override
    public double getUtility(EntityID agent, EntityID target) {
        EntityID location = agentLocations.get(agent);
        if (location == null) {
            Logger.warn("Cannot find location for agent " + agent);
            location = agent;
        }

        Building b = (Building) world.getEntity(target);
        double f = b.getFieryness();
        double utility = 1.0;
        if (f == 1.0) {
            utility = 1E9;
        } else if (f == 2.0) {
            utility = 1E6;
        } else if (f == 3.0) {
            utility = 100.0;
        }

        double distance = world.getDistance(location, target);
        utility = utility / Math.pow(distance * Params.TRADE_OFF_FACTOR_TRAVEL_COST_AND_UTILITY, 2.0);
        return utility;
    }

    @Override
    public int getRequiredAgentCount(EntityID target) {
        Building b = (Building) world.getEntity(target);
        if (b == null) {
            Logger.error("Requested the agent count of a non-building target {}.", target);
            throw new RuntimeException("Requested the agent count of a non-building target");
        }
        
        double area = (double) b.getTotalArea();
        double neededAgents = Math.ceil(area / (double) Params.AREA_COVERED_BY_FIRE_BRIGADE);

        if (b.getFieryness() == 1) {
            neededAgents *= 1.5;
        } else if (b.getFieryness() == 2) {
            neededAgents *= 3.0;
        }
        //Logger.debugColor("BASE: " + base + " | FIERYNESS: " + b.getFieryness() + " | NEEEDED AGENTS: " + neededAgents, Logger.BG_RED);

        int result = (int) Math.round(neededAgents);
        if (result < 1) {
            Logger.warn("Computed {} required agents for a fire. Correcting to 1.", result);
            result = 1;
        }
        return result;
    }
    
}