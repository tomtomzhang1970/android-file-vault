package com.vandenbreemen.mobilesecurestorage.android;

import android.content.Intent;
import android.os.Environment;
import android.view.KeyEvent;
import android.widget.TextView;

import com.vandenbreemen.mobilesecurestorage.R;
import com.vandenbreemen.mobilesecurestorage.android.api.FileWorkflow;
import com.vandenbreemen.mobilesecurestorage.android.sfs.SFSCredentials;
import com.vandenbreemen.mobilesecurestorage.file.ChunkedMediumException;
import com.vandenbreemen.mobilesecurestorage.security.SecureString;
import com.vandenbreemen.mobilesecurestorage.security.crypto.persistence.SecureFileSystem;

import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowActivity;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.plugins.RxJavaPlugins;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.robolectric.Shadows.shadowOf;

/**
 * <h2>Intro</h2>
 * <p>
 * <h2>Other Details</h2>
 *
 * @author kevin
 */
@RunWith(RobolectricTestRunner.class)
public class LoadSecureFileSystemFunctionalTest {

    private String password;

    private FileWorkflow workflow;

    private Intent startLoadSFS;

    @Before
    public void setup() throws ChunkedMediumException {

        //  This is a workaround to deal with issue in which
        //  the success callback never gets called
        //  https://github.com/robolectric/robolectric/issues/2534
        RxJavaPlugins.setIoSchedulerHandler(scheduler -> AndroidSchedulers.mainThread());

        File sfsFile = new File(Environment.getExternalStorageDirectory() + File.separator + "test");
        password = "password";

        //  Stand up SFS
        new SecureFileSystem(sfsFile) {
            @Override
            protected SecureString getPassword() {
                return SecureFileSystem.generatePassword(SecureString.fromPassword(password));
            }
        };

        this.workflow = new FileWorkflow();
        this.workflow.setFileOrDirectory(sfsFile);

        FileSelectActivity activity = Robolectric.setupActivity(FileSelectActivity.class);
        this.startLoadSFS = new Intent(activity, LoadSecureFileSystem.class);
        this.startLoadSFS.putExtra(FileWorkflow.PARM_WORKFLOW_NAME, workflow);
    }

    @Test
    public void shouldStartFileLoadActivityForResult() {
        LoadSecureFileSystem load = Robolectric.buildActivity(LoadSecureFileSystem.class, startLoadSFS)
                .create()
                .get();


        ShadowActivity.IntentForResult intentForResult = shadowOf(load).getNextStartedActivityForResult();
        assertNotNull(intentForResult);
        assertEquals(FileSelectActivity.class, shadowOf(intentForResult.intent).getIntentClass());
    }

    @Test
    public void shouldFinishAndSendCancelOnCancel() {
        LoadSecureFileSystem load = Robolectric.buildActivity(LoadSecureFileSystem.class, startLoadSFS)
                .create()
                .get();

        load.findViewById(R.id.cancel).performClick();
        assertEquals(RESULT_CANCELED, shadowOf(load).getResultCode());
        assertTrue(shadowOf(load).isFinishing());
    }

    @Test
    public void testGetCredentails() {

        //  Arrange
        AtomicReference<SFSCredentials> credentials = new AtomicReference<>(null);
        LoadSecureFileSystem load = Robolectric.buildActivity(LoadSecureFileSystem.class, startLoadSFS)
                .create()
                .get();

        //  Simulate that we've selected a file to load
        Intent successfullyLoadedFile = new Intent();
        successfullyLoadedFile.putExtra(FileWorkflow.PARM_WORKFLOW_NAME, workflow);
        ShadowActivity.IntentForResult intentForResult = shadowOf(load).getNextStartedActivityForResult();
        shadowOf(load).receiveResult(intentForResult.intent, RESULT_OK, successfullyLoadedFile);

        load.setListener(cred -> credentials.set(cred));

        //  Act
        TextView textView = load.findViewById(R.id.password);
        textView.setText(password);

        load.findViewById(R.id.ok).performClick();

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> credentials.get() != null);

        //  Assert
        assertTrue("Password",
                SecureFileSystem.generatePassword(SecureString.fromPassword(password)).equals(credentials.get().getPassword()));
    }

    @Test
    public void shouldAllowEnterToDecrypt() {

        AtomicReference<SFSCredentials> credentials = new AtomicReference<>(null);

        LoadSecureFileSystem load = Robolectric.buildActivity(LoadSecureFileSystem.class, startLoadSFS)
                .create()
                .get();

        load.setListener(cred -> credentials.set(cred));

        //  Simulate that we've selected a file to load
        Intent successfullyLoadedFile = new Intent();
        successfullyLoadedFile.putExtra(FileWorkflow.PARM_WORKFLOW_NAME, workflow);
        ShadowActivity.IntentForResult intentForResult = shadowOf(load).getNextStartedActivityForResult();
        shadowOf(load).receiveResult(intentForResult.intent, RESULT_OK, successfullyLoadedFile);

        TextView textView = load.findViewById(R.id.password);
        textView.setText(password);

        load.onKeyUp(KeyEvent.KEYCODE_ENTER, new KeyEvent(1, 1));

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> credentials.get() != null);

        assertTrue("Password",
                SecureFileSystem.generatePassword(SecureString.fromPassword(password)).equals(credentials.get().getPassword()));
    }

    @Test
    public void shouldReturnCredentialsResult() {
        FileSelectActivity activity = Robolectric.setupActivity(FileSelectActivity.class);
        startLoadSFS = new Intent(activity, LoadSecureFileSystem.class);
        this.startLoadSFS.putExtra(FileWorkflow.PARM_WORKFLOW_NAME, workflow);

        LoadSecureFileSystem load = Robolectric.buildActivity(LoadSecureFileSystem.class, startLoadSFS)
                .create()
                .get();

        //  Simulate that we've selected a file to load
        Intent successfullyLoadedFile = new Intent();
        successfullyLoadedFile.putExtra(FileWorkflow.PARM_WORKFLOW_NAME, workflow);
        ShadowActivity.IntentForResult intentForResult = shadowOf(load).getNextStartedActivityForResult();
        shadowOf(load).receiveResult(intentForResult.intent, RESULT_OK, successfullyLoadedFile);

        TextView textView = load.findViewById(R.id.password);
        textView.setText(password);


        load.findViewById(R.id.ok).performClick();

        Intent result = shadowOf(load).getResultIntent();
        assertEquals(RESULT_OK, shadowOf(load).getResultCode());
        assertNotNull("Credentials", result.getParcelableExtra(SFSCredentials.PARM_CREDENTIALS));
    }

}
