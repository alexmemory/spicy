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
 
package it.unibas.spicy.test.structuralanalysis;

import it.unibas.spicy.test.TestFixture;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestStructuralAnalisys extends TestFixture {

    private Log logger = LogFactory.getLog(this.getClass());
//    public void testStatDBExpenseDB() {
//        MappingTask mappingTask = statDBExpenseDB;
//        analyzer.runAnalisys(mappingTask);
//        printLog(mappingTask);
//    }
//
//    public void testExpenseDBStatDB() {
//        MappingTask mappingTask = expenseDBstatDB;
//        analyzer.runAnalisys(mappingTask);
//        printLog(mappingTask);
//    }
//
//    private void printLog(MappingTask mappingTask) {
//        DataSource source = mappingTask.getSource();
//        DataSource target = mappingTask.getTarget();
//        if (logger.isDebugEnabled()) logger.debug(source.toLongString());
//        if (logger.isDebugEnabled()) logger.debug(target.toLongString());
//        List<Transformation> transformations = (List<Transformation>)mappingTask.getAnnotation(it.unibas.schemamapper.SchemaMapperConstants.TRANSFORMATIONS);
//        if (logger.isDebugEnabled()) logger.debug(transformations);
//        for (Transformation transformation : transformations) {
//            List<INode> translatedInstances = transformation.getTranslatedInstances();
//            if (logger.isDebugEnabled()) logger.debug(translatedInstances);
//        }
//        List<SimilarityCheck> similarityChecks = (List<SimilarityCheck>)mappingTask.getAnnotation(SpicyConstants.SIMILARITY_CHECKS);
//        for (SimilarityCheck similarityCheck : similarityChecks) {
//            if (logger.isDebugEnabled()) logger.debug(similarityCheck.toStringWithCircuits());
//            List<PathExpression> exclusionList = (List<PathExpression>)similarityCheck.getAnnotation(SpicyConstants.EXCLUSION_LIST);
//            if (logger.isDebugEnabled()) logger.debug(exclusionList);
//            List<SimilarityFeature> features = similarityCheck.getFeatures();
//            if (logger.isDebugEnabled()) logger.debug(features);
//        }
//
//    }
}
