package jp.ac.meijou.android.nanndatteii.ui.dashboard;

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jp.ac.meijou.android.nanndatteii.R;
import jp.ac.meijou.android.nanndatteii.databinding.FragmentDashboardBinding;
import jp.ac.meijou.android.nanndatteii.db.AppDatabaseHelper;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class DashboardFragment extends Fragment
{
    private FragmentDashboardBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState)
    {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        RecyclerView recyclerView = binding.fileRecyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        FileAdapter fileAdapter = new FileAdapter(getContext(), new ArrayList<>());
        recyclerView.setAdapter(fileAdapter);

        // [必須] データベースからタグ一覧を取得してSpinnerにセット
        AppDatabaseHelper dbHelper = new AppDatabaseHelper(requireContext());
        List<String> tags = dbHelper.getAllTags();

        // [必須] 先頭に「ホーム」を追加
        List<String> spinnerItems = new java.util.ArrayList<>();
        spinnerItems.add("ホーム");
        spinnerItems.addAll(tags);

        Spinner setTagsSpinner = binding.SetTags;

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            spinnerItems
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        setTagsSpinner.setAdapter(spinnerAdapter);

        // OpenTagsボタンの処理
        binding.OpenTags.setOnClickListener(v -> {
            String selectedTag = binding.SetTags.getSelectedItem().toString();
            String photoFolderName = getContext().getString(R.string.photo_folder_name);

            File baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File targetDir;
            if ("ホーム".equals(selectedTag)) {
                targetDir = new File(baseDir, photoFolderName);
            } else {
                targetDir = new File(baseDir, photoFolderName + "/" + selectedTag);
            }

            File[] filesArray = (targetDir.exists() && targetDir.isDirectory()) ? targetDir.listFiles() : null;
            List<File> selectedFileList = (filesArray != null) ? Arrays.asList(filesArray) : new ArrayList<>();

            fileAdapter.setFiles(selectedFileList);
            // fileAdapter.notifyDataSetChanged(); ← setFiles内で呼んでいれば不要

        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}