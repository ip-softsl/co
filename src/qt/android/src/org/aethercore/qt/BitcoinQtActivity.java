package org.aethercore.qt;

import android.os.Bundle;
import android.system.ErrnoException;
import android.system.Os;

import org.qtproject.qt5.android.bindings.QtActivity;

import java.io.File;

public class AetherQtActivity extends QtActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        final File aetherDir = new File(getFilesDir().getAbsolutePath() + "/.aether");
        if (!aetherDir.exists()) {
            aetherDir.mkdir();
        }

        super.onCreate(savedInstanceState);
    }
}
