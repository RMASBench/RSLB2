package RSLBench.Algorithms.DSA;

import RSLBench.Helpers.Logging.Markers;
import RSLBench.Helpers.Utility.UtilityMatrix;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rescuecore2.worldmodel.EntityID;

public class TargetScores {
    private static final Logger Logger = LogManager.getLogger(TargetScores.class);
    
    private HashMap<EntityID, Integer> _numAssignedAgents;
    private UtilityMatrix utilities;
    private EntityID agent;

    public TargetScores(EntityID agent, UtilityMatrix utilities) {
        _numAssignedAgents = new HashMap<>();
        this.utilities = utilities;
        this.agent = agent;
    }

    public void increaseAgentCount(EntityID target) {
        Integer count = _numAssignedAgents.get(target);
        if (count == null) {
            _numAssignedAgents.put(target, 1);
        } else {
            _numAssignedAgents.put(target, count+1);
        }
    }

    public double computeScore(EntityID target) {
        int nAgents = 1;
        if (_numAssignedAgents.containsKey(target)) {
            nAgents = _numAssignedAgents.get(target);
        }
        double penalty = utilities.getUtilityPenalty(target, nAgents);
        return utilities.getUtility(agent, target) - penalty;
    }

    public void resetAssignments() {
        Iterator<Entry<EntityID, Integer>> it = _numAssignedAgents.entrySet().iterator();
        while (it.hasNext()) {
            Entry<EntityID, Integer> pair = it.next();
            pair.setValue(0);
        }
    }
}