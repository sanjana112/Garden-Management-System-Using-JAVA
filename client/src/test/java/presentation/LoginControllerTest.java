package presentation;

import bll.AuthenticationManager;
import bll.exceptions.BllException;
import bll.service.PlantService;
import bll.service.PlantedPlantService;
import bll.service.PlotService;
import bll.service.RoleService;
import bll.service.UserPlantRequestService;
import bll.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LoginControllerTest {

    @Mock
    private LoginView view;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AuthenticationManager authenticationManager;
    @Mock
    private UserService userService;
    @Mock
    private PlantService plantService;
    @Mock
    private RoleService roleService;
    @Mock
    private PlantedPlantService plantedPlantService;
    @Mock
    private PlotService plotService;
    @Mock
    private UserPlantRequestService requestService;
    @Mock
    private GardenView gardenView;
    @Mock
    private UserView userView;

    private LoginController loginController;

    @Before
    public void setUp() {
        loginController = spy(new LoginController(view, userService,
                plantService, roleService, plantedPlantService,
                plotService, requestService, authenticationManager));
    }

    @Test
    public void testLogInAdmin() {
        String username = "admin";
        String password = "admin";

        when(view.showPasswordDialog()).thenReturn(new String[]{username, password});
        when(authenticationManager.validateUser(username, password)).thenReturn(true);
        when(authenticationManager.getClient().getRole()).thenReturn("admin");

        doReturn(userView).when(loginController).getUserView();

        loginController.logIn();

        verify(view).showPasswordDialog();
        verify(loginController).createAdminGUI();
    }

    @Test
    public void testLogInUser() {
        String username = "user";
        String password = "user";

        when(view.showPasswordDialog()).thenReturn(new String[]{username, password});
        when(authenticationManager.validateUser(username, password)).thenReturn(true);
        when(authenticationManager.getClient().getRole()).thenReturn("user");

        doReturn(gardenView).when(loginController).getGardenView();

        loginController.logIn();

        verify(view).showPasswordDialog();
        verify(loginController).createEmployeeGUI();
    }

    @Test
    public void testLogInInvalidUser() {
        String username = "user";
        String password = "user";

        when(view.showPasswordDialog()).thenReturn(new String[]{username, password});
        when(authenticationManager.validateUser(username, password)).thenReturn(false, true);
        when(authenticationManager.getClient().getRole()).thenReturn("user");

        doReturn(gardenView).when(loginController).getGardenView();

        loginController.logIn();

        verify(view, times(2)).showPasswordDialog();
        verify(loginController).createEmployeeGUI();
    }

    @Test
    public void testLogInBllException() {
        String username = "user";
        String password = "user";

        doReturn(gardenView).when(loginController).getGardenView();

        when(view.showPasswordDialog()).thenReturn(new String[]{username, password});
        when(authenticationManager.validateUser(username, password)).thenThrow(new BllException("new Error")).
                thenReturn(true);
        when(authenticationManager.getClient().getRole()).thenReturn("user");

        loginController.logIn();

        verify(view).showMessage("new Error");
    }
}