package org.havenapp.main.service;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import org.havenapp.main.R;
import org.havenapp.main.Utils;
import org.havenapp.main.database.HavenEventDB;
import org.havenapp.main.model.Event;
import org.havenapp.main.model.EventTrigger;
import org.havenapp.main.resources.ResourceManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by n8fr8 on 6/25/17.
 */

public class WebServer extends NanoHTTPD {

    public final static String LOCAL_HOST = "127.0.0.1";
    public final static int LOCAL_PORT = 8888;

    private final static String TAG = "WebServer";
    private String appTitle = "Haven";

    private String mPassword = null;
    private String mSession = null;

    private Context mContext;

    public WebServer(Context context, String password) throws IOException {
        super(LOCAL_HOST, LOCAL_PORT);
        mContext = context;
        mPassword = password;

        if (!TextUtils.isEmpty(mPassword)) //require a password to start the server
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        else
            throw new IOException ("Web password must not be null");
    }

    @Override
    public Response serve(IHTTPSession session) {

        StringBuffer page = new StringBuffer();
        Cookie cookie = null;

        if (mPassword != null)
        {
            // We have to use session.parseBody() to obtain POST data.
            // See https://github.com/NanoHttpd/nanohttpd/issues/427
            Map<String, String> content = new HashMap<>();
            Method method = session.getMethod();
            if (Method.PUT.equals(method) || Method.POST.equals(method)) try {
                session.parseBody(content);
            } catch (IOException ioe) {
                Log.e(TAG,"unable to parse body of request",ioe);
            } catch (ResponseException re) {
                Log.e(TAG,"unable to parse body of request",re);
            }
            String inPassword = session.getParms().get("p");
            String inSid = session.getCookies().read("sid");

            if (inPassword != null && safeEquals(inPassword, mPassword)) {
                mSession = UUID.randomUUID().toString();
                cookie = new OnionCookie ("sid",mSession,100000);
                session.getCookies().set(cookie);
            }
            else if (inSid == null || mSession == null || (inSid != null && (!safeEquals(inSid, mSession)))) {
                showLogin(page);
                return newFixedLengthResponse(page.toString());
            }
        }


        Uri uri = Uri.parse(session.getUri());
        List<String> pathSegs = uri.getPathSegments();

        if (pathSegs.size() == 4  && pathSegs.get(2).equals("trigger"))
        {
            //long eventId = Long.parseLong(pathSegs.get(1));

            long eventTriggerId = Long.parseLong(pathSegs.get(3));
            EventTrigger eventTrigger = HavenEventDB.getDatabase(mContext).getEventTriggerDAO()
                    .findById(eventTriggerId);

            try {
                File fileMedia = new File(Objects.requireNonNull(eventTrigger.getPath()));
                FileInputStream fis = new FileInputStream(fileMedia);
                return newChunkedResponse(Response.Status.OK, getMimeType(eventTrigger), fis);

            }
            catch (IOException ioe)
            {
                Log.e(TAG,"unable to return media file",ioe);
            } catch (NullPointerException npe) {
                Log.e(TAG,"unable to return media file", npe);
            }
        }
        else if (uri.getPath().startsWith("/feed"))
        {
            //do RSS feed

        }
        else {
            page.append("<html><head><title>").append(appTitle).append("</title>");
            page.append("<meta http-equiv=\"Content-Type\" content=\"application/xhtml+xml; charset=utf-8\" />");
            page.append("<meta name = \"viewport\" content = \"user-scalable=no, initial-scale=1.0, maximum-scale=1.0, width=device-width\">");
            page.append("</head><body>");

            if (TextUtils.isEmpty(uri.getPath()) || uri.getPath().equals("/"))
                showEvents(page);
            else {
                try {
                    if (pathSegs.size() == 2 && pathSegs.get(0).equals("event")) {
                        long eventId = Long.parseLong(pathSegs.get(1));
                        Event event = HavenEventDB.getDatabase(mContext)
                                .getEventDAO().findById(eventId);
                        showEvent(event, page);

                    }

                } catch (Exception e) {
                    Log.e(TAG, "Something went wrong with parsing the path", e);
                }
            }

            page.append("</body></html>\n");
            Response response = newFixedLengthResponse(page.toString());
            session.getCookies().unloadQueue(response);
            return response;
        }

        Response response = newFixedLengthResponse(Response.Status.INTERNAL_ERROR,"text/plain","Error");
        session.getCookies().unloadQueue(response);
        return response;
    }

    private void showLogin (StringBuffer page) {

        page.append("<html><head><title>").append(appTitle).append("</title>");
        page.append("<meta http-equiv=\"Content-Type\" content=\"application/xhtml+xml; charset=utf-8\" />");
        page.append("<meta name = \"viewport\" content = \"user-scalable=no, initial-scale=1.0, maximum-scale=1.0, width=device-width\">");
        page.append("</head><body>");

        page.append("<form action=\"/\" method=\"post\">" +
                "  <div class=\"container\">\n" +
                "    <label><b>" + mContext.getString(R.string.password) + "</b></label>\n" +
                "    <input type=\"password\" placeholder=\"Enter Password\" name=\"p\" required>\n" +
                "\n" +
                "    <button type=\"submit\">Login</button>\n" +
                "  </div></form>");

        page.append("</body></html>\n");
    }

    private void showEvent (Event event, StringBuffer page) {

        List<EventTrigger> triggers = event.getEventTriggers();

        page.append("<h1>Event: ").append(event.getStartTime().toLocaleString()).append("</h1><hr/>\n");

        for (EventTrigger eventTrigger: triggers)
        {
            String title = eventTrigger.getStringType(new ResourceManager(mContext));
            String desc = new SimpleDateFormat(Utils.DATE_TIME_PATTERN,
                    Locale.getDefault()).format(eventTrigger.getTime());

            page.append("<b>");
            page.append(title).append("</b><br/>");
            page.append(desc).append("<br/>");

            String mediaPath = "/event/" + event.getId() + "/trigger/" + eventTrigger.getId();

            if (eventTrigger.getType() == EventTrigger.CAMERA)
            {
                page.append("<img src=\"").append(mediaPath).append("\" width=\"100%\"/>");
                page.append("<a href=\"").append(mediaPath).append("\">Download Media").append("</a>");

            }
            else if (eventTrigger.getType() == EventTrigger.MICROPHONE)
            {
                page.append("<audio src=\"").append(mediaPath).append("\"></audio>");
                page.append("<a href=\"").append(mediaPath).append("\">Download Media").append("</a>");

            }


            page.append("<hr/>");
        }



    }

    private void showEvents(StringBuffer page)
    {
        page.append("<h1>Events</h1><hr/>\n");

        List<Event> events = HavenEventDB.getDatabase(mContext).getEventDAO().getAllEvent();

        for (Event event: events)
        {
            String title = event.getStartTime().toLocaleString();
            String desc = event.getEventTriggers().size() + " triggered events";

            page.append("<b>").append("<a href=\"/event/").append(event.getId()).append("\">");
            page.append(title).append("</a></b><br/>");
            page.append(desc);
            page.append("<hr/>");
        }

    }

    private String getMimeType (EventTrigger eventTrigger)
    {
        String sType = "";

        switch (eventTrigger.getType()) {
            case EventTrigger.CAMERA:
                sType = "image/jpeg";
                break;
            case EventTrigger.MICROPHONE:
                sType = "audio/mp4";
                break;
            default:
                sType = null;
        }

        return sType;

    }

    private boolean safeEquals (String a, String b) {
        byte[] aByteArray = a.getBytes(Charset.forName("UTF-8"));
        byte[] bByteArray = b.getBytes(Charset.forName("UTF-8"));
        return MessageDigest.isEqual(aByteArray, bByteArray);
    }

    static class OnionCookie extends Cookie
    {

        public OnionCookie(String name, String value, int numDays) {
            super(name, value, numDays);
        }

        public String getHTTPHeader() {
           return super.getHTTPHeader() + "; path=/";
        }
    }

}
