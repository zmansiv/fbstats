package com.zmansiv.fbstats.chartcreator;

import com.zmansiv.fbstats.misc.ProgressUpdater;

public class ChartCreator {

    private final ChartCreator[] creators;
    private ProgressUpdater updater;

    public ChartCreator() {
        creators = new ChartCreator[0];
    }

    public ChartCreator(ProgressUpdater updater, ChartCreator... creators) {
        this.updater = updater;
        this.creators = creators;
    }

    public final void execute() {
        //Util.getDB().dropDatabase();
        downloadData(0, updater);
        calculateMetrics(0, updater);
        generateChart(0, updater);
        updater.finish();
    }

    public boolean skipDownload() {
        return false;
    }

    public void downloadData(int progressLevel, ProgressUpdater updater) {
        updater.update("Step 1/3 - Downloading data", progressLevel);
        for (int i = 0; i < creators.length; i++) {
            updater.update("Downloading set " + (i + 1) + "/" + creators.length, progressLevel + 1);
            ChartCreator creator = creators[i];
            if (!creator.skipDownload()) {
                creator.downloadData(progressLevel + 2, updater);
            }
        }
    }

    public boolean skipCalculation() {
        return false;
    }

    public void calculateMetrics(int progressLevel, ProgressUpdater updater) {
        updater.update("Step 2/3 - Calculating metrics", progressLevel);
        for (int i = 0; i < creators.length; i++) {
            updater.update("Calculating set " + (i + 1) + "/" + creators.length, progressLevel + 1);
            ChartCreator creator = creators[i];
            if (!creator.skipCalculation()) {
                creator.calculateMetrics(progressLevel + 2, updater);
            }
        }
    }

    public void generateChart(int progressLevel, ProgressUpdater updater) {
        updater.update("Step 3/3 - Generating charts", progressLevel);
        for (int i = 0; i < creators.length; i++) {
            updater.update("Generating chart " + (i + 1) + "/" + creators.length, progressLevel + 1);
            creators[i].generateChart(progressLevel + 2, updater);
        }
        updater.update("", progressLevel + 1);
        updater.update("Done", progressLevel);
    }

}