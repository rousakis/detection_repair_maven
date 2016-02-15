package services;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.diachron.detection.utils.StoreConnection;

public class AppStarter  extends HttpServlet {/**
 *
 */
private static final long serialVersionUID = -8939757419152372438L;

    @Override
    public void init() throws ServletException {
        StoreConnection.init();
    }



}
