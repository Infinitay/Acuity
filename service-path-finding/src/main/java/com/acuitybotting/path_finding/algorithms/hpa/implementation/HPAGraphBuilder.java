package com.acuitybotting.path_finding.algorithms.hpa.implementation;


import com.acuitybotting.db.arango.path_finding.domain.SceneEntity;
import com.acuitybotting.path_finding.algorithms.graph.Edge;
import com.acuitybotting.path_finding.algorithms.graph.Node;
import com.acuitybotting.path_finding.algorithms.hpa.implementation.graph.HPANode;
import com.acuitybotting.path_finding.algorithms.hpa.implementation.graph.HPARegion;
import com.acuitybotting.path_finding.rs.domain.location.Locateable;
import com.acuitybotting.path_finding.rs.domain.location.Location;
import com.acuitybotting.path_finding.rs.domain.location.LocationPair;
import com.acuitybotting.path_finding.rs.utils.RsEnvironment;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class HPAGraphBuilder {

    private Location lower, upper;
    private int regionWidth, regionHeight;

    private Map<String, HPARegion> regions;

    private int externalConnectionsCount = 0;
    private int internalConnectionCount = 0;
    private int stairNodesAddedCount = 0;

    public Map<String, HPARegion> init(Location lower, Location upper){
        this.lower = lower;
        this.upper = upper;

        regions = findRegions();
        log.info("Initiated with {} regions.", regions.size());

        return regions;
    }

    public Map<String, HPARegion> build(PathFindingSupplier pathFindingSupplier) {
        log.info("Started building HPA graph between {} and {} with width {} and height {}.", lower, upper, regionWidth, regionHeight);

        long startTimestamp = System.currentTimeMillis();

        ExecutorService executorService = Executors.newFixedThreadPool(20);

        for (HPARegion internalHPARegion : regions.values()) {
            executorService.submit(() -> {
                List<LocationPair> externalConnections = findExternalConnections(internalHPARegion, pathFindingSupplier);
                log.info("Found {} external connections from {}.", externalConnections.size(), internalHPARegion);
                for (LocationPair externalConnection : externalConnections) {
                    HPARegion externalHPARegion = getRegionContaining(externalConnection.getEnd()); //Check this is actually always the external region, and isn't null

                    HPANode internalNode = new HPANode(internalHPARegion, externalConnection.getStart());
                    HPANode externalNode = new HPANode(externalHPARegion, externalConnection.getEnd());

                    internalNode.addConnection(externalNode, 1);
                    externalNode.addConnection(internalNode, 1);

                    internalHPARegion.getNodes().add(internalNode);
                    externalHPARegion.getNodes().add(externalNode);

                    externalConnectionsCount++;
                }
            });
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log.info("Found {} external connections.", externalConnectionsCount);


        for (HPARegion hpaRegion : regions.values()) {
            addStairConnections(hpaRegion);
        }

        log.info("Found {} stair nodes.", stairNodesAddedCount);

        executorService = Executors.newFixedThreadPool(10);
        for (HPARegion HPARegion : regions.values()) {
            executorService.submit(() -> findInternalConnections(HPARegion, pathFindingSupplier));
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log.info("Found {} internal connections.", internalConnectionCount);

        log.info("Finished creating HPA graph in {} seconds.", (System.currentTimeMillis() - startTimestamp) / 1000);

        return regions;
    }



    public void addStairConnections(HPARegion region){
        for (SceneEntity sceneEntity : RsEnvironment.getStairsWithin(region)) {
            if (sceneEntity.getActions() == null) continue;
            boolean up = Arrays.stream(sceneEntity.getActions()).anyMatch(s -> s.toLowerCase().contains("up"));
            boolean down = Arrays.stream(sceneEntity.getActions()).anyMatch(s -> s.toLowerCase().contains("down"));

            if (up || down){
                HPANode stairNode = new HPANode(region, new Location(sceneEntity.getX(), sceneEntity.getY(), sceneEntity.getPlane())).setType(HPANode.STAIR);
                region.getNodes().add(stairNode);

                stairNodesAddedCount++;
            }
        }
    }

    public HPARegion getRegionContaining(Locateable locateable) {
        Location offset = locateable.getLocation().subtract(lower);
        int offX = ((int) (offset.getX() / (double) regionWidth)) * regionWidth;
        int offY = ((int) (offset.getY() / (double) regionHeight)) * regionHeight;
        Location base = lower.clone(offX, offY);
        return regions.get(HPARegion.getKey(base.getX(), base.getY(), base.getPlane(), regionWidth, regionHeight));

    }

    private void findInternalConnections(HPARegion region, PathFindingSupplier pathFindingSupplier) {
        for (HPANode startNode : region.getNodes()) {
            List<HPANode> endNodes = region.getNodes().stream()
                    .filter(hpaNode -> !hpaNode.equals(startNode))
                    .filter(hpaNode -> startNode.getEdges().stream().noneMatch(edge -> edge.getEnd().equals(hpaNode)))
                    .sorted((o1, o2) -> (int) o1.getLocation().getTraversalCost(o2.getLocation()))
                    .collect(Collectors.toList());

            int found = 0;
            for (HPANode endNode : endNodes) {
                if (found >= 3) break;

                int pathSize = pathFindingSupplier.findPath(
                        startNode.getLocation(),
                        endNode.getLocation(),
                        edge -> limitToRegion(region, edge)
                );

                if (pathSize > 0) {
                    found++;
                    internalConnectionCount++;
                    startNode.addConnection(endNode, pathSize);
                    endNode.addConnection(startNode, pathSize);
                }
            }
        }
    }

    private boolean limitToRegion(HPARegion HPARegion, Edge edge) {
        if (HPARegion == null) return false;
        Node node = edge.getStart();
        if (node == null || node instanceof Locateable && !HPARegion.contains(((Locateable) node).getLocation()))
            return false;
        node = edge.getEnd();
        if (node == null || node instanceof Locateable && !HPARegion.contains(((Locateable) node).getLocation()))
            return false;
        return true;
    }

    private List<LocationPair> findExternalConnections(HPARegion region, PathFindingSupplier pathFindingSupplier) {
        List<LocationPair> connections = new ArrayList<>();
        connections.addAll(filterEdgeConnections(region, region.getEdgeConnections(0), pathFindingSupplier));
        connections.addAll(filterEdgeConnections(region, region.getEdgeConnections(1), pathFindingSupplier));
        connections.addAll(filterEdgeConnections(region, region.getEdgeConnections(2), pathFindingSupplier));
        connections.addAll(filterEdgeConnections(region, region.getEdgeConnections(3), pathFindingSupplier));
        return connections;
    }

    private List<LocationPair> filterEdgeConnections(HPARegion region, List<LocationPair> connections, PathFindingSupplier pathFindingSupplier) {
        boolean lastPairConnected = false;
        for (LocationPair connection : new ArrayList<>(connections)) {
            if (getRegionContaining(connection.getEnd()) == null) { // Evaluate this.
                connections.remove(connection);
                continue;
            }

            boolean directlyConnected = pathFindingSupplier.isDirectlyConnected(connection.getStart(), connection.getEnd());
            if (lastPairConnected || !directlyConnected) connections.remove(connection);
            lastPairConnected = directlyConnected;
        }

 /*       if (connections.size() > 1) { //Evaluate this.
            HPARegion HPARegion = getRegionContaining(connections.get(0).getEnd());
            for (LocationPair connection : new ArrayList<>(connections)) {
                boolean duplicate = connections.stream()
                        .filter(locationPair -> !locationPair.equals(connection))
                        .anyMatch(locationPair -> pathFindingSupplier.isReachableFrom(connection.getEnd(), locationPair.getEnd(), edge -> limitToRegion(HPARegion, edge)));
                if (duplicate) connections.remove(connection);
            }
        }*/

        return connections;
    }

    private Map<String, HPARegion> findRegions() {
        Map<String, HPARegion> regions = new HashMap<>();
        for (int z = lower.getPlane(); z <= upper.getPlane(); z++) {
            for (int x = lower.getX(); x <= upper.getX(); x += regionWidth) {
                for (int y = lower.getY(); y <= upper.getY(); y += regionHeight) {
                    HPARegion HPARegion = new HPARegion(new Location(x, y, z), regionWidth, regionHeight);
                    regions.put(HPARegion.getKey(), HPARegion);
                }
            }
        }
        return regions;
    }

    public HPAGraphBuilder setRegionHeight(int regionHeight) {
        this.regionHeight = regionHeight;
        return this;
    }

    public HPAGraphBuilder setRegionWidth(int regionWidth) {
        this.regionWidth = regionWidth;
        return this;
    }
}
