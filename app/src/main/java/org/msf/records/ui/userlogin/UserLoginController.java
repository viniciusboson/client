package org.msf.records.ui.userlogin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.msf.records.R;
import org.msf.records.events.user.KnownUsersLoadFailedEvent;
import org.msf.records.events.user.KnownUsersLoadedEvent;
import org.msf.records.events.user.UserAddFailedEvent;
import org.msf.records.events.user.UserAddedEvent;
import org.msf.records.net.model.User;
import org.msf.records.ui.ProgressFragment;
import org.msf.records.ui.dialogs.AddNewUserDialogFragment;
import org.msf.records.user.UserManager;
import org.msf.records.utils.EventBusRegistrationInterface;
import org.msf.records.utils.Logger;

import com.google.common.collect.Ordering;

/**
 * Controller for {@link UserLoginActivity}.
 *
 * <p>Don't add untestable dependencies to this class.
 */
final class UserLoginController {

    private static final Logger LOG = Logger.create();

    private static final boolean DEBUG = true;

    public interface Ui {

        void showAddNewUserDialog();

        void showSettings();

        void showErrorToast(int stringResourceId);

        void showSyncFailedDialog(boolean show);

        void showTentSelectionScreen();
    }

    public interface FragmentUi {

        void showSpinner(boolean show);

        void showUsers(List<User> users);
    }

    private final EventBusRegistrationInterface mEventBus;
    private final Ui mUi;
    private final FragmentUi mFragmentUi;
    private final DialogUi mDialogUi = new DialogUi();
    private final UserManager mUserManager;
    private final List<User> mUsersSortedByName = new ArrayList<>();
    private final BusEventSubscriber mSubscriber = new BusEventSubscriber();

    public UserLoginController(
            UserManager userManager,
            EventBusRegistrationInterface eventBus,
            Ui ui,
            FragmentUi fragmentUi) {
        mUserManager = userManager;
        mEventBus = eventBus;
        mUi = ui;
        mFragmentUi = fragmentUi;
    }

    public void init() {
        mEventBus.register(mSubscriber);
        mFragmentUi.showSpinner(true);
        mUserManager.loadKnownUsers();
    }

    /** Attempts to reload users. */
    public void onSyncRetry() {
        mFragmentUi.showSpinner(true);
        mUserManager.loadKnownUsers();
    }

    public void suspend() {
        mEventBus.unregister(mSubscriber);
    }

    /** Call when the user presses the 'add user' button. */
    public void onAddUserPressed() {
        mUi.showAddNewUserDialog();
    }

    /** Call when the user presses the settings button. */
    public void onSettingsPressed() {
        mUi.showSettings();
    }

    /** Call when the user taps to select a user. */
    public void onUserSelected(User user) {
        mUserManager.setActiveUser(user);
        mUi.showTentSelectionScreen();
    }

    @SuppressWarnings("unused") // Called by reflection from event bus.
    private final class BusEventSubscriber {

        /** Updates the UI when the list of users is loaded. */
        public void onEventMainThread(KnownUsersLoadedEvent event) {
            LOG.d("Loaded list of " + event.knownUsers.size() + " users");
            mUsersSortedByName.clear();
            mUsersSortedByName
                    .addAll(Ordering.from(User.COMPARATOR_BY_NAME).sortedCopy(event.knownUsers));
            mFragmentUi.showUsers(mUsersSortedByName);
            mFragmentUi.showSpinner(false);
            mUi.showSyncFailedDialog(false);
        }

        public void onEventMainThread(KnownUsersLoadFailedEvent event) {
            LOG.e("Failed to load list of users");
            // TODO(akalachman): Replace toast here with dialog a la tent selection.
            mUi.showSyncFailedDialog(true);
        }

        public void onEventMainThread(UserAddedEvent event) {
            mUi.showSyncFailedDialog(false);  // Just in case.
            LOG.d("User added");
            insertIntoSortedList(mUsersSortedByName, User.COMPARATOR_BY_NAME, event.addedUser);
            mFragmentUi.showUsers(mUsersSortedByName);
            mFragmentUi.showSpinner(false);
        }

        public void onEventMainThread(UserAddFailedEvent event) {
            LOG.d("Failed to add user");
            mUi.showErrorToast(errorToStringId(event));
            mFragmentUi.showSpinner(false);
        }
    }

    public AddNewUserDialogFragment.Ui getDialogUi() {
        return mDialogUi;
    }

    public final class DialogUi implements AddNewUserDialogFragment.Ui {

        @Override
        public void showSpinner(boolean show) {
            mFragmentUi.showSpinner(show);
        }
    }

    /** Converts a {@link UserAddFailedEvent} to an error string resource id. */
    private static int errorToStringId(UserAddFailedEvent event) {
        switch (event.reason) {
            case UserAddFailedEvent.REASON_UNKNOWN:
                return R.string.add_user_unknown_error;
            case UserAddFailedEvent.REASON_INVALID_USER:
                return R.string.add_user_invalid_user;
            case UserAddFailedEvent.REASON_USER_EXISTS_LOCALLY:
                return R.string.add_user_user_exists_locally;
            case UserAddFailedEvent.REASON_USER_EXISTS_ON_SERVER:
                return R.string.add_user_user_exists_on_server;
            case UserAddFailedEvent.REASON_CONNECTION_ERROR:
                return R.string.add_user_connection_error;
            default:
                return R.string.add_user_unknown_error;
        }
    }

    /**
     * Given a sorted list, inserts a new element in the correct position to maintain the sorted
     * order.
     */
    private static <T> void insertIntoSortedList(
            List<T> list, Comparator<T> comparator, T newItem) {
        int index;
        for (index = 0; index < list.size(); index++) {
            if (comparator.compare(list.get(index), newItem) > 0) {
                break;
            }
        }
        list.add(index, newItem);
    }
}
