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
 
package it.unibas.spicy.test.persistence;

import it.unibas.spicy.persistence.DAOComaCorrespondences;
import it.unibas.spicy.test.TestFixture;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestDAOComaCorrespondences extends TestFixture {
    
    private Log logger = LogFactory.getLog(this.getClass());
    private String DIRECTORY_PATH = "/testdata/coma/";

//    public void testAmalgamAmalgam() {
//        String filename = "amalgam-amalgam-comaLog.txt";
//        try{
//            logger.debug(" File: " + filename);
//            printStrings(DAOComaCorrespondences.loadComaCorrespondences(DIRECTORY_PATH + filename));
//        } catch(Exception ex) {
//            logger.warn(ex);
//            fail();
//        }
//    }
//    
//    public void testDblpAmalgam() {
//        String filename = "dblp-amalgam-comaLog.txt";
//        try{
//            logger.debug(" File: " + filename);
//            printStrings(DAOComaCorrespondences.loadComaCorrespondences(DIRECTORY_PATH + filename));
//        } catch(Exception ex) {
//            logger.warn(ex);
//            fail();
//        }
//    }
//
//    public void testInformaticaLbdb() {
//        String filename = "informatica-lb_db-comaLog.txt";
//        try{
//            logger.debug(" File: " + filename);
//            printStrings(DAOComaCorrespondences.loadComaCorrespondences(DIRECTORY_PATH + filename));
//        } catch(Exception ex) {
//            logger.warn(ex);
//            fail();
//        }
//    }

//    public void testLbdbInformatica() {
//        String filename = "informatica-lb_db-comaLog.txt";
//        try{
//            logger.debug(" File: " + filename);
//            printStrings(DAOComaCorrespondences.loadComaCorrespondences(DIRECTORY_PATH + filename));
//        } catch(Exception ex) {
//            logger.warn(ex);
//            fail();
//        }
//    }

//    public void testMondialCia() {
//        String filename = "mondial-cia-comaLog.txt";
//        try{
//            logger.debug(" File: " + filename);
//            printStrings(DAOComaCorrespondences.loadComaCorrespondences(DIRECTORY_PATH + filename));
//        } catch(Exception ex) {
//            logger.warn(ex);
//            fail();
//        }
//    }
//    
//    public void testMondialGs() {
//        String filename = "mondial-gs-comaLog.txt";
//        try{
//            logger.debug(" File: " + filename);
//            printStrings(DAOComaCorrespondences.loadComaCorrespondences(DIRECTORY_PATH + filename));
//        } catch(Exception ex) {
//            logger.warn(ex);
//            fail();
//        }
//    }
//
//    public void testMondialOrganization() {
//        String filename = "mondial-organization_Set-comaLog.txt";
//        try{
//            logger.debug(" File: " + filename);
//            printStrings(DAOComaCorrespondences.loadComaCorrespondences(DIRECTORY_PATH + filename));
//        } catch(Exception ex) {
//            logger.warn(ex);
//            fail();
//        }
//    }
//
//    public void testMoodleLbUser() {
//        String filename = "moodle-lb_user-comaLog.txt";
//        try{
//            logger.debug(" File: " + filename);
//            printStrings(DAOComaCorrespondences.loadComaCorrespondences(DIRECTORY_PATH + filename));
//        } catch(Exception ex) {
//            logger.warn(ex);
//            fail();
//        }
//    }
//
//    public void testPeoplePeople() {
//        String filename = "people-people-comaLog.txt";
//        try{
//            logger.debug(" File: " + filename);
//            printStrings(DAOComaCorrespondences.loadComaCorrespondences(DIRECTORY_PATH + filename));
//        } catch(Exception ex) {
//            logger.warn(ex);
//            fail();
//        }
//    }
//
//    public void testStatDBExpenseDB() {
//        String filename = "statDB-expenseDB-comaLog.txt";
//        try{
//            logger.debug(" File: " + filename);
//            printStrings(DAOComaCorrespondences.loadComaCorrespondences(DIRECTORY_PATH + filename));
//        } catch(Exception ex) {
//            logger.warn(ex);
//            fail();
//        }
//    }
//
//    public void testUniversityDBPersonnelSet() {
//        String filename = "universityDB-personnelSet-comaLog.txt";
//        try{
//            logger.debug(" File: " + filename);
//            printStrings(DAOComaCorrespondences.loadComaCorrespondences(DIRECTORY_PATH + filename));
//        } catch(Exception ex) {
//            logger.warn(ex);
//            fail();
//        }
//    }

    private void printStrings(List<String[]> list){
        String str = "\n\n ";
        for(String[] vector : list){
            str += vector[0] + "\n";
            str += vector[1] + "\n";
            str += vector[2] + "\n---------------------- \n";
        }
        logger.debug(str);
    }
}
