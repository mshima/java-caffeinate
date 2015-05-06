package br.net.shima.utils.caffeinate;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.time.DateUtils;

public class CaffeinateRunner {

	private static final int TINY_TIMEOUT = 2;
	private static final int SMALL_TIMEOUT = 5;
	private static final int DEFAULT_TIMEOUT = 10;
	private static final int MEDIUM_TIMEOUT = 30;

	private static Logger logger = Logger.getLogger(CaffeinateRunner.class.getName());

	private Process runningProcess;
	private Date runningProcessUntil;

	private boolean processRunning = false;

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

	public CaffeinateRunner() {
	}

	public boolean caffeinate() {
		return this.caffeinate(DEFAULT_TIMEOUT);
	}

	public boolean caffeinateSeconds(int seconds) {
		return this.caffeinate(true, seconds);
	}

	public boolean tinyCaffeinate() {
		return this.caffeinate(TINY_TIMEOUT);
	}

	public boolean smallCaffeinate() {
		return this.caffeinate(SMALL_TIMEOUT);
	}

	public boolean mediumCaffeinate() {
		return this.caffeinate(MEDIUM_TIMEOUT);
	}

	public boolean caffeinate(int minutes) {
		return this.caffeinate(false, minutes);
	}

	private boolean caffeinate(boolean seconds, int timeout) {
		Date newUntil = DateUtils.addMinutes(new Date(), timeout);
		if(processRunning && runningProcessUntil != null && runningProcessUntil.after(newUntil)){
			return true;
		}

		try{
			if(!seconds){
				timeout = timeout * 60;
				logger.info("Starting caffeinate -s -t " + timeout + " minutes");
			}else{
				logger.info("Starting caffeinate -s -t " + timeout + " seconds");
			}
			Process exec = Runtime.getRuntime().exec("caffeinate -s -t " + timeout);
			this.processRunning = true;

			if(runningProcess != null){
				runningProcess.destroy();
				try {
					runningProcess.waitFor();
				} catch (InterruptedException e) {
				}
			}

			new Thread(new CaffeinateNotifier(this, exec)).run();

			runningProcess = exec;
			runningProcessUntil = newUntil;
			return true;

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean cancelableCaffeinate(String name) {
		if(processes == null){
			processes = new HashMap<String, Process>();
		}
		if(processes.containsKey(name)){
			return false;
		}
		try{
			Runtime r = Runtime.getRuntime();
			logger.info("Starting caffeinate -s -t 3600");
			processes.put(name, r.exec("caffeinate -s -t 3600"));
			return true;

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public void waitFor(String name) throws InterruptedException {
		if(processes == null || !processes.containsKey(name)){
			return;
		}
		processes.get(name).waitFor();
	}

	public boolean cancel(String name) {
		if(processes == null || !processes.containsKey(name)){
			return false;
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
		return true;
	}

	private void setProcessNotRunning(int status) {
		this.processRunning = false;
		logger.info("Caffeinate exited");
	}

	public static class CaffeinateNotifier implements Runnable
	{
		private final CaffeinateRunner thread;
		private final Process p;

		public CaffeinateNotifier(CaffeinateRunner thread, Process p) {
			this.thread = thread;
			this.p = p;
		}

		@Override
		public void run() {
			int status = -1;
			try {
				status = p.waitFor();
			} catch (InterruptedException e) {
			}
			this.thread.setProcessNotRunning(status);
		}
	}
}
