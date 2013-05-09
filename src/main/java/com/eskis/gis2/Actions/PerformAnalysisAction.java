/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eskis.gis2.Actions;

import com.eskis.gis2.GUI.GUI;
import com.eskis.gis2.MapFrame;
import java.awt.event.ActionEvent;
import org.geotools.swing.action.SafeAction;

/**
 *
 * @author Marcus
 */
public class PerformAnalysisAction extends SafeAction {
    protected MapFrame mapFrame;
    
    public PerformAnalysisAction(MapFrame map)
    {
        super("Analyze selected area");
        this.mapFrame = map;
    }

    @Override
    public void action(ActionEvent ae) throws Throwable {
        GUI gui = new GUI(mapFrame);
        mapFrame.gui = gui;
        gui.show();
    }
   
}
