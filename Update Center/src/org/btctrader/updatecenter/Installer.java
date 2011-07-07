package org.btctrader.updatecenter;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import org.openide.filesystems.FileUtil;
import org.openide.modules.ModuleInstall;
import org.openide.util.NbPreferences;

/**
 * Manages a module's lifecycle.
 * Installs user keystore with trusted CA and module-signing certificates.
 * @author coinfreak
 */
public final class Installer extends ModuleInstall
{
    private static final Preferences autoUpdatePreferences = NbPreferences.root().node("/org/netbeans/modules/autoupdate");

    @Override
    public void restored()
    {
        initKeystore();
    }

    private void initKeystore()
    {
        try {
            File dest = new File( getCacheDirectory(), "user.ks" );
            if ( !dest.exists() ) {
                dest.createNewFile();
                FileUtil.copy(
                        Installer.class.getResourceAsStream("user.ks"),
                        FileUtil.toFileObject(dest).getOutputStream() );

               autoUpdatePreferences.put("userKS", "user.ks");
               autoUpdatePreferences.put("period", "1");
            }
        }
        catch (IOException ex) {
            Logger.getLogger(Installer.class.getName()).log(
                    Level.WARNING, "Can't copy user keystore.", ex );
        }
    }

    // Copy-pasted from netpeans platform sources:
    // org.netbeans.modules.autoupdate.services.Utilities
    private File getCacheDirectory()
    {
        File cacheDir = null;
        String userDir = System.getProperty ("netbeans.user"); // NOI18N
        if (userDir != null) {
            cacheDir = new File (new File (new File (userDir, "var"), "cache"), "catalogcache"); // NOI18N
        } else {
            File dir = FileUtil.toFile (FileUtil.getConfigRoot());
            cacheDir = new File (dir, "catalogcache"); // NOI18N
        }
        cacheDir.mkdirs();
        return cacheDir;
    }
}
