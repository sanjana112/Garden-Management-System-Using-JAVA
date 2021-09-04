package presentation;

import bll.AuthenticationManager;
import bll.exceptions.DuplicateEntityException;
import bll.observer.Channel;
import bll.observer.ChannelObservable;
import bll.service.PlantService;
import bll.service.PlantedPlantService;
import bll.service.PlotService;
import bll.service.RoleService;
import bll.service.UserPlantRequestService;
import bll.service.UserService;
import bll.util.FileMaker;
import bll.util.ReportGenerator;
import bll.util.ReportType;
import model.Garden;
import model.Plant;
import model.PlantedPlant;
import model.Plot;
import model.User;
import model.UserPlantRequest;
import model.UserRole;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static presentation.TestMatchersUtil.userNamed;

@RunWith(MockitoJUnitRunner.class)
public class UserControllerTest {
    @Mock
    private UserView view;
    @Mock
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
    private ReportGenerator reportGenerator;
    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    private UserController controller;

    private CrudModel model;

    private UserRole userRole;

    @Before
    public void setUp() {
        userRole = new UserRole("user", new HashSet<>());
        loadGarden();

        model = new CrudModel(new Garden(new HashSet<>(), new HashSet<>()), new HashSet<>(), new HashSet<>());
        controller = new UserController(view, model, userService,
                plantService, roleService, plantedPlantService,
                plotService, requestService, authenticationManager, reportGenerator);
    }

    @Test
    public void testLoadGardenUser() {
        verify(userService, times(1)).findRegularUsers();
        assertEquals("2 users loaded", 2, model.getRegularUsers().size());
    }

    @Test
    public void testLoadGardenPlot() {
        verify(plotService, times(1)).findAll();
        assertEquals("2 plots loaded", 2, model.getGarden().getPlots().size());
    }

    @Test
    public void testLoadGardenPlant() {
        verify(plantService, times(1)).findAll();
        assertEquals("6 plants loaded", 5, model.getPlants().size());
    }

    @Test
    public void testLoadGardenPlantedPlant() {
        verify(plantedPlantService, times(1)).findAll();
        assertEquals("3 plantedplants loaded", 3, model.getGarden().getPlantedPlants().size());
    }

    @Test
    public void testLoadGardenRole() {
        verify(roleService, times(1)).findByName("user");

        assertEquals("userrole loaded", "user", model.getRegularRole().getName());
    }

    @Test
    public void testChangeTable() {

        controller.changeTable(new ArrayList<>(model.getRegularUsers()));

        //these methods are also called
        verify(view, atLeast(1)).setTable(any());
        verify(view, atLeast(1)).addFrame();
    }

    @Test
    public void testGetValueColumnId() {
        assertEquals("usercontroller id field is passed as it is", "1", controller.getValueColumn("id", 1L));
    }

    @Test
    public void testGetValueColumnUser() {
        assertEquals("usercontroller user field is passed as it is", "user", controller.getValueColumn("userName", "user"));
    }

    @Test
    public void testGetValueColumnPassword() {
        assertEquals("usercontroller password field is passed as it is", "password", controller.getValueColumn("password", "password"));
    }

    @Test
    public void testIsCellEditableTable() {
        assertFalse("None of the cells is editable", controller.isCellEditableTable(0, 0));
    }

    @Test
    public void testCreateTableId() {
        DefaultTableModel defaultTableModel = controller.createTable(new ArrayList<>(model.getRegularUsers()));
        assertEquals("first column is id", "id", defaultTableModel.getColumnName(0));
    }

    @Test
    public void testCreateTableUserName() {
        DefaultTableModel defaultTableModel = controller.createTable(new ArrayList<>(model.getRegularUsers()));
        assertEquals("second column is username", "username", defaultTableModel.getColumnName(1));
    }

    @Test
    public void testGetRegularUsers() {
        List<User> users = controller.getRegularUsers();
        assertEquals("initialized with 2 users", 2, users.size());
    }

    @Test
    public void testAddUser() {
        when(userService.save(any(User.class))).thenReturn(
                new User("test", "encoded", userRole));

        controller.addUser("test", "test");

        assertTrue("new user was added", model.getRegularUsers().stream().anyMatch(t -> t.getUsername().equals("test")
                && t.getPassword().equals("encoded")));
        verify(userService, times(1)).save(any(User.class));
    }

    @Test
    public void testAddUserEmpty() {
        final String username = "";
        final String password = "pass";

        controller.addUser(username, password);

        assertFalse("new user does not appear with new password and userName", model.getRegularUsers().stream().
                anyMatch(t -> t.getUsername().equals(username)
                        && t.getId().equals(1L) && t.getPassword().equals(password)));
        verify(userService, never()).update(anyLong(),
                argThat(userNamed(username, password, "user")), anyBoolean());
        verify(view).showMessage("Cannot be Empty!", JOptionPane.WARNING_MESSAGE);
    }

    @Test
    public void testAddDuplicateUser() {
        when(userService.save(any(User.class))).thenThrow(new DuplicateEntityException("duplicate"));

        controller.addUser("user2", "user2");

        assertEquals("duplicated user was not added", 1, model.getRegularUsers().stream().filter(t -> t.getUsername().equals("user2"))
                .collect(Collectors.toList()).size());
        verify(userService, times(1)).save(any(User.class));
    }

    @Test
    public void testGetUsernameById() {
        when(userService.findById(1L)).thenReturn(new User("a", "a", userRole));
        assertEquals("userName is a", "a", controller.getUsernameById(1L));
    }

    @Test
    public void testUpdateUserEmpty() {
        final String username = "";
        final String password = "pass";

        controller.updateUser(1L, username, password, true);

        assertFalse("new user does not appear with new password and userName", model.getRegularUsers().stream().
                anyMatch(t -> t.getUsername().equals(username)
                        && t.getId().equals(1L) && t.getPassword().equals(password)));
        verify(userService, never()).update(anyLong(),
                argThat(userNamed(username, password, "user")), anyBoolean());
        verify(view).showMessage("Cannot be Empty!", JOptionPane.WARNING_MESSAGE);
    }

    @Test
    public void testUpdateUserChangePassword() {
        final String username = "test";
        final String password = "encoded";

        when(userService.update(anyLong(), any(User.class), anyBoolean())).thenReturn(new User(username, password, userRole));

        controller.updateUser(1L, username, password, true);

        assertTrue("new user appears with new password", model.getRegularUsers().stream().anyMatch(t -> t.getUsername().equals(username)
                && t.getId().equals(1L) && t.getPassword().equals(password)));
        verify(userService, times(1)).update(anyLong(),
                argThat(userNamed(username, password, "user")), anyBoolean());
    }

    @Test
    public void testUpdateUserNotChangePassword() {
        final String username = "test";
        final String password = "newPassFake";
        when(userService.findById(1L)).thenReturn(new User("test", "test", userRole));
        when(userService.update(anyLong(), any(User.class), anyBoolean())).thenReturn(new User(username, "test", userRole));

        controller.updateUser(1L, username, password, false);

        assertTrue("old password remains", model.getRegularUsers().stream().anyMatch(t -> t.getUsername().equals(username)
                && t.getId().equals(1L) && t.getPassword().equals("test")));

        verify(userService, times(1)).update(anyLong(),
                argThat(userNamed(username, "test", "user")), anyBoolean());
    }

    @Test
    public void testUpdateUserDuplicate() {
        final String username = "test";
        final String password = "test22";
        doThrow(new DuplicateEntityException("duplicate")).when(userService).update(anyLong(), any(User.class), anyBoolean());
        when(userService.findById(1L)).thenReturn(new User(username, password, userRole));

        controller.updateUser(1L, username, password, false);

        assertFalse("user is not saved", model.getRegularUsers().stream().anyMatch(t -> t.getUsername().equals(username)
                && t.getId().equals(1L) && t.getPassword().equals(password)));
        verify(userService, times(1)).update(anyLong(),
                argThat(userNamed(username, password, "user")), anyBoolean());
    }

    @Test
    public void testGenerateReportTxt() {
        FileMaker maker = mock(FileMaker.class);
        when(reportGenerator.generateReport(any())).thenReturn(maker);

        controller.generateReportTxt("/file");

        verify(reportGenerator).generateReport(ReportType.TXT);
    }

    @Test
    public void testGenerateReportPdf() {
        FileMaker maker = mock(FileMaker.class);
        when(reportGenerator.generateReport(any())).thenReturn(maker);

        controller.generateReportPdf("/file");

        verify(reportGenerator).generateReport(ReportType.PDF);
    }

    @Test
    public void testIsRestrictedField() {
        assertFalse("id is not restricted field in user", controller.isRestrictedField("id"));
    }

    @Test
    public void testUpdate() {
        final Long userId = 1L;
        final String newUsername = "update";
        final String newPassword = "update";

        JTable jTable = mock(JTable.class);
        when(jTable.getSelectedRow()).thenReturn(1);
        when(view.getTable()).thenReturn(jTable);

        DefaultTableModel tableModel = mock(DefaultTableModel.class);
        when(view.getTableModel()).thenReturn(tableModel);
        when(tableModel.getValueAt(1, 0)).thenReturn(userId.toString());

        User user = new User();
        user.setUsername("user");

        when(userService.findById(userId)).thenReturn(user);
        when(userService.update(eq(userId), any(), anyBoolean())).thenReturn(new User(userId, newUsername,
                newPassword, userRole));

        doAnswer(invocation -> {
            Object[] arg = invocation.getArguments();
            ((JTextField) arg[2]).setText(newUsername);
            ((JTextField) arg[4]).setText(newPassword);
            ((JCheckBox) arg[5]).setSelected(true);
            return true;
        }).when(view).createDialog(anyVararg());

        controller.update();

        verify(userService).update(eq(1L), any(), anyBoolean());
        assertTrue("user was updated", model.getRegularUsers().stream().anyMatch(t -> t.getUsername().
                equals(newUsername) && t.getPassword().equals(newPassword)));
    }

    @Test
    public void testRequestListener() {
        UserRole userRole = new UserRole("user", new HashSet<>());
        Plant plant = new Plant("a", 1, 1, 1, 1);

        User user = new User("test", "test", userRole);
        UserPlantRequest request1 = new UserPlantRequest(plant, user, 1L);

        when(requestService.findNotAccepted()).thenReturn(Arrays.asList(request1));

        doAnswer(invocation -> {
            Object[] arg = invocation.getArguments();
            JScrollPane scrollPane = (JScrollPane) arg[0];
            JPanel panel = (JPanel) scrollPane.getViewport().getView();
            ((JRadioButton) panel.getComponents()[1]).setSelected(true);
            ((JRadioButton) panel.getComponents()[2]).setSelected(false);
            return true;
        }).when(view).createDialog(anyVararg());

        ActionListener listener = controller.new AddRequestListener();
        ActionEvent mockEvent = mock(ActionEvent.class);

        listener.actionPerformed(mockEvent);

        verify(requestService).acceptRequest(any());
    }

    @Test
    public void testDelete() {
        JTable jTable = mock(JTable.class);
        when(jTable.getSelectedRow()).thenReturn(1);
        when(view.getTable()).thenReturn(jTable);

        DefaultTableModel tableModel = mock(DefaultTableModel.class);
        when(view.getTableModel()).thenReturn(tableModel);
        when(tableModel.getValueAt(1, 0)).thenReturn("1");

        controller.delete();

        assertFalse("user was deleted", model.getRegularUsers().stream().anyMatch(t -> t.getId().equals(1L)));
        verify(userService, times(1)).delete(anyLong());
    }

    @Test
    public void testRefreshTable() {
        model.addUser(new User(10L, "refreshName", "b", userRole));
        List[] result = new Vector[2];//use array to overcome lambda expression violation

        doAnswer(invocation -> {
            Object[] arg = invocation.getArguments();
            DefaultTableModel tableModel = (DefaultTableModel) arg[0];
            result[1] = tableModel.getDataVector();

            return true;
        }).when(view).setTable(any(DefaultTableModel.class));

        controller.refreshTable();

        assertTrue("view was updated with the new user", result[1].stream().
                anyMatch(vector -> ((List) vector).stream().
                        anyMatch(t -> "refreshName".equals(t))));
    }

    @Test
    public void testRequestListenerDeny() {
        UserRole userRole = new UserRole("user", new HashSet<>());
        Plant plant = new Plant("a", 1, 1, 1, 1);

        User user = new User("test", "test", userRole);
        UserPlantRequest request1 = new UserPlantRequest(plant, user, 1L);

        when(requestService.findNotAccepted()).thenReturn(Arrays.asList(request1));

        doAnswer(invocation -> {
            Object[] arg = invocation.getArguments();
            JScrollPane scrollPane = (JScrollPane) arg[0];
            JPanel panel = (JPanel) scrollPane.getViewport().getView();
            ((JRadioButton) panel.getComponents()[1]).setSelected(false);
            ((JRadioButton) panel.getComponents()[2]).setSelected(true);
            return true;
        }).when(view).createDialog(anyVararg());

        ActionListener listener = controller.new AddRequestListener();
        ActionEvent mockEvent = mock(ActionEvent.class);

        listener.actionPerformed(mockEvent);

        verify(requestService).denyRequest(any());
    }

    @Test
    public void testRequestListenerEmpty() {

        when(requestService.findNotAccepted()).thenReturn(new ArrayList<>());

        ActionListener listener = controller.new AddRequestListener();
        ActionEvent mockEvent = mock(ActionEvent.class);

        listener.actionPerformed(mockEvent);

        verify(view).showMessage("No more requests!", JOptionPane.INFORMATION_MESSAGE);
    }

    @Test
    public void testUpdateListener() {
        final int invalidUpdateRow = -1;

        ActionListener listener = controller.new UpdateListener();
        ActionEvent mockEvent = mock(ActionEvent.class);

        JTable jTable = mock(JTable.class);
        when(jTable.getSelectedRow()).thenReturn(invalidUpdateRow);
        when(view.getTable()).thenReturn(jTable);

        listener.actionPerformed(mockEvent);

        verify(view).getTable();

        //this shouldn't be called because of invalidUpdateRow
        verify(view, never()).getTableModel();
    }

    @Test
    public void testDeleteListener() {
        final int invalidUpdateRow = -1;

        ActionListener listener = controller.new DeleteListener();
        ActionEvent mockEvent = mock(ActionEvent.class);

        JTable jTable = mock(JTable.class);
        when(jTable.getSelectedRow()).thenReturn(invalidUpdateRow);
        when(view.getTable()).thenReturn(jTable);

        listener.actionPerformed(mockEvent);

        verify(view).getTable();

        //this shouldn't be called because of invalidUpdateRow
        verify(view, never()).getTableModel();
    }

    @Test
    public void testInsertListener() {
        final String userName = "update";
        final String password = "update";

        ActionListener listener = controller.new InsertListener();
        ActionEvent mockEvent = mock(ActionEvent.class);

        setUpInsertUser(userName, password);

        listener.actionPerformed(mockEvent);

        verify(userService).save(any());
        assertTrue("user was inserted", model.getRegularUsers().stream().anyMatch(user -> user.getUsername().equals(userName)));
    }

    @Test
    public void testWindowAdapterListenerExit() {
        WindowAdapter listener = controller.new WindowAdapterListener();
        WindowEvent mockEvent = mock(WindowEvent.class);

        exit.expectSystemExit();
        listener.windowClosed(mockEvent);

        verify(authenticationManager).logout();
    }

    @Test
    public void testUpdateDataChange() {
        controller.update(new ChannelObservable(), Channel.LOGIN, "login#user1");

        ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
        //verify that tooltip was shown with live notification
        verify(view).showTooltip(msg.capture());
        assertTrue("msg contains the username", msg.getValue().contains("user1"));
    }

    private void loadGarden() {
        User user = new User("user", "user", userRole);
        User user2 = new User("user2", "user2", userRole);
        user.setId(1L);
        user2.setId(2L);

        Plot plot1 = new Plot(6, 4, 0, 0);
        Plot plot2 = new Plot(4, 2, 10, 10);
        plot1.setId(1L);
        plot2.setId(2L);

        Plant plant = new Plant("a", 1, 1, 1, 1);
        Plant plant2 = new Plant("b", 1, 1, 1, 5);
        Plant plant3 = new Plant("c", 1, 1, 2, 1);
        Plant plant4 = new Plant("d", 1, 1, 3, 0);
        Plant plant5 = new Plant("e", 1, 1, 4, 2);
        plant.setId(1L);
        plant2.setId(2L);
        plant3.setId(3L);
        plant4.setId(4L);
        plant5.setId(5L);

        PlantedPlant plantedPlant = new PlantedPlant(0, 0, plant);
        PlantedPlant plantedPlant2 = new PlantedPlant(2, 2, plant2);
        PlantedPlant plantedPlant3 = new PlantedPlant(3, 3, plant2);
        plantedPlant.setId(1L);
        plantedPlant2.setId(2L);
        plantedPlant3.setId(3L);

        when(userService.findRegularUsers()).thenReturn(Arrays.asList(user, user2));
        when(plotService.findAll()).thenReturn(Arrays.asList(plot1, plot2));
        when(plantService.findAll()).thenReturn(Arrays.asList(plant, plant2, plant3, plant4, plant5));
        when(plantedPlantService.findAll()).thenReturn(Arrays.asList(plantedPlant,
                plantedPlant2, plantedPlant3));
        when(roleService.findByName("user")).thenReturn(userRole);
    }

    private void setUpInsertUser(String userName, String password) {
        when(userService.save(any())).thenReturn(new User(10L, userName, password, userRole));

        doAnswer(invocation -> {
            Object[] arg = invocation.getArguments();
            ((JTextField) arg[2]).setText(userName);
            ((JTextField) arg[4]).setText(password);
            return true;
        }).when(view).createDialog(anyVararg());
    }
}