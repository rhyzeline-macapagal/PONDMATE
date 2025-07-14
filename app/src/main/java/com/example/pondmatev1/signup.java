package com.example.pondmatev1;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class signup extends AppCompatActivity {

    EditText fullname, username1, password1, Cpassword, address;

    private RadioGroup rgUserType;
    private DatabaseHelper dbHelper;

    Button signupButton;
    TextView logindirect;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase(); // Forces DB creation
        db.close();

        // Initialize EditText fields
        fullname = findViewById(R.id.fullname_input);
        username1 = findViewById(R.id.username_input1);
        password1 = findViewById(R.id.password_input1);
        Cpassword = findViewById(R.id.password_input2);
        address = findViewById(R.id.address_input);
        rgUserType = findViewById(R.id.rgUserType);
        signupButton = findViewById(R.id.signup_button);
        logindirect = findViewById(R.id.loginalready);


        password1.setTransformationMethod(PasswordTransformationMethod.getInstance());
        Cpassword.setTransformationMethod(PasswordTransformationMethod.getInstance());

        setPasswordEyeIcon(password1, R.drawable.hidden);
        setPasswordEyeIcon(Cpassword, R.drawable.hidden);

        signupButton.setOnClickListener(v -> {
            String username = username1.getText().toString().trim();
            String password = password1.getText().toString().trim();
            String confirmPassword = Cpassword.getText().toString().trim();
            String fname = fullname.getText().toString().trim();
            String address1 = address.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() ||
                    fname.isEmpty() || address1.isEmpty()) {
                Toast.makeText(signup.this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(signup.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            int selectedId = rgUserType.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(signup.this, "Please select a user type", Toast.LENGTH_SHORT).show();
                return;
            }

            String userType = (selectedId == R.id.rbOwner) ? "Owner" : "Caretaker";

            new Thread(() -> {
                try {
                    URL url = new URL("https://pondmate.alwaysdata.net/register_user.php"); // your PHP URL
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                    String postData = "username=" + URLEncoder.encode(username, "UTF-8") +
                            "&password=" + URLEncoder.encode(password, "UTF-8") +
                            "&fullname=" + URLEncoder.encode(fname, "UTF-8") +
                            "&address=" + URLEncoder.encode(address1, "UTF-8") +
                            "&usertype=" + URLEncoder.encode(userType, "UTF-8");

                    OutputStream os = conn.getOutputStream();
                    os.write(postData.getBytes());
                    os.flush();
                    os.close();

                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String response = in.readLine();
                    in.close();

                    runOnUiThread(() -> {
                        switch (response) {
                            case "success":
                                Toast.makeText(signup.this, "✅ Registration successful!", Toast.LENGTH_SHORT).show();

                                // Optionally insert into local SQLite
                                dbHelper.addUser(username, password, fname, address1, userType);

                                Intent intent = new Intent(signup.this, login.class);
                                startActivity(intent);
                                finish();
                                break;

                            case "exists":
                                Toast.makeText(signup.this, "❗ Username already exists", Toast.LENGTH_LONG).show();
                                break;

                            case "missing":
                                Toast.makeText(signup.this, "⚠️ Missing required data", Toast.LENGTH_LONG).show();
                                break;

                            default:
                                Toast.makeText(signup.this, "❌ Server error: " + response, Toast.LENGTH_LONG).show();
                                break;
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(signup.this, "🚫 Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            }).start();
        });

        signupButton.setOnClickListener(v -> {
            String username = username1.getText().toString().trim();
            String password = password1.getText().toString().trim();
            String confirmPassword = Cpassword.getText().toString().trim();
            String fname = fullname.getText().toString().trim();
            String address1 = address.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() ||
                    fname.isEmpty() || address1.isEmpty()) {
                Toast.makeText(signup.this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(signup.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            int selectedId = rgUserType.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(signup.this, "Please select a user type", Toast.LENGTH_SHORT).show();
                return;
            }

            String userType = (selectedId == R.id.rbOwner) ? "Owner" : "Caretaker";

            new Thread(() -> {
                try {
                    URL url = new URL("https://pondmate.alwaysdata.net/register_user.php"); // your PHP URL
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                    String postData = "username=" + URLEncoder.encode(username, "UTF-8") +
                            "&password=" + URLEncoder.encode(password, "UTF-8") +
                            "&fullname=" + URLEncoder.encode(fname, "UTF-8") +
                            "&address=" + URLEncoder.encode(address1, "UTF-8") +
                            "&usertype=" + URLEncoder.encode(userType, "UTF-8");

                    OutputStream os = conn.getOutputStream();
                    os.write(postData.getBytes());
                    os.flush();
                    os.close();

                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String response = in.readLine();
                    in.close();

                    runOnUiThread(() -> {
                        switch (response) {
                            case "success":
                                Toast.makeText(signup.this, "✅ Registration successful!", Toast.LENGTH_SHORT).show();

                                // Optionally insert into local SQLite
                                dbHelper.addUser(username, password, fname, address1, userType);

                                Intent intent = new Intent(signup.this, login.class);
                                startActivity(intent);
                                finish();
                                break;

                            case "exists":
                                Toast.makeText(signup.this, "❗ Username already exists", Toast.LENGTH_LONG).show();
                                break;

                            case "missing":
                                Toast.makeText(signup.this, "⚠️ Missing required data", Toast.LENGTH_LONG).show();
                                break;

                            default:
                                Toast.makeText(signup.this, "❌ Server error: " + response, Toast.LENGTH_LONG).show();
                                break;
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(signup.this, "🚫 Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            }).start();
        });


        // pag set ng pass visib sa et click 1
        password1.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (password1.getRight() - password1.getCompoundPaddingRight())) {
                    togglePasswordVisibility(password1);
                    return true;
                }
            }
            return false;
        });

        // pag set ng confirm pass visib sa et click
        Cpassword.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (Cpassword.getRight() - Cpassword.getCompoundPaddingRight())) {
                    togglePasswordVisibility(Cpassword);
                    return true;
                }
            }
            return false;
        });


        logindirect.setOnClickListener(v -> {
            Intent intent = new Intent(signup.this, login.class);
            startActivity(intent);
        });
    }

    // Toggle password visibility and update eye icon
    public void togglePasswordVisibility(EditText editText) {
        if (editText.getTransformationMethod() instanceof PasswordTransformationMethod) {
            // Change to plain text
            editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            setPasswordEyeIcon(editText, R.drawable.eye_open);  // Set open eye icon
        } else {
            // Change to password text
            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            setPasswordEyeIcon(editText, R.drawable.hidden);  // Set closed eye icon
        }
    }



    // Helper method to set drawable icons for EditText
    private void setPasswordEyeIcon(EditText editText, int drawableResEnd) {
        editText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.lock_icon, // start (left)
                0,                    // top
                drawableResEnd,       // end (right)
                0                     // bottom
        );
    }

}
