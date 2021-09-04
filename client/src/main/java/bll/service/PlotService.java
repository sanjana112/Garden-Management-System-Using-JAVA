package bll.service;

import connection.ClientConnection;
import model.Plot;

public class PlotService extends GenericService<Plot> {

    public PlotService(ClientConnection client) {
        super(client);
    }


}
