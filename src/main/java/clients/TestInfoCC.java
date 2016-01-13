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
/**
 *
 * @author rousakis
 */
public class TestInfoCC {

    public static void main(String[] args) {
        Client c = Client.create();
        System.out.println("Testing Fetch CC Service...");
        String ip = "139.91.183.48:8181";
//        ip = "139.91.183.40:8080";
        String url = "http://" + ip + "/ForthMaven-1.0/diachron/complex_change";
        WebResource r = c.resource(url);
        String ccName = "Add Synonym";
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.putSingle("name", ccName);
        params.putSingle("dataset_uri", "http://www.ebi.ac.uk/efo/");
        ClientResponse response = r.queryParams(params).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        System.out.println(response.getEntity(String.class));
        System.out.println(response.getStatus());
        System.out.println("-----\n");

    }
}
