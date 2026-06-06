package gui;

import model.Admin;
import model.Equipment;
import model.EquipmentType;
import model.Hero;
import model.HeroType;
import model.MatchRecord;
import model.Person;
import model.Player;
import model.Role;
import model.Team;
import service.AuthenticationService;
import service.CombatSimulator;
import service.FileStorageService;
import service.GameDataManager;
import service.RankingService;
import service.RecommendationEngine;
import service.SearchService;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VisualizationFrame extends JFrame {
    private static final String DATA_FILE = "data/game-data.json";
    private static final String EDIT_LOGIN_CARD = "login";
    private static final String EDIT_PLAYER_CARD = "player";
    private static final String EDIT_ADMIN_CARD = "admin";
    private static final String LOGIN_FAILURE_MESSAGE = "登录失败，请检查用户名和密码。";

    private final GameDataManager manager;
    private final AuthenticationService authService;
    private final SearchService searchService;
    private final RankingService rankingService;
    private final CombatSimulator combatSimulator;
    private final JLabel loginStatus = new JLabel("未登录：可使用公开可视化");
    private final JButton loginButton = new JButton("登录");
    private final JButton logoutButton = new JButton("登出");
    private final JButton currentUserButton = new JButton("我的信息");
    private final CardLayout editCardLayout = new CardLayout();
    private final JPanel editPanel = new JPanel(editCardLayout);
    private final JLabel editLoginPrompt = new JLabel("登录以查看信息。", JLabel.CENTER);
    private final JTextField playerEditName = new JTextField(20);
    private final JTextField playerEditPassword = new JTextField(20);
    private final JComboBox<String> adminDataType = new JComboBox<>(new String[]{"管理员", "玩家", "英雄", "装备", "战队", "对战记录"});
    private final DefaultListModel<Object> adminEditListModel = new DefaultListModel<>();
    private final JList<Object> adminEditList = new JList<>(adminEditListModel);
    private final JPanel adminFormPanel = new JPanel(new GridBagLayout());
    private final Map<String, JTextField> adminFields = new LinkedHashMap<>();
    private String currentEditCard = EDIT_LOGIN_CARD;

    private final DefaultListModel<Player> playerListModel = new DefaultListModel<>();
    private final JList<Player> playerList = new JList<>(playerListModel);
    private final JTextArea playerDetails = readOnlyArea();

    private final DefaultListModel<Team> teamListModel = new DefaultListModel<>();
    private final JList<Team> teamList = new JList<>(teamListModel);
    private final JTextArea teamDetails = readOnlyArea();

    private final DefaultListModel<Hero> heroListModel = new DefaultListModel<>();
    private final JList<Hero> heroList = new JList<>(heroListModel);
    private final JTextArea heroDetails = readOnlyArea();

    private final JComboBox<String> rankingMode = new JComboBox<>(new String[]{"综合实力", "胜率", "等级", "对战次数"});
    private final JSpinner rankingLimit = new JSpinner(new SpinnerNumberModel(10, 1, 20, 1));
    private final DefaultTableModel rankingTableModel = new DefaultTableModel(
            new String[]{"名次", "玩家ID", "昵称", "等级", "胜率", "对战次数", "综合实力"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JComboBox<Team> simulationTeamA = new JComboBox<>();
    private final JComboBox<Team> simulationTeamB = new JComboBox<>();
    private final JTextArea simulationReport = readOnlyArea();

    public VisualizationFrame(GameDataManager manager) {
        super("王者荣耀信息管理系统");
        this.manager = manager;
        this.authService = new AuthenticationService(manager);
        RecommendationEngine recommendationEngine = new RecommendationEngine(manager);
        this.searchService = new SearchService(manager, recommendationEngine);
        this.rankingService = new RankingService(manager);
        this.combatSimulator = new CombatSimulator(manager);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 680));
        setLocationByPlatform(true);
        setContentPane(createContent());
        refreshRanking();
        updateLoginState();
        pack();
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 18, 18, 18));
        root.add(createHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("玩家查询", createPlayerPanel());
        tabs.addTab("战队概览", createTeamPanel());
        tabs.addTab("英雄详情", createHeroPanel());
        tabs.addTab("排行榜", createRankingPanel());
        tabs.addTab("对战模拟", createSimulationPanel());
        tabs.addTab("编辑信息", createEditPanel());
        root.add(tabs, BorderLayout.CENTER);
        return root;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 4));
        JLabel title = new JLabel("王者荣耀信息管理系统");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));

        JPanel titlePanel = new JPanel(new BorderLayout(0, 4));
        titlePanel.add(title, BorderLayout.CENTER);

        loginStatus.setName("loginStatus");
        loginButton.setName("loginButton");
        logoutButton.setName("logoutButton");
        currentUserButton.setName("currentUserButton");
        loginButton.addActionListener(event -> showLoginDialog());
        logoutButton.addActionListener(event -> logoutCurrentUser());
        currentUserButton.addActionListener(event -> showCurrentUser());

        JPanel accountPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 4, 2, 4);
        c.anchor = GridBagConstraints.EAST;
        accountPanel.add(loginStatus, c);
        c.gridy = 1;
        accountPanel.add(loginButton, c);
        c.gridx = 1;
        accountPanel.add(logoutButton, c);
        c.gridx = 2;
        accountPanel.add(currentUserButton, c);

        header.add(titlePanel, BorderLayout.CENTER);
        header.add(accountPanel, BorderLayout.EAST);
        return header;
    }

    private JPanel createPlayerPanel() {
        JTextField keyword = new JTextField();
        JButton searchButton = new JButton("查询玩家");
        JPanel searchBar = searchBar("输入玩家ID、用户名或昵称关键字", keyword, searchButton);
        searchButton.addActionListener(event -> searchPlayers(keyword.getText()));
        keyword.addActionListener(event -> searchPlayers(keyword.getText()));

        playerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        playerList.setCellRenderer((list, value, index, selected, focus) -> {
            JLabel label = (JLabel) new DefaultListCellRenderer()
                    .getListCellRendererComponent(list, value, index, selected, focus);
            label.setText(value.getDisplayName() + " (" + value.getId() + ") - 胜率 "
                    + String.format("%.2f%%", value.getWinRate()));
            return label;
        });
        playerList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                Player player = playerList.getSelectedValue();
                if (player != null) {
                    playerDetails.setText(searchService.playerDetails(player));
                    playerDetails.setCaretPosition(0);
                }
            }
        });

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(searchBar, BorderLayout.NORTH);
        panel.add(splitPane(new JScrollPane(playerList), new JScrollPane(playerDetails)), BorderLayout.CENTER);
        showAllPlayers();
        return panel;
    }

    private JPanel createTeamPanel() {
        JTextField keyword = new JTextField();
        JButton searchButton = new JButton("查询战队");
        JPanel searchBar = searchBar("输入战队ID或名称关键字", keyword, searchButton);
        searchButton.addActionListener(event -> searchTeams(keyword.getText()));
        keyword.addActionListener(event -> searchTeams(keyword.getText()));

        teamList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        teamList.setCellRenderer((list, value, index, selected, focus) -> {
            JLabel label = (JLabel) new DefaultListCellRenderer()
                    .getListCellRendererComponent(list, value, index, selected, focus);
            label.setText(value.getName() + " (" + value.getId() + ") - " + value.getMemberIds().size() + "人");
            return label;
        });
        teamList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                Team team = teamList.getSelectedValue();
                if (team != null) {
                    teamDetails.setText(searchService.teamOverview(team));
                    teamDetails.setCaretPosition(0);
                }
            }
        });

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(searchBar, BorderLayout.NORTH);
        panel.add(splitPane(new JScrollPane(teamList), new JScrollPane(teamDetails)), BorderLayout.CENTER);
        showAllTeams();
        return panel;
    }

    private JPanel createHeroPanel() {
        JTextField keyword = new JTextField();
        JButton searchButton = new JButton("查询英雄");
        JPanel searchBar = searchBar("输入英雄ID或名称关键字", keyword, searchButton);
        searchButton.addActionListener(event -> searchHeroes(keyword.getText()));
        keyword.addActionListener(event -> searchHeroes(keyword.getText()));

        heroList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        heroList.setCellRenderer((list, value, index, selected, focus) -> {
            JLabel label = (JLabel) new DefaultListCellRenderer()
                    .getListCellRendererComponent(list, value, index, selected, focus);
            label.setText(value.getName() + " (" + value.getId() + ") - " + value.getType());
            return label;
        });
        heroList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                Hero hero = heroList.getSelectedValue();
                if (hero != null) {
                    heroDetails.setText(searchService.heroDetails(hero));
                    heroDetails.setCaretPosition(0);
                }
            }
        });

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(searchBar, BorderLayout.NORTH);
        panel.add(splitPane(new JScrollPane(heroList), new JScrollPane(heroDetails)), BorderLayout.CENTER);
        showAllHeroes();
        return panel;
    }

    private JPanel createRankingPanel() {
        JButton refreshButton = new JButton("刷新排行榜");
        refreshButton.addActionListener(event -> refreshRanking());
        rankingMode.addActionListener(event -> refreshRanking());
        rankingLimit.addChangeListener(event -> refreshRanking());

        JPanel controls = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        controls.add(new JLabel("排序方式:"), c);
        c.gridx = 1;
        controls.add(rankingMode, c);
        c.gridx = 2;
        controls.add(new JLabel("显示数量:"), c);
        c.gridx = 3;
        controls.add(rankingLimit, c);
        c.gridx = 4;
        c.weightx = 1;
        controls.add(refreshButton, c);

        JTable table = new JTable(rankingTableModel);
        table.setAutoCreateRowSorter(false);
        table.setRowHeight(26);
        table.getTableHeader().setReorderingAllowed(false);

        JTextArea rule = readOnlyArea();
        rule.setText("综合实力=100*(0.40*贝叶斯胜率+0.25*等级归一化+0.25*对战量归一化+0.10*英雄多样性)。同分时按贝叶斯胜率、对战次数、等级、玩家ID排序。");
        rule.setRows(3);

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(controls, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(rule, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createSimulationPanel() {
        JButton simulateButton = new JButton("开始模拟");
        simulateButton.addActionListener(event -> runCombatSimulation());

        DefaultListCellRenderer renderer = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Team team) {
                    label.setText(team.getName() + " (" + team.getId() + ")");
                }
                return label;
            }
        };
        simulationTeamA.setRenderer(renderer);
        simulationTeamB.setRenderer(renderer);

        JPanel controls = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        controls.add(new JLabel("队伍A:"), c);
        c.gridx = 1;
        controls.add(simulationTeamA, c);
        c.gridx = 2;
        controls.add(new JLabel("队伍B:"), c);
        c.gridx = 3;
        controls.add(simulationTeamB, c);
        c.gridx = 4;
        c.weightx = 1;
        controls.add(simulateButton, c);

        simulationReport.setRows(18);
        simulationReport.setText("选择两支战队后开始模拟。模拟只生成预览报告，不写入对战记录，不影响排行榜和历史数据。");
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(controls, BorderLayout.NORTH);
        panel.add(new JScrollPane(simulationReport), BorderLayout.CENTER);
        refreshSimulationTeams();
        return panel;
    }

    private JPanel createEditPanel() {
        editLoginPrompt.setFont(editLoginPrompt.getFont().deriveFont(Font.BOLD, 20f));
        editPanel.add(centeredPanel(editLoginPrompt), EDIT_LOGIN_CARD);
        editPanel.add(createPlayerEditPanel(), EDIT_PLAYER_CARD);
        editPanel.add(createAdminEditPanel(), EDIT_ADMIN_CARD);
        return editPanel;
    }

    private JPanel createPlayerEditPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        JTextArea note = readOnlyArea();
        note.setText("玩家用户只能查看通用数据，并在此编辑自己的昵称和密码。玩家ID、用户名、等级、胜负和战队信息由管理员维护。");
        note.setRows(3);

        JPanel form = new JPanel(new GridBagLayout());
        addFormRow(form, 0, "昵称", playerEditName);
        addFormRow(form, 1, "新密码", playerEditPassword);

        JButton save = new JButton("保存我的信息");
        save.addActionListener(event -> saveCurrentPlayerInfo());

        panel.add(note, BorderLayout.NORTH);
        panel.add(form, BorderLayout.CENTER);
        panel.add(save, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createAdminEditPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        top.add(new JLabel("数据类型:"), c);
        c.gridx = 1;
        top.add(adminDataType, c);

        JButton newButton = new JButton("新建");
        JButton saveButton = new JButton("保存");
        JButton deleteButton = new JButton("删除");
        JButton saveJsonButton = new JButton("保存JSON");
        c.gridx = 2;
        top.add(newButton, c);
        c.gridx = 3;
        top.add(saveButton, c);
        c.gridx = 4;
        top.add(deleteButton, c);
        c.gridx = 5;
        top.add(saveJsonButton, c);

        adminEditList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        adminEditList.setCellRenderer((list, value, index, selected, focus) -> {
            JLabel label = (JLabel) new DefaultListCellRenderer()
                    .getListCellRendererComponent(list, value, index, selected, focus);
            label.setText(adminListText(value));
            return label;
        });
        adminEditList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                loadSelectedAdminItem();
            }
        });
        adminDataType.addActionListener(event -> refreshAdminEditorList());
        newButton.addActionListener(event -> prepareNewAdminForm());
        saveButton.addActionListener(event -> saveAdminForm());
        deleteButton.addActionListener(event -> deleteSelectedAdminItem());
        saveJsonButton.addActionListener(event -> saveJsonFromGui());

        JSplitPane splitPane = splitPane(new JScrollPane(adminEditList), new JScrollPane(adminFormPanel));
        panel.add(top, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private void searchPlayers(String rawKeyword) {
        String keyword = rawKeyword.trim();
        if (keyword.isEmpty()) {
            showAllPlayers();
            return;
        }
        List<Player> exact = searchService.searchExactPlayers(keyword);
        List<Player> players = exact.size() == 1 ? exact : searchService.search(keyword);
        playerListModel.clear();
        players.forEach(playerListModel::addElement);
        selectFirstOrShowEmpty(playerList, playerDetails, "未找到玩家。管理员和教练超级账户不会进入公开玩家查询。");
    }

    private void searchTeams(String rawKeyword) {
        String keyword = rawKeyword.trim();
        if (keyword.isEmpty()) {
            showAllTeams();
            return;
        }
        teamListModel.clear();
        searchService.searchTeams(keyword).forEach(teamListModel::addElement);
        selectFirstOrShowEmpty(teamList, teamDetails, "未找到战队。");
    }

    private void searchHeroes(String rawKeyword) {
        String keyword = rawKeyword.trim();
        if (keyword.isEmpty()) {
            showAllHeroes();
            return;
        }
        heroListModel.clear();
        searchService.searchHeroes(keyword).forEach(heroListModel::addElement);
        selectFirstOrShowEmpty(heroList, heroDetails, "未找到英雄。");
    }

    private void showAllPlayers() {
        playerListModel.clear();
        manager.getPlayers().forEach(playerListModel::addElement);
        selectFirstOrShowEmpty(playerList, playerDetails, "暂无玩家数据。");
    }

    private void showAllTeams() {
        teamListModel.clear();
        manager.getTeams().forEach(teamListModel::addElement);
        selectFirstOrShowEmpty(teamList, teamDetails, "暂无战队数据。");
    }

    private void showAllHeroes() {
        heroListModel.clear();
        manager.getHeroes().forEach(heroListModel::addElement);
        selectFirstOrShowEmpty(heroList, heroDetails, "暂无英雄数据。");
    }

    private void refreshRanking() {
        int limit = (Integer) rankingLimit.getValue();
        List<Player> players = switch ((String) rankingMode.getSelectedItem()) {
            case "综合实力" -> rankingService.topByComprehensiveScore(limit);
            case "等级" -> rankingService.topByLevel(limit);
            case "对战次数" -> rankingService.topByMatchCount(limit);
            default -> rankingService.topByWinRate(limit);
        };

        rankingTableModel.setRowCount(0);
        int rank = 1;
        for (Player player : players) {
            rankingTableModel.addRow(new Object[]{
                    rank++,
                    player.getId(),
                    player.getDisplayName(),
                    player.getLevel(),
                    String.format("%.2f%%", player.getWinRate()),
                    player.getTotalMatches(),
                    String.format("%.2f", rankingService.playerComprehensiveScore(player))
            });
        }
    }

    public boolean loginUser(String username, String password) {
        return performLogin(username, password, false);
    }

    public void logoutCurrentUser() {
        authService.logout();
        updateLoginState();
    }

    public String getLoginStatusText() {
        return loginStatus.getText();
    }

    private void showLoginDialog() {
        JTextField username = new JTextField(18);
        JPasswordField password = new JPasswordField(18);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("用户名:"), c);
        c.gridx = 1;
        panel.add(username, c);
        c.gridx = 0;
        c.gridy = 1;
        panel.add(new JLabel("密码:"), c);
        c.gridx = 1;
        panel.add(password, c);

        int result = JOptionPane.showConfirmDialog(this, panel, "用户登录", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            performLogin(username.getText(), new String(password.getPassword()), true);
        }
    }

    private boolean performLogin(String username, String password, boolean showMessage) {
        authService.logout();
        Person user = authService.login(username.trim(), password);
        if (user == null) {
            if (showMessage) {
                JOptionPane.showMessageDialog(this, LOGIN_FAILURE_MESSAGE, "登录失败", JOptionPane.WARNING_MESSAGE);
            }
            updateLoginState();
            return false;
        }

        updateLoginState();
        if (user instanceof Player player) {
            showAllPlayers();
            playerList.setSelectedValue(player, true);
        }
        if (showMessage) {
            JOptionPane.showMessageDialog(this, "登录成功，欢迎 " + user.getDisplayName(), "登录成功", JOptionPane.INFORMATION_MESSAGE);
        }
        return true;
    }

    private void showCurrentUser() {
        Person user = authService.getCurrentUser();
        if (user == null) {
            JOptionPane.showMessageDialog(this, "当前未登录。", "我的信息", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (user instanceof Player player) {
            showAllPlayers();
            playerList.setSelectedValue(player, true);
            JOptionPane.showMessageDialog(this, searchService.playerDetails(player), "我的信息", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog(this, user.generateReport(), "我的信息", JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateLoginState() {
        Person user = authService.getCurrentUser();
        if (user == null) {
            loginStatus.setText("未登录：可使用公开可视化");
            loginButton.setText("登录");
            logoutButton.setEnabled(false);
            currentUserButton.setEnabled(false);
            refreshEditPanel();
            return;
        }
        String role = user.getRole() == Role.ADMIN ? "管理员/教练" : "玩家";
        loginStatus.setText("已登录：" + user.getDisplayName() + "（" + role + "）");
        loginButton.setText("切换登录");
        logoutButton.setEnabled(true);
        currentUserButton.setEnabled(true);
        refreshEditPanel();
    }

    public String getEditPromptText() {
        return editLoginPrompt.getText();
    }

    public String getCurrentEditCardForTest() {
        return currentEditCard;
    }

    private void refreshEditPanel() {
        Person user = authService.getCurrentUser();
        if (user == null) {
            showEditCard(EDIT_LOGIN_CARD);
            return;
        }
        if (user instanceof Player player) {
            playerEditName.setText(player.getDisplayName());
            playerEditPassword.setText(player.getPassword());
            showEditCard(EDIT_PLAYER_CARD);
            return;
        }
        refreshAdminEditorList();
        showEditCard(EDIT_ADMIN_CARD);
    }

    private void showEditCard(String cardName) {
        currentEditCard = cardName;
        editCardLayout.show(editPanel, cardName);
    }

    private void saveCurrentPlayerInfo() {
        Person user = authService.getCurrentUser();
        if (!(user instanceof Player player)) {
            JOptionPane.showMessageDialog(this, "请先以玩家身份登录。", "无法保存", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String name = playerEditName.getText().trim();
        String password = playerEditPassword.getText();
        if (name.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "昵称和密码不能为空。", "无法保存", JOptionPane.WARNING_MESSAGE);
            return;
        }
        player.setDisplayName(name);
        player.setPassword(password);
        refreshAllViews();
        playerList.setSelectedValue(player, true);
        playerDetails.setText(searchService.playerDetails(player));
        playerDetails.setCaretPosition(0);
        updateLoginState();
        JOptionPane.showMessageDialog(this, "玩家基本信息已更新。", "保存成功", JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshAdminEditorList() {
        if (authService.getCurrentUser() == null || authService.getCurrentUser().getRole() != Role.ADMIN) {
            return;
        }
        adminEditListModel.clear();
        for (Object item : currentAdminItems()) {
            adminEditListModel.addElement(item);
        }
        buildAdminForm();
        if (!adminEditListModel.isEmpty()) {
            adminEditList.setSelectedIndex(0);
        } else {
            clearAdminForm();
        }
    }

    private Collection<?> currentAdminItems() {
        return switch (currentAdminType()) {
            case "管理员" -> manager.getAdmins();
            case "玩家" -> manager.getPlayers();
            case "英雄" -> manager.getHeroes();
            case "装备" -> manager.getEquipment();
            case "战队" -> manager.getTeams();
            case "对战记录" -> manager.getMatches();
            default -> List.of();
        };
    }

    private String currentAdminType() {
        return String.valueOf(adminDataType.getSelectedItem());
    }

    private void buildAdminForm() {
        adminFields.clear();
        adminFormPanel.removeAll();
        String[][] fields = switch (currentAdminType()) {
            case "管理员" -> new String[][]{{"id", "管理员ID"}, {"username", "用户名"}, {"password", "密码"}, {"displayName", "显示名"}};
            case "玩家" -> new String[][]{{"id", "玩家ID"}, {"username", "用户名*"}, {"password", "密码*"}, {"displayName", "昵称*"}, {"level", "等级"}, {"wins", "胜场"}, {"losses", "负场"}, {"teamId", "战队ID"}, {"heroIds", "英雄ID(逗号)"}};
            case "英雄" -> new String[][]{{"id", "英雄ID"}, {"name", "名称*"}, {"type", "类型*"}, {"attack", "攻击*"}, {"defense", "防御*"}, {"skillPower", "技能*"}, {"equipmentIds", "装备ID(逗号)*"}};
            case "装备" -> new String[][]{{"id", "装备ID"}, {"name", "名称*"}, {"type", "类型*"}, {"rating", "评分*"}, {"description", "属性描述"}};
            case "战队" -> new String[][]{{"id", "战队ID"}, {"name", "名称*"}, {"memberIds", "成员ID(逗号)"}};
            case "对战记录" -> new String[][]{{"id", "对战ID"}, {"date", "日期yyyy-MM-dd*"}, {"teamAId", "队伍A*"}, {"teamBId", "队伍B*"}, {"winnerTeamId", "胜者战队*"}, {"choices", "英雄选择(P001:H003,...)*"}};
            default -> new String[0][0];
        };
        for (int i = 0; i < fields.length; i++) {
            JTextField field = new JTextField(28);
            if ("id".equals(fields[i][0]) && isAutoIdType(currentAdminType())) {
                field.setEditable(false);
                field.setFocusable(false);
            }
            adminFields.put(fields[i][0], field);
            addFormRow(adminFormPanel, i, fields[i][1], field);
        }
        adminFormPanel.revalidate();
        adminFormPanel.repaint();
    }

    private void loadSelectedAdminItem() {
        Object item = adminEditList.getSelectedValue();
        if (item == null) {
            return;
        }
        clearAdminFields();
        if (item instanceof Admin admin) {
            setField("id", admin.getId());
            setField("username", admin.getUsername());
            setField("password", admin.getPassword());
            setField("displayName", admin.getDisplayName());
        } else if (item instanceof Player player) {
            setField("id", player.getId());
            setField("username", player.getUsername());
            setField("password", player.getPassword());
            setField("displayName", player.getDisplayName());
            setField("level", String.valueOf(player.getLevel()));
            setField("wins", String.valueOf(player.getWins()));
            setField("losses", String.valueOf(player.getLosses()));
            setField("teamId", player.getTeamId());
            setField("heroIds", String.join(",", player.getHeroIds()));
        } else if (item instanceof Hero hero) {
            setField("id", hero.getId());
            setField("name", hero.getName());
            setField("type", hero.getType().name());
            setField("attack", String.valueOf(hero.getAttack()));
            setField("defense", String.valueOf(hero.getDefense()));
            setField("skillPower", String.valueOf(hero.getSkillPower()));
            setField("equipmentIds", String.join(",", hero.getCompatibleEquipmentIds()));
        } else if (item instanceof Equipment equipment) {
            setField("id", equipment.getId());
            setField("name", equipment.getName());
            setField("type", equipment.getType().name());
            setField("rating", String.valueOf(equipment.getRating()));
            setField("description", equipment.getAttributeDescription());
        } else if (item instanceof Team team) {
            setField("id", team.getId());
            setField("name", team.getName());
            setField("memberIds", String.join(",", team.getMemberIds()));
        } else if (item instanceof MatchRecord match) {
            setField("id", match.getId());
            setField("date", match.getDate().toString());
            setField("teamAId", match.getTeamAId());
            setField("teamBId", match.getTeamBId());
            setField("winnerTeamId", match.getWinnerTeamId());
            setField("choices", formatChoices(match.getPlayerHeroChoices()));
        }
    }

    private void clearAdminForm() {
        clearAdminFields();
        adminEditList.clearSelection();
        applyNewAdminDefaults();
    }

    private void clearAdminFields() {
        adminFields.values().forEach(field -> field.setText(""));
    }

    private void prepareNewAdminForm() {
        clearAdminForm();
    }

    private void saveAdminForm() {
        try {
            String type = currentAdminType();
            String id = field("id");
            if (id.isEmpty()) {
                throw new IllegalArgumentException("ID不能为空。");
            }
            boolean saved = switch (type) {
                case "管理员" -> saveAdmin(id);
                case "玩家" -> savePlayer(id);
                case "英雄" -> saveHero(id);
                case "装备" -> saveEquipment(id);
                case "战队" -> saveTeam(id);
                case "对战记录" -> saveMatch(id);
                default -> throw new IllegalArgumentException("未知数据类型。");
            };
            if (!saved) {
                return;
            }
            refreshAllViews();
            refreshAdminEditorList();
            JOptionPane.showMessageDialog(this, type + "已保存。", "保存成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "保存失败: " + ex.getMessage(), "保存失败", JOptionPane.WARNING_MESSAGE);
        }
    }

    private boolean saveAdmin(String id) {
        id = normalizeRequiredCode(id, "A", "管理员ID");
        Admin admin = manager.findAdminById(id).orElse(null);
        if (admin == null) {
            manager.addAdmin(new Admin(id, field("username"), field("password"), field("displayName")));
            return true;
        }
        admin.setUsername(field("username"));
        admin.setPassword(field("password"));
        admin.setDisplayName(field("displayName"));
        updateLoginState();
        return true;
    }

    private boolean savePlayer(String id) {
        id = normalizeRequiredCode(id, "P", "玩家ID");
        ensureRequiredFields(List.of("username", "password", "displayName"), Map.of(
                "username", "用户名",
                "password", "密码",
                "displayName", "昵称"));
        String teamId = normalizeOptionalCode(field("teamId"), "T");
        List<String> heroIds = normalizeCodeList(field("heroIds"), "H");
        Player player = manager.findPlayerById(id).orElse(null);
        if (player != null && !isEditingSelectedId(id, Player.class)
                && !confirmOverwrite("玩家", id, "该玩家原有用户名、密码、昵称和战绩会被新表单覆盖。")) {
            return false;
        }
        if (player == null) {
            player = new Player(id, field("username"), field("password"), field("displayName"),
                    intField("level", 1), intField("wins", 0), intField("losses", 0), teamId);
            player.replaceHeroIds(heroIds);
            manager.addPlayer(player);
            return true;
        }
        player.setUsername(field("username"));
        player.setPassword(field("password"));
        player.setDisplayName(field("displayName"));
        player.setLevel(intField("level", 1));
        player.setWins(intField("wins", 0));
        player.setLosses(intField("losses", 0));
        player.setTeamId(teamId);
        player.replaceHeroIds(heroIds);
        manager.replacePlayer(player);
        return true;
    }

    private boolean saveHero(String id) {
        id = normalizeRequiredCode(id, "H", "英雄ID");
        ensureRequiredFields(List.of("name", "type", "attack", "defense", "skillPower", "equipmentIds"), Map.of(
                "name", "名称",
                "type", "类型",
                "attack", "攻击",
                "defense", "防御",
                "skillPower", "技能",
                "equipmentIds", "装备ID"));
        Hero candidate = new Hero(id, field("name"), HeroType.valueOf(field("type").toUpperCase()),
                intField("attack", 0), intField("defense", 0), intField("skillPower", 0));
        candidate.replaceCompatibleEquipmentIds(normalizeCodeList(field("equipmentIds"), "E"));
        manager.validateHeroReferences(candidate);
        Hero hero = manager.findHeroById(id).orElse(null);
        if (hero != null && !isEditingSelectedId(id, Hero.class)
                && !confirmOverwrite("英雄", id, "该英雄原有名称、类型、属性和兼容装备会被新表单覆盖。")) {
            return false;
        }
        if (hero == null) {
            manager.addHero(candidate);
            return true;
        }
        hero.setName(candidate.getName());
        hero.setType(candidate.getType());
        hero.setAttack(candidate.getAttack());
        hero.setDefense(candidate.getDefense());
        hero.setSkillPower(candidate.getSkillPower());
        hero.replaceCompatibleEquipmentIds(candidate.getCompatibleEquipmentIds());
        return true;
    }

    private boolean saveEquipment(String id) {
        id = normalizeRequiredCode(id, "E", "装备ID");
        ensureRequiredFields(List.of("name", "type", "rating"), Map.of(
                "name", "名称",
                "type", "类型",
                "rating", "评分"));
        Equipment equipment = manager.findEquipmentById(id).orElse(null);
        if (equipment != null && !isEditingSelectedId(id, Equipment.class)
                && !confirmOverwrite("装备", id, "该装备原有名称、类型、评分和属性描述会被新表单覆盖。")) {
            return false;
        }
        if (equipment == null) {
            manager.addEquipment(new Equipment(id, field("name"), EquipmentType.valueOf(field("type").toUpperCase()),
                    doubleField("rating", 0.0), field("description")));
            return true;
        }
        equipment.setName(field("name"));
        equipment.setType(EquipmentType.valueOf(field("type").toUpperCase()));
        equipment.setRating(doubleField("rating", 0.0));
        equipment.setAttributeDescription(field("description"));
        return true;
    }

    private boolean saveTeam(String id) {
        id = normalizeRequiredCode(id, "T", "战队ID");
        ensureRequiredFields(List.of("name"), Map.of("name", "名称"));
        Team candidate = new Team(id, field("name"));
        candidate.replaceMemberIds(normalizeCodeList(field("memberIds"), "P"));
        manager.validateTeamMemberReferences(candidate);
        Team team = manager.findTeamById(id).orElse(null);
        if (team != null && !isEditingSelectedId(id, Team.class)
                && !confirmOverwrite("战队", id, "该战队原有名称和成员列表会被新表单覆盖。")) {
            return false;
        }
        if (team == null) {
            manager.addTeam(candidate);
            return true;
        }
        team.setName(candidate.getName());
        team.replaceMemberIds(candidate.getMemberIds());
        return true;
    }

    private boolean saveMatch(String id) {
        id = normalizeRequiredCode(id, "M", "对战ID");
        ensureRequiredFields(List.of("date", "teamAId", "teamBId", "winnerTeamId", "choices"), Map.of(
                "date", "日期",
                "teamAId", "队伍A",
                "teamBId", "队伍B",
                "winnerTeamId", "胜者战队",
                "choices", "英雄选择"));
        MatchRecord candidate = new MatchRecord(id, parseDate(field("date")), normalizeRequiredCode(field("teamAId"), "T", "队伍A"),
                normalizeRequiredCode(field("teamBId"), "T", "队伍B"), normalizeRequiredCode(field("winnerTeamId"), "T", "胜者战队"));
        candidate.replacePlayerHeroChoices(parseChoices(field("choices")));
        manager.validateMatchReferences(candidate);
        String matchId = id;
        MatchRecord match = manager.getMatches().stream()
                .filter(item -> item.getId().equals(matchId))
                .findFirst()
                .orElse(null);
        if (match != null && !isEditingSelectedId(id, MatchRecord.class)
                && !confirmOverwrite("对战记录", id, "该对战记录原有日期、队伍、胜者和英雄选择会被新表单覆盖。")) {
            return false;
        }
        if (match == null) {
            manager.addMatch(candidate);
            return true;
        }
        match.setDate(candidate.getDate());
        match.setTeamAId(candidate.getTeamAId());
        match.setTeamBId(candidate.getTeamBId());
        match.setWinnerTeamId(candidate.getWinnerTeamId());
        match.replacePlayerHeroChoices(candidate.getPlayerHeroChoices());
        return true;
    }

    private void deleteSelectedAdminItem() {
        Object item = adminEditList.getSelectedValue();
        if (item == null) {
            JOptionPane.showMessageDialog(this, "请先选择要删除的数据。", "无法删除", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, deleteConfirmationMessage(item), "确认删除", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        boolean deleted = switch (currentAdminType()) {
            case "管理员" -> manager.deleteAdmin(((Admin) item).getId());
            case "玩家" -> manager.deletePlayer(((Player) item).getId());
            case "英雄" -> manager.deleteHero(((Hero) item).getId());
            case "装备" -> manager.deleteEquipment(((Equipment) item).getId());
            case "战队" -> manager.deleteTeam(((Team) item).getId());
            case "对战记录" -> manager.deleteMatch(((MatchRecord) item).getId());
            default -> false;
        };
        if (deleted && item instanceof Person person && authService.getCurrentUser() != null
                && authService.getCurrentUser().getId().equals(person.getId())) {
            authService.logout();
        }
        refreshAllViews();
        updateLoginState();
        refreshAdminEditorList();
    }

    private void saveJsonFromGui() {
        try {
            new FileStorageService().save(manager, DATA_FILE);
            JOptionPane.showMessageDialog(this, "数据已保存到 " + DATA_FILE, "保存成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "保存失败: " + ex.getMessage(), "保存失败", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void refreshAllViews() {
        showAllPlayers();
        showAllTeams();
        showAllHeroes();
        refreshRanking();
        refreshSimulationTeams();
    }

    private void refreshSimulationTeams() {
        Team selectedA = (Team) simulationTeamA.getSelectedItem();
        Team selectedB = (Team) simulationTeamB.getSelectedItem();
        simulationTeamA.removeAllItems();
        simulationTeamB.removeAllItems();
        for (Team team : manager.getTeams()) {
            simulationTeamA.addItem(team);
            simulationTeamB.addItem(team);
        }
        restoreTeamSelection(simulationTeamA, selectedA);
        restoreTeamSelection(simulationTeamB, selectedB);
        if (simulationTeamB.getItemCount() > 1 && simulationTeamA.getSelectedIndex() == simulationTeamB.getSelectedIndex()) {
            simulationTeamB.setSelectedIndex(1);
        }
    }

    private void restoreTeamSelection(JComboBox<Team> comboBox, Team selected) {
        if (selected == null) {
            return;
        }
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            if (comboBox.getItemAt(i).getId().equals(selected.getId())) {
                comboBox.setSelectedIndex(i);
                return;
            }
        }
    }

    private void runCombatSimulation() {
        Team teamA = (Team) simulationTeamA.getSelectedItem();
        Team teamB = (Team) simulationTeamB.getSelectedItem();
        if (teamA == null || teamB == null) {
            simulationReport.setText("模拟失败: 请先选择两支战队。");
            return;
        }
        try {
            simulationReport.setText(combatSimulator.simulate(teamA.getId(), teamB.getId()).formatReport());
            simulationReport.setCaretPosition(0);
        } catch (IllegalArgumentException ex) {
            simulationReport.setText("模拟失败: " + ex.getMessage());
            simulationReport.setCaretPosition(0);
        }
    }

    private String adminListText(Object item) {
        if (item instanceof Admin admin) {
            return admin.getDisplayName() + " (" + admin.getId() + ")";
        }
        if (item instanceof Player player) {
            return player.getDisplayName() + " (" + player.getId() + ")";
        }
        if (item instanceof Hero hero) {
            return hero.getName() + " (" + hero.getId() + ")";
        }
        if (item instanceof Equipment equipment) {
            return equipment.getName() + " (" + equipment.getId() + ")";
        }
        if (item instanceof Team team) {
            return team.getName() + " (" + team.getId() + ")";
        }
        if (item instanceof MatchRecord match) {
            return match.getId() + " " + match.getDate();
        }
        return String.valueOf(item);
    }

    private String deleteConfirmationMessage(Object item) {
        if (item instanceof Player) {
            return "确认删除玩家 " + adminListText(item) + " 吗？\n删除后会同步从战队成员列表中移除。";
        }
        if (item instanceof Hero) {
            return "确认删除英雄 " + adminListText(item) + " 吗？\n删除后会同步从玩家英雄列表中移除。";
        }
        if (item instanceof Equipment) {
            return "确认删除装备 " + adminListText(item) + " 吗？\n删除后会同步从英雄兼容装备中移除。";
        }
        if (item instanceof Team) {
            return "确认删除战队 " + adminListText(item) + " 吗？\n删除后相关玩家的战队ID会被清空。";
        }
        return "确认删除 " + adminListText(item) + " 吗？";
    }

    private void applyNewAdminDefaults() {
        String type = currentAdminType();
        if ("玩家".equals(type)) {
            String id = nextCode(manager.getPlayers().stream().map(Player::getId).toList(), "P");
            setField("id", id);
            setField("username", id);
        } else if ("英雄".equals(type)) {
            setField("id", nextCode(manager.getHeroes().stream().map(Hero::getId).toList(), "H"));
        } else if ("装备".equals(type)) {
            setField("id", nextCode(manager.getEquipment().stream().map(Equipment::getId).toList(), "E"));
        } else if ("战队".equals(type)) {
            setField("id", nextCode(manager.getTeams().stream().map(Team::getId).toList(), "T"));
        } else if ("对战记录".equals(type)) {
            setField("id", nextCode(manager.getMatches().stream().map(MatchRecord::getId).toList(), "M"));
        }
    }

    private boolean isAutoIdType(String type) {
        return "玩家".equals(type) || "英雄".equals(type) || "装备".equals(type)
                || "战队".equals(type) || "对战记录".equals(type);
    }

    private boolean confirmOverwrite(String label, String id, String detail) {
        int confirm = JOptionPane.showConfirmDialog(this,
                label + "ID " + id + " 已存在，是否覆盖原有信息？\n" + detail,
                "确认覆盖", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return confirm == JOptionPane.YES_OPTION;
    }

    private boolean isEditingSelectedId(String id, Class<?> itemType) {
        Object selected = adminEditList.getSelectedValue();
        if (!itemType.isInstance(selected)) {
            return false;
        }
        if (selected instanceof Person person) {
            return person.getId().equals(id);
        }
        if (selected instanceof Equipment equipment) {
            return equipment.getId().equals(id);
        }
        if (selected instanceof Hero hero) {
            return hero.getId().equals(id);
        }
        if (selected instanceof Team team) {
            return team.getId().equals(id);
        }
        if (selected instanceof MatchRecord match) {
            return match.getId().equals(id);
        }
        return false;
    }

    private void ensureRequiredFields(List<String> keys, Map<String, String> labels) {
        List<String> missing = new ArrayList<>();
        for (String key : keys) {
            if (field(key).isBlank()) {
                missing.add(labels.getOrDefault(key, key));
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("请补充必填项: " + String.join("、", missing) + "。");
        }
    }

    private String normalizeOptionalCode(String raw, String prefix) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return normalizeCode(raw, prefix);
    }

    private String normalizeRequiredCode(String raw, String prefix, String label) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(label + "不能为空。");
        }
        return normalizeCode(raw, prefix);
    }

    private List<String> normalizeCodeList(String raw, String prefix) {
        return csv(raw).stream()
                .map(value -> normalizeCode(value, prefix))
                .toList();
    }

    private String normalizeCode(String raw, String prefix) {
        String value = raw.trim().toUpperCase(Locale.ROOT);
        String upperPrefix = prefix.toUpperCase(Locale.ROOT);
        if (value.startsWith(upperPrefix)) {
            value = value.substring(upperPrefix.length());
        }
        if (!value.matches("\\d+")) {
            throw new IllegalArgumentException(upperPrefix + "类ID必须由前缀和数字组成，例如 " + upperPrefix + "001。");
        }
        int number = Integer.parseInt(value);
        return upperPrefix + String.format("%03d", number);
    }

    private String nextCode(Collection<String> ids, String prefix) {
        int max = ids.stream()
                .mapToInt(id -> numericSuffix(id, prefix))
                .max()
                .orElse(0);
        return prefix + String.format("%03d", max + 1);
    }

    private int numericSuffix(String id, String prefix) {
        if (id == null) {
            return 0;
        }
        String value = id.trim().toUpperCase(Locale.ROOT);
        String upperPrefix = prefix.toUpperCase(Locale.ROOT);
        if (value.startsWith(upperPrefix)) {
            value = value.substring(upperPrefix.length());
        }
        if (!value.matches("\\d+")) {
            return 0;
        }
        return Integer.parseInt(value);
    }

    private String field(String key) {
        JTextField textField = adminFields.get(key);
        return textField == null ? "" : textField.getText().trim();
    }

    private void setField(String key, String value) {
        JTextField textField = adminFields.get(key);
        if (textField != null) {
            textField.setText(value == null ? "" : value);
        }
    }

    private int intField(String key, int defaultValue) {
        String value = field(key);
        return value.isEmpty() ? defaultValue : Integer.parseInt(value);
    }

    private double doubleField(String key, double defaultValue) {
        String value = field(key);
        return value.isEmpty() ? defaultValue : Double.parseDouble(value);
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("日期格式无效，请使用 yyyy-MM-dd。");
        }
    }

    private List<String> csv(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return List.of(raw.split(",")).stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    private Map<String, String> parseChoices(String raw) {
        Map<String, String> choices = new LinkedHashMap<>();
        for (String pair : csv(raw)) {
            String[] parts = pair.split(":", 2);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                throw new IllegalArgumentException("英雄选择格式应为 P001:H003,P002:H011。");
            }
            choices.put(normalizeCode(parts[0], "P"), normalizeCode(parts[1], "H"));
        }
        return choices;
    }

    private String formatChoices(Map<String, String> choices) {
        return choices.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(java.util.stream.Collectors.joining(","));
    }

    private JPanel searchBar(String labelText, JTextField keyword, JButton button) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 8);
        c.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(labelText), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(keyword, c);
        c.gridx = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        panel.add(button, c);
        return panel;
    }

    private JPanel centeredPanel(Component component) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.add(component);
        return panel;
    }

    private void addFormRow(JPanel panel, int row, String labelText, JTextField field) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 6, 5, 6);
        c.gridx = 0;
        c.gridy = row;
        c.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(labelText + ":"), c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        panel.add(field, c);
    }

    private JSplitPane splitPane(Component left, Component right) {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        splitPane.setResizeWeight(0.34);
        splitPane.setDividerLocation(320);
        return splitPane;
    }

    private JTextArea readOnlyArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        area.setMargin(new Insets(10, 10, 10, 10));
        return area;
    }

    private <T> void selectFirstOrShowEmpty(JList<T> list, JTextArea details, String emptyMessage) {
        if (list.getModel().getSize() > 0) {
            list.setSelectedIndex(0);
        } else {
            details.setText(emptyMessage);
        }
    }
}
