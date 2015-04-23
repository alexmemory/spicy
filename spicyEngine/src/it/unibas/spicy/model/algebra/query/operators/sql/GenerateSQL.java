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
package it.unibas.spicy.model.algebra.query.operators.sql;

import it.unibas.spicy.model.mapping.MappingTask;
import it.unibas.spicy.model.datasource.INode;
import it.unibas.spicy.model.datasource.nodes.SetCloneNode;
import it.unibas.spicy.model.datasource.nodes.SetNode;
import it.unibas.spicy.model.datasource.operators.FindNode;
import it.unibas.spicy.model.exceptions.IllegalMappingTaskException;
import it.unibas.spicy.model.expressions.Expression;
import it.unibas.spicy.model.mapping.ComplexConjunctiveQuery;
import it.unibas.spicy.model.mapping.ComplexQueryWithNegations;
import it.unibas.spicy.model.mapping.EngineConfiguration;
import it.unibas.spicy.model.mapping.FORule;
import it.unibas.spicy.model.mapping.NegatedComplexQuery;
import it.unibas.spicy.model.mapping.SimpleConjunctiveQuery;
import it.unibas.spicy.model.mapping.operators.GenerateSetVariables;
import it.unibas.spicy.model.mapping.proxies.ChainingDataSourceProxy;
import it.unibas.spicy.model.paths.PathExpression;
import it.unibas.spicy.model.paths.SetAlias;
import it.unibas.spicy.model.paths.VariableCorrespondence;
import it.unibas.spicy.model.paths.VariableJoinCondition;
import it.unibas.spicy.model.paths.VariablePathExpression;
import it.unibas.spicy.model.paths.VariableSelectionCondition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class GenerateSQL {

    //////////////////////////////////////////////
    private static Log logger = LogFactory.getLog(GenerateSQL.class);
    public final static String SOURCE_SCHEMA_NAME = "source";
    public final static String TARGET_SCHEMA_NAME = "target";
    public final static String WORK_SCHEMA_NAME = "work";
    public final static String INTERMEDIATE_SCHEMA_NAME = "intermediate";
    final static String CREATE_VIEW = "create or replace view ";
    final static String CREATE_TABLE = "create table ";
    final static String SKOLEM_TABLE_NAME = WORK_SCHEMA_NAME + ".skolemTable";
    final static String SKOLEM_TABLE_COLUMN_ID = "id";
    final static String SKOLEM_TABLE_COLUMN_SKOLEM = "skolem";
    final static String SKOLEM_VIEW_NAME = WORK_SCHEMA_NAME + ".skolemString";
    final static String NULL_VALUE = "null";
    final static String RELATION_ALIAS_PREFIX = "rel_";
    final static String PRE_EXPANSION_PREFIX = "pre_";
    final static String POSITIVE_QUERY = "pos_";
    final static String INDENT = "      ";
    final static String DOUBLE_INDENT = "            ";
    public final static int SKOLEM_ID_MIN_VALUE = 1000;
    public final static int CHAINING_FIRST_STEP = 0;
    public final static int CHAINING_LAST_STEP = 1;
    public final static int CHAINING_NO_CHAINING = 2;
    private static List<String> allViewsToDelete = new ArrayList<String>();
    private static List<String> allTablesToDelete = new ArrayList<String>();
    public static Map<String, String> materializedViews = new HashMap<String, String>();

    public String generateSQL(MappingTask mappingTask) {
        if (mappingTask.getSourceProxy().getMappingData().isNested()
                || mappingTask.getTargetProxy().getMappingData().isNested()) {
            throw new IllegalMappingTaskException("Data Sources are nested. SQL can be generated for relational sources only");
        }
        StringBuilder result = new StringBuilder();
        result.append("-- This script was automatically generated by the ++Spicy mapping tool. (http://db.unibas.it/projects/spicy/)\n\n");
        result.append("BEGIN TRANSACTION;\n");
        result.append("SET CONSTRAINTS ALL DEFERRED;\n");
        if (mappingTask.getConfig().useSortInSkolems()) {
            result.append(mappingTask.getDBMSHandler().generateSortArrayFunction());
        }

        // Generate the SQL script for the first step in chaining scenarios

        if (mappingTask.getSourceProxy() instanceof ChainingDataSourceProxy) {
            ChainingDataSourceProxy proxy = (ChainingDataSourceProxy) mappingTask.getSourceProxy();
            GenerateSQLForSourceToTargetExchange stGeneratorFirstStep = new GenerateSQLForSourceToTargetExchange();
            result.append(stGeneratorFirstStep.generateSQL(proxy.getMappingTask(), CHAINING_FIRST_STEP));
        }

        result.append(this.getDeleteTablesScript(mappingTask));
        GenerateSQLForSourceToTargetExchange stGenerator = new GenerateSQLForSourceToTargetExchange();

        if (!mappingTask.getConfig().useHashTextForSkolems() && mappingTask.getConfig().useSkolemTable()) {
            result.append(generateSQLForSkolemTable(mappingTask));
        }
        if (mappingTask.getSourceProxy() instanceof ChainingDataSourceProxy) {
            result.append(stGenerator.generateSQL(mappingTask, CHAINING_LAST_STEP));
        } else {
            result.append(stGenerator.generateSQL(mappingTask, CHAINING_NO_CHAINING));
        }
        allTablesToDelete.addAll(stGenerator.getAllTablesToDelete());
        if (mappingTask.getConfig().useCreateTableInSTExchange()) {
            allTablesToDelete.addAll(stGenerator.getAllViewsToDelete());
        } else {
            allViewsToDelete.addAll(stGenerator.getAllViewsToDelete());
        }
        result.append(generateFinalInserts(mappingTask));
        result.append("COMMIT;\n");
        if (!mappingTask.getConfig().useDebugMode()) {
            result.append(this.getAllViewsToDeleteScript());
            result.append(this.getAllTablesToDeleteScript());
        }
        return result.toString();
    }

    public static String getSourceSchemaName(int chainingStep) {
        if (chainingStep == CHAINING_FIRST_STEP || chainingStep == CHAINING_NO_CHAINING) {
            return SOURCE_SCHEMA_NAME;
        }
        if (chainingStep == CHAINING_LAST_STEP) {
            return INTERMEDIATE_SCHEMA_NAME;
        }
        throw new IllegalArgumentException("Wrong value for chainingStep parameter: " + chainingStep);
    }

    public static String getTargetSchemaName(int chainingStep) {
        if (chainingStep == CHAINING_FIRST_STEP) {
            return INTERMEDIATE_SCHEMA_NAME;
        }
        if (chainingStep == CHAINING_LAST_STEP || chainingStep == CHAINING_NO_CHAINING) {
            return TARGET_SCHEMA_NAME;
        }
        throw new IllegalArgumentException("Wrong value for chainingStep parameter: " + chainingStep);
    }

    private List<FORule> findRelevantTGDs(SetAlias targetVariable, List<FORule> tgds) {
        List<FORule> result = new ArrayList<FORule>();
        for (FORule tgd : tgds) {
            if (containsUpToClone(tgd.getTargetView().getGenerators(), targetVariable)) {
                result.add(tgd);
            }
        }
        return result;
    }
    private boolean containsUpToClone(List<SetAlias> variables, SetAlias targetVariable) {
        for (SetAlias variable : variables) {
            if (variable.equalsOrIsClone(targetVariable)) {
                return true;
            }
        }
        return false;
    }
    private String generateFinalInserts(MappingTask mappingTask) {
        StringBuilder result = new StringBuilder();
        if (mappingTask.getConfig().useSkolemTable()) {
            result.append("\n------------------------------  FINAL INSERTS  -----------------------------------\n\n");
            result.append("\n------------------------------  INSERT INTO TARGET DATABASE  ---------------------------\n");
            result.append(generateFinalInsert(mappingTask));
        }
        return result.toString();
    }
    private static String generateSQLForSkolemTable(MappingTask mappingTask) {
        StringBuilder result = new StringBuilder();
        result.append("\n--------------------------  SKOLEM TABLE  -------------------------------\n\n");
        result.append(CREATE_TABLE).append(SKOLEM_TABLE_NAME).append("(\n");
        result.append(INDENT).append(SKOLEM_TABLE_COLUMN_ID).append(" ").append(mappingTask.getDBMSHandler().getAutoGeneratedColumnType()).append(",\n");
        result.append(INDENT).append(SKOLEM_TABLE_COLUMN_SKOLEM);
        result.append(GenerateSQL.getSkolemColumnType(mappingTask.getConfig()));
        allTablesToDelete.add(SKOLEM_TABLE_NAME);
        result.append(");\n");
        return result.toString();
    }

    public static String generateInsertInSkolemTable() {
        StringBuilder result = new StringBuilder();
        result.append("insert into ").append(SKOLEM_TABLE_NAME).append("(").append(SKOLEM_TABLE_COLUMN_SKOLEM).append(")\n");
        result.append(INDENT).append("select *\n");
        result.append(INDENT).append("from ").append(SKOLEM_VIEW_NAME);
        result.append(";\n\n");
        return result.toString();
    }

    private String generateFinalInsert(MappingTask mappingTask) {
        StringBuilder result = new StringBuilder();
        for (SetAlias targetVariable : mappingTask.getTargetProxy().getMappingData().getVariables()) {
            List<FORule> relevantTgds = findRelevantTGDs(targetVariable, mappingTask.getMappingData().getSTTgds());
            if (!relevantTgds.isEmpty()) {
                result.append(generateInsertForRelationAfterExchange(targetVariable, mappingTask));
            }
        }
        return result.toString();
    }
///////////////////////////  SQL INSERTS  //////////////////////////////////
    private String generateInsertForRelationAfterExchange(SetAlias targetVariable, MappingTask mappingTask) {
        StringBuilder result = new StringBuilder();
        String fromValue = finalSqlNameAfterExchange(targetVariable);
        if (allTablesToDelete.contains(fromValue) || allViewsToDelete.contains(fromValue)) {
            result.append("insert into ").append(targetRelationName(targetVariable, mappingTask, CHAINING_NO_CHAINING)).append("\n");
            result.append(INDENT).append("select distinct *\n");
            result.append(INDENT).append("from ").append(fromValue);
            result.append(";\n\n");
        }
        return result.toString();
    }
//////////////////////  SQL VIEW NAMES
    private static String sqlNameForTgdWithNoSchemaName(FORule tgd) {
        return replaceSpecialCharacters(tgd.getId().toLowerCase());
    }

    public static String tgdFinalSQLName(FORule tgd) {
        return WORK_SCHEMA_NAME + "." + "TARGET_VALUES_" + sqlNameForTgdWithNoSchemaName(tgd);
    }

    public static String finalSqlNameAfterExchange(SetAlias variable) {
        return WORK_SCHEMA_NAME + "." + "EXCHANGE_RESULT_FOR_" + variable.getBindingPathExpression().getLastStep();
    }

    public static String attributeNameInVariable(VariablePathExpression attributePath) {
        return attributePath.getStartingVariable().toShortString() + attributePath.getLastStep();
    }

    public static String attributeNameWithVariable(VariablePathExpression attributePath) {
        return RELATION_ALIAS_PREFIX + attributePath.getStartingVariable().toShortString() + "." + attributePath.getLastStep();
    }

    private static String sourceRelationName(SetAlias variable, int chainingStep) {
        return getSourceSchemaName(chainingStep) + "." + variable.getBindingPathExpression().getLastStep();
    }

    public static String targetRelationName(SetAlias variable, MappingTask mappingTask, int chainingStep) {
        SetNode bindingNode = variable.getBindingNode(mappingTask.getTargetProxy().getIntermediateSchema());
        String result = variable.getBindingPathExpression().getLastStep();
        if (bindingNode instanceof SetCloneNode) {
            result = ((SetCloneNode) bindingNode).getOriginalNodePath().getLastStep();
        }
        return getTargetSchemaName(chainingStep) + "." + result;
    }

    private static String sqlString(Expression expression) {
        String result = "";
        List<VariablePathExpression> attributePaths = expression.getAttributePaths();
        if (expression.toString().startsWith("isNull(")) {
            String attributeName = extractAttributeNameFromExpression(attributePaths.get(0));
            return attributeName + " IS NULL";
        }
        if (expression.toString().startsWith("isNotNull(")) {
            String attributeName = extractAttributeNameFromExpression(attributePaths.get(0));
            return attributeName + " IS NOT NULL";
        }
        for (VariablePathExpression attributePath : attributePaths) {
            String attributeName = extractAttributeNameFromExpression(attributePaths.get(0));
            result = expression.toString().replaceAll(attributePath.toString(), attributeName);
        }
        result = result.replaceAll("==", "=");
        result = result.replaceAll("\"", "\'");
        return result.replaceAll("&&", "AND");
    }

    private static String extractAttributeNameFromExpression(VariablePathExpression attributePath) {
        String variableName = attributePath.getStartingVariable().toShortString();
        String lastStep = attributePath.getLastStep();
        return GenerateSQL.RELATION_ALIAS_PREFIX + variableName + "." + lastStep;
    }

    private static String replaceSpecialCharacters(String value) {
        return value.replaceAll("#", "_");
    }

    public static String sqlNameForViewWithIntersection(ComplexQueryWithNegations query) {
        return WORK_SCHEMA_NAME + "." + query.getId();
    }

    public static String sqlNameForPositiveView(ComplexQueryWithNegations query) {
        return WORK_SCHEMA_NAME + "." + POSITIVE_QUERY + query.getId();
    }

    public static String sqlNameForRule(FORule tgd) {
        return WORK_SCHEMA_NAME + "." + sqlNameForTgdWithNoSchemaName(tgd);
    }

    public static String generateCasting(VariablePathExpression attributePath, String columnName, MappingTask mappingTask) {
        INode attributeNode = new FindNode().findNodeInSchema(attributePath.getAbsolutePath(), mappingTask.getTargetProxy());
        String value = attributeNode.getChild(0).getLabel();
        String castedColumnName = mappingTask.getDBMSHandler().forceCast(columnName, value);
        return castedColumnName;
    }

    public static String renameSkolemTable(int skolemTableCounter) {
        return "sk_" + skolemTableCounter;
    }

    private String getAllViewsToDeleteScript() {
        StringBuilder result = new StringBuilder();
        for (int i = allViewsToDelete.size() - 1; i >= 0; i--) {
            result.append("drop view ").append(allViewsToDelete.get(i)).append(" cascade;\n");
        }
        return result.toString();
    }

    private String getAllTablesToDeleteScript() {
        StringBuilder result = new StringBuilder();
        for (int i = allTablesToDelete.size() - 1; i >= 0; i--) {
            result.append("drop table ").append(allTablesToDelete.get(i)).append(" cascade;\n");
        }
        return result.toString();
    }

    public String getDeleteTablesScript(MappingTask mappingTask) {
        StringBuilder result = new StringBuilder();
        List<PathExpression> setPaths = new GenerateSetVariables().findSetAbsolutePaths(mappingTask.getTargetProxy());
        for (PathExpression pathExpression : setPaths) {
            INode node = new FindNode().findNodeInSchema(pathExpression, mappingTask.getTargetProxy());
            if (!(node instanceof SetCloneNode)) {
                result.append("delete from ").append(TARGET_SCHEMA_NAME).append(".").append(pathExpression.getLastStep()).append(";\n");
            }
        }
        return result.toString();
    }

    public static boolean hasDifferences(FORule tgd) {
        boolean negations = !tgd.getComplexSourceQuery().getNegatedComplexQueries().isEmpty();
        return negations;
    }

    private static String getSkolemColumnType(EngineConfiguration config) {
        if (config.useHashTextForSkolems()) {
            return " integer";
        }
        return " text";
    }

    public static VariablePathExpression findSourcePath(List<VariableCorrespondence> correspondences, VariablePathExpression targetPath) {
        for (VariableCorrespondence variableCorrespondence : correspondences) {
            if (variableCorrespondence.getTargetPath().equalsAndHasSameVariableId(targetPath)) {
                return variableCorrespondence.getFirstSourcePath();
            }
        }
        return null;
    }

    private static VariablePathExpression findSourcePathWithEqualsId(List<VariableCorrespondence> correspondences, VariablePathExpression targetPath) {
        for (VariableCorrespondence variableCorrespondence : correspondences) {
            if (variableCorrespondence.getTargetPath().equalsAndHasSameVariableId(targetPath)) {
                return variableCorrespondence.getFirstSourcePath();
            }
        }
        return null;
    }

//////////////////////////////////////////////(
    public static String generateFromClause(List<SetAlias> variables, int chainingStep) {
        StringBuilder result = new StringBuilder();
        result.append("from ");
        List<String> addedVariables = new ArrayList<String>();
        for (int i = 0; i < variables.size(); i++) {
            SetAlias variable = variables.get(i);
            String relationName = GenerateSQL.sourceRelationName(variable, chainingStep);
            String aliasRelationName = GenerateSQL.RELATION_ALIAS_PREFIX + variable.toShortString();
            // NOTE: we are preventing to reuse the same variable twice in the from clause
            // this might not be correct in all cases
            if (!addedVariables.contains(aliasRelationName)) {
                addedVariables.add(aliasRelationName);
                result.append(relationName).append(" AS ").append(aliasRelationName);
//                if (i != variables.size() - 1) result.append(", ");
                result.append(", ");
            }
        }
        result.delete(result.length() - ", ".length(), result.length() - 1);
        result.append("\n");
        return result.toString();
    }

    public static String generateWhereClause(ComplexConjunctiveQuery view) {
        if (!isNeededAWhereClause(view)) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        result.append("where \n");
        result.append(generateWhereClauseWithoutWhere(view));
        return result.toString();
    }

    private static String generateWhereClauseForIntersection(ComplexConjunctiveQuery view) {
        StringBuilder result = new StringBuilder();
        result.append(INDENT).append("where \n");
        List<VariableSelectionCondition> allSelectionConditions = view.getAllSelections();
        List<VariableJoinCondition> joinConditions = new ArrayList<VariableJoinCondition>(view.getJoinConditions());
        for (SimpleConjunctiveQuery simpleConjunctiveQuery : view.getConjunctions()) {
            joinConditions.addAll(simpleConjunctiveQuery.getAllJoinConditions());
        }
        if (!joinConditions.isEmpty()) {
            for (int i = 0; i < joinConditions.size(); i++) {
                VariableJoinCondition joinCondition = joinConditions.get(i);
                result.append(DOUBLE_INDENT).append(generateSQLStringForJoinConditionForIntersection(joinCondition, view));
                result.append(" AND\n");
            }
        }
        // check selection conditions
        for (int i = 0; i < allSelectionConditions.size(); i++) {
            VariableSelectionCondition condition = allSelectionConditions.get(i);
            result.append(DOUBLE_INDENT).append(GenerateSQL.sqlString(condition.getCondition()));
            if (i != allSelectionConditions.size() - 1 || view.hasIntersection()) result.append(" AND\n");
        }
        if (view.hasIntersection()) {
            List<VariablePathExpression> leftIntersectionPaths = generateTargetPaths(view.getIntersectionEqualities().getLeftCorrespondences());
            List<VariablePathExpression> rightIntersectionPaths = generateTargetPaths(view.getIntersectionEqualities().getRightCorrespondences());
            List<VariableCorrespondence> allCorrespondences = new ArrayList<VariableCorrespondence>();
            allCorrespondences.addAll(view.getIntersectionEqualities().getLeftCorrespondences());
            allCorrespondences.addAll(view.getIntersectionEqualities().getRightCorrespondences());

            for (int i = 0; i < leftIntersectionPaths.size(); i++) {
                VariablePathExpression leftPath = leftIntersectionPaths.get(i);
                VariablePathExpression rightPath = rightIntersectionPaths.get(i);
                VariablePathExpression leftSourcePath = findSourcePathWithEqualsId(allCorrespondences, leftPath);
                if (leftSourcePath == null) {
                    leftSourcePath = leftPath;
                }
                VariablePathExpression rightSourcePath = findSourcePathWithEqualsId(allCorrespondences, rightPath);
                if (rightSourcePath == null) {
                    rightSourcePath = rightPath;
                }
                result.append(DOUBLE_INDENT).append(attributeNameWithVariable(leftSourcePath)).append(" = ").append(attributeNameWithVariable(rightSourcePath));
                if (i != leftIntersectionPaths.size() - 1) result.append(" AND\n");
            }
        }
        result.append("\n");
        return result.toString();
    }

    private static List<VariablePathExpression> generateTargetPaths(List<VariableCorrespondence> correspondences) {
        List<VariablePathExpression> result = new ArrayList<VariablePathExpression>();
        for (VariableCorrespondence correspondence : correspondences) {
            result.add(correspondence.getTargetPath());
        }
        return result;
    }

    public static String generateWhereClauseForNegation(ComplexConjunctiveQuery view) {
        if (!isNeededAWhereClause(view)) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        result.append("where \n");
        result.append(generateWhereClauseWithoutWhere(view));
        return result.toString();
    }

    private static boolean isNeededAWhereClause(ComplexConjunctiveQuery view) {
        if (view.getVariables().size() > 1) {
            return true;
        }
        if (view.getAllSelections().size() > 0) {
            return true;
        }
        return false;
    }

    private static String generateWhereClauseWithoutWhere(ComplexConjunctiveQuery view) {
        if (!isNeededAWhereClause(view)) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        List<VariableSelectionCondition> allSelectionConditions = view.getAllSelections();
        List<SimpleConjunctiveQuery> conjunctions = view.getConjunctions();
        List<VariableJoinCondition> joinConditions = new ArrayList<VariableJoinCondition>(view.getJoinConditions());
        for (SimpleConjunctiveQuery simpleConjunctiveQuery : conjunctions) {
            joinConditions.addAll(simpleConjunctiveQuery.getAllJoinConditions());
        }
        if (!joinConditions.isEmpty()) {
            result.append("\n");
            for (int i = 0; i < joinConditions.size(); i++) {
                VariableJoinCondition joinCondition = joinConditions.get(i);
                List<VariableCorrespondence> correspondences = new ArrayList<VariableCorrespondence>();
                for (List<VariableCorrespondence> list : view.getCorrespondencesForConjunctions()) {
                    correspondences.addAll(list);
                }
                result.append(DOUBLE_INDENT).append(generateSQLStringForJoinConditionSourcePath(joinCondition, correspondences));
                if (i != joinConditions.size() - 1 || !allSelectionConditions.isEmpty()) result.append(" AND\n");
            }
        }
        // check selection conditions
        for (int i = 0; i < allSelectionConditions.size(); i++) {
            VariableSelectionCondition condition = allSelectionConditions.get(i);
            result.append(DOUBLE_INDENT).append(GenerateSQL.sqlString(condition.getCondition()));
            if (i != allSelectionConditions.size() - 1) result.append(" AND\n");
        }
        result.append("\n");
        return result.toString();
    }

    private static String generateSQLStringForAdditionalJoinConditionSourcePath(VariableJoinCondition joinCondition, List<VariableCorrespondence> correspondences, String negationViewName) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < joinCondition.getFromPaths().size(); i++) {
            VariablePathExpression fromPath = joinCondition.getFromPaths().get(i);
            VariablePathExpression toPath = joinCondition.getToPaths().get(i);
            VariablePathExpression fromSourcePath = findSourcePath(correspondences, fromPath);
            if (fromSourcePath == null) {
                fromSourcePath = fromPath;
            }
            VariablePathExpression toSourcePath = findSourcePath(correspondences, toPath);
            if (toSourcePath == null) {
                toSourcePath = toPath;
            }
            result.append(DOUBLE_INDENT).append(negationViewName).append(".").append(attributeNameInVariable(fromSourcePath));
            result.append(" = ");
            result.append(negationViewName).append(".").append(attributeNameInVariable(toSourcePath));
            if (i != joinCondition.getFromPaths().size() - 1) result.append(" AND\n");
        }
        return result.toString();
    }

    private static String generateSQLStringForJoinConditionSourcePath(VariableJoinCondition joinCondition, List<VariableCorrespondence> correspondences) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < joinCondition.getFromPaths().size(); i++) {
            VariablePathExpression fromPath = joinCondition.getFromPaths().get(i);
            VariablePathExpression toPath = joinCondition.getToPaths().get(i);
            VariablePathExpression fromSourcePath = findSourcePath(correspondences, fromPath);
            if (fromSourcePath == null) {
                fromSourcePath = fromPath;
            }
            VariablePathExpression toSourcePath = findSourcePath(correspondences, toPath);
            if (toSourcePath == null) {
                toSourcePath = toPath;
            }
            result.append(attributeNameWithVariable(fromSourcePath)).append(" = ").append(attributeNameWithVariable(toSourcePath));
            if (i != joinCondition.getFromPaths().size() - 1) result.append(" AND ");
        }
        return result.toString();
    }

    private static String generateSQLStringForJoinConditionForIntersection(VariableJoinCondition joinCondition, ComplexConjunctiveQuery view) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < joinCondition.getFromPaths().size(); i++) {
            VariablePathExpression fromPath = joinCondition.getFromPaths().get(i);
            VariablePathExpression toPath = joinCondition.getToPaths().get(i);
            VariablePathExpression fromSourcePath = findSourcePathWithEqualsId(view.getAllCorrespondences(), fromPath);
            if (fromSourcePath == null) {
                fromSourcePath = fromPath;
            }
            VariablePathExpression toSourcePath = findSourcePathWithEqualsId(view.getAllCorrespondences(), toPath);
            if (toSourcePath == null) {
                toSourcePath = toPath;
            }
            result.append(attributeNameWithVariable(fromSourcePath)).append(" = ").append(attributeNameWithVariable(toSourcePath));
            if (i != joinCondition.getFromPaths().size() - 1) result.append(" AND ");
        }
        return result.toString();
    }

    public static String generateProjectionWithIntersection(ComplexConjunctiveQuery query, MappingTask mappingTask, int chainingStep) {
        StringBuilder result = new StringBuilder();
        List<SetAlias> sourceVariables = new ArrayList<SetAlias>();
        sourceVariables.addAll(query.getVariables());
        sourceVariables.addAll(query.getConjunctionForIntersection().getVariables());
        String fromClause = GenerateSQL.generateFromClause(sourceVariables, chainingStep);
        String whereClause = GenerateSQL.generateWhereClauseForIntersection(query);
        result.append(INDENT).append("select distinct \n");
//        List<SetAlias> variables = extractDifferentVariables(query.getVariables());
        List<SetAlias> variables = query.getVariables();
        result.append(generateProjectionOnAllAttributes(variables, mappingTask));
        result.append(INDENT).append(fromClause);
        if (!whereClause.equals("")) {
            result.append(INDENT).append(whereClause);
        }
        return result.toString();
    }

    public static String generateProjectionWithoutIntersection(ComplexConjunctiveQuery query, MappingTask mappingTask, int chainingStep) {
        StringBuilder result = new StringBuilder();
        List<SetAlias> sourceVariables = query.getVariables();
        String fromClause = GenerateSQL.generateFromClause(sourceVariables, chainingStep);
        String whereClause = GenerateSQL.generateWhereClause(query);
        result.append(INDENT).append("select distinct \n");
//        List<SetAlias> variables = extractDifferentVariables(query.getVariables());
        List<SetAlias> variables = query.getVariables();
        result.append(generateProjectionOnAllAttributes(variables, mappingTask));
        result.append(INDENT).append(fromClause);
        if (!whereClause.equals("")) {
            result.append(INDENT).append(whereClause);
        }
        return result.toString();
    }

//    private static List<SetAlias> extractDifferentVariables(List<SetAlias> variables) {
//        List<SetAlias> result = new ArrayList<SetAlias>();
//        for (SetAlias variable : variables) {
//            if (!containsVariableWithSameName(variable, result)) {
//                result.add(variable);
//            }
//        }
//        return result;
//    }
//    private static boolean containsVariableWithSameName(SetAlias variable, List<SetAlias> variables) {
//        for (SetAlias setAlias : variables) {
//            if (setAlias.hasSameId(variable)) {
//                return true;
//            }
//        }
//        return false;
//    }
    private static String generateProjectionOnAllAttributes(List<SetAlias> variables, MappingTask mappingTask) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < variables.size(); i++) {
            SetAlias variable = variables.get(i);
            List<VariablePathExpression> attributes = variable.getAttributes(mappingTask.getSourceProxy().getIntermediateSchema());
            for (int j = 0; j < attributes.size(); j++) {
                VariablePathExpression attribute = attributes.get(j);
                result.append(DOUBLE_INDENT).append(GenerateSQL.attributeNameWithVariable(attribute));
                result.append(" AS ").append(attributeNameInVariable(attribute));
                if (j != attributes.size() - 1) result.append(",\n");
            }
            if (i != variables.size() - 1) result.append(",\n");
        }
        result.append("\n");
        return result.toString();
    }

    public static String generateProjectionForTgd(ComplexQueryWithNegations query, List<VariableCorrespondence> correspondences, int chainingStep) {
        String fromClause = GenerateSQL.generateFromClause(query.getVariables(), chainingStep);
        String whereClause = GenerateSQL.generateWhereClause(query.getComplexQuery());
        return projectionOnTargetValuesFromCorrespondences(correspondences, fromClause, whereClause);
    }

    public static String generateProjectionFromPositiveQuery(ComplexQueryWithNegations query) {
        StringBuilder result = new StringBuilder();
        result.append("select distinct *\n");
        result.append("from ").append(sqlNameForPositiveView(query)).append("\n");
        return result.toString();
    }

    public static String generateProjectionFromFinalRuleNegation(String positiveViewName, NegatedComplexQuery negation) {
        StringBuilder result = new StringBuilder();
        result.append(INDENT).append("select distinct ").append(positiveViewName).append(".*\n");
        String negationViewName = findViewName(negation);
        result.append(INDENT).append("from ").append(positiveViewName).append(", ").append(negationViewName).append("\n");
        result.append(INDENT).append(generateWhereClauseFromEqualities(negation, positiveViewName, negationViewName));
        result.append(INDENT).append(generateWhereContentForAdditionalJoins(negation, negation.getCorrespondencesForJoin(), positiveViewName, negationViewName));
        return result.toString();
    }

    public static String generateProjectionFromNegation(String positiveViewName, NegatedComplexQuery negation) {
        StringBuilder result = new StringBuilder();
        result.append(INDENT).append("select distinct ").append(positiveViewName).append(".*\n");
        String negationViewName = findViewName(negation);
        result.append(INDENT).append("from ").append(positiveViewName).append(", ").append(negationViewName).append("\n");
        result.append(INDENT).append(generateWhereClauseFromEqualities(negation, positiveViewName, negationViewName));
        result.append(INDENT).append(generateWhereContentForAdditionalJoins(negation, negation.getCorrespondencesForJoin(), positiveViewName, negationViewName));
        return result.toString();
    }

    private static String generateWhereClauseFromEqualities(NegatedComplexQuery negation, String positiveViewName, String negationViewName) {
        StringBuilder result = new StringBuilder();
        result.append("where\n");
        if (negation.isTargetDifference()) {
            for (int i = 0; i < negation.getTargetEqualities().getLeftCorrespondences().size(); i++) {
                VariableCorrespondence leftCorrespondence = negation.getTargetEqualities().getLeftCorrespondences().get(i);
                VariableCorrespondence rightCorrespondence = negation.getTargetEqualities().getRightCorrespondences().get(i);
                VariablePathExpression leftPath = leftCorrespondence.getFirstSourcePath();
                VariablePathExpression rightPath = rightCorrespondence.getFirstSourcePath();
                result.append(DOUBLE_INDENT).append(positiveViewName).append(".").append(attributeNameInVariable(leftPath));
                result.append(" = ");
                result.append(negationViewName).append(".").append(attributeNameInVariable(rightPath));
                if (i != negation.getTargetEqualities().getLeftCorrespondences().size() - 1) result.append(" AND\n");
            }
        } else {
            for (int i = 0; i < negation.getSourceEqualities().getLeftPaths().size(); i++) {
                VariablePathExpression leftPath = negation.getSourceEqualities().getLeftPaths().get(i);
                VariablePathExpression rightPath = negation.getSourceEqualities().getRightPaths().get(i);
                result.append(DOUBLE_INDENT).append(positiveViewName).append(".").append(attributeNameInVariable(leftPath));
                result.append(" = ");
                result.append(negationViewName).append(".").append(attributeNameInVariable(rightPath));
                if (i != negation.getSourceEqualities().getLeftPaths().size() - 1) result.append(" AND\n");
            }
        }
        result.append("\n");
        return result.toString();
    }

    private static String generateWhereContentForAdditionalJoins(NegatedComplexQuery negation, List<VariableCorrespondence> correspondences, String positiveViewName, String negationViewName) {
        StringBuilder result = new StringBuilder();
        if (negation.getAdditionalCyclicJoins().isEmpty()) {
            return "";
        }
        result.append(" AND\n");
        for (int i = 0; i < negation.getAdditionalCyclicJoins().size(); i++) {
            VariableJoinCondition joinCondition = negation.getAdditionalCyclicJoins().get(i);
            result.append(generateSQLStringForAdditionalJoinConditionSourcePath(joinCondition, correspondences, negationViewName));
            if (i != negation.getAdditionalCyclicJoins().size() - 1) result.append(" AND\n");
        }
        result.append("\n");
        return result.toString();
    }

    public static String generateProjectionFromNegationOnTargetRenaming(ComplexQueryWithNegations positiveQuery, NegatedComplexQuery negation) {
        StringBuilder result = new StringBuilder();
        String positiveViewName = sqlNameForPositiveView(positiveQuery);
        result.append(INDENT).append("select distinct ").append(positiveViewName).append(".*\n");
        String negationViewName = findViewName(negation);
        result.append(INDENT).append("from ").append(positiveViewName).append(", ").append(negationViewName).append("\n");
        result.append(INDENT).append(generateWhereClauseFromEqualities(negation, positiveViewName, negationViewName));
        return result.toString();
    }

    private static String findViewName(NegatedComplexQuery negation) {
        String fromViewName;
        ComplexQueryWithNegations negatedQuery = negation.getComplexQuery();
        if (materializedViews.containsKey(negatedQuery.getId())) {
            return materializedViews.get(negatedQuery.getId());
        }
//        if (materializedViews.containsKey(negatedQuery.toString())) {
//            return materializedViews.get(negatedQuery.toString());
//        }
        if (negatedQuery.getNegatedComplexQueries().isEmpty()) {// || isRewiQueryWithOnlySubsumeNegations(negatedQuery)) {
            fromViewName = sqlNameForPositiveView(negation.getComplexQuery());
        } else {
            fromViewName = sqlNameForViewWithIntersection(negation.getComplexQuery());
        }
        return fromViewName;
    }

    public static String projectionOnTargetValuesFromCorrespondences(List<VariableCorrespondence> correspondences, String fromClause, String whereClause) {
        StringBuilder result = new StringBuilder();
        result.append(INDENT).append("select distinct \n");
        for (int i = 0; i < correspondences.size(); i++) {
            VariableCorrespondence correspondence = correspondences.get(i);

            if (correspondence.getSourcePaths() != null) {
                result.append(DOUBLE_INDENT).append(GenerateSQL.attributeNameWithVariable(correspondence.getFirstSourcePath()));
            } else {
                String sourcePathName = correspondence.getSourceValue().toString();
                sourcePathName = sourcePathName.replaceAll("\"", "\'");
                result.append(DOUBLE_INDENT).append(sourcePathName);
            }

            result.append(" as ").append(GenerateSQL.attributeNameInVariable(correspondence.getFirstSourcePath()));
            if (i != correspondences.size() - 1) result.append(",\n");
        }
        result.append("\n");
        result.append(INDENT).append(fromClause);
        if (!whereClause.equals("")) {
            result.append(INDENT).append(whereClause);
        }
        return result.toString();
    }
}
