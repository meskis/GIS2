/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eskis.gis2.Actions;

import com.eskis.gis2.MapFrame;
import java.awt.event.ActionEvent;
import org.geotools.swing.action.SafeAction;

/**
 *
 * @author Marcus
 */
public class ExecuteQueryAction extends SafeAction {
    
    protected MapFrame mapFrame;
    
    
    public ExecuteQueryAction(MapFrame mapFrame)
    {
        super("execute query");
        this.mapFrame = mapFrame;
    }

    @Override
    public void action(ActionEvent ae) throws Throwable {
        mapFrame.executeQuery(mapFrame.searchField.getText());
    }
    
    
}
