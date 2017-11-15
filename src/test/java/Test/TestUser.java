package Test;

import httpServer.booter;
import nlogger.nlogger;

public class TestUser {
	public static void main(String[] args) {
	    booter booter = new booter();
        try {
            System.out.println("GrapeUser");
            System.setProperty("AppName", "GrapeUser");
            booter.start(1008);
        } catch (Exception e) {
            nlogger.logout(e);
        } 
	}
}
