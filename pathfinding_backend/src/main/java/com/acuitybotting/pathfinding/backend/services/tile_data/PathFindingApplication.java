package com.acuitybotting.pathfinding.backend.services.tile_data;

import com.acuitybotting.pathfinding.backend.services.tile_data.domain.TileCapture;
import com.google.gson.Gson;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Created by Zachary Herridge on 5/29/2018.
 */
@SpringBootApplication(scanBasePackages = "com.acuitybotting")
public class PathFindingApplication {

    public static void main(String[] args) {
        Class[] runner = new Class[]{PathFindingApplication.class};
        SpringApplication.run(runner, args);
    }
}
