package bll.service;

import connection.ClientConnection;
import model.Plant;

public class PlantService extends GenericService<Plant> {

    public PlantService(ClientConnection client) {
        super(client);
    }

}
