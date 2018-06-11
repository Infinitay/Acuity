package com.acuitybotting.path_finding.algorithms.astar;

import com.acuitybotting.path_finding.algorithms.graph.Edge;
import com.acuitybotting.path_finding.algorithms.graph.Node;

import java.util.*;
import java.util.function.Predicate;

public class AStar {

    private int maxAttempts = 1000000;

    private Map<Node, Edge> pathCache = new HashMap<>();
    private Map<Node, Double> costCache = new HashMap<>();
    private PriorityQueue<AStarStore> open = new PriorityQueue<>();

    public Optional<List<Edge>> findPath(AStarHeuristicSupplier heuristicSupplier, Node start, Node end) {
        return findPath(heuristicSupplier, start, end, null);
    }

    public Optional<List<Edge>> findPath(AStarHeuristicSupplier heuristicSupplier, Node start, Node end, Predicate<Edge> edgePredicate) {
        open.add(new AStarStore(start, 0));
        costCache.put(start, 0d);

        int attempts = 0;
        while (!open.isEmpty()){
            attempts++;
            AStarStore current = open.poll();

            if (attempts >= maxAttempts){
                System.out.println("Failed to find  path");
                break;
            }

            if (current.getNode().equals(end)){
                System.out.println("Path found");
                List<Edge> path = collectPath(end, start);
                clear();
                return Optional.ofNullable(path);
            }

            for (Edge edge : current.getNode().getNeighbors()) {
                if (edgePredicate != null && !edgePredicate.test(edge)) continue;

                Node next = edge.getEnd();

                double newCost = costCache.getOrDefault(current.getNode(), 0d) + heuristicSupplier.getHeuristic(start, current.getNode(), next, edge);

                Double oldCost = costCache.get(next);
                if (oldCost == null || newCost < oldCost){
                    costCache.put(next, newCost);
                    double priority = newCost + heuristicSupplier.getHeuristic(start, next, end, edge);
                    open.add(new AStarStore(next, priority));
                    pathCache.put(next, edge);
                }
            }
        }

        clear();
        return Optional.empty();
    }

    private List<Edge> collectPath(Node end, Node start){
        List<Edge> path = new ArrayList<>();
        Edge edge = pathCache.get(end);
        while (edge != null){
            path.add(edge);
            if (edge.getStart().equals(start)) break;
            edge = pathCache.get(edge.getStart());
        }
        Collections.reverse(path);
        return path;
    }

    private void clear(){
        open.clear();
        costCache.clear();
        pathCache.clear();
    }

    public AStar setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
        return this;
    }
}
