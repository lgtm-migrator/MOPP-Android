package ee.ria.EstEIDUtility.fragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ListView;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ee.ria.EstEIDUtility.BuildConfig;
import ee.ria.EstEIDUtility.R;
import ee.ria.EstEIDUtility.adapter.DataFilesAdapter;
import ee.ria.EstEIDUtility.container.ContainerBuilder;
import ee.ria.EstEIDUtility.container.ContainerFacade;
import ee.ria.EstEIDUtility.util.Constants;
import ee.ria.EstEIDUtility.util.FileUtils;
import ee.ria.EstEIDUtility.util.LayoutUtils;
import ee.ria.EstEIDUtility.util.NotificationUtil;
import ee.ria.libdigidocpp.DataFile;

public class ContainerDataFilesFragment extends ListFragment {

    public static final String TAG = "DATAFILES_FRAGMENT";

    private DataFilesAdapter filesAdapter;
    private ContainerFacade containerFacade;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String containerWorkingPath = getArguments().getString(Constants.CONTAINER_PATH_KEY);
        containerFacade = ContainerBuilder.aContainer(getContext()).fromExistingContainer(containerWorkingPath).build();


        filesAdapter = new DataFilesAdapter(getActivity(), containerFacade, this);
        setListAdapter(filesAdapter);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        calculateFragmentHeight();
        setEmptyText(getText(R.string.empty_container_files));
        getListView().setOnItemLongClickListener(new FileLongClickListener());
    }

    public void calculateFragmentHeight() {
        LayoutUtils.calculateFragmentHeight(getListView());
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        launchFileContentActivity(position);
    }

    public void addFile(DataFile dataFile) {
        filesAdapter.add(dataFile);
        calculateFragmentHeight();
    }

    private File extractAttachment(String fileName) {
        File attachment = null;
        File containerFile = containerFacade.getContainerFile();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(containerFile))) {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                if (fileName.equals(ze.getName())) {
                    File cacheDir = FileUtils.getDataFilesCacheDirectory(getContext());
                    attachment = File.createTempFile(FilenameUtils.removeExtension(fileName), "." + FilenameUtils.getExtension(fileName), cacheDir);
                    try (FileOutputStream out = new FileOutputStream(attachment)) {
                        IOUtils.copy(zis, out);
                    }
                    break;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "extractAttachment: ", e);
        }
        return attachment;
    }

    private void launchFileContentActivity(int position) {
        DataFile file = (DataFile) getListAdapter().getItem(position);
        String fileName = file.fileName();

        File attachment = extractAttachment(fileName);

        if (attachment == null) {
            NotificationUtil.showWarning(getActivity(), R.string.attachment_extract_failed, NotificationUtil.NotificationDuration.LONG);
            return;
        }

        Uri contentUri = FileProvider.getUriForFile(getContext(), BuildConfig.APPLICATION_ID, attachment);
        getContext().grantUriPermission(BuildConfig.APPLICATION_ID, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(FilenameUtils.getExtension(fileName));
        intent.setDataAndType(contentUri, mimeType);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "launchFileContentActivity: no handler for this type of file ", e);
            NotificationUtil.showError(getActivity(), R.string.file_handler_error, NotificationUtil.NotificationDuration.LONG);
        }
    }

    class FileLongClickListener implements AdapterView.OnItemLongClickListener {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            DataFile file = (DataFile) getListAdapter().getItem(position);
            String fileName = file.fileName();

            File attachment = extractAttachment(fileName);

            if (attachment == null) {
                NotificationUtil.showWarning(getActivity(), R.string.attachment_extract_failed, NotificationUtil.NotificationDuration.LONG);
                return false;
            }

            Uri contentUri = FileProvider.getUriForFile(getContext(), BuildConfig.APPLICATION_ID, attachment);
            getContext().grantUriPermission(BuildConfig.APPLICATION_ID, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(FilenameUtils.getExtension(fileName));
            if (mimeType == null) {
                NotificationUtil.showWarning(getActivity(), R.string.file_unsharable, NotificationUtil.NotificationDuration.LONG);
                return false;
            }

            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.setType(mimeType);
            startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.upload_to)));
            return true;
        }
    }

}