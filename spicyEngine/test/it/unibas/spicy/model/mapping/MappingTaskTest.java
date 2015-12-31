/**
 * 
 */
package it.unibas.spicy.model.mapping;

import it.unibas.spicy.model.algebra.Compose;
import it.unibas.spicy.model.algebra.IAlgebraOperator;
import it.unibas.spicy.model.algebra.Nest;
import it.unibas.spicy.model.algebra.NestSourceViewTupleVisitorStandAlone;
import it.unibas.spicy.model.algebra.operators.GenerateAlgebraTree;
import it.unibas.spicy.model.algebra.operators.NormalizeViewForExecutionPlan;
import it.unibas.spicy.model.correspondence.ValueCorrespondence;
import it.unibas.spicy.model.datasource.DataSource;
import it.unibas.spicy.model.datasource.INode;
import it.unibas.spicy.model.datasource.JoinCondition;
import it.unibas.spicy.model.datasource.nodes.SetNode;
import it.unibas.spicy.model.datasource.values.INullValue;
import it.unibas.spicy.model.datasource.values.OID;
import it.unibas.spicy.model.mapping.operators.GenerateCandidateSTTGDs;
import it.unibas.spicy.model.mapping.operators.GenerateSolution;
import it.unibas.spicy.model.mapping.proxies.ConstantDataSourceProxy;
import it.unibas.spicy.model.paths.PathExpression;
import it.unibas.spicy.model.paths.SetAlias;
import it.unibas.spicy.model.paths.VariableCorrespondence;
import it.unibas.spicy.model.paths.VariablePathExpression;
import it.unibas.spicy.parser.operators.ParseMappingTask;
import it.unibas.spicy.persistence.DAOMappingTaskLines;
import it.unibas.spicy.persistence.xml.DAOXsd;
import it.unibas.spicy.utility.SpicyEngineConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

/**
 * Not a real test. Just used to gain familiarity. Remove this.
 * 
 */
public class MappingTaskTest extends TestCase {

    public void notATestLoadATgdFile() {
        ParseMappingTask pmt = new ParseMappingTask();
        try {
            MappingTask mt = pmt
                .generateMappingTask("/Users/memorya/Dropbox/Working/20150401_Spicy_examples/bookPublisher/bookPublisherGenerate.tgd");
            System.out.println(mt);
            for (FORule rule : mt.getLoadedTgds()) {
                System.out.println("RULE:");
                for (VariableCorrespondence vc : rule
                         .getCoveredCorrespondences()) {
                    System.out.println("VARC:");
                    System.out.println(vc);
                    for (VariablePathExpression pe : vc.getSourcePaths()) {
                        System.out.println("PE:");
                        System.out.println(pe);
                        System.out.println(pe.getPathSteps());
                    }
                }
                System.out.println("\n");
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void notATestScratch() {
        ParseMappingTask pmt = new ParseMappingTask();
        // pmt.cre
    }

    public void notATestLoadMappingTaskFileGenTgdsAndGenInstance() {
        // String dir = "/Users/memorya/Git/other/schema-mapping/data/Spicy/";
        String dir = "/Users/alex/Git/schema-mapping/data/Spicy/";
        try {
            // MappingTask mt = new DAOMappingTaskLines().loadMappingTask(dir
            // + "examples/bookPublisher/bookPublisher-mappingTask.xml");
            // MappingTask mt = new DAOMappingTaskLines().loadMappingTask(dir
            // + "projTask/projTask-mappingTask.xml");
            MappingTask mt = new DAOMappingTaskLines().loadMappingTask(dir
                                                                       + "projTask05/projTask-mappingTask.xml");
            System.out.println(mt);

            System.out.println("====================================");
            System.out.println("====================================");
            System.out.println("============ gen tgds starting =====");
            System.out.println("====================================");
            System.out.println("====================================");

            /*
             * Generate TGDs
             */

            List<FORule> tgds = new GenerateCandidateSTTGDs()
                .generateCandidateTGDs(mt);

            IDataSourceProxy targetInstance = new GenerateSolution()
                .generateSolution(mt);

            for (INode inst : targetInstance.getInstances()) {
                for (INode rel : inst.getChildren()) {
                    System.out.println(rel);
                    for (INode tup : rel.getChildren()) {
                        System.out.println(tup);
                        for (INode att : tup.getChildren()) {
                            System.out.println(att);
                            System.out.println(att.getLabel());
                            // System.out.println(att.getLabel);
                            // System.out.println(att.getValue());
                            for (INode val : att.getChildren()) {
                                System.out.println(val.getValue());
                            }
                        }
                    }
                }
            }

            // for (INode inst : targetInstance.getInstances()) {
            // INode rel = inst.getChildren().get(0);
            // rel.getLabel();
            // // rel.get
            // }

            for (FORule tgd : tgds) {
                System.out.println("=========TGD:=========");
                System.out.println("TGD:" + tgd.toLogicalString(mt));

                SimpleConjunctiveQuery sq = tgd.getSimpleSourceView();
                // System.out.println("SQ:"+sq);
                List<SetAlias> svars = sq.getVariables();
                System.out.println("SVARS:" + svars);
                System.out.println("SVARS len:" + svars.size());

                SimpleConjunctiveQuery tq = tgd.getTargetView();
                // System.out.println("TQ:"+tq);
                List<SetAlias> tvars = tq.getVariables();
                System.out.println("TVARS:" + tvars);
                System.out.println("TVARS len:" + tvars.size());
            }
            // System.out.println(tgds);
            //
            // System.out.println("====================================");
            // System.out.println("====================================");
            // System.out.println("============ gen tgds done =========");
            // System.out.println("====================================");
            // System.out.println("====================================");
            //
            // System.out.println(mt);
            //
            // System.out.println("====================================");
            // System.out.println("====================================");
            // System.out.println("============ load tgds done ========");
            // System.out.println("====================================");
            // System.out.println("====================================");
            //
            // mt.setLoadedTgds(tgds);
            // System.out.println(mt);
            //
            // System.out.println("====================================");
            // System.out.println("====================================");
            // System.out.println("============ gen targ inst starting=");
            // System.out.println("====================================");
            // System.out.println("====================================");
            //
            // IDataSourceProxy targetInstance = new GenerateSolution()
            // .generateSolution(mt);
            //
            // System.out.println(targetInstance);
            // System.out.println("====================================");
            // System.out.println("====================================");
            // System.out.println("============ gen targ inst done ====");
            // System.out.println("====================================");
            // System.out.println("====================================");
            // System.out.println(mt);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void notATestGenTargInstForArbitTgd() {
        DAOXsd daoXSD = new DAOXsd();
        try {
            // String dir =
            // "/Users/memorya/Git/other/schema-mapping/data/Spicy/projTask05/";
            String dir = "/Users/alex/Git/schema-mapping/data/Spicy/projTask05/";
            IDataSourceProxy source = daoXSD.loadSchema(dir
                                                        + "projTask-source.xsd");
            daoXSD.loadInstance(source, dir + "projTask-source-instance.xml");
            IDataSourceProxy target = daoXSD.loadSchema(dir
                                                        + "projTask-target.xsd");

            // System.out.println(target);
            MappingTask mt = new MappingTask(source, target,
                                             SpicyEngineConstants.TGD_BASED_MAPPING_TASK);

            /*
             * Add a TGD manually
             */

            List<FORule> tgds = new ArrayList<FORule>();

            /*
             * SOURCE
             */

            SimpleConjunctiveQuery view = new SimpleConjunctiveQuery();
            SetAlias projVar = source.getMappingData().getVariables().get(0);
            view.addVariable(projVar);
            NormalizeViewForExecutionPlan viewNormalizer = new NormalizeViewForExecutionPlan();
            SimpleConjunctiveQuery simpleSourceView = viewNormalizer
                .normalizeView(view);
            ComplexConjunctiveQuery sourceView = new ComplexConjunctiveQuery(
                                                                             simpleSourceView);
            ComplexQueryWithNegations complexQueryWithNegations = new ComplexQueryWithNegations(
                                                                                                sourceView);

            /*
             * TARGET
             */

            SimpleConjunctiveQuery viewT = new SimpleConjunctiveQuery();
            SetAlias taskVar = target.getMappingData().getVariables().get(0);
            viewT.addVariable(taskVar);
            viewNormalizer = new NormalizeViewForExecutionPlan();
            SimpleConjunctiveQuery normalizedTargetView = viewNormalizer
                .normalizeView(viewT);

            /*
             * VARIABLE CORRESPONDENCES
             */

            List<VariableCorrespondence> variableCorrespondences = new ArrayList<VariableCorrespondence>();
            List<String> projNamePath = new ArrayList<String>();
            projNamePath.add("proj");
            projNamePath.add("name");
            VariablePathExpression projName = new VariablePathExpression(
                                                                         projVar, projNamePath);
            List<String> taskTitlePath = new ArrayList<String>();
            taskTitlePath.add("task");
            taskTitlePath.add("title");
            VariablePathExpression taskTitle = new VariablePathExpression(
                                                                          taskVar, taskTitlePath);

            VariableCorrespondence correspondence1 = new VariableCorrespondence(
                                                                                projName, taskTitle);
            variableCorrespondences.add(correspondence1);

            /*
             * TGD, combining source and target views and variable
             * correspondences
             */

            FORule tgd = new FORule(complexQueryWithNegations,
                                    normalizedTargetView, variableCorrespondences);
            tgds.add(tgd);
            mt.setLoadedTgds(tgds);
            // System.out.println(mt);

            /*
             * Generate target instance
             */

            IDataSourceProxy targetInstance = new GenerateSolution()
                .generateSolution(mt);

            // System.out.println(targetInstance); // Won't show instance
            // for (INode inode : targetInstance.getInstances()) {
            // System.out.println(inode);
            // }
            System.out.println("====================================");
            System.out.println("====================================");
            System.out.println("============ gen targ inst done ====");
            System.out.println("====================================");
            System.out.println("====================================");
            System.out.println(mt);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void notATestGenCreates2ForArbitTgd() {
        DAOXsd daoXSD = new DAOXsd();
        try {
            // String dir =
            // "/Users/memorya/Git/other/schema-mapping/data/Spicy/projTask05/";
            String dir = "/Users/alex/Git/schema-mapping/data/Spicy/projTask05/";
            IDataSourceProxy source = daoXSD.loadSchema(dir
                                                        + "projTask-source.xsd");
            daoXSD.loadInstance(source, dir + "projTask-source-instance.xml");
            IDataSourceProxy target = daoXSD.loadSchema(dir
                                                        + "projTask-target.xsd");

            System.out.println(target);
            MappingTask mt = new MappingTask(source, target,
                                             SpicyEngineConstants.TGD_BASED_MAPPING_TASK);

            /*
             * Add a TGD manually
             */

            List<FORule> tgds = new ArrayList<FORule>();

            /*
             * SOURCE
             */

            SimpleConjunctiveQuery view = new SimpleConjunctiveQuery();
            SetAlias projVar = source.getMappingData().getVariables().get(0);
            view.addVariable(projVar);
            NormalizeViewForExecutionPlan viewNormalizer = new NormalizeViewForExecutionPlan();
            SimpleConjunctiveQuery simpleSourceView = viewNormalizer
                .normalizeView(view);
            ComplexConjunctiveQuery sourceView = new ComplexConjunctiveQuery(
                                                                             simpleSourceView);
            ComplexQueryWithNegations complexQueryWithNegations = new ComplexQueryWithNegations(
                                                                                                sourceView);

            /*
             * TARGET
             */

            SimpleConjunctiveQuery viewT = new SimpleConjunctiveQuery();
            SetAlias taskVar = target.getMappingData().getVariables().get(0);
            viewT.addVariable(taskVar);
            viewNormalizer = new NormalizeViewForExecutionPlan();
            SimpleConjunctiveQuery normalizedTargetView = viewNormalizer
                .normalizeView(viewT);

            /*
             * VARIABLE CORRESPONDENCES
             */

            List<VariableCorrespondence> variableCorrespondences = new ArrayList<VariableCorrespondence>();
            List<String> projNamePath = new ArrayList<String>();
            projNamePath.add("proj");
            projNamePath.add("name");
            VariablePathExpression projName = new VariablePathExpression(
                                                                         projVar, projNamePath);
            List<String> taskTitlePath = new ArrayList<String>();
            taskTitlePath.add("task");
            taskTitlePath.add("title");
            VariablePathExpression taskTitle = new VariablePathExpression(
                                                                          taskVar, taskTitlePath);

            VariableCorrespondence correspondence1 = new VariableCorrespondence(
                                                                                projName, taskTitle);
            variableCorrespondences.add(correspondence1);

            /*
             * TGD, combining source and target views and variable
             * correspondences
             */

            FORule tgd = new FORule(complexQueryWithNegations,
                                    normalizedTargetView, variableCorrespondences);
            tgds.add(tgd);
            mt.setLoadedTgds(tgds);
            System.out.println(mt);

            /*
             * Generate target instance
             */

            IDataSourceProxy targetInstance = new GenerateSolution()
                .generateSolution(mt);

            System.out.println(targetInstance); // Won't show instance
            // for (INode inode : targetInstance.getInstances()) {
            // System.out.println(inode);
            // }
            System.out.println("====================================");
            System.out.println("====================================");
            System.out.println("============ gen targ inst done ====");
            System.out.println("====================================");
            System.out.println("====================================");
            System.out.println(mt);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void notATestGenerateCandidateTGDs() {
        DAOXsd daoXSD = new DAOXsd();
        try {

            /*
             * Source and target
             */

            String dir = "/Users/memorya/Git/other/schema-mapping/data/Spicy/projTask/";
            IDataSourceProxy source = daoXSD.loadSchema(dir
                                                        + "projTask-source.xsd");
            daoXSD.loadInstance(source, dir + "projTask-source-instance.xml");
            IDataSourceProxy target = daoXSD.loadSchema(dir
                                                        + "projTask-target.xsd");

            /*
             * Correspondences (value correspondences)
             */

            List<String> projNamePath = new ArrayList<String>();
            projNamePath.add("source");
            projNamePath.add("projSet");
            projNamePath.add("proj");
            projNamePath.add("name");
            PathExpression projName = new PathExpression(projNamePath);

            List<String> taskTitlePath = new ArrayList<String>();
            taskTitlePath.add("target");
            taskTitlePath.add("taskSet");
            taskTitlePath.add("task");
            taskTitlePath.add("title");
            PathExpression taskTitle = new PathExpression(taskTitlePath);

            ValueCorrespondence correspondence = new ValueCorrespondence(
                                                                         projName, taskTitle);

            /*
             * Mapping task
             */

            MappingTask mt = new MappingTask(source, target,
                                             SpicyEngineConstants.TGD_BASED_MAPPING_TASK);
            mt.addCorrespondence(correspondence);
            System.out.println(mt);

            /*
             * Generate TGDs
             */

            System.out.println("starting");
            List<FORule> tgds = new GenerateCandidateSTTGDs()
                .generateCandidateTGDs(mt);
            System.out.println(tgds);
            System.out.println("done");

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void notATestGenCreates2ForArbitTgd2() {
        String dir = "/Users/memorya/Git/other/schema-mapping/data/Spicy/";
        // String dir = "/Users/alex/Git/schema-mapping/data/Spicy/";
        try {
            // MappingTask mt = new DAOMappingTaskLines().loadMappingTask(dir
            // + "examples/bookPublisher/bookPublisher-mappingTask.xml");
            // MappingTask mt = new DAOMappingTaskLines().loadMappingTask(dir
            // + "projTask/projTask-mappingTask.xml");
            MappingTask mt = new DAOMappingTaskLines().loadMappingTask(dir
                                                                       + "projTask05/projTask-mappingTask.xml");
            System.out.println(mt);

            System.out.println("====================================");
            System.out.println("====================================");
            System.out.println("============ gen tgds starting =====");
            System.out.println("====================================");
            System.out.println("====================================");

            /*
             * Generate TGDs
             */

            List<FORule> tgds = new GenerateCandidateSTTGDs()
                .generateCandidateTGDs(mt);

            mt.setLoadedTgds(tgds);
            System.out.println(mt);

            // IDataSourceProxy targetInstance = new GenerateSolution()
            // .generateSolution(mt);

            // for (INode inst : targetInstance.getInstances()) {
            // for (INode rel : inst.getChildren()) {
            // System.out.println(rel);
            // for (INode tup : rel.getChildren()) {
            // System.out.println(tup);
            // for (INode att : tup.getChildren()) {
            // System.out.println(att);
            // System.out.println(att.getLabel());
            // // System.out.println(att.getLabel);
            // // System.out.println(att.getValue());
            // for (INode val : att.getChildren()) {
            // System.out.println(val.getValue());
            // }
            // }
            // }
            // }
            // }

            // for (INode inst : targetInstance.getInstances()) {
            // INode rel = inst.getChildren().get(0);
            // rel.getLabel();
            // // rel.get
            // }

            for (FORule tgd : tgds) {
                System.out.println("=========TGD:=========");
                System.out.println("TGD:" + tgd.toLogicalString(mt));

                // SimpleConjunctiveQuery sq = tgd.getSimpleSourceView();
                // // System.out.println("SQ:"+sq);
                // List<SetAlias> svars = sq.getVariables();
                // System.out.println("SVARS:" + svars);
                // System.out.println("SVARS len:" + svars.size());
                //
                // SimpleConjunctiveQuery tq = tgd.getTargetView();
                // // System.out.println("TQ:"+tq);
                // List<SetAlias> tvars = tq.getVariables();
                // System.out.println("TVARS:" + tvars);
                // System.out.println("TVARS len:" + tvars.size());
            }

            // System.out.println(tgds);

            System.out.println("====================================");
            System.out.println("====================================");
            System.out.println("============ gen tgds done =========");
            System.out.println("====================================");
            System.out.println("====================================");

            System.out.println("====================================");
            System.out.println("====================================");
            System.out.println("============ gnd tgds starting =====");
            System.out.println("====================================");
            System.out.println("====================================");

            DAOXsd daoXSD = new DAOXsd();
            IDataSourceProxy source = daoXSD.loadSchema(dir
                                                        + "projTask05/projTask-source.xsd");
            IDataSourceProxy target = daoXSD.loadSchema(dir
                                                        + "projTask05/projTask-target.xsd");

            FORule aTgd = tgds.get(2).clone();
            ComplexQueryWithNegations cSrcQ = aTgd.getComplexSourceQuery()
                .clone();
            FORule groundingTgd = aTgd.clone();
            groundingTgd.setTargetView(aTgd.getSimpleSourceView());
            // IAlgebraOperator srcAlgTree = cSrcQ.getAlgebraTree();

            MappingTask groundingMt = new MappingTask(source, source,
                                                      SpicyEngineConstants.TGD_BASED_MAPPING_TASK);

            System.out.println("old value correspondences "
                               + mt.getValueCorrespondences());
            System.out.println("Old variables " + cSrcQ.getVariables());

            System.out.println(groundingTgd.toLogicalString(groundingMt));
            List<FORule> groundingTgds = new ArrayList<FORule>();
            groundingTgds.add(groundingTgd);
            IAlgebraOperator srcAlgTree = new GenerateAlgebraTree()
                .generateTreeForRules(mt, groundingTgds);
            System.out.println(srcAlgTree);
            // mt.getSourceProxy()
            IDataSourceProxy srcGnd = srcAlgTree.execute(mt.getSourceProxy());

            System.out.println("====================================");
            System.out.println("====================================");
            System.out.println("============ getInstances      =====");
            System.out.println("====================================");
            System.out.println("====================================");

            for (INode inst : srcGnd.getInstances()) {
                System.out.println("Inst:\n");
                System.out.println(inst);
                // for (INode rel : inst.getChildren()) {
                // System.out.println("grounding:\n" + rel);
                // for (INode tup : rel.getChildren()) {
                // System.out.println("binding:\n" + tup);
                // }
                // }
            }

            // Both blank
            // srcGnd.getOriginalInstances()
            // srcGnd.getIntermediateInstances()

            INode aInst = srcGnd.getInstances().get(0);
            // aInst.
            System.out.println("a inst:\n" + aInst);
            System.out.println("a child:\n" + aInst.getChild(0).getLabel());
            INode aGnd = srcGnd.getInstances().get(0).getChild(0);
            INode newInst = new SetNode("new inst");
            newInst.setRoot(true);
            newInst.addChild(aGnd);
            aGnd.setFather(newInst);
            System.out.println("a grounding:\n" + aGnd);

            System.out.println("====================================");
            System.out.println("====================================");
            System.out.println("============ gnd to instance========");
            System.out.println("====================================");
            System.out.println("====================================");

            // INode schemaClone = mt.getSourceProxy().getIntermediateSchema()
            // .clone();
            DataSource singleGroundingSrcDataSource = new DataSource(
                                                                     SpicyEngineConstants.TYPE_ALGEBRA_RESULT,
                                                                     source.getIntermediateSchema());
            IDataSourceProxy singleGroundingSrcDataSourceProxy = new ConstantDataSourceProxy(
                                                                                             singleGroundingSrcDataSource);

            for (JoinCondition cond : mt.getSourceProxy().getJoinConditions()) {
                singleGroundingSrcDataSourceProxy.addJoinCondition(cond);
            }

            // for (INode instance : dataSource.getInstances()) {
            // clone.addInstance(aGnd.clone());
            // singleGroundingSrcDataSource.addInstance(newInst.clone());
            singleGroundingSrcDataSource.addInstanceWithCheck(newInst.clone());
            // }

            System.out.println("====================================");
            System.out.println("====================================");
            System.out.println("============ load single tgd========");
            System.out.println("====================================");
            System.out.println("====================================");

            tgds.clear();
            // aTgd.getComplexSourceQuery().getAlgebraTree().
            tgds.add(aTgd);
            // mt.setLoadedTgds(tgds);
            // System.out.println(mt);

            // Allows old data in? Probably not...
            // MappingTask newMt = new MappingTask(
            // singleGroundingSrcDataSourceProxy, mt.getTargetProxy(),
            // SpicyEngineConstants.TGD_BASED_MAPPING_TASK);

            // Missing join conditions?
            MappingTask newMt = new MappingTask(
                                                singleGroundingSrcDataSourceProxy, target,
                                                SpicyEngineConstants.TGD_BASED_MAPPING_TASK);
            newMt.setValueCorrespondences(mt.getValueCorrespondences());
            newMt.setLoadedTgds(tgds);
            System.out.println(newMt);

            System.out.println("====================================");
            System.out.println("====================================");
            System.out.println("============ DE on gnd insta========");
            System.out.println("====================================");
            System.out.println("====================================");

            IDataSourceProxy targetInstance = new GenerateSolution()
                .generateSolution(newMt);
            System.out.println(newMt);
            // IDataSourceProxy targetInstance = new GenerateSolution()
            // .generateSolution(mt);
            // System.out.println(mt);

            // System.out.println("====================================");
            // System.out.println("====================================");
            // System.out.println("============ load tgds done ========");
            // System.out.println("====================================");
            // System.out.println("====================================");

            // mt.setLoadedTgds(tgds);
            // System.out.println(mt);

            // System.out.println("====================================");
            // System.out.println("====================================");
            // System.out.println("============ gen targ inst starting=");
            // System.out.println("====================================");
            // System.out.println("====================================");

            // IDataSourceProxy targetInstance = new GenerateSolution()
            // .generateSolution(mt);

            // System.out.println(targetInstance);
            // System.out.println("====================================");
            // System.out.println("====================================");
            // System.out.println("============ gen targ inst done ====");
            // System.out.println("====================================");
            // System.out.println("====================================");
            // System.out.println(mt);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void testGenCreates2ForArbitTgd3() {
        // String dir = "/Users/memorya/Git/other/schema-mapping/data/Spicy/";
        String dir = "/home/alex/Git/schema-mapping/data/Spicy/";
        // String dir = "/Users/alex/Git/schema-mapping/data/Spicy/";
        try {
            // MappingTask mt = new DAOMappingTaskLines().loadMappingTask(dir
            // + "examples/bookPublisher/bookPublisher-mappingTask.xml");
            // MappingTask mt = new DAOMappingTaskLines().loadMappingTask(dir
            // + "projTask/projTask-mappingTask.xml");
            // String mtFile = "projTask05/projTask-mappingTask.xml";
            // String mtFile = "projTask06/projTask-mappingTask.xml";
            // String mtFile = "projTask07/projTask-mappingTask.xml";
            // String mtFile = "projTask08/projTask-mappingTask.xml";
            // String mtFile = "projTask09/projTask-mappingTask.xml";
            String mtFile = "projTask10/projTask-mappingTask.xml";
            MappingTask mt = new DAOMappingTaskLines().loadMappingTask(dir
                                                                       + mtFile);
            System.out.println(mt);

            System.out.println("====================================");
            System.out.println("====================================");
            System.out.println("============ gen tgds starting =====");
            System.out.println("====================================");
            System.out.println("====================================");

            /*
             * Generate TGDs
             */

            List<FORule> tgds = new GenerateCandidateSTTGDs()
                .generateCandidateTGDs(mt);

            // mt.setLoadedTgds(tgds);
            // System.out.println(mt);

            for (FORule tgd : tgds) {
                System.out.println("=========TGD:=========");
                System.out.println("TGD:" + tgd.toLogicalString(mt));
            }

            // System.out.println(tgds);

            System.out.println("====================================");
            System.out.println("====================================");
            System.out.println("============ gen tgds done =========");
            System.out.println("====================================");
            System.out.println("====================================");

            System.out.println("====================================");
            System.out.println("====================================");
            System.out.println("=====per-tgd solution starting =====");
            System.out.println("====================================");
            System.out.println("====================================");

            System.out.println("TGD count:" + tgds.size());
            // FORule aTgd = tgds.get(2).clone();
            // FORule aTgd = tgds.get(10).clone();
            FORule aTgd = tgds.get(tgds.size() - 1).clone();
            System.out.println("a TGD:" + aTgd.toLogicalString(mt));

            mt = new DAOMappingTaskLines().loadMappingTask(dir + mtFile);
            mt.setLoadedTgds(Arrays.asList(aTgd));

            // IDataSourceProxy solution = mt.getMappingData().getAlgebraTree()
            // .execute(mt.getSourceProxy());
            // IAlgebraOperator at = mt.getMappingData().getAlgebraTree();
            Compose compose = (Compose) mt.getMappingData().getAlgebraTree();
            List<IAlgebraOperator> merges = compose.getChildren();
            if (merges.size() != 1)
                throw new Exception("Unexpected algebra tree");
            List<IAlgebraOperator> nests = merges.get(0).getChildren();
            if (nests.size() != 1)
                throw new Exception("Unexpected algebra tree");
            Nest nest = (Nest) nests.get(0);

            IDataSourceProxy sourceView = nest.getChildren().get(0)
                .execute(mt.getSourceProxy());

            ComplexConjunctiveQuery exampleCCQ = aTgd.getComplexSourceQuery()
                .getComplexQuery();
            SimpleConjunctiveQuery exampleSCQ = exampleCCQ.getConjunctions()
                .get(0);
            // IDataSourceProxy nestResult = new NestOperator().nest(child,
            // aTgd,
            // mt.getTargetProxy(), nest.getGenerators(),
            // nest.getNodeCache(), mt);
            INode targetSchemaClone = mt.getTargetProxy()
                .getIntermediateSchema().clone();
            for (int i = 0; i < sourceView.getInstances().size(); i++) {
                INode sourceViewInstance = sourceView.getInstances().get(i);
                if (sourceViewInstance.getChildren().isEmpty()) {
                    continue;
                }
                for (INode sourceViewTuple : sourceViewInstance.getChildren()) {
                    Map<String, INode> nodeCache = new HashMap<String, INode>();
                    IDataSourceProxy result = new ConstantDataSourceProxy(
                                                                          new DataSource(
                                                                                         SpicyEngineConstants.TYPE_ALGEBRA_RESULT,
                                                                                         targetSchemaClone));
                    // result.getDataSource().getInstances().clear();
                    for (SetAlias targetVariable : aTgd.getTargetView()
                             .getVariables()) {
                        NestSourceViewTupleVisitorStandAlone visitor = new NestSourceViewTupleVisitorStandAlone(
                                                                                                                aTgd, sourceViewTuple, i, nodeCache, nest
                                                                                                                .getGenerators()
                                                                                                                .getGeneratorsForVariable(
                                                                                                                                          targetVariable), result, mt);
                        targetSchemaClone.accept(visitor);
                    }
                    List<INode> instances = result.getDataSource()
                        .getInstances();
                    if (instances.size() != 1)
                        throw new Exception("Unexpected number of instances: "
                                            + instances.size());
                    System.out.println("====================================");
                    System.out.println("=====per-grounding solution :  =====");
                    System.out.println("====================================");
                    System.out.println(instances.get(0));

                    /*
                     * Prepare to convert into a query
                     */
                    SimpleConjunctiveQuery conjunction = new SimpleConjunctiveQuery();

                    // SetAlias projVar = mt.getTargetProxy().getMappingData()
                    // .getVariables().get(0);
                    // view.addVariable(projVar);
                    // NormalizeViewForExecutionPlan viewNormalizer = new
                    // NormalizeViewForExecutionPlan();
                    // SimpleConjunctiveQuery simpleSourceView = viewNormalizer
                    // .normalizeView(view);
                    // ComplexConjunctiveQuery sourceView = new
                    // ComplexConjunctiveQuery(
                    // simpleSourceView);
                    // ComplexQueryWithNegations complexQueryWithNegations = new
                    // ComplexQueryWithNegations(
                    // sourceView);

                    /*
                     * Print the atoms
                     */
                    System.out
                        .println("schema: " + instances.get(0).getLabel());
                    for (INode relation : instances.get(0).getChildren()) {
                        System.out.println("relationSet: "
                                           + relation.getLabel());
                        for (INode atom : relation.getChildren()) {
                            System.out.println("relation: " + atom.getLabel());
                            // System.out.println(atom);

                            List<VariablePathExpression> fromPaths = new ArrayList<VariablePathExpression>();
                            List<String> pathSteps = new ArrayList<String>();
                            pathSteps.add(atom.getLabel());
                            // SetAlias startingVariable = new SetAlias();
                            // VariablePathExpression fromPath = new
                            // VariablePathExpression();

                            // List<VariableJoinCondition> joinConditions = new
                            // ArrayList<VariableJoinCondition>();
                            // VariableJoinCondition joinCondition = new
                            // VariableJoinCondition();

                            // Print the atom
                            System.out.println(atom.getLabel());
                            for (INode attribute : atom.getChildren()) {
                                pathSteps.add(attribute.getLabel());

                                System.out.print("  " + attribute.getLabel()
                                                 + " = ");
                                List<INode> leaves = attribute.getChildren();
                                if (leaves.size() != 1)
                                    throw new Exception(
                                                        "Unexpected number of leaves: "
                                                        + leaves.size());
                                INode leaf = leaves.get(0);
                                // System.out.println("leaf value class: "
                                // + leaf.getValue().getClass());

                                if (leaf.getValue() instanceof OID) {	
                                    OID oid = (OID) leaf.getValue();
                                    System.out.print("(variable id: "
                                                     + oid.getValue() + ") ");
                                    System.out.println(oid.getSkolemString());
                                } else if (leaf.getValue() instanceof INullValue) {
                                    INullValue inull = (INullValue) leaf
                                        .getValue();
                                    System.out.println("(null value: "
                                                       + inull.toString() + ") ");
                                } else {
                                    System.out.println(leaf.getValue());
                                }
                            }
                            System.out.println("\n");

                        }
                    }
                }
            }

            // IDataSourceProxy solution = compose.execute(mt.getSourceProxy());

            // IDataSourceProxy targetProxy = mt.getTargetProxy();
            // targetProxy.getInstances().clear();
            // for (INode instance : solution.getInstances()) {
            // System.out.println("Inst:\n");
            // System.out.println(instance);
            // // targetProxy.addInstanceWithCheck(instance);
            // // for (INode grounding :
            // // instance.getChildren().get(0).getChildren()) {
            // // System.out.println("result of a source grounding:\n" +
            // // grounding);
            // // // for (INode tup : rel.getChildren()) {
            // // // System.out.println("binding:\n" + tup);
            // // // }
            // // }
            // }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}

// class NestSourceViewTupleVisitor implements INodeVisitor {
//
// private static Log logger = LogFactory
// .getLog(NestSourceViewTupleVisitor.class);
// private FORule tgd;
// private INode sourceTuple;
// private int instanceId;
// private MappingTask mappingTask;
// private Map<String, INode> nodeCache;
// private Map<PathExpression, IValueGenerator> generators;
// private IDataSourceProxy nestResult;
// private INode resultingInstance;
// private GeneratePathExpression pathGenerator = new GeneratePathExpression();
//
// NestSourceViewTupleVisitor(FORule tgd, INode sourceTuple, int instanceId,
// Map<String, INode> nodeCache,
// Map<PathExpression, IValueGenerator> generators,
// IDataSourceProxy nestResult, MappingTask mappingTask) {
// this.tgd = tgd;
// this.sourceTuple = sourceTuple;
// this.instanceId = instanceId;
// this.nodeCache = nodeCache;
// this.generators = generators;
// this.nestResult = nestResult;
// this.mappingTask = mappingTask;
// }
//
// public void visitSetNode(SetNode node) {
// visitIntermediateNode(node);
// }
//
// public void visitTupleNode(TupleNode node) {
// visitIntermediateNode(node);
// }
//
// public void visitSequenceNode(SequenceNode node) {
// visitIntermediateNode(node);
// }
//
// public void visitUnionNode(UnionNode node) {
// visitIntermediateNode(node);
// }
//
// private void visitIntermediateNode(INode node) {
// if (logger.isDebugEnabled())
// logger.debug("Visiting intermediate node: " + node.getLabel());
// Object value = generateValue(node);
// if (value instanceof INullValue) {
// if (logger.isDebugEnabled())
// logger.debug("Node has null generator, returning...");
// return;
// }
// String key = generateKey(node);
// if (logger.isTraceEnabled())
// logger.trace("Node key: " + key);
// INode nodeInstance = nodeCache.get(key);
// if (nodeInstance == null) {
// OID oid = (OID) value;
// String instanceNodeLabel = generateInstanceNodeLabel(node);
// nodeInstance = SpicyEngineUtility.createNode(node.getClass()
// .getSimpleName(), instanceNodeLabel, oid);
// nodeCache.put(key, nodeInstance);
// if (logger.isTraceEnabled())
// logger.trace("Node not found in cache. Creating node");
// if (logger.isTraceEnabled())
// logger.trace("OID: " + oid);
// if (logger.isDebugEnabled())
// logger.debug("Created new intermediate node: " + nodeInstance);
// if (node.isRoot()) {
// if (logger.isDebugEnabled())
// logger.debug("Node is root");
// nodeInstance.setRoot(true);
// nodeInstance.addAnnotation(
// SpicyEngineConstants.SOURCE_INSTANCE_POSITION,
// instanceId);
// nestResult.addInstance(nodeInstance);
// } else {
// INode fatherInstance = findFatherInstance(node);
// fatherInstance.addChild(nodeInstance);
// if (logger.isDebugEnabled())
// logger.debug("Father instance: "
// + fatherInstance.getLabel());
// }
// }
// if (nodeInstance instanceof TupleNode) {
// addProvenance((TupleNode) nodeInstance);
// }
// if (node.isRoot()) {
// resultingInstance = nodeInstance;
// }
// visitChildren(node);
// }
//
// private void addProvenance(TupleNode tupleNode) {
// tupleNode.addProvenance(tgd.getId());
// }
//
// private void visitChildren(final INode node) {
// for (INode child : node.getChildren()) {
// child.accept(this);
// }
// }
//
// public void visitAttributeNode(AttributeNode node) {
// String key = generateKey(node);
// if (logger.isDebugEnabled())
// logger.debug("Visiting attribute: " + node.getLabel());
// if (logger.isTraceEnabled())
// logger.trace("Node key: " + key);
// INode nodeInstance = nodeCache.get(key);
// if (nodeInstance == null) {
// OID oid = (OID) generateValue(node);
// if (logger.isTraceEnabled())
// logger.trace("Node not found in cache. Creating node");
// if (logger.isTraceEnabled())
// logger.trace("Node oid: " + oid);
// nodeInstance = SpicyEngineUtility.createNode(node.getClass()
// .getSimpleName(), node.getLabel(), oid);
// nodeCache.put(key, nodeInstance);
// INode fatherInstance = findFatherInstance(node);
// fatherInstance.addChild(nodeInstance);
// INode leafNode = node.getChild(0);
// // IValueGenerator leafGenerator = getGenerator(leafNode);
// Object value = generateValue(leafNode);
// INode leafNodeInstance = SpicyEngineUtility.createNode(leafNode
// .getClass().getSimpleName(), leafNode.getLabel(), value);
// nodeInstance.addChild(leafNodeInstance);
// if (logger.isDebugEnabled())
// logger.debug("Created new attribute node: " + nodeInstance);
// if (logger.isDebugEnabled())
// logger.debug("Father instance: " + fatherInstance.getLabel());
// if (logger.isDebugEnabled())
// logger.debug("Current instance: " + this.resultingInstance);
// }
// }
//
// public void visitMetadataNode(MetadataNode node) {
// visitAttributeNode(node);
// }
//
// public void visitLeafNode(LeafNode node) {
// return;
// }
//
// private String generateInstanceNodeLabel(INode schemaNode) {
// String label = schemaNode.getLabel();
// // codice introdotto per i cloni
// // if (schemaNode instanceof SetNode) {
// // if (((SetNode)schemaNode).getClones().size() != 0) {
// // label = DuplicateSet.getCloneLabel(0, (SetNode)schemaNode);
// // }
// // }
// return label;
// }
//
// private Object generateValue(INode node) {
// IValueGenerator nodeGenerator = getGenerator(node);
// return nodeGenerator
// .generateValue((TupleNode) sourceTuple, mappingTask);
// }
//
// private IValueGenerator getGenerator(INode node) {
// PathExpression nodePath = pathGenerator.generatePathFromRoot(node);
// return generators.get(nodePath);
// }
//
// private String generateKey(INode node) {
// if (node.getFather() == null) {
// return instanceId + SpicyEngineConstants.ALGEBRA_SEPARATOR
// + generateValue(node);
// }
// return generateKey(node.getFather())
// + SpicyEngineConstants.ALGEBRA_SEPARATOR + generateValue(node);
// }
//
// private INode findFatherInstance(INode node) {
// INode fatherNode = node.getFather();
// if (fatherNode == null) {
// return null;
// }
// String fatherKey = generateKey(fatherNode);
// INode fatherInstance = nodeCache.get(fatherKey);
// return fatherInstance;
// }
//
// public Object getResult() {
// return null;
// }
// }
