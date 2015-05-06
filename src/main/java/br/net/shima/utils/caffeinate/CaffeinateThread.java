package br.net.shima.utils.caffeinate;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.time.DateUtils;

public class CaffeinateThread {

	private static final int TINY_TIMEOUT = 2;
	private static final int SMALL_TIMEOUT = 5;
	private static final int DEFAULT_TIMEOUT = 10;
	private static final int MEDIUM_TIMEOUT = 30;

	private static Logger logger = Logger.getLogger(CaffeinateThread.class.getName());

	private Process runningProcess;
	private Date runningProcessUntil;

	private Map<String, Process> processes;

	//	Available options:
	//
	//	     -d      Create an assertion to prevent the display from sleeping.
	//
	//	     -i      Create an assertion to prevent the system from idle sleeping.
	//
	//	     -m      Create an assertion to prevent the disk from idle sleeping.
	//
	//	     -s      Create an assertion to prevent the system from sleeping. This assertion is valid only when sys-tem system
	//	             tem is running on AC power.
	//
	//	     -u      Create an assertion to declare that user is active. If the display is off, this option turns
	//	             the display on and prevents the display from going into idle sleep. If a timeout is not speci-fied specified
	//	             fied with '-t' option, then this assertion is taken with a default of 5 second timeout.
	//
	//	     -t      Specifies the timeout value in seconds for which this assertion has to be valid. The assertion
	//	             is dropped after the specified timeout. Timeout value is not used when an utility is invoked
	//	             with this command.

	public CaffeinateThread() {
	}

	public void caffeinate() {
		this.caffeinate(DEFAULT_TIMEOUT);
	}

	public void tinyCaffeinate() {
		this.caffeinate(TINY_TIMEOUT);
	}

	public void smallCaffeinate() {
		this.caffeinate(SMALL_TIMEOUT);
	}

	public void mediumCaffeinate() {
		this.caffeinate(MEDIUM_TIMEOUT);
	}

	public void caffeinate(int minutes) {
		Date newUntil = DateUtils.addMinutes(new Date(), minutes);
		if(runningProcessUntil != null && runningProcessUntil != null && runningProcessUntil.after(newUntil)){
			return;
		}

		try{
			int seconds = minutes * 60;
			logger.info("Iniciando caffeinate -s -t " + minutes + " minutes");
			Process exec = Runtime.getRuntime().exec("caffeinate -s -t " + seconds);
			if(runningProcess != null){
				runningProcess.destroy();
			}
			runningProcess = exec;
			runningProcessUntil = newUntil;

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void cancelableCaffeinate(String name) {
		if(processes == null){
			processes = new HashMap<String, Process>();
		}
		if(processes.containsKey(name)){
			return;
		}
		try{
			Runtime r = Runtime.getRuntime();
			logger.info("Iniciando caffeinate -s -t 3600");
			processes.put(name, r.exec("caffeinate -s -t 3600"));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void waitFor(String name) throws InterruptedException {
		if(processes == null || !processes.containsKey(name)){
			return;
		}
		processes.get(name).waitFor();
	}

	public void cancel(String name) {
		if(processes == null || !processes.containsKey(name)){
			return;
		}
		synchronized (this) {
			Process currentProcess = processes.get(name);
			if(currentProcess != null){
				currentProcess.destroy();
				try {
					currentProcess.waitFor();
				} catch (InterruptedException e) {
				}
				processes.remove(name);
			}
		}
	}

}
