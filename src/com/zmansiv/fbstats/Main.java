package com.zmansiv.fbstats;

import com.zmansiv.fbstats.ui.MainFrame;

public class Main {

    public static void main(String[] args) throws Exception {
        //Process pr = Runtime.getRuntime().exec(new String[]{"cmd.exe", "/k", "start", "mongod", "--rest", "-dbpath",
        //      File.listRoots()[0].getAbsolutePath()});
        new MainFrame();
        /*FacebookClient fbc = new DefaultFacebookClient(Util.getAccessToken());
        Connection<Group> groupConnection = fbc.fetchConnection("me/groups", Group.class);
        for (Group group : groupConnection.getData()) {
            String name = group.getName();
            if (name.equals("Beach Week '13") || name.equals("WL Class of 2013") || name.equals("RSCA FB Group!")) {
                continue;
            }
            System.out.println(name + ": " + fbc.deleteObject(group.getId() + "/members/1176196085"));
        }*/
    }

}