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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jp.ac.meijou.android.nanndatteii.R;
import jp.ac.meijou.android.nanndatteii.databinding.FragmentDashboardBinding;

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
        RecyclerView recyclerView = root.findViewById(R.id.fileRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Download/MyAppPhotos フォルダのファイル一覧取得
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MyAppPhotos");
        List<File> fileList = (dir.exists() && dir.isDirectory())
                ? Arrays.asList(dir.listFiles())
                : Collections.emptyList();

        FileAdapter adapter = new FileAdapter(getContext(), fileList);
        recyclerView.setAdapter(adapter);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}