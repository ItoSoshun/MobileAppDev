package jp.ac.meijou.android.nanndatteii.ui.dashboard;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jp.ac.meijou.android.nanndatteii.R;
import jp.ac.meijou.android.nanndatteii.databinding.FragmentDashboardBinding;
import jp.ac.meijou.android.nanndatteii.db.entity.Item;
import jp.ac.meijou.android.nanndatteii.db.entity.ItemFile;
import jp.ac.meijou.android.nanndatteii.db.entity.Tag;
import jp.ac.meijou.android.nanndatteii.repository.ItemRepository;
import jp.ac.meijou.android.nanndatteii.repository.TagRepository;
import jp.ac.meijou.android.nanndatteii.storage.FileStorageManager;

public class DashboardFragment extends Fragment {
    private FragmentDashboardBinding binding;
    private TagRepository tagRepository;
    private ItemRepository itemRepository;
    private FileStorageManager fileStorageManager;
    private ItemAdapter itemAdapter;
    private List<Tag> tagsList = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Repositoryとストレージマネージャーを初期化
        tagRepository = new TagRepository(requireContext());
        itemRepository = new ItemRepository(requireContext());
        fileStorageManager = new FileStorageManager(requireContext());

        // RecyclerViewの設定
        RecyclerView recyclerView = binding.fileRecyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        itemAdapter = new ItemAdapter(getContext(), item -> {
            // アイテムクリック時の処理
            openItemFiles(item);
        });
        recyclerView.setAdapter(itemAdapter);

        // Spinnerの設定
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            new ArrayList<>()
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.SetTags.setAdapter(spinnerAdapter);

        // タグ一覧を取得してSpinnerに反映
        tagRepository.getAllTags().observe(getViewLifecycleOwner(), tags -> {
            tagsList = tags;
            spinnerAdapter.clear();
            spinnerAdapter.add("すべて");
            for (Tag tag : tags) {
                spinnerAdapter.add(tag.getName());
            }
            spinnerAdapter.notifyDataSetChanged();
        });

        // 初期表示：すべてのアイテムを表示
        loadAllItems();

        // OpenTagsボタンの処理
        binding.OpenTags.setOnClickListener(v -> {
            if (binding.SetTags.getSelectedItem() == null) {
                return;
            }

            String selectedTagName = binding.SetTags.getSelectedItem().toString();
            int selectedPosition = binding.SetTags.getSelectedItemPosition();

            if (selectedPosition == 0 || "すべて".equals(selectedTagName)) {
                // すべてのアイテムを表示
                loadAllItems();
            } else {
                // 選択されたタグでフィルタリング
                Tag selectedTag = tagsList.get(selectedPosition - 1); // "すべて"の分を引く
                loadItemsByTag(selectedTag.getId());
            }
        });

        return root;
    }

    /**
     * すべてのアイテムを読み込む
     */
    private void loadAllItems() {
        itemRepository.getAllItems().observe(getViewLifecycleOwner(), items -> {
            itemAdapter.setItems(items);
        });
    }

    /**
     * タグでフィルタリングしたアイテムを読み込む
     */
    private void loadItemsByTag(long tagId) {
        itemRepository.getItemsByTag(tagId).observe(getViewLifecycleOwner(), items -> {
            itemAdapter.setItems(items);
        });
    }

    /**
     * アイテムのファイルを開く
     */
    private void openItemFiles(Item item) {
        itemRepository.getFilesByItemId(item.getId()).observe(getViewLifecycleOwner(), files -> {
            if (files == null || files.isEmpty()) {
                Toast.makeText(requireContext(), "ファイルがありません", Toast.LENGTH_SHORT).show();
                return;
            }

            // 最初のファイルを開く
            ItemFile firstFile = files.get(0);
            File file = fileStorageManager.getFile(firstFile.getFilePath());

            if (!file.exists()) {
                Toast.makeText(requireContext(), "ファイルが見つかりません", Toast.LENGTH_SHORT).show();
                return;
            }

            // 閲覧日時を更新
            itemRepository.updateLastViewed(item.getId());

            try {
                // FileProviderでUriを取得
                Uri fileUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    file
                );

                // 外部アプリで開く
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(fileUri, firstFile.getMimeType());
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(requireContext(), "ファイルを開けるアプリがありません", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(requireContext(), "ファイルを開けませんでした: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
