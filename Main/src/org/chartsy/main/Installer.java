package org.chartsy.main;

import java.beans.PropertyEditorManager;
import java.util.prefs.Preferences;
import org.chartsy.main.managers.AnnotationManager;
import org.chartsy.main.managers.ChartManager;
import org.chartsy.main.managers.DataProviderManager;
import org.chartsy.main.managers.FacebookManager;
import org.chartsy.main.managers.IndicatorManager;
import org.chartsy.main.managers.OverlayManager;
import org.chartsy.main.managers.ProxyManager;
import org.chartsy.main.managers.StockManager;
import org.chartsy.main.managers.TemplateManager;
import org.chartsy.main.managers.TwitterManager;
import org.chartsy.main.utils.AlphaPropertyEditor;
import org.chartsy.main.utils.PricePropertyEditor;
import org.chartsy.main.utils.StrokePropertyEditor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.modules.ModuleInstall;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;

public class Installer extends ModuleInstall implements Runnable
{

	private Preferences chartsyPreferences = NbPreferences.root().node("/org/chartsy/register");
	private Preferences printPreferences = NbPreferences.root().node("/org/netbeans/modules/print");

	@Override public void run()
	{
		System.setProperty("sun.java2d.opengl", "true");
		System.setProperty("sun.java2d.d3d", "false");
		setPrintProperties();
	}

    public @Override void restored()
    {
		WindowManager.getDefault().invokeWhenUIReady(this);
		
		PropertyEditorManager.registerEditor(int.class, StrokePropertyEditor.class);
		PropertyEditorManager.registerEditor(String.class, PricePropertyEditor.class);
		PropertyEditorManager.registerEditor(int.class, AlphaPropertyEditor.class);

		ProxyManager.getDefault();
		DataProviderManager.getDefault();
		ChartManager.getDefault();
		IndicatorManager.getDefault();
		OverlayManager.getDefault();
		AnnotationManager.getDefault();
		TemplateManager.getDefault();
		StockManager.getDefault();
                FacebookManager.getDefault();
                TwitterManager.getDefault();
    }

    public @Override boolean closing()
    {
        NotifyDescriptor descriptor = new NotifyDescriptor.Confirmation(
			"Do you really want to exit the application?",
			"Exit",
			NotifyDescriptor.YES_NO_OPTION);
        Object retval = DialogDisplayer.getDefault().notify(descriptor);
        if (retval.equals(NotifyDescriptor.YES_OPTION))
            return true; 
        else
            return false;
    }

    private void setPrintProperties()
    {
		if (!chartsyPreferences.getBoolean("print.init", false))
		{
			printPreferences.put("print.area.height", "697.8897637795276");
			printPreferences.put("print.area.width", "451.2755905511811");
			printPreferences.put("print.area.x", "72.0");
			printPreferences.put("print.area.y", "72.0");
			printPreferences.put("print.border", "false");
			printPreferences.put("print.border.color", "0,0,0");
			printPreferences.put("print.footer", "false");
			printPreferences.put("print.footer.center", "");
			printPreferences.put("print.footer.color", "0,0,0");
			printPreferences.put("print.footer.font", "Serif,0,10");
			printPreferences.put("print.footer.left", "%ROW%.%COLUMN% of %COUNT%");
			printPreferences.put("print.footer.right", "%MODIFIED_DATE%  %MODIFIED_TIME%");
			printPreferences.put("print.header", "false");
			printPreferences.put("print.header.center", "");
			printPreferences.put("print.header.color", "0,0,0");
			printPreferences.put("print.header.font", "Serif,0,10");
			printPreferences.put("print.header.left", "%NAME%");
			printPreferences.put("print.header.right", "");
			printPreferences.put("print.page.orientation", "0.0");
			printPreferences.put("print.paper.height", "841.8897637795276");
			printPreferences.put("print.paper.width", "595.275590551181");
			printPreferences.put("print.text.as.editor", "false");
			printPreferences.put("print.text.background.color", "255,250,255");
			printPreferences.put("print.text.color", "0,0,0");
			printPreferences.put("print.text.font", "Monospaced,0,10");
			printPreferences.put("print.text.line.numbers", "false");
			printPreferences.put("print.text.line.spacing", "1.0");
			printPreferences.put("print.text.selection", "false");
			printPreferences.put("print.text.use.color", "true");
			printPreferences.put("print.text.use.font", "true");
			printPreferences.put("print.text.wrap.lines", "false");
			printPreferences.put("print.zoom", "0.0");
			chartsyPreferences.putBoolean("print.init", true);
		}
    }

}
