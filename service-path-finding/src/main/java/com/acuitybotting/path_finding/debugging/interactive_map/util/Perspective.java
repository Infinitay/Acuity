package com.acuitybotting.path_finding.debugging.interactive_map.util;

import com.acuitybotting.path_finding.debugging.interactive_map.ui.MapPanel;
import com.acuitybotting.path_finding.rs.domain.location.Location;
import lombok.Getter;

import java.awt.*;

@Getter
public class Perspective {

    private GameMap gameMap;
    private MapPanel mapPanel;
    private Location base;

    private double scale = 1;

    public Perspective(GameMap gameMap, MapPanel mapPanel) {
        this.gameMap = gameMap;
        this.base = new Location(3138, 3384 + (600 / 3), 0);
        this.mapPanel = mapPanel;
    }

    public Point locationToScreen(Location location){
        Point basePoint = locationToMap(base);
        Point locationPoint = locationToMap(location);
        return scale(new Point(locationPoint.x - basePoint.x, basePoint.y - locationPoint.y));
    }

    public Point locationToMap(Location location){
        Location offset = new Location(location.getX() - gameMap.getBase().getX(), gameMap.getBase().getY() - location.getY(), location.getPlane());
        return new Point(round(offset.getX() * gameMap.getTilePixelSize()), round(offset.getY() * gameMap.getTilePixelSize()));
    }

    public Location screenToLocation(Point point){
        if (point == null) return null;
        Location offset = new Location(round(point.x / getTileSize()), round(point.y / getTileSize()), base.getPlane());
        return base.clone(offset.getX(), -offset.getY());
    }

    public Point scale(Point point){
        return new Point(round(point.x * scale), round(point.y * scale));
    }

    public double getTileSize(){
        return gameMap.getTilePixelSize() * scale;
    }

    public double getTileWidth(){
        return mapPanel.getWidth() / getTileSize();
    }

    public double getTileHeight(){
        return mapPanel.getHeight() / getTileSize();
    }

    private static int round(double value){
        return (int) Math.round(value);
    }
}
