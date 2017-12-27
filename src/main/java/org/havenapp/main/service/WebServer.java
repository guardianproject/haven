package org.havenapp.main.service;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import fi.iki.elonen.NanoHTTPD;
import org.havenapp.main.model.Event;
import org.havenapp.main.model.EventTrigger;

/**
 * Created by n8fr8 on 6/25/17.
 */

public class WebServer extends NanoHTTPD {

    public final static int LOCAL_PORT = 8888;

    private final static String TAG = "WebServer";
    private String appTitle = "Haven";

    private String mPassword = null;
    private String mSession = null;

    private Context mContext;

    public WebServer(Context context) throws IOException {
        super(LOCAL_PORT);
        mContext = context;
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }

    public void setPassword (String password)
    {
        mPassword = password;
    }

    @Override
    public Response serve(IHTTPSession session) {

        StringBuffer page = new StringBuffer();
        Cookie cookie = null;

        if (mPassword != null)
        {
            String inPassword = session.getParms().get("p");
            String inSid = session.getCookies().read("sid");

            if (inPassword != null && mPassword.equals(inPassword)) {
                mSession = UUID.randomUUID().toString();
                cookie = new OnionCookie ("sid",mSession,100000);
                session.getCookies().set(cookie);
            }
            else if (inSid == null || (inSid != null && (!mSession.equals(inSid)))) {
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
            EventTrigger eventTrigger = EventTrigger.findById(EventTrigger.class, eventTriggerId);

            try {
                File fileMedia = new File(eventTrigger.getPath());
                FileInputStream fis = new FileInputStream(fileMedia);
                Response res = newChunkedResponse(Response.Status.OK, getMimeType(eventTrigger), fis);
                fis.close();
                return res;

            }
            catch (IOException ioe)
            {
                Log.e(TAG,"unable to return media file",ioe);
            }
        }
        else if (uri.getPath().startsWith("/feed"))
        {
            //do RSS feed

        }
        else {
            page.append("<html><head><title>" + appTitle + "</title>");
            page.append("<meta http-equiv=\"Content-Type\" content=\"application/xhtml+xml; charset=utf-8\" />");
            page.append("<meta name = \"viewport\" content = \"user-scalable=no, initial-scale=1.0, maximum-scale=1.0, width=device-width\">");
            page.append("</head><body>");

            if (TextUtils.isEmpty(uri.getPath()) || uri.getPath().equals("/"))
                showEvents(page);
            else {
                try {
                    if (pathSegs.size() == 2 && pathSegs.get(0).equals("event")) {
                        long eventId = Long.parseLong(pathSegs.get(1));
                        Event event = Event.findById(Event.class, eventId);
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

        page.append("<html><head><title>PhoneyPot</title>");
        page.append("<meta http-equiv=\"Content-Type\" content=\"application/xhtml+xml; charset=utf-8\" />");
        page.append("<meta name = \"viewport\" content = \"user-scalable=no, initial-scale=1.0, maximum-scale=1.0, width=device-width\">");
        page.append("</head><body>");

        page.append("<form action=\"/\">" +
                "  <div class=\"container\">\n" +
                "    <label><b>Password</b></label>\n" +
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
            String title = eventTrigger.getStringType(mContext);
            String desc = eventTrigger.getTriggerTime().toString();

            page.append("<b>");
            page.append(title).append("</b><br/>");
            page.append(desc).append("<br/>");

            String mediaPath = "/event/" + event.getId() + "/trigger/" + eventTrigger.getId();

            if (eventTrigger.getType() == EventTrigger.CAMERA)
            {
                page.append("<img src=\"").append(mediaPath).append("\" width=\"100%\"/>");
                page.append("<a href=\"" + mediaPath + "\">Download Media").append("</a>");

            }
            else if (eventTrigger.getType() == EventTrigger.MICROPHONE)
            {
                page.append("<audio src=\"").append(mediaPath).append("\"></audio>");
                page.append("<a href=\"" + mediaPath + "\">Download Media").append("</a>");

            }


            page.append("<hr/>");
        }



    }

    private void showEvents(StringBuffer page)
    {
        page.append("<h1>Events</h1><hr/>\n");

        List<Event> events = Event.listAll(Event.class);

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

    class OnionCookie extends Cookie
    {

        public OnionCookie(String name, String value, int numDays) {
            super(name, value, numDays);
        }

        public String getHTTPHeader() {
           return super.getHTTPHeader() + "; path=/";
        }
    }

}
