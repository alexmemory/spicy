/*
    Copyright (C) 2007-2011  Database Group - Universita' della Basilicata
    Giansalvatore Mecca - giansalvatore.mecca@unibas.it
    Salvatore Raunich - salrau@gmail.com
    Marcello Buoncristiano - marcello.buoncristiano@yahoo.it

    This file is part of ++Spicy - a Schema Mapping and Data Exchange Tool
    
    ++Spicy is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    ++Spicy is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ++Spicy.  If not, see <http://www.gnu.org/licenses/>.
 */
 
package it.unibas.spicygui.controllo.tree;

import it.unibas.spicygui.Costanti;
import it.unibas.spicygui.vista.treepm.TreeNodeAdapter;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openide.util.NbBundle;

public class ActionViewAllSkolemFunctors extends AbstractAction {

    private static Log logger = LogFactory.getLog(ActionViewAllSkolemFunctors.class);
    private JTree albero;
    private boolean visualizzati;

    public ActionViewAllSkolemFunctors(JTree albero) {
        this.albero = albero;
        this.putValue(NAME, NbBundle.getMessage(Costanti.class, Costanti.ACTION_VIEW_ALL_FUNCTORS));
    }

    public void actionPerformed(ActionEvent e) {
        for (int i = 0; i < albero.getRowCount(); i++) {
            TreePath path = albero.getPathForRow(i);
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getLastPathComponent();
            TreeNodeAdapter adapter = (TreeNodeAdapter) treeNode.getUserObject();
            adapter.setSkolem(!visualizzati);
        }
        visualizzati = !visualizzati;
        albero.updateUI();
    }
}
