package com.zmansiv.fbstats.chartcreator;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.DefaultWebRequestor;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.zmansiv.fbstats.misc.JsonToMongoMapper;
import com.zmansiv.fbstats.misc.ProgressUpdater;
import com.zmansiv.fbstats.misc.Util;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.Week;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MessagesOverTimeCreator extends ChartCreator {

    protected static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public boolean skipDownload() {
        DB db = Util.getDB();
        DBCollection meColl = db.getCollection("me"), messageColl = db.getCollection("messages");
        return meColl.getCount() > 0 && messageColl.getCount(new BasicDBObject("created_time",
                new BasicDBObject("$exists", 1))) > 0;
    }

    @Override
    public void downloadData(int progressLevel, ProgressUpdater updater) {
        FacebookClient fbc = new DefaultFacebookClient(Util.getAccessToken(), new DefaultWebRequestor(), new JsonToMongoMapper());
        DB db = Util.getDB();
        DBCollection messageColl = db.getCollection("messages");
        int threadCount = 0;
        if (messageColl.getCount() > 0) {
            java.util.List<DBObject> messages = messageColl.find().toArray();
            for (int messageCount = 0; messageCount < messages.size(); messageCount++) {
                updater.update("Downloading message " + (messageCount + 1) + "/" + messages.size() + " timestamp", progressLevel);
                DBObject oldMessage = messages.get(messageCount);
                try { //TODO illegal character in message id breaks Facebook Graph so we skip it
                    DBObject message = fbc.fetchObject(oldMessage.get("id").toString(), DBObject.class, Parameter.with("fields", "created_time"));
                    messageColl.update(new BasicDBObject("id", message.get("id")), new BasicDBObject("$set", new BasicDBObject("created_time", message.get("created_time"))), true, false);
                } catch (Exception e) {
                    System.out.println("Exception retrieving timestamp for message:");
                    System.out.println(oldMessage);
                    System.out.println("Current message count: " + messageCount);
                    e.printStackTrace();
                    if (e.getMessage().contains("User request limit reached")) {
                        System.out.println("Request limit reached, terminating loop.");
                        break;
                    }
                }
            }
        } else {
            Connection<DBObject> threadsConnection = fbc.fetchConnection("me/threads", DBObject.class, Parameter.with("fields", "message_count"));
            do {
                for (DBObject thread : threadsConnection.getData()) {
                    updater.update("Downloading thread " + (threadCount++ + 1) + " messages", progressLevel);
                    try { //TODO illegal character in thread id breaks Facebook Graph so we skip it
                        Connection<DBObject> messagesConnection = fbc.fetchConnection(thread.get("id") + "/messages", DBObject.class, Parameter.with("fields", "created_time"));
                        int messageCount = 0;
                        try {
                            do {
                                for (DBObject message : messagesConnection.getData()) {
                                    updater.update("Downloading message " + (messageCount++ + 1) + "/" + thread.get("message_count"), progressLevel + 1);
                                    String messageText = (String) message.get("message");
                                    if (messageText != null) {
                                        BasicDBObject setMod = new BasicDBObject();
                                        setMod.put("id", message.get("id"));
                                        setMod.put("created_time", message.get("created_time"));
                                        messageColl.update(new BasicDBObject("id", message.get("id")), new BasicDBObject("$set", setMod), true, false);
                                    }
                                }
                            }
                            while (messagesConnection.getNextPageUrl() != null && (messagesConnection = fbc.fetchConnectionPage(messagesConnection.getNextPageUrl(), DBObject.class)) != null);
                        } catch (Exception e) {
                            System.out.println("Exception retrieving messages for thread:");
                            System.out.println(thread);
                            System.out.println("Current message count: " + messageCount);
                            e.printStackTrace();
                        }
                    } catch (Exception e) {
                        System.out.println("Exception retrieving thread:");
                        System.out.println(thread);
                        e.printStackTrace();
                    }
                }
            }
            while (threadsConnection.getNextPageUrl() != null && (threadsConnection = fbc.fetchConnectionPage(threadsConnection.getNextPageUrl(), DBObject.class)) != null);
        }
        updater.update("", progressLevel + 1);
    }

    @Override
    public boolean skipCalculation() {
        DBCollection metricColl = Util.getDB().getCollection("calculated_metrics");
        BasicDBObject query = new BasicDBObject();
        query.put("day", new BasicDBObject("$exists", 1));
        query.put("message_count", new BasicDBObject("$exists", 1));
        return metricColl.getCount(query) > 0;
    }

    @Override
    public void calculateMetrics(int progressLevel, ProgressUpdater updater) {
        DB db = Util.getDB();
        DBCollection messageColl = db.getCollection("messages"), metricColl = db.getCollection("calculated_metrics");
        java.util.List<DBObject> messages = messageColl.find(new BasicDBObject("created_time", new BasicDBObject("$exists", "true")), new BasicDBObject("created_time", 1)).toArray();
        for (int i = 0; i < messages.size(); i++) {
            updater.update("Parsing message " + (i + 1) + "/" + messages.size() + " date", progressLevel);
            String date = messages.get(i).get("created_time").toString();
            date = date.substring(0, date.indexOf('T'));
            BasicDBObject update = new BasicDBObject();
            update.put("$set", new BasicDBObject("day", date));
            update.put("$inc", new BasicDBObject("message_count", 1));
            metricColl.update(new BasicDBObject("day", date), update, true, false);
        }
        updater.update("", progressLevel);
    }

    @Override
    public void generateChart(int progressLevel, ProgressUpdater updater) {
        DB db = Util.getDB();
        DBCollection metricColl = db.getCollection("calculated_metrics");
        BasicDBObject query = new BasicDBObject();
        query.put("day", new BasicDBObject("$exists", 1));
        query.put("message_count", new BasicDBObject("$exists", 1));
        BasicDBObject fieldSelection = new BasicDBObject();
        fieldSelection.put("day", 1);
        fieldSelection.put("message_count", 1);
        DBCursor messages = metricColl.find(query, fieldSelection);
        TimeSeries timeSeries = new TimeSeries("Message Count");
        Map<Week, Integer> counts = new HashMap<Week, Integer>();
        for (DBObject message : messages) {
            try {
                Date date = DATE_FORMAT.parse(message.get("day").toString());
                int message_count = Integer.parseInt(message.get("message_count").toString());
                Week week = new Week(date);
                counts.put(week, message_count + (counts.containsKey(week) ? counts.get(week) : 0));
            } catch (ParseException pe) {
                pe.printStackTrace();
            }
        }
        for (Week week : counts.keySet()) {
            timeSeries.add(week, counts.get(week));
        }
        Util.saveChart(Util.createLineChart("Messages Over Time", "Time", "Message Count", new TimeSeriesCollection(timeSeries)));
        updater.update("", progressLevel);
    }

}