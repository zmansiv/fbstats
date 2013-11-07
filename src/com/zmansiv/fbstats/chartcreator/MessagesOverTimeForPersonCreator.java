package com.zmansiv.fbstats.chartcreator;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.zmansiv.fbstats.misc.ProgressUpdater;
import com.zmansiv.fbstats.misc.Util;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.Week;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MessagesOverTimeForPersonCreator extends MessagesOverTimeCreator {

    private final String person;
    private final String person_count_field;

    public MessagesOverTimeForPersonCreator(String person) {
        super();
        this.person = person;
        this.person_count_field = person.toLowerCase().replace(" ", "_") + "_message_count";
    }

    @Override
    public boolean skipCalculation() {
        DBCollection metricColl = Util.getDB().getCollection("calculated_metrics");
        BasicDBObject query = new BasicDBObject();
        query.put("day", new BasicDBObject("$exists", 1));
        query.put(person_count_field, new BasicDBObject("$exists", 1));
        return metricColl.getCount(query) > 0;
    }

    @Override
    public void calculateMetrics(int progressLevel, ProgressUpdater updater) {
        DB db = Util.getDB();
        DBCollection messageColl = db.getCollection("messages"), metricColl = db.getCollection("calculated_metrics");
        BasicDBObject query = new BasicDBObject();
        query.put("created_time", new BasicDBObject("$exists", 1));
        query.put("to", new BasicDBObject("$exists", 1));
        BasicDBObject fieldSelection = new BasicDBObject();
        fieldSelection.put("created_time", 1);
        fieldSelection.put("to", 1);
        java.util.List<DBObject> messages = messageColl.find(query, fieldSelection).toArray();
        for (int i = 0; i < messages.size(); i++) {
            updater.update("Parsing message " + (i + 1) + "/" + messages.size() + " correspondent and date", progressLevel);
            DBObject message = messages.get(i);
            BasicDBList correspondents = (BasicDBList) ((BasicDBObject) message.get("to")).get("data");
            if (correspondents.size() != 2) {
                continue;
            }
            for (Object correspondent : correspondents) {
                String name = (String) ((BasicDBObject) correspondent).get("name");
                if (person.equals(name)) {
                    String date = message.get("created_time").toString();
                    date = date.substring(0, date.indexOf('T'));
                    BasicDBObject update = new BasicDBObject();
                    update.put("$set", new BasicDBObject("day", date));
                    update.put("$inc", new BasicDBObject(person_count_field, 1));
                    metricColl.update(new BasicDBObject("day", date), update, true, false);
                } else {
                    String date = message.get("created_time").toString();
                    date = date.substring(0, date.indexOf('T'));
                    BasicDBObject update = new BasicDBObject();
                    update.put("$set", new BasicDBObject("day", date));
                    update.put("$inc", new BasicDBObject(person_count_field, 0));
                    metricColl.update(new BasicDBObject("day", date), update, true, false);
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
        query.put("day", new BasicDBObject("$exists", 1));
        query.put(person_count_field, new BasicDBObject("$exists", 1));
        BasicDBObject fieldSelection = new BasicDBObject();
        fieldSelection.put("day", 1);
        fieldSelection.put(person_count_field, 1);
        DBCursor messages = metricColl.find(query, fieldSelection);
        TimeSeries timeSeries = new TimeSeries("Message Count");
        Map<Week, Integer> counts = new HashMap<Week, Integer>();
        for (DBObject message : messages) {
            try {
                Date date = DATE_FORMAT.parse(message.get("day").toString());
                int message_count = Integer.parseInt(message.get(person_count_field).toString());
                Week week = new Week(date);
                counts.put(week, message_count + (counts.containsKey(week) ? counts.get(week) : 0));
            } catch (ParseException pe) {
                pe.printStackTrace();
            }
        }
        for (Week week : counts.keySet()) {
            timeSeries.add(week, counts.get(week));
        }
        Util.saveChart(Util.createLineChart("Messages Over Time for " + person, "Time", "Message Count", new TimeSeriesCollection(timeSeries)));
        updater.update("", progressLevel);
    }

}