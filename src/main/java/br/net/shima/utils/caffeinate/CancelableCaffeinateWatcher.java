package br.net.shima.utils.caffeinate;

public class CancelableCaffeinateWatcher extends CaffeinateWatcher
{
	private final String name;

	public CancelableCaffeinateWatcher(CaffeinateRunner thread, Process process, String name) {
		super(thread, process);
		this.name = name;
	}

	@Override
	public void run() {
		this.getThread().setCancelableProcessNotRunning(this, this.waitForProcess(), this.name);
	}
}
