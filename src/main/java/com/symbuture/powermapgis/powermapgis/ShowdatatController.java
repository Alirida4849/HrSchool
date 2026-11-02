package com.symbuture.powermapgis.powermapgis;

import java.net.URL;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.NodeOrientation;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.print.PrinterJob;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ShowdatatController implements Initializable {

    @FXML
    private BorderPane rootPane;

    private final String[] classes = {
        "الأول", "الثاني", "الثالث",
        "الرابع", "الخامس", "السادس"
    };

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (rootPane == null) {
            showError("لم يتم ربط rootPane بملف FXML. تأكد من وجود fx:id=\"rootPane\".");
            return;
        }

        try {
            Database.initialize();
            ensureImagePathColumn();
            createTables();
        } catch (SQLException e) {
            e.printStackTrace();
            showError("تعذر تحميل قاعدة البيانات:\n" + e.getMessage());
        }
    }

    // التأكد من وجود حقل مسار الصورة في قاعدة البيانات.
    // يفيد هذا مع قواعد البيانات القديمة التي أُنشئت قبل إضافة الصور.
    private void ensureImagePathColumn() throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement check = conn.prepareStatement("PRAGMA table_info(students)");
             ResultSet rs = check.executeQuery()) {
            while (rs.next()) {
                if ("image_path".equalsIgnoreCase(rs.getString("name"))) {
                    return;
                }
            }
        }

        try (Connection conn = Database.getConnection();
             PreparedStatement alter = conn.prepareStatement(
                     "ALTER TABLE students ADD COLUMN image_path TEXT")) {
            alter.executeUpdate();
        }
    }

    private void createTables() {
        TabPane tabPane = new TabPane();

        for (String className : classes) {
            Tab tab = new Tab("الصف " + className);
            TableView<StudentRecord> table = createStudentTable();
            table.setUserData(className);
            loadStudents(table, className);

            Label title = new Label();
            title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 12px;");
            updateTitle(title, className, table.getItems().size());

            table.getItems().addListener(
                (javafx.collections.ListChangeListener<StudentRecord>) change ->
                    updateTitle(title, className, table.getItems().size())
            );

            BorderPane content = new BorderPane();
            content.setTop(title);
            content.setCenter(table);
            tab.setContent(content);
            tab.setClosable(false);
            tabPane.getTabs().add(tab);
        }

        rootPane.setCenter(tabPane);
    }

    private void updateTitle(Label title, String className, int count) {
        title.setText("طلاب الصف " + className + " - العدد: " + count);
    }

    private TableView<StudentRecord> createStudentTable() {
        TableView<StudentRecord> table = new TableView<>();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("لا توجد بيانات مسجلة لهذا الصف"));
        table.setFixedCellSize(78);

        table.getColumns().add(createColumn("رقم الطالب", "id", 90));

        // عمود صورة الطالب: يقرأ مسار الصورة المحفوظ في image_path.
        TableColumn<StudentRecord, String> imageColumn = new TableColumn<>("صورة الطالب");
        imageColumn.setPrefWidth(110);
        imageColumn.setMinWidth(110);
        imageColumn.setSortable(false);
        imageColumn.setCellValueFactory(cell -> cell.getValue().imagePathProperty());
        imageColumn.setCellFactory(column -> new TableCell<>() {
            private final ImageView imageView = new ImageView();
            private final Label noImageLabel = new Label("لا توجد صورة");
            {
                imageView.setFitWidth(58);
                imageView.setFitHeight(64);
                imageView.setPreserveRatio(true);
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(String path, boolean empty) {
                super.updateItem(path, empty);
                setText(null);
                setGraphic(null);
                imageView.setImage(null);

                if (empty) {
                    return;
                }

                if (path == null || path.isBlank()) {
                    setGraphic(noImageLabel);
                    return;
                }

                try {
                    File imageFile = new File(path);
                    if (imageFile.isFile()) {
                        Image image = new Image(imageFile.toURI().toString(), 58, 64, true, true, true);
                        if (!image.isError()) {
                            imageView.setImage(image);
                            setGraphic(imageView);
                        } else {
                            setGraphic(new Label("تعذر تحميل الصورة"));
                        }
                    } else {
                        setGraphic(new Label("الصورة غير موجودة"));
                    }
                } catch (Exception ex) {
                    setGraphic(new Label("تعذر تحميل الصورة"));
                }
            }
        });
        table.getColumns().add(imageColumn);

        TableColumn<StudentRecord, String> nameColumn = createColumn("اسم الطالب", "fullName", 200);
        nameColumn.setStyle("-fx-cursor: hand;");
        nameColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty || name == null) {
                    setText(null);
                    setGraphic(null);
                    setOnMouseClicked(null);
                } else {
                    setText(name);
                    setGraphic(null);
                    setStyle("-fx-text-fill: #0078d4; -fx-underline: true; -fx-cursor: hand;");
                    setOnMouseClicked(event -> {
                        if (!isEmpty() && event.getClickCount() == 2) {
                            StudentRecord student = getTableView().getItems().get(getIndex());
                            editStudent(student, getTableView());
                        }
                    });
                }
            }
        });
        nameColumn.setOnEditStart(event -> { });
        table.getColumns().add(nameColumn);

        table.getColumns().add(createColumn("اسم الأم", "motherName", 180));
        table.getColumns().add(createColumn("الشعبة", "section", 90));
        table.getColumns().add(createColumn("تاريخ الميلاد", "birthDate", 130));
        table.getColumns().add(createColumn("هاتف ولي الأمر", "phone", 160));
        table.getColumns().add(createColumn("البطاقة الوطنية", "nationalId", 160));
        table.getColumns().add(createColumn("بطاقة السكن", "residenceCard", 160));
        table.getColumns().add(createColumn("الملاحظات", "notes", 250));

        TableColumn<StudentRecord, Void> actionsColumn = new TableColumn<>("الإجراءات");
        actionsColumn.setPrefWidth(175);
        actionsColumn.setMinWidth(175);
        actionsColumn.setSortable(false);
        actionsColumn.setCellFactory(column -> new TableCell<>() {
            private final Button viewButton = new Button("عرض المعلومات");
            private final Button deleteButton = new Button("حذف");
            private final HBox buttons = new HBox(6, viewButton, deleteButton);
            {
                viewButton.setStyle(
                    "-fx-background-color: #0078d4; -fx-text-fill: white; " +
                    "-fx-font-weight: bold; -fx-background-radius: 5px; -fx-cursor: hand;"
                );
                deleteButton.setStyle(
                    "-fx-background-color: #dc2626; -fx-text-fill: white; " +
                    "-fx-font-weight: bold; -fx-background-radius: 5px; -fx-cursor: hand;"
                );
                buttons.setAlignment(Pos.CENTER);
                viewButton.setOnAction(event -> {
                    if (!isEmpty()) {
                        StudentRecord student = getTableView().getItems().get(getIndex());
                        showStudentDetails(student);
                    }
                });
                deleteButton.setOnAction(event -> {
                    if (!isEmpty()) {
                        StudentRecord student = getTableView().getItems().get(getIndex());
                        deleteStudent(getTableView(), student);
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
                setAlignment(Pos.CENTER);
            }
        });
        table.getColumns().add(actionsColumn);
        return table;
    }

    private TableColumn<StudentRecord, String> createColumn(String title, String field, double width) {
        TableColumn<StudentRecord, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cell -> {
            StudentRecord student = cell.getValue();
            return switch (field) {
                case "id" -> student.idProperty();
                case "fullName" -> student.fullNameProperty();
                case "motherName" -> student.motherNameProperty();
                case "section" -> student.sectionProperty();
                case "birthDate" -> student.birthDateProperty();
                case "phone" -> student.phoneProperty();
                case "nationalId" -> student.nationalIdProperty();
                case "residenceCard" -> student.residenceCardProperty();
                case "notes" -> student.notesProperty();
                default -> new SimpleStringProperty("");
            };
        });
        return column;
    }

    private void loadStudents(TableView<StudentRecord> table, String className) {
        ObservableList<StudentRecord> students = FXCollections.observableArrayList();
        String sql = """
            SELECT id, full_name, mother_name, section,
                   birth_date, parent_phone, national_id,
                   residence_card, notes, image_path
            FROM students
            WHERE student_class = ?
            ORDER BY full_name COLLATE NOCASE
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, className);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    students.add(new StudentRecord(
                        String.valueOf(rs.getInt("id")),
                        rs.getString("full_name"),
                        rs.getString("mother_name"),
                        rs.getString("section"),
                        rs.getString("birth_date"),
                        rs.getString("parent_phone"),
                        rs.getString("national_id"),
                        rs.getString("residence_card"),
                        rs.getString("notes"),
                        rs.getString("image_path")
                    ));
                }
            }
            table.getItems().setAll(students);
        } catch (SQLException e) {
            e.printStackTrace();
            showError("تعذر تحميل طلاب الصف " + className + ":\n" + e.getMessage());
        }
    }

    private void editStudent(StudentRecord student, TableView<StudentRecord> table) {
        Stage window = new Stage();
        window.setTitle("معلومات الطالب وتعديل البيانات");
        window.initModality(Modality.APPLICATION_MODAL);

        TextField fullName = new TextField(safe(student.fullNameProperty().get()));
        TextField motherName = new TextField(safe(student.motherNameProperty().get()));
        TextField section = new TextField(safe(student.sectionProperty().get()));
        TextField phone = new TextField(safe(student.phoneProperty().get()));
        TextField nationalId = new TextField(safe(student.nationalIdProperty().get()));
        TextField residenceCard = new TextField(safe(student.residenceCardProperty().get()));
        TextArea notes = new TextArea(safe(student.notesProperty().get()));
        notes.setPrefRowCount(3);
        notes.setWrapText(true);

        DatePicker birthDate = new DatePicker();
        String currentDate = student.birthDateProperty().get();
        if (currentDate != null && !currentDate.isBlank()) {
            try {
                birthDate.setValue(LocalDate.parse(currentDate));
            } catch (Exception ex) {
                // إذا كان التاريخ القديم بصيغة مختلفة، يترك الحقل فارغاً ليختار المستخدم تاريخاً صحيحاً.
            }
        }

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        grid.addRow(0, new Label("اسم الطالب:"), fullName);
        grid.addRow(1, new Label("اسم الأم:"), motherName);
        grid.addRow(2, new Label("الشعبة:"), section);
        grid.addRow(3, new Label("تاريخ الميلاد:"), birthDate);
        grid.addRow(4, new Label("هاتف ولي الأمر:"), phone);
        grid.addRow(5, new Label("البطاقة الوطنية:"), nationalId);
        grid.addRow(6, new Label("بطاقة السكن:"), residenceCard);
        grid.addRow(7, new Label("الملاحظات:"), notes);

        for (javafx.scene.Node node : grid.getChildren()) {
            if (node instanceof TextField field) {
                field.setPrefWidth(300);
            }
        }
        birthDate.setPrefWidth(300);
        notes.setPrefWidth(300);

        Button saveButton = new Button("حفظ التعديلات");
        Button cancelButton = new Button("إلغاء");
        saveButton.setDefaultButton(true);
        cancelButton.setCancelButton(true);
        saveButton.setStyle("-fx-background-color: #0078d4; -fx-text-fill: white; -fx-font-weight: bold;");

        saveButton.setOnAction(event -> {
            if (fullName.getText() == null || fullName.getText().isBlank()) {
                showError("يرجى إدخال اسم الطالب.");
                return;
            }

            String sql = """
                UPDATE students
                SET full_name = ?, mother_name = ?, section = ?, birth_date = ?,
                    parent_phone = ?, national_id = ?, residence_card = ?, notes = ?
                WHERE id = ?
                """;

            try (Connection conn = Database.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, fullName.getText().trim());
                pstmt.setString(2, motherName.getText().trim());
                pstmt.setString(3, section.getText().trim());
                pstmt.setString(4, birthDate.getValue() == null ? null : birthDate.getValue().toString());
                pstmt.setString(5, phone.getText().trim());
                pstmt.setString(6, nationalId.getText().trim());
                pstmt.setString(7, residenceCard.getText().trim());
                pstmt.setString(8, notes.getText().trim());
                pstmt.setInt(9, Integer.parseInt(student.idProperty().get()));

                int updated = pstmt.executeUpdate();
                if (updated > 0) {
                    String className = (String) table.getUserData();
                    if (className != null) {
                        loadStudents(table, className);
                    }
                    window.close();
                    Alert alert = new Alert(AlertType.INFORMATION);
                    alert.setTitle("إدارة الطلاب");
                    alert.setHeaderText("تم تحديث البيانات");
                    alert.setContentText("تم حفظ تعديلات الطالب بنجاح.");
                    alert.showAndWait();
                } else {
                    showError("لم يتم العثور على سجل الطالب لتحديثه.");
                }
            } catch (SQLException | NumberFormatException e) {
                e.printStackTrace();
                showError("تعذر تحديث بيانات الطالب:\n" + e.getMessage());
            }
        });
        cancelButton.setOnAction(event -> window.close());

        HBox buttons = new HBox(10, saveButton, cancelButton);
        buttons.setAlignment(Pos.CENTER);
        buttons.setPadding(new Insets(10));
        VBox root = new VBox(10, grid, buttons);
        root.setPadding(new Insets(10));
        root.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        window.setScene(new Scene(root, 600, 600));
        window.setMinWidth(560);
        window.setMinHeight(580);
        window.showAndWait();
    }


    // عرض معلومات الطالب مع صورته، مع إمكانية طباعة التقرير.
    private void showStudentDetails(StudentRecord student) {
        Stage window = new Stage();
        window.setTitle("معلومات الطالب");
        window.initModality(Modality.APPLICATION_MODAL);

        ImageView photoView = new ImageView();
        photoView.setFitWidth(150);
        photoView.setFitHeight(170);
        photoView.setPreserveRatio(true);
        photoView.setSmooth(true);
        String path = student.imagePathProperty().get();
        if (path != null && !path.isBlank()) {
            try {
                File photoFile = new File(path);
                if (photoFile.isFile()) {
                    Image image = new Image(photoFile.toURI().toString());
                    if (!image.isError()) {
                        photoView.setImage(image);
                    }
                }
            } catch (Exception ex) {
                // تبقى مساحة الصورة فارغة إذا تعذر تحميلها.
            }
        }

        Label photoPlaceholder = new Label("لا توجد صورة متاحة");
        StackPane photoBox = new StackPane(photoView);
        photoBox.setMinHeight(180);
        photoBox.setPrefHeight(180);
        photoBox.setMaxWidth(190);
        photoBox.setStyle("-fx-border-color: #cbd5e1; -fx-border-radius: 6px; -fx-padding: 8px;");
        if (photoView.getImage() == null) {
            photoBox.getChildren().setAll(photoPlaceholder);
        }

        GridPane details = new GridPane();
        details.setHgap(14);
        details.setVgap(10);
        details.setPadding(new Insets(16));
        details.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        details.addRow(0, new Label("رقم الطالب:"), valueLabel(student.idProperty().get()));
        details.addRow(1, new Label("اسم الطالب:"), valueLabel(student.fullNameProperty().get()));
        details.addRow(2, new Label("اسم الأم:"), valueLabel(student.motherNameProperty().get()));
        details.addRow(3, new Label("الشعبة:"), valueLabel(student.sectionProperty().get()));
        details.addRow(4, new Label("تاريخ الميلاد:"), valueLabel(student.birthDateProperty().get()));
        details.addRow(5, new Label("هاتف ولي الأمر:"), valueLabel(student.phoneProperty().get()));
        details.addRow(6, new Label("البطاقة الوطنية:"), valueLabel(student.nationalIdProperty().get()));
        details.addRow(7, new Label("بطاقة السكن:"), valueLabel(student.residenceCardProperty().get()));
        details.addRow(8, new Label("الملاحظات:"), valueLabel(student.notesProperty().get()));

        Label heading = new Label("معلومات الطالب");
        heading.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #172554;");
        HBox top = new HBox(20, photoBox, details);
        top.setAlignment(Pos.TOP_CENTER);
        top.setPadding(new Insets(16));
        top.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        Button printButton = new Button("طباعة المعلومات");
        Button closeButton = new Button("إغلاق");
        printButton.setStyle("-fx-background-color: #15803d; -fx-text-fill: white; -fx-font-weight: bold;");
        printButton.setOnAction(event -> printStudentDetails(heading, photoView, photoPlaceholder,
                student, window));
        closeButton.setOnAction(event -> window.close());
        HBox actions = new HBox(10, printButton, closeButton);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(12));

        VBox content = new VBox(8, heading, top, actions);
        content.setPadding(new Insets(16));
        content.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        content.setStyle("-fx-background-color: white;");
        window.setScene(new Scene(content, 760, 570));
        window.setMinWidth(700);
        window.showAndWait();
    }

    private Label valueLabel(String value) {
        Label label = new Label(safe(value).isBlank() ? "—" : safe(value));
        label.setWrapText(true);
        label.setMaxWidth(360);
        return label;
    }

    private void printStudentDetails(Label heading, ImageView photoView,
            Label photoPlaceholder, StudentRecord student, Stage owner) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            showError("لا توجد طابعة متاحة على النظام.");
            return;
        }

        if (!job.showPrintDialog(owner)) {
            job.cancelJob();
            return;
        }

        ImageView printPhoto = new ImageView();
        printPhoto.setFitWidth(120);
        printPhoto.setFitHeight(140);
        printPhoto.setPreserveRatio(true);
        if (photoView.getImage() != null) {
            printPhoto.setImage(photoView.getImage());
        }
        VBox printable = new VBox(10,
                new Label("معلومات الطالب"),
                printPhoto,
                printLine("رقم الطالب", student.idProperty().get()),
                printLine("اسم الطالب", student.fullNameProperty().get()),
                printLine("اسم الأم", student.motherNameProperty().get()),
                printLine("الشعبة", student.sectionProperty().get()),
                printLine("تاريخ الميلاد", student.birthDateProperty().get()),
                printLine("هاتف ولي الأمر", student.phoneProperty().get()),
                printLine("البطاقة الوطنية", student.nationalIdProperty().get()),
                printLine("بطاقة السكن", student.residenceCardProperty().get()),
                printLine("الملاحظات", student.notesProperty().get()));
        printable.setPadding(new Insets(24));
        printable.setPrefWidth(520);
        printable.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        printable.applyCss();
        printable.layout();

        boolean printed = job.printPage(printable);
        if (printed) {
            job.endJob();
        } else {
            job.cancelJob();
            showError("تعذرت طباعة معلومات الطالب.");
        }
    }

    private Label printLine(String title, String value) {
        Label label = new Label(title + ": " + (safe(value).isBlank() ? "—" : safe(value)));
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 12px;");
        return label;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void deleteStudent(TableView<StudentRecord> table, StudentRecord student) {
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("تأكيد الحذف");
        confirm.setHeaderText("حذف الطالب: " + safe(student.fullNameProperty().get()));
        confirm.setContentText("هل أنت متأكد من حذف هذا الطالب نهائياً؟");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        String sql = "DELETE FROM students WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, Integer.parseInt(student.idProperty().get()));
            int deleted = pstmt.executeUpdate();
            if (deleted > 0) {
                table.getItems().remove(student);
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("إدارة الطلاب");
                alert.setHeaderText("تم الحذف بنجاح");
                alert.setContentText("تم حذف الطالب: " + safe(student.fullNameProperty().get()));
                alert.showAndWait();
            } else {
                showError("لم يتم العثور على الطالب في قاعدة البيانات.");
            }
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
            showError("تعذر حذف الطالب:\n" + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("إدارة الطلاب");
        alert.setHeaderText("حدث خطأ");
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class StudentRecord {
        private final SimpleStringProperty id;
        private final SimpleStringProperty fullName;
        private final SimpleStringProperty motherName;
        private final SimpleStringProperty section;
        private final SimpleStringProperty birthDate;
        private final SimpleStringProperty phone;
        private final SimpleStringProperty nationalId;
        private final SimpleStringProperty residenceCard;
        private final SimpleStringProperty notes;
        private final SimpleStringProperty imagePath;

        public StudentRecord(String id, String fullName, String motherName,
                String section, String birthDate, String phone, String nationalId,
                String residenceCard, String notes, String imagePath) {
            this.id = new SimpleStringProperty(id);
            this.fullName = new SimpleStringProperty(fullName);
            this.motherName = new SimpleStringProperty(motherName);
            this.section = new SimpleStringProperty(section);
            this.birthDate = new SimpleStringProperty(birthDate);
            this.phone = new SimpleStringProperty(phone);
            this.nationalId = new SimpleStringProperty(nationalId);
            this.residenceCard = new SimpleStringProperty(residenceCard);
            this.notes = new SimpleStringProperty(notes);
            this.imagePath = new SimpleStringProperty(imagePath);
        }

        public SimpleStringProperty idProperty() { return id; }
        public SimpleStringProperty fullNameProperty() { return fullName; }
        public SimpleStringProperty motherNameProperty() { return motherName; }
        public SimpleStringProperty sectionProperty() { return section; }
        public SimpleStringProperty birthDateProperty() { return birthDate; }
        public SimpleStringProperty phoneProperty() { return phone; }
        public SimpleStringProperty nationalIdProperty() { return nationalId; }
        public SimpleStringProperty residenceCardProperty() { return residenceCard; }
        public SimpleStringProperty notesProperty() { return notes; }
        public SimpleStringProperty imagePathProperty() { return imagePath; }
    }
}
