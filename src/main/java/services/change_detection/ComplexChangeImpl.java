/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package services.change_detection;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.ws.rs.PathParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.diachron.detection.change_detection_utils.JSONMessagesParser;
import org.diachron.detection.complex_change.CCDefinitionError.CODE;
import org.diachron.detection.complex_change.CCManager;
import org.diachron.detection.repositories.JDBCVirtuosoRep;
import utils.PropertiesManager;
import utils.Utils;

/**
 * REST Web Service
 *
 * @author rousakis
 */
@Path("complex_change")
public class ComplexChangeImpl {

    PropertiesManager propertiesManager = PropertiesManager.getPropertiesManager();

    /**
     * Creates a new instance of ComplexChangeImpl
     */
    public ComplexChangeImpl() {
    }

    /**
     * <b>GET</b> method which returns high level information about the
     * definition of a complex changes. Such information can be the name of the
     * complex change, the simple changes which compose it, its parameters along
     * with their corresponding filters etc.<br>
     * <b>URL:</b> /diachron/complex_change/{com_change}
     *
     * @param <b>com_change</b> Path parameter which refers on the complex
     * change name.
     * @return A Response instance which has a JSON-encoded entity content. We
     * discriminate the following cases: <br>
     * <ul>
     * <li> Error code: <b>400</b> and entity content: { "Result" : false,
     * "Message" : "...."} if there occurs an exception during the communication
     * with Virtuoso Triple Store.
     * <li> Error code: <b>204</b> and entity content: { "Result" : false,
     * "Message" : "Complex change was not found in the ontology of changes."}
     * if the complex change is not found.
     * <li> Error code: <b>200</b> and entity content: { "Result" : true,
     * "Message" : "...." } where an example response message could be: <br>
     * { <br>
     * "Complex_Change": "Mark_as_Obsolete_v2", <br>
     * "Priority": 1, <br>
     * "Complex_Change_Parameters": [ <br>
     * { <br>
     * "obs_class": "sc1:-subclass" <br>
     * } <br>
     * ], <br>
     * "Simple_Changes": [ <br>
     * { <br>
     * "Simple_Change": "ADD_SUPERCLASS", <br>
     * "Simple_Change_Uri": "sc1", <br>
     * "Is_Optional": false, <br>
     * "Selection_Filter": "sc1:-superclass =
     * <http://www.geneontology.org/formats/oboInOwl#ObsoleteClass>", <br>
     * "Mapping_Filter": "", <br>
     * "Join_Filter": "", <br>
     * "Version_Filter": "" <br>
     * } <br>
     * ] <br>
     * } <br>
     * </ul>
     */
    @GET
    @Path("{com_change}")
    public Response getCCJSON(@PathParam("com_change") String name) {
        boolean result = false;
        String message = null;
        int code = 0;

        JDBCVirtuosoRep jdbcRep;
        try {
            jdbcRep = new JDBCVirtuosoRep(propertiesManager.getProperties());
        } catch (ClassNotFoundException | SQLException | IOException ex) {
            result = false;
            String json = "{ \"Message\" : \"Exception Occured: " + ex.getMessage() + ", \"Result\" : " + result + " }";
            return Response.status(400).entity(json).build();
        }
        String ontologySchema = propertiesManager.getPropertyValue("Changes_Ontology_Schema");
        String query = "select ?json from <" + ontologySchema + "> where { ?s co:name \"" + name + "\"; co:json ?json. }";
        ResultSet res = jdbcRep.executeSparqlQuery(query, false);
        try {
            if (res.next()) {
                message = (String) res.getString("json");
                result = true;
                code = 200;
            } else {
                message = "\"Complex change was not found in the ontology of changes.\"";
                result = false;
                code = 204;
            }
        } catch (SQLException ex) {
            message = ex.getMessage();
            result = false;
            code = 400;
        }
        jdbcRep.terminate();
        String json = "{ \"Message\" : " + message + ", \"Result\" : " + result + " }";
        return Response.status(code).entity(json).build();
    }

    /**
     * <b>DELETE</b> method which deletes the complex name with name given as
     * parameter from the ontology of changes. In fact, it deletes all the
     * correlated triples with the corresponding complex change. This means that
     * any detected changes of this complex type will be deleted as well. <br>
     * <b>URL:</b> /diachron/complex_change/{com_change}
     *
     * @param <b>com_change</b> Path parameter which refers on the complex
     * change name.
     * @return A Response instance which has a JSON-encoded entity content. We
     * discriminate the following cases: <br>
     * <ul>
     * <li> Error code: <b>200</b> and entity content: { "Result" : true,
     * "Message" : "Complex Change was successfully deleted from the ontology of
     * changes."} if the complex change is found and successfully deleted.
     * <li> Error code: <b>200</b> and entity content: { "Result" : false,
     * "Message" : "Complex Change was not found in the ontology of changes." }
     * if the complex change is not found.
     * </ul>
     */
    @DELETE
    @Path("{com_change}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteCCJSON(@PathParam("com_change") String name) {
        String message = null;
        int code;
        boolean result = false;

        String changesOntologySchema = propertiesManager.getPropertyValue("Changes_Ontology_Schema");
        String datasetUri = propertiesManager.getPropertyValue("Dataset_Uri");
        CCManager manager = null;
        try {
            manager = new CCManager(propertiesManager.getProperties(), changesOntologySchema);
        } catch (Exception ex) {
            result = false;
            String json = "{ \"Message\" : \"Exception Occured: " + ex.getMessage() + ", \"Result\" : " + result + " }";
            return Response.status(400).entity(json).build();
        }
        List<String> ontologies = Utils.getChangesOntologies(datasetUri, manager.getJdbcRep());
        if (ontologies.isEmpty()) {
            result = manager.deleteComplexChange(changesOntologySchema, name);
        } else {
            for (String ontology : ontologies) {
                boolean tmp = manager.deleteComplexChange(ontology, name);
                if (tmp) {
                    result = tmp;
                }
            }
        }
        message = null;
        if (result) {
            code = 200;
            message = "Complex Change was successfully deleted from the ontology of changes.";
        } else {
            code = 204;
            message = "Complex Change was not found in the ontology of changes.";
        }
        String json = "{ \"Message\" : \"" + message + "\", \"Result\" : " + result + " }";
        manager.terminate();
        return Response.status(code).entity(json).build();
    }

    /**
     * <b>POST</b> method which is responsible for the definition of a complex
     * change and its storage in the ontology of changes.
     * <b>URL:</b> /diachron/complex_change
     *
     * @param <b>inputMessage</b> : A JSON-encoded string which represents the
     * definition of a complex change and has the following form: <br>
     * { <br>
     * "Complex_Change": "Mark_as_Obsolete_v2", <br>
     * "Priority": 1, <br>
     * "Complex_Change_Parameters": [ <br>
     * { <br>
     * "obs_class": "sc1:-subclass" <br>
     * } <br>
     * ], <br>
     * "Simple_Changes": [ <br>
     * { <br>
     * "Simple_Change": "ADD_SUPERCLASS", <br>
     * "Simple_Change_Uri": "sc1", <br>
     * "Is_Optional": false, <br>
     * "Selection_Filter": "sc1:-superclass =
     * <http://www.geneontology.org/formats/oboInOwl#ObsoleteClass>", <br>
     * "Mapping_Filter": "", <br>
     * "Join_Filter": "" <br>
     * } <br>
     * ], 
     * "Version_Filters" : [ 
     * { <br>
     * "Subject" : "sc1:ADD_SUPERCLASS:-subject", <br>
     * "Predicate" : "rdfs:subClassOf", <br>
     * "Object" : "rdfs:Resource", <br>
     * "Presence" : "EXISTS_IN_V2" <br>
     * } <br>
     * ] <br>
     * } <br>
     * where
     * <ul>
     * <li> Complex_Change: the name of the complex change.
     * <li> Priority: its priority which can be any double number.
     * <li> Complex_Change_Parameters: the parameter names of the complex change
     * along with the simple change parameter names which are binded with.
     * <li> Simple_Changes: an array of simple changes which consist the complex
     * change. For each simple change we have the following fields:
     * <ul>
     * <li> Simple_Change: the name of the simple change.
     * <li> Simple_Change_Uri: a uri instance of the simple change.
     * <li> Is_Optional: a boolean which indicates whether the simple change is
     * optional (true) or mandatory (false).
     * <li> Selection_Filter: filter which assigns selective values upon simple
     * change parameters.
     * <li> Mapping_Filter: filter which expresses mappings upon simple change
     * parameters.
     * <li> Join_Filter: filter which expresses joins across different
     * parameters of simple changes.
     * </ul>
     * <li> Version_Filters: an array of version filters which have to be satisfied. 
     * Each version filter is essentialy a triple which have to exist (or not) in either 
     * new or old dataset version. Each part of a trile (subject, predicate, object) can 
     * be a) a specific uri, b) a complex change parameter, c) a simple change parameter. 
     * The presence of the triple is denoted by a flag which takes one of the values; 
     * EXISTS_IN_V2, EXISTS_IN_V1, NOT_EXISTS_IN_V2, NOT_EXISTS_IN_V1
     * </ul>
     * @return A Response instance which has a JSON-encoded entity content
     * depending on the input parameter of the method. We discriminate the
     * following cases: <br>
     * <ul>
     * <li> Error code: <b>200</b> and entity content: { "Success" : true,
     * "Message" : "Complex Change's definition was inserted in the ontology of
     * changes." } if the complex change is successfully defined and stored.
     * <li> Error code: <b>204</b> and entity content: { "Success" : false,
     * "Message" : "There is already defined a Complex Change with the same
     * name." } if there exists a complex change with the same name in the
     * ontology of changes.
     * <li> Error code: <b>400</b> and entity content: { "Success" : false,
     * "Message" : "JSON input message could not be parsed." } if the input
     * parameter has not the correct form.
     * <li> Error code: <b>400</b> and entity content: { "Success" : false,
     * "Message" : "Error in {Selection|Join|Parameter} Filters." } if there is
     * the issued filters namely, Selection, Join, Parameter has not the correct
     * syntactic form.
     * </ul>
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response defineCCJSON(String inputMessage) {

        CCManager ccDef = null;
        String changesOntologySchema = propertiesManager.getPropertyValue("Changes_Ontology_Schema");
        try {
            ccDef = JSONMessagesParser.createCCDefinition(propertiesManager.getProperties(), inputMessage, changesOntologySchema);
        } catch (Exception ex) {
            boolean result = false;
            String json = "{ \"Message\" : \"Exception Occured: " + ex.getMessage() + ", \"Result\" : " + result + " }";
            return Response.status(400).entity(json).build();
        }
        if (ccDef == null) {
            String message = "JSON input message could not be parsed.";
            String json = "{ \"Message\" : \"" + message + "\", \"Success\" : false }";
            return Response.status(400).entity(json).build();
        } else {
            String message = null;
            ccDef.insertChangeDefinition();
            int code;
            boolean result;

            if (ccDef.getCcDefError().getErrorCode() == CODE.NON_UNIQUE_CC_NAME) {
                code = 204;
                message = ccDef.getCcDefError().getDescription();
                result = false;
            } else if (ccDef.getCcDefError().getErrorCode() == null) {
                code = 200;
                message = "Complex Change's definition was inserted in the ontology of changes.";
                result = true;
            } else {
                code = 400;
                message = ccDef.getCcDefError().getDescription();
                result = false;
            }
            String json = "{ \"Message\" : \"" + message + "\", \"Success\" : " + result + " }";
            ccDef.terminate();
            return Response.status(code).entity(json).build();
        }
    }
}
