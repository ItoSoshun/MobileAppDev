package jp.ac.meijou.android.nanndatteii.ui.dashboard;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.List;

import jp.ac.meijou.android.nanndatteii.R;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder>
{
    private final List<File> fileList;
    private final Context context;

    public FileAdapter(Context context, List<File> fileList)
    {
        this.context = context;
        this.fileList = fileList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        File file = fileList.get(position);
        holder.fileName.setText(file.getName());

        // 拡張子でアイコンを変える例
        String ext = MimeTypeMap.getFileExtensionFromUrl(file.getName()).toLowerCase();
        if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png"))
        {
            holder.fileIcon.setImageResource(android.R.drawable.ic_menu_gallery);
        }
        else if (ext.equals("txt"))
        {
            holder.fileIcon.setImageResource(android.R.drawable.ic_menu_edit);
        }
        else
        {
            holder.fileIcon.setImageResource(android.R.drawable.ic_menu_help);
        }

        holder.itemView.setOnClickListener(v -> {
            try
            {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = Uri.fromFile(file);
                String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
                if (mime == null) mime = "*/*";
                intent.setDataAndType(uri, mime);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(intent);
            }
            catch (Exception e)
            {
                Toast.makeText(context, "ファイルを開けません", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount()
    {
        return fileList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        ImageView fileIcon;
        TextView fileName;

        ViewHolder(View itemView)
        {
            super(itemView);
            fileIcon = itemView.findViewById(R.id.fileIcon);
            fileName = itemView.findViewById(R.id.fileName);
        }
    }
}
