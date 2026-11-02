package com.symbuture.powermapgis.powermapgis;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.image.ImageView;
import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
public class PowerMapGISController {

   @FXML
    private ImageView imgRegisterStudent;

@FXML
private TextField txtSearchStudent;
    @FXML
    VBox cardRegisterStudent,cardViewStudents;
   
    @FXML
    private ImageView imgViewStudents;

    @FXML
    void initialize() {

         loadImage(imgRegisterStudent, "/images/register.png");
        loadImage(imgViewStudents, "/images/website.png");
        cardRegisterStudent.setOnMouseClicked(event -> {
        openPage("/com/symbuture/powermapgis/powermapgis/StudentAdd.fxml");
           
    });
        
        cardViewStudents.setOnMouseClicked(event -> {
        
           openPagetwo("/fxml/showdatat.fxml");
    });
        
    }
    

@FXML
private void searchStudent() {

    String keyword = txtSearchStudent.getText();

    if (keyword == null || keyword.isBlank()) {
        showMessage("يرجى إدخال اسم الطالب أو رقمه.");
        return;
    }

    String sql = """
        SELECT id, full_name, mother_name, student_class,
               section, birth_date, parent_phone,
               national_id, residence_card, notes, image_path
        FROM students
        WHERE CAST(id AS TEXT) = ?
           OR full_name LIKE ?
        ORDER BY
            CASE WHEN CAST(id AS TEXT) = ? THEN 0 ELSE 1 END
        LIMIT 1
        """;

    try (Connection conn = Database.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        String search = keyword.trim();

        pstmt.setString(1, search);
        pstmt.setString(2, "%" + search + "%");
        pstmt.setString(3, search);

        try (ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                showStudentWindow(
                    rs.getString("id"),
                    rs.getString("full_name"),
                    rs.getString("mother_name"),
                    rs.getString("student_class"),
                    rs.getString("section"),
                    rs.getString("birth_date"),
                    rs.getString("parent_phone"),
                    rs.getString("national_id"),
                    rs.getString("residence_card"),
                    rs.getString("notes"),
                    rs.getString("image_path")
                );
            } else {
                showMessage("لم يتم العثور على الطالب.");
            }
        }

    } catch (SQLException e) {
        e.printStackTrace();
        showMessage("حدث خطأ أثناء البحث:\n" + e.getMessage());
    }
}
private void showStudentWindow(
        String id,
        String name,
        String mother,
        String studentClass,
        String section,
        String birthDate,
        String phone,
        String nationalId,
        String residenceCard,
        String notes,
        String imagePath) {

    Stage window = new Stage();
    window.setTitle("معلومات الطالب");

    VBox content = new VBox(12);
    content.setPadding(new javafx.geometry.Insets(20));
    content.setAlignment(javafx.geometry.Pos.CENTER);
    content.setNodeOrientation(
        javafx.geometry.NodeOrientation.RIGHT_TO_LEFT
    );
    content.setStyle("-fx-background-color: white;");

    ImageView photo = new ImageView();
    photo.setFitWidth(130);
    photo.setFitHeight(150);
    photo.setPreserveRatio(true);

    if (imagePath != null && !imagePath.isBlank()) {
        File file = new File(imagePath);

        if (file.isFile()) {
            Image image = new Image(file.toURI().toString());

            if (!image.isError()) {
                photo.setImage(image);
            }
        }
    }

    Label heading = new Label("معلومات الطالب");
    heading.setStyle(
        "-fx-font-size: 22px;" +
        "-fx-font-weight: bold;" +
        "-fx-text-fill: #172554;"
    );

    VBox details = new VBox(8);
    details.setMaxWidth(450);

    details.getChildren().addAll(
        new Label("رقم الطالب: " + safeText(id)),
        new Label("اسم الطالب: " + safeText(name)),
        new Label("اسم الأم: " + safeText(mother)),
        new Label("الصف: " + safeText(studentClass)),
        new Label("الشعبة: " + safeText(section)),
        new Label("تاريخ الميلاد: " + safeText(birthDate)),
        new Label("هاتف ولي الأمر: " + safeText(phone)),
        new Label("البطاقة الوطنية: " + safeText(nationalId)),
        new Label("بطاقة السكن: " + safeText(residenceCard)),
        new Label("الملاحظات: " + safeText(notes))
    );

    details.setNodeOrientation(
        javafx.geometry.NodeOrientation.RIGHT_TO_LEFT
    );

    Button closeButton = new Button("إغلاق");
    closeButton.setStyle(
        "-fx-background-color: #172554;" +
        "-fx-text-fill: white;" +
        "-fx-font-weight: bold;"
    );
    closeButton.setOnAction(event -> window.close());

    content.getChildren().addAll(
        heading, photo, details, closeButton
    );

    javafx.scene.control.ScrollPane scrollPane =
        new javafx.scene.control.ScrollPane(content);

    scrollPane.setFitToWidth(true);

    window.setScene(new Scene(scrollPane, 520, 620));
    window.show();
}
private String safeText(String value) {
    return value == null || value.isBlank() ? "—" : value;
}

private void showMessage(String message) {
    Alert alert = new Alert(AlertType.INFORMATION);
    alert.setTitle("البحث عن طالب");
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
}
    private void loadImage(ImageView imageView, String path) {

        var resource = getClass().getResource(path);

        if (resource == null) {
            System.out.println("لم يتم العثور على الصورة: " + path);
            return;
        }

        imageView.setImage(new Image(resource.toExternalForm()));

        System.out.println("تم تحميل الصورة بنجاح: " + path);
    }
    private void openPage(String path) {
    openInNewWindow(path, "تسجيل طالب جديد");
}

private void openPagetwo(String path) {
    openInNewWindow(path, "عرض بيانات الطلاب");
}

private void openInNewWindow(String path, String title) {
    try {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource(path)
        );

        Parent root = loader.load();

        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(new Scene(root));
        stage.setMinWidth(900);
        stage.setMinHeight(600);

        stage.show();

    } catch (IOException | NullPointerException e) {
        e.printStackTrace();
    }
 
}
}