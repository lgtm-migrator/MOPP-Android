/*
 * Copyright 2017 Riigi Infosüsteemide Amet
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package ee.ria.EstEIDUtility.adapter;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.container.ContainerBuilder;
import ee.ria.EstEIDUtility.container.ContainerFacade;
import ee.ria.EstEIDUtility.fragment.ContainerDataFilesFragment;
import ee.ria.EstEIDUtility.util.FileUtils;
import ee.ria.EstEIDUtility.util.NotificationUtil;
import ee.ria.libdigidocpp.DataFile;

public class DataFilesAdapter extends ArrayAdapter<DataFile> {

    private ContainerFacade containerFacade;
    private ContainerDataFilesFragment containerDataFilesFragment;

    private NotificationUtil notificationUtil;
    private Activity activity;

    private static class ViewHolder {
        TextView fileName;
        TextView fileSize;
        ImageView removeFile;
    }

    public DataFilesAdapter(Activity activity, ContainerFacade containerFacade, ContainerDataFilesFragment containerDataFilesFragment) {
        super(activity, 0, containerFacade.getDataFiles());
        this.activity = activity;
        this.containerFacade = containerFacade;
        this.containerDataFilesFragment = containerDataFilesFragment;
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        notificationUtil = new NotificationUtil(activity);
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_container_datafiles, parent, false);

            viewHolder.fileSize = (TextView) convertView.findViewById(R.id.fileSize);
            viewHolder.fileName = (TextView) convertView.findViewById(R.id.fileName);
            viewHolder.removeFile = (ImageView) convertView.findViewById(R.id.removeFile);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final DataFile file = getItem(position);
        viewHolder.fileName.setText(file.fileName());

        String fileSizeText = getContext().getString(R.string.file_size);
        viewHolder.fileSize.setText(String.format(fileSizeText, FileUtils.getKilobytes(file.fileSize())));

        viewHolder.removeFile.setOnClickListener(new RemoveFileListener(position, file.fileName()));

        return convertView;
    }

    private class RemoveFileListener implements View.OnClickListener {

        private int position;
        private String fileName;

        RemoveFileListener(int position, String fileName) {
            this.position = position;
            this.fileName = fileName;
        }

        @Override
        public void onClick(View v) {
            containerFacade = ContainerBuilder.aContainer(getContext()).fromExistingContainer(containerFacade.getAbsolutePath()).build();

            if (containerFacade.isSigned()) {
                notificationUtil.showWarningMessage(getContext().getText(R.string.datafile_delete_not_allowed_signed));
                return;
            }

            if (getCount() == 1) {
                notificationUtil.showWarningMessage(getContext().getText(R.string.datafile_delete_not_allowed_empty));
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(R.string.bdoc_remove_confirm_title);

            String confirmMessage = getContext().getString(R.string.file_remove_confirm_message);
            confirmMessage = String.format(confirmMessage, fileName);

            builder.setMessage(confirmMessage);

            builder.setPositiveButton(R.string.confirm_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    containerFacade.removeDataFile(position);
                    containerFacade.save();
                    notificationUtil.showSuccessMessage(getContext().getText(R.string.file_removed));
                    DataFile dataFile = getItem(position);
                    remove(dataFile);
                    notifyDataSetChanged();
                    containerDataFilesFragment.calculateFragmentHeight();
                }
            }).setNegativeButton(R.string.cancel_button, null);

            builder.create().show();
        }
    }
}
