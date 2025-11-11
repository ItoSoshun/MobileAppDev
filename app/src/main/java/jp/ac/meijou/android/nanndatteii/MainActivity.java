package jp.ac.meijou.android.nanndatteii;

import android.os.Bundle;
import android.os.Environment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import jp.ac.meijou.android.nanndatteii.databinding.ActivityMainBinding;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        // [必須] ファイル保存処理を呼び出す
        saveSampleFile();
    }

    private void saveSampleFile() {
        // [必須] フォルダ名・タグ名・日時をstrings.xmlから取得
        String photoFolderName = getString(R.string.photo_folder_name);
        String tagName = getString(R.string.Tags);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = tagName + "_" + timeStamp + ".txt";

        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File photoDir = new File(downloadsDir, photoFolderName);
        if (!photoDir.exists()) photoDir.mkdirs();
        File saveFile = new File(photoDir, fileName);

        // [必須] ファイル保存処理例（テキスト書き込み）
        try {
            java.io.FileWriter writer = new java.io.FileWriter(saveFile);
            writer.write("サンプルデータを書き込みます。\n");
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}