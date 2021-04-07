package org.geogebra.web.full.gui.menubar.action;

import org.geogebra.common.gui.InputHandler;
import org.geogebra.common.move.ggtapi.models.Material;
import org.geogebra.web.full.gui.menubar.MenuAction;
import org.geogebra.web.full.main.AppWFull;
import org.geogebra.web.html5.gui.tooltip.ToolTipManagerW;
import org.geogebra.web.shared.components.ComponentInputDialog;
import org.geogebra.web.shared.components.DialogData;

public class SaveExamAction implements MenuAction<Void> {

	@Override
	public boolean isAvailable(Void item) {
		return true;
	}

	@Override
	public void execute(Void item, AppWFull app) {
		DialogData data = app.getDialogManager().getSaveDialogData();
		InputHandler inputHandler = (input, handler, callback) -> {
			String msg = app.getLocalization().getMenu("SavedSuccessfully");
			try {
				Material material = new Material(-1, Material.MaterialType.ggb);
				material.setTitle(input);
				material.setBase64(app.getGgbApi().getBase64());
				app.getExam().saveTempMaterial(material);
			} catch (RuntimeException ex) {
				msg = app.getLocalization().getError("SaveFileFailed");
			}
			ToolTipManagerW.sharedInstance().showBottomInfoToolTip(
					app.getLocalization().getMenu(msg),
					null, null, app,
					app.getAppletFrame().isKeyboardShowing());
			if (callback != null) {
				callback.callback(true);
			}
		};
		ComponentInputDialog examSave = new ComponentInputDialog(app, data, false,
				true, inputHandler, "Title", "", false);
		examSave.center();
	}
}
