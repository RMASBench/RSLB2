package RSLBench.Algorithms.DSA;

import RSLBench.Helpers.Logging.Markers;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rescuecore2.worldmodel.EntityID;

public class TargetScores {
    private static final Logger Logger = LogManager.getLogger(TargetScores.class);
    
    private HashMap<EntityID, Integer> _numAssignedAgents;
    private HashMap<EntityID, Integer> _numRequiredAgents;

    public TargetScores() {
        _numRequiredAgents = new HashMap<>();
        _numAssignedAgents = new HashMap<>();
    }

    public void initializeTarget(EntityID target, int requiredAgents) {
        _numRequiredAgents.put(target, requiredAgents);
        _numAssignedAgents.put(target, 0);
        Logger.debug(Markers.BLUE, "TargetScores: intitialize target " + target);
    }

    public void increaseAgentCount(EntityID target) {
        if (!_numAssignedAgents.containsKey(target)) {
            Logger.warn("Target " + target + " has not been initialized!");
            return;
        }
        _numAssignedAgents.put(target, _numAssignedAgents.get(target) + 1);
    }

    public double computeScore(EntityID target, double targetUtility) {
        if (_numAssignedAgents.get(target) < _numRequiredAgents.get(target)) {
            return targetUtility;
        }
        return -Double.MAX_VALUE;
    }

    public void resetAssignments() {
        Iterator<Entry<EntityID, Integer>> it = _numAssignedAgents.entrySet().iterator();
        while (it.hasNext()) {
            Entry<EntityID, Integer> pair = it.next();
            pair.setValue(0);
        }
    }
}