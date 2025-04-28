import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.table.DefaultTableModel;

public class EnhancedMarksReport extends JFrame {
    // Database connection parameters
    private static final String URL = "jdbc:oracle:thin:@localhost:1521:XE";
    private static final String USER = "student_user";
    private static final String PASSWORD = "password"; // Replace with your Oracle password
    private static final String[] SUBJECTS = {"Mathematics", "Science", "English", "History", "Geography", "Computer Science"};

    // Color constants
    private static final Color PANEL_BACKGROUND = new Color(240, 240, 240);
    private static final Color BORDER_COLOR = new Color(0, 50, 150);

    // Class-level variables
    private JTextField tfRoll, tfName, tfClass;
    private JComboBox<String> cbSubject;
    private JTextField tfMarks;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JButton btnSearch, btnExport, btnClear, btnInsert, btnUpdate, btnDelete, btnSort;
    private JTabbedPane tabbedPane;

    // For Add Student
    private JTextField tfAddRoll, tfAddName, tfAddClass;
    private JTextField[] tfAddMarks = new JTextField[SUBJECTS.length];
    private JButton btnSaveAdd, btnClearAdd;

    // For Edit Student
    private JTextField tfEditRollSearch;
    private JButton btnLoadStudent;
    private JTextField tfEditName, tfEditClass;
    private JTextField[] tfEditMarks = new JTextField[SUBJECTS.length];
    private JButton btnSaveEdit;
    private String originalName, originalClass;
    private int[] originalMarks = new int[SUBJECTS.length];
    private int editStudentId = -1;

    public EnhancedMarksReport() {
        setTitle("Student Marks Management System");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        initComponents();
        setupLayout();

        setVisible(true);
    }

    private void initComponents() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setUI(new CustomTabbedPaneUI());
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 14));

        // Search & View components
        tfRoll = new JTextField(10);
        tfName = new JTextField(15);
        tfClass = new JTextField(5);
        tfMarks = new JTextField(5);
        cbSubject = new JComboBox<>();
        cbSubject.addItem("Select Subject");
        for (String subject : SUBJECTS) {
            cbSubject.addItem(subject);
        }

        btnSearch = createStyledButton("Search", "Search for students");
        btnExport = createStyledButton("Export", "Export search results to CSV");
        btnClear = createStyledButton("Clear", "Clear search fields");
        btnInsert = createStyledButton("Add New", "Add a new student");
        btnUpdate = createStyledButton("Update", "Update selected student");
        btnDelete = createStyledButton("Delete", "Delete selected student");
        btnSort = createStyledButton("Sort by Total", "Sort results by total marks");

        String[] columns = {"Roll No.", "Name", "Class", "Mathematics", "Science", "English", 
                           "History", "Geography", "Computer Science", "Total", "Percentage", "Rank"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultTable = new JTable(tableModel);
        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        resultTable.getTableHeader().setReorderingAllowed(false);
        resultTable.setFont(new Font("Arial", Font.PLAIN, 12));
        resultTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        resultTable.setGridColor(BORDER_COLOR);
        resultTable.setSelectionBackground(new Color(180, 200, 255));
        resultTable.setSelectionForeground(Color.BLACK);

        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(Color.BLUE);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        // Add Student components
        tfAddRoll = new JTextField(10);
        tfAddName = new JTextField(20);
        tfAddClass = new JTextField(5);
        for (int i = 0; i < SUBJECTS.length; i++) {
            tfAddMarks[i] = new JTextField(5);
        }
        btnSaveAdd = createStyledButton("Save", "Save new student");
        btnClearAdd = createStyledButton("Clear", "Clear add student fields");

        // Edit Student components
        tfEditRollSearch = new JTextField(10);
        btnLoadStudent = createStyledButton("Load", "Load student by roll number");
        tfEditName = new JTextField(20);
        tfEditClass = new JTextField(5);
        for (int i = 0; i < SUBJECTS.length; i++) {
            tfEditMarks[i] = new JTextField(5);
        }
        btnSaveEdit = createStyledButton("Save", "Save edited student");

        // Action listeners
        btnSearch.addActionListener(e -> performSearch());
        btnExport.addActionListener(e -> exportResults());
        btnClear.addActionListener(e -> clearFields());
        btnInsert.addActionListener(e -> tabbedPane.setSelectedIndex(1));
        btnUpdate.addActionListener(e -> updateSelectedStudent());
        btnDelete.addActionListener(e -> deleteSelectedStudent());
        btnSort.addActionListener(e -> sortByTotal());
        btnSaveAdd.addActionListener(e -> addStudent());
        btnClearAdd.addActionListener(e -> clearAddFields());
        btnLoadStudent.addActionListener(e -> loadStudentForEdit());
        btnSaveEdit.addActionListener(e -> saveEditedStudent());
    }

    private JButton createStyledButton(String text, String tooltip) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(100, 150, 255), 0, getHeight(), BORDER_COLOR));
                g2.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
                g2.dispose();
            }
        };
        button.setForeground(new Color(0, 0, 100));
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setPreferredSize(new Dimension(120, 35));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBorder(new RoundedBorder(10));
        button.setToolTipText(tooltip);
        return button;
    }

    private JPanel createAddStudentPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(PANEL_BACKGROUND);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblRoll = createStyledLabel("Roll Number:");
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(lblRoll, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(tfAddRoll, gbc);

        JLabel lblName = createStyledLabel("Name:");
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panel.add(lblName, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(tfAddName, gbc);

        JLabel lblClass = createStyledLabel("Class:");
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        panel.add(lblClass, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(tfAddClass, gbc);

        JPanel marksPanel = new JPanel(new GridLayout(6, 2, 10, 8));
        marksPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER_COLOR), "Subject Marks (Optional)",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            new Font("Arial", Font.BOLD, 12)));
        marksPanel.setBackground(PANEL_BACKGROUND);
        for (int i = 0; i < SUBJECTS.length; i++) {
            JLabel lblSubject = createStyledLabel(SUBJECTS[i] + ":");
            marksPanel.add(lblSubject);
            marksPanel.add(tfAddMarks[i]);
        }

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.weightx = 1.0;
        panel.add(marksPanel, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(btnSaveAdd);
        buttonPanel.add(btnClearAdd);
        panel.add(buttonPanel, gbc);

        return panel;
    }

    private JPanel createEditStudentPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PANEL_BACKGROUND);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setOpaque(false);
        JLabel lblRollSearch = createStyledLabel("Roll Number:");
        topPanel.add(lblRollSearch);
        topPanel.add(tfEditRollSearch);
        topPanel.add(btnLoadStudent);
        panel.add(topPanel, BorderLayout.NORTH);

        JPanel dataPanel = new JPanel(new GridBagLayout());
        dataPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblName = createStyledLabel("Name:");
        gbc.gridx = 0; gbc.gridy = 0;
        dataPanel.add(lblName, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        dataPanel.add(tfEditName, gbc);

        JLabel lblClass = createStyledLabel("Class:");
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        dataPanel.add(lblClass, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        dataPanel.add(tfEditClass, gbc);

        JPanel marksPanel = new JPanel(new GridLayout(6, 2, 10, 8));
        marksPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER_COLOR), "Subject Marks (Optional)",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            new Font("Arial", Font.BOLD, 12)));
        marksPanel.setBackground(PANEL_BACKGROUND);
        for (int i = 0; i < SUBJECTS.length; i++) {
            JLabel lblSubject = createStyledLabel(SUBJECTS[i] + ":");
            marksPanel.add(lblSubject);
            marksPanel.add(tfEditMarks[i]);
        }

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.weightx = 1.0;
        dataPanel.add(marksPanel, gbc);

        panel.add(dataPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(btnSaveEdit);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        label.setForeground(Color.DARK_GRAY);
        return label;
    }

    private void setupLayout() {
        JPanel searchPanel = createSearchPanel();
        tabbedPane.addTab("Search & View", createViewPanel(searchPanel));
        tabbedPane.addTab("Add Student", createAddStudentPanel());
        tabbedPane.addTab("Edit Student", createEditStudentPanel());

        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(PANEL_BACKGROUND);
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        statusPanel.add(statusLabel, BorderLayout.WEST);
        add(statusPanel, BorderLayout.SOUTH);
    }

    private JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel(new GridBagLayout());
        searchPanel.setBackground(PANEL_BACKGROUND);
        searchPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel lblRoll = createStyledLabel("Roll No:");
        gbc.gridx = 0; gbc.gridy = 0;
        searchPanel.add(lblRoll, gbc);
        gbc.gridx = 1;
        searchPanel.add(tfRoll, gbc);
        JLabel lblName = createStyledLabel("Name:");
        gbc.gridx = 2;
        searchPanel.add(lblName, gbc);
        gbc.gridx = 3;
        searchPanel.add(tfName, gbc);

        JLabel lblClass = createStyledLabel("Class:");
        gbc.gridx = 0; gbc.gridy = 1;
        searchPanel.add(lblClass, gbc);
        gbc.gridx = 1;
        searchPanel.add(tfClass, gbc);
        JLabel lblSubject = createStyledLabel("Subject:");
        gbc.gridx = 2;
        searchPanel.add(lblSubject, gbc);
        gbc.gridx = 3;
        searchPanel.add(cbSubject, gbc);

        JLabel lblMarks = createStyledLabel("Marks:");
        gbc.gridx = 0; gbc.gridy = 2;
        searchPanel.add(lblMarks, gbc);
        gbc.gridx = 1;
        searchPanel.add(tfMarks, gbc);

        return searchPanel;
    }

    private JPanel createViewPanel(JPanel searchPanel) {
        JPanel viewPanel = new JPanel(new BorderLayout());
        viewPanel.setBackground(PANEL_BACKGROUND);
        viewPanel.add(searchPanel, BorderLayout.NORTH);
        viewPanel.add(new JScrollPane(resultTable), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.add(btnSearch);
        buttonPanel.add(btnSort);
        buttonPanel.add(btnUpdate);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnInsert);
        buttonPanel.add(btnExport);
        buttonPanel.add(btnClear);
        viewPanel.add(buttonPanel, BorderLayout.SOUTH);

        return viewPanel;
    }

    private void performSearch() {
        String subject = (String) cbSubject.getSelectedItem();
        searchRecords(subject);
    }

    private void searchRecords(String subject) {
        StringBuilder queryBuilder = new StringBuilder(
            "SELECT s.student_id, s.roll_number, s.name, s.class_name, " +
            "MAX(CASE WHEN m.subject = 'Mathematics' THEN NVL(TO_NUMBER(m.marks_obtained), 0) ELSE 0 END) AS Mathematics, " +
            "MAX(CASE WHEN m.subject = 'Science' THEN NVL(TO_NUMBER(m.marks_obtained), 0) ELSE 0 END) AS Science, " +
            "MAX(CASE WHEN m.subject = 'English' THEN NVL(TO_NUMBER(m.marks_obtained), 0) ELSE 0 END) AS English, " +
            "MAX(CASE WHEN m.subject = 'History' THEN NVL(TO_NUMBER(m.marks_obtained), 0) ELSE 0 END) AS History, " +
            "MAX(CASE WHEN m.subject = 'Geography' THEN NVL(TO_NUMBER(m.marks_obtained), 0) ELSE 0 END) AS Geography, " +
            "MAX(CASE WHEN m.subject = 'Computer Science' THEN NVL(TO_NUMBER(m.marks_obtained), 0) ELSE 0 END) AS ComputerScience " +
            "FROM students s LEFT JOIN marks m ON s.student_id = m.student_id WHERE 1=1 "
        );

        List<String> conditions = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();

        String roll = tfRoll.getText().trim();
        String name = tfName.getText().trim();
        String className = tfClass.getText().trim();
        String marksStr = tfMarks.getText().trim();

        if (!roll.isEmpty()) {
            conditions.add("AND LOWER(s.roll_number) LIKE LOWER(?)");
            parameters.add("%" + roll + "%");
        }

        if (!name.isEmpty()) {
            conditions.add("AND LOWER(s.name) LIKE LOWER(?)");
            parameters.add("%" + name + "%");
        }

        if (!className.isEmpty()) {
            conditions.add("AND LOWER(s.class_name) LIKE LOWER(?)");
            parameters.add("%" + className + "%");
        }

        if (!subject.equals("Select Subject") && !subject.isEmpty()) {
            conditions.add("AND EXISTS (SELECT 1 FROM marks m2 WHERE m2.student_id = s.student_id AND LOWER(m2.subject) = LOWER(?))");
            parameters.add(subject);
        }

        if (!marksStr.isEmpty() && marksStr.matches("\\d+")) {
            int marks = Integer.parseInt(marksStr);
            conditions.add("AND EXISTS (SELECT 1 FROM marks m2 WHERE m2.student_id = s.student_id AND NVL(TO_NUMBER(m2.marks_obtained), 0) = ?)");
            parameters.add(marks);
        }

        for (String condition : conditions) {
            queryBuilder.append(condition);
        }

        queryBuilder.append(" GROUP BY s.student_id, s.roll_number, s.name, s.class_name");

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(queryBuilder.toString())) {

            for (int i = 0; i < parameters.size(); i++) {
                pstmt.setObject(i + 1, parameters.get(i));
            }

            ResultSet rs = pstmt.executeQuery();
            processResults(rs);
            setStatus("Search complete. " + tableModel.getRowCount() + " records found.", false);
        } catch (SQLException e) {
            showErrorMessage("⚠️ Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processResults(ResultSet rs) throws SQLException {
        tableModel.setRowCount(0);
        List<StudentRecord> students = new ArrayList<>();

        while (rs.next()) {
            int studentId = rs.getInt("student_id");
            String rollNumber = rs.getString("roll_number");
            String name = rs.getString("name");
            String className = rs.getString("class_name");

            int mathMarks = rs.getObject("Mathematics") != null ? rs.getInt("Mathematics") : 0;
            int scienceMarks = rs.getObject("Science") != null ? rs.getInt("Science") : 0;
            int englishMarks = rs.getObject("English") != null ? rs.getInt("English") : 0;
            int historyMarks = rs.getObject("History") != null ? rs.getInt("History") : 0;
            int geographyMarks = rs.getObject("Geography") != null ? rs.getInt("Geography") : 0;
            int computerMarks = rs.getObject("ComputerScience") != null ? rs.getInt("ComputerScience") : 0;

            int totalMarks = mathMarks + scienceMarks + englishMarks + historyMarks + geographyMarks + computerMarks;
            double percentage = (totalMarks / 600.0) * 100;

            students.add(new StudentRecord(studentId, rollNumber, name, className, mathMarks, scienceMarks, englishMarks,
                historyMarks, geographyMarks, computerMarks, totalMarks, percentage));
        }

        Collections.sort(students, Comparator.comparing(StudentRecord::getTotalMarks).reversed());

        int rank = 1, prevTotal = -1, sameRankCount = 0;
        DecimalFormat df = new DecimalFormat("0.00");

        for (StudentRecord student : students) {
            if (prevTotal != student.getTotalMarks()) {
                rank += sameRankCount;
                sameRankCount = 1;
                prevTotal = student.getTotalMarks();
            } else {
                sameRankCount++;
            }

            tableModel.addRow(new Object[] {
                student.getRollNumber(), student.getName(), student.getClassName(),
                student.getMathMarks(), student.getScienceMarks(), student.getEnglishMarks(),
                student.getHistoryMarks(), student.getGeographyMarks(), student.getComputerMarks(),
                student.getTotalMarks(), df.format(student.getPercentage()) + "%", rank
            });
        }
    }

    private void exportResults() {
        String roll = JOptionPane.showInputDialog(this, "Enter Roll Number (leave blank for all):");
        String name = JOptionPane.showInputDialog(this, "Enter Student Name (leave blank for all):");

        if (roll == null || name == null) return;

        List<Integer> rowsToExport = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String rowRoll = (String) tableModel.getValueAt(i, 0);
            String rowName = (String) tableModel.getValueAt(i, 1);
            if ((roll.isEmpty() || rowRoll.toLowerCase().contains(roll.toLowerCase())) &&
                (name.isEmpty() || rowName.toLowerCase().contains(name.toLowerCase()))) {
                rowsToExport.add(i);
            }
        }

        if (rowsToExport.isEmpty()) {
            showErrorMessage("No matching records found for export.");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Export File");
        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        String filePath = fileChooser.getSelectedFile().getAbsolutePath();
        if (!filePath.toLowerCase().endsWith(".csv")) filePath += ".csv";

        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("Roll Number,Name,Class,Mathematics,Science,English,History,Geography,Computer Science,Total,Percentage,Rank");
            for (int rowIndex : rowsToExport) {
                StringBuilder line = new StringBuilder();
                for (int j = 0; j < tableModel.getColumnCount(); j++) {
                    line.append(tableModel.getValueAt(rowIndex, j));
                    if (j < tableModel.getColumnCount() - 1) line.append(",");
                }
                writer.println(line);
            }
            setStatus("✅ Successfully exported " + rowsToExport.size() + " records to " + filePath, false);
        } catch (IOException e) {
            showErrorMessage("Export failed: " + e.getMessage());
        }
    }

    private void clearFields() {
        tfRoll.setText("");
        tfName.setText("");
        tfClass.setText("");
        tfMarks.setText("");
        cbSubject.setSelectedIndex(0);
        tableModel.setRowCount(0);
        setStatus("Fields cleared.", false);
    }

    private void addStudent() {
        String rollNo = tfAddRoll.getText().trim();
        String name = tfAddName.getText().trim();
        String className = tfAddClass.getText().trim();
        int[] marks = new int[SUBJECTS.length];

        if (rollNo.isEmpty() || name.isEmpty() || className.isEmpty()) {
            showErrorMessage("Please enter Roll Number, Name, and Class.");
            return;
        }

        for (int i = 0; i < SUBJECTS.length; i++) {
            String marksStr = tfAddMarks[i].getText().trim();
            if (marksStr.isEmpty()) {
                marks[i] = -1; // Indicate no mark entered
            } else if (!marksStr.matches("\\d+")) {
                showErrorMessage("Invalid marks for " + SUBJECTS[i] + ". Please enter a number or leave blank.");
                return;
            } else {
                marks[i] = Integer.parseInt(marksStr);
                if (marks[i] < 0 || marks[i] > 100) {
                showErrorMessage("Marks for " + SUBJECTS[i] + " must be between 0 and 100.");
                return;
                }
            }
        }

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            conn.setAutoCommit(false);

            String checkSql = "SELECT student_id FROM students WHERE roll_number = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, rollNo);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    showErrorMessage("Roll number already exists.");
                    return;
                }
            }

            String insertStudentSql = "INSERT INTO students (student_id, roll_number, name, class_name) " +
                                     "VALUES (student_id_seq.NEXTVAL, ?, ?, ?)";
            int studentId;
            try (PreparedStatement pstmt = conn.prepareStatement(insertStudentSql, new String[] {"STUDENT_ID"})) {
                pstmt.setString(1, rollNo);
                pstmt.setString(2, name);
                pstmt.setString(3, className);
                pstmt.executeUpdate();
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) studentId = rs.getInt(1);
                    else throw new SQLException("Failed to retrieve student_id.");
                }
            }

            for (int i = 0; i < SUBJECTS.length; i++) {
                if (marks[i] != -1) { // Only insert if mark is provided
                    String insertMarksSql = "INSERT INTO marks (mark_id, student_id, subject, marks_obtained) " +
                                           "VALUES (mark_id_seq.NEXTVAL, ?, ?, ?)";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertMarksSql)) {
                        insertStmt.setInt(1, studentId);
                        insertStmt.setString(2, SUBJECTS[i]);
                        insertStmt.setString(3, String.valueOf(marks[i]));
                        insertStmt.executeUpdate();
                    }
                }
            }

            conn.commit();
            setStatus("✅ Added new student: " + name, false);
            clearAddFields();
            performSearch();
        } catch (SQLException e) {
            showErrorMessage("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadStudentForEdit() {
        String rollNo = tfEditRollSearch.getText().trim();
        if (rollNo.isEmpty()) {
            showErrorMessage("Please enter a roll number.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            String sql = "SELECT s.student_id, s.name, s.class_name, m.subject, m.marks_obtained " +
                         "FROM students s LEFT JOIN marks m ON s.student_id = m.student_id " +
                         "WHERE s.roll_number = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, rollNo);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    editStudentId = rs.getInt("student_id");
                    originalName = rs.getString("name");
                    originalClass = rs.getString("class_name");
                    tfEditName.setText(originalName);
                    tfEditClass.setText(originalClass);

                    for (int i = 0; i < SUBJECTS.length; i++) {
                        originalMarks[i] = 0;
                        tfEditMarks[i].setText("0");
                    }

                    do {
                        String subject = rs.getString("subject");
                        if (subject != null) {
                            for (int i = 0; i < SUBJECTS.length; i++) {
                                if (SUBJECTS[i].equalsIgnoreCase(subject)) {
                                    originalMarks[i] = rs.getInt("marks_obtained");
                                    tfEditMarks[i].setText(String.valueOf(originalMarks[i]));
                                    break;
                                }
                            }
                        }
                    } while (rs.next());

                    setStatus("Student loaded: " + originalName, false);
                } else {
                    showErrorMessage("Student not found with roll number: " + rollNo);
                    clearEditFields();
                }
            }
        } catch (SQLException e) {
            showErrorMessage("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveEditedStudent() {
        if (editStudentId == -1) {
            showErrorMessage("No student loaded for editing.");
            return;
        }

        String newName = tfEditName.getText().trim();
        String newClass = tfEditClass.getText().trim();
        int[] newMarks = new int[SUBJECTS.length];
        for (int i = 0; i < SUBJECTS.length; i++) {
            String marksStr = tfEditMarks[i].getText().trim();
            if (marksStr.isEmpty()) marksStr = "0";
            if (!marksStr.matches("\\d+")) {
                showErrorMessage("Invalid marks for " + SUBJECTS[i]);
                return;
            }
            newMarks[i] = Integer.parseInt(marksStr);
            if (newMarks[i] < 0 || newMarks[i] > 100) {
                showErrorMessage("Marks for " + SUBJECTS[i] + " must be between 0 and 100.");
                return;
            }
        }

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            conn.setAutoCommit(false);

            if (!newName.equals(originalName) || !newClass.equals(originalClass)) {
                String updateStudentSql = "UPDATE students SET name = ?, class_name = ? WHERE student_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateStudentSql)) {
                    pstmt.setString(1, newName);
                    pstmt.setString(2, newClass);
                    pstmt.setInt(3, editStudentId);
                    pstmt.executeUpdate();
                }
            }

            for (int i = 0; i < SUBJECTS.length; i++) {
                if (newMarks[i] != originalMarks[i]) {
                    String checkSql = "SELECT mark_id FROM marks WHERE student_id = ? AND subject = ?";
                    try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                        checkStmt.setInt(1, editStudentId);
                        checkStmt.setString(2, SUBJECTS[i]);
                        ResultSet rs = checkStmt.executeQuery();
                        if (rs.next()) {
                            String updateMarksSql = "UPDATE marks SET marks_obtained = ? WHERE student_id = ? AND subject = ?";
                            try (PreparedStatement updateStmt = conn.prepareStatement(updateMarksSql)) {
                                updateStmt.setString(1, String.valueOf(newMarks[i]));
                                updateStmt.setInt(2, editStudentId);
                                updateStmt.setString(3, SUBJECTS[i]);
                                updateStmt.executeUpdate();
                            }
                        } else {
                            String insertMarksSql = "INSERT INTO marks (mark_id, student_id, subject, marks_obtained) " +
                                                   "VALUES (mark_id_seq.NEXTVAL, ?, ?, ?)";
                            try (PreparedStatement insertStmt = conn.prepareStatement(insertMarksSql)) {
                                insertStmt.setInt(1, editStudentId);
                                insertStmt.setString(2, SUBJECTS[i]);
                                insertStmt.setString(3, String.valueOf(newMarks[i]));
                                insertStmt.executeUpdate();
                            }
                        }
                    }
                }
            }

            conn.commit();
            setStatus("✅ Student updated: " + newName, false);
            clearEditFields();
            performSearch();
        } catch (SQLException e) {
            showErrorMessage("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateSelectedStudent() {
        int selectedRow = resultTable.getSelectedRow();
        if (selectedRow == -1) {
            showErrorMessage("Please select a student to update");
            return;
        }
        String rollNo = tableModel.getValueAt(selectedRow, 0).toString();
        tfEditRollSearch.setText(rollNo);
        loadStudentForEdit();
        tabbedPane.setSelectedIndex(2);
    }

    private void deleteSelectedStudent() {
        int selectedRow = resultTable.getSelectedRow();
        if (selectedRow == -1) {
            showErrorMessage("Please select a student to delete");
            return;
        }

        String rollNo = tableModel.getValueAt(selectedRow, 0).toString();
        String name = tableModel.getValueAt(selectedRow, 1).toString();

        int confirm = JOptionPane.showConfirmDialog(this, "Delete " + name + " (" + rollNo + ")?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            conn.setAutoCommit(false);
            int studentId = getStudentId(conn, rollNo);
            if (studentId == -1) {
                showErrorMessage("Student not found in database");
                return;
            }

            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM marks WHERE student_id = ?")) {
                pstmt.setInt(1, studentId);
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM students WHERE student_id = ?")) {
                pstmt.setInt(1, studentId);
                pstmt.executeUpdate();
            }

            conn.commit();
            tableModel.removeRow(selectedRow);
            setStatus("✅ Student " + name + " deleted", false);
        } catch (SQLException e) {
            showErrorMessage("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sortByTotal() {
        if (tableModel.getRowCount() == 0) {
            showErrorMessage("No data to sort. Please perform a search first.");
            return;
        }

        List<Object[]> data = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Object[] row = new Object[tableModel.getColumnCount()];
            for (int j = 0; j < tableModel.getColumnCount(); j++) row[j] = tableModel.getValueAt(i, j);
            data.add(row);
        }

        data.sort((row1, row2) -> Integer.compare((Integer) row2[9], (Integer) row1[9]));

        int rank = 1, prevTotal = -1, sameRankCount = 0;
        for (Object[] row : data) {
            int total = (Integer) row[9];
            if (prevTotal != total) {
                rank += sameRankCount;
                sameRankCount = 1;
                prevTotal = total;
            } else sameRankCount++;
            row[11] = rank;
        }

        tableModel.setRowCount(0);
        for (Object[] row : data) tableModel.addRow(row);
        setStatus("Table sorted by total marks", false);
    }

    private void clearAddFields() {
        tfAddRoll.setText("");
        tfAddName.setText("");
        tfAddClass.setText("");
        for (JTextField tf : tfAddMarks) tf.setText("");
    }

    private void clearEditFields() {
        tfEditRollSearch.setText("");
        tfEditName.setText("");
        tfEditClass.setText("");
        for (JTextField tf : tfEditMarks) tf.setText("");
        editStudentId = -1;
    }

    private int getStudentId(Connection conn, String rollNo) throws SQLException {
        String sql = "SELECT student_id FROM students WHERE roll_number = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, rollNo);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt("student_id") : -1;
            }
        }
    }

    private void setStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setForeground(isError ? Color.RED : Color.BLUE);
    }

    private void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
        setStatus(message, true);
    }

    private static class StudentRecord {
        private final int studentId;
        private final String rollNumber, name, className;
        private final int mathMarks, scienceMarks, englishMarks, historyMarks, geographyMarks, computerMarks;
        private final int totalMarks;
        private final double percentage;

        public StudentRecord(int studentId, String rollNumber, String name, String className, int mathMarks,
                             int scienceMarks, int englishMarks, int historyMarks, int geographyMarks, int computerMarks,
                             int totalMarks, double percentage) {
            this.studentId = studentId;
            this.rollNumber = rollNumber;
            this.name = name;
            this.className = className;
            this.mathMarks = mathMarks;
            this.scienceMarks = scienceMarks;
            this.englishMarks = englishMarks;
            this.historyMarks = historyMarks;
            this.geographyMarks = geographyMarks;
            this.computerMarks = computerMarks;
            this.totalMarks = totalMarks;
            this.percentage = percentage;
        }

        public int getStudentId() { return studentId; }
        public String getRollNumber() { return rollNumber; }
        public String getName() { return name; }
        public String getClassName() { return className; }
        public int getMathMarks() { return mathMarks; }
        public int getScienceMarks() { return scienceMarks; }
        public int getEnglishMarks() { return englishMarks; }
        public int getHistoryMarks() { return historyMarks; }
        public int getGeographyMarks() { return geographyMarks; }
        public int getComputerMarks() { return computerMarks; }
        public int getTotalMarks() { return totalMarks; }
        public double getPercentage() { return percentage; }
    }

    private static class RoundedBorder extends AbstractBorder {
        private final int radius;

        RoundedBorder(int radius) {
            this.radius = radius;
        }

        @Override
        public void paintBorder(java.awt.Component c, Graphics g, int x, int y, int width, int height) {
            g.setColor(BORDER_COLOR);
            g.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        }

        @Override
        public Insets getBorderInsets(java.awt.Component c) {
            return new Insets(radius / 2, radius, radius / 2, radius);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    private static class CustomTabbedPaneUI extends BasicTabbedPaneUI {
        @Override
        protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
            Graphics2D g2 = (Graphics2D) g.create();
            if (isSelected) {
                g2.setPaint(new GradientPaint(x, y, new Color(100, 150, 255), x, y + h, BORDER_COLOR));
            } else {
                g2.setColor(new Color(220, 220, 220));
            }
            g2.fillRect(x, y, w, h);
            g2.dispose();
        }

        @Override
        protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
            g.setColor(BORDER_COLOR);
            g.drawRect(x, y, w - 1, h - 1);
        }

        @Override
        protected void paintText(Graphics g, int tabPlacement, Font font, java.awt.FontMetrics metrics, int tabIndex, String title, java.awt.Rectangle textRect, boolean isSelected) {
            g.setFont(font);
            if (isSelected) {
                g.setColor(Color.WHITE);
            } else {
                g.setColor(Color.BLACK);
            }
            int x = textRect.x + (textRect.width - metrics.stringWidth(title)) / 2;
            int y = textRect.y + metrics.getAscent();
            g.drawString(title, x, y);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EnhancedMarksReport());
    }
}