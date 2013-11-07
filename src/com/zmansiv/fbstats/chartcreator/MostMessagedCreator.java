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

public class MostMessagedCreator extends ChartCreator {

    @Override
    public boolean skipDownload() {
        DB db = Util.getDB();
        DBCollection meColl = db.getCollection("me"), threadColl = db.getCollection("threads");
        return meColl.getCount() > 0 && threadColl.getCount(new BasicDBObject("message_count",
                new BasicDBObject("$exists", 1))) > 0;
    }

    @Override
    public void downloadData(int progressLevel, ProgressUpdater updater) {
        FacebookClient fbc = new DefaultFacebookClient(Util.getAccessToken(), new DefaultWebRequestor(), new JsonToMongoMapper());
        DB db = Util.getDB();
        DBCollection meColl = db.getCollection("me"), threadColl = db.getCollection("threads");
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
            }
        }
        while (threadsConnection.getNextPageUrl() != null && (threadsConnection = fbc.fetchConnectionPage(threadsConnection.getNextPageUrl(), DBObject.class)) != null);
    }

    @Override
    public boolean skipCalculation() {
        DBCollection metricColl = Util.getDB().getCollection("calculated_metrics");
        BasicDBObject query = new BasicDBObject();
        query.put("name", new BasicDBObject("$exists", 1));
        query.put("message_count", new BasicDBObject("$exists", 1));
        return metricColl.getCount(query) > 0;
    }

    @Override
    public void calculateMetrics(int progressLevel, ProgressUpdater updater) {
        DB db = Util.getDB();
        DBCollection meColl = db.getCollection("me"), threadColl = db.getCollection("threads"),
                metricColl = db.getCollection("calculated_metrics");
        String myName = meColl.findOne().get("name").toString();
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
        updater.update("", progressLevel);
    }

    @Override
    public void generateChart(int progressLevel, ProgressUpdater updater) {
        DB db = Util.getDB();
        DBCollection metricColl = db.getCollection("calculated_metrics");
        BasicDBObject query = new BasicDBObject();
        query.put("name", new BasicDBObject("$exists", 1));
        query.put("message_count", new BasicDBObject("$exists", 1));
        BasicDBObject fieldSelection = new BasicDBObject();
        fieldSelection.put("name", 1);
        fieldSelection.put("message_count", 1);
        DBCursor results = metricColl.find(query, fieldSelection).sort(new BasicDBObject("message_count",
                -1)).limit(20);
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (DBObject result : results) {
            dataset.setValue(Integer.parseInt(result.get("message_count").toString()), "Message Count", (String) result.get("name"));
        }
        Util.saveChart(Util.createBarChart("20 Most Messaged Friends", "Friend", "Message Count", dataset));
        updater.update("", progressLevel);
    }

}