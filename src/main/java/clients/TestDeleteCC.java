/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package clients;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.json.simple.JSONObject;

/**
 *
 * @author rousakis
 */
public class TestDeleteCC {

    public static void main(String[] args) {
        Client c = Client.create();

        // test delete cc service
        System.out.println("Testing Delete Service...");
        String ip = "139.91.183.48:8181";
        ip = "139.91.183.40:8080";
        String url = "http://" + ip + "/ForthMaven-1.0/diachron/complex_change";
        String ccName = "Mark_as_Obsolete_v2";
//        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
//        params.putSingle("name", ccName);
//        params.putSingle("dataset_uri", "http://test");
//        WebResource r = c.resource(url);
//        ClientResponse response = r.queryParams(params).accept(MediaType.APPLICATION_JSON).delete(ClientResponse.class);
//        System.out.println(response.getEntity(String.class));
//        System.out.println(response.getStatus());
//        System.out.println("-----\n");
        ////
        String datasetUri;
        for (int i = 0; i < 200; i++) {
            datasetUri = "http://test/" + i;
            MultivaluedMap<String, String> params = new MultivaluedMapImpl();
            params.putSingle("name", ccName);
            params.putSingle("dataset_uri", datasetUri);
            WebResource r = c.resource(url);
            ClientResponse response = r.queryParams(params).accept(MediaType.APPLICATION_JSON).delete(ClientResponse.class);
            System.out.println(datasetUri + "\tStatus: " + response.getStatus());
            System.out.println("-----");
        }

    }
}
