package org.knime.knip.qc.nodes.patcher;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.knip.base.data.img.ImgPlusValue;

public class PatcherNodeDialog<L extends Comparable<L>> extends DefaultNodeSettingsPane {

        /**
         * Default Constructor
         */
        @SuppressWarnings("unchecked")
        public PatcherNodeDialog() {

                String[] patchnums = {"1", "2", "4", "8", "16", "32", "64", "128"};
                addDialogComponent(new DialogComponentStringSelection(PatcherNodeModel.createNumPatchesSelectionModel(), "Number of Patches",
                                patchnums));

                createNewTab("Column Selection");
                addDialogComponent(new DialogComponentColumnFilter(PatcherNodeModel.createImgColumnSelectionModel(), 0, true, ImgPlusValue.class));

        }
}
