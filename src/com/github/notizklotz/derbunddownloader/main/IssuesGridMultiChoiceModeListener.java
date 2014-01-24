/*
 * Der Bund ePaper Downloader - App to download ePaper issues of the Der Bund newspaper
 * Copyright (C) 2013 Adrian Gygax
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see {http://www.gnu.org/licenses/}.
 */

package com.github.notizklotz.derbunddownloader.main;

import android.app.DownloadManager;
import android.content.Context;
import android.view.*;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.GridView;
import com.github.notizklotz.derbunddownloader.R;

class IssuesGridMultiChoiceModeListener implements AbsListView.MultiChoiceModeListener {

    private final Context context;
    private final GridView gridView;

    public IssuesGridMultiChoiceModeListener(Context context, GridView gridView) {
        this.context = context;
        this.gridView = gridView;
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position,
                                          long id, boolean checked) {
        View view = gridView.getChildAt(position);
        CheckBox issueSelectCheckBox = (CheckBox) view.findViewById(R.id.issueSelectCheckBox);
        if(issueSelectCheckBox.isChecked() != checked) {
            issueSelectCheckBox.setChecked(checked);
        }
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_delete:
                long[] checkedItemIds = gridView.getCheckedItemIds();
                DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                downloadManager.remove(checkedItemIds);
                mode.finish();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        assert inflater != null;
        inflater.inflate(R.menu.issue_context_menu, menu);
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }
}
