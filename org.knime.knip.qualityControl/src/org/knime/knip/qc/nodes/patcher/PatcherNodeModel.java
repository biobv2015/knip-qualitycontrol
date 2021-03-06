/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.knip.qc.nodes.patcher;

import java.io.File;

import net.imagej.ImgPlus;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.NodeUtils;
import org.knime.knip.qc.patching.Patcher;

/**
 * Cut image into patches of mainly the same size
 *
 * @author <a href="mailto:adrian.nembach@uni-konstanz.de">Adrian Nembach</a>
 * 
 * @param <L>
 * @param <T>
 */
@SuppressWarnings("deprecation")
public class PatcherNodeModel<L extends Comparable<L>, T extends RealType<T>> extends NodeModel implements BufferedDataTableHolder {

        static final String[] BACKGROUND_OPTIONS = new String[] {"Min Value of Result", "Max Value of Result", "Zero", "Source"};

        static final String[] PATCHING_METHOD_SWITCH_OPTIONS = new String[] {"Total number of patches",
                        "Number of patches per dimension (first and second)"};
        static final int DEFAULT_TOTAL_NUM_PATCHES = 1;
        static final int[] DEFAULT_NUM_PATCHES_PER_DIMENSION = {1, 1};

        /**
         * Helper
         *
         * @return SettingsModel to store img column
         */
        static SettingsModelString createImgColumnSelectionModel() {
                return new SettingsModelString("ADRIAN", "");
        }

        static SettingsModelInteger createTotalNumPatchesSelectionModel() {
                return new SettingsModelIntegerBounded("totalNumPatches", DEFAULT_TOTAL_NUM_PATCHES, 0, Integer.MAX_VALUE);
        }

        static SettingsModelInteger[] createNumPatchesPerDimensionModel() {
                SettingsModelInteger patchesPerDimensionModelX = new SettingsModelIntegerBounded("patchesPerDimensionModelX",
                                DEFAULT_NUM_PATCHES_PER_DIMENSION[0], 1, Integer.MAX_VALUE);
                SettingsModelInteger patchesPerDimensionModelY = new SettingsModelIntegerBounded("patchesPerDimensionModelY",
                                DEFAULT_NUM_PATCHES_PER_DIMENSION[1], 1, Integer.MAX_VALUE);
                patchesPerDimensionModelX.setEnabled(false);
                patchesPerDimensionModelY.setEnabled(false);
                return new SettingsModelInteger[] {patchesPerDimensionModelX, patchesPerDimensionModelY};
        }

        static SettingsModelString createPatchingMethodSwitchModel() {
                return new SettingsModelString("patchingMethodSwitch", PATCHING_METHOD_SWITCH_OPTIONS[0]);
        }

        /* SettingsModels */

        private SettingsModelString m_imgColumnNameModel = createImgColumnSelectionModel();

        private SettingsModelInteger m_totalNumPatchesModel = createTotalNumPatchesSelectionModel();

        private SettingsModelInteger[] m_numPatchesPerDimensionModels = createNumPatchesPerDimensionModel();

        private SettingsModelString m_patchingMethodSwitchModel = createPatchingMethodSwitchModel();

        /* Resulting BufferedDataTable */
        private BufferedDataTable m_data;

        /**
         * Constructor SegementCropperNodeModel
         */
        public PatcherNodeModel() {
                super(1, 1);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

                int imgColIdx = inSpecs[0].findColumnIndex(m_imgColumnNameModel.getStringValue());

                // check if selected image column is contained in input table
                if (imgColIdx == -1) {
                        // automatically select an image column
                        if (NodeUtils.autoOptionalColumnSelection(inSpecs[0], m_imgColumnNameModel, ImgPlusValue.class) >= 0) {
                                setWarningMessage("Auto-configure Label Column: " + m_imgColumnNameModel.getStringValue());
                        } else {
                                throw new InvalidSettingsException("No column selected!");
                        }
                }

                // check number of patches per dimension settings
                if (m_numPatchesPerDimensionModels[0].getIntValue() < 1 || m_numPatchesPerDimensionModels[1].getIntValue() < 1) {
                        throw new InvalidSettingsException("There must be at least one patch per dimension!");
                }

                // check total number of patches settings
                if (m_totalNumPatchesModel.getIntValue() < 0) {
                        throw new InvalidSettingsException("The minimal number of patches is 1 (2^0)!");
                }

                return createOutSpec();
        }

        private DataTableSpec[] createOutSpec() {
                DataColumnSpec sourceImgSpec = new DataColumnSpecCreator("Source Image", ImgPlusCell.TYPE).createSpec();
                DataColumnSpec patchSpec = new DataColumnSpecCreator("Patch", ImgPlusCell.TYPE).createSpec();

                return new DataTableSpec[] {new DataTableSpec(sourceImgSpec, patchSpec)};
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @SuppressWarnings({"unchecked"})
        protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec) throws Exception {

                // find img column index
                int imgColIdx = inData[0].getDataTableSpec().findColumnIndex(m_imgColumnNameModel.getStringValue());
                // check if selected image column is contained in input table
                if (imgColIdx == -1) {
                        // automatically select an image column
                        if (NodeUtils.autoOptionalColumnSelection(inData[0].getDataTableSpec(), m_imgColumnNameModel, ImgPlusValue.class) >= 0) {
                                setWarningMessage("Auto-configure Label Column: " + m_imgColumnNameModel.getStringValue());
                        } else {
                                throw new InvalidSettingsException("No column selected!");
                        }
                }

                int rowIdx = 0;
                final int rowCount = inData[0].getRowCount();

                final BufferedDataContainer container = exec.createDataContainer(createOutSpec()[0]);
                final ImgPlusCellFactory imgCellFac = new ImgPlusCellFactory(exec);

                int numPatches = (int) Math.pow(2, m_totalNumPatchesModel.getIntValue());

                for (final DataRow row : inData[0]) {
                        final ImgPlusValue<T> imgPlusValue = (ImgPlusValue<T>) row.getCell(imgColIdx);
                        DataCell[] cells = new DataCell[2];

                        // get Img from ImgPlus
                        Img<T> img = imgPlusValue.getImgPlus().getImg();

                        // calculate parameters for patching
                        long[] dimensionSizes = new long[img.numDimensions()];
                        img.dimensions(dimensionSizes);
                        int[] dimensions = new int[img.numDimensions()];
                        for (int i = 0; i < img.numDimensions(); i++)
                                dimensions[i] = i;
                        int[] patchesPerDimension = null;

                        // check which patching option is selected
                        if (m_patchingMethodSwitchModel.getStringValue().equals(PATCHING_METHOD_SWITCH_OPTIONS[0])) {
                                patchesPerDimension = Patcher.calculatePatchesPerDimension(numPatches, dimensionSizes);
                        } else {
                                patchesPerDimension = new int[img.numDimensions()];
                                patchesPerDimension[0] = m_numPatchesPerDimensionModels[0].getIntValue();
                                patchesPerDimension[1] = m_numPatchesPerDimensionModels[1].getIntValue();
                                // The remaining dimensions need to be set to one
                                if (patchesPerDimension.length > 2) {
                                        for (int d = 2; d < patchesPerDimension.length; d++) {
                                                patchesPerDimension[d] = 1;
                                        }
                                }
                        }

                        // patch image
                        Object[] patches = Patcher.patchImg(img, dimensions, patchesPerDimension);

                        // write patches into data conainer
                        for (int p = 0; p < patches.length; p++) {
                                // write patch into ImgPlus
                                ImgPlus<T> impPatch = new ImgPlus<T>((Img<T>) patches[p]);
                                cells[0] = imgCellFac.createCell(imgPlusValue.getImgPlus());
                                cells[1] = imgCellFac.createCell(impPatch);
                                container.addRowToTable(new DefaultRow(row.getKey().toString() + "#" + p, cells));
                        }

                        exec.checkCanceled();
                        exec.setProgress((double) ++rowIdx / rowCount);
                }

                container.close();
                m_data = container.getTable();
                return new BufferedDataTable[] {m_data};
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public BufferedDataTable[] getInternalTables() {
                return new BufferedDataTable[] {m_data};
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) {
                //
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
                m_imgColumnNameModel.loadSettingsFrom(settings);
                m_totalNumPatchesModel.loadSettingsFrom(settings);
                m_patchingMethodSwitchModel.loadSettingsFrom(settings);
                m_numPatchesPerDimensionModels[0].loadSettingsFrom(settings);
                m_numPatchesPerDimensionModels[1].loadSettingsFrom(settings);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void reset() {
                //
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) {
                //
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void saveSettingsTo(final NodeSettingsWO settings) {
                m_imgColumnNameModel.saveSettingsTo(settings);
                m_totalNumPatchesModel.saveSettingsTo(settings);
                m_patchingMethodSwitchModel.saveSettingsTo(settings);
                m_numPatchesPerDimensionModels[0].saveSettingsTo(settings);
                m_numPatchesPerDimensionModels[1].saveSettingsTo(settings);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setInternalTables(final BufferedDataTable[] tables) {
                m_data = tables[0];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
                m_imgColumnNameModel.validateSettings(settings);
                m_totalNumPatchesModel.validateSettings(settings);
                m_patchingMethodSwitchModel.validateSettings(settings);
                m_numPatchesPerDimensionModels[0].validateSettings(settings);
                m_numPatchesPerDimensionModels[1].validateSettings(settings);
        }
}
