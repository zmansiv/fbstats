package com.zmansiv.fbstats.ui;

import com.zmansiv.fbstats.chartcreator.ChartCreator;
import com.zmansiv.fbstats.chartcreator.MessagesOverTimeCreator;
import com.zmansiv.fbstats.chartcreator.MessagesOverTimeForPersonCreator;
import com.zmansiv.fbstats.chartcreator.MostDenselyMessagedCreator;
import com.zmansiv.fbstats.chartcreator.MostMessagedCharactersCreator;
import com.zmansiv.fbstats.chartcreator.MostMessagedCreator;
import com.zmansiv.fbstats.misc.ProgressUpdater;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class MainFrame extends FSFrame {

    @Override
    protected void createContent(JPanel pane) {
        final JCheckBox mostMessagedBox = new JCheckBox("Most Messaged Friends", true), mostMessagedCharactersBox = new JCheckBox("Most Messaged " +
                "Friends by Character " +
                "Count", true),
                mostDenselyMessagedBox = new JCheckBox("Most Densely Messaged Friends", true), messagesOverTimeBox = new JCheckBox("Messages Over Time", true), messagesOverTimeForPersonBox = new JCheckBox("Messages Over Time " + "for", false);
        final JTextField messagesOverTimeForPersonField = new JTextField();
        final JButton startButton = new JButton("Start"), okButton = new JButton("Ok");
        final JLabel progressLabel1 = new JLabel(), progressLabel2 = new JLabel(), progressLabel3 = new JLabel(), progressLabel4 = new JLabel();
        try {
            BufferedReader br = new BufferedReader(new FileReader("./resources/mot_person.txt"));
            messagesOverTimeForPersonField.setText(br.readLine());
            br.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        mostMessagedBox.setSize(360, 20);
        mostMessagedBox.setLocation(10, 10);
        mostMessagedCharactersBox.setSize(360, 20);
        mostMessagedCharactersBox.setLocation(10, 35);
        mostDenselyMessagedBox.setSize(360, 20);
        mostDenselyMessagedBox.setLocation(10, 60);
        messagesOverTimeBox.setSize(360, 20);
        messagesOverTimeBox.setLocation(10, 85);
        messagesOverTimeForPersonBox.setSize(180, 20);
        messagesOverTimeForPersonBox.setLocation(10, 110);
        messagesOverTimeForPersonField.setSize(180, 30);
        messagesOverTimeForPersonField.setLocation(190, 105);
        startButton.setSize(360, 25);
        startButton.setLocation(10, 140);
        progressLabel1.setSize(350, 20);
        progressLabel1.setLocation(15, 175);
        progressLabel2.setSize(350, 20);
        progressLabel2.setLocation(15, 205);
        progressLabel3.setSize(350, 20);
        progressLabel3.setLocation(15, 235);
        progressLabel4.setSize(350, 20);
        progressLabel4.setLocation(15, 265);
        okButton.setSize(360, 25);
        okButton.setLocation(10, 295);
        messagesOverTimeForPersonField.setEnabled(false);
        okButton.setEnabled(false);
        pane.add(mostMessagedBox);
        pane.add(mostMessagedCharactersBox);
        pane.add(mostDenselyMessagedBox);
        pane.add(messagesOverTimeBox);
        pane.add(messagesOverTimeForPersonBox);
        pane.add(messagesOverTimeForPersonField);
        pane.add(startButton);
        pane.add(progressLabel1);
        pane.add(progressLabel2);
        pane.add(progressLabel3);
        pane.add(progressLabel4);
        pane.add(okButton);
        messagesOverTimeForPersonBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                messagesOverTimeForPersonField.setEnabled(messagesOverTimeForPersonBox.isSelected());
            }

        });
        startButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                mostMessagedBox.setEnabled(false);
                mostMessagedCharactersBox.setEnabled(false);
                mostDenselyMessagedBox.setEnabled(false);
                messagesOverTimeBox.setEnabled(false);
                messagesOverTimeForPersonBox.setEnabled(false);
                messagesOverTimeForPersonField.setEnabled(false);
                startButton.setEnabled(false);
                if (!messagesOverTimeForPersonField.getText().trim().isEmpty()) {
                    try {
                        BufferedWriter bw = new BufferedWriter(new FileWriter("./resources/mot_person.txt"));
                        bw.write(messagesOverTimeForPersonField.getText());
                        bw.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        List<ChartCreator> creators = new LinkedList<ChartCreator>();
                        if (mostMessagedBox.isSelected()) {
                            creators.add(new MostMessagedCreator());
                        }
                        if (mostMessagedCharactersBox.isSelected()) {
                            creators.add(new MostMessagedCharactersCreator());
                        }
                        if (mostDenselyMessagedBox.isSelected()) {
                            creators.add(new MostDenselyMessagedCreator());
                        }
                        if (messagesOverTimeBox.isSelected()) {
                            creators.add(new MessagesOverTimeCreator());
                        }
                        if (messagesOverTimeForPersonBox.isSelected()) {
                            for (String person : messagesOverTimeForPersonField.getText().split(",")) {
                                person = person.replace(".", "").trim();
                                creators.add(new MessagesOverTimeForPersonCreator(person));
                            }
                        }
                        new ChartCreator(new ProgressUpdater(new Runnable() {

                            @Override
                            public void run() {
                                okButton.setEnabled(true);
                            }

                        }, progressLabel1, progressLabel2, progressLabel3, progressLabel4), creators.toArray(new ChartCreator[creators.size()])).execute();
                    }

                }).start();
            }

        });
        okButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }

        });
        pane.setSize(380, 355);
    }

}