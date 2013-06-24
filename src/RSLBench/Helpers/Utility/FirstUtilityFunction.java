/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Helpers.Utility;

import RSLBench.Helpers.Logger;
import RSLBench.Params;
import rescuecore2.standard.entities.Building;
import rescuecore2.worldmodel.EntityID;

/**
 * Utility function that mimicks the pre-utility functions evaluation.
 * 
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
public class FirstUtilityFunction extends AbstractUtilityFunction {

    @Override
    public double getUtility(EntityID agent, EntityID target) {
        EntityID location = agentLocations.get(agent);
        if (location == null) {
            Logger.warn("Cannot find location for agent " + agent);
            location = agent;
        }
        double distance = world.getDistance(location, target);
        return 100.0 / Math.pow(distance * Params.TRADE_OFF_FACTOR_TRAVEL_COST_AND_UTILITY, 2.0);
    }

    @Override
    public int getRequiredAgentCount(EntityID target) {
        Building b = (Building) world.getEntity(target);
        if (b == null) {
            Logger.fatal("Requested the agent count of a non-building target.");
            System.exit(1);
        }
        
        double area = (double) b.getTotalArea();
        double neededAgents = Math.ceil(area / (double) Params.AREA_COVERED_BY_FIRE_BRIGADE);

        if (b.getFieryness() == 1) {
            neededAgents *= 1.5;
        } else if (b.getFieryness() == 2) {
            neededAgents *= 3.0;
        }
        //Logger.debugColor("BASE: " + base + " | FIERYNESS: " + b.getFieryness() + " | NEEEDED AGENTS: " + neededAgents, Logger.BG_RED);

        return (int) Math.round(neededAgents);
    }
    
}