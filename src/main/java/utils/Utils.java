/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.diachron.detection.change_detection_utils.ChangesManager;
import org.diachron.detection.repositories.JDBCVirtuosoRep;

/**
 *
 * @author lenovo
 */
public class Utils {

    public static List<String> getChangesOntologies(String datasetUri, JDBCVirtuosoRep jdbc) {
        if (datasetUri.endsWith("/")) {
            datasetUri = datasetUri.substring(0, datasetUri.length() - 1);
        }
        String datasetChanges = datasetUri + "/changes";
        String query = "select ?ontol from <http://datasets> where {\n"
                + "<" + datasetChanges + "> rdfs:member ?ontol.\n"
                + "?ontol co:old_version ?v1.\n"
                + "}  order by ?v1";
        List<String> ontologies = new ArrayList<>();
        try {
            ResultSet results = jdbc.executeSparqlQuery(query, false);
            if (results.next()) {
                do {
                    ontologies.add(results.getString(1));
                } while (results.next());
            }
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage() + " occured .");
        }
        return ontologies;
    }

    public static void main(String[] args) throws Exception {
        Properties prop = new Properties();
        prop.load(new FileInputStream("C:/config.properties"));
        String datasetUri = prop.getProperty("Dataset_Uri");
        String v1 = "http://www.diachron-fp7.eu/resource/recordset/efo/2.34";
        String v2 = "http://www.diachron-fp7.eu/resource/recordset/efo/2.35";
        ChangesManager cManager = new ChangesManager(prop, datasetUri, v1, v2, false);
        String changesOntology = cManager.getChangesOntology();
        System.out.println(changesOntology);
        cManager.terminate();
    }

}
