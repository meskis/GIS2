/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eskis.gis2.Actions;

import com.eskis.gis2.MapFrame;
import com.eskis.gis2.Tools.AreaSelectorTool;
import java.awt.event.ActionEvent;
import org.geotools.swing.action.SafeAction;

/**
 *
 * @author Marcus
 */
public class AreaSelectorAction extends SafeAction {
    private final MapFrame mapFrame;
    
        public AreaSelectorAction(MapFrame map){
        super("Mark area for analysis");
        mapFrame = map;
    }

    @Override
    public void action(ActionEvent ae) throws Throwable {
        this.mapFrame.getMapPane().setCursorTool(new AreaSelectorTool(mapFrame));
    }
}
