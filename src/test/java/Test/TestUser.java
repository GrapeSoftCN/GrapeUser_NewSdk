package Test;

import httpServer.booter;
import nlogger.nlogger;

public class TestUser {
	public static void main(String[] args) {
	    booter booter = new booter();
        try {
            System.out.println("User");
            System.setProperty("AppName", "User");
            booter.start(1008);
        } catch (Exception e) {
            nlogger.logout(e);
        } 
	}
}
