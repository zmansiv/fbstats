package com.zmansiv.fbstats.chartcreator;

import com.mongodb.BasicDBList;
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
import org.jfree.data.category.DefaultCategoryDataset;

public class MostDenselyMessagedCreator extends ChartCreator {

    @Override
    public boolean skipDownload() {
        DB db = Util.getDB();
        DBCollection meColl = db.getCollection("me"), threadColl = db.getCollection("threads"),
                messageColl = db.getCollection("messages");
        return meColl.getCount() > 0 && threadColl.getCount(new BasicDBObject("message_count",
                new BasicDBObject("$exists", 1))) > 0 && messageColl.getCount(new BasicDBObject("message",
                new BasicDBObject("$exists", 1))) > 0;
    }

    @Override
    public void downloadData(int progressLevel, ProgressUpdater updater) {
        FacebookClient fbc = new DefaultFacebookClient(Util.getAccessToken(), new DefaultWebRequestor(), new JsonToMongoMapper());
        DB db = Util.getDB();
        DBCollection meColl = db.getCollection("me"), threadColl = db.getCollection("threads"),
                messageColl = db.getCollection("messages");
        DBObject me = fbc.fetchObject("me", DBObject.class, Parameter.with("fields", "name"));
        meColl.update(new BasicDBObject("name", me.get("name")), new BasicDBObject("$set", new BasicDBObject("name", me.get("name"))), true, false);
        Connection<DBObject> threadsConnection = fbc.fetchConnection("me/threads", DBObject.class, Parameter.with("fields", "participants, message_count"));
        int threadCount = 0;
        do {
            for (DBObject thread : threadsConnection.getData()) {
                updater.update("Downloading thread " + (threadCount++ + 1) + " data", progressLevel);
                BasicDBObject setMod = new BasicDBObject();
                setMod.put("id", thread.get("id"));
                setMod.put("participants", thread.get("participants"));
                setMod.put("message_count", thread.get("message_count"));
                threadColl.update(new BasicDBObject("id", thread.get("id")), new BasicDBObject("$set", setMod), true, false);
                try { //TODO illegal character in thread id breaks Facebook Graph so we skip it
                    Connection<DBObject> messagesConnection = fbc.fetchConnection(thread.get("id") + "/messages", DBObject.class, Parameter.with("fields", "to, message"));
                    int messageCount = 0;
                    try {
                        do {
                            for (DBObject message : messagesConnection.getData()) {
                                updater.update("Downloading message " + (messageCount++ + 1) + "/" + thread.get("message_count"), progressLevel + 1);
                                String messageText = (String) message.get("message");
                                if (messageText != null) {
                                    BasicDBObject setMod2 = new BasicDBObject();
                                    setMod.put("id", message.get("id"));
                                    setMod.put("to", message.get("to"));
                                    setMod.put("message", message.get("message"));
                                    messageColl.update(new BasicDBObject("id", message.get("id")), new BasicDBObject("$set", setMod2), true, false);
                                }
                            }
                        }
                        while (messagesConnection.getNextPageUrl() != null && (messagesConnection = fbc.fetchConnectionPage(messagesConnection.getNextPageUrl(), DBObject.class)) != null);
                    } catch (Exception e) {
                        System.out.println("Exception retrieving messages for thread:");
                        System.out.println(thread);
                        System.out.println("Current message count: " + messageCount);
                        e.printStackTrace();
                        if (e.getMessage().contains("User request limit reached")) {
                            System.out.println("Request limit reached, terminating loop.");
                            break;
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Exception retrieving thread:");
                    System.out.println(thread);
                    e.printStackTrace();
                }
            }
        }
        while (threadsConnection.getNextPageUrl() != null && (threadsConnection = fbc.fetchConnectionPage(threadsConnection.getNextPageUrl(), DBObject.class)) != null);
        updater.update("", progressLevel + 1);
    }

    @Override
    public boolean skipCalculation() {
        DBCollection metricColl = Util.getDB().getCollection("calculated_metrics");
        return metricColl.getCount(new BasicDBObject("average_message_density", new BasicDBObject("$exists",
                1))) > 0;
    }

    @Override
    public void calculateMetrics(int progressLevel, ProgressUpdater updater) {
        DB db = Util.getDB();
        DBCollection meColl = db.getCollection("me"), threadColl = db.getCollection("threads"),
                messageColl = db.getCollection("messages"), metricColl = db.getCollection("calculated_metrics");
        String myName = meColl.findOne().get("name").toString();
        BasicDBObject query1 = new BasicDBObject();
        query1.put("name", new BasicDBObject("$exists", 1));
        query1.put("message_count", new BasicDBObject("$exists", 1));
        if (metricColl.find(query1).size() < 1) {
            java.util.List<DBObject> threads = threadColl.find().toArray();
            for (int i = 0; i < threads.size(); i++) {
                updater.update("Calculating thread " + (i + 1) + "/" + threads.size() + " size", progressLevel);
                DBObject thread = threads.get(i);
                BasicDBList correspondents = (BasicDBList) ((BasicDBObject) thread.get("participants")).get("data");
                if (correspondents.size() != 2) {
                    continue;
                }
                for (Object correspondent : correspondents) {
                    String name = (String) ((BasicDBObject) correspondent).get("name");
                    if (name == null) {
                        name = "null";
                    }
                    name = name.replace(".", "");
                    if (!myName.equals(name)) {
                        int count = Integer.parseInt(thread.get("message_count").toString());
                        BasicDBObject setMod = new BasicDBObject();
                        setMod.put("name", name);
                        setMod.put("message_count", count);
                        metricColl.update(new BasicDBObject("name", name), new BasicDBObject("$set", setMod), true, false);
                    }
                }
            }
        }
        if (metricColl.find(query1).size() < 1) {
            java.util.List<DBObject> messages = messageColl.find().toArray();
            for (int i = 0; i < messages.size(); i++) {
                updater.update("Counting message " + (i + 1) + "/" + messages.size() + " length", progressLevel);
                DBObject message = messages.get(i);
                BasicDBList correspondents = (BasicDBList) ((BasicDBObject) message.get("to")).get("data");
                if (correspondents.size() != 2) {
                    continue;
                }
                for (Object correspondent : correspondents) {
                    String name = (String) ((BasicDBObject) correspondent).get("name");
                    if (name == null) {
                        name = "null";
                    }
                    name = name.replace(".", "");
                    if (!myName.equals(name)) {
                        int length = message.get("message").toString().length();
                        BasicDBObject update = new BasicDBObject();
                        update.put("$set", new BasicDBObject("name", name));
                        update.put("$inc", new BasicDBObject("message_character_count", length));
                        metricColl.update(new BasicDBObject("name", name), update, true, false);
                    }
                }
            }
        }
        BasicDBObject query2 = new BasicDBObject();
        query2.put("name", new BasicDBObject("$exists", 1));
        query2.put("message_count", new BasicDBObject("$exists", 1));
        query2.put("message_character_count", new BasicDBObject("$exists", 1));
        BasicDBObject fieldSelection = new BasicDBObject();
        fieldSelection.put("name", 1);
        fieldSelection.put("message_count", 1);
        fieldSelection.put("message_character_count", 1);
        for (DBObject result : metricColl.find(query2, fieldSelection)) {
            int message_count = Integer.parseInt(result.get("message_count").toString());
            int character_count = Integer.parseInt(result.get("message_character_count").toString());
            double density = ((double) character_count) / ((double) message_count);
            BasicDBObject setMod = new BasicDBObject();
            setMod.put("name", result.get("name"));
            setMod.put("average_message_density", density);
            metricColl.update(new BasicDBObject("name", result.get("name")), new BasicDBObject("$set", setMod), true, false);
        }
        updater.update("", progressLevel);
    }

    @Override
    public void generateChart(int progressLevel, ProgressUpdater updater) {
        DB db = Util.getDB();
        DBCollection metricColl = db.getCollection("calculated_metrics");
        BasicDBObject query = new BasicDBObject();
        query.put("average_message_density", new BasicDBObject("$exists", "true"));
        query.put("message_count", new BasicDBObject("$gt", 300));
        BasicDBObject fieldSelection = new BasicDBObject();
        fieldSelection.put("name", 1);
        fieldSelection.put("average_message_density", 1);
        DBCursor results = metricColl.find(query, fieldSelection).sort(new BasicDBObject("average_message_density", -1)).limit(20);
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (DBObject result : results) {
            dataset.setValue(Double.parseDouble(result.get("average_message_density").toString()), "Avg. Message Density", (String) result.get("name"));
        }
        Util.saveChart(Util.createBarChart("20 Most Densely Messaged Friends", "Friend", "Avg. Message Density", dataset));
        updater.update("", progressLevel);
    }

}