package jp.ac.meijou.android.nanndatteii.ui.home;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import jp.ac.meijou.android.nanndatteii.databinding.FragmentHomeBinding;
import jp.ac.meijou.android.nanndatteii.R;

public class HomeFragment extends Fragment
{
    private FragmentHomeBinding binding;
    private Uri photoUri;
    private String memoText = ""; // [必須] メモ内容を保持する変数

    // [必須] Tagsの値を保持する変数
    private String currentTag;

    // カメラ起動用ランチャー
    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState)
    {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textHome;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);


        // カメラ結果受け取り
        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK)
                {
                    Toast.makeText(requireContext(), "写真を保存しました。", Toast.LENGTH_SHORT).show();
                }
            }
        );

        // パーミッションの確認とリクエスト（CAMERAのみ）
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(
                new String[]{Manifest.permission.CAMERA},
                100
            );
        }

        Button openCameraButton = binding.OpenCamera;

        openCameraButton.setOnClickListener(v -> {
            // Download/MyAppPhotos フォルダに保存
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String folderName = requireContext().getString(R.string.photo_folder_name);
            String relativePath = "Download/" + folderName;

            File photoDir = new File(downloadsDir, folderName);

            if (!photoDir.exists()) {
                photoDir.mkdirs();
            }

            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, "photo_" + System.currentTimeMillis() + ".jpg");
            values.put(MediaStore.Downloads.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Downloads.RELATIVE_PATH, relativePath); // メモと同じ場所

            photoUri = requireContext().getContentResolver().insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            cameraLauncher.launch(intent);
        });

        // 初期値をstrings.xmlから取得
        currentTag = requireContext().getString(R.string.Tags);

        // Add_Tagボタンの処理
        binding.AddTag.setOnClickListener(v -> {
            // 例: EditTextからタグ名を取得して更新
            String newTag = binding.TextTag.getText().toString().trim();
            if (!newTag.isEmpty()) {
                currentTag = newTag;
                Toast.makeText(requireContext(), "タグを更新しました: " + currentTag, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "タグ名を入力してください", Toast.LENGTH_SHORT).show();
            }
        });

        // SendButtonの処理
        binding.SendButton.setOnClickListener(v -> {
            memoText = binding.Textbox.getText().toString();

            // [必須] フォルダ名・タグ名取得
            String folderName = requireContext().getString(R.string.photo_folder_name);
            String tagName = currentTag; // ここでcurrentTagを使用

            // [必須] サブフォルダパス生成
            String relativePath = "Download/" + folderName + "/" + tagName;

            // [必須] サブフォルダも物理的に作成（API 29未満用、API 29以上はRELATIVE_PATHで自動作成）
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File tagDir = new File(downloadsDir, folderName + "/" + tagName);
            if (!tagDir.exists()) {
                tagDir.mkdirs();
            }

            // [必須] ファイル名生成
            String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            String fileName = tagName + "_" + timeStamp + ".txt";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
            values.put(MediaStore.Downloads.RELATIVE_PATH, relativePath); // サブフォルダに保存

            Uri textUri = null;
            try {
                textUri = requireContext().getContentResolver().insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            } catch (Exception e) {
                Log.e("HomeFragment", "ファイル挿入エラー", e);
            }

            if (textUri != null)
            {
                try (OutputStream os = requireContext().getContentResolver().openOutputStream(textUri))
                {
                    os.write(memoText.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                    Toast.makeText(requireContext(), "メモを保存しました。", Toast.LENGTH_SHORT).show();
                }
                catch (Exception e)
                {
                    Log.e("HomeFragment", "メモ保存エラー", e);
                    Toast.makeText(requireContext(), "メモの保存に失敗しました: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
            else
            {
                Toast.makeText(requireContext(), "メモの保存先を作成できませんでした。", Toast.LENGTH_SHORT).show();
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}