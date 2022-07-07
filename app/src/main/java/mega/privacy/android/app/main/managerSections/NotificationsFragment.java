package mega.privacy.android.app.main.managerSections;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.main.adapters.MegaNotificationsAdapter;
import mega.privacy.android.app.utils.ColorUtils;
import mega.privacy.android.app.utils.ContactUtil;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaUser;
import nz.mega.sdk.MegaUserAlert;
import timber.log.Timber;

public class NotificationsFragment extends Fragment implements View.OnClickListener {

    private static final String BUNDLE_RECYCLER_LAYOUT = "classname.recycler.layout";

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;

    DatabaseHandler dbH;

    Context context;
    ActionBar aB;
    MegaNotificationsAdapter adapterList;
    RelativeLayout mainRelativeLayout;

    RecyclerView listView;
    LinearLayoutManager mLayoutManager;

    ArrayList<MegaUserAlert> notifications;

    int numberOfClicks = 0;

    //Empty screen
    TextView emptyTextView;
    RelativeLayout emptyLayout;
    ImageView emptyImageView;

    float density;
    DisplayMetrics outMetrics;
    Display display;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("onCreate");

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        }

        if (megaChatApi == null) {
            megaChatApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaChatApi();
        }

        dbH = DatabaseHandler.getDbHandler(getActivity());
    }

    public void checkScroll() {
        if (listView != null) {
            if (listView.canScrollVertically(-1)) {
                ((ManagerActivity) context).changeAppBarElevation(true);
            } else {
                ((ManagerActivity) context).changeAppBarElevation(false);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        Timber.d("onCreateView");

        display = ((Activity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        density = getResources().getDisplayMetrics().density;

        View v = inflater.inflate(R.layout.notifications_fragment, container, false);

        listView = (RecyclerView) v.findViewById(R.id.notifications_list_view);

        listView.setClipToPadding(false);
        mLayoutManager = new LinearLayoutManager(context);
        listView.setHasFixedSize(true);
        listView.setItemAnimator(new DefaultItemAnimator());
        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (context instanceof ManagerActivity) {
                    checkScroll();
                }
            }
        });

        listView.setLayoutManager(mLayoutManager);

        emptyLayout = (RelativeLayout) v.findViewById(R.id.empty_layout_notifications);

        emptyTextView = (TextView) v.findViewById(R.id.empty_text_notifications);
        emptyImageView = (ImageView) v.findViewById(R.id.empty_image_view_notifications);

        mainRelativeLayout = (RelativeLayout) v.findViewById(R.id.main_relative_layout_notifications);

        setNotifications();

        return v;
    }

    public static NotificationsFragment newInstance() {
        Timber.d("newInstance");
        NotificationsFragment fragment = new NotificationsFragment();
        return fragment;
    }

    public void setNotifications() {
        Timber.d("setNotifications");

        notifications = megaApi.getUserAlerts();

        Collections.reverse(notifications);

        if (isAdded()) {
            if (adapterList == null) {
                Timber.w("adapterList is NULL");
                adapterList = new MegaNotificationsAdapter(context, this, notifications, listView);

            } else {
                adapterList.setNotifications(notifications);
            }

            listView.setAdapter(adapterList);

            if (notifications == null || notifications.isEmpty()) {
                listView.setVisibility(View.GONE);
                emptyImageView.setVisibility(View.VISIBLE);
                emptyTextView.setVisibility(View.VISIBLE);

                if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    emptyImageView.setImageResource(R.drawable.empty_notification_landscape);
                } else {
                    emptyImageView.setImageResource(R.drawable.empty_notification_portrait);
                }

                String textToShow = String.format(getString(R.string.context_empty_notifications));
                try {
                    textToShow = textToShow.replace("[A]", "<font color=\'"
                            + ColorUtils.getColorHexString(context, R.color.grey_900_grey_100)
                            + "\'>");
                    textToShow = textToShow.replace("[/A]", "</font>");
                    textToShow = textToShow.replace("[B]", "<font color=\'"
                            + ColorUtils.getColorHexString(context, R.color.grey_300_grey_600)
                            + "\'>");
                    textToShow = textToShow.replace("[/B]", "</font>");
                } catch (Exception e) {
                }
                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }
                emptyTextView.setText(result);

            } else {
Timber.d("Number of notifications: %s",  notifications.size());
                listView.setVisibility(View.VISIBLE);
                emptyLayout.setVisibility(View.GONE);
            }

            ((ManagerActivity) context).markNotificationsSeen(false);
        }
    }

    public void addNotification(MegaUserAlert newAlert) {
        Timber.d("addNotification");
        //Check scroll position
        boolean shouldScroll = false;
        if (!listView.canScrollVertically(-1)) {
            shouldScroll = true;
        }

        notifications.add(0, newAlert);
        if (adapterList != null) {
            adapterList.notifyItemInserted(0);
        }

        //Before scrolling be sure it was on the first
        if (shouldScroll) {
            listView.smoothScrollToPosition(0);
        }

        ((ManagerActivity) context).markNotificationsSeen(false);
    }

    @Override
    public void onClick(View v) {
        Timber.d("onClick");

        switch (v.getId()) {
            case R.id.empty_image_view_chat: {
                numberOfClicks++;
                Timber.d("Number of clicks: %s", numberOfClicks);
                if (numberOfClicks >= 5) {
                    numberOfClicks = 0;

                }

                break;
            }
        }
    }

    public void itemClick(int position) {
        Timber.d("Position: %s", position);
        MegaUserAlert notif = notifications.get(position);

        int alertType = notif.getType();

        switch (alertType) {

            case MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_REQUEST:
            case MegaUserAlert.TYPE_CONTACTCHANGE_CONTACTESTABLISHED:
            case MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_ACCEPTED:
            case MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_REMINDER: {
                MegaUser contact = megaApi.getContact(notif.getEmail());
                if (contact != null && contact.getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                    Timber.d("Go to contact info");
                    ContactUtil.openContactInfoActivity(context, notif.getEmail());
                } else {
                    ArrayList<MegaContactRequest> contacts = megaApi.getIncomingContactRequests();
                    if (contacts != null) {
                        for (int i = 0; i < contacts.size(); i++) {
                            MegaContactRequest c = contacts.get(i);
                            if (c.getSourceEmail().equals(notif.getEmail())) {
                                Timber.d("Go to Received requests");
                                ((ManagerActivity) context).navigateToContactRequests();
                                break;
                            }
                        }
                    }

                }
                Timber.w("Request not found");
                break;
            }
            case MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTOUTGOING_ACCEPTED: {
                MegaUser contact = megaApi.getContact(notif.getEmail());
                if (contact != null && contact.getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                    Timber.d("Go to contact info");
                    ContactUtil.openContactInfoActivity(context, notif.getEmail());
                }
                break;
            }
            case MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_CANCELLED:
            case MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_IGNORED:
            case MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTINCOMING_DENIED:
            case MegaUserAlert.TYPE_UPDATEDPENDINGCONTACTOUTGOING_DENIED:
            case MegaUserAlert.TYPE_CONTACTCHANGE_DELETEDYOU:
            case MegaUserAlert.TYPE_CONTACTCHANGE_ACCOUNTDELETED:
            case MegaUserAlert.TYPE_CONTACTCHANGE_BLOCKEDYOU: {
                Timber.d("Do not navigate");
                break;
            }
            case MegaUserAlert.TYPE_PAYMENT_SUCCEEDED:
            case MegaUserAlert.TYPE_PAYMENT_FAILED:
            case MegaUserAlert.TYPE_PAYMENTREMINDER: {
                Timber.d("Go to My Account");
                ((ManagerActivity) context).navigateToMyAccount();
                break;
            }
            case MegaUserAlert.TYPE_TAKEDOWN:
            case MegaUserAlert.TYPE_TAKEDOWN_REINSTATED: {
                if (notif.getNodeHandle() != -1) {
                    MegaNode node = megaApi.getNodeByHandle(notif.getNodeHandle());
                    if (node != null) {
                        if (node.isFile()) {
                            ((ManagerActivity) context).openLocation(node.getParentHandle(), null);
                        } else {
                            ((ManagerActivity) context).openLocation(notif.getNodeHandle(), null);
                        }
                    }
                }
                break;
            }
            case MegaUserAlert.TYPE_NEWSHARE:
            case MegaUserAlert.TYPE_REMOVEDSHAREDNODES:
            case MegaUserAlert.TYPE_DELETEDSHARE: {
                Timber.d("Go to open corresponding location");
                if (notif.getNodeHandle() != -1 && megaApi.getNodeByHandle(notif.getNodeHandle()) != null) {
                    ((ManagerActivity) context).openLocation(notif.getNodeHandle(), null);
                }
                break;
            }
            case MegaUserAlert.TYPE_NEWSHAREDNODES: {
                Timber.d("Go to open corresponding location");
                int numOfFolders = (int) notif.getNumber(0);
                int numOfFiles = (int) notif.getNumber(1);
                int childNodeHandleCount = numOfFolders + numOfFiles;

                long[] childNodeHandleList = new long[childNodeHandleCount];
                for (int i = 0; i < childNodeHandleCount; i++) {
                    childNodeHandleList[i] = notif.getHandle(i);
                }
                if (notif.getNodeHandle() != -1 && megaApi.getNodeByHandle(notif.getNodeHandle()) != null) {
                    ((ManagerActivity) context).openLocation(notif.getNodeHandle(), childNodeHandleList);
                }
                break;
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity) activity).getSupportActionBar();
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        aB = ((AppCompatActivity) context).getSupportActionBar();
    }

    public void updateNotifications(List<MegaUserAlert> updatedUserAlerts) {
        Timber.d("updateNotifications");

        if (!isAdded()) {
            Timber.d("return!");
            return;
        }

        for (int i = 0; i < updatedUserAlerts.size(); i++) {

            if (updatedUserAlerts.get(i).isOwnChange()) {
                Timber.d("isOwnChange");
                continue;
            }
Timber.d("User alert type: %s",  updatedUserAlerts.get(i).getType());
            long idToUpdate = updatedUserAlerts.get(i).getId();
            int indexToReplace = -1;

            ListIterator<MegaUserAlert> itrReplace = notifications.listIterator();
            while (itrReplace.hasNext()) {
                MegaUserAlert notification = itrReplace.next();

                if (notification != null) {
                    if (notification.getId() == idToUpdate) {
                        indexToReplace = itrReplace.nextIndex() - 1;
                        break;
                    }
                } else {
                    break;
                }
            }
            if (indexToReplace != -1) {
                Timber.d("Index to replace: %s", indexToReplace);

                notifications.set(indexToReplace, updatedUserAlerts.get(i));
                if (adapterList != null) {
                    adapterList.notifyItemChanged(indexToReplace);
                }

                ((ManagerActivity) context).markNotificationsSeen(false);
            } else {
                addNotification(updatedUserAlerts.get(i));
            }

        }
    }

    public int getItemCount() {
        if (adapterList != null) {
            return adapterList.getItemCount();
        }
        return 0;
    }
}