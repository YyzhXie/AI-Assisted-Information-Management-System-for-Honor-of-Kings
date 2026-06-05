package gui;

import model.Hero;
import model.Player;
import model.Team;
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
import javax.swing.JPanel;
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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

public class VisualizationFrame extends JFrame {
    private final GameDataManager manager;
    private final SearchService searchService;
    private final RankingService rankingService;

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
        RecommendationEngine recommendationEngine = new RecommendationEngine(manager);
        this.searchService = new SearchService(manager, recommendationEngine);
        this.rankingService = new RankingService(manager);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 680));
        setLocationByPlatform(true);
        setContentPane(createContent());
        refreshRanking();
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
        root.add(tabs, BorderLayout.CENTER);
        return root;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 4));
        JLabel title = new JLabel("王者荣耀 AI 辅助信息管理系统");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        JLabel subtitle = new JLabel("Swing 可视化版本：覆盖玩家查询、战队概览、英雄详情和排行榜");
        subtitle.setForeground(java.awt.Color.DARK_GRAY);
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.CENTER);
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
