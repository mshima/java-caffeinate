package br.net.shima.utils.caffeinate;
import br.net.shima.utils.caffeinate.CaffeinateThread;


public class CaffeinateTest {
	public static void main(String[] args) {
		try {
			CaffeinateThread caffeine = new CaffeinateThread();
			caffeine.caffeinate(1);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
