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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VisualizationFrame extends JFrame {
    private static final String DATA_FILE = "data/game-data.json";
    private static final String EDIT_LOGIN_CARD = "login";
    private static final String EDIT_PLAYER_CARD = "player";
    private static final String EDIT_ADMIN_CARD = "admin";

    private final GameDataManager manager;
    private final AuthenticationService authService;
    private final SearchService searchService;
    private final RankingService rankingService;
    private final JLabel loginStatus = new JLabel("未登录：可使用公开可视化");
    private final JButton loginButton = new JButton("登录");
    private final JButton logoutButton = new JButton("登出");
    private final JButton currentUserButton = new JButton("我的信息");
    private final CardLayout editCardLayout = new CardLayout();
    private final JPanel editPanel = new JPanel(editCardLayout);
    private final JLabel editLoginPrompt = new JLabel("登录已查看信息。", JLabel.CENTER);
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

    private final JComboBox<String> rankingMode = new JComboBox<>(new String[]{"胜率", "等级", "对战次数"});
    private final JSpinner rankingLimit = new JSpinner(new SpinnerNumberModel(10, 1, 20, 1));
    private final DefaultTableModel rankingTableModel = new DefaultTableModel(
            new String[]{"名次", "玩家ID", "昵称", "等级", "胜率", "对战次数"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public VisualizationFrame(GameDataManager manager) {
        super("王者荣耀 AI 辅助信息管理系统 - Swing 可视化");
        this.manager = manager;
        this.authService = new AuthenticationService(manager);
        RecommendationEngine recommendationEngine = new RecommendationEngine(manager);
        this.searchService = new SearchService(manager, recommendationEngine);
        this.rankingService = new RankingService(manager);

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
        tabs.addTab("编辑信息", createEditPanel());
        root.add(tabs, BorderLayout.CENTER);
        return root;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 4));
        JLabel title = new JLabel("王者荣耀 AI 辅助信息管理系统");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        JLabel subtitle = new JLabel("Swing 可视化版本：覆盖玩家查询、战队概览、英雄详情和排行榜");
        subtitle.setForeground(java.awt.Color.DARK_GRAY);

        JPanel titlePanel = new JPanel(new BorderLayout(0, 4));
        titlePanel.add(title, BorderLayout.NORTH);
        titlePanel.add(subtitle, BorderLayout.CENTER);

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
        rule.setText("排序规则：单局比赛没有平局。排行榜按当前筛选条件排序；同位时依次按等级、胜率、对战次数降序，最后按玩家ID升序。");
        rule.setRows(2);

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(controls, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(rule, BorderLayout.SOUTH);
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
        newButton.addActionListener(event -> clearAdminForm());
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
                    player.getTotalMatches()
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
                JOptionPane.showMessageDialog(this, "登录失败，请检查用户名和密码。", "登录失败", JOptionPane.WARNING_MESSAGE);
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
            case "玩家" -> new String[][]{{"id", "玩家ID"}, {"username", "用户名"}, {"password", "密码"}, {"displayName", "昵称"}, {"level", "等级"}, {"wins", "胜场"}, {"losses", "负场"}, {"teamId", "战队ID"}, {"heroIds", "英雄ID(逗号)"}};
            case "英雄" -> new String[][]{{"id", "英雄ID"}, {"name", "名称"}, {"type", "类型"}, {"attack", "攻击"}, {"defense", "防御"}, {"skillPower", "技能"}, {"equipmentIds", "装备ID(逗号)"}};
            case "装备" -> new String[][]{{"id", "装备ID"}, {"name", "名称"}, {"type", "类型"}, {"rating", "评分"}, {"description", "属性描述"}};
            case "战队" -> new String[][]{{"id", "战队ID"}, {"name", "名称"}, {"memberIds", "成员ID(逗号)"}};
            case "对战记录" -> new String[][]{{"id", "对战ID"}, {"date", "日期yyyy-MM-dd"}, {"teamAId", "队伍A"}, {"teamBId", "队伍B"}, {"winnerTeamId", "胜者战队"}, {"choices", "英雄选择(P001:h003,...)"}};
            default -> new String[0][0];
        };
        for (int i = 0; i < fields.length; i++) {
            JTextField field = new JTextField(28);
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
    }

    private void clearAdminFields() {
        adminFields.values().forEach(field -> field.setText(""));
    }

    private void saveAdminForm() {
        try {
            String type = currentAdminType();
            String id = field("id");
            if (id.isEmpty()) {
                throw new IllegalArgumentException("ID不能为空。");
            }
            switch (type) {
                case "管理员" -> saveAdmin(id);
                case "玩家" -> savePlayer(id);
                case "英雄" -> saveHero(id);
                case "装备" -> saveEquipment(id);
                case "战队" -> saveTeam(id);
                case "对战记录" -> saveMatch(id);
                default -> throw new IllegalArgumentException("未知数据类型。");
            }
            refreshAllViews();
            refreshAdminEditorList();
            JOptionPane.showMessageDialog(this, type + "已保存。", "保存成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "保存失败: " + ex.getMessage(), "保存失败", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void saveAdmin(String id) {
        Admin admin = manager.findAdminById(id).orElse(null);
        if (admin == null) {
            manager.addAdmin(new Admin(id, field("username"), field("password"), field("displayName")));
            return;
        }
        admin.setUsername(field("username"));
        admin.setPassword(field("password"));
        admin.setDisplayName(field("displayName"));
        updateLoginState();
    }

    private void savePlayer(String id) {
        Player player = manager.findPlayerById(id).orElse(null);
        if (player == null) {
            player = new Player(id, field("username"), field("password"), field("displayName"),
                    intField("level", 1), intField("wins", 0), intField("losses", 0), field("teamId"));
            player.replaceHeroIds(csv(field("heroIds")));
            manager.addPlayer(player);
            return;
        }
        player.setUsername(field("username"));
        player.setPassword(field("password"));
        player.setDisplayName(field("displayName"));
        player.setLevel(intField("level", 1));
        player.setWins(intField("wins", 0));
        player.setLosses(intField("losses", 0));
        player.setTeamId(field("teamId"));
        player.replaceHeroIds(csv(field("heroIds")));
        manager.replacePlayer(player);
    }

    private void saveHero(String id) {
        Hero hero = manager.findHeroById(id).orElse(null);
        if (hero == null) {
            hero = new Hero(id, field("name"), HeroType.valueOf(field("type").toUpperCase()),
                    intField("attack", 0), intField("defense", 0), intField("skillPower", 0));
            hero.replaceCompatibleEquipmentIds(csv(field("equipmentIds")));
            manager.addHero(hero);
            return;
        }
        hero.setName(field("name"));
        hero.setType(HeroType.valueOf(field("type").toUpperCase()));
        hero.setAttack(intField("attack", 0));
        hero.setDefense(intField("defense", 0));
        hero.setSkillPower(intField("skillPower", 0));
        hero.replaceCompatibleEquipmentIds(csv(field("equipmentIds")));
    }

    private void saveEquipment(String id) {
        Equipment equipment = manager.findEquipmentById(id).orElse(null);
        if (equipment == null) {
            manager.addEquipment(new Equipment(id, field("name"), EquipmentType.valueOf(field("type").toUpperCase()),
                    doubleField("rating", 0.0), field("description")));
            return;
        }
        equipment.setName(field("name"));
        equipment.setType(EquipmentType.valueOf(field("type").toUpperCase()));
        equipment.setRating(doubleField("rating", 0.0));
        equipment.setAttributeDescription(field("description"));
    }

    private void saveTeam(String id) {
        Team team = manager.findTeamById(id).orElse(null);
        if (team == null) {
            team = new Team(id, field("name"));
            team.replaceMemberIds(csv(field("memberIds")));
            manager.addTeam(team);
            return;
        }
        team.setName(field("name"));
        team.replaceMemberIds(csv(field("memberIds")));
    }

    private void saveMatch(String id) {
        MatchRecord match = manager.getMatches().stream()
                .filter(item -> item.getId().equals(id))
                .findFirst()
                .orElse(null);
        if (match == null) {
            match = new MatchRecord(id, parseDate(field("date")), field("teamAId"), field("teamBId"), field("winnerTeamId"));
            match.replacePlayerHeroChoices(parseChoices(field("choices")));
            manager.addMatch(match);
            return;
        }
        match.setDate(parseDate(field("date")));
        match.setTeamAId(field("teamAId"));
        match.setTeamBId(field("teamBId"));
        match.setWinnerTeamId(field("winnerTeamId"));
        match.replacePlayerHeroChoices(parseChoices(field("choices")));
    }

    private void deleteSelectedAdminItem() {
        Object item = adminEditList.getSelectedValue();
        if (item == null) {
            JOptionPane.showMessageDialog(this, "请先选择要删除的数据。", "无法删除", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "确认删除 " + adminListText(item) + " 吗？", "确认删除", JOptionPane.YES_NO_OPTION);
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
                throw new IllegalArgumentException("英雄选择格式应为 P001:h003,P002:h011。");
            }
            choices.put(parts[0].trim(), parts[1].trim());
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
