package logic.crossinglogic.scenarios.pluscrossroad;

import microtrafficsim.core.logic.nodes.Node;
import microtrafficsim.core.logic.routes.MetaRoute;
import microtrafficsim.core.logic.routes.Route;
import microtrafficsim.core.logic.streetgraph.Graph;
import microtrafficsim.core.simulation.builder.ScenarioBuilder;
import microtrafficsim.core.simulation.configs.SimulationConfig;
import microtrafficsim.core.simulation.scenarios.impl.QueueScenarioSmall;
import microtrafficsim.core.simulation.utils.RouteContainer;
import microtrafficsim.core.simulation.utils.SortedRouteContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * @author Dominic Parga Cacheiro
 */
public abstract class AbstractPlusCrossroadScenario extends QueueScenarioSmall {

    public final Node bottomLeft, topLeft, mid, topRight, bottomRight;
    private final TreeMap<Node, Integer> lengthToMid;

    public AbstractPlusCrossroadScenario(SimulationConfig config, Graph graph, ScenarioBuilder scenarioBuilder) {
        super(config, graph, scenarioBuilder);


        /* get nodes sorted by lon */
        ArrayList<Node> sortedNodes = new ArrayList<>(getGraph().getNodes());
        sortedNodes.sort((n1, n2) -> n1.getCoordinate().lon > n2.getCoordinate().lon ? 1 : -1);

        bottomLeft  = sortedNodes.get(0); // length to mid: 8
        topLeft     = sortedNodes.get(1); // length to mid: 7
        mid         = sortedNodes.get(2); // length to mid: 0
        topRight    = sortedNodes.get(3); // length to mid: 9
        bottomRight = sortedNodes.get(4); // length to mid: 12

        /* street lengths for general sub-scenario definition (below) */
        lengthToMid = new TreeMap<>();
        lengthToMid.put(bottomLeft,   8);
        lengthToMid.put(topLeft,      7);
        lengthToMid.put(mid,          0);
        lengthToMid.put(topRight,     9);
        lengthToMid.put(bottomRight, 12);

        init();
    }

    /**
     * @param config
     * @return the given config updated; just for practical purpose
     */
    public static SimulationConfig setupConfig(SimulationConfig config) {

        QueueScenarioSmall.setupConfig(config);

        config.maxVehicleCount                            = 4;
        config.crossingLogic.friendlyStandingInJamEnabled = true;

        return config;
    }

    protected abstract void init();


    protected final void addPriorityToTheRight(Node rightStart) {
        addBothStraight(rightStart, neighbor(rightStart, true));
    }

    protected final void addPriorityTurning(Node prioStart) {
        addOneTurning(prioStart, opposite(prioStart), getConfig().crossingLogic.drivingOnTheRight);
    }


    protected final void addOneTurning(Node straightStart, Node turnStart, boolean leftTurn) {

        Node straightEnd = opposite(straightStart);
        Node turnEnd     = neighbor(turnStart, leftTurn);

        addSubScenario(
                new MetaRoute(straightStart, straightEnd, -1),
                new MetaRoute(turnStart, turnEnd, -1)
        );
    }

    protected final void addBothStraight(Node aStart, Node bStart) {
        Node aEnd = opposite(aStart);
        Node bEnd = opposite(bStart);

        addSubScenario(
                new MetaRoute(aStart, aEnd, -1),
                new MetaRoute(bStart, bEnd, -1)
        );
    }

    /**
     * negative spawn delay means: <br>
     * Let m be the maximum spawn delay of all given routes. The spawn delay of a route with negative spawn delay is
     * calculated by:<br>
     * <br>
     * {@code spawndelay := m - lengthToMid.get(route.getOrigin())} <br>
     * <br>
     * With this, all vehicles of negative spawn delay arrive at the crossroad at the same time
     *
     * @param routes
     */
    protected final void addSubScenario(Route... routes) {
        RouteContainer routeContainer = new SortedRouteContainer();

        // used for spawn delay
        int maxLength = 0;

        // used for all not defined spawn delays
        for (Route route : routes) {
            int tmp = Math.max(
                    route.getSpawnDelay(),
                    lengthToMid.get(route.getOrigin()));
            maxLength = Math.max(maxLength, tmp);
        }

        for (Route route : routes) {
            int delay = route.getSpawnDelay();
            if (delay < 0)
                delay = maxLength - lengthToMid.get(route.getOrigin());

            route.setSpawnDelay(delay);
            routeContainer.add(route);
        }

        addSubScenario(routeContainer);
    }


    private Node opposite(Node node) {

        if (node == topRight)
            return bottomLeft;

        if (node == topLeft)
            return bottomRight;

        if (node == bottomLeft)
            return topRight;

        if (node == bottomRight)
            return topLeft;

        return null;
    }

    private Node neighbor(Node node, boolean cw) {

        if (node == topRight)
            return cw ? bottomRight : topLeft;

        if (node == topLeft)
            return cw ? topRight : bottomLeft;

        if (node == bottomLeft)
            return cw ? topLeft : bottomRight;

        if (node == bottomRight)
            return cw ? bottomLeft : topRight;

        return null;
    }
}