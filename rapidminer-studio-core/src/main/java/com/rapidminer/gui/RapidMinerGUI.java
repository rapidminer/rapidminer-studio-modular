/**
 * Copyright (C) 2001-2021 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Policy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.regex.Pattern;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.PopupFactory;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import com.rapidminer.FileProcessLocation;
import com.rapidminer.Process;
import com.rapidminer.ProcessLocation;
import com.rapidminer.RapidMiner;
import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.connection.adapter.ConnectionAdapterHandler;
import com.rapidminer.core.io.data.source.DataSourceFactoryRegistry;
import com.rapidminer.gui.actions.OpenAction;
import com.rapidminer.gui.actions.search.ActionsGlobalSearch;
import com.rapidminer.gui.actions.search.ActionsGlobalSearchGUIProvider;
import com.rapidminer.gui.autosave.AutoSave;
import com.rapidminer.gui.dialog.EULADialog;
import com.rapidminer.gui.dnd.DropFileIntoProcessActionRegistry;
import com.rapidminer.gui.dnd.DropFileIntoProcessCallback;
import com.rapidminer.gui.docking.RapidDockableContainerFactory;
import com.rapidminer.gui.internal.GUIStartupListener;
import com.rapidminer.gui.license.LicenseTools;
import com.rapidminer.gui.look.HeavyweightOSXPopupFactory;
import com.rapidminer.gui.look.RapidLookAndFeel;
import com.rapidminer.gui.look.fc.BookmarkIO;
import com.rapidminer.gui.look.ui.RapidDockingUISettings;
import com.rapidminer.gui.osx.OSXAdapter;
import com.rapidminer.gui.plotter.PlotterPanel;
import com.rapidminer.gui.processeditor.ChangingProcessEditor;
import com.rapidminer.gui.processeditor.search.OperatorGlobalSearch;
import com.rapidminer.gui.processeditor.search.OperatorGlobalSearchGUIProvider;
import com.rapidminer.gui.safemode.SafeMode;
import com.rapidminer.gui.search.GlobalSearchGUIRegistry;
import com.rapidminer.gui.search.GlobalSearchPanel;
import com.rapidminer.gui.security.BlacklistedOperatorProcessEditor;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.DecisionRememberingConfirmDialog;
import com.rapidminer.gui.tools.logging.LogHandlerModel;
import com.rapidminer.gui.tools.logging.LogModel;
import com.rapidminer.gui.tools.logging.LogModelRegistry;
import com.rapidminer.gui.tools.logging.LogViewer;
import com.rapidminer.gui.viewer.MetaDataViewerTableModel;
import com.rapidminer.license.LicenseManagerRegistry;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.internal.ProcessEmbeddingOperator;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeColor;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryLocationBuilder;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.RepositoryTools;
import com.rapidminer.repository.gui.BinaryEntryResultRendererRegistry;
import com.rapidminer.repository.gui.search.RepositoryGlobalSearchGUIProvider;
import com.rapidminer.repository.local.LocalRepository;
import com.rapidminer.repository.search.RepositoryGlobalSearch;
import com.rapidminer.repository.versioned.FilesystemRepositoryAdapter;
import com.rapidminer.search.GlobalSearchIndexer;
import com.rapidminer.search.GlobalSearchRegistry;
import com.rapidminer.security.PluginSandboxPolicy;
import com.rapidminer.security.PluginSecurityManager;
import com.rapidminer.settings.Settings;
import com.rapidminer.settings.SettingsConstants;
import com.rapidminer.studio.io.data.internal.file.LocalFileDataSourceFactory;
import com.rapidminer.studio.io.data.internal.file.binary.BinaryDataSourceFactory;
import com.rapidminer.studio.io.data.internal.file.csv.CSVDataSourceFactory;
import com.rapidminer.studio.io.data.internal.file.excel.ExcelDataSourceFactory;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.FileUtils;
import com.rapidminer.tools.FontTools;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LaunchListener;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.PlatformUtilities;
import com.rapidminer.tools.RMUrlHandler;
import com.rapidminer.tools.ShutdownHooks;
import com.rapidminer.tools.SystemInfoUtilities;
import com.rapidminer.tools.SystemInfoUtilities.OperatingSystem;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.plugin.Plugin;
import com.rapidminer.tools.usagestats.CtaEventAggregator;
import com.rapidminer.tools.usagestats.UsageStatistics;
import com.rapidminer.tools.usagestats.UsageStatsScheduler;
import com.rapidminer.tools.usagestats.UsageStatsTransmissionDialog;
import com.vlsolutions.swing.docking.DockableContainerFactory;
import com.vlsolutions.swing.docking.ui.DockingUISettings;

import sun.misc.ObjectInputFilter;


/**
 * The main class if RapidMiner is started in GUI mode. This class keeps a reference to the
 * {@link MainFrame} and some other GUI relevant information and methods.
 *
 * @author Ingo Mierswa, Simon Fischer
 */
public class RapidMinerGUI extends RapidMiner {

	public static final String PROPERTY_GEOMETRY_X = "rapidminer.gui.geometry.x";
	public static final String PROPERTY_GEOMETRY_Y = "rapidminer.gui.geometry.y";
	public static final String PROPERTY_GEOMETRY_EXTENDED_STATE = "rapidminer.gui.geometry.extendedstate";
	public static final String PROPERTY_GEOMETRY_WIDTH = "rapidminer.gui.geometry.width";
	public static final String PROPERTY_GEOMETRY_HEIGHT = "rapidminer.gui.geometry.height";
	public static final String PROPERTY_GEOMETRY_DIVIDER_MAIN = "rapidminer.gui.geometry.divider.main";
	public static final String PROPERTY_GEOMETRY_DIVIDER_EDITOR = "rapidminer.gui.geometry.divider.editor";
	public static final String PROPERTY_GEOMETRY_DIVIDER_LOGGING = "rapidminer.gui.geometry.divider.logging";
	public static final String PROPERTY_GEOMETRY_DIVIDER_GROUPSELECTION = "rapidminer.gui.geometry.divider.groupselection";
	public static final String PROPERTY_EXPERT_MODE = "rapidminer.gui.expertmode";
	public static final String PROPERTY_SHOW_PARAMETER_HELP = "rapidminer.gui.show_parameter_help";

	// GUI Properties

	public static final String PROPERTY_RAPIDMINER_GUI_MAX_STATISTICS_ROWS = "rapidminer.gui.max_statistics_rows";
	public static final String PROPERTY_RAPIDMINER_GUI_MAX_SORTABLE_ROWS = "rapidminer.gui.max_sortable_rows";
	public static final String PROPERTY_RAPIDMINER_GUI_MAX_DISPLAYED_VALUES = "rapidminer.gui.max_displayed_values";
	public static final String PROPERTY_RAPIDMINER_GUI_SNAP_TO_GRID = "rapidminer.gui.snap_to_grid";

	/**
	 * Instead of possibly overlapping operators the connected operators to the right should be moved when moving an operator.
	 * @since 9.2
 	 */
	public static final String PROPERTY_RAPIDMINER_GUI_MOVE_CONNECTED_OPERATORS = "rapidminer.gui.move_connected_operators";

	/**
	 * The property name for &quot;Maximum number of states in the undo list.&quot;
	 *
	 * @since 7.5
	 */
	public static final String PROPERTY_RAPIDMINER_GUI_UNDOLIST_SIZE = "rapidminer.gui.undolist.size";
	public static final String PROPERTY_AUTOWIRE_INPUT = "rapidminer.gui.autowire_input";
	public static final String PROPERTY_AUTOWIRE_OUTPUT = "rapidminer.gui.autowire_output";
	public static final String PROPERTY_RESOLVE_RELATIVE_REPOSITORY_LOCATIONS = "rapidminer.gui.resolve_relative_repository_locations";
	public static final String PROPERTY_CLOSE_RESULTS_BEFORE_RUN = "rapidminer.gui.close_results_before_run";
	public static final String PROPERTY_ADD_BREAKPOINT_RESULTS_TO_HISTORY = "rapidminer.gui.add_breakpoint_results_to_history";
	public static final String PROPERTY_CONFIRM_EXIT = "rapidminer.gui.confirm_exit";
	public static final String PROPERTY_RUN_REMOTE_NOW = "rapidminer.gui.run_process_on_rapidanalytics_now";
	public static final String PROPERTY_OPEN_IN_FILEBROWSER = "rapidminer.gui.entry_open_in_filebrowser";
	public static final String PROPERTY_OPTIMIZED_FILEBROWSER = "rapidminer.gui.optimized_filebrowser_mapped_drives";
	public static final String PROPERTY_CLOSE_ALL_RESULTS_NOW = "rapidminer.gui.close_all_results_without_confirmation";
	public static final String PROPERTY_FETCH_DATA_BASE_TABLES_NAMES = "rapidminer.gui.fetch_data_base_table_names";
	/** determines if a warning notification bubble is shown when no result ports are connected */
	public static final String PROPERTY_SHOW_NO_RESULT_WARNING = "rapidminer.gui.no_result_port_connected";
	public static final String PROPERTY_FONT_CONFIG = "rapidminer.gui.font_config";

	public static final String PROPERTY_TRANSFER_USAGESTATS = "rapidminer.gui.transfer_usagestats";
	public static final String[] PROPERTY_TRANSFER_USAGESTATS_ANSWERS = { "ask", "always", "never" };

	public static final String PROPERTY_DRAG_TARGET_HIGHLIGHTING = "rapidminer.gui.drag_target_highlighting";
	public static final String[] PROPERTY_DRAG_TARGET_HIGHLIGHTING_VALUES = { "full", "border", "none" };
	public static final int DRAG_TARGET_HIGHLIGHTING_FULL = 0;

	// GUI flag key for caching
	public static final String IS_GUI_PROCESS = "com.rapidminer.gui.isGUIProcess";

	// Update Properties

	public static final String PROPERTY_RAPIDMINER_GUI_PURCHASED_NOT_INSTALLED_CHECK = "rapidminer.update.purchased.not_installed.check";
	public static final String PROPERTY_RAPIDMINER_GUI_UPDATE_CHECK = "rapidminer.update.check";

	// Admin Settings
	public static final String PROPERTY_RAPIDMINER_DISALLOW_REMEMBER_PASSWORD = "rapidminer.disallow.remember.password";
	public static final String PROPERTY_RAPIDMINER_DISALLOW_STUDIO_UPDATE = "rapidminer.disallow.studio.update";

	// different behavior for disconnecting deleted and disabled operators
	public static final String PROPERTY_DISABLE_OPERATOR_CONNECTION_BEHAVIOR = "rapidminer.gui.disable_op_conn_behavior";
	public static final String[] PROPERTY_DISABLE_OPERATOR_CONNECTION_BEHAVIOR_VALUES = { "removed", "bridged", "kept" };
	public static final String PROPERTY_DELETE_OPERATOR_CONNECTION_BEHAVIOR = "rapidminer.gui.delete_op_conn_behavior";
	public static final String[] PROPERTY_DELETE_OPERATOR_CONNECTION_BEHAVIOR_VALUES = { "removed", "bridged" };
	public static final int PROPERTY_DISABLE_OPERATOR_CONNECTION_BEHAVIOR_DEFAULT_VALUE = 1;
	public static final int PROPERTY_DELETE_OPERATOR_CONNECTION_BEHAVIOR_DEFAULT_VALUE = 1;


	static {

		// GUI Parameters

		RapidMiner.registerParameter(
				new ParameterTypeInt(PROPERTY_RAPIDMINER_GUI_MAX_STATISTICS_ROWS, "", 1, Integer.MAX_VALUE, 100000));
		RapidMiner.registerParameter(
				new ParameterTypeInt(PROPERTY_RAPIDMINER_GUI_MAX_SORTABLE_ROWS, "", 1, Integer.MAX_VALUE, 100000));
		RapidMiner.registerParameter(new ParameterTypeInt(PROPERTY_RAPIDMINER_GUI_MAX_DISPLAYED_VALUES, "", 1,
				Integer.MAX_VALUE, MetaDataViewerTableModel.DEFAULT_MAX_DISPLAYED_VALUES));
		RapidMiner.registerParameter(new ParameterTypeBoolean(PROPERTY_RAPIDMINER_GUI_SNAP_TO_GRID, "", true));
		RapidMiner.registerParameter(new ParameterTypeBoolean(PROPERTY_RAPIDMINER_GUI_MOVE_CONNECTED_OPERATORS, "", true));
		RapidMiner.registerParameter(new ParameterTypeBoolean(PROPERTY_AUTOWIRE_INPUT, "", false));
		RapidMiner.registerParameter(new ParameterTypeBoolean(PROPERTY_AUTOWIRE_OUTPUT, "", false));
		RapidMiner.registerParameter(new ParameterTypeBoolean(PROPERTY_RESOLVE_RELATIVE_REPOSITORY_LOCATIONS, "", true));
		RapidMiner.registerParameter(new ParameterTypeCategory(PROPERTY_CLOSE_RESULTS_BEFORE_RUN, "",
				DecisionRememberingConfirmDialog.PROPERTY_VALUES, DecisionRememberingConfirmDialog.TRUE));
		RapidMiner.registerParameter(new ParameterTypeBoolean(PROPERTY_ADD_BREAKPOINT_RESULTS_TO_HISTORY, "", false));
		RapidMiner.registerParameter(new ParameterTypeBoolean(PROPERTY_CONFIRM_EXIT, "", false));
		RapidMiner.registerParameter(new ParameterTypeCategory(PROPERTY_OPEN_IN_FILEBROWSER, "",
				DecisionRememberingConfirmDialog.PROPERTY_VALUES, DecisionRememberingConfirmDialog.ASK));
		RapidMiner.registerParameter(new ParameterTypeBoolean(PROPERTY_OPTIMIZED_FILEBROWSER, "", false));
		RapidMiner.registerParameter(new ParameterTypeCategory(PROPERTY_CLOSE_ALL_RESULTS_NOW, "",
				DecisionRememberingConfirmDialog.PROPERTY_VALUES, DecisionRememberingConfirmDialog.ASK));
		RapidMiner.registerParameter(new ParameterTypeBoolean(PROPERTY_FETCH_DATA_BASE_TABLES_NAMES, "", true));

		RapidMiner.registerParameter(new ParameterTypeCategory(PROPERTY_DISABLE_OPERATOR_CONNECTION_BEHAVIOR, "", PROPERTY_DISABLE_OPERATOR_CONNECTION_BEHAVIOR_VALUES, PROPERTY_DISABLE_OPERATOR_CONNECTION_BEHAVIOR_DEFAULT_VALUE));
		RapidMiner.registerParameter(new ParameterTypeCategory(PROPERTY_DELETE_OPERATOR_CONNECTION_BEHAVIOR, "", PROPERTY_DELETE_OPERATOR_CONNECTION_BEHAVIOR_VALUES, PROPERTY_DELETE_OPERATOR_CONNECTION_BEHAVIOR_DEFAULT_VALUE));

		RapidMiner.registerParameter(new ParameterTypeBoolean(PROPERTY_SHOW_NO_RESULT_WARNING, "", true));
		RapidMiner.registerParameter(new ParameterTypeCategory(RapidMinerGUI.PROPERTY_FONT_CONFIG, "",
				FontTools.getAvailableFonts(), FontTools.OPTION_INDEX_STANDARD_FONTS));
		RapidMiner.registerParameter(new ParameterTypeCategory(PROPERTY_TRANSFER_USAGESTATS, "",
				RapidMinerGUI.PROPERTY_TRANSFER_USAGESTATS_ANSWERS, UsageStatsTransmissionDialog.ALWAYS));
		RapidMiner.registerParameter(new ParameterTypeCategory(PROPERTY_DRAG_TARGET_HIGHLIGHTING, "",
				PROPERTY_DRAG_TARGET_HIGHLIGHTING_VALUES, DRAG_TARGET_HIGHLIGHTING_FULL));

		// GUI Parameters MainFrame

		RapidMiner.registerParameter(new ParameterTypeDouble(MainFrame.PROPERTY_RAPIDMINER_GUI_VISUALIZATIONS_MAX_ROWS_MODIFIER, "", 0.1, 10.0, 1.0));
		RapidMiner.registerParameter(new ParameterTypeBoolean(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_SHOW_LEGACY_SIMPLE_CHARTS, "", false));
		RapidMiner.registerParameter(new ParameterTypeBoolean(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_SHOW_LEGACY_ADVANCED_CHARTS, "", false));
		RapidMiner.registerParameter(new ParameterTypeInt(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_MATRIXPLOT_SIZE, "", 1,
				Integer.MAX_VALUE, 200));
		RapidMiner.registerParameter(new ParameterTypeInt(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_ROWS_MAXIMUM, "", 1,
				Integer.MAX_VALUE, PlotterPanel.DEFAULT_MAX_NUMBER_OF_DATA_POINTS));
		RapidMiner.registerParameter(new ParameterTypeInt(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_DEFAULT_MAXIMUM, "", -1,
				Integer.MAX_VALUE, 100000));
		RapidMiner.registerParameter(new ParameterTypeInt(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_LEGEND_CLASSLIMIT, "",
				-1, Integer.MAX_VALUE, 10));
		RapidMiner.registerParameter(
				new ParameterTypeColor(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_LEGEND_MINCOLOR, "", java.awt.Color.blue));
		RapidMiner.registerParameter(
				new ParameterTypeColor(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_LEGEND_MAXCOLOR, "", java.awt.Color.red));
		RapidMiner.registerParameter(new ParameterTypeInt(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_COLORS_CLASSLIMIT, "",
				-1, Integer.MAX_VALUE, 10));
		RapidMiner.registerParameter(
				new ParameterTypeInt(PROPERTY_RAPIDMINER_GUI_UNDOLIST_SIZE, "", 1, Integer.MAX_VALUE, 100));
		RapidMiner.registerParameter(new ParameterTypeInt(MainFrame.PROPERTY_RAPIDMINER_GUI_ATTRIBUTEEDITOR_ROWLIMIT, "", -1,
				Integer.MAX_VALUE, 50));
		RapidMiner.registerParameter(new ParameterTypeBoolean(MainFrame.PROPERTY_RAPIDMINER_GUI_BEEP_SUCCESS, "", false));
		RapidMiner.registerParameter(new ParameterTypeBoolean(MainFrame.PROPERTY_RAPIDMINER_GUI_BEEP_ERROR, "", false));
		RapidMiner.registerParameter(new ParameterTypeBoolean(MainFrame.PROPERTY_RAPIDMINER_GUI_BEEP_BREAKPOINT, "", false));
		RapidMiner.registerParameter(new ParameterTypeInt(MainFrame.PROPERTY_RAPIDMINER_GUI_MESSAGEVIEWER_ROWLIMIT, "", -1,
				Integer.MAX_VALUE, LogViewer.DEFAULT_LOG_ENTRY_NUMBER));
		RapidMiner.registerParameter(new ParameterTypeCategory(MainFrame.PROPERTY_RAPIDMINER_GUI_SAVE_BEFORE_RUN, "",
				DecisionRememberingConfirmDialog.PROPERTY_VALUES, DecisionRememberingConfirmDialog.FALSE));
		RapidMiner.registerParameter(
				new ParameterTypeBoolean(MainFrame.PROPERTY_RAPIDMINER_GUI_SAVE_ON_PROCESS_CREATION, "", false));
		RapidMiner.registerParameter(new ParameterTypeCategory(MainFrame.PROPERTY_RAPIDMINER_GUI_AUTO_SWITCH_TO_RESULTVIEW,
				"", DecisionRememberingConfirmDialog.PROPERTY_VALUES, DecisionRememberingConfirmDialog.TRUE));
		RapidMiner.registerParameter(new ParameterTypeCategory(MainFrame.PROPERTY_RAPIDMINER_GUI_LOG_LEVEL, "",
				LogViewer.SELECTABLE_LEVEL_NAMES, LogViewer.DEFAULT_LEVEL_INDEX));

		// Update Parameters
		RapidMiner.registerParameter(
				new ParameterTypeBoolean(PROPERTY_RAPIDMINER_GUI_PURCHASED_NOT_INSTALLED_CHECK, "", true));
		RapidMiner.registerParameter(new ParameterTypeBoolean(PROPERTY_RAPIDMINER_GUI_UPDATE_CHECK, "", true));

		// ---------- Load bundled fonts----------- -----------------
		try {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, Tools.getResourceInputStream("fonts/OpenSans-Light.ttf")));
			ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, Tools.getResourceInputStream("fonts/OpenSans.ttf")));
			ge.registerFont(
					Font.createFont(Font.TRUETYPE_FONT, Tools.getResourceInputStream("fonts/OpenSans-Semibold.ttf")));
			ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, Tools.getResourceInputStream("fonts/OpenSans-Bold.ttf")));
			ge.registerFont(
					Font.createFont(Font.TRUETYPE_FONT, Tools.getResourceInputStream("fonts/OpenSans-ExtraBold.ttf")));
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.MainFrame.font_load_failed", e.getMessage());
		}
		// ----------------------------------------------------------
	}

	private static final int NUMBER_OF_RECENT_FILES = 10;

	private static MainFrame mainFrame;

	private static LinkedList<ProcessLocation> recentFiles = new LinkedList<>();

	private static SafeMode safeMode;

	private static LogModel defaultLogModel;

	private static List<GUIStartupListener> startupListener = new LinkedList<>();
	private static boolean startupStarted = false;

	private static AutoSave autosave = new AutoSave();

	/**
	 * This thread listens for System shutdown and cleans up after shutdown. This included saving
	 * the recent file list and other GUI properties.
	 */
	private static class ShutdownHook extends Thread {

		@Override
		public void run() {
			// only need to do these if at least the MainFrame came up successfully.
			// Otherwise saving might even be detrimental!
			if (RapidMinerGUI.getMainFrame() != null) {
				LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.RapidMinerGUI.running_shutdown_sequence");
				RapidMinerGUI.getMainFrame().getPerspectiveController().shutdown();
				RapidMinerGUI.saveRecentFileList();
				UsageStatistics.getInstance().save();
				RepositoryManager.shutdown();
				UsageStatsScheduler.transmitOnShutdown();
			}
		}
	}

	// private static UpdateManager updateManager = new CommunityUpdateManager();

	public synchronized void run(final String openLocation) throws Exception {
		startupStarted = true;

		// check if resources were copied
		URL logoURL = Tools.getResource("rapidminer_logo.png");
		if (logoURL == null) {
			LogService.getRoot().log(Level.SEVERE, "com.rapidminer.gui.RapidMinerGUI.finding_resources_error");
			RapidMiner.quit(RapidMiner.ExitMode.ERROR);
		}

		// Initialize Docking UI -- important must be done as early as possible!
		DockingUISettings.setInstance(new RapidDockingUISettings());
		DockableContainerFactory.setFactory(new RapidDockableContainerFactory());

		LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.RapidMinerGUI.start_version", new Object[] {
				RapidMiner.getLongVersion(),
				PlatformUtilities.getReleasePlatform(),
				String.format("%s %s", System.getProperty("java.vendor"), System.getProperty("java.version")),
				String.format("%s (%s / %s)", System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch"))
		} );

		// inform listeners that splash is about to be shown
		for (GUIStartupListener sl : startupListener) {
			try {
				sl.splashWillBeShown();
			} catch (RuntimeException e) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.RapidMinerGUI.startup_listener_error", e);
			}
		}

		RapidMiner.showSplash();

		// register what happens when you drag a process from disk into the canvas
		DropFileIntoProcessActionRegistry.getInstance().registerCallback(ProcessEntry.RMP_SUFFIX, new DropFileIntoProcessCallback() {
			@Override
			public List<Operator> createAndConfigureOperatorsForDroppedFile(Path filePath) throws OperatorCreationException {
				// This is a process file
				Operator processEmbedder = OperatorService.createOperator(ProcessEmbeddingOperator.OPERATOR_KEY);
				processEmbedder.setParameter(ProcessEmbeddingOperator.PARAMETER_PROCESS_FILE, filePath.toAbsolutePath().toString());
				return Collections.singletonList(processEmbedder);
			}

			@Override
			public void triggerAction(Path filePath) {
				// do nothing
			}

			@Override
			public boolean willReturnOperator(Path filePath) {
				return true;
			}
		});

		RapidMiner.splashMessage("plaf");
		setupToolTipManager();
		setupGUI();
		FontTools.checkAndSetFallbackUIFont();

		RapidMiner.splashMessage("basic");

		// initialize Global Search framework
		GlobalSearchIndexer.INSTANCE.initialize(FileSystemService.getUserRapidMinerDir().toPath().resolve(FileSystemService.RAPIDMINER_INTERNAL_CACHE_SEARCH_INSTANCE_FULL));

		// initialize RapidMiner
		// As side effect this also initialized the ConstraintManager
		// with the default product and constraints
		RapidMiner.init();

		// store (possibly new) active license (necessary, since no
		// ACTIVE_LICENSE_CHANGED event is fired on startup)
		LicenseManagerRegistry.INSTANCE.get().getAllActiveLicenses().forEach(LicenseTools::storeActiveLicenseProperties);

		// init logging GUI
		defaultLogModel = new LogHandlerModel(LogService.getRoot(),
				SwingTools.createIcon("16/" + I18N.getGUIMessage("gui.logging.default.icon")),
				I18N.getGUIMessage("gui.logging.default.label"), false);
		// set log level for our own log to the defined one
		String levelName = ParameterService.getParameterValue(MainFrame.PROPERTY_RAPIDMINER_GUI_LOG_LEVEL);
		Level logViewLevel = Level.INFO;
		if (levelName != null) {
			logViewLevel = Level.parse(levelName);
		}
		defaultLogModel.setLogLevel(logViewLevel);
		LogService.getRoot().setLevel(logViewLevel);
		LogModelRegistry.INSTANCE.register(defaultLogModel);

		RapidMiner.splashMessage("workspace");

		// check whether current EULA has been accepted
		if (!EULADialog.getEULAAccepted()) {
			// show EULA dialog
			RapidMiner.splashMessage("eula");
			SwingTools.invokeAndWait(() -> new EULADialog().setVisible(true));
		}

		RapidMiner.splashMessage("history");
		loadRecentFileList();

		RepositoryManager.getInstance(null).createRepositoryIfNoneIsDefined();
		RepositoryTools.initFileBrowser();
		RepositoryTools.initOpenFileInOperatorSystem();

		RapidMiner.splashMessage("create_frame");

		SwingTools.invokeAndWait(() -> setMainFrame(new MainFrame()));
		RapidMiner.splashMessage("gui_properties");
		loadGUIProperties(mainFrame);

		// initialize Global Search for actions
		mainFrame.initActionsGlobalSearch();

		// init custom renderers
		BinaryEntryResultRendererRegistry.initialize();

		RapidMiner.splashMessage("plugin_gui");
		Plugin.initPluginGuis(mainFrame);
		// reload the sample repository to include plugin resources
		RepositoryManager.refreshSampleRepository();

		// only load the perspective here after all plugins have registered their dockables
		// otherwise you may end up breaking the perspective and VLDocking exists in invalid states
		// and explodes into the smoking piece of crap it is
		mainFrame.getPerspectiveController().showPerspective(PerspectiveModel.DESIGN);

		autosave.init();

		// inform listeners that the Mainframe was initialized
		for (GUIStartupListener sl : startupListener) {
			try {
				sl.mainFrameInitialized(mainFrame);
			} catch (RuntimeException e) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.RapidMinerGUI.startup_listener_error", e);
			}
		}
		mainFrame.addExtendedProcessEditor(new BlacklistedOperatorProcessEditor());
		// add clean up listener for configurable cache
		mainFrame.addExtendedProcessEditor(new ChangingProcessEditor() {
			@Override
			protected void doWork(Process currentProcess, Process newProcess) {
				if (currentProcess != null) {
					ConnectionAdapterHandler.cleanupAdapters(currentProcess);
				}
			}
		});

		SwingTools.invokeAndWait(mainFrame::finishInitialization);

		// register data source factories
		DataSourceFactoryRegistry.INSTANCE.register(new LocalFileDataSourceFactory());

		// Register file data source factories: binary data source factory is registered first,
		// since it is the first choice for unknown formats.
		DataSourceFactoryRegistry.INSTANCE.register(new BinaryDataSourceFactory());
		DataSourceFactoryRegistry.INSTANCE.register(new ExcelDataSourceFactory());
		DataSourceFactoryRegistry.INSTANCE.register(new CSVDataSourceFactory());

		RapidMiner.splashMessage("checks");
		Plugin.initFinalChecks();

		RapidMiner.splashMessage("show_frame");

		mainFrame.setVisible(true);

		RapidMiner.splashMessage("ready");

		RapidMiner.hideSplash();

		// set log model back to default log of Studio
		// otherwise the last extension log is active
		mainFrame.getLogViewer().getLogSelectionModel().setSelectedLogModel(RapidMinerGUI.getDefaultLogModel());

		// Global Search GUI Provider initialization
		if (GlobalSearchRegistry.INSTANCE.getSearchCategoryById(OperatorGlobalSearch.CATEGORY_ID) != null) {
			GlobalSearchGUIRegistry.INSTANCE.registerSearchVisualizationProvider(GlobalSearchRegistry.INSTANCE.getSearchCategoryById(
					OperatorGlobalSearch.CATEGORY_ID), new OperatorGlobalSearchGUIProvider());
		}
		if (GlobalSearchRegistry.INSTANCE.getSearchCategoryById(RepositoryGlobalSearch.CATEGORY_ID) != null) {
			GlobalSearchGUIRegistry.INSTANCE.registerSearchVisualizationProvider(GlobalSearchRegistry.INSTANCE.getSearchCategoryById(
					RepositoryGlobalSearch.CATEGORY_ID), new RepositoryGlobalSearchGUIProvider());
		}
		if (GlobalSearchRegistry.INSTANCE.getSearchCategoryById(ActionsGlobalSearch.CATEGORY_ID) != null) {
			GlobalSearchGUIRegistry.INSTANCE.registerSearchVisualizationProvider(GlobalSearchRegistry.INSTANCE.getSearchCategoryById(
					ActionsGlobalSearch.CATEGORY_ID), new ActionsGlobalSearchGUIProvider());
		}

		// inform listeners that the Splash screen was hidden
		for (GUIStartupListener sl : startupListener) {
			try {
				sl.splashWasHidden();
			} catch (RuntimeException e) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.RapidMinerGUI.startup_listener_error", e);
			}
		}

		// init visualization after extensions had the chance to add their GUI providers as well
		GlobalSearchPanel.getInstance().initializeSearchResultVisualization();

		UsageStatsScheduler.init();

		if (openLocation != null) {
			if (!RepositoryLocation.isAbsolute(openLocation)) {
				SwingTools.showVerySimpleErrorMessage("malformed_repository_location", openLocation);
			} else {
				OpenAction.open(openLocation, false);
			}
		}

		// check for updates
		Plugin.initPluginUpdateManager();

		// inform listeners that GUI startup has finished
		for (GUIStartupListener sl : startupListener) {
			try {
				sl.startupCompleted();
			} catch (RuntimeException e) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.RapidMinerGUI.startup_listener_error", e);
			}
		}

		// If the CTA extension was not loaded for whatever reason, kill the CTA event aggregator to avoid useless event map bloating
		if (!CtaEventAggregator.INSTANCE.isCtaSystemLive()) {
			CtaEventAggregator.INSTANCE.killAggregator();
		}

		// After all is done, clean up memory for the best possible starting conditions
		System.gc();
	}

	private void setupToolTipManager() {
		// setup tool tip text manager
		ToolTipManager manager = ToolTipManager.sharedInstance();
		manager.setDismissDelay(25000); // original: 4000
		manager.setInitialDelay(1125);   // original: 750
		manager.setReshowDelay(50);    // original: 500
		// heavyweight popups are necessary because of native Chromium window by our browser extension (JxBrowser)
		manager.setLightWeightPopupEnabled(false);
	}

	/**
	 * This default implementation only setup the tool tip durations. Subclasses might override this
	 * method.
	 */
	protected void setupGUI() throws Exception {
		System.setProperty(BookmarkIO.PROPERTY_BOOKMARKS_DIR, FileSystemService.getUserRapidMinerDir().getAbsolutePath());
		System.setProperty(BookmarkIO.PROPERTY_BOOKMARKS_FILE, ".bookmarks");

		try {
			if (SystemInfoUtilities.getOperatingSystem() == OperatingSystem.OSX) {
				// to support OS Xs menu bar shown in the OS X menu bar,
				// we have to load the default system look and feel
				// to exchange the MenuBarUI from RapidLookAndFeel with the
				// default OS X look and feel UI class.
				// See here for more information:
				// http://www.pushing-pixels.org/2008/07/13/swing-applications-and-mac-os-x-menu-bar.html
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				Map<String, Object> macUIDefaults = new HashMap<>();
				macUIDefaults.put("MenuBarUI", UIManager.get("MenuBarUI"));
				UIManager.setLookAndFeel(new RapidLookAndFeel(macUIDefaults));

				// tooltips are painted behind heavyweight windows (e.g. the native Chromium browser window) on OS X
				// despite the call above of ToolTipManager#setLightWeightPopupEnabled(false);
				// so we force a heavyweight popup factory for OS X
				PopupFactory.setSharedInstance(new HeavyweightOSXPopupFactory());
			} else {
				UIManager.setLookAndFeel(new RapidLookAndFeel());
			}
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
					"com.rapidminer.gui.RapidMinerGUI.setting_up_modern_look_and_feel_error"), e);
		}

		// needed because of native browser window which otherwise renders above all popup menus
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
	}

	public static void setMainFrame(final MainFrame mf) {
		mainFrame = mf;
	}

	public static MainFrame getMainFrame() {
		return mainFrame;
	}

	/**
	 * Checks whether the repository location belongs to the currently loaded process.
	 *
	 * @param location
	 *            Repository location to check.
	 * @return true if the process belonging to this entry is currently presented on the GUI, false
	 *         otherwise.
	 */
	public static boolean isMainFrameProcessLocation(RepositoryLocation location) {
		if (getMainFrame() != null && getMainFrame().getProcess() != null) {
			RepositoryLocation currentProcessLocation = getMainFrame().getProcess().getRepositoryLocation();
			if (location.equals(currentProcessLocation)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Resets the location of the MainFrame process without reloading it. Use only if the currently
	 * displayed process is stored in repository, and got renamed or moved.
	 *
	 * @param repositoryLocation
	 *            The new process location.
	 */
	public static void resetProcessLocation(RepositoryProcessLocation repositoryLocation) {
		getMainFrame().getProcess().setProcessLocation(repositoryLocation);
		getMainFrame().setTitle();
		addToRecentFiles(repositoryLocation);
		getMainFrame().updateRecentFileList();
	}

	public static void useProcessFile(final Process process) {
		ProcessLocation location = process.getProcessLocation();
		addToRecentFiles(location);
	}

	public static void addToRecentFiles(final ProcessLocation location) {
		// skip transient repositories, do not add to recent files
		if (location instanceof RepositoryProcessLocation) {
			try {
				if (((RepositoryProcessLocation) location).getRepositoryLocation().getRepository().isTransient()) {
					return;
				}
			} catch (RepositoryException e) {
				// ignore
			}
		}

		if (location != null) {
			while (recentFiles.contains(location)) {
				recentFiles.remove(location);
			}
			recentFiles.addFirst(location);
			while (recentFiles.size() > NUMBER_OF_RECENT_FILES) {
				recentFiles.removeLast();
			}
			saveRecentFileList();
		}
	}

	public static List<ProcessLocation> getRecentFiles() {
		return recentFiles;
	}

	/**
	 * Returns the default {@link LogModel} for RapidMiner Studio.
	 *
	 * @return
	 */
	public static LogModel getDefaultLogModel() {
		return defaultLogModel;
	}

	/**
	 * @return the object that handles autosave information
	 */
	public static AutoSave getAutoSave() {
		return autosave;
	}

	private static void loadRecentFileList() {
		File file = FileSystemService.getUserConfigFile("history");
		if (!file.exists()) {
			return;
		}
		BufferedReader in = null;
		try (FileReader fr = new FileReader(file)) {
			in = new BufferedReader(fr);
			recentFiles.clear();
			String line = null;
			while ((line = in.readLine()) != null) {
				if (line.startsWith("file ")) {
					recentFiles.add(new FileProcessLocation(new File(line.substring(5))));
				} else if (line.startsWith("repository ")) {
					try {
						recentFiles.add(new RepositoryProcessLocation(new RepositoryLocationBuilder().withExpectedDataEntryType(ProcessEntry.class).buildFromAbsoluteLocation(line.substring(11))));
					} catch (MalformedRepositoryLocationException e) {
						LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.RapidMinerGUI.unparseable_line", line);
					}
				} else {
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.RapidMinerGUI.unparseable_line", line);
				}
			}
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
					"com.rapidminer.gui.RapidMinerGUI.reading_history_file_error"), e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.gui.RapidMinerGUI.reading_history_file_error"), e);
				}
			}
		}
	}

	private static void saveRecentFileList() {
		File file = FileSystemService.getUserConfigFile("history");
		try (FileWriter fw = new FileWriter(file); PrintWriter out = new PrintWriter(fw)) {
			for (ProcessLocation loc : recentFiles) {
				out.println(loc.toHistoryFileString());
			}
		} catch (IOException e) {
			SwingTools.showSimpleErrorMessage("cannot_write_history_file", e);
		}
	}

	static void saveGUIProperties() {
		Properties properties = new Properties();
		MainFrame mainFrame = getMainFrame();
		if (mainFrame != null) {
			properties.setProperty(PROPERTY_GEOMETRY_X, "" + (int) mainFrame.getLocation().getX());
			properties.setProperty(PROPERTY_GEOMETRY_Y, "" + (int) mainFrame.getLocation().getY());
			properties.setProperty(PROPERTY_GEOMETRY_WIDTH, "" + mainFrame.getWidth());
			properties.setProperty(PROPERTY_GEOMETRY_HEIGHT, "" + mainFrame.getHeight());
			properties.setProperty(PROPERTY_GEOMETRY_EXTENDED_STATE, "" + mainFrame.getExtendedState());
			properties.setProperty(PROPERTY_SHOW_PARAMETER_HELP, "" + mainFrame.getPropertyPanel().isShowParameterHelp());
			properties.setProperty(PROPERTY_EXPERT_MODE, "" + mainFrame.getPropertyPanel().isExpertMode());
			File file = FileSystemService.getUserConfigFile("gui.properties");
			OutputStream out = null;
			try {
				out = new FileOutputStream(file);
				properties.store(out, "RapidMiner Studio GUI properties");
			} catch (IOException e) {
				LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
						"com.rapidminer.gui.RapidMinerGUI.writing_gui_properties_error", e.getMessage()), e);
			} finally {
				try {
					if (out != null) {
						out.close();
					}
				} catch (IOException e) {
				}
			}
			mainFrame.getResultDisplay().clearAll();
			// mainFrame.getApplicationPerspectiveController().saveAll();
			mainFrame.getPerspectiveController().saveAll();
		}
	}

	private static void loadGUIProperties(final MainFrame mainFrame) {
		Properties properties = new Properties();
		File file = FileSystemService.getUserConfigFile("gui.properties");
		if (file.exists()) {
			InputStream in = null;
			try {
				in = new FileInputStream(file);
				properties.load(in);
			} catch (IOException e) {
				setDefaultGUIProperties();
			} finally {
				try {
					if (in != null) {
						in.close();
					}
				} catch (IOException e) {
					throw new Error(e); // should not occur
				}
			}
			try {
				int x = Integer.parseInt(properties.getProperty(PROPERTY_GEOMETRY_X));
				int y = Integer.parseInt(properties.getProperty(PROPERTY_GEOMETRY_Y));
				mainFrame.setLocation(x, y);
				int extendedState;
				if (properties.getProperty(PROPERTY_GEOMETRY_EXTENDED_STATE) == null) {
					extendedState = Frame.NORMAL;
				} else {
					extendedState = Integer.parseInt(properties.getProperty(PROPERTY_GEOMETRY_EXTENDED_STATE));
				}
				mainFrame.setExtendedState(extendedState);
				if ((extendedState & JFrame.MAXIMIZED_BOTH) != JFrame.MAXIMIZED_BOTH) {
					int width = Integer.parseInt(properties.getProperty(PROPERTY_GEOMETRY_WIDTH));
					int height = Integer.parseInt(properties.getProperty(PROPERTY_GEOMETRY_HEIGHT));
					if (SystemInfoUtilities.getOperatingSystem() == OperatingSystem.OSX) {
						// OS X likes to set up size 0px * 0px if height exceeds available height, so make sure it does not
						GraphicsConfiguration graphicsConfig = mainFrame.getGraphicsConfiguration();
						if (graphicsConfig != null) {
							Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfig);
							int xInsets = insets.left + insets.right;
							// random further -15px because a precise calculation does still produce above 0px error
							int yInsets = insets.top + insets.bottom + 15;
							int availableWidth = graphicsConfig.getDevice().getDisplayMode().getWidth() - xInsets;
							int availableHeight = graphicsConfig.getDevice().getDisplayMode().getHeight() - yInsets;
							width = Math.min(width, availableWidth - x);
							height = Math.min(height, availableHeight - y);
						}
					}
					mainFrame.setSize(new Dimension(width, height));
				}
				mainFrame.getPropertyPanel()
						.setExpertMode(Boolean.valueOf(properties.getProperty(PROPERTY_EXPERT_MODE)).booleanValue());

				// If the property is not set we want the parameter help to be shown
				String showHelpProperty = properties.getProperty(PROPERTY_SHOW_PARAMETER_HELP);
				if (showHelpProperty == null || showHelpProperty.isEmpty()) {
					mainFrame.getPropertyPanel().setShowParameterHelp(true);
				} else {
					mainFrame.getPropertyPanel().setShowParameterHelp(Boolean.valueOf(showHelpProperty).booleanValue());
				}
				if (SystemInfoUtilities.getOperatingSystem() == OperatingSystem.OSX) {
					OSXAdapter.checkForFullScreen(mainFrame);
				}
			} catch (NumberFormatException e) {
				setDefaultGUIProperties();
			}
		} else {
			setDefaultGUIProperties();
		}
	}

	/**
	 * This method sets some default GUI properties. This method can be invoked if the properties
	 * file was not found or produced any error messages (which might happen after version changes).
	 */
	private static void setDefaultGUIProperties() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		mainFrame.setLocation((int) (0.05d * screenSize.getWidth()), (int) (0.05d * screenSize.getHeight()));
		mainFrame.setSize((int) (0.9d * screenSize.getWidth()), (int) (0.9d * screenSize.getHeight()));
		mainFrame.getPropertyPanel().setExpertMode(false);
	}

	public static void main(String[] args) throws Exception {
		// prevent MetaData objects from being deserialized
		// this is to enforce that meta data in the future needs a different serialization
		// in legacy repositories, this restriction is removed
		ObjectInputFilter.Config.setSerialFilter(filterInfo -> {
			Class<?> serialClass = filterInfo.serialClass();
			if (serialClass == null) {
				return ObjectInputFilter.Status.UNDECIDED;
			}
			while (serialClass.isArray()) {
				serialClass = serialClass.getComponentType();
			}
			if (MetaData.class.isAssignableFrom(serialClass)) {
				return ObjectInputFilter.Status.REJECTED;
			}
			return ObjectInputFilter.Status.UNDECIDED;
		});
		Policy.setPolicy(new PluginSandboxPolicy());
		System.setSecurityManager(new PluginSecurityManager());

		ShutdownHooks.addShutdownHook(new ShutdownHook());
		setExecutionMode(
				System.getProperty(PROPERTY_HOME_REPOSITORY_URL) == null ? ExecutionMode.UI : ExecutionMode.WEBSTART);
		// make sure logging is set up correctly
		if (RapidMiner.getExecutionMode().canAccessFilesystem()) {
			Settings.setSetting(SettingsConstants.EXECUTION_WORKING_DIRECTORY, FileSystemService.getStandardWorkingDir());
			Settings.setSetting(SettingsConstants.LOGGING_LOG_FILE, FileSystemService.getLogFile().getAbsolutePath());
		}

		// init ParameterService as early as possible (but after FileSystemService and LogService)
		ParameterService.init();

		// register all Core i18n here
		// legacy plotter i18n
		ResourceBundle plotterBundle = ResourceBundle.getBundle("com.rapidminer.resources.i18n.PlotterMessages", I18N.getI18nLocale(),
				I18N.class.getClassLoader());
		I18N.registerGUIBundle(plotterBundle);

		if (RapidMiner.getExecutionMode().canAccessFilesystem()) {
			// if this file exists, it gets filled with missing translations if locale is not english
			Settings.setSetting(SettingsConstants.I18N_TRANSLATION_HELPER, FileSystemService.getUserRapidMinerDir().toPath().resolve("translation_helper.txt").toAbsolutePath().toString());
		}

		boolean shouldLaunch = true;
		if (!LaunchListener.defaultLaunchWithArguments(args, callArguments -> {
			LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.RapidMinerGUI.received_message",
					Arrays.toString(callArguments));
			mainFrame.requestFocus();
			if (callArguments.length >= 1) {
				String fileArg = callArguments[0];
				if (fileArg.startsWith(RapidMiner.RAPIDMINER_URL_PREFIX)) {
					RMUrlHandler.handleUrl(fileArg);
				} else if (FileUtils.isFile(fileArg) &&
						FilesystemRepositoryAdapter.getInternalSuffixes().contains(FileUtils.getSuffixFromFilename(fileArg))) {
					loadExternalFile(fileArg);
				} else if (!fileArg.trim().isEmpty()) {
					OpenAction.open(fileArg, false);
				}
			}

			return true;
		})) {
			// always start if there are no args so user can start new Studio instances at will
			shouldLaunch = args.length == 0;
		}

		if (shouldLaunch) {
			safeMode = new SafeMode();
			safeMode.launchStarts();
			try {
				launch(args);
			} catch (Throwable t) {
				// log the throwable and actually shutdown the whole process
				LogService.getRoot().log(Level.SEVERE, "Error while starting up RapidMiner Studio. Controlled shutdown.", t);
				System.exit(1);
			}
			safeMode.launchComplete();
		} else {
			LogService.getRoot().log(Level.CONFIG, "com.rapidminer.gui.RapidMinerGUI.other_instance_up");
		}
	}

	/**
	 * Try to load the file of the given path into RapidMiner by looking up the repository which is registered for this
	 * filepath and fetching the entry referenced here. Greedy first match will be used, quits after first match. If
	 * none of the registered repositories match the path and the file has the process rmp suffix, it will be opened
	 * just like import process opens external process files.
	 *
	 * @param filePath absolute path to a file, like a process .rmp file, to be loaded by Studio
	 * @since 9.8.0
	 */
	private static void loadExternalFile(String filePath) {
		List<Repository> repositories = RepositoryManager.getInstance(null).getRepositories();
		Path pathArg = Paths.get(filePath.trim());
		for (Repository repo : repositories) {
			if (repo instanceof FilesystemRepositoryAdapter && pathArg.startsWith(((FilesystemRepositoryAdapter) repo).getRoot())
					|| repo instanceof LocalRepository && pathArg.startsWith(((LocalRepository) repo).getRoot().toPath())) {
				Path root;
				if (repo instanceof FilesystemRepositoryAdapter) {
					root = ((FilesystemRepositoryAdapter) repo).getRoot();
				} else {
					root = ((LocalRepository) repo).getRoot().toPath();
				}
				String[] pathComponents = FileUtils.removeSuffix(root.relativize(pathArg).toString()).split(Pattern.quote(root.getFileSystem().getSeparator()));
				try {
					RepositoryLocation repositoryLocation = new RepositoryLocationBuilder().buildFromPathComponents(repo.getName(), pathComponents);
					DataEntry entry = repositoryLocation.locateData();
					OpenAction.open(entry, true);
				} catch (MalformedRepositoryLocationException | RepositoryException e) {
					SwingTools.showSimpleErrorMessage("while_loading", e, filePath, e.getMessage());
				}
				return;
			}
		}

		// fallback action for external process (only process!): import process
		if (pathArg.endsWith(ProcessEntry.RMP_SUFFIX)) {
			FileProcessLocation location = new FileProcessLocation(new File(filePath.trim()));
			OpenAction.open(location, true);
		}
	}

	private static void launch(final String[] args) throws Exception {
		String openLocation = null;
		String rapidminerURL = null;

		if (args.length > 0) {
			if (args.length != 1) {
				System.out.println("java " + RapidMinerGUI.class.getName() + " [processfile]");
				return;
			}
			if (args[0].startsWith(RapidMiner.RAPIDMINER_URL_PREFIX)) {
				rapidminerURL = args[0];
			} else if (!args[0].trim().isEmpty()) {
				openLocation = args[0];
			}
		}

		new RapidMinerGUI().run(openLocation);
		// make sure urls of type 'rm://{action}' can be used
		RMUrlHandler.initActions();
		if (rapidminerURL != null) {
			RMUrlHandler.handleUrl(rapidminerURL);
		}
	}

	/**
	 * @return the safeMode
	 */
	public static SafeMode getSafeMode() {
		return safeMode;
	}

	public enum DragHighlightMode {
		FULL, BORDER, NONE
	}

	public static DragHighlightMode getDragHighlighteMode() {
		String dragParameter = ParameterService.getParameterValue(PROPERTY_DRAG_TARGET_HIGHLIGHTING);
		if (dragParameter.equals(PROPERTY_DRAG_TARGET_HIGHLIGHTING_VALUES[0])) {
			return DragHighlightMode.FULL;
		} else if (dragParameter.equals(PROPERTY_DRAG_TARGET_HIGHLIGHTING_VALUES[1])) {
			return DragHighlightMode.BORDER;
		} else {
			return DragHighlightMode.NONE;
		}
	}

	/**
	 * @deprecated since 7.0, use {@link com.rapidminer.gui.look.Colors} constants instead
	 */
	@Deprecated
	public static Color getBodyHighlightColor() {
		return new Color(255, 255, 242);
	}

	/**
	 * @deprecated since 7.0, use {@link com.rapidminer.gui.look.Colors} constants instead
	 */
	@Deprecated
	public static Color getBorderHighlightColor() {
		return SwingTools.RAPIDMINER_ORANGE;
	}

	/**
	 * Register a {@link GUIStartupListener}.
	 *
	 * @param listener
	 *            the listener to register
	 * @throws IllegalStateException
	 *             if the startup process has already been started
	 */
	public static synchronized void registerStartupListener(GUIStartupListener listener) {
		if (startupStarted) {
			throw new IllegalStateException("Cannot register a Startup listener after startup has been started");
		}
		startupListener.add(listener);
	}
}
