/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RSLBench.Assignment;

import RSLBench.Helpers.Utility.UtilityMatrix;
import java.util.ArrayList;
import java.util.List;
import rescuecore2.config.Config;
import rescuecore2.standard.entities.StandardWorldModel;

/**
 *
 * @author Marc Pujol <mpujol@iiia.csic.es>
 */
public class CompositeSolver implements Solver {

    private Solver mainSolver;
    private List<Solver> testSolvers;

    public CompositeSolver(Solver main) {
        mainSolver = main;
        testSolvers = new ArrayList<>();
    }

    public void addSolver(Solver solver) {
        testSolvers.add(solver);
    }

    @Override
    public void initialize(StandardWorldModel world, Config config) {
        mainSolver.initialize(world, config);
        for (Solver s : testSolvers) {
            s.initialize(world, config);
        }
    }

    @Override
    public String getIdentifier() {
        return "CompoundSolver";
    }

    @Override
    public Assignment solve(int time, UtilityMatrix utility) {
        Assignment solution = mainSolver.solve(time, utility);
        for (Solver s : testSolvers) {
            s.solve(time, utility);
        }
        return solution;
    }

    @Override
    public List<String> getUsedConfigurationKeys() {
        return null;
    }

}
