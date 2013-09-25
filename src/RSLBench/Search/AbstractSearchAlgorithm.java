/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

/**
 *
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
public abstract class AbstractSearchAlgorithm implements SearchAlgorithm {
    private static final Logger Logger = LogManager.getLogger(AbstractSearchAlgorithm.class);

    @Override
    public List<EntityID> search(EntityID start, EntityID goal, Graph graph, DistanceInterface distanceMatrix) {
        Area init = getArea(graph, start);
        List<Area> ends = toAreas(graph, goal);
        return toEntityIDs(search(init, ends, graph, distanceMatrix));
    }

    public abstract List<Area> search(Area start, Collection<Area> goals, Graph graph, DistanceInterface distanceMatrix);

    @Override
    public List<EntityID> search(EntityID start, Collection<EntityID> goals, Graph graph, DistanceInterface distanceMatrix) {
        Area init = getArea(graph, start);
        List<Area> ends = toAreas(graph, goals);
        return toEntityIDs(search(init, ends, graph, distanceMatrix));
    }

    protected Area getArea(Graph graph, EntityID id) {
        StandardEntity entity = graph.getWorld().getEntity(id);
        if (entity instanceof Area) {
            return (Area)entity;
        }
        Logger.error("Unable to find are for entity " + id);
        return null;
    }

    private List<EntityID> toEntityIDs(List<Area> path) {
        List<EntityID> idpath = new ArrayList<>(path.size());
        for (Area a : path) {
            idpath.add(a.getID());
        }
        return idpath;
    }

    private List<Area> toAreas(Graph graph, Collection<EntityID> goals) {
        List<Area> ends = new ArrayList<>(goals.size());
        for (EntityID goal : goals) {
            ends.add(getArea(graph, goal));
        }
        return ends;
    }

    private List<Area> toAreas(Graph graph, EntityID goal) {
        List<Area> ends = new ArrayList<>(1);
        ends.add(getArea(graph, goal));
        return ends;
    }
    
}
