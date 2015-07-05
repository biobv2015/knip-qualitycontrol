package org.knime.knip.qc.nodes.patcher;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.knip.base.data.img.ImgPlusValue;

public class PatcherNodeDialog<L extends Comparable<L>> extends DefaultNodeSettingsPane {

        /**
         * Default Constructor
         */
        @SuppressWarnings("unchecked")
        public PatcherNodeDialog() {
                createNewGroup("Column selection:");
                addDialogComponent(new DialogComponentColumnNameSelection(PatcherNodeModel.createImgColumnSelectionModel(), "Column Selection", 0,
                                ImgPlusValue.class));
                createNewGroup("Patching options:");

                addDialogComponent(new DialogComponentButtonGroup(PatcherNodeModel.createPatchingMethodSwitchModel(), "Patching method:", true,
                                PatcherNodeModel.PATCHING_METHOD_SWITCH_OPTIONS[0], PatcherNodeModel.PATCHING_METHOD_SWITCH_OPTIONS[1]));

                String[] patchnums = {"1", "2", "4", "8", "16", "32", "64", "128"};
                addDialogComponent(new DialogComponentStringSelection(PatcherNodeModel.createTotalNumPatchesSelectionModel(),
                                "Total number of patches", patchnums));

        }
}
