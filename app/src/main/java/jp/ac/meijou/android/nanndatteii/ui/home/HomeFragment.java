package jp.ac.meijou.android.nanndatteii.ui.home;

import static android.net.Uri.fromFile;

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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jp.ac.meijou.android.nanndatteii.databinding.FragmentHomeBinding;
import jp.ac.meijou.android.nanndatteii.R;
import jp.ac.meijou.android.nanndatteii.db.AppDatabaseHelper;

public class HomeFragment extends Fragment
{
    private FragmentHomeBinding binding;
    private Uri photoUri;
    private String memoText = ""; // [必須] メモ内容を保持する変数

    // currentTagはAdd_Tagボタンで更新される変数とする
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

        // DBからタグ一覧を読み込み、Spinnerにセットする（adapterを後で再利用）
        final AppDatabaseHelper dbHelper = new AppDatabaseHelper(requireContext());
        List<String> initialTags = dbHelper.getAllTags();
        if (initialTags == null) initialTags = new java.util.ArrayList<>();
        final ArrayAdapter<String>[] spinnerAdapter = new ArrayAdapter[1];
        spinnerAdapter[0] = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            initialTags
        );
        spinnerAdapter[0].setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.HomeTagSet.setAdapter(spinnerAdapter[0]);


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

        ImageButton openCameraImageButton = binding.OpenCamera;

        openCameraImageButton.setOnClickListener(v -> {
            // [必須] Tags名のサブフォルダに保存
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String folderName = requireContext().getString(R.string.photo_folder_name);
            String tagName = currentTag; // 最新のタグ名
            String relativePath = "Download/" + folderName + "/" + tagName;

            File photoDir = new File(downloadsDir, folderName + "/" + tagName);
            if (!photoDir.exists()) {
                photoDir.mkdirs();
            }

            String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            String fileName = tagName + "_" + timeStamp + ".jpg";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Downloads.RELATIVE_PATH, relativePath); // [必須] Tags名のサブフォルダ

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
            String tagName = binding.TextTag.getText().toString().trim();
            if (tagName.isEmpty()) {
                Toast.makeText(requireContext(), "タグ名を入力してください", Toast.LENGTH_SHORT).show();
                return;
            }

            dbHelper.insertTag(tagName);

            // プルダウンを最新化（adapterを使い回す）
            List<String> newTags = dbHelper.getAllTags();
            if (newTags == null) newTags = new java.util.ArrayList<>();
            spinnerAdapter[0].clear();
            spinnerAdapter[0].addAll(newTags);
            spinnerAdapter[0].notifyDataSetChanged();
            int pos = spinnerAdapter[0].getPosition(tagName);
            if (pos >= 0) binding.HomeTagSet.setSelection(pos);

            Toast.makeText(requireContext(), "タグを追加しました", Toast.LENGTH_SHORT).show();
            binding.TextTag.setText("");
        });

        // SendButton: HomeTagSet に表示されたタグ名のフォルダへ Textbox の内容をテキストファイルとして保存する
        binding.SendButton.setOnClickListener(v -> {
            // Spinner の選択優先（HomeTagSet に表示されたタグ名を使う）
            String selectedTag = null;
            if (binding.HomeTagSet.getSelectedItem() != null) {
                selectedTag = binding.HomeTagSet.getSelectedItem().toString();
            }
            if (selectedTag == null || selectedTag.trim().isEmpty()) {
                // fallback to currentTag
                selectedTag = (currentTag != null) ? currentTag : requireContext().getString(R.string.Tags);
            }

            String memo = binding.Textbox.getText() != null ? binding.Textbox.getText().toString() : "";

            String folderName = requireContext().getString(R.string.photo_folder_name);
            String relativePath = "Download/" + folderName + "/" + selectedTag;

            String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            String fileName = selectedTag + "_" + timeStamp + ".txt";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
            values.put(MediaStore.Downloads.RELATIVE_PATH, relativePath);

            Uri textUri = null;
            try {
                textUri = requireContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            } catch (Exception e) {
                Log.e("HomeFragment", "MediaStore insert error", e);
            }

            if (textUri == null) {
                Toast.makeText(requireContext(), "保存先の作成に失敗しました。", Toast.LENGTH_SHORT).show();
                return;
            }

            try (OutputStream os = requireContext().getContentResolver().openOutputStream(textUri)) {
                os.write(memo.getBytes(StandardCharsets.UTF_8));
                os.flush();
                Toast.makeText(requireContext(), "メモを保存しました: " + folderName + "/" + selectedTag, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e("HomeFragment", "メモ保存エラー", e);
                Toast.makeText(requireContext(), "メモの保存に失敗しました: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        Button openFolderButton = binding.OpenFolder;
        openFolderButton.setOnClickListener(v -> {
            String folderName = requireContext().getString(R.string.photo_folder_name);
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File targetDir = new File(downloadsDir, folderName);

            // [必須] FileProvider経由でUriを取得
            Uri folderUri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                targetDir
            );

            // 以下は今まで通り
            Intent defaultIntent = new Intent(Intent.ACTION_VIEW);
            defaultIntent.setDataAndType(folderUri, "*/*");
            defaultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (defaultIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(defaultIntent);
                return;
            }

            Intent fileIntent = new Intent(Intent.ACTION_VIEW);
            fileIntent.setDataAndType(folderUri, "*/*");
            fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            fileIntent.setPackage("com.google.android.documentsui");

            if (fileIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(fileIntent);
                return;
            }

            Intent filesByGoogleIntent = new Intent(Intent.ACTION_VIEW);
            filesByGoogleIntent.setDataAndType(folderUri, "*/*");
            filesByGoogleIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            filesByGoogleIntent.setPackage("com.google.android.apps.nbu.files");

            if (filesByGoogleIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(filesByGoogleIntent);
                return;
            }

            Intent playStoreIntent = new Intent(Intent.ACTION_VIEW);
            playStoreIntent.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.nbu.files"));
            try {
                startActivity(playStoreIntent);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "フォルダを開けるアプリがありません", Toast.LENGTH_SHORT).show();
            }
        });

        binding.DeleteTag.setOnClickListener(v -> {
            String selectedTag = binding.HomeTagSet.getSelectedItem().toString();
            if (selectedTag.isEmpty()) {
                Toast.makeText(getContext(), "タグを選択してください", Toast.LENGTH_SHORT).show();
                return;
            }

            dbHelper.deleteTag(selectedTag);

            // プルダウンを最新化（adapterを使い回す）
            List<String> tags = dbHelper.getAllTags();
            if (tags == null) tags = new java.util.ArrayList<>();
            spinnerAdapter[0].clear();
            spinnerAdapter[0].addAll(tags);
            spinnerAdapter[0].notifyDataSetChanged();

            Toast.makeText(getContext(), "タグを削除しました", Toast.LENGTH_SHORT).show();
        });

        binding.HomeTagSet.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                currentTag = parent.getItemAtPosition(position).toString();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // 何もしない
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