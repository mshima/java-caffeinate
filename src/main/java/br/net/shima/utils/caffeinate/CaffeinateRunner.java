package br.net.shima.utils.caffeinate;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

public class CaffeinateRunner {

	private static final int TINY_TIMEOUT = 2;
	private static final int SMALL_TIMEOUT = 5;
	private static final int DEFAULT_TIMEOUT = 10;
	private static final int MEDIUM_TIMEOUT = 30;

	private static final String CANCELABLE_COMMAND = "caffeinate -s -t 3600";

	private static final Logger logger = Logger.getLogger(CaffeinateRunner.class.getName());

	private final String source;

	private Process runningProcess;
	private Date runningProcessUntil;
	private boolean processRunning = false;

	private final Map<String, Process> processes;

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
		this.source = "Unknown";
		this.processes = new HashMap<String, Process>();
	}

	public CaffeinateRunner(String source) {
		this.source = source;
		this.processes = new HashMap<String, Process>();
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
		Date newUntil;
		if(!seconds){
			newUntil = DateUtils.addMinutes(new Date(), timeout);
			newUntil = DateUtils.ceiling(newUntil, Calendar.MINUTE);
		}else{
			newUntil = DateUtils.addSeconds(new Date(), timeout);
		}
		if(processRunning && runningProcessUntil != null && runningProcessUntil.after(newUntil)){
			logger.fine(this.source + ": Caffeinate already running");
			return true;
		}

		try{
			if(!seconds){
				timeout = timeout * 60;
			}
			String command = "caffeinate -s -t " + timeout;
			logger.info(this.source + ": Starting " + command + " until " + DateFormatUtils.ISO_TIME_NO_T_FORMAT.format(newUntil));
			Process exec = Runtime.getRuntime().exec(command);

			synchronized (this) {
				this.processRunning = true;

				if(runningProcess != null){
					runningProcess.destroy();
					try {
						runningProcess.waitFor();
					} catch (InterruptedException e) {
					}
				}

				new Thread(new CaffeinateNotifier(this, exec, newUntil)).run();

				runningProcess = exec;
				runningProcessUntil = newUntil;
			}
			return true;

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean cancelableCaffeinate(String name) {
		if(this.processes == null){
			return false;
		}
		synchronized (this.processes) {
			if(this.processes.containsKey(name)){
				return false;
			}
			try{
				Runtime r = Runtime.getRuntime();
				String command = CANCELABLE_COMMAND;
				logger.info(this.source + ": Starting cancelable (" + name + ") " + command);
				this.processes.put(name, r.exec(command));
				return true;

			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
	}

	public void waitFor(String name) throws InterruptedException {
		if(this.processes == null || !this.processes.containsKey(name)){
			return;
		}
		this.processes.get(name).waitFor();
	}

	public boolean cancel(String name) {
		if(this.processes == null){
			return false;
		}
		synchronized (this.processes) {
			if(!this.processes.containsKey(name)){
				return false;
			}
			Process currentProcess = this.processes.get(name);
			if(currentProcess != null){
				logger.info(this.source + ": Stoping cancelable (" + name + ")");
				currentProcess.destroy();
				try {
					currentProcess.waitFor();
				} catch (InterruptedException e) {
				}
				this.processes.remove(name);
			}else{
				logger.fine(this.source + ": Cancelable not running (" + name + ")");
			}
		}
		return true;
	}

	private void setProcessNotRunning(CaffeinateNotifier caffeinateNotifier, int status, Date until) {
		synchronized (this) {
			if(this.runningProcess == caffeinateNotifier.getProcess()){
				this.processRunning = false;
			}
		}
		logger.info("Caffeinate exited (" + status + ") was until " + DateFormatUtils.ISO_TIME_NO_T_FORMAT.format(until));
	}

	private void setCancelableProcessNotRunning(CancelableCaffeinateNotifier caffeinateNotifier, int status, String name) {
		synchronized (this.processes) {
			this.processes.remove(name);
		}
		logger.info("Caffeinate exited (" + status + ") with name " + name);
	}

	public static class CaffeinateNotifier implements Runnable
	{
		private final CaffeinateRunner thread;
		private final Process process;
		private final Date until;

		public CaffeinateNotifier(CaffeinateRunner thread, Process process, Date until) {
			this.thread = thread;
			this.process = process;
			this.until = until;
		}

		@Override
		public void run() {
			int status = -1;
			try {
				status = getProcess().waitFor();
			} catch (InterruptedException e) {
			}
			this.thread.setProcessNotRunning(this, status, this.until);
		}

		public Process getProcess() {
			return this.process;
		}
	}

	public static class CancelableCaffeinateNotifier implements Runnable
	{
		private final CaffeinateRunner thread;
		private final Process process;
		private final String name;

		public CancelableCaffeinateNotifier(CaffeinateRunner thread, Process process, String name) {
			this.thread = thread;
			this.process = process;
			this.name = name;
		}

		@Override
		public void run() {
			int status = -1;
			try {
				status = getProcess().waitFor();
			} catch (InterruptedException e) {
			}
			this.thread.setCancelableProcessNotRunning(this, status, this.name);
		}

		public Process getProcess() {
			return this.process;
		}
	}
}
