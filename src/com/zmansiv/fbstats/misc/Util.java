package com.zmansiv.fbstats.misc;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.restfb.exception.FacebookOAuthException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.TextAnchor;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;

public class Util {

    static {
        getAccessToken();
    }

    public static final String HOST = null;
    //public static final String HOST = "localhost";

    public static final String AUTH_PAGE = null;

    public static final String APP_ID = null;

    public static final String APP_SECRET = null;

    public static final Color ROYAL_BLUE = new Color(65, 105, 225);

    private static String ACCESS_TOKEN = null;

    public static String getAccessToken() {
        if (ACCESS_TOKEN == null) {
            try {
                try {
                    BufferedReader br = new BufferedReader(new FileReader("./resources/access_token.txt"));
                    ACCESS_TOKEN = br.readLine();
                    br.close();
                    /*DefaultFacebookClient fbc = new DefaultFacebookClient(ACCESS_TOKEN);
                    ACCESS_TOKEN = fbc.obtainExtendedAccessToken(APP_ID, APP_SECRET, ACCESS_TOKEN).getAccessToken();
                    BufferedWriter bw = new BufferedWriter(new FileWriter("./resources/access_token.txt"));
                    bw.write(ACCESS_TOKEN);
                    bw.close();*/
                } catch (FacebookOAuthException foae) {
                    foae.printStackTrace();
                    try {
                        Desktop.getDesktop().browse(new URI("http://" + HOST + "/" + AUTH_PAGE));
                        System.exit(0);
                    } catch (URISyntaxException use) {
                        use.printStackTrace();
                    }
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return ACCESS_TOKEN;
    }

    private static DB database = null;

    public static DB getDB() {
        if (database == null) {
            try {
                database = new Mongo(HOST).getDB("fbstats");
            } catch (UnknownHostException uhe) {
                uhe.printStackTrace();
            }
        }
        return database;
    }

    public static void saveChart(JFreeChart chart) {
        try {
            BufferedImage image = new BufferedImage(750, 500, BufferedImage.TYPE_INT_ARGB);
            Graphics g = image.createGraphics();
            chart.draw((Graphics2D) g, new Rectangle(750, 500));
            g.dispose();
            ImageIO.write(image, "png", new File("./generated/" + chart.getTitle().getText() + ".png"));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public static JFreeChart createBarChart(String title, String domain, String range, CategoryDataset dataset) {
        JFreeChart chart = ChartFactory.createBarChart(title, domain, range, dataset, PlotOrientation.HORIZONTAL, false, true, false);
        chart.setBackgroundPaint(new Color(0, 0, 0, 0));
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setRangeGridlinePaint(new Color(0, 0, 0, 100));
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        renderer.setBaseItemLabelsVisible(true);
        ItemLabelPosition lp = new ItemLabelPosition(ItemLabelAnchor.INSIDE3, TextAnchor.CENTER_RIGHT);
        renderer.setBasePositiveItemLabelPosition(lp);
        renderer.setSeriesPaint(0, Util.ROYAL_BLUE);
        return chart;
    }

    public static JFreeChart createLineChart(String title, String domain, String range, TimeSeriesCollection dataset) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(title, domain, range, dataset, false, true, false);
        chart.setBackgroundPaint(new Color(0, 0, 0, 0));
        XYPlot weekPlot = chart.getXYPlot();
        weekPlot.setRangeGridlinePaint(new Color(0, 0, 0, 100));
        weekPlot.setDomainGridlinePaint(new Color(0, 0, 0, 100));
        NumberAxis weekRangeAxis = (NumberAxis) weekPlot.getRangeAxis();
        weekRangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        DateAxis weekDomainAxis = (DateAxis) weekPlot.getDomainAxis();
        weekDomainAxis.setDateFormatOverride(new SimpleDateFormat("MMM ''yy"));
        weekDomainAxis.setVerticalTickLabels(true);
        XYItemRenderer weekRenderer = weekPlot.getRenderer();
        weekRenderer.setSeriesPaint(0, Util.ROYAL_BLUE);
        return chart;
    }

}