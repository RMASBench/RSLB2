/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Helpers;

import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

/**
 *
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
public class Distance {

    public static double humanToBuilding(EntityID agent, EntityID target, StandardWorldModel model) {
        Human hagent = (Human)model.getEntity(agent);
        EntityID position = hagent.getPosition();
        return model.getDistance(position, target);
    }

    public static double humanToBlockade(EntityID agent, EntityID target, StandardWorldModel model) {
        Human hagent = (Human)model.getEntity(agent);
        EntityID position = hagent.getPosition();
        Blockade blockade = (Blockade)model.getEntity(target);
        EntityID position2 = blockade.getPosition();
        return model.getDistance(position, position2);
    }

}
