/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Helpers.Utility;

import RSLBench.Params;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

/**
 * Utility function that mimicks the pre-utility functions evaluation.
 * 
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
public class IgnoreFieryness extends AbstractUtilityFunction {
    private static final Logger Logger = LogManager.getLogger(IgnoreFieryness.class);

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
        StandardEntity e = world.getEntity(target);
        if (e == null || !(e instanceof Building)) {
            Logger.error("Requested the agent count of a non-building target.");
            System.exit(1);
        }
        
        Building b = (Building)e;
        double area = b.getTotalArea();
        double neededAgents = Math.ceil(area / (double) Params.AREA_COVERED_BY_FIRE_BRIGADE);
        
        // The fireyness only indicates wether the building is burning or not,
        // and for how long (despite the suggestive names).
        switch(b.getFierynessEnum()) {
            case HEATING:
            case BURNING:
            case INFERNO:
                break;
            default:
                neededAgents = 0;
                break;
        }

        return (int) Math.round(neededAgents);
    }
    
}