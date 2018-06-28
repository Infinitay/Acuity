package com.acuitybotting.path_finding;

import com.acuitybotting.db.arango.path_finding.domain.xtea.RegionInfo;
import com.acuitybotting.db.arango.path_finding.domain.xtea.SceneEntityDefinition;
import com.acuitybotting.db.arango.path_finding.domain.xtea.Xtea;
import com.acuitybotting.path_finding.algorithms.astar.AStarService;
import com.acuitybotting.path_finding.algorithms.graph.Edge;
import com.acuitybotting.path_finding.algorithms.hpa.implementation.HPAGraph;
import com.acuitybotting.path_finding.algorithms.hpa.implementation.PathFindingSupplier;
import com.acuitybotting.path_finding.debugging.interactive_map.plugin.impl.HpaPlugin;
import com.acuitybotting.path_finding.debugging.interactive_map.plugin.impl.PathPlugin;
import com.acuitybotting.path_finding.debugging.interactive_map.plugin.impl.PositionPlugin;
import com.acuitybotting.path_finding.debugging.interactive_map.plugin.impl.RegionPlugin;
import com.acuitybotting.path_finding.debugging.interactive_map.ui.MapFrame;
import com.acuitybotting.path_finding.rs.domain.graph.TileNode;
import com.acuitybotting.path_finding.rs.domain.location.LocateableHeuristic;
import com.acuitybotting.path_finding.rs.domain.location.Location;
import com.acuitybotting.path_finding.rs.utils.RsEnvironment;
import com.acuitybotting.path_finding.rs.utils.RsMapService;
import com.acuitybotting.path_finding.web_processing.HpaWebService;
import com.acuitybotting.path_finding.web_processing.WebImageProcessingService;
import com.acuitybotting.path_finding.xtea.XteaService;
import com.acuitybotting.path_finding.xtea.domain.Region;
import com.arangodb.springframework.repository.ArangoRepository;
import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PathFindingRunner implements CommandLineRunner {

    private final WebImageProcessingService webImageProcessingService;
    private final RsMapService rsMapService;
    private final AStarService aStarService;

    private final PathPlugin pathPlugin;
    private final HpaPlugin hpaPlugin = new HpaPlugin();
    private final XteaService xteaService;
    private final HpaWebService hpaWebService;
    private RegionPlugin regionPlugin = new RegionPlugin();

    @Autowired
    public PathFindingRunner(WebImageProcessingService webImageProcessingService, RsMapService rsMapService, AStarService aStarService, PathPlugin pathPlugin, XteaService xteaService, HpaWebService hpaWebService) {
        this.webImageProcessingService = webImageProcessingService;
        this.rsMapService = rsMapService;
        this.aStarService = aStarService;
        this.pathPlugin = pathPlugin;
        this.xteaService = xteaService;
        this.hpaWebService = hpaWebService;
    }

    private PathFindingSupplier getPathfindingSupplier() {
        return new PathFindingSupplier() {
            @Override
            public Optional<List<Edge>> findPath(Location start, Location end, Predicate<Edge> predicate) {
                return aStarService.findPath(
                        new LocateableHeuristic(),
                        RsEnvironment.getNode(start),
                        RsEnvironment.getNode(end),
                        predicate
                );
            }

            @Override
            public boolean isDirectlyConnected(Location start, Location end) {
                TileNode sNode = RsEnvironment.getNode(start);
                TileNode endNode = RsEnvironment.getNode(end);
                return sNode.getNeighbors().stream().anyMatch(edge -> edge.getEnd().equals(endNode));
            }
        };
    }

    private HPAGraph initGraph() {
        HPAGraph graph = new HPAGraph();
        graph.init(
                new Location(3138 - 1000, 3384 - 1000, 0),
                new Location(3138 + 1000, 3384 + 1000, 1),
                30,
                30
        );
        graph.setPathFindingSupplier(getPathfindingSupplier());
        return graph;
    }

    private void loadHpa(int version) {
        HPAGraph graph = initGraph();
        hpaWebService.loadInto(graph, version);
        graph.addCustomNodes();
        hpaPlugin.setGraph(graph);
    }

    private void buildHpa(int version) {
        HPAGraph graph = initGraph();

        graph.build();

        hpaWebService.deleteVersion(version);
        hpaWebService.save(graph, version);
    }

    public void printXteas() {
        Set<Map.Entry<String, Set<Xtea>>> keySets = xteaService.findUnique(171).entrySet();

        for (Map.Entry<String, Set<Xtea>> keySetEntry : keySets) {
            StringBuilder result = new StringBuilder(keySetEntry.getKey());
            for (Xtea xtea : keySetEntry.getValue()) {
                result.append(" ").append(Arrays.stream(xtea.getKeys()).mapToObj(String::valueOf).collect(Collectors.joining(",")));
            }
            System.out.println(result);
        }
    }

    private void saveDefs() throws IOException {
        xteaService.getDefinitionRepository().deleteAll();
        Gson gson = new Gson();
        File[] files = new File("C:\\Users\\zgher\\Desktop\\Map Info\\json\\objects").listFiles();
        Set<SceneEntityDefinition> sceneEntityDefinitions = new HashSet<>();
        for (File child : files) {
            SceneEntityDefinition def = gson.fromJson(Files.readAllLines(child.toPath()).stream().collect(Collectors.joining("\n")), SceneEntityDefinition.class);
            for (int i = 0; i < def.getActions().length; i++) {
                if (def.getActions()[i] == null) {
                    def.getActions()[i] = "null";
                }
            }
            sceneEntityDefinitions.add(def);
        }

        save(xteaService.getDefinitionRepository(), 400, sceneEntityDefinitions);

        System.out.println("Done");
    }

    private void save(ArangoRepository repository, int size, Collection<?> collection) {
        final AtomicInteger counter = new AtomicInteger(0);
        collection.stream().collect(Collectors.groupingBy(it -> counter.getAndIncrement() / size)).values().parallelStream().forEach(set -> {
            repository.saveAll(set);
        });
    }

    private void dumpRegionImages() {
        ExecutorService executorService = Executors.newFixedThreadPool(30);
        for (RegionInfo regionInfo : RsEnvironment.getRegionMap().values()) {
            executorService.submit(() -> {
                BufferedImage[] tileFlagImage = webImageProcessingService.createTileFlagImage2(regionInfo.getKey());
                for (int i = 0; i < tileFlagImage.length; i++) {

                    try {
                        ImageIO.write(tileFlagImage[i], "png", new File(RsEnvironment.INFO_BASE, "\\img\\a_regions\\" + regionInfo.getKey() + "_" + i + ".png"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        executorService.shutdown();

        try {
            executorService.awaitTermination(3, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Finished image dump");
    }

    private void dumpRegionInfo() {
        log.info("Starting RegionInfo dump.");

        xteaService.getRegionInfoRepository().deleteAll();

        ExecutorService executorService = Executors.newFixedThreadPool(30);

        xteaService.findUnique(171).keySet().forEach(s -> {
            executorService.submit(() -> {
                Region region = xteaService.getRegion(Integer.parseInt(s)).orElse(null);
                if (region != null) {
                    RegionInfo save = xteaService.save(region);
                    log.info("Saved {}.", save);
                }
            });
        });

        executorService.shutdown();
        try {
            executorService.awaitTermination(3, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log.info("Finished RegionInfo dump.");
    }

    @Override
    public void run(String... args) {
        try {
            RsEnvironment.setRsMapService(rsMapService);

            RsEnvironment.loadRegions();
            dumpRegionImages();

            MapFrame mapFrame = new MapFrame();
            regionPlugin.setXteaService(xteaService);
            mapFrame.getMapPanel().addPlugin(regionPlugin);
            mapFrame.getMapPanel().addPlugin(new PositionPlugin());
            mapFrame.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
