/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Search;

import java.util.LinkedList;
import java.util.List;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.worldmodel.EntityID;

/**
 * Class that holds the results of the path search of an agent.
 *
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
public class SearchResults {

    private List<EntityID> pathIds = new LinkedList<>();
    private List<Area> pathAreas = new LinkedList<>();
    private List<Blockade> pathBlocks = new LinkedList<>();

    public List<EntityID> getPathIds() {
        return pathIds;
    }

    public void setPathIds(List<EntityID> pathIds) {
        this.pathIds = pathIds;
    }

    public List<Area> getPathAreas() {
        return pathAreas;
    }

    public void setPathAreas(List<Area> pathAreas) {
        this.pathAreas = pathAreas;
    }

    public List<Blockade> getPathBlocks() {
        return pathBlocks;
    }

    public void setPathBlocks(List<Blockade> pathBlocks) {
        this.pathBlocks = pathBlocks;
    }

}
