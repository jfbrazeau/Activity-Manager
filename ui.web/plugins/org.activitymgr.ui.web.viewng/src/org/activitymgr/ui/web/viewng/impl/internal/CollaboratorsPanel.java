package org.activitymgr.ui.web.viewng.impl.internal;

import java.util.stream.Stream;

import org.activitymgr.ui.web.logic.ICollaboratorsTabLogic;
import org.activitymgr.ui.web.logic.ILogic.IView;
import org.activitymgr.ui.web.logic.ITableCellProviderCallback;
import org.activitymgr.ui.web.viewng.AbstractTabPanel;
import org.activitymgr.ui.web.viewng.IResourceCache;
import org.activitymgr.ui.web.viewng.impl.internal.util.DefaultStyleGenerator;

import com.google.inject.Inject;
import com.vaadin.data.HasValue;
import com.vaadin.data.ValueProvider;
import com.vaadin.data.provider.AbstractBackEndDataProvider;
import com.vaadin.data.provider.Query;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.server.Setter;
import com.vaadin.shared.ui.grid.ColumnResizeMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.TextField;
import com.vaadin.ui.components.grid.Editor;

@SuppressWarnings("serial")
public class CollaboratorsPanel extends AbstractTabPanel<ICollaboratorsTabLogic> implements ICollaboratorsTabLogic.View {

	private Grid<Long> collaboratorsTable;

	@Inject
	public CollaboratorsPanel(IResourceCache resourceCache) {
		super(resourceCache);
	}

	@Override
	protected Component createBodyComponent() {
		// Collaborators table
		collaboratorsTable = new Grid<Long>();
		collaboratorsTable.setSizeFull();
		collaboratorsTable.setColumnResizeMode(ColumnResizeMode.SIMPLE);
		return collaboratorsTable;
	}
    @Override
	public void setCollaboratorsProviderCallback(
			final ITableCellProviderCallback<Long> collaboratorsProvider) {
		AbstractBackEndDataProvider<Long, SerializablePredicate<Long>> dataProvider = new AbstractBackEndDataProvider<Long, SerializablePredicate<Long>>() {
			@Override
			public boolean isInMemory() {
				return false;
			}

			@Override
			protected Stream<Long> fetchFromBackEnd(
					Query<Long, SerializablePredicate<Long>> query) {
				return collaboratorsProvider.getRootElements().stream();
			}

			@Override
			protected int sizeInBackEnd(
					Query<Long, SerializablePredicate<Long>> query) {
				return collaboratorsProvider.getRootElements().size();
			}
		};
		collaboratorsTable.setDataProvider(dataProvider);
		for (String propertyId : collaboratorsProvider.getPropertyIds()) {
			Column<Long, String> column = collaboratorsTable
					.addColumn(new ValueProvider<Long, String>() {
						@Override
						public String apply(Long source) {
							IView<?> cell = collaboratorsProvider.getCell(
									source, (String) propertyId);
							return String.valueOf(((HasValue<?>) cell)
									.getValue());
						}
					});
			column.setCaption(propertyId);
			column.setStyleGenerator(new DefaultStyleGenerator<Long>(
					collaboratorsProvider.getColumnAlign(propertyId)));
			column.setEditorComponent(new TextField(),
					new Setter<Long, String>() {
						@Override
						public void accept(Long bean, String fieldvalue) {
							System.out.println("Update " + fieldvalue);
						}
					});
			column.setEditable(true);
		}
		Editor<Long> editor = collaboratorsTable.getEditor();
		editor.setEnabled(true);
		editor.setBuffered(false);
	}
    
}
