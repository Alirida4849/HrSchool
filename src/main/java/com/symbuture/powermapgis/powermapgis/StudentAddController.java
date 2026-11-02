
package com.symbuture.powermapgis.powermapgis;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

public class StudentAddController implements Initializable {

    @FXML private TextField txtFullName;
    @FXML private TextField txtMotherName;
    @FXML private TextField txtParentPhone;
    @FXML private TextField txtNationalId;
    @FXML private TextField txtResidenceCard;

    @FXML private ComboBox<String> cmbClass;
    @FXML private ComboBox<String> cmbSection;

    @FXML private DatePicker dateBirth;
    @FXML private ImageView imgStudent;
    @FXML private TextArea txtNotes;

    @FXML private Button btnChooseImage;
    @FXML private Button btnDeleteImage;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    private File selectedImage;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // تهيئة قاعدة البيانات
        try {
            Database.initialize();
        } catch (SQLException e) {
            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "تعذر الاتصال بقاعدة البيانات:\n"
                    + e.getMessage()
            );

            btnSave.setDisable(true);
            return;
        }

        // إعداد الصفوف الدراسية
        cmbClass.setItems(FXCollections.observableArrayList(
                "الأول", "الثاني", "الثالث",
                "الرابع", "الخامس", "السادس"
        ));

        // إعداد الشعب
        cmbSection.setItems(FXCollections.observableArrayList(
                "أ", "ب", "ج", "د"
        ));

        // ربط الأزرار بالدوال
        btnChooseImage.setOnAction(event -> chooseImage());
        btnDeleteImage.setOnAction(event -> deleteImage());
        btnSave.setOnAction(event -> saveStudent());
        btnCancel.setOnAction(event -> clearFields());
    }

    // اختيار صورة الطالب
    private void chooseImage() {

        FileChooser chooser = new FileChooser();
        chooser.setTitle("اختيار صورة الطالب");

        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "ملفات الصور",
                        "*.png", "*.jpg", "*.jpeg"
                )
        );

        File file = chooser.showOpenDialog(
                btnChooseImage.getScene().getWindow()
        );

        if (file == null) {
            return;
        }

        try {
            Image image = new Image(file.toURI().toString());

            if (image.isError()) {
                showMessage(
                        Alert.AlertType.ERROR,
                        "تعذر فتح الصورة المحددة"
                );
                return;
            }

            selectedImage = file;
            imgStudent.setImage(image);

        } catch (Exception e) {
            showMessage(
                    Alert.AlertType.ERROR,
                    "حدث خطأ أثناء تحميل الصورة:\n"
                    + e.getMessage()
            );
        }
    }

    // حذف الصورة من الواجهة
    private void deleteImage() {
        selectedImage = null;
        imgStudent.setImage(null);
    }

    // حفظ بيانات الطالب
    private void saveStudent() {

        String fullName = txtFullName.getText().trim();
        String motherName = txtMotherName.getText().trim();
        String phone = txtParentPhone.getText().trim();
        String nationalId = txtNationalId.getText().trim();
        String residenceCard = txtResidenceCard.getText().trim();

        String studentClass = cmbClass.getValue();
        String section = cmbSection.getValue();
        LocalDate birthDate = dateBirth.getValue();

        String notes = txtNotes.getText().trim();

        // التحقق من الحقول المطلوبة
        if (fullName.isEmpty()
                || motherName.isEmpty()
                || studentClass == null
                || section == null
                || birthDate == null) {

            showMessage(
                    Alert.AlertType.WARNING,
                    "يرجى ملء الاسم واسم الأم والصف والشعبة وتاريخ الميلاد"
            );
            return;
        }

        // التحقق من تاريخ الميلاد
        if (birthDate.isAfter(LocalDate.now())) {
            showMessage(
                    Alert.AlertType.WARNING,
                    "تاريخ الميلاد لا يمكن أن يكون في المستقبل"
            );
            return;
        }

        // مسار الصورة
        String imagePath = selectedImage != null
                ? selectedImage.getAbsolutePath()
                : null;

        // أمر SQL لحفظ البيانات
        String sql = """
                INSERT INTO students (
                    full_name,
                    mother_name,
                    parent_phone,
                    national_id,
                    residence_card,
                    student_class,
                    section,
                    birth_date,
                    image_path,
                    notes
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, fullName);
            pstmt.setString(2, motherName);
            pstmt.setString(3, phone);
            pstmt.setString(4, nationalId);
            pstmt.setString(5, residenceCard);
            pstmt.setString(6, studentClass);
            pstmt.setString(7, section);
            pstmt.setString(8, birthDate.toString());
            pstmt.setString(9, imagePath);
            pstmt.setString(10, notes);

            int rows = pstmt.executeUpdate();

            if (rows > 0) {

                Alert alert = new Alert(
                        Alert.AlertType.INFORMATION
                );

                alert.setTitle("إدارة الطلاب");
                alert.setHeaderText(
                        "تم حفظ بيانات الطالب بنجاح"
                );

                alert.setContentText(
                        "الاسم: " + fullName
                        + "\nاسم الأم: " + motherName
                        + "\nالصف: " + studentClass
                        + "\nالشعبة: " + section
                        + "\nتاريخ الميلاد: " + birthDate
                        + "\nهاتف ولي الأمر: " + phone
                        + "\nالبطاقة الوطنية: " + nationalId
                        + "\nبطاقة السكن: " + residenceCard
                        + "\nالملاحظات: " + notes
                        + "\n\nتم حفظ البيانات في قاعدة البيانات."
                );

                alert.getDialogPane().setPrefWidth(450);
                alert.showAndWait();

                // إفراغ الحقول بعد نجاح الحفظ
                resetFields();
            }

        } catch (SQLException e) {

            e.printStackTrace();

            showMessage(
                    Alert.AlertType.ERROR,
                    "حدث خطأ أثناء حفظ بيانات الطالب:\n"
                    + e.getMessage()
            );
        }
    }

    // زر الإلغاء
    private void clearFields() {

        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION
        );

        alert.setTitle("إلغاء");
        alert.setHeaderText(
                "هل تريد إفراغ جميع الحقول؟"
        );
        alert.setContentText(
                "لن يتم حفظ البيانات الموجودة حالياً."
        );

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent()
                && result.get() == ButtonType.OK) {

            resetFields();
        }
    }

    // إفراغ الحقول
    private void resetFields() {

        txtFullName.clear();
        txtMotherName.clear();
        txtParentPhone.clear();
        txtNationalId.clear();
        txtResidenceCard.clear();

        cmbClass.getSelectionModel().clearSelection();
        cmbSection.getSelectionModel().clearSelection();

        dateBirth.setValue(null);
        txtNotes.clear();

        deleteImage();
    }

    // رسائل التنبيه
    private void showMessage(
            Alert.AlertType type,
            String message) {

        Alert alert = new Alert(type);
        alert.setTitle("إدارة الطلاب");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
