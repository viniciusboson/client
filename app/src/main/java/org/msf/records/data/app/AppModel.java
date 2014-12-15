package org.msf.records.data.app;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.AsyncTask;

import org.msf.records.data.app.converters.AppTypeConverter;
import org.msf.records.data.app.converters.AppTypeConverters;
import org.msf.records.data.app.tasks.AppAddPatientAsyncTask;
import org.msf.records.data.app.tasks.AppAsyncTaskFactory;
import org.msf.records.data.app.tasks.AppUpdatePatientAsyncTask;
import org.msf.records.data.app.tasks.FetchSingleAsyncTask;
import org.msf.records.events.CrudEventBus;
import org.msf.records.events.data.SingleItemFetchedEvent;
import org.msf.records.events.data.TypedCursorFetchedEvent;
import org.msf.records.filter.SimpleSelectionFilter;
import org.msf.records.filter.UuidFilter;
import org.msf.records.net.Server;
import org.msf.records.sync.PatientProjection;
import org.msf.records.sync.PatientProviderContract;

import de.greenrobot.event.NoSubscriberEvent;

/**
 * A model that manages all data access within the application.
 *
 * <p>This model's {@code fetch} methods often provide {@link TypedCursor}s as results, which MUST
 * be closed when the consumer is done with them.
 *
 * <p>Updates done through this model are written through to a backing {@link Server}; callers do
 * not need to worry about the implementation details of this.
 */
public class AppModel {

    private final ContentResolver mContentResolver;
    private final AppTypeConverters mConverters;
    private final AppAsyncTaskFactory mTaskFactory;

    AppModel(ContentResolver contentResolver, AppTypeConverters converters, AppAsyncTaskFactory taskFactory) {
    	mContentResolver = contentResolver;
        mConverters = converters;
        mTaskFactory = taskFactory;
    }

    /**
     * Asynchronously fetches patients, posting a {@link TypedCursorFetchedEvent} with
     * {@link AppPatient}s on the specified event bus when complete.
     */
    public void fetchPatients(CrudEventBus bus, SimpleSelectionFilter filter, String constraint) {
        bus.register(new CrudEventBusErrorSubscriber(bus));

        FetchTypedCursorAsyncTask<AppPatient> task = new FetchTypedCursorAsyncTask<AppPatient>(
                mContentResolver, filter, constraint, mConverters.patient, bus);
        task.execute();
    }

    /**
     * Asynchronously fetches a single patient by UUID, posting a {@link SingleItemFetchedEvent}
     * with the {@link AppPatient} on the specified event bus when complete.
     */
    public void fetchSinglePatient(CrudEventBus bus, String uuid) {
        FetchSingleAsyncTask<AppPatient> task = mTaskFactory.newFetchSingleAsyncTask(
                new UuidFilter(), uuid, mConverters.patient, bus);
        task.execute();
    }

    /**
     * Asynchronously fetches patients, posting a {@link TypedCursorFetchedEvent} with
     * {@link AppUser}s on the specified event bus when complete.
     */
    public void fetchUsers(CrudEventBus bus) {
        // Register for error events so that we can close cursors if we need to.
        bus.register(new CrudEventBusErrorSubscriber(bus));

        // TODO(dxchen): Asynchronously fetch users.
    }

    // TODO(dxchen): Consider defining a special PatientUpdatedEvent.
    /**
     * Asynchronously adds a patient, posting a {@link SingleItemFetchedEvent} with the newly-added
     * patient on the specified event bus when complete.
     */
    public void addPatient(CrudEventBus bus, AppPatientDelta patientDelta) {
        AppAddPatientAsyncTask task = mTaskFactory.newAddPatientAsyncTask(patientDelta, bus);
        task.execute();
    }

    // TODO(dxchen): Consider defining a special PatientUpdatedEvent.
    /**
     * Asynchronously updates a patient, posting a {@link SingleItemFetchedEvent} with the updated
     * {@link AppPatient} on the specified event bus when complete.
     */
    public void updatePatient(CrudEventBus bus, String uuid, AppPatientDelta patientDelta) {
        AppUpdatePatientAsyncTask task =
                mTaskFactory.newUpdatePatientAsyncTask(uuid, patientDelta, bus);
        task.execute();
    }

    /**
     * A subscriber that handles error events posted to {@link CrudEventBus}es.
     */
    @SuppressWarnings("unused") // Called by reflection from event bus.
    private static class CrudEventBusErrorSubscriber {

    	// TODO(rjlothian): This memory freeing strategy feels error prone.
    	// We don't unregister from the bus if delivery succeeds...
        private final CrudEventBus mBus;

        public CrudEventBusErrorSubscriber(CrudEventBus bus) {
            mBus = bus;
        }

        /**
         * Handles {@link NoSubscriberEvent}s.
         */
        public void onEvent(NoSubscriberEvent event) {
            if (event.originalEvent instanceof TypedCursorFetchedEvent<?>) {
                // If no subscribers were registered for a DataFetchedEvent, then the TypedCursor in
                // the event won't be managed by anyone else; therefore, we close it ourselves.
                ((TypedCursorFetchedEvent<?>) event.originalEvent).mCursor.close();
            }

            mBus.unregister(this);
        }
    }

    private static class FetchTypedCursorAsyncTask<T>
    		extends AsyncTask<Void, Void, TypedCursor<T>> {

		private final ContentResolver mContentResolver;
		private final SimpleSelectionFilter mFilter;
		private final String mConstraint;
		private final AppTypeConverter<T> mConverter;
		private final CrudEventBus mBus;

		public FetchTypedCursorAsyncTask(
		        ContentResolver contentResolver,
		        SimpleSelectionFilter filter,
		        String constraint,
		        AppTypeConverter<T> converter,
		        CrudEventBus bus) {
		    mContentResolver = contentResolver;
		    mFilter = filter;
		    mConstraint = constraint;
		    mConverter = converter;
		    mBus = bus;
		}

		@Override
		protected  TypedCursor<T> doInBackground(Void... voids) {
		    // TODO(dxchen): Refactor this (and possibly FilterQueryProviderFactory) to support
		    // different types of queries.
		    Cursor cursor = null;
		    try {
		        cursor = mContentResolver.query(
		                PatientProviderContract.CONTENT_URI,
		                PatientProjection.getProjectionColumns(),
		                mFilter.getSelectionString(),
		                mFilter.getSelectionArgs(mConstraint),
		                null);

		        return new TypedConvertedCursor<>(mConverter, cursor);
		    } finally {
		        if (cursor != null) {
		            cursor.close();
		        }
		    }
		}

		@Override
		protected void onPostExecute(TypedCursor<T> result) {
		    mBus.post(new TypedCursorFetchedEvent<T>(result));
		}
	}
}
