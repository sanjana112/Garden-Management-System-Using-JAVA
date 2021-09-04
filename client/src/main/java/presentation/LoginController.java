package presentation;

import bll.AuthenticationManager;
import bll.exceptions.BllException;
import bll.service.PlantService;
import bll.service.PlantedPlantService;
import bll.service.PlotService;
import bll.service.RoleService;
import bll.service.UserPlantRequestService;
import bll.service.UserService;
import bll.util.ReportGenerator;
import lombok.AllArgsConstructor;
import model.Garden;

import java.util.HashSet;

@AllArgsConstructor
public class LoginController {
    private LoginView view;
    private UserService userService;
    private PlantService plantService;
    private RoleService roleService;
    private PlantedPlantService plantedPlantService;
    private PlotService plotService;
    private UserPlantRequestService requestService;
    private AuthenticationManager authenticationManager;

    public void logIn() {
        String loginDetails[] = view.showPasswordDialog();

        if (loginDetails != null) {
            String userName = loginDetails[0];
            String password = loginDetails[1];

            try {
                if (authenticationManager.validateUser(userName, password)) {//valid login
                    if (authenticationManager.getClient().getRole().equals("admin")) {
                        createAdminGUI();
                    } else {
                        createEmployeeGUI();
                    }
                } else {
                    view.showMessage("Something happened. Try again!");
                    logIn();//of course this can be an infinite loop
                }
            } catch (BllException e) {
                view.showMessage(e.getMessage());
                logIn();//of course this can be an infinite loop
            }
        }
    }

    protected void createEmployeeGUI() {
        GardenModel gardenModel = new GardenModel();
        new GardenController(getGardenView(), gardenModel, plantService,
                plantedPlantService, plotService, requestService,
                authenticationManager);
    }

    protected void createAdminGUI() {
        CrudModel model = new CrudModel(new Garden(new HashSet<>(), new HashSet<>()), new HashSet<>(), new HashSet<>());
        new UserController(getUserView(), model, userService, plantService,
                roleService, plantedPlantService, plotService, requestService,
                authenticationManager, new ReportGenerator());
    }

    protected GardenView getGardenView() {
        return new GardenViewImpl();
    }

    protected UserView getUserView() {
        return new UserViewImpl();
    }
}
