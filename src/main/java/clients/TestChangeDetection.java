/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package clients;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import javax.ws.rs.core.MediaType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author rousakis
 */
public class TestChangeDetection {

    public static void main(String[] args) {
        Client c = Client.create();
        String v1 = "http://www.diachron-fp7.eu/resource/recordset/efo/2.34";
        String v2 = "http://www.diachron-fp7.eu/resource/recordset/efo/2.35";
        boolean ingest = true;
        String ip = "139.91.183.48";
        ip = "localhost";
        WebResource r = c.resource("http://" + ip + ":8181/ForthMaven-1.0/diachron/change_detection");
        JSONObject input = new JSONObject();
        input.put("Dataset_URI", "http://www.ebi.ac.uk/efo/");
        input.put("Old_Version", v1);
        input.put("New_Version", v2);
        input.put("Ingest", ingest);
        input.put("Complex_Changes", new JSONArray());
        input.put("Associations", null);
        ClientResponse response = r.type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, input.toJSONString());
        System.out.println(response.getEntity(String.class));
        System.out.println(response.getStatus());
    }
}
