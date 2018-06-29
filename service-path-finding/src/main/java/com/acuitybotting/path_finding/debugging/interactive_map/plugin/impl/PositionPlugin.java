package com.acuitybotting.path_finding.debugging.interactive_map.plugin.impl;

import com.acuitybotting.path_finding.debugging.interactive_map.plugin.Plugin;
import com.acuitybotting.path_finding.rs.domain.location.Location;
import com.acuitybotting.path_finding.rs.utils.RegionUtils;
import com.acuitybotting.path_finding.rs.utils.RsEnvironment;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.event.MouseEvent;

@Slf4j
public class PositionPlugin extends Plugin {

    @Override
    public void onPaint(Graphics2D graphics) {
        getPaintUtil().debug("Cursor (Map): " + getPerspective().screenToLocation(this.getMapPanel().getMousePosition()));
        getPaintUtil().debug("Cursor: " + this.getMapPanel().getMousePosition());
        getPaintUtil().debug("View Base: " + getPerspective().getBase());
        getPaintUtil().debug("RsRegion: " + RegionUtils.locationToRegionId(getPerspective().screenToLocation(this.getMapPanel().getMousePosition())));

        Location mouseLocation = getMapPanel().getMouseLocation();
        if (mouseLocation != null){
            getPaintUtil().markLocation(graphics, mouseLocation, Color.BLACK);
            getPaintUtil().connectLocations(graphics, RsEnvironment.getRsMap().getNode(mouseLocation).getNeighbors(), Color.BLACK);
        }
    }

    public void onLoad() {
    }

    public void onClose() {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Location mouseLocation = getMapPanel().getMouseLocation();
        if (mouseLocation != null){
            log.info("Flag at {} is {}.", mouseLocation, RsEnvironment.getRsMap().getFlagAt(mouseLocation));
        }
    }
}

