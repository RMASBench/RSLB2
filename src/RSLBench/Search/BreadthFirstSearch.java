package RSLBench.Search;

import RSLBench.Algorithms.BMS.RSLBenchCommunicationAdapter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;


import rescuecore2.worldmodel.EntityID;

public class BreadthFirstSearch extends AbstractSearchAlgorithm
{
    private static final Logger Logger = LogManager.getLogger(BreadthFirstSearch.class);

    @Override
    public List<Area> search(Area start, Collection<Area> goals, Graph graph, DistanceInterface distanceMatrix)
    {
        List<Area> open = new LinkedList<>();
        Map<Area, Area> ancestors = new HashMap<>();
        open.add(start);
        Area next = null;
        boolean found = false;
        ancestors.put(start, start);
        do
        {
            next = open.remove(0);

            if (isGoal(next, goals)) {
                found = true;
                break;
            }
            
            Collection<Area> neighbours = graph.getNeighbors(next);
            if (neighbours.isEmpty()) {
                continue;
            }
            
            for (Area neighbour : neighbours) {
                
                if (isGoal(neighbour, goals)) {
                    ancestors.put(neighbour, next);
                    next = neighbour;
                    found = true;
                    break;
                } else {
                    if (!ancestors.containsKey(neighbour)) {
                        open.add(neighbour);
                        ancestors.put(neighbour, next);
                    }
                }
            }

        } while (!found && !open.isEmpty());
        
        if (!found) {
            // No path
            return null;
        }
        
        // Walk back from goal to start
        Area current = next;
        List<EntityID> blockers = new LinkedList<>();
        List<Area> path = new LinkedList<>();
        do
        {
            path.add(0, current);

            // Check whether the path is blocked
            if (current.isBlockadesDefined()) {
                for (EntityID b : current.getBlockades()) {
                    Blockade blockade = (Blockade)(graph.getWorld().getEntity(b));
                    if (blockade.getRepairCost() > 0) {
                        blockers.add(0, b);
                    }
                }
            }

            current = ancestors.get(current);
            if (current == null)
            {
                throw new RuntimeException("Found a node with no ancestor! Something is broken.");
            }
        } while (current != start);
        return path;
    }

    private boolean isGoal(Area e, Collection<Area> test)
    {
        return test.contains(e);
    }

}
