/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eskis.gis2.Tools;

import com.eskis.gis2.MapFrame;
import java.awt.Point;
import java.awt.geom.Point2D;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.swing.event.MapMouseEvent;
import org.geotools.swing.tool.CursorTool;
import java.awt.Rectangle;
import java.io.IOException;
import static java.lang.Math.abs;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Marcus
 */
public class AreaSelectorTool extends CursorTool {

    private MapFrame mapFrame;
    private final Point startPosDevice;
    private final Point2D startPosWorld;
    protected Point startPoint;
    protected Point endPoint;
    protected Rectangle rectangle;
    private boolean dragged;

    public AreaSelectorTool(MapFrame map) {
        this.mapFrame = map;
        startPosDevice = new Point();
        startPosWorld = new DirectPosition2D();
    }

    @Override
    public void onMouseClicked(MapMouseEvent ev) {
        try {
            java.awt.Point screenPos = ev.getPoint();
            Rectangle rectangle = new Rectangle(screenPos.x - 2, screenPos.y - 2, 5, 5);
            this.mapFrame.selectFeatures(rectangle, mapFrame.getSelectedLayer(), "");
        } catch (IOException ex) {
            Logger.getLogger(AreaSelectorTool.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void onMouseDragged(MapMouseEvent ev) {
        dragged = true;
    }

    @Override
    public void onMouseReleased(MapMouseEvent ev) {
        endPoint = ev.getPoint();

        System.out.println("Selected X:" + startPosWorld.getX());
        System.out.println("Selected Y:" + startPosWorld.getY());

        rectangle = new Rectangle(
                (int) startPosDevice.getX(),
                (int) startPosDevice.getY(),
                abs((int) (ev.getX() - startPosDevice.getX())),
                abs((int) (ev.getY() - startPosDevice.getY())));
        
        dragged = false;

        mapFrame.setSelectedRectangle(rectangle);

        System.out.println("Selected Rect: " + rectangle.toString());
    }

    @Override
    public void onMousePressed(MapMouseEvent ev) {
        startPosDevice.setLocation(ev.getPoint());
        startPosWorld.setLocation(ev.getWorldPos());
    }

    @Override
    public boolean drawDragBox() {
        return true;
    }
}