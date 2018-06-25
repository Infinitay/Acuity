package com.acuitybotting.path_finding.rs.utils;

import com.acuitybotting.db.arango.path_finding.domain.SceneEntity;
import com.acuitybotting.db.arango.path_finding.domain.TileFlag;
import com.acuitybotting.db.arango.path_finding.domain.xtea.RegionInfo;
import com.acuitybotting.path_finding.algorithms.hpa.implementation.graph.HPARegion;
import com.acuitybotting.path_finding.rs.domain.graph.TileNode;
import com.acuitybotting.path_finding.rs.domain.location.Location;
import com.acuitybotting.path_finding.xtea.domain.Region;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Created by Zachary Herridge on 6/11/2018.
 */
@Slf4j
public class RsEnvironment {

    public static final int PLANE_PENALTY = 25;
    public static final int CACHE_AREA = 15;

    public static final String[] DOOR_NAMES = new String[]{"Door", "Gate", "Large door", "Castle door", "Gate of War", "Rickety door", "Oozing barrier", "Portal of Death", "Magic guild door", "Prison door", "Barbarian door"};
    public static final String[] DOOR_ACTIONS = new String[]{"OPEN"};


    public static final String[] STAIR_NAMES = new String[]{"Stairs", "Ladder", "Stair"};

    private static RsMapService rsMapService;

    private static Map<String, RegionInfo> regionMap = new HashMap<>();

    public static TileNode getNode(Location location) {
        return new TileNode(location);
    }

    public static void loadRegions(){
        log.info("Starting region load.");
        for (RegionInfo regionInfo : rsMapService.getRegionInfoRepository().findAll()) {
            regionInfo.init();
            regionMap.put(regionInfo.getKey(), regionInfo);
        }
        log.info("Finished region load with {} regions.", regionMap.size());
    }

    private static RegionInfo getRegion(Location location){
        return regionMap.get(String.valueOf(RsMapService.worldToRegionId(location.getX(), location.getY())));
    }

    public static Integer getFlagAt(Location location) {
        RegionInfo regionInfo = getRegion(location);
        if (regionInfo == null) {
            return null;
        }
        int localX = location.getX() - regionInfo.getBaseX();
        int localY = location.getY() - regionInfo.getBaseY();
        return regionInfo.getFlags()[location.getPlane()][localX][localY];
    }

    public static Iterable<SceneEntity> getStairsWithin(HPARegion region) {
        return Collections.emptyList();
    }

    public static List<SceneEntity> getDoorsAt(Location location) {
        return Collections.emptyList();
    }

    public static RsMapService getRsMapService() {
        return rsMapService;
    }

    public static void setRsMapService(RsMapService rsMapService) {
        RsEnvironment.rsMapService = rsMapService;
    }
}
