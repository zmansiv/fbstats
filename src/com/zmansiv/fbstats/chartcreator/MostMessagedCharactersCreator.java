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

public class MostMessagedCharactersCreator extends ChartCreator {

    @Override
    public boolean skipDownload() {
        DB db = Util.getDB();
        DBCollection meColl = db.getCollection("me"), messageColl = db.getCollection("messages");
        return meColl.getCount() > 0 && messageColl.getCount(new BasicDBObject("message", new BasicDBObject("$exists",
                1))) > 0;
    }

    @Override
    public void downloadData(int progressLevel, ProgressUpdater updater) {
        FacebookClient fbc = new DefaultFacebookClient(Util.getAccessToken(), new DefaultWebRequestor(), new JsonToMongoMapper());
        DB db = Util.getDB();
        DBCollection meColl = db.getCollection("me"), messageColl = db.getCollection("messages");
        DBObject me = fbc.fetchObject("me", DBObject.class, Parameter.with("fields", "name"));
        meColl.update(new BasicDBObject("name", me.get("name")), new BasicDBObject("$set", new BasicDBObject("name", me.get("name"))), true, false);
        Connection<DBObject> threadsConnection = fbc.fetchConnection("me/threads", DBObject.class, Parameter.with("fields", "participants, message_count"));
        int threadCount = 0;
        do {
            for (DBObject thread : threadsConnection.getData()) {
                updater.update("Downloading thread " + (threadCount++ + 1) + " messages", progressLevel);
                try { //TODO illegal character in thread id breaks Facebook Graph so we skip it
                    Connection<DBObject> messagesConnection = fbc.fetchConnection(thread.get("id") + "/messages", DBObject.class, Parameter.with("fields", "to, message"));
                    int messageCount = 0;
                    try {
                        do {
                            for (DBObject message : messagesConnection.getData()) {
                                updater.update("Downloading message " + (messageCount++ + 1) + "/" + thread.get("message_count"), progressLevel + 1);
                                String messageText = (String) message.get("message");
                                if (messageText != null) {
                                    BasicDBObject setMod = new BasicDBObject();
                                    setMod.put("id", message.get("id"));
                                    setMod.put("to", message.get("to"));
                                    setMod.put("message", message.get("message"));
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
        return metricColl.getCount(new BasicDBObject("message_character_count", new BasicDBObject("$exists",
                1))) > 0;
    }

    @Override
    public void calculateMetrics(int progressLevel, ProgressUpdater updater) {
        DB db = Util.getDB();
        DBCollection meColl = db.getCollection("me"), messageColl = db.getCollection("messages"),
                metricColl = db.getCollection("calculated_metrics");
        String myName = meColl.findOne().get("name").toString();
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
        updater.update("", progressLevel);
    }

    @Override
    public void generateChart(int progressLevel, ProgressUpdater updater) {
        DB db = Util.getDB();
        DBCollection metricColl = db.getCollection("calculated_metrics");
        BasicDBObject fieldSelection = new BasicDBObject();
        fieldSelection.put("name", 1);
        fieldSelection.put("message_character_count", 1);
        DBCursor results = metricColl.find(new BasicDBObject("message_character_count", new BasicDBObject("$exists", "true")), fieldSelection).sort(new BasicDBObject("message_character_count", -1)).limit(20);
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (DBObject result : results) {
            dataset.setValue(Integer.parseInt(result.get("message_character_count").toString()), "Character Count", (String) result.get("name"));
        }
        Util.saveChart(Util.createBarChart("20 Most Messaged Friends by Character Count", "Friend", "Character Count", dataset));
        updater.update("", progressLevel);
    }

}