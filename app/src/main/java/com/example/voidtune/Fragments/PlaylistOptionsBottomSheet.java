package com.example.voidtune.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.voidtune.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class PlaylistOptionsBottomSheet extends BottomSheetDialogFragment {

    private OnOptionSelectedListener listener;

    public interface OnOptionSelectedListener {
        void onEditPlaylist();
        void onDeletePlaylist();
    }

    public void setOnOptionSelectedListener(OnOptionSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_playlist, container, false);

        TextView editPlaylist = view.findViewById(R.id.editPlaylist);
        TextView deletePlaylist = view.findViewById(R.id.deletePlaylist);

        editPlaylist.setOnClickListener(v -> {
            if (listener != null) listener.onEditPlaylist();
            dismiss();
        });

        deletePlaylist.setOnClickListener(v -> {
            if (listener != null) listener.onDeletePlaylist();
            dismiss();
        });

        return view;
    }
}