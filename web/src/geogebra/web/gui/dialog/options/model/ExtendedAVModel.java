package geogebra.web.gui.dialog.options.model;

import geogebra.common.gui.dialog.options.model.BooleanOptionModel;
import geogebra.common.kernel.geos.GeoElement;
import geogebra.common.kernel.geos.HasExtendedAV;

public class ExtendedAVModel extends BooleanOptionModel {

	public ExtendedAVModel(IBooleanOptionListener listener) {
		super(listener);
	}

	@Override
	public boolean isValidAt(int index) {
		return getGeoAt(index) instanceof HasExtendedAV;
	}

	@Override
	public boolean getValueAt(int index) {
		return isValidAt(index)
				&& ((HasExtendedAV) getGeoAt(index)).isShowingExtendedAV();
	}

	@Override
	public void apply(int index, boolean value) {
		if (isValidAt(index)) {
			GeoElement geo = getGeoAt(index);
			((HasExtendedAV) geo).setShowExtendedAV(value);
			geo.updateRepaint();
		}
	}

}
