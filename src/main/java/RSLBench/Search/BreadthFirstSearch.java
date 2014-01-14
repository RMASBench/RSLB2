package RSLBench.Search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
    public SearchResults search(Area start, Collection<Area> goals, Graph graph, DistanceInterface distanceMatrix)
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
        SearchResults result = new SearchResults();
        List<Blockade> blockers = new ArrayList<>();
        List<Area> path = new ArrayList<>();
        List<EntityID> entityPath = new ArrayList<>();
        do
        {
            path.add(current);
            entityPath.add(current.getID());
            addBlockers(graph, blockers, current);

            current = ancestors.get(current);
            if (current == null)
            {
                throw new RuntimeException("Found a node with no ancestor! Something is broken.");
            }
        } while (current != start);
        addBlockers(graph, blockers, start);

        Collections.reverse(path);
        Collections.reverse(entityPath);
        Collections.reverse(blockers);

        result.setPathAreas(path);
        result.setPathIds(entityPath);
        result.setPathBlocks(blockers);
        return result;
    }

    private boolean isGoal(Area e, Collection<Area> test)
    {
        return test.contains(e);
    }

}
