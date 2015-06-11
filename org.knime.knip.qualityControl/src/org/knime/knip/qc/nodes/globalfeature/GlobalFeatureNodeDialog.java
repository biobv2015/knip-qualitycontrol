package org.knime.knip.qc.nodes.globalfeature;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.knip.base.data.img.ImgPlusValue;

public class GlobalFeatureNodeDialog<L extends Comparable<L>> extends DefaultNodeSettingsPane {

        /**
         * Default Constructor
         */
        @SuppressWarnings("unchecked")
        public GlobalFeatureNodeDialog() {
                addDialogComponent(new DialogComponentColumnNameSelection(GlobalFeatureNodeModel.createImgColumnSelectionModel(), "Column Selection", 0,
                                ImgPlusValue.class));
                String[] patchnums = {"1", "2", "4", "8", "16", "32", "64", "128"};
                addDialogComponent(new DialogComponentStringSelection(GlobalFeatureNodeModel.createNumPatchesSelectionModel(), "Number of Patches",
                                patchnums));

        }
}
