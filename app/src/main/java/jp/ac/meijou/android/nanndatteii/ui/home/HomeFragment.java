package jp.ac.meijou.android.nanndatteii.ui.home;


import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jp.ac.meijou.android.nanndatteii.databinding.FragmentHomeBinding;
import jp.ac.meijou.android.nanndatteii.R;
import jp.ac.meijou.android.nanndatteii.db.entity.Item;
import jp.ac.meijou.android.nanndatteii.db.entity.ItemFile;
import jp.ac.meijou.android.nanndatteii.db.entity.Tag;
import jp.ac.meijou.android.nanndatteii.repository.ItemRepository;
import jp.ac.meijou.android.nanndatteii.repository.TagRepository;
import jp.ac.meijou.android.nanndatteii.storage.FileStorageManager;
import jp.ac.meijou.android.nanndatteii.storage.SavedFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class HomeFragment extends Fragment
{
    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private Uri photoUri;
    private String memoText = ""; // [必須] メモ内容を保持する変数

    // currentTagはAdd_Tagボタンで更新される変数とする
    private String currentTag;
    private Long currentTagId; // 選択中のタグID

    // カメラ起動用ランチャー
    private ActivityResultLauncher<Intent> cameraLauncher;

    // 新しいRepository層
    private TagRepository tagRepository;
    private ItemRepository itemRepository;
    private FileStorageManager fileStorageManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState)
    {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Repositoryとストレージマネージャーを初期化
        tagRepository = new TagRepository(requireContext());
        itemRepository = new ItemRepository(requireContext());
        fileStorageManager = new FileStorageManager(requireContext());

        // タグリストをSpinnerで表示
        final List<Tag>[] tagsList = new List[]{new ArrayList<>()};
        final ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            new ArrayList<>()
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.HomeTagSet.setAdapter(spinnerAdapter);

        // TagRepositoryからタグ一覧を取得してSpinnerに反映
        tagRepository.getAllTags().observe(getViewLifecycleOwner(), tags -> {
            tagsList[0] = tags;
            spinnerAdapter.clear();
            for (Tag tag : tags) {
                spinnerAdapter.add(tag.getName());
            }
            spinnerAdapter.notifyDataSetChanged();
        });


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

        // パーミッションの確認とリクエスト
        checkAndRequestPermissions();

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

            // 新しい実装: TagRepositoryを使用
            Tag newTag = new Tag();
            newTag.setName(tagName);
            newTag.setCreatedAt(System.currentTimeMillis());

            tagRepository.insertTag(newTag, new TagRepository.OnTagInsertedListener() {
                @Override
                public void onSuccess(long tagId) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "タグを追加しました", Toast.LENGTH_SHORT).show();
                        binding.TextTag.setText("");
                        // LiveDataが自動更新するのでSpinnerは自動で更新される
                    });
                }

                @Override
                public void onError(Exception e) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "エラー: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        // SendButton: 写真とメモを一つのアイテムとして保存
        binding.SendButton.setOnClickListener(v -> {
            saveNewItem();
        });

        ImageButton openFolderImageButton = binding.OpenFolder;
        openFolderImageButton.setOnClickListener(v -> {
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
            if (binding.HomeTagSet.getSelectedItem() == null) {
                Toast.makeText(getContext(), "タグを選択してください", Toast.LENGTH_SHORT).show();
                return;
            }

            String selectedTagName = binding.HomeTagSet.getSelectedItem().toString();
            tagRepository.deleteTagByName(selectedTagName, new TagRepository.OnTagDeletedListener() {
                @Override
                public void onSuccess() {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "タグを削除しました", Toast.LENGTH_SHORT).show();
                        currentTagId = null;
                        currentTag = null;
                    });
                }

                @Override
                public void onError(Exception e) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "削除エラー: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        binding.HomeTagSet.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < tagsList[0].size()) {
                    Tag selectedTag = tagsList[0].get(position);
                    currentTag = selectedTag.getName();
                    currentTagId = selectedTag.getId();
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                currentTagId = null;
                currentTag = null;
            }
        });

        return root;
    }

    /**
     * 新しいアイテムを保存（写真 + メモ）
     */
    private void saveNewItem() {
        Log.d(TAG, "saveNewItem: 開始");
        String memoText = binding.Textbox.getText() != null ? binding.Textbox.getText().toString() : "";
        Log.d(TAG, "saveNewItem: メモテキスト: " + memoText);
        Log.d(TAG, "saveNewItem: 選択中のタグID: " + currentTagId);

        // タグが選択されていない場合
        if (currentTagId == null) {
            Toast.makeText(requireContext(), "タグを選択してください", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "saveNewItem: タグが選択されていません。");
            return;
        }

        // 写真もメモも空の場合
        if (photoUri == null && memoText.trim().isEmpty()) {
            Toast.makeText(requireContext(), "写真またはメモを入力してください", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "saveNewItem: 写真もメモも空です。");
            return;
        }

        List<ItemFile> files = new ArrayList<>();
        List<Long> tagIds = new ArrayList<>();
        tagIds.add(currentTagId);

        try {
            // 1. 写真を保存
            if (photoUri != null) {
                Log.d(TAG, "saveNewItem: 写真を保存します。URI: " + photoUri);
                InputStream photoStream = requireContext().getContentResolver().openInputStream(photoUri);
                if (photoStream != null) {
                    SavedFile savedPhoto = fileStorageManager.saveFile(photoStream, "image/jpeg");
                    Log.d(TAG, "saveNewItem: 写真の保存成功: " + savedPhoto.getRelativePath());

                    ItemFile photoFile = new ItemFile();
                    photoFile.setFilePath(savedPhoto.getRelativePath());
                    photoFile.setFileName(savedPhoto.getFileName());
                    photoFile.setFileType("IMAGE");
                    photoFile.setFileSize(savedPhoto.getFileSize());
                    photoFile.setMimeType(savedPhoto.getMimeType());
                    photoFile.setCreatedAt(System.currentTimeMillis());
                    files.add(photoFile);

                    photoStream.close();
                } else {
                    Log.w(TAG, "saveNewItem: photoStreamがnullです。");
                }
            }

            // 2. メモを保存
            if (!memoText.trim().isEmpty()) {
                Log.d(TAG, "saveNewItem: メモを保存します。");
                byte[] textBytes = memoText.getBytes(StandardCharsets.UTF_8);
                InputStream textStream = new ByteArrayInputStream(textBytes);
                SavedFile savedText = fileStorageManager.saveFile(textStream, "text/plain");
                Log.d(TAG, "saveNewItem: メモの保存成功: " + savedText.getRelativePath());

                ItemFile textFile = new ItemFile();
                textFile.setFilePath(savedText.getRelativePath());
                textFile.setFileName(savedText.getFileName());
                textFile.setFileType("TEXT");
                textFile.setFileSize(savedText.getFileSize());
                textFile.setMimeType(savedText.getMimeType());
                textFile.setCreatedAt(System.currentTimeMillis());
                files.add(textFile);

                textStream.close();
            }

            // 3. アイテムを作成
            Item item = new Item();
            item.setDescription(memoText);
            item.setCreatedAt(System.currentTimeMillis());
            item.setUpdatedAt(System.currentTimeMillis());
            Log.d(TAG, "saveNewItem: ItemRepository.createItemを呼び出します。");

            itemRepository.createItem(item, files, tagIds, new ItemRepository.OnItemCreatedListener() {
                @Override
                public void onSuccess(long itemId) {
                    Log.d(TAG, "onSuccess: アイテムの保存に成功。ItemID: " + itemId);
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "保存しました", Toast.LENGTH_SHORT).show();
                        // UIをリセット
                        binding.Textbox.setText("");
                        photoUri = null;
                    });
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "onError: アイテムの保存に失敗。", e);
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "保存に失敗しました: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });

        } catch (IOException e) {
            Log.e(TAG, "saveNewItem: ファイル保存中にIOExceptionが発生しました。", e);
            Toast.makeText(requireContext(), "ファイル保存エラー: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * パーミッションの確認とリクエスト
     */
    private void checkAndRequestPermissions() {
        java.util.ArrayList<String> permissionsToRequest = new java.util.ArrayList<>();

        // カメラパーミッション
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA);
        }

        // 写真読み取りパーミッション（カメラで撮影した写真を読み込むため）
        // Android 13以降
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        }
        // Android 12以下
        else if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.S_V2) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        // パーミッションリクエスト
        if (!permissionsToRequest.isEmpty()) {
            requestPermissions(
                permissionsToRequest.toArray(new String[0]),
                100
            );
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
