package microtrafficsim.core.logic;

import microtrafficsim.core.logic.vehicles.AbstractVehicle;
import microtrafficsim.core.logic.vehicles.VehicleState;
import microtrafficsim.core.map.Coordinate;
import microtrafficsim.core.shortestpath.ShortestPathEdge;
import microtrafficsim.core.shortestpath.ShortestPathNode;
import microtrafficsim.core.simulation.configs.SimulationConfig;
import microtrafficsim.math.Geometry;
import microtrafficsim.math.Vec2f;
import microtrafficsim.utils.hashing.FNVHashBuilder;

import java.util.*;
import java.util.stream.Collectors;


/**
 * This class represents one crossing point of two or more @DirectedEdge#s.
 * <p>
 * ShortestPathNode serves functionality for shortest path calculations.
 *
 * @author Jan-Oliver Schmidt, Dominic Parga Cacheiro
 */
public class Node implements ShortestPathNode {

    public final Long        ID;
    private SimulationConfig config;
    private Random           random;
    private Coordinate       coordinate;
    private HashMap<Lane, ArrayList<Lane>> restrictions;

    // crossing logic
    private HashSet<AbstractVehicle>                             maxPrioVehicles;
    private HashMap<AbstractVehicle, Set<AbstractVehicle>>       assessedVehicles;
    private Comparator<AbstractVehicle>                          crossingLogic;
    private boolean                                              anyChangeSinceUpdate;

    // edges
    private HashMap<DirectedEdge, Byte> leavingEdges;    // edge, index(for crossing logic)
    private HashMap<DirectedEdge, Byte> incomingEdges;    // edge, index(for crossing logic)


    /**
     * Standard constructor. The name is just for printing use and has no
     * meaning for the simulation.
     */
    public Node(SimulationConfig config, Coordinate coordinate) {
        this.config     = config;
        ID              = config.longIDGenerator.next();
        random          = new Random(config.seedGenerator.next()); // TODO determinism => remember seed
        this.coordinate = coordinate;

        // crossing logic
        assessedVehicles      = new HashMap<>();
        maxPrioVehicles       = new HashSet<>();
        crossingLogic         = generateCrossingLogic();
        anyChangeSinceUpdate  = false;

        // edges
        restrictions  = new HashMap<>();
        leavingEdges  = new HashMap<>();
        incomingEdges = new HashMap<>();
    }

    @Override
    public int hashCode() {
        return new FNVHashBuilder().add(ID).add(coordinate).getHash();
    }

    @Override
    public String toString() {
        String output = "Node name = " + ID + " at " + coordinate.toString();

        //		for (Lane start : restrictions.keySet())
        //			for (Lane end : restrictions.get(start))
        //				output += start + " to " + end + "\n";

        return output;
    }

    /*
    |================|
    | crossing logic |
    |================|
    */
    /**
     * <p>
     * Rules:<br>
     * &bull two not-spawned vehicles are compared by their IDs. The greater ID wins.<br>
     * &bull spawned vehicles gets priority over not spawned vehicles. This makes sense when thinking about the
     * situation, when you want to enter the street from your private parking place.<br>
     * &bull two spawned vehicles means they are coming from a street and want to make a turn. Thus they have to be
     * compared by the crossing logic (below).<br>
     * IMPORTANT: The registration does NOT check the positions relative to each other vehicle ON THE STREET, but it
     * checks/compares all information relevant for the crossing itself.
     *
     * <p>
     * At first, the crossing logic checks whether the two vehicles' turning ways are crossing each other (otherwise
     * return 0). If they are crossing, the origin-priorities are compared. If equal, the destination-priorities are
     * compared. If equal, they have to be compared by right-before-left or randomly. All sub-comparisons can be
     * enabled/disabled with the {@link SimulationConfig}.
     *
     * @return a comparator for two vehicles comparing their priority in a crossing situation
     */
    private Comparator<AbstractVehicle> generateCrossingLogic() {
        return (v1, v2) -> {
            // main rules:
            // (1) two not-spawned vehicles are compared by their IDs. The greater ID wins.
            // (2) spawned vehicles before not spawned vehicles
            // (3) two spawned vehicles => comparator

            if (v1.getState() != VehicleState.SPAWNED) {
                if (v2.getState() != VehicleState.SPAWNED) {
                    // (1) v1 is NOT SPAWNED, v2 is NOT SPAWNED
                    return Long.compare(v1.ID, v2.ID);
                } else {
                    // (2) v1 is NOT SPAWNED, v2 is SPAWNED
                    return -1;
                }
            } else if (v2.getState() != VehicleState.SPAWNED) {
                // (2) v1 is SPAWNED, v2 is NOT SPAWNED
                return 1;
            }

            // (3) both SPAWNED => there is always a current edge and a next edge per vehicle
            byte origin1        = incomingEdges.get(v1.getDirectedEdge());
            byte destination1   = leavingEdges.get(v1.peekNextRouteSection());
            byte origin2        = incomingEdges.get(v2.getDirectedEdge());
            byte destination2   = leavingEdges.get(v2.peekNextRouteSection());
            byte indicesPerNode = (byte) (incomingEdges.size() + leavingEdges.size());

            // if vehicles are crossing each other's way
            if (IndicesCalculator.areIndicesCrossing(origin1, destination1, origin2, destination2, indicesPerNode)) {
                // compare priorities of origins
                byte cmp = (byte) (v1.getDirectedEdge().getPriorityLevel() - v2.getDirectedEdge().getPriorityLevel());
                boolean edgePriorityEnabled = config.crossingLogic.edgePriorityEnabled;
                if (cmp == 0 || !edgePriorityEnabled) {
                    // compare priorities of destinations
                    cmp = (byte) (v1.peekNextRouteSection().getPriorityLevel()
                                  - v2.peekNextRouteSection().getPriorityLevel());
                    if (cmp == 0 || !edgePriorityEnabled) {
                        // compare right before left (or left before right)
                        if (config.crossingLogic.priorityToTheRightEnabled) {
                            byte leftmostMatchingIdx = IndicesCalculator.leftmostIndexInMatching(
                                    origin1, destination1, origin2, destination2, indicesPerNode);
                            if (leftmostMatchingIdx == origin1) return 1;
                            if (leftmostMatchingIdx == origin2) return -1;
                            return 0;
                        } else {
                            // random out of {-1, 1}
                            return random.nextInt(2) * 2 - 1;
                        }
                    }
                }
                return cmp;
            }
            return 0;
        };
    }

    /**
     * The node empties its crossing sets etc., but also reset its instance of {@link Random}, whereas it is not
     * guaranteed, that it will be identical.
     */
    void reset() {
        random = new Random(config.seedGenerator.next()); // TODO determinism => remember seed

        // crossing logic
        assessedVehicles.clear();
        maxPrioVehicles.clear();
        anyChangeSinceUpdate = false;
    }

    /**
     * <p>
     * If any vehicle has unregistered since the last call of {@code update}, all vehicles are compared to each other
     * for getting the highest priority. This needs O(n^2) comparisons due to the Gauss sum. This cannot be done
     */
    public synchronized void update() {

        /*
        |==============|
        | remove trash |
        |==============|
        */
        // if there is trash => assess all vehicles; due to random comparison, you can't trace back which vehicle
        // has won
        if (!trashVehicles.isEmpty()) {
            // reset assessed vehicles due to random
            for (AbstractVehicle vehicle : assessedVehicles) {
                if (!trashVehicles.contains(vehicle)) {
                    newRegisteredVehicles.add(vehicle);
                    vehicle.resetPriorityCounter();
                }
            }

            assessedVehicles.clear();
            trashVehicles.clear();
        }

        // TODO hier hab ich aufgehört; möglicherweise kann das aber genau so bleiben.
        /*
        |==============================|
        | find max prioritized vehicle |
        |==============================|
        */
        if (!assessedVehicles.isEmpty()) {
            maxPrioVehicles.clear();

            // get vehicles with max prio
            int maxPrio = Integer.MIN_VALUE;
            for (AbstractVehicle vehicle : assessedVehicles) {
                if (maxPrio <= vehicle.getPriorityCounter()) {
                    // For all vehicles until now: the current vehicle is allowed to drive regarding priority.
                    // BUT: it is still NOT allowed if all of the following conditions are true

                    // PRO-TIP: Read the following statements as "if condition (1) is false, the case is clear and
                    // vehicle gets permission. If it is true but condition (2) is false, the case is clear and vehicle
                    // gets permission. If it is true as well but condition (3) is false, the case is clear and vehicle
                    // gets permission."

                    // (1) no change since last update
                    // (2) no space for the vehicle at the next road
                    // (3) friendly standing in jam is enabled; the effect of this boolean points out if it is false,
                    // because then, a vehicle is taken into account although it has no space at the next road

                    // (1)
                    if (!anyChangeSinceUpdate) {
                        // (3), different order than above for better performance
                        if (config.crossingLogic.friendlyStandingInJamEnabled)
                            // (2), different order than above for better performance
                            if (!(vehicle.peekNextRouteSection().getLane(0).getMaxInsertionIndex() >= 0)) {
                                continue;
                        }
                    }

                    // if priority is truly greater than current max => remove all current vehicles of max priority
                    if (maxPrio < vehicle.getPriorityCounter()) {
                        maxPrioVehicles.clear();
                        maxPrio = vehicle.getPriorityCounter();
                    }
                    maxPrioVehicles.add(vehicle);
                }
            }

            if (!maxPrioVehicles.isEmpty()) {
                // case #1: maxPrio == assessedVehicles.size() - 1
                // => all vehicles are beaten (otherwise: deadlock between vehicles if more than one has priority)
                boolean allOthersBeaten = maxPrio == assessedVehicles.size() - 1;
                // XOR
                // case #2: deadlock OR tooManyVehicles
                // => choose random vehicle
                boolean tooManyVehicles = config.crossingLogic.isOnlyOneVehicleEnabled() && maxPrioVehicles.size() > 1;
                if (!allOthersBeaten || tooManyVehicles) {
                    Iterator<AbstractVehicle> bla = maxPrioVehicles.iterator();
                    for (int i = 0; i < random.nextInt(maxPrioVehicles.size()); i++)
                        bla.next();
                    AbstractVehicle prioritizedVehicle = bla.next();
                    maxPrioVehicles.clear();
                    maxPrioVehicles.add(prioritizedVehicle);
                }
            }
        }

        /*
        |=======|
        | reset |
        |=======|
        */
        anyChangeSinceUpdate = false;
    }

    /**
     * <p>
     * Registers the given vehicle at this node. For more information about the comparison itself, see
     * {@link #generateCrossingLogic()}.
     *
     * <p>
     * This method is synchronized because the assertion works with the information whether a vehicle is registered or
     * not => access should be after registration has finished.
     *
     * @param newVehicle This vehicle gets registered in this node.
     */
    public synchronized void registerVehicle(AbstractVehicle newVehicle) {

        // check if already registered
        if (assessedVehicles.containsKey(newVehicle))
            return;
        // init new entry in assessed vehicles for new vehicle and the set of defeated vehicles used for unregistration
        Set<AbstractVehicle> newSetDefeated = new HashSet<>();
        assessedVehicles.put(newVehicle, newSetDefeated);

        newVehicle.resetPriorityCounter();

        // compare new vehicle to each already assessed vehicle and save the loser in the winner's defeated list
        for (AbstractVehicle assessedVehicle : assessedVehicles.keySet()) {

            int cmp = crossingLogic.compare(newVehicle, assessedVehicle);

            if (cmp > 0) { // new vehicle has won
                newVehicle.incPriorityCounter();
                assessedVehicle.decPriorityCounter();
                newSetDefeated.add(assessedVehicle);
            } else if (cmp < 0) { // assessed vehicle has won
                newVehicle.decPriorityCounter();
                assessedVehicle.incPriorityCounter();
                assessedVehicles.get(assessedVehicle).add(newVehicle);
            } else { // they don't intersect
                newVehicle.incPriorityCounter();
                assessedVehicle.incPriorityCounter();
                newSetDefeated.add(assessedVehicle);
                assessedVehicles.get(assessedVehicle).add(newVehicle);
            }
        }
        anyChangeSinceUpdate = true;
    }

    public synchronized void unregisterVehicle(AbstractVehicle vehicle) {

        anyChangeSinceUpdate |= trashVehicles.add(vehicle);
    }

    /**
     * <p>
     * This method is synchronized because its return value depends on {@link #registerVehicle(AbstractVehicle)},
     * {@link #unregisterVehicle(AbstractVehicle)} and {@link #update()}, which can be called concurrently.
     *
     * @param vehicle This vehicle asks whether it has permission to cross or not
     * @return true if the vehicle has permission to cross, false otherwise
     */
    public synchronized boolean permissionToCross(AbstractVehicle vehicle) {
        return maxPrioVehicles.contains(vehicle);
    }

    /*
    |===========================|
    | add edges (preprocessing) |
    |===========================|
    */
    /**
     * This method adds one turning lane. Not every lane is connected to any
     * other lane.
     *
     * @param incoming
     * @param leaving
     * @param direction UNUSED
     */
    public void addConnector(Lane incoming, Lane leaving, Direction direction) {
        if (!restrictions.containsKey(incoming))
            restrictions.put(incoming, new ArrayList<>());
        restrictions.get(incoming).add(leaving);
    }

    /**
     * Adds a {@link DirectedEdge} to this node. It's traffic index for
     * calculating crossing order is set to -1.
     */
    public void addEdge(DirectedEdge edge) {
        if (edge.getOrigin() == this)
            leavingEdges.put(edge, (byte) -1);
        else if (edge.getDestination() == this)
            incomingEdges.put(edge, (byte) -1);
    }

    /**
     * This method should be called after all edges are added to this node. It
     * calculates the order of the edges that is needed for crossing logic
     * calculation.
     */
    public void calculateEdgeIndices() {

        // set zero vector
        Vec2f zero = null;
        for (DirectedEdge edge : leavingEdges.keySet()) {
            zero = new Vec2f(edge.getOriginDirection());
            break;
        }
        if (zero == null)
            for (DirectedEdge edge : incomingEdges.keySet()) {
                zero = Vec2f.mul(edge.getDestinationDirection(), -1);
                break;
            }

        // get all vectors for sorting
        HashMap<Vec2f, ArrayList<DirectedEdge>> edges = new HashMap<>();
        for (DirectedEdge edge : leavingEdges.keySet()) {
            Vec2f v = new Vec2f(edge.getOriginDirection());
            if (!edges.containsKey(v)) edges.put(v, new ArrayList<>(2));
            edges.get(v).add(edge);
        }
        for (DirectedEdge edge : incomingEdges.keySet()) {
            Vec2f v = Vec2f.mul(edge.getDestinationDirection(), -1);
            if (!edges.containsKey(v)) edges.put(v, new ArrayList<>(2));
            edges.get(v).add(edge);
        }

        // now: all vectors are keys
        Queue<Vec2f> sortedVectors
                = Geometry.sortClockwiseAsc(zero, edges.keySet(), !config.crossingLogic.drivingOnTheRight);
        byte nextCrossingIndex = 0;
        while (!sortedVectors.isEmpty()) {
            ArrayList<DirectedEdge> nextEdges = edges.remove(sortedVectors.poll());
            for (DirectedEdge nextEdge : nextEdges)
                if (leavingEdges.containsKey(nextEdge)) leavingEdges.put(nextEdge, nextCrossingIndex++);
            for (DirectedEdge nextEdge : nextEdges)
                if (incomingEdges.containsKey(nextEdge)) incomingEdges.put(nextEdge, nextCrossingIndex++);
        }
    }

    /*
    |======================|
    | (i) ShortestPathNode |
    |======================|
    */
    @Override
    public Set<ShortestPathEdge> getLeavingEdges(ShortestPathEdge incoming) {
        HashSet<ShortestPathEdge> returnEdges = new HashSet<>();

        if (incoming != null) {
            for (Lane incomingLane : ((DirectedEdge) incoming).getLanes()) {
                ArrayList<Lane> restrictedLeavingLanes = restrictions.get(incomingLane);
                // if there exist restrictions
                if (restrictedLeavingLanes != null)
                    returnEdges.addAll(
                            restrictedLeavingLanes.stream().map(Lane::getAssociatedEdge).collect(Collectors.toList()));
                    // before: (wtf Intellij is so awesome)
                    // for (Lane leavingLane : restrictedLeavingLanes)
                    // returnEdges.add(leavingLane.getAssociatedEdge());
                else
                    returnEdges.addAll(leavingEdges.keySet());
            }
        } else {
            returnEdges.addAll(leavingEdges.keySet());
        }

        return returnEdges;
    }

    @Override
    public Set<ShortestPathEdge> getIncomingEdges() {
        return new HashSet<>(incomingEdges.keySet());
    }

    @Override
    public Coordinate getCoordinate() {
        return coordinate;
    }
}