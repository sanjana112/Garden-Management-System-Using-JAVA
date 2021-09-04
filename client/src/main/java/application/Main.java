package application;

import bll.AuthenticationManager;
import bll.exceptions.GlobalExceptionHandlerUtil;
import bll.service.PlantService;
import bll.service.PlantedPlantService;
import bll.service.PlotService;
import bll.service.RoleService;
import bll.service.UserPlantRequestService;
import bll.service.UserService;
import connection.ClientConnection;
import connection.ClientConnectionImpl;
import connection.NotificationConnection;
import presentation.LoginController;
import presentation.LoginViewImpl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws IOException {

        GlobalExceptionHandlerUtil.enableGlobalException();

        Properties prop = new Properties();
        String fileName = "src/main/resources/app.config";
        InputStream is = new FileInputStream(fileName);
        prop.load(is);

        String ip = prop.getProperty("app.ip");

        final ClientConnection clientConnection = new ClientConnectionImpl(6666, ip);
        final NotificationConnection notificationConnection = new NotificationConnection(6667, ip);

        final RoleService roleService = new RoleService(clientConnection);
        final UserService userService = new UserService(clientConnection);
        final PlantService plantService = new PlantService(clientConnection);
        final PlantedPlantService plantedPlantService = new PlantedPlantService(clientConnection);
        final PlotService plotService = new PlotService(clientConnection);
        final UserPlantRequestService requestService = new UserPlantRequestService(clientConnection);

        final AuthenticationManager authenticationManager = new AuthenticationManager(clientConnection,
                notificationConnection);

        LoginController loginController = new LoginController(new LoginViewImpl(), userService,
                plantService, roleService, plantedPlantService,
                plotService, requestService, authenticationManager);

        loginController.logIn();

    }
}

