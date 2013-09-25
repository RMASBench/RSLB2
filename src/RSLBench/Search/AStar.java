package RSLBench.Search;

import RSLBench.Helpers.Logging.Markers;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rescuecore2.standard.entities.Area;


public class AStar extends AbstractSearchAlgorithm
{
    private static final Logger Logger = LogManager.getLogger(AStar.class);

    @Override
    public List<Area> search(Area start, Collection<Area> goals, Graph graph, DistanceInterface distanceMatrix)
    {
        Logger.debug(Markers.GREEN, "start multi target search");
        
        PriorityQueue<SearchNode> openList = new PriorityQueue<>();
        Map<Area, SearchNode> closedList = new HashMap<>();
        
        // reverse search: add all goals to the open list
        for (Area id: goals)
        {
            int heuristicValue = distanceMatrix.getDistance(id.getID(), start.getID());
            openList.add(new SearchNode(id, null, 0, heuristicValue));
        }
        
        SearchNode currentNode = null;
        boolean searchComplete = false;
        Set<Area> neighbors;
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
            for (Area id: neighbors)
            {
                if (! closedList.containsKey(id))
                {
                    // if this neighbor is not closed, add it to the open list
                    int distanceToCurrentNode = distanceMatrix.getDistance(id.getID(), currentNode.getNodeID().getID());
                    int heuristicValue = distanceMatrix.getDistance(id.getID(), start.getID());
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
        List<Area> path = new LinkedList<>();
        while (currentNode.getParent() != null)
        {
            path.add(currentNode.getNodeID());
            currentNode = currentNode.getParent();
        }
//        Logger.debugColor("path found. length: "+path.size(), Logger.BG_GREEN);
        return path;
    }
}
