/*
    Copyright (C) 2007-2011  Database Group - Universita' della Basilicata
    Giansalvatore Mecca - giansalvatore.mecca@unibas.it
    Salvatore Raunich - salrau@gmail.com

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
 
package it.unibas.spicy.model.algebra;

import it.unibas.spicy.model.mapping.IDataSourceProxy;
import it.unibas.spicy.model.paths.VariablePathExpression;
import it.unibas.spicy.utility.SpicyEngineUtility;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HashProjectOperator implements IProjectOperator {

    private static Log logger = LogFactory.getLog(HashProjectOperator.class);

    public IDataSourceProxy project(IDataSourceProxy dataSource, List<VariablePathExpression> attributePaths) {
        IDataSourceProxy clone = SpicyEngineUtility.cloneAlgebraDataSource(dataSource);
        ProjectWithDuplicatesOperator projectorWithDuplicates = new ProjectWithDuplicatesOperator();
        IDataSourceProxy projectionWithDuplicates = projectorWithDuplicates.projectWithDuplicates(dataSource, attributePaths);
        RemoveDuplicatesOperator duplicateRemover = new RemoveDuplicatesOperator();
        IDataSourceProxy result = duplicateRemover.removeDuplicates(projectionWithDuplicates);
        return result;
    }

}
