package com.acuitybotting.path_finding.rs.utils;

import com.acuitybotting.db.arango.path_finding.domain.xtea.RegionMap;
import com.acuitybotting.path_finding.algorithms.hpa.implementation.graph.HPAEdge;
import com.acuitybotting.path_finding.rs.domain.graph.TileEdge;
import com.acuitybotting.path_finding.rs.domain.graph.TileNode;
import com.acuitybotting.path_finding.rs.domain.location.Locateable;
import com.acuitybotting.path_finding.rs.domain.location.Location;
import com.acuitybotting.path_finding.xtea.domain.rs.cache.RsRegion;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Zachary Herridge on 6/29/2018.
 */
@Slf4j
@Getter
public class RsMap {

    private Map<Integer, RegionMap> regions = new HashMap<>();

    private Map<String, List<Location>> pathMap = new HashMap<>();

    private Integer lowestX, lowestY, highestX, highestY;

    public Optional<RegionMap> getRegion(int regionId) {
        return Optional.ofNullable(regions.get(regionId));
    }

    public Optional<RegionMap> getRegion(Location location) {
        return Optional.ofNullable(regions.get(RegionUtils.locationToRegionId(location.getX(), location.getY())));
    }

    public boolean checkWalkable(Location start, Direction direction){
        return checkWalkable(start, direction, false);
    }

    public boolean checkWalkable(Location start, Direction direction, boolean ignoreStartBlocked){
        Integer startFlag = getFlagAt(start).orElse(null);
        if (startFlag == null) return false;

        Location end = start.clone(direction.getXOff(), direction.getYOff());
        Integer endFlag = getFlagAt(end).orElse(null);
        if (endFlag == null) return false;


        if (!MapFlags.check(endFlag, MapFlags.OPEN_OVERRIDE | MapFlags.OPEN_OVERRIDE_END)){
            if (start.getPlane() > 0 && MapFlags.check(endFlag, MapFlags.NO_OVERLAY)) return false;
            if (MapFlags.isBlocked(endFlag)) return false;
        }

        if (!ignoreStartBlocked && !MapFlags.check(startFlag, MapFlags.OPEN_OVERRIDE | MapFlags.OPEN_OVERRIDE_START)){
            if (start.getPlane() > 0 && MapFlags.check(startFlag, MapFlags.NO_OVERLAY)) return false;
            if (MapFlags.isBlocked(startFlag)) return false;
        }

        if (MapFlags.check(startFlag, MapFlags.WALL_NORTH_EAST_TO_SOUTH_WEST | MapFlags.WALL_NORTH_WEST_TO_SOUTH_EAST)) return false;
        if (MapFlags.check(endFlag, MapFlags.WALL_NORTH_EAST_TO_SOUTH_WEST | MapFlags.WALL_NORTH_WEST_TO_SOUTH_EAST)) return false;

        switch (direction) {
            case NORTH:
                return !(MapFlags.check(startFlag, MapFlags.WALL_NORTH) || MapFlags.check(endFlag, MapFlags.WALL_SOUTH));
            case SOUTH:
                return !(MapFlags.check(startFlag, MapFlags.WALL_SOUTH) || MapFlags.check(endFlag, MapFlags.WALL_NORTH));
            case WEST:
                return !(MapFlags.check(startFlag, MapFlags.WALL_WEST) || MapFlags.check(endFlag, MapFlags.WALL_EAST));
            case EAST:
                return !(MapFlags.check(startFlag, MapFlags.WALL_EAST) || MapFlags.check(endFlag, MapFlags.WALL_WEST));

            case NORTH_WEST:
                return checkWalkable(end, Direction.SOUTH) && checkWalkable(end, Direction.EAST);
            case NORTH_EAST:
                return checkWalkable(end, Direction.SOUTH) && checkWalkable(end, Direction.WEST);
            case SOUTH_EAST:
                return checkWalkable(end, Direction.NORTH) && checkWalkable(end, Direction.WEST);
            case SOUTH_WEST:
                return checkWalkable(end, Direction.NORTH) && checkWalkable(end, Direction.EAST);
        }

        return false;
    }

    public Optional<Integer> getFlagAt(Location location) {
        RegionMap regionMap = getRegion(location).orElse(null);
        if (regionMap == null) return Optional.empty();
        int localX = location.getX() - regionMap.getBaseX();
        int localY = location.getY() - regionMap.getBaseY();
        return Optional.of(regionMap.getFlags()[location.getPlane()][localX][localY]);
    }

    public String addPath(List<TileEdge> path, boolean reverse){
        return addPath(null, path, reverse);
    }

    public String addPath(String key, List<TileEdge> path, boolean reverse){
        if (key == null) key = UUID.randomUUID().toString();

        List<Location> locationPath = convertPath(path, reverse);

        pathMap.put(key, locationPath);
        return key;
    }

    public TileNode getNode(Location location) {
        return new TileNode(location);
    }

    public List<Location> getPath(HPAEdge hpaEdge){
        String pathKey = hpaEdge.getPathKey();
        if (pathKey != null) return pathMap.get(pathKey);
        return null;
    }

    public void calculateBounds() {
        for (RegionMap region : regions.values()) {
            if (lowestX == null || region.getBaseX() < lowestX) {
                lowestX = region.getBaseX();
            }

            if (highestX == null || (region.getBaseX() + RsRegion.X) > highestX) {
                highestX = (region.getBaseX() + RsRegion.X);
            }

            if (lowestY == null || region.getBaseY() < lowestY) {
                lowestY = region.getBaseY();
            }

            if (highestY == null || (region.getBaseY() + RsRegion.Y) > highestY) {
                highestY = (region.getBaseY() + RsRegion.Y);
            }
        }

        log.info("Calculated RsMap bound {}, {} to {}, {}.", lowestX, lowestY, highestX, highestY);
    }

    public static List<Location> convertPath(List<TileEdge> path, boolean reverse){
        List<Location> localPath = path.stream().map(edge -> {
            if (edge.getEnd() instanceof Locateable) return ((Locateable) edge.getEnd()).getLocation();
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());

        if (reverse) Collections.reverse(localPath);

        return localPath;
    }
}
