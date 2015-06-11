package org.knime.knip.qc.nodes.globalfeature;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentDoubleRange;
import org.knime.knip.base.data.img.ImgPlusValue;

public class GlobalFeatureNodeDialog<L extends Comparable<L>> extends DefaultNodeSettingsPane {

        /**
         * Default Constructor
         */
        @SuppressWarnings("unchecked")
        public GlobalFeatureNodeDialog() {
                addDialogComponent(new DialogComponentColumnNameSelection(GlobalFeatureNodeModel.createImgColumnSelectionModel(), "Column Selection",
                                0, ImgPlusValue.class));

                addDialogComponent(new DialogComponentDoubleRange(GlobalFeatureNodeModel.createRangeSelectionModel(), 0, 255, 1,
                                "Select Range of Intensity"));

        }
}
