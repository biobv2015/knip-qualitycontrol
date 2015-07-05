package org.knime.knip.qc.nodes.patcher;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
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

                final SettingsModelString patchingMethodSwitch = PatcherNodeModel.createPatchingMethodSwitchModel();
                final SettingsModelInteger totalNumPatches = PatcherNodeModel.createTotalNumPatchesSelectionModel();
                final SettingsModelInteger[] patchesPerDimension = PatcherNodeModel.createNumPatchesPerDimensionModel();

                addDialogComponent(new DialogComponentButtonGroup(patchingMethodSwitch, true, "Patching method:",
                                PatcherNodeModel.PATCHING_METHOD_SWITCH_OPTIONS[0], PatcherNodeModel.PATCHING_METHOD_SWITCH_OPTIONS[1]));

                addDialogComponent(new DialogComponentNumber(totalNumPatches, "Total number of patches (2 to the power of)", 1));

                addDialogComponent(new DialogComponentNumber(patchesPerDimension[0], "Number of patches in first dimension:", 1));
                addDialogComponent(new DialogComponentNumber(patchesPerDimension[1], "Number of patches in second dimension:", 1));

                patchingMethodSwitch.addChangeListener(new ChangeListener() {
                        public void stateChanged(final ChangeEvent e) {
                                if (patchingMethodSwitch.getStringValue().equals(PatcherNodeModel.PATCHING_METHOD_SWITCH_OPTIONS[0])) {
                                        totalNumPatches.setEnabled(true);
                                        patchesPerDimension[0].setEnabled(false);
                                        patchesPerDimension[1].setEnabled(false);
                                } else {
                                        System.out.println(patchingMethodSwitch.getStringValue());
                                        totalNumPatches.setEnabled(false);
                                        patchesPerDimension[0].setEnabled(true);
                                        patchesPerDimension[1].setEnabled(true);
                                }
                        }
                });

        }
}
