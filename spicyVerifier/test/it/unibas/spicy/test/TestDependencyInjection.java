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
 
package it.unibas.spicy.test;

import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestDependencyInjection extends TestCase {
    
    private Log logger = LogFactory.getLog(this.getClass());
        
//    public void testStructuralAnalysis() {
//        MappingTask mappingTask = new MockMappingTaskStatDBExpenseDB().getMappingTask();
//        PerformStructuralAnalysis analyzer = (PerformStructuralAnalysis)Application.getInstance().getComponentInstance(PerformStructuralAnalysis.class);
//        analyzer.runAnalisys(mappingTask);
//        printLog(mappingTask);
//                
//    }

//    public void testFindMappings() {
//        DataSource source = MockSourceStatDB.getInstance();
//        DataSource target = MockSourceExpenseDB.getInstance();
//        MappingTask mappingTask = new MappingTask(source, target);
//        IFindBestMappingsStrategy mappingFinder = (IFindBestMappingsStrategy)Application.getInstance().getComponentInstance(FindMappingsMatchMapCheck.class);
//        List<MappingTask> mappingTasks = mappingFinder.findBestMappings(mappingTask);
//        //printLog(mappingTasks);
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
//    }
//
//    private void printLog(List<MappingTask> mappingTasks) {
//        for (MappingTask mappingTask : mappingTasks) {
//            if (logger.isDebugEnabled()) logger.debug(mappingTask.toLongString());
//            List<Transformation> transformations = (List<Transformation>)mappingTask.getAnnotation(it.unibas.schemamapper.SchemaMapperConstants.TRANSFORMATIONS);
//            if (logger.isDebugEnabled()) logger.debug(transformations);
//            for (Transformation transformation : transformations) {
//                List<INode> translatedInstances = transformation.getTranslatedInstances();
//                if (logger.isDebugEnabled()) logger.debug(translatedInstances);
//            }
//            List<SimilarityCheck> similarityChecks = (List<SimilarityCheck>)mappingTask.getAnnotation(SpicyConstants.SIMILARITY_CHECKS);
//            for (SimilarityCheck similarityCheck : similarityChecks) {
//                if (logger.isDebugEnabled()) logger.debug(similarityCheck.toStringWithCircuits());
//                List<SimilarityFeature> features = similarityCheck.getFeatures();
//                if (logger.isDebugEnabled()) logger.debug(features);
//            }
//        }
//    }
}
