/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package services.change_detection;

import java.io.IOException;
import java.io.OutputStream;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.sparqljson.SPARQLResultsJSONWriter;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLWriter;
import org.openrdf.query.resultio.text.csv.SPARQLResultsCSVWriter;
import org.openrdf.query.resultio.text.tsv.SPARQLResultsTSVWriter;
import org.openrdf.repository.RepositoryException;
import org.diachron.detection.repositories.SesameVirtRep;
import org.diachron.detection.utils.ChangesDetector;
import org.diachron.detection.utils.ChangesManager;
import utils.PropertiesManager;
import utils.Utils;

/**
 * REST Web Service
 *
 * @author rousakis
 */
@Path("change_detection")
public class ChangeDetectionImpl {

    PropertiesManager propertiesManager = PropertiesManager.getPropertiesManager();

    public ChangeDetectionImpl() {
    }

    /**
     * <b>POST</b> method which is responsible for the change detection process
     * among two dataset versions. This method detects any existing simple and
     * complex changes and updates the ontology of changes accordingly. <br>
     * <b>URL:</b> /diachron/change_detection
     *
     * @param <b>inputMessage</b> : A JSON-encoded string which has the
     * following form: <br>
     * { <br>
     * "Dataset_Uri" : "http://dataset", <br>
     * "Old_Version" : "v1", <br>
     * "New_Version" : "v2", <br>
     * "Ingest" : true, <br>
     * "Complex_Changes" : ["Label_Obsolete", ...] <br>
     * "Associations" : "assoc" <br>
     * } <br>
     * where
     * <ul>
     * <li>Dataset_Uri - The URI of the dataset whose versions will be compared.
     * If this parameter is missing from the JSON input message, then the URI
     * will be taken from the properties file. <br>
     * <li>Old_Version - The old version URI of a DIACHRON entity.<br>
     * <li>New_Version - The old version URI of a DIACHRON entity.<br>
     * <li>Ingest - A flag which denotes whether the service is called due to a
     * new dataset ingestion or not.
     * <li>Complex_Changes - The set of complex change types which will be
     * considered. If the set is empty, then all the defined complex changes in
     * the ontology of changes will be considered.
     * <li>Associations - The named graph URI which contains the associations
     * among URIs between the old and new version. We say that a URI is
     * associated with another one when they refer on the same object across
     * versions thus, it would be more intuitively correct to report such
     * changes in a different way. If null is given, then no associations are
     * considered.
     * </ul>
     * @return A Response instance which has a JSON-encoded entity content
     * depending on the input parameter of the method. We discriminate the
     * following cases: <br>
     * <ul>
     * <li> Error code: <b>400</b> and entity content: { "Success" : false,
     * "Message" : "JSON input message should have exactly 5 arguments." } if
     * the input parameter has not five JSON parameters.
     * <li> Error code: <b>200</b> and entity content: { "Success" : true,
     * "Message" : "Change detection among versions Old_Version, New_Version was
     * executed." } if the input parameter has the correct form.
     * <li> Error code: <b>400</b> and entity content: { "Success" : false,
     * "Message" : "JSON input message could not be parsed." } if the input
     * parameter has not the correct form.
     * </ul>
     *
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeDetectJSON(String inputMessage) {
        JSONParser jsonParser = new JSONParser();
        ChangesDetector detector = null;
        try {
            JSONObject jsonObject = (JSONObject) jsonParser.parse(inputMessage);
            if (jsonObject.size() != 5 && jsonObject.size() != 6) {
                String message = "JSON input message should have 5 or 6 arguments.";
                String json = "{ \"Success\" : false, "
                        + "\"Message\" : \"" + message + "\" }";
                return Response.status(400).entity(json).build();
            } else {
                String datasetUri = (String) jsonObject.get("Dataset_URI");
                if (datasetUri == null) {
                    datasetUri = propertiesManager.getPropertyValue("Dataset_URI");
                }
                String oldVersion = (String) jsonObject.get("Old_Version");
                String newVersion = (String) jsonObject.get("New_Version");
                boolean ingest = (Boolean) jsonObject.get("Ingest");
                JSONArray ccs = (JSONArray) jsonObject.get("Complex_Changes");
                String associations = (String) jsonObject.get("Associations");
                if (oldVersion == null || newVersion == null || ccs == null) {
                    throw new ParseException(-1);
                }
                String changesOntologySchema = Utils.getDatasetSchema(datasetUri);
                ChangesManager cManager = null;
                try {
                    cManager = new ChangesManager(propertiesManager.getProperties(), datasetUri, oldVersion, newVersion, false);
                    String changesOntology = cManager.getChangesOntology();
                    detector = new ChangesDetector(propertiesManager.getProperties(), changesOntology, changesOntologySchema, associations);
                } catch (Exception ex) {
                    String json = "{ \"Success\" : false, "
                            + "\"Message\" : \"Exception Occured: " + ex.getMessage() + " \" }";
                    return Response.status(400).entity(json).build();
                } finally {
                    if(cManager != null){cManager.terminate();}
                }
                if (ingest) {
                    detector.detectAssociations(oldVersion, newVersion);
                    detector.detectSimpleChanges(oldVersion, newVersion, null);
                }
                String[] cChanges = {};
                if (!ccs.isEmpty()) {
                    cChanges = new String[ccs.size()];
                    for (int i = 0; i < ccs.size(); i++) {
                        cChanges[i] = (String) ccs.get(i);
                    }
                }
                detector.detectComplexChanges(oldVersion, newVersion, cChanges);
                String json = "{ \"Success\" : true, "
                        + "\"Message\" : \"Change detection among versions <" + oldVersion + ">, <" + newVersion + "> was executed. \" }";
                return Response.status(200).entity(json).build();
            }
        } catch (ParseException ex) {
            String message = "JSON input message could not be parsed.";
            String json = "{ \"Success\" : false, "
                    + "\"Message\" : \"" + message + "\" }";
            return Response.status(400).entity(json).build();
        } finally {
            if (detector != null) {detector.terminate();}
        }
    }

    /**
     * <b>GET</b> method which applies SPARQL queries on the ontology of changes
     * and returns the results in various formats. <br>
     * <b>URL:</b> /diachron/change_detection?query={query1}&format={format1}
     *
     * @param <b>query</b> Query parameter which has a string value representing
     * the requested SPARQL query.
     * @param <b>format</b> Query parameter which refers on the requested format
     * of the results. The formats which are supported are: <b>xml</b>,
     * <b>csv</b>, <b>tsv</b>, <b>json.</b>
     * @return A Response instance which has a JSON-encoded entity content with
     * the query results in the requested format. We discriminate the following
     * cases: <br>
     * <ul>
     * <li> Error code: <b>400</b> if the given SPARQL query contains syntax
     * errors or there was an internal server issue. In any case, the entity
     * content explains the problem's category.
     * <li> Error code: <b>200</b> and entity content with the query results
     * with the requested format.
     * <li> Error code: <b>406</b> and entity content "Invalid results format
     * given." if the requested query results format is not recognized.
     * </ul>
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response queryChangesOntologyGet(@QueryParam("query") String query,
            @QueryParam("format") String format) {
        OutputStream output = null;
        SesameVirtRep sesame = null;
        try {
            String ip = propertiesManager.getPropertyValue("Repository_IP");
            String username = propertiesManager.getPropertyValue("Repository_Username");
            String password = propertiesManager.getPropertyValue("Repository_Password");
            String changesOntol = propertiesManager.getPropertyValue("Changes_Ontology");
            int port = Integer.parseInt(propertiesManager.getPropertyValue("Repository_Port"));
            sesame = new SesameVirtRep(ip, port, username, password);
            query = query.replace(" where ", " from <" + changesOntol + "> ");
            TupleQuery tupleQuery = sesame.getCon().prepareTupleQuery(QueryLanguage.SPARQL, query);
            output = new OutputStream() {
                private StringBuilder string = new StringBuilder();

                @Override
                public void write(int b) throws IOException {
                    this.string.append((char) b);
                }

                public String toString() {
                    return this.string.toString();
                }
            };
            TupleQueryResultHandler writer;
            switch (format) {
                case "xml":
                    writer = new SPARQLResultsXMLWriter(output);
                    break;
                case "csv":
                    writer = new SPARQLResultsCSVWriter(output);
                    break;
                case "tsv":
                    writer = new SPARQLResultsTSVWriter(output);
                    break;
                case "json":
                    writer = new SPARQLResultsJSONWriter(output);
                    break;
                default:
                    String result = "Invalid results format given.";
                    return Response.status(406).entity(result).build();
            }
            tupleQuery.evaluate(writer);
        } catch (MalformedQueryException | QueryEvaluationException | TupleQueryResultHandlerException | RepositoryException ex) {
            return Response.status(400).entity(ex.getMessage()).build();
        } finally {
            try {output.close();} catch (IOException e) {e.printStackTrace();}
            sesame.terminate();
        }
        return Response.status(200).entity(output.toString()).build();
    }

    /**
     * <b>POST</b> method which applies SPARQL queries on the ontology of
     * changes and returns the results in various formats. <br>
     * <b>URL (partial):</b> /diachron/change_detection/query
     *
     * @param <b>inputMessage</b> : A JSON-encoded string which has the
     * following form: <br>
     * { <br>
     * "Query" : "select ?s ?p ...", <br>
     * "Format" : "json", <br>
     * } <br>
     * where
     * <ul>
     * <li>Query - A string which represents the SPARQL query which will be
     * applied.<br>
     * <li>Format - The format(MIME type) of the query results. The formats
     * which are supported are: <b>xml</b>, <b>csv</b>, <b>tsv</b>, <b>json.</b>
     * </ul>
     * @return A Response instance which has a JSON-encoded entity content with
     * the query results in the requested format. We discriminate the following
     * cases: <br>
     * <ul>
     * <li> Error code: <b>400</b> if the given SPARQL query contains syntax
     * errors or there was an internal server issue. In any case, the entity
     * content explains the problem's category.
     * <li> Error code: <b>200</b> and entity content with the query results
     * with the requested format.
     * <li> Error code: <b>406</b> and entity content "Invalid results format
     * given." if the requested query results format is not recognized.
     * <li> Error code: <b>400</b> and entity content: { "Success" : false,
     * "Message" : "JSON input message should have exactly 2 arguments." } if
     * the input parameter has more than two JSON parameters.
     * <li> Error code: <b>400</b> and entity content: { "Success" : false,
     * "Message" : "JSON input message could not be parsed." } if the input
     * parameter has not the correct form.
     * </ul>
     */
    @POST
    @Path("/query")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response queryChangesOntologyPost(String inputMessage) {
        JSONParser jsonParser = new JSONParser();
        OutputStream output = null;
        SesameVirtRep sesame = null;
        try {
            JSONObject jsonObject = (JSONObject) jsonParser.parse(inputMessage);
            if (jsonObject.size() != 2) {
                String message = "JSON input message should have exactly 2 arguments.";
                String json = "{ \"Success\" : false, "
                        + "\"Message\" : \"" + message + "\" }";
                return Response.status(400).entity(json).build();
            } else {
                String query = (String) jsonObject.get("Query");
                String format = (String) jsonObject.get("Format");
                if (format == null || query == null) {
                    throw new ParseException(-1);
                }
                String ip = propertiesManager.getPropertyValue("Repository_IP");
                String username = propertiesManager.getPropertyValue("Repository_Username");
                String password = propertiesManager.getPropertyValue("Repository_Password");
                String changesOntol = propertiesManager.getPropertyValue("Changes_Ontology");
                int port = Integer.parseInt(propertiesManager.getPropertyValue("Repository_Port"));
                sesame = new SesameVirtRep(ip, port, username, password);
                query = query.replace(" where ", " from <" + changesOntol + "> ");
                TupleQuery tupleQuery = sesame.getCon().prepareTupleQuery(QueryLanguage.SPARQL, query);
                output = new OutputStream() {

                    private StringBuilder string = new StringBuilder();

                    @Override
                    public void write(int b) throws IOException {
                        this.string.append((char) b);
                    }

                    public String toString() {
                        return this.string.toString();
                    }
                };
                ///
                TupleQueryResultHandler writer;
                switch (format) {
                    case "xml":
                        writer = new SPARQLResultsXMLWriter(output);
                        break;
                    case "csv":
                        writer = new SPARQLResultsCSVWriter(output);
                        break;
                    case "tsv":
                        writer = new SPARQLResultsTSVWriter(output);
                        break;
                    case "json":
                        writer = new SPARQLResultsJSONWriter(output);
                        break;
                    default:
                        String result = "Invalid results format given.";
                        return Response.status(406).entity(result).build();
                }
                tupleQuery.evaluate(writer);
            }
        } catch (MalformedQueryException | QueryEvaluationException | TupleQueryResultHandlerException | RepositoryException ex) {
            return Response.status(400).entity(ex.getMessage()).build();
        } catch (ParseException ex) {
            String message = "JSON input message could not be parsed.";
            String json = "{ \"Success\" : false, "
                    + "\"Message\" : \"" + message + "\" }";
            return Response.status(400).entity(json).build();
        } finally {
            try {if (output != null){output.close();}} catch (IOException e) {e.printStackTrace();}
            if (sesame != null){sesame.terminate();}
        }
        return Response.status(200).entity(output.toString()).build();
    }
}
