package com.acuitybotting.path_finding.web_processing;

import com.acuitybotting.db.arango.path_finding.domain.SceneEntity;
import com.acuitybotting.db.arango.path_finding.domain.TileFlag;
import com.acuitybotting.db.arango.path_finding.domain.xtea.RegionInfo;
import com.acuitybotting.db.arango.path_finding.domain.xtea.SceneEntityDefinition;
import com.acuitybotting.db.arango.path_finding.repositories.SceneEntityRepository;
import com.acuitybotting.db.arango.path_finding.repositories.TileFlagRepository;
import com.acuitybotting.path_finding.rs.domain.location.Location;
import com.acuitybotting.path_finding.rs.utils.RsEnvironment;
import com.acuitybotting.path_finding.rs.utils.RsMapService;
import com.acuitybotting.path_finding.xtea.XteaService;
import com.acuitybotting.path_finding.xtea.domain.Region;
import com.acuitybotting.path_finding.xtea.domain.SceneEntityInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Set;

/**
 * Created by Zachary Herridge on 6/5/2018.
 */
@Service
public class WebImageProcessingService {

    private XteaService xteaService;
    private TileFlagRepository flagRepository;
    private SceneEntityRepository sceneEntityRepository;

    @Autowired
    public WebImageProcessingService(XteaService xteaService, TileFlagRepository flagRepository, SceneEntityRepository sceneEntityRepository) {
        this.xteaService = xteaService;
        this.flagRepository = flagRepository;
        this.sceneEntityRepository = sceneEntityRepository;
    }

    public BufferedImage createTileFlagImage(int plane, RegionInfo regionInfo) {
        int tilePixelSize = 4;
        BufferedImage mapImage = new BufferedImage(64 * tilePixelSize, 64 * tilePixelSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D mapImageGraphics = mapImage.createGraphics();
        AffineTransform original = transform(mapImageGraphics, mapImage.getHeight());

        mapImageGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
        mapImageGraphics.fillRect(0, 0, 64 * tilePixelSize, 64 * tilePixelSize);
        mapImageGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
        mapImageGraphics.setRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));

        for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 64; y++) {
                int localX = (x) * tilePixelSize;
                int localY = (y) * tilePixelSize;

                Integer flagAt = regionInfo.getFlags()[plane][x][y];
                TileFlag tileFlag = new TileFlag();
                tileFlag.setFlag(flagAt);

                mapImageGraphics.setColor(new Color(3, 1, 3, 47));
                mapImageGraphics.fillRect(localX, localY, tilePixelSize, tilePixelSize);

                if (!tileFlag.isWalkable()) {
                    mapImageGraphics.setColor(new Color(50, 109, 255, 223));
                    mapImageGraphics.fillRect(localX, localY, tilePixelSize, tilePixelSize);
                }

                mapImageGraphics.setColor(new Color(249, 122, 39, 223));
                if (tileFlag.blockedNorth()) {
                    mapImageGraphics.fillRect(localX, localY, tilePixelSize, tilePixelSize / 4);
                }
                if (tileFlag.blockedEast()) {
                    mapImageGraphics.fillRect(localX + tilePixelSize - tilePixelSize / 4, localY, tilePixelSize / 4, tilePixelSize);
                }
                if (tileFlag.blockedSouth()) {
                    mapImageGraphics.fillRect(localX, localY + tilePixelSize - tilePixelSize / 4, tilePixelSize, tilePixelSize / 4);
                }
                if (tileFlag.blockedWest()) {
                    mapImageGraphics.fillRect(localX, localY, tilePixelSize / 4, tilePixelSize);
                }
            }
        }

        mapImageGraphics.setTransform(original);
        return mapImage;
    }

    private AffineTransform transform(Graphics2D graphics2D, int height) {
        AffineTransform old = graphics2D.getTransform();
        graphics2D.translate(0, height - 1);
        graphics2D.scale(1, -1);
        return old;
    }

    public BufferedImage createTileFlagImage2(int plane, RegionInfo regionInfo) {
        Region region = xteaService.getRegion(Integer.parseInt(regionInfo.getKey())).orElse(null);
        if (region == null) return null;

        int tilePixelSize = 4;

        int pixelsX = Region.X * tilePixelSize;
        int pixelsY = Region.Y * tilePixelSize;
        BufferedImage image = new BufferedImage(pixelsX, pixelsY, BufferedImage.TYPE_INT_RGB);

        int baseX = region.getBaseX();
        int baseY = region.getBaseY();

        for (int regionX = 0; regionX < 64; regionX++) {
            for (int regionY = 0; regionY < 64; regionY++) {
                int setting = region.getTileSettings()[plane][regionX][regionY];

                int drawX = regionX * tilePixelSize;
                int drawY = (Region.Y - 1 - regionY) * tilePixelSize;

                if (setting == 1) {
                    fillTile(image, drawX, drawY, tilePixelSize, new Color(50, 109, 255, 223));
                } else {
                    fillTile(image, drawX, drawY, tilePixelSize, new Color(215, 216, 216, 255));
                }
            }
        }

        for (SceneEntityInstance location : region.getLocations()) {


            boolean isBridge = (region.getTileSetting(location.getPosition().toLocation()) & 2) != 0;

            int locationType = location.getType();

            int regionX = location.getPosition().getX() - baseX;
            int regionY = location.getPosition().getY() - baseY;

            int drawX = regionX * tilePixelSize;
            int drawY = (Region.Y - 1 - regionY) * tilePixelSize;

            if (location.getPosition().getZ() == plane + 1) {
                if (!isBridge) {
                    continue;
                }
            }
            else if (location.getPosition().getZ() == plane) {
                if (isBridge) {
                    fillTile(image, drawX, drawY, tilePixelSize, new Color(189, 187, 22, 198));
                    continue;
                }

                if ((region.getTileSetting(location.getPosition().toLocation()) & 24) != 0) {
                    continue;
                }
            } else {
                continue;
            }

            if (location.getType() == 9) {
                Set<SceneEntityDefinition> allSceneEntityDefinitions = xteaService.getAllSceneEntityDefinitions(location.getId());

                int hash = (regionX << 7) + regionY + (location.getId() << 14) + 0x4000_0000;
                if ((hash >> 29 & 3) != 2) {
                    continue;
                }

                boolean wall = allSceneEntityDefinitions.stream().anyMatch(sceneEntityDefinition -> sceneEntityDefinition.getMapSceneId() == -1);
                if (wall) {
                    int rgb = new Color(249, 122, 39, 223).getRGB();

                    int orientation = location.getOrientation();
                    if (orientation != 0 && orientation != 2) {
                        //North-West to South-East wall
                        image.setRGB(drawX + 0, drawY + 0, rgb);
                        image.setRGB(drawX + 1, drawY + 1, rgb);
                        image.setRGB(drawX + 2, drawY + 2, rgb);
                        image.setRGB(drawX + 3, drawY + 3, rgb);
                    } else {
                        //North-East to South-West wall
                        image.setRGB(drawX + 0, drawY + 3, rgb);
                        image.setRGB(drawX + 1, drawY + 2, rgb);
                        image.setRGB(drawX + 2, drawY + 1, rgb);
                        image.setRGB(drawX + 3, drawY + 0, rgb);
                    }
                }
            } else if (location.getType() >= 0 && location.getType() <= 3) {
                int rgb = new Color(249, 122, 39, 223).getRGB();

                int rotation = location.getOrientation();

                if (locationType == 0 || locationType == 2) {
                    if (rotation == 0) {
                        //West wall
                        image.setRGB(drawX + 0, drawY + 0, rgb);
                        image.setRGB(drawX + 0, drawY + 1, rgb);
                        image.setRGB(drawX + 0, drawY + 2, rgb);
                        image.setRGB(drawX + 0, drawY + 3, rgb);
                    } else if (rotation == 1) {
                        //North wall
                        image.setRGB(drawX + 0, drawY + 0, rgb);
                        image.setRGB(drawX + 1, drawY + 0, rgb);
                        image.setRGB(drawX + 2, drawY + 0, rgb);
                        image.setRGB(drawX + 3, drawY + 0, rgb);
                    } else if (rotation == 2) {
                        //East wall
                        image.setRGB(drawX + 3, drawY + 0, rgb);
                        image.setRGB(drawX + 3, drawY + 1, rgb);
                        image.setRGB(drawX + 3, drawY + 2, rgb);
                        image.setRGB(drawX + 3, drawY + 3, rgb);
                    } else if (rotation == 3) {
                        //South wall
                        image.setRGB(drawX + 0, drawY + 3, rgb);
                        image.setRGB(drawX + 1, drawY + 3, rgb);
                        image.setRGB(drawX + 2, drawY + 3, rgb);
                        image.setRGB(drawX + 3, drawY + 3, rgb);
                    }
                }

                if (locationType == 3) {
                    if (rotation == 0) {
                        //Pillar North-West
                        image.setRGB(drawX + 0, drawY + 0, rgb);
                    } else if (rotation == 1) {
                        //Pillar North-East
                        image.setRGB(drawX + 3, drawY + 0, rgb);
                    } else if (rotation == 2) {
                        //Pillar South-East
                        image.setRGB(drawX + 3, drawY + 3, rgb);
                    } else if (rotation == 3) {
                        //Pillar South-West
                        image.setRGB(drawX + 0, drawY + 3, rgb);
                    }
                }

                if (locationType == 2) {
                    if (rotation == 3) {
                        //West wall
                        image.setRGB(drawX + 0, drawY + 0, rgb);
                        image.setRGB(drawX + 0, drawY + 1, rgb);
                        image.setRGB(drawX + 0, drawY + 2, rgb);
                        image.setRGB(drawX + 0, drawY + 3, rgb);
                    } else if (rotation == 0) {
                        //North wall
                        image.setRGB(drawX + 0, drawY + 0, rgb);
                        image.setRGB(drawX + 1, drawY + 0, rgb);
                        image.setRGB(drawX + 2, drawY + 0, rgb);
                        image.setRGB(drawX + 3, drawY + 0, rgb);
                    } else if (rotation == 1) {
                        //East wall
                        image.setRGB(drawX + 3, drawY + 0, rgb);
                        image.setRGB(drawX + 3, drawY + 1, rgb);
                        image.setRGB(drawX + 3, drawY + 2, rgb);
                        image.setRGB(drawX + 3, drawY + 3, rgb);
                    } else if (rotation == 2) {
                        //South wall
                        image.setRGB(drawX + 0, drawY + 3, rgb);
                        image.setRGB(drawX + 1, drawY + 3, rgb);
                        image.setRGB(drawX + 2, drawY + 3, rgb);
                        image.setRGB(drawX + 3, drawY + 3, rgb);
                    }
                }
            }
            else {
                Set<SceneEntityDefinition> allSceneEntityDefinitions = xteaService.getAllSceneEntityDefinitions(location.getId());

                Integer clipType = allSceneEntityDefinitions.stream().map(SceneEntityDefinition::getClipType).findAny().orElse(null);

                if (locationType == 22){
                    if (clipType == 1){
                        //block22
                        fillTile(image, drawX, drawY, tilePixelSize, new Color(20, 189, 153, 198));
                    }
                }
                else {
                    if (locationType != 10 && locationType != 11){
                        if(locationType >= 12) {
                            if (clipType != 0){
                                //addObject
                                fillTile(image, drawX, drawY, tilePixelSize, new Color(51, 189, 20, 198));
                            }
                        }
                        else if (locationType == 0){
                            if (clipType != 0){
                                //removeWall
                            }
                        }
                        else if (locationType == 1){
                            if (clipType != 0){
                                //removeWall
                            }
                        }
                        else {
                            if(locationType == 2) {
                                if (clipType != 0){
                                    //removeWall
                                }
                            }
                            else if(locationType == 3) {
                                if (clipType != 0){
                                    //removeWall
                                }
                            }
                            else if(locationType == 9) {
                                if (clipType != 0){
                                    //addObject
                                    fillTile(image, drawX, drawY, tilePixelSize, new Color(51, 189, 20, 198));
                                }
                            }
                            else if(locationType == 4) {
                                //addBoundaryDecoration
                            }
                            else {
                                if(locationType == 5) {
                                    //addBoundaryDecoration
                                }
                                else if (locationType == 6){
                                    //addBoundaryDecoration
                                }
                                else if (locationType == 7){
                                    //addBoundaryDecoration
                                }
                                else if (locationType == 8){
                                    //addBoundaryDecoration
                                }
                            }
                        }
                    }
                    else {
                        if (clipType != 0){
                            //addObject
                            //fillTile(image, drawX, drawY, tilePixelSize, new Color(51, 189, 20, 198));
                        }
                    }
                }

            }
        }

        return image;
    }


    private static void thinking(int locationType, int clipType){

    }


    private void fillTile(BufferedImage image, int drawX, int drawY, int tilePixelSize, Color color) {
        int rgb = color.getRGB();
        for (int x = 0; x < tilePixelSize; x++) {
            for (int y = 0; y < tilePixelSize; y++) {
                image.setRGB(drawX + x, drawY + y, rgb);
            }
        }
    }
}
