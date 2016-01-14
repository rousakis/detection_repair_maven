/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package clients;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import javax.ws.rs.core.MediaType;
import org.diachron.detection.complex_change.CCManager;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utils.Utils;

/**
 *
 * @author rousakis
 */
public class TestDefineCC {

    public static void main(String[] args) throws ParseException {
        Client c = Client.create();
        System.out.println("Testing Define CC Service...");
        String ip = "139.91.183.48:8181";
        ip = "139.91.183.40:8080";
        String url = "http://" + ip + "/ForthMaven-1.0/diachron/complex_change";
        WebResource r = c.resource(url);

        String ccDef = "{ "
                + "\"Complex_Change\" : \"Mark_as_Obsolete_v2\", "
                + "\"Priority\" : 1.0, "
                + "\"Complex_Change_Parameters\": ["
                + "{ \"obs_class\" : \"sc1:-subclass\" }"
                + "],"
                + "\"Simple_Changes\": ["
                + "{"
                + "\"Simple_Change\" : \"ADD_SUPERCLASS\", "
                + "\"Simple_Change_Uri\" : \"sc1\", "
                + "\"Is_Optional\" : false, "
                + "\"Selection_Filter\" : \"sc1:-superclass = <http://www.geneontology.org/formats/oboInOwl#ObsoleteClass>\", "
                + "\"Mapping_Filter\" : \"\", "
                + "\"Join_Filter\" : \"\" "
                + "} ], "
                + " \"Version_Filters\" : [\n"
                + " ]"
                + "}";

        String datasetUri = "http://test";

//        JSONObject input = new JSONObject();
//        input.put("Dataset_URI", datasetUri);
//        input.put("CC_Definition", ccDef);
////
//        ClientResponse response = r.type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, input.toJSONString());
//        System.out.println(response.getEntity(String.class));
//        System.out.println(response.getStatus());
//        System.out.println("-----\n");
        for (int i = 0; i < 500; i++) {
            datasetUri = "http://test/" + i;
            JSONObject input = new JSONObject();
            input.put("Dataset_URI", datasetUri);
            input.put("CC_Definition", ccDef);
            ClientResponse response = r.type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, input.toJSONString());
            System.out.println(datasetUri + "\tStatus: " + response.getStatus());
            System.out.println("-----");
        }

    }
}
