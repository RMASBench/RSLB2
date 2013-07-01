package RSLBench.Search;

import RSLBench.Helpers.Logging.Markers;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rescuecore2.worldmodel.EntityID;

public class AStar implements SearchAlgorithm
{
    private static final Logger Logger = LogManager.getLogger(AStar.class);
    
    @Override
    public List<EntityID> search(EntityID start, EntityID goal, Graph graph, DistanceInterface distanceMatrix)
    {
        Logger.debug(Markers.GREEN, "start single target search");
        HashSet<EntityID> goals = new HashSet<EntityID>();
        goals.add(goal);
        return search(start, goals, graph, distanceMatrix);
    }

    @Override
    public List<EntityID> search(EntityID start, Collection<EntityID> goals, Graph graph, DistanceInterface distanceMatrix)
    {
        Logger.debug(Markers.GREEN, "start multi target search");
        
        PriorityQueue<SearchNode> openList = new PriorityQueue<>();
        Map<EntityID, SearchNode> closedList = new HashMap<>();
        
        // reverse search: add all goals to the open list
        for (EntityID id: goals)
        {
            int heuristicValue = distanceMatrix.getDistance(id, start);
            openList.add(new SearchNode(id, null, 0, heuristicValue));
        }
        
        SearchNode currentNode = null;
        boolean searchComplete = false;
        Set<EntityID> neighbors;
        while (! openList.isEmpty() && ! searchComplete)
        {
            currentNode = openList.remove();
            if (currentNode.getNodeID().equals(start))
            {
                searchComplete = true;
                break;
            }
            
            // check if this is closed
            if (closedList.containsKey(currentNode.getNodeID()))
            {
                // check distance of the previous path found to this node
                SearchNode previousNode = closedList.get(currentNode.getNodeID());
                if (previousNode.getPathLength() > currentNode.getPathLength())
                {
                    continue;
                }
            }
            
            // put current node on close list
            closedList.put(currentNode.getNodeID(), currentNode);
            
            // expand node
            neighbors = graph.getNeighbors(currentNode.getNodeID());
            for (EntityID id: neighbors)
            {
                if (! closedList.containsKey(id))
                {
                    // if this neighbor is not closed, add it to the open list
                    int distanceToCurrentNode = distanceMatrix.getDistance(id, currentNode.getNodeID());
                    int heuristicValue = distanceMatrix.getDistance(id, start);
                    openList.add(new SearchNode(id, currentNode, distanceToCurrentNode, heuristicValue));
                }
            }
        }
        if (! searchComplete)
        {
            // no path found
            Logger.debug(Markers.RED, "no path found");
            return null;
        }
        // construct the path
        List<EntityID> path = new LinkedList<>();
        while (currentNode.getParent() != null)
        {
            path.add(currentNode.getNodeID());
            currentNode = currentNode.getParent();
        }
//        Logger.debugColor("path found. length: "+path.size(), Logger.BG_GREEN);
        return path;
    }
}
