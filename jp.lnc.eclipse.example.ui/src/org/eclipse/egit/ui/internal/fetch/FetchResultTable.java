package org.eclipse.egit.ui.internal.fetch;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.TrackingRefUpdate;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * Component displaying table with results of fetch operation.
 */
class FetchResultTable {
	private static final int TABLE_PREFERRED_HEIGHT = 600;

	private static final int TABLE_PREFERRED_WIDTH = 300;

	private static final int COLUMN_SRC_WEIGHT = 10;

	private static final int COLUMN_DST_WEIGHT = 10;

	private static final int COLUMN_STATUS_WEIGHT = 7;

	private final Composite tablePanel;

	private final TableViewer tableViewer;

	private final Color rejectedColor;

	private final Color updatedColor;

	private final Color upToDateColor;

	private ObjectReader reader;

	private Map<ObjectId, String> abbrevations;

	FetchResultTable(final Composite parent) {
		tablePanel = new Composite(parent, SWT.NONE);
		tablePanel.setLayout(new GridLayout());
		final GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
		layoutData.heightHint = TABLE_PREFERRED_HEIGHT;
		layoutData.widthHint = TABLE_PREFERRED_WIDTH;
		tableViewer = new TableViewer(tablePanel);
		ColumnViewerToolTipSupport.enableFor(tableViewer);
		final Table table = tableViewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);

		rejectedColor = new Color(parent.getDisplay(), 255, 0, 0);
		updatedColor = new Color(parent.getDisplay(), 0, 255, 0);
		upToDateColor = new Color(parent.getDisplay(), 245, 245, 245);

		tablePanel.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if (reader != null)
					reader.release();

				// dispose of our allocated Color instances
				rejectedColor.dispose();
				updatedColor.dispose();
				upToDateColor.dispose();
			}
		});

		tableViewer.setContentProvider(new TrackingRefUpdateContentProvider());
		tableViewer.setInput(null);

		createTableColumns();
	}

	void setData(final Repository db, final FetchResult fetchResult) {
		tableViewer.setInput(null);
		this.reader = db.newObjectReader();
		this.abbrevations = new HashMap<ObjectId, String>();
		tableViewer.setInput(fetchResult);
	}

	Control getControl() {
		return tablePanel;
	}

	private void createTableColumns() {
		final TableColumnLayout layout = new TableColumnLayout();
		tablePanel.setLayout(layout);

		final TableViewerColumn srcViewer = createColumn(layout,
				UIText.FetchResultTable_columnSrc, COLUMN_SRC_WEIGHT, SWT.LEFT);
		srcViewer.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((TrackingRefUpdate) element).getRemoteName();
			}
		});

		final TableViewerColumn dstViewer = createColumn(layout,
				UIText.FetchResultTable_columnDst, COLUMN_DST_WEIGHT, SWT.LEFT);
		dstViewer.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((TrackingRefUpdate) element).getLocalName();
			}
		});

		final TableViewerColumn statusViewer = createColumn(layout,
				UIText.FetchResultTable_columnStatus, COLUMN_STATUS_WEIGHT,
				SWT.LEFT);
		statusViewer.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(final Object element) {
				final TrackingRefUpdate tru = (TrackingRefUpdate) element;
				final RefUpdate.Result r = tru.getResult();
				if (r == RefUpdate.Result.LOCK_FAILURE)
					return UIText.FetchResultTable_statusLockFailure;

				if (r == RefUpdate.Result.IO_FAILURE)
					return UIText.FetchResultTable_statusIOError;

				if (r == RefUpdate.Result.NEW) {
					if (tru.getRemoteName().startsWith(Constants.R_HEADS))
						return UIText.FetchResultTable_statusNewBranch;
					else if (tru.getLocalName().startsWith(Constants.R_TAGS))
						return UIText.FetchResultTable_statusNewTag;
					return UIText.FetchResultTable_statusNew;
				}

				if (r == RefUpdate.Result.FORCED) {
					final String o = safeAbbreviate(tru.getOldObjectId());
					final String n = safeAbbreviate(tru.getNewObjectId());
					return o + "..." + n; //$NON-NLS-1$
				}

				if (r == RefUpdate.Result.FAST_FORWARD) {
					final String o = safeAbbreviate(tru.getOldObjectId());
					final String n = safeAbbreviate(tru.getNewObjectId());
					return o + ".." + n; //$NON-NLS-1$
				}

				if (r == RefUpdate.Result.REJECTED)
					return UIText.FetchResultTable_statusRejected;
				if (r == RefUpdate.Result.NO_CHANGE)
					return UIText.FetchResultTable_statusUpToDate;
				throw new IllegalArgumentException(NLS.bind(
						UIText.FetchResultTable_statusUnexpected, r));
			}

			private String safeAbbreviate(ObjectId id) {
				String abbrev = abbrevations.get(id);
				if (abbrev == null) {
					try {
						abbrev = reader.abbreviate(id).name();
					} catch (IOException cannotAbbreviate) {
						abbrev = id.name();
					}
					abbrevations.put(id, abbrev);
				}
				return abbrev;
			}

			@Override
			public String getToolTipText(final Object element) {
				final Result result = ((TrackingRefUpdate) element).getResult();
				switch (result) {
				case FAST_FORWARD:
					return UIText.FetchResultTable_statusDetailFastForward;
				case FORCED:
				case REJECTED:
					return UIText.FetchResultTable_statusDetailNonFastForward;
				case NEW:
				case NO_CHANGE:
					return null;
				case IO_FAILURE:
					return UIText.FetchResultTable_statusDetailIOError;
				case LOCK_FAILURE:
					return UIText.FetchResultTable_statusDetailCouldntLock;
				default:
					throw new IllegalArgumentException(NLS.bind(
							UIText.FetchResultTable_statusUnexpected, result));
				}
			}

			@Override
			public Color getBackground(final Object element) {
				final Result result = ((TrackingRefUpdate) element).getResult();
				switch (result) {
				case FAST_FORWARD:
				case FORCED:
				case NEW:
					return updatedColor;
				case NO_CHANGE:
					return upToDateColor;
				case IO_FAILURE:
				case LOCK_FAILURE:
				case REJECTED:
					return rejectedColor;
				default:
					throw new IllegalArgumentException(NLS.bind(
							UIText.FetchResultTable_statusUnexpected, result));
				}
			}
		});
	}

	private TableViewerColumn createColumn(
			final TableColumnLayout columnLayout, final String text,
			final int weight, final int style) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(
				tableViewer, style);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(text);
		columnLayout.setColumnData(column, new ColumnWeightData(weight));
		return viewerColumn;
	}
}
